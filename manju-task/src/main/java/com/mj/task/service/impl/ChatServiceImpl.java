package com.mj.task.service.impl;

import com.mj.task.domain.vo.ChatEventVO;
import com.mj.task.enums.ChatEventTypeEnum;
import com.mj.task.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;


    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        return this.chatClient.prompt()
                .user(question)
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()  // 标记输出结束
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }
}
