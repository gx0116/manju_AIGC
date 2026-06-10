package com.mj.agent.service;

import com.mj.common.domain.ComicGenResultDTO;
import com.mj.common.domain.StoryboardDTO;
import com.mj.common.domain.TTSResultDTO;
import com.mj.common.domain.VideoGenResultDTO;

import java.util.List;
import java.util.Map;

/**
 * AI Agent服务 - 分镜生成 + 漫画增强 + TTS合成 + 文生视频
 */
public interface AgentService {

    /**
     * ScriptAgent - 根据剧情描述生成标准化分镜
     */
    StoryboardDTO generateStoryboard(Map<String, Object> params);

    /**
     * EnhancementAgent - Liblib文生漫画图片（同步，直接返回结果）
     */
    List<ComicGenResultDTO> generateComicImages(Map<String, Object> params);

    /**
     * EnhancementAgent - 文生漫画（异步，结果写入Redis，通过轮询接口获取）
     */
    void generateComicImagesAsync(Map<String, Object> params);

    /**
     * EnhancementAgent - Azure TTS语音合成（同步，直接返回结果）
     */
    List<TTSResultDTO> generateTTSAudio(Map<String, Object> params);

    /**
     * EnhancementAgent - TTS语音合成（异步，结果写入Redis，通过轮询接口获取）
     */
    void generateTTSAudioAsync(Map<String, Object> params);

    /**
     * EnhancementAgent - 文生视频（同步，直接返回结果）
     * <p>
     * 逐镜头调用DashScope通义万相文生视频API，每个分镜生成一段短视频
     */
    List<VideoGenResultDTO> generateVideo(Map<String, Object> params);

    /**
     * EnhancementAgent - 文生视频（异步，结果写入Redis，通过轮询接口获取）
     */
    void generateVideoAsync(Map<String, Object> params);
}