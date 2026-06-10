package com.mj.api.client;

import com.mj.common.domain.Result;
import com.mj.common.domain.StoryboardDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Agent服务 Feign客户端
 */
@FeignClient(name = "agent-service")
public interface AgentClient {

    /**
     * 调用ScriptAgent生成标准化分镜
     */
    @PostMapping("/agent/script/generate")
    Result<StoryboardDTO> generateStoryboard(@RequestBody Map<String, Object> params);

    // ==================== 异步任务接口 ====================

    /**
     * 提交漫画生成任务（异步，立即返回）
     */
    @PostMapping("/agent/enhance/comic/submit")
    Result<Map<String, Object>> submitComic(@RequestBody Map<String, Object> params);

    /**
     * 查询漫画生成任务状态和结果
     */
    @GetMapping("/agent/enhance/comic/status")
    Result<Map<String, Object>> getComicStatus(@RequestParam("taskId") Long taskId);

    /**
     * 提交TTS合成任务（异步，立即返回）
     */
    @PostMapping("/agent/enhance/tts/submit")
    Result<Map<String, Object>> submitTTS(@RequestBody Map<String, Object> params);

    /**
     * 查询TTS合成任务状态和结果
     */
    @GetMapping("/agent/enhance/tts/status")
    Result<Map<String, Object>> getTTSStatus(@RequestParam("taskId") Long taskId);

    // ==================== 文生视频异步任务接口 ====================

    /**
     * 提交文生视频任务（异步，立即返回）
     */
    @PostMapping("/agent/enhance/video/submit")
    Result<Map<String, Object>> submitVideo(@RequestBody Map<String, Object> params);

    /**
     * 查询文生视频任务状态和结果
     */
    @GetMapping("/agent/enhance/video/status")
    Result<Map<String, Object>> getVideoStatus(@RequestParam("taskId") Long taskId);
}