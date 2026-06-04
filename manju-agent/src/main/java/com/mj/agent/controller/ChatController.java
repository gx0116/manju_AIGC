package com.mj.agent.controller;

import com.mj.common.domain.Result;
import com.mj.agent.domain.dto.ChatDTO;
import com.mj.agent.domain.vo.ChatEventVO;
import com.mj.common.annotations.NoWrapper;
import com.mj.agent.domain.vo.MessageVO;
import com.mj.agent.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @NoWrapper // 标记结果不进行包装
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }

    @PostMapping("/stop")
    public void stop(@RequestParam("sessionId") String sessionId) {
        chatService.stop(sessionId);
    }

    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public Result<List<MessageVO>> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        List<MessageVO> messageVOS = chatService.queryBySessionId(sessionId);
        return Result.success(messageVOS);
    }

}
