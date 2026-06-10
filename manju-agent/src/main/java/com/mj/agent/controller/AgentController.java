package com.mj.agent.controller;

import com.mj.agent.service.AgentService;
import com.mj.agent.service.AIGCTaskManager;
import com.mj.common.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final AIGCTaskManager taskManager;

    /**
     * ScriptAgent - 生成标准化分镜
     */
    @PostMapping("/script/generate")
    public Result<StoryboardDTO> generateStoryboard(@RequestBody Map<String, Object> params) {
        log.info("[AgentController] 收到分镜生成请求, taskId={}", params.get("taskId"));
        StoryboardDTO storyboard = agentService.generateStoryboard(params);
        return Result.success(storyboard);
    }

    // ==================== 漫画生成（异步模式） ====================

    /**
     * 提交漫画生成任务（异步）
     * <p>
     * 立即返回，后台异步处理。前端通过 /enhance/comic/status?taskId=xx 轮询结果。
     */
    @PostMapping("/enhance/comic/submit")
    public Result<Map<String, Object>> submitComic(@RequestBody Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        log.info("[AgentController] 提交漫画异步任务, taskId={}", taskId);

        // 检查是否有正在进行的任务
        String currentStatus = taskManager.getStatus("comic", taskId);
        if ("PROCESSING".equals(currentStatus)) {
            return Result.error(409, "漫画生成任务正在处理中，请勿重复提交");
        }

        // 标记为处理中并异步执行
        agentService.generateComicImagesAsync(params);

        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", "PROCESSING");
        resp.put("message", "漫画生成任务已提交，请通过 /enhance/comic/status 轮询结果");
        return Result.success(resp);
    }

    /**
     * 查询漫画生成任务状态和结果
     */
    @GetMapping("/enhance/comic/status")
    public Result<Map<String, Object>> getComicStatus(@RequestParam Long taskId) {
        String status = taskManager.getStatus("comic", taskId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", status != null ? status : "NOT_FOUND");

        if ("COMPLETED".equals(status)) {
            List<ComicGenResultDTO> results = taskManager.getComicResult(taskId);
            resp.put("data", results);
        } else if ("FAILED".equals(status)) {
            resp.put("error", taskManager.getError("comic", taskId));
        }

        return Result.success(resp);
    }

    // ==================== TTS合成（异步模式） ====================

    /**
     * 提交TTS合成任务（异步）
     * <p>
     * 立即返回，后台异步处理。前端通过 /enhance/tts/status?taskId=xx 轮询结果。
     */
    @PostMapping("/enhance/tts/submit")
    public Result<Map<String, Object>> submitTTS(@RequestBody Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        log.info("[AgentController] 提交TTS异步任务, taskId={}", taskId);

        String currentStatus = taskManager.getStatus("tts", taskId);
        if ("PROCESSING".equals(currentStatus)) {
            return Result.error(409, "TTS合成任务正在处理中，请勿重复提交");
        }

        agentService.generateTTSAudioAsync(params);

        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", "PROCESSING");
        resp.put("message", "TTS合成任务已提交，请通过 /enhance/tts/status 轮询结果");
        return Result.success(resp);
    }

    /**
     * 查询TTS合成任务状态和结果
     */
    @GetMapping("/enhance/tts/status")
    public Result<Map<String, Object>> getTTSStatus(@RequestParam Long taskId) {
        String status = taskManager.getStatus("tts", taskId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("taskId", taskId);
        resp.put("status", status != null ? status : "NOT_FOUND");

        if ("COMPLETED".equals(status)) {
            List<TTSResultDTO> results = taskManager.getTTSResult(taskId);
            resp.put("data", results);
        } else if ("FAILED".equals(status)) {
            resp.put("error", taskManager.getError("tts", taskId));
        }

        return Result.success(resp);
    }

    // ==================== 旧同步接口（已改为异步模式，向后兼容） ====================

    /**
     * 漫画生成
     */
    @PostMapping("/enhance/comic")
    public List<ComicGenResultDTO> generateComic(@RequestBody Map<String, Object> params) {
        log.info("[AgentController] 收到漫画生成请求, taskId={}", params.get("taskId"));
        return agentService.generateComicImages(params);
    }

    /**
     * TTS合成
     */
    @PostMapping("/enhance/tts")
    public List<TTSResultDTO> generateTTS(@RequestBody Map<String, Object> params) {
        log.info("[AgentController] 收到TTS合成请求, taskId={}", params.get("taskId"));
        return agentService.generateTTSAudio(params);
    }
}