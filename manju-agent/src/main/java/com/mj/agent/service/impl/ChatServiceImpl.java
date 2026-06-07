package com.mj.agent.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mj.agent.config.SystemPromptConfig;
import com.mj.agent.config.ToolResultHolder;
import com.mj.agent.constants.Constant;
import com.mj.agent.domain.vo.ChatEventVO;
import com.mj.agent.domain.vo.MessageVO;
import com.mj.agent.enums.ChatEventTypeEnum;
import com.mj.agent.enums.MessageTypeEnum;
import com.mj.agent.memory.MyAssistantMessage;
import com.mj.agent.service.ChatService;
import com.mj.agent.service.ChatSessionService;
import com.mj.common.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    // 存储大模型的生成状态，这里采用ConcurrentHashMap是确保线程安全
    // 目前的版本暂时用Map实现，如果考虑分布式环境的话，可以考虑用redis来实现
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;
    private final ChatSessionService chatSessionService;
    // 输出结束的标记
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 获取对话id
        String conversationId = ChatService.getConversationId(sessionId);
        // 创建请求ID
        String requestId = IdUtil.fastSimpleUUID();
        // 创建RAG增强
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build())
                .build();

        // 更新会话信息
        chatSessionService.update(sessionId, question, UserContext.getUserId());

        return this.chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptConfig.getChatSystemMessage().get())// 设置系统提示语
                        .param("now", DateUtil.now())//设置当前时间参数
                )
                .advisors(advisor -> advisor
                        //设置RAG增强
                        .advisors(qaAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(Constant.REQUEST_ID, requestId)) // 通过工具上下文传递参数
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(() -> GENERATE_STATUS.put(sessionId, true)) // 第一次输出内容时执行
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 出现异常时，删除标识
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 完成时执行，删除标识
                .takeWhile(response -> { // 通过返回值来控制Flux流是否继续，true：继续，false：终止
                    return GENERATE_STATUS.getOrDefault(sessionId, false);
                })
                .map(chatResponse -> {
                    // 对于响应结果进行处理，如果是最后一条数据，就把此次消息id放到内存中
                    // 主要用于存储消息数据到 redis中，可以根据消息id获取的请求id，再通过请求id就可以获取到参数列表了
                    // 从而解决，在历史聊天记录中没有外参数的问题
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(Constant.STOP, finishReason)) {
                        String messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId);
                    }
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    // 通过请求id获取到参数列表，如果不为空，就将其追加到返回结果中
                    Map<String, Object> map = ToolResultHolder.get(requestId);
                    if (CollUtil.isNotEmpty(map)) {
                        ToolResultHolder.remove(requestId); // 清除参数列表
                        // 响应给前端的参数数据
                        ChatEventVO chatEventVO = ChatEventVO.builder()
                                .eventData(map)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build();
                        return Flux.just(chatEventVO, STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }

    @Override
    public void stop(String sessionId) {
        // 移除标记
        GENERATE_STATUS.remove(sessionId);
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        // 根据会话ID获取对话ID
        String conversationId = ChatService.getConversationId(sessionId);
        // 从Redis中获取历史消息
        List<Message> messageList = this.chatMemory.get(conversationId);
        // 过滤并转换消息列表
        return StreamUtil.of(messageList)
                // 过滤掉非用户消息和助手消息
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                // 转换为MessageVO对象
                .map(message -> {
                    if (message instanceof MyAssistantMessage) {
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(((MyAssistantMessage) message).getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                })
                .toList();
    }

}
