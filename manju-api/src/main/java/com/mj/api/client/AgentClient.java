package com.mj.api.client;

import com.mj.common.domain.ComicGenResultDTO;
import com.mj.common.domain.Result;
import com.mj.common.domain.StoryboardDTO;
import com.mj.common.domain.TTSResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
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

    /**
     * 调用增强Agent - 文生漫画（Liblib风格）
     */
    @PostMapping("/agent/enhance/comic")
    Result<List<ComicGenResultDTO>> generateComic(@RequestBody Map<String, Object> params);

    /**
     * 调用增强Agent - TTS语音合成（Azure风格）
     */
    @PostMapping("/agent/enhance/tts")
    Result<List<TTSResultDTO>> generateTTS(@RequestBody Map<String, Object> params);
}