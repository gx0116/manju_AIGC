package com.mj.agent.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.agent.config.SystemPromptConfig;
import com.mj.agent.service.AgentService;
import com.mj.agent.service.AIGCTaskManager;
import com.mj.common.domain.ComicGenResultDTO;
import com.mj.common.domain.StoryboardDTO;
import com.mj.common.domain.TTSResultDTO;
import com.mj.common.domain.VideoGenResultDTO;
import com.mj.common.enums.ArtStyleEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Agent服务实现
 * <p>
 * ScriptAgent: 使用ChatClient+Prompt生成标准化分镜JSON
 * EnhancementAgent-Comic: 调用图像生成模型（DashScope/StableDiffusion）生成漫画图
 * EnhancementAgent-TTS: 调用TTS服务合成语音
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final SystemPromptConfig systemPromptConfig;
    private final ImageModel imageModel;
    private final AIGCTaskManager taskManager;

    /** DashScope API Key */
    @Value("${spring.ai.dashscope.api-key:}")
    private String dashScopeApiKey;

    /** TTS 模型配置（从 Nacos 读取） */
    @Value("${mj.ai.tts.model:qwen3-tts-flash}")
    private String ttsModel;

    /** TTS 音色配置（从 Nacos 读取） */
    @Value("${mj.ai.tts.voice:Cherry}")
    private String ttsVoice;

    /** 文生视频模型（从 Nacos 读取，默认 wanx2.1-t2v-turbo 有免费额度） */
    @Value("${mj.ai.video.model:wanx2.1-t2v-turbo}")
    private String videoModel;

    /** 视频分辨率 */
    @Value("${mj.ai.video.resolution:720P}")
    private String videoResolution;

    /** 视频宽高比 */
    @Value("${mj.ai.video.ratio:16:9}")
    private String videoRatio;

    /** 单个镜头视频时长（秒） */
    @Value("${mj.ai.video.duration:4}")
    private Integer videoDuration;

    /** DashScope 文生视频 API 端点 */
    @Value("${mj.ai.video.endpoint:https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis}")
    private String videoEndpoint;

    /** 媒体文件本地存储目录 */
    @Value("${mj.media.storage-dir:/tmp/manju-media}")
    private String storageDir;

    public AgentServiceImpl(ChatClient.Builder chatClientBuilder,
                            ObjectMapper objectMapper,
                            SystemPromptConfig systemPromptConfig,
                            ImageModel imageModel,
                            AIGCTaskManager taskManager) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
        this.systemPromptConfig = systemPromptConfig;
        this.imageModel = imageModel;
        this.taskManager = taskManager;
    }

    // ==================== ScriptAgent: 分镜生成 ====================

    @Override
    public StoryboardDTO generateStoryboard(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String title = (String) params.get("title");
        String description = (String) params.get("description");
        String style = (String) params.get("style");
        String promptKey = (String) params.get("promptKey");
        String mainCharacters = (String) params.get("mainCharacters");

        log.info("[ScriptAgent] 开始生成分镜, taskId={}, title={}, style={}", taskId, title, style);

        // 构建分镜生成的系统提示词
        String systemPrompt = buildStoryboardSystemPrompt(style, promptKey);

        // 构建用户输入
        String userInput = buildStoryboardUserInput(title, description, mainCharacters, style);

        // 构建干净的无记忆ChatClient（分镜生成不需要聊天历史）
        ChatClient cleanClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        // 调用ChatClient生成分镜JSON
        String aiResponse = cleanClient.prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();

        log.info("[ScriptAgent] AI分镜响应, taskId={}, responseLen={}", taskId,
                aiResponse != null ? aiResponse.length() : 0);

        // 解析AI返回的JSON为StoryboardDTO
        return parseStoryboardResponse(taskId, aiResponse);
    }

    /**
     * 构建分镜生成的System Prompt
     */
    private String buildStoryboardSystemPrompt(String style, String promptKey) {
        String basePrompt = """
            你是一位资深漫剧导演、分镜师，擅长创作短视频漫剧。
            请根据用户需求，生成【专业、紧凑、有镜头感、适合AI生成画面】的分镜脚本。

            输出严格规则：
            1. 只返回标准JSON，无任何多余文字、无markdown
            2. 分镜数量：8-12个镜头（节奏紧凑）
            3. 单镜头时长：3-6秒，总时长控制在45-60秒
            4. 每个镜头必须包含：
               - index：序号
               - title：镜头小标题
               - description：剧情简述
               - dialogue：对白/旁白（简洁有力）
               - imagePrompt：**AI文生图专用提示词**（越详细越好，画风统一）
               - cameraType：镜头类型（特写/中景/全景/远景/近景）
               - atmosphere：氛围
               - duration：时长（数字）

            【imagePrompt 强制要求】
            - 必须包含：画风、光影、色彩、构图、人物状态、场景细节
            - 必须适合直接输入AI绘画（Stable Diffusion / Midjourney）
            - 不要使用抽象词

            【镜头节奏要求】
            - 开场：吸引眼球
            - 中间：剧情推进
            - 结尾：留悬念

            返回格式示例：
            {
              "sceneCount": 10,
              "scenes": [
                {
                  "index": 1,
                  "title": "少年觉醒",
                  "description": "小星在星辰遗迹中感受到力量涌动",
                  "dialogue": "这是……星辰的力量？",
                  "imagePrompt": "国风场景，少年站在星空遗迹中央，金色光芒环绕，云雾缭绕，东方神话美学，细节精致，光影唯美",
                  "cameraType": "中景",
                  "atmosphere": "神圣震撼",
                  "duration": 4.0
                }
              ]
            }
            """;

        // 风格强化指令（行业级优化）
        String styleInstruction = switch (promptKey) {
            case "anime_style" -> """
                \n【风格强化：二次元动漫】
                - 大眼睛、精致脸型、光影柔和
                - 高饱和度、明快色彩
                - 动态姿势、镜头张力强
                """;

            case "realistic_style" -> """
                \n【风格强化：写实风格】
                - 真实人物比例、真实光影、电影质感
                - 画面细腻、质感真实
                - 镜头偏电影感
                """;

            case "chinese_style" -> """
                \n【风格强化：中国风/国风】
                - 东方美学、水墨意境、祥云、星辰、古风建筑
                - 色彩典雅：青、金、蓝、白为主
                - 服饰古风、仙气、飘逸、东方神韵
                - 画面唯美、大气、史诗感
                """;

            case "cartoon_style" -> """
                \n【风格强化：卡通风格】
                - 造型Q版/可爱/简洁
                - 色彩明亮、色块清晰
                - 构图简单、轻松有趣
                """;

            default -> "";
        };

        return basePrompt + "\n" + styleInstruction;
    }

    /**
     * 构建分镜生成的User Input
     */
    private String buildStoryboardUserInput(String title, String description,
                                            String mainCharacters, String style) {
        return String.format("""
            请创作一集完整的漫剧分镜：
            
            漫剧标题：%s
            剧情简介：%s
            出场角色：%s
            美术风格：%s
            
            请严格按照要求输出JSON格式分镜，确保imagePrompt可直接用于AI绘画。
            """,
                title,
                description,
                mainCharacters != null ? mainCharacters : "无",
                style
        );
    }

    /**
     * 解析AI返回的分镜JSON
     */
    private StoryboardDTO parseStoryboardResponse(Long taskId, String aiResponse) {
        if (StrUtil.isBlank(aiResponse)) {
            throw new RuntimeException("AI分镜响应为空");
        }

        // 清理可能的markdown代码块标记
        String jsonStr = aiResponse.trim();
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substring(7);
        } else if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substring(3);
        }
        if (jsonStr.endsWith("```")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
        }
        jsonStr = jsonStr.trim();

        try {
            StoryboardDTO storyboard = objectMapper.readValue(jsonStr, StoryboardDTO.class);
            storyboard.setTaskId(taskId);
            // 计算sceneCount
            if (storyboard.getScenes() != null) {
                storyboard.setSceneCount(storyboard.getScenes().size());
            }
            return storyboard;
        } catch (Exception e) {
            log.error("[ScriptAgent] 分镜JSON解析失败, taskId={}, response={}", taskId, aiResponse, e);
            throw new RuntimeException("分镜JSON解析失败: " + e.getMessage(), e);
        }
    }

    // ==================== EnhancementAgent: 文生漫画 ====================

    @Override
    public List<ComicGenResultDTO> generateComicImages(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String style = (String) params.get("style");
        String promptKey = (String) params.get("promptKey");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenesList = (List<Map<String, Object>>) params.get("scenes");

        if (scenesList == null || scenesList.isEmpty()) {
            throw new RuntimeException("分镜列表为空，无法生成漫画");
        }

        log.info("[EnhanceAgent-Comic] 开始生成漫画, taskId={}, sceneCount={}, style={}",
                taskId, scenesList.size(), style);

        List<ComicGenResultDTO> results = new ArrayList<>();

        // 逐镜头调用图像生成API
        for (Map<String, Object> scene : scenesList) {
            Integer index = (Integer) scene.get("index");
            String imagePrompt = (String) scene.get("imagePrompt");

            try {
                ComicGenResultDTO result = generateSingleComicImage(taskId, index, imagePrompt, style, promptKey);
                results.add(result);
                log.info("[EnhanceAgent-Comic] 镜头{}漫画生成完成, taskId={}, url={}",
                        index, taskId, result.getImageUrl());
            } catch (Exception e) {
                log.error("[EnhanceAgent-Comic] 镜头{}生成失败, taskId={}", index, taskId, e);
                // 失败时返回空结果标记
                results.add(ComicGenResultDTO.builder()
                        .taskId(taskId)
                        .sceneIndex(index)
                        .imageUrl(null)
                        .build());
            }
        }

        return results;
    }

    /**
     * 生成单张漫画图片
     * <p>
     * 通过 Spring AI ImageModel 调用 DashScope 通义万相 wanx-v1 文生图API
     */
    private ComicGenResultDTO generateSingleComicImage(Long taskId, Integer index,
                                                        String imagePrompt, String style, String promptKey) {
        long startTime = System.currentTimeMillis();

        // 构建增强的imagePrompt（加入画风修饰）
        String enhancedPrompt = enhanceImagePrompt(imagePrompt, style, promptKey);

        log.info("[EnhanceAgent-Comic] 调用DashScope wanx-v1 图像生成, taskId={}, index={}, promptLen={}",
                taskId, index, enhancedPrompt.length());

        // ============ 实际 DashScope 图像生成调用 ============
        // wanx-v1 是异步模型，DashScopeImageModel 内部通过 Spring Retry 自动轮询等待结果
        ImageResponse response = imageModel.call(new ImagePrompt(enhancedPrompt));
        String tempImageUrl = response.getResult().getOutput().getUrl();

        // DashScope 返回的临时URL有效期较短，下载到本地存储
        String localPath = downloadToLocalStorage(tempImageUrl, taskId, "comic", index);

        long costTime = System.currentTimeMillis() - startTime;
        log.info("[EnhanceAgent-Comic] 图像生成完成, taskId={}, index={}, localPath={}, costTime={}ms",
                taskId, index, localPath, costTime);

        return ComicGenResultDTO.builder()
                .taskId(taskId)
                .sceneIndex(index)
                .imageUrl(localPath)
                .width(1024)
                .height(1024)
                .costTime(costTime)
                .build();
    }

    /**
     * 增强imagePrompt - 根据画风添加修饰词
     */
    private String enhanceImagePrompt(String basePrompt, String style, String promptKey) {
        String styleModifier = switch (promptKey) {
            case "anime_style" ->
                    "anime style, vibrant colors, detailed character design, Studio Ghibli inspired, " + basePrompt;
            case "realistic_style" ->
                    "photorealistic, 8K, detailed textures, cinematic lighting, " + basePrompt;
            case "chinese_style" ->
                    "traditional Chinese ink painting style, watercolor, elegant, " + basePrompt;
            case "cartoon_style" ->
                    "cartoon style, bright colors, simple shapes, fun, " + basePrompt;
            default -> basePrompt;
        };
        return styleModifier;
    }

    // ==================== EnhancementAgent: TTS语音合成 ====================

    @Override
    public List<TTSResultDTO> generateTTSAudio(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenesList = (List<Map<String, Object>>) params.get("scenes");

        if (scenesList == null || scenesList.isEmpty()) {
            throw new RuntimeException("分镜列表为空，无法合成语音");
        }

        log.info("[EnhanceAgent-TTS] 开始TTS合成, taskId={}, sceneCount={}", taskId, scenesList.size());

        List<TTSResultDTO> results = new ArrayList<>();

        // 逐镜头调用TTS API
        for (Map<String, Object> scene : scenesList) {
            Integer index = (Integer) scene.get("index");
            String dialogue = (String) scene.get("dialogue");

            if (StrUtil.isBlank(dialogue)) {
                // 无对白场景跳过
                results.add(TTSResultDTO.builder()
                        .taskId(taskId)
                        .sceneIndex(index)
                        .audioUrl(null)
                        .duration(0.0)
                        .build());
                continue;
            }

            try {
                TTSResultDTO result = synthesizeSpeech(taskId, index, dialogue);
                results.add(result);
                log.info("[EnhanceAgent-TTS] 镜头{} TTS完成, taskId={}, audio={}",
                        index, taskId, result.getAudioUrl());
            } catch (Exception e) {
                log.error("[EnhanceAgent-TTS] 镜头{} TTS失败, taskId={}", index, taskId, e);
                results.add(TTSResultDTO.builder()
                        .taskId(taskId)
                        .sceneIndex(index)
                        .audioUrl(null)
                        .build());
            }
        }

        return results;
    }

    /**
     * 调用DashScope Qwen-TTS 服务合成单段语音
     * <p>
     * 通过 HTTP 调用 multimodal-generation API（SpeechSynthesisModel 使用的是旧版 text-to-speech API，
     * 不支持 qwen3-tts-vd/qwen3-tts-flash 等新模型）
     */
    private TTSResultDTO synthesizeSpeech(Long taskId, Integer index, String dialogue) {
        long startTime = System.currentTimeMillis();

        log.info("[EnhanceAgent-TTS] 调用Qwen-TTS multimodal-generation, taskId={}, index={}, textLen={}",
                taskId, index, dialogue.length());

        // ============ Qwen-TTS multimodal-generation HTTP API ============
        try {
            // 从 Nacos 配置读取模型和音色
            log.info("[EnhanceAgent-TTS] 使用TTS配置, model={}, voice={}", ttsModel, ttsVoice);

            String requestBody = String.format("""
                    {
                        "model": "%s",
                        "input": {
                            "text": "%s",
                            "voice": "%s"
                        },
                        "parameters": {
                            "volume": 80,
                            "speed": 1.0
                        }
                    }
                    """, ttsModel, escapeJson(dialogue), ttsVoice);

            // 调用 multimodal-generation 端点
            HttpResponse response = HttpRequest.post(
                            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
                    .header("Authorization", "Bearer " + dashScopeApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .timeout(120000)
                    .execute();

            if (!response.isOk()) {
                String errorBody = response.body();
                log.error("[EnhanceAgent-TTS] HTTP请求失败, status={}, body={}", response.getStatus(), errorBody);
                throw new RuntimeException("TTS HTTP请求失败: " + response.getStatus() + " - " + errorBody);
            }

            // 响应为 JSON，解析获取音频 URL
            String respBody = response.body();
            Map<String, Object> respMap = objectMapper.readValue(respBody, new TypeReference<>() {});
            Map<String, Object> output = (Map<String, Object>) respMap.get("output");
            Map<String, Object> audio = (Map<String, Object>) output.get("audio");
            String audioUrl = (String) audio.get("url");

            if (audioUrl == null) {
                throw new RuntimeException("TTS响应中未找到音频URL: " + respBody);
            }

            // 下载音频到本地
            String localPath = downloadToLocalStorage(audioUrl, taskId, "tts", index);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("[EnhanceAgent-TTS] TTS合成完成, taskId={}, index={}, localPath={}, costTime={}ms",
                    taskId, index, localPath, costTime);

            return TTSResultDTO.builder()
                    .taskId(taskId)
                    .sceneIndex(index)
                    .audioUrl(localPath)
                    .duration(0.0)   // 实际时长由播放端确定
                    .costTime(costTime)
                    .build();

        } catch (Exception e) {
            log.error("[EnhanceAgent-TTS] TTS调用失败, taskId={}, index={}", taskId, index, e);
            throw new RuntimeException("TTS语音合成失败: " + e.getMessage(), e);
        }
    }

    // ==================== EnhancementAgent: 文生视频 ====================

    @Override
    public List<VideoGenResultDTO> generateVideo(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String style = (String) params.get("style");
        String promptKey = (String) params.get("promptKey");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenesList = (List<Map<String, Object>>) params.get("scenes");

        if (scenesList == null || scenesList.isEmpty()) {
            throw new RuntimeException("分镜列表为空，无法生成视频");
        }

        log.info("[EnhanceAgent-Video] 开始文生视频, taskId={}, sceneCount={}, model={}",
                taskId, scenesList.size(), videoModel);

        List<VideoGenResultDTO> results = new ArrayList<>();

        // 逐镜头调用文生视频API
        for (Map<String, Object> scene : scenesList) {
            Integer index = (Integer) scene.get("index");
            String imagePrompt = (String) scene.get("imagePrompt");

            try {
                VideoGenResultDTO result = generateSingleVideo(taskId, index, imagePrompt, style, promptKey);
                results.add(result);
                log.info("[EnhanceAgent-Video] 镜头{}视频生成完成, taskId={}, url={}",
                        index, taskId, result.getVideoUrl());
            } catch (Exception e) {
                log.error("[EnhanceAgent-Video] 镜头{}视频生成失败, taskId={}", index, taskId, e);
                results.add(VideoGenResultDTO.builder()
                        .taskId(taskId)
                        .sceneIndex(index)
                        .videoUrl(null)
                        .build());
            }
        }

        return results;
    }

    /**
     * 生成单个镜头的短视频
     * <p>
     * 通过 DashScope 通义万相文生视频 API，异步创建任务 → 轮询等待 → 下载视频
     */
    private VideoGenResultDTO generateSingleVideo(Long taskId, Integer index,
                                                    String imagePrompt, String style, String promptKey) {
        long startTime = System.currentTimeMillis();

        // 构建增强的 videoPrompt（融合画风描述+动态运镜）
        String videoPrompt = enhanceVideoPrompt(imagePrompt, style, promptKey);

        log.info("[EnhanceAgent-Video] 调用DashScope文生视频, taskId={}, index={}, model={}, promptLen={}",
                taskId, index, videoModel, videoPrompt.length());

        try {
            // ============ Step 1: 创建异步任务 ============
            // wanx2.1 系列模型不支持 duration 参数（固定5秒），仅 wan2.7+ 支持
            String requestBody;
            if (videoModel != null && videoModel.startsWith("wan2.")) {
                // wan2.7 新版协议，支持 duration 自定义
                requestBody = String.format("""
                    {
                        "model": "%s",
                        "input": {
                            "prompt": "%s"
                        },
                        "parameters": {
                            "resolution": "%s",
                            "ratio": "%s",
                            "prompt_extend": true,
                            "duration": %d
                        }
                    }
                    """, videoModel, escapeJson(videoPrompt), videoResolution, videoRatio, videoDuration);
            } else {
                // wanx2.1 旧版，不支持 duration，固定5秒
                requestBody = String.format("""
                    {
                        "model": "%s",
                        "input": {
                            "prompt": "%s"
                        },
                        "parameters": {
                            "resolution": "%s",
                            "ratio": "%s",
                            "prompt_extend": true
                        }
                    }
                    """, videoModel, escapeJson(videoPrompt), videoResolution, videoRatio);
            }

            HttpResponse createResp = HttpRequest.post(videoEndpoint)
                    .header("Authorization", "Bearer " + dashScopeApiKey)
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable")
                    .body(requestBody)
                    .timeout(30000)
                    .execute();

            if (!createResp.isOk()) {
                String errorBody = createResp.body();
                log.error("[EnhanceAgent-Video] 创建任务失败, status={}, body={}", createResp.getStatus(), errorBody);
                throw new RuntimeException("文生视频任务创建失败: " + createResp.getStatus() + " - " + errorBody);
            }

            // 解析 task_id
            String createBody = createResp.body();
            Map<String, Object> createMap = objectMapper.readValue(createBody, new TypeReference<>() {});
            Map<String, Object> output = (Map<String, Object>) createMap.get("output");
            String dashTaskId = (String) output.get("task_id");

            if (dashTaskId == null) {
                throw new RuntimeException("文生视频响应中未找到task_id: " + createBody);
            }
            log.info("[EnhanceAgent-Video] 任务已创建, taskId={}, index={}, dashTaskId={}", taskId, index, dashTaskId);

            // ============ Step 2: 轮询等待完成 ============
            String videoUrl = pollVideoTask(dashTaskId, taskId, index);

            // ============ Step 3: 下载到本地存储 ============
            String localPath = downloadToLocalStorage(videoUrl, taskId, "video", index);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("[EnhanceAgent-Video] 视频生成完成, taskId={}, index={}, localPath={}, costTime={}ms",
                    taskId, index, localPath, costTime);

            return VideoGenResultDTO.builder()
                    .taskId(taskId)
                    .sceneIndex(index)
                    .videoUrl(localPath)
                    .duration(isWan27Model() ? videoDuration : 5)
                    .width(determineVideoWidth())
                    .height(determineVideoHeight())
                    .costTime(costTime)
                    .build();

        } catch (Exception e) {
            log.error("[EnhanceAgent-Video] 视频生成异常, taskId={}, index={}", taskId, index, e);
            throw new RuntimeException("文生视频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 轮询DashScope视频任务直到完成
     * <p>
     * 状态流转：PENDING → RUNNING → SUCCEEDED / FAILED
     */
    private String pollVideoTask(String dashTaskId, Long taskId, Integer index) {
        String queryUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + dashTaskId;
        int maxRetries = 120; // 最多轮询120次（120 * 5秒 = 10分钟）

        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(5000); // 每5秒轮询一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("视频任务轮询被中断", e);
            }

            try {
                HttpResponse queryResp = HttpRequest.get(queryUrl)
                        .header("Authorization", "Bearer " + dashScopeApiKey)
                        .timeout(10000)
                        .execute();

                if (!queryResp.isOk()) {
                    log.warn("[EnhanceAgent-Video] 查询任务状态失败, dashTaskId={}, status={}, retry={}",
                            dashTaskId, queryResp.getStatus(), i);
                    continue;
                }

                String queryBody = queryResp.body();
                Map<String, Object> queryMap = objectMapper.readValue(queryBody, new TypeReference<>() {});
                Map<String, Object> queryOutput = (Map<String, Object>) queryMap.get("output");
                String taskStatus = (String) queryOutput.get("task_status");

                log.info("[EnhanceAgent-Video] 任务状态轮询, taskId={}, index={}, dashTaskId={}, status={}, retry={}",
                        taskId, index, dashTaskId, taskStatus, i);

                if ("SUCCEEDED".equals(taskStatus)) {
                    String videoUrl = (String) queryOutput.get("video_url");
                    if (videoUrl == null) {
                        // 某些模型版本可能用 results 字段
                        Object results = queryOutput.get("results");
                        if (results instanceof List && !((List<?>) results).isEmpty()) {
                            Map<String, Object> firstResult = (Map<String, Object>) ((List<?>) results).get(0);
                            videoUrl = (String) firstResult.get("video_url");
                        }
                    }
                    if (videoUrl == null) {
                        throw new RuntimeException("视频任务成功但未返回video_url: " + queryBody);
                    }
                    log.info("[EnhanceAgent-Video] 视频生成成功, dashTaskId={}, videoUrl={}", dashTaskId, videoUrl);
                    return videoUrl;
                } else if ("FAILED".equals(taskStatus)) {
                    String errorMsg = (String) queryOutput.get("message");
                    if (errorMsg == null) errorMsg = (String) queryOutput.get("code");
                    throw new RuntimeException("视频生成失败: " + (errorMsg != null ? errorMsg : "未知错误"));
                }
                // PENDING / RUNNING → 继续轮询

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[EnhanceAgent-Video] 轮询异常, dashTaskId={}, retry={}", dashTaskId, i, e);
            }
        }

        throw new RuntimeException("视频生成超时，dashTaskId=" + dashTaskId);
    }

    /**
     * 增强视频Prompt - 添加画风+动态描述
     */
    private String enhanceVideoPrompt(String basePrompt, String style, String promptKey) {
        // 动态运镜描述
        String cameraMotion = "cinematic camera movement, smooth panning, dynamic shots";

        String styleModifier = switch (promptKey != null ? promptKey : "") {
            case "anime_style" ->
                    "anime style, vibrant colors, fluid animation, Studio Ghibli inspired, " + cameraMotion + ", " + basePrompt;
            case "realistic_style" ->
                    "photorealistic, 8K, cinematic lighting, film grain, " + cameraMotion + ", " + basePrompt;
            case "chinese_style" ->
                    "traditional Chinese ink painting animation, watercolor flow, elegant motion, " + cameraMotion + ", " + basePrompt;
            case "cartoon_style" ->
                    "cartoon style, bright colors, bouncy animation, fun motion, " + cameraMotion + ", " + basePrompt;
            default -> cameraMotion + ", " + basePrompt;
        };
        return styleModifier;
    }

    /**
     * 根据配置的分辨率确定视频宽度
     */
    private Integer determineVideoWidth() {
        if ("720P".equalsIgnoreCase(videoResolution)) {
            return "9:16".equals(videoRatio) ? 720 : "1:1".equals(videoRatio) ? 960 :
                   "4:3".equals(videoRatio) ? 1104 : "3:4".equals(videoRatio) ? 832 : 1280;
        }
        // 1080P
        return "9:16".equals(videoRatio) ? 1080 : "1:1".equals(videoRatio) ? 1440 :
               "4:3".equals(videoRatio) ? 1648 : "3:4".equals(videoRatio) ? 1248 : 1920;
    }

    /**
     * 根据配置的分辨率确定视频高度
     */
    private Integer determineVideoHeight() {
        if ("720P".equalsIgnoreCase(videoResolution)) {
            return "9:16".equals(videoRatio) ? 1280 : "1:1".equals(videoRatio) ? 960 :
                   "4:3".equals(videoRatio) ? 832 : "3:4".equals(videoRatio) ? 1104 : 720;
        }
        // 1080P
        return "9:16".equals(videoRatio) ? 1920 : "1:1".equals(videoRatio) ? 1440 :
               "4:3".equals(videoRatio) ? 1248 : "3:4".equals(videoRatio) ? 1648 : 1080;
    }

    /**
     * 判断是否为 wan2.7+ 新协议模型（支持 duration 自定义）
     */
    private boolean isWan27Model() {
        return videoModel != null && videoModel.startsWith("wan2.");
    }

    // ==================== 文件存储辅助方法 ====================

    /**
     * 从URL下载文件到本地存储
     */
    private String downloadToLocalStorage(String url, Long taskId, String type, Integer index) {
        if (StrUtil.isBlank(url)) {
            log.warn("[AgentService] downloadToLocalStorage url为空, taskId={}, type={}, index={}", taskId, type, index);
            return null;
        }

        String ext = determineFileExtension(url);
        String dir = storageDir + File.separator + type + File.separator + "task_" + taskId;
        FileUtil.mkdir(dir);
        String localPath = dir + File.separator + type + "_" + index + ext;

        try {
            long fileSize = HttpUtil.downloadFile(url, localPath);
            log.info("[AgentService] 文件下载成功, url={} -> localPath={}, size={}bytes", url, localPath, fileSize);
        } catch (Exception e) {
            log.error("[AgentService] 文件下载失败, url={}, localPath={}", url, localPath, e);
            return url; // 下载失败时返回原始URL作为降级
        }
        return localPath;
    }

    /**
     * 将TTS音频字节保存到本地存储
     */
    private String saveAudioToLocalStorage(byte[] audioBytes, Long taskId, Integer index) {
        String dir = storageDir + File.separator + "tts" + File.separator + "task_" + taskId;
        FileUtil.mkdir(dir);
        String localPath = dir + File.separator + "tts_" + index + ".mp3";
        FileUtil.writeBytes(audioBytes, localPath);
        log.info("[AgentService] TTS音频保存成功, localPath={}, size={}bytes", localPath, audioBytes.length);
        return localPath;
    }

    /**
     * 根据URL确定文件扩展名
     */
    private String determineFileExtension(String url) {
        if (StrUtil.isBlank(url)) return ".png";
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".png")) return ".png";
        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) return ".jpg";
        if (lowerUrl.contains(".webp")) return ".webp";
        if (lowerUrl.contains(".mp4")) return ".mp4";
        if (lowerUrl.contains(".mp3")) return ".mp3";
        if (lowerUrl.contains(".wav")) return ".wav";
        return ".mp4";  // 默认视频格式
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // ==================== 异步任务方法 ====================

    /**
     * 异步生成漫画图片（后台线程执行，结果写入Redis）
     * <p>
     * 解决HTTP长连接超时问题：前端提交任务后立即返回，
     * 通过 /agent/enhance/comic/status 轮询获取结果。
     */
    @Override
    @Async("aigcTaskExecutor")
    public void generateComicImagesAsync(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        log.info("[EnhanceAgent-Comic-Async] 异步漫画生成启动, taskId={}", taskId);
        taskManager.markProcessing("comic", taskId);

        try {
            List<ComicGenResultDTO> results = generateComicImages(params);
            taskManager.saveComicResult(taskId, results);
            log.info("[EnhanceAgent-Comic-Async] 异步漫画生成完成, taskId={}, sceneCount={}",
                    taskId, results.size());
        } catch (Exception e) {
            log.error("[EnhanceAgent-Comic-Async] 异步漫画生成失败, taskId={}", taskId, e);
            taskManager.markFailed("comic", taskId, e.getMessage());
        }
    }

    /**
     * 异步TTS语音合成（后台线程执行，结果写入Redis）
     * <p>
     * 解决HTTP长连接超时问题：前端提交任务后立即返回，
     * 通过 /agent/enhance/tts/status 轮询获取结果。
     */
    @Override
    @Async("aigcTaskExecutor")
    public void generateTTSAudioAsync(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        log.info("[EnhanceAgent-TTS-Async] 异步TTS合成启动, taskId={}", taskId);
        taskManager.markProcessing("tts", taskId);

        try {
            List<TTSResultDTO> results = generateTTSAudio(params);
            taskManager.saveTTSResult(taskId, results);
            log.info("[EnhanceAgent-TTS-Async] 异步TTS合成完成, taskId={}, sceneCount={}",
                    taskId, results.size());
        } catch (Exception e) {
            log.error("[EnhanceAgent-TTS-Async] 异步TTS合成失败, taskId={}", taskId, e);
            taskManager.markFailed("tts", taskId, e.getMessage());
        }
    }

    /**
     * 异步文生视频（后台线程执行，结果写入Redis）
     * <p>
     * 解决HTTP长连接超时问题：前端提交任务后立即返回，
     * 通过 /agent/enhance/video/status 轮询获取结果。
     */
    @Override
    @Async("aigcTaskExecutor")
    public void generateVideoAsync(Map<String, Object> params) {
        Long taskId = Long.valueOf(params.get("taskId").toString());
        log.info("[EnhanceAgent-Video-Async] 异步文生视频启动, taskId={}", taskId);
        taskManager.markProcessing("video", taskId);

        try {
            List<VideoGenResultDTO> results = generateVideo(params);
            taskManager.saveVideoResult(taskId, results);
            log.info("[EnhanceAgent-Video-Async] 异步文生视频完成, taskId={}, sceneCount={}",
                    taskId, results.size());
        } catch (Exception e) {
            log.error("[EnhanceAgent-Video-Async] 异步文生视频失败, taskId={}", taskId, e);
            taskManager.markFailed("video", taskId, e.getMessage());
        }
    }

}
