package com.mj.agent.controller;

import com.mj.agent.domain.vo.ChatSessionVO;
import com.mj.agent.domain.vo.SessionVO;
import com.mj.agent.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 新建会话
     */
    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return chatSessionService.createSession(num);
    }

    /**
     * 查询历史会话列表
     */
    @GetMapping("/history")
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        return chatSessionService.queryHistorySession();
    }

    @DeleteMapping("/history")
    public void deleteSession(@RequestParam String sessionId) {
        chatSessionService.deleteHistorySession(sessionId);
    }

}