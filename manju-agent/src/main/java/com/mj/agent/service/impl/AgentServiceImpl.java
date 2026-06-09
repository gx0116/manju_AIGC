package com.mj.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.agent.config.SystemPromptConfig;
import com.mj.agent.service.AgentService;
import com.mj.common.domain.ComicGenResultDTO;
import com.mj.common.domain.StoryboardDTO;
import com.mj.common.domain.TTSResultDTO;
import com.mj.common.enums.ArtStyleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final SystemPromptConfig systemPromptConfig;

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
        // 根据画风选择不同的分镜Prompt模板
        String basePrompt = """
                你是一位专业的漫剧分镜师。请根据用户提供的剧情描述，生成标准化的漫剧分镜脚本。
                
                输出要求：
                1. 返回严格的JSON格式，不要包含任何markdown标记
                2. 分镜数量控制在6-12个镜头之间
                3. 每个镜头必须包含：序号(index)、标题(title)、场景描述(description)、
                   对白/旁白(dialogue)、画面描述(imagePrompt)、镜头类型(cameraType)、
                   氛围(atmosphere)、时长(duration秒)
                4. imagePrompt需要详细描述画面内容，用于后续AI文生图
                5. cameraType可选值：近景/中景/远景/特写/全景
                6. 对白需要自然流畅，符合角色性格
                
                JSON格式示例：
                {
                  "sceneCount": 8,
                  "scenes": [
                    {
                      "index": 1,
                      "title": "开场",
                      "description": "阳光明媚的校园...",
                      "dialogue": "今天真是个美好的早晨！",
                      "imagePrompt": "二次元风格，阳光明媚的校园场景...",
                      "cameraType": "全景",
                      "atmosphere": "温暖明亮",
                      "duration": 5.0
                    }
                  ]
                }
                """;

        // 根据画风追加特定指令
        String styleInstruction = switch (promptKey) {
            case "anime_style" -> "请使用二次元动漫风格的分镜语言，注重夸张的表情和动作表现。";
            case "realistic_style" -> "请使用写实风格的分镜语言，注重光影和真实感。";
            case "chinese_style" -> "请使用中国风/国风的分镜语言，融入水墨意境和东方美学。";
            case "cartoon_style" -> "请使用卡通风格的分镜语言，色彩明亮，构图简洁。";
            default -> "请使用通用的分镜语言。";
        };

        return basePrompt + "\n" + styleInstruction;
    }

    /**
     * 构建分镜生成的User Input
     */
    private String buildStoryboardUserInput(String title, String description,
                                             String mainCharacters, String style) {
        return String.format("""
                请为以下漫剧生成分镜脚本：
                
                标题：%s
                剧情描述：%s
                主要角色：%s
                画风：%s
                
                请严格按照JSON格式输出分镜结果。
                """, title, description, mainCharacters != null ? mainCharacters : "未指定", style);
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
     * 实际项目中调用Liblib API / DashScope 图像生成 / Stable Diffusion等
     */
    private ComicGenResultDTO generateSingleComicImage(Long taskId, Integer index,
                                                        String imagePrompt, String style, String promptKey) {
        // TODO: 替换为实际的图像生成API调用
        // 示例：DashScope图像生成
        // ImageResponse response = dashScopeImageService.generate(stylePrompt, imagePrompt);

        // 当前返回模拟数据（开发阶段占位）
        log.info("[EnhanceAgent-Comic] 调用图像生成API, taskId={}, index={}, promptKey={}",
                taskId, index, promptKey);

        // 构建增强的imagePrompt（加入画风修饰）
        String enhancedPrompt = enhanceImagePrompt(imagePrompt, style, promptKey);

        // 模拟图像生成结果
        return ComicGenResultDTO.builder()
                .taskId(taskId)
                .sceneIndex(index)
                .imageUrl(String.format("https://cdn.example.com/comic/%d/scene_%d.png", taskId, index))
                .width(1920)
                .height(1080)
                .costTime(3000L)
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
     * 调用TTS服务合成单段语音
     * <p>
     * 实际项目中调用Azure TTS / DashScope TTS / 其他TTS服务
     */
    private TTSResultDTO synthesizeSpeech(Long taskId, Integer index, String dialogue) {
        // TODO: 替换为实际的TTS API调用
        // 示例：Azure TTS
        // AudioData audio = azureTTSService.synthesize(dialogue, voiceName="zh-CN-XiaoxiaoNeural");

        log.info("[EnhanceAgent-TTS] 调用TTS API, taskId={}, index={}, textLen={}",
                taskId, index, dialogue.length());

        // 模拟TTS结果
        double estimatedDuration = dialogue.length() * 0.25; // 估算：每个字约0.25秒

        return TTSResultDTO.builder()
                .taskId(taskId)
                .sceneIndex(index)
                .audioUrl(String.format("https://cdn.example.com/tts/%d/scene_%d.mp3", taskId, index))
                .duration(estimatedDuration)
                .costTime(1500L)
                .build();
    }
}