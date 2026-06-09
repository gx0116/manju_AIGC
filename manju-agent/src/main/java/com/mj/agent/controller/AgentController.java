package com.mj.agent.controller;

import com.mj.agent.service.AgentService;
import com.mj.common.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent服务控制器 - 提供ScriptAgent和EnhanceAgent的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * ScriptAgent - 生成标准化分镜
     */
    @PostMapping("/script/generate")
    public Result<StoryboardDTO> generateStoryboard(@RequestBody Map<String, Object> params) {
        log.info("[AgentController] 收到分镜生成请求, taskId={}", params.get("taskId"));
        StoryboardDTO storyboard = agentService.generateStoryboard(params);
        return Result.success(storyboard);
    }

    /**
     * EnhancementAgent - 文生漫画（Liblib风格）
     */
    @PostMapping("/enhance/comic")
    public Result<List<ComicGenResultDTO>> generateComic(@RequestBody Map<String, Object> params) {
        log.info("[AgentController] 收到漫画生成请求, taskId={}", params.get("taskId"));
        List<ComicGenResultDTO> results = agentService.generateComicImages(params);
        return Result.success(results);
    }

    /**
     * EnhancementAgent - TTS语音合成（Azure风格）
     */
    @PostMapping("/enhance/tts")
    public Result<List<TTSResultDTO>> generateTTS(@RequestBody Map<String, Object> params) {
        log.info("[AgentController] 收到TTS合成请求, taskId={}", params.get("taskId"));
        List<TTSResultDTO> results = agentService.generateTTSAudio(params);
        return Result.success(results);
    }
}