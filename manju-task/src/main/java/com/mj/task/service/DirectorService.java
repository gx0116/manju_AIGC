package com.mj.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.api.client.AgentClient;
import com.mj.api.client.MediaClient;
import com.mj.api.client.WorkClient;
import com.mj.common.domain.*;
import com.mj.common.enums.ArtStyleEnum;
import com.mj.common.enums.TaskStatusEnum;
import com.mj.task.config.RabbitMQConfig;
import com.mj.task.domain.po.AiComicTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Director 协调器 - 漫剧生成流程编排
 * <p>
 * 流程：
 * 1. 【路由】根据画风路由不同Prompt配置
 * 2. 【链式】串行调用ScriptAgent生成标准化分镜
 * 3. 【并行】分镜完成后：
 *    - 增强Agent：Liblib文生漫画
 *    - 增强Agent：Azure TTS语音合成
 * 4. Feign调用manju-media FFmpeg合成漫剧视频
 * 5. manju-work作品入库更新任务状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {

    private final ITaskService taskService;
    private final AgentClient agentClient;
    private final MediaClient mediaClient;
    private final WorkClient workClient;
    private final ObjectMapper objectMapper;

    /**
     * 监听MQ消息，启动编排流程
     */
    @RabbitListener(queues = RabbitMQConfig.DIRECTOR_QUEUE)
    public void onTaskReceived(Map<String, Object> message) {
        Long taskId = Long.valueOf(message.get("taskId").toString());
        log.info("[Director] 收到任务消息, taskId={}", taskId);

        try {
            orchestrate(message);
        } catch (Exception e) {
            log.error("[Director] 任务编排异常, taskId={}", taskId, e);
            taskService.updateTaskStatus(taskId, TaskStatusEnum.FAILED.getValue(), e.getMessage());
        }
    }

    /**
     * 编排核心流程
     */
    private void orchestrate(Map<String, Object> message) {
        Long taskId = Long.valueOf(message.get("taskId").toString());
        String title = (String) message.get("title");
        String description = (String) message.get("description");
        Integer style = (Integer) message.get("style");
        Integer contentType = (Integer) message.get("contentType");
        String mainCharacters = (String) message.get("mainCharacters");
        // userId 可能为 null（未登录/测试场景），安全转换
        Long userId = cn.hutool.core.convert.Convert.toLong(message.get("userId"), 0L);

        ArtStyleEnum artStyle = ArtStyleEnum.fromValue(style);
        log.info("[Director] 开始编排, taskId={}, style={}", taskId, artStyle.getDesc());

        // ========== Step 1: 路由 ==========
        // 根据画风选择不同的Prompt配置
        String promptConfig = routePromptConfig(artStyle);
        log.info("[Director] 路由Prompt配置, taskId={}, promptKey={}", taskId, promptConfig);

        // ========== Step 2: 链式串行 - ScriptAgent生成分镜 ==========
        taskService.updateTaskStatus(taskId, TaskStatusEnum.SCRIPT_GENERATING.getValue(), null);
        taskService.updateProgress(taskId, 10);

        StoryboardDTO storyboard = generateStoryboard(taskId, title, description, artStyle, mainCharacters, promptConfig);
        if (storyboard == null || storyboard.getScenes() == null || storyboard.getScenes().isEmpty()) {
            throw new RuntimeException("分镜生成失败，无有效场景");
        }
        log.info("[Director] 分镜生成完成, taskId={}, sceneCount={}", taskId, storyboard.getSceneCount());

        // 保存分镜JSON到任务表
        saveStoryboard(taskId, storyboard);
        taskService.updateTaskStatus(taskId, TaskStatusEnum.SCRIPT_COMPLETED.getValue(), null);
        taskService.updateProgress(taskId, 30);

        // ========== Step 3: 并行 - 增强Agent ==========
        taskService.updateTaskStatus(taskId, TaskStatusEnum.ENHANCING.getValue(), null);

        List<StoryboardDTO.StoryboardScene> scenes = storyboard.getScenes();

        // 并行执行：漫画生成 + TTS语音合成
        CompletableFuture<List<ComicGenResultDTO>> comicFuture =
                CompletableFuture.supplyAsync(() -> generateComicImages(taskId, scenes, artStyle, promptConfig));

        CompletableFuture<List<TTSResultDTO>> ttsFuture =
                CompletableFuture.supplyAsync(() -> generateTTSAudio(taskId, scenes));

        // 等待两个并行任务完成
        List<ComicGenResultDTO> comicResults;
        List<TTSResultDTO> ttsResults;
        try {
            comicResults = comicFuture.get();
            ttsResults = ttsFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("并行增强生成失败: " + e.getMessage(), e);
        }

        log.info("[Director] 增强生成完成, taskId={}, comicCount={}, ttsCount={}",
                taskId, comicResults.size(), ttsResults.size());

        // 保存增强结果JSON
        saveEnhanceResults(taskId, comicResults, ttsResults);
        taskService.updateTaskStatus(taskId, TaskStatusEnum.ENHANCE_COMPLETED.getValue(), null);
        taskService.updateProgress(taskId, 60);

        // ========== Step 4: Feign调用manju-media FFmpeg合成 ==========
        taskService.updateTaskStatus(taskId, TaskStatusEnum.COMPOSITING.getValue(), null);
        taskService.updateProgress(taskId, 70);

        MediaCompositeResultDTO compositeResult = compositeVideo(taskId, title, artStyle, scenes, comicResults, ttsResults);
        if (compositeResult == null || !Boolean.TRUE.equals(compositeResult.getSuccess())) {
            throw new RuntimeException("视频合成失败: " +
                    (compositeResult != null ? compositeResult.getErrorMsg() : "未知错误"));
        }
        log.info("[Director] 视频合成完成, taskId={}, videoUrl={}", taskId, compositeResult.getVideoUrl());

        // 保存视频URL
        saveVideoUrl(taskId, compositeResult.getVideoUrl());
        taskService.updateProgress(taskId, 90);

        // ========== Step 5: manju-work作品入库&更新状态 ==========
        // 使用第一张漫画图作为封面
        String coverUrl = comicResults.isEmpty() ? null : comicResults.get(0).getImageUrl();
        Long workId = saveToWorkService(taskId, title, description, userId, artStyle,
                compositeResult.getVideoUrl(), coverUrl, mainCharacters, contentType);

        // 更新任务最终状态
        AiComicTask task = new AiComicTask();
        task.setId(taskId);
        task.setWorkId(workId);
        task.setCoverUrl(coverUrl);
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getValue());
        task.setProgress(100);
        task.setUpdateTime(LocalDateTime.now());
        taskService.updateById(task);

        log.info("[Director] 任务完成! taskId={}, workId={}", taskId, workId);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据画风路由Prompt配置
     */
    private String routePromptConfig(ArtStyleEnum artStyle) {
        // 不同画风对应不同的Prompt模板key
        return artStyle.getPromptKey();
    }

    /**
     * 调用ScriptAgent生成标准化分镜
     */
    private StoryboardDTO generateStoryboard(Long taskId, String title, String description,
                                              ArtStyleEnum artStyle, String mainCharacters, String promptConfig) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("title", title);
        params.put("description", description);
        params.put("style", artStyle.getDesc());
        params.put("styleCode", artStyle.getValue());
        params.put("promptKey", promptConfig);
        params.put("mainCharacters", mainCharacters);
        params.put("type", "storyboard");

        Result<StoryboardDTO> result = agentClient.generateStoryboard(params);
        if (result == null || result.getCode() != 200) {
            throw new RuntimeException("ScriptAgent分镜生成失败: " + (result != null ? result.getMsg() : "无响应"));
        }
        return result.getData();
    }

    /**
     * 调用增强Agent - 文生漫画（异步submit + 轮询）
     */
    @SuppressWarnings("unchecked")
    private List<ComicGenResultDTO> generateComicImages(Long taskId,
                                                         List<StoryboardDTO.StoryboardScene> scenes,
                                                         ArtStyleEnum artStyle, String promptConfig) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("style", artStyle.getDesc());
        params.put("styleCode", artStyle.getValue());
        params.put("promptKey", promptConfig);
        params.put("type", "comic_gen");
        params.put("scenes", scenes);

        // 1. 提交异步任务
        Result<Map<String, Object>> submitResult = agentClient.submitComic(params);
        if (submitResult == null || submitResult.getCode() != 200) {
            throw new RuntimeException("提交漫画生成任务失败: " + (submitResult != null ? submitResult.getMsg() : "无响应"));
        }
        log.info("[Director] 漫画任务已提交, taskId={}", taskId);

        // 2. 轮询直到完成
        return pollComicResult(taskId);
    }

    /**
     * 轮询漫画生成结果
     */
    @SuppressWarnings("unchecked")
    private List<ComicGenResultDTO> pollComicResult(Long taskId) {
        int maxRetries = 300; // 最多轮询300次（300 * 3秒 = 15分钟）
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("轮询被中断", e);
            }

            Result<Map<String, Object>> statusResult = agentClient.getComicStatus(taskId);
            if (statusResult == null || statusResult.getCode() != 200 || statusResult.getData() == null) {
                log.warn("[Director] 漫画状态查询异常, taskId={}, retry={}", taskId, i);
                continue;
            }

            String status = (String) statusResult.getData().get("status");
            log.info("[Director] 漫画状态轮询, taskId={}, status={}, retry={}", taskId, status, i);

            if ("COMPLETED".equals(status)) {
                Object data = statusResult.getData().get("data");
                // 统一使用 ObjectMapper 转换，避免 ClassCastException
                List<ComicGenResultDTO> results = objectMapper.convertValue(data,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ComicGenResultDTO.class));
                log.info("[Director] 漫画结果转换成功, taskId={}, count={}", taskId, results.size());
                return results;
            } else if ("FAILED".equals(status)) {
                String error = (String) statusResult.getData().get("error");
                throw new RuntimeException("漫画生成失败: " + error);
            }
            // PROCESSING 或 NOT_FOUND，继续轮询
        }
        throw new RuntimeException("漫画生成超时，taskId=" + taskId);
    }

    /**
     * 调用增强Agent - TTS语音合成（异步submit + 轮询）
     */
    @SuppressWarnings("unchecked")
    private List<TTSResultDTO> generateTTSAudio(Long taskId,
                                                 List<StoryboardDTO.StoryboardScene> scenes) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("type", "tts");
        params.put("scenes", scenes);

        // 1. 提交异步任务
        Result<Map<String, Object>> submitResult = agentClient.submitTTS(params);
        if (submitResult == null || submitResult.getCode() != 200) {
            throw new RuntimeException("提交TTS合成任务失败: " + (submitResult != null ? submitResult.getMsg() : "无响应"));
        }
        log.info("[Director] TTS任务已提交, taskId={}", taskId);

        // 2. 轮询直到完成
        return pollTTSResult(taskId);
    }

    /**
     * 轮询TTS合成结果
     */
    @SuppressWarnings("unchecked")
    private List<TTSResultDTO> pollTTSResult(Long taskId) {
        int maxRetries = 300;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("轮询被中断", e);
            }

            Result<Map<String, Object>> statusResult = agentClient.getTTSStatus(taskId);
            if (statusResult == null || statusResult.getCode() != 200 || statusResult.getData() == null) {
                log.warn("[Director] TTS状态查询异常, taskId={}, retry={}", taskId, i);
                continue;
            }

            String status = (String) statusResult.getData().get("status");
            log.info("[Director] TTS状态轮询, taskId={}, status={}, retry={}", taskId, status, i);

            if ("COMPLETED".equals(status)) {
                Object data = statusResult.getData().get("data");
                // 统一使用 ObjectMapper 转换，避免 ClassCastException
                List<TTSResultDTO> results = objectMapper.convertValue(data,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, TTSResultDTO.class));
                log.info("[Director] TTS结果转换成功, taskId={}, count={}", taskId, results.size());
                return results;
            } else if ("FAILED".equals(status)) {
                String error = (String) statusResult.getData().get("error");
                throw new RuntimeException("TTS合成失败: " + error);
            }
        }
        throw new RuntimeException("TTS合成超时，taskId=" + taskId);
    }

    /**
     * 调用manju-media FFmpeg合成视频
     */
    private MediaCompositeResultDTO compositeVideo(Long taskId, String title, ArtStyleEnum artStyle,
                                                    List<StoryboardDTO.StoryboardScene> scenes,
                                                    List<ComicGenResultDTO> comicResults,
                                                    List<TTSResultDTO> ttsResults) {
        // 构建合成请求
        Map<Integer, ComicGenResultDTO> comicMap = comicResults.stream()
                .collect(Collectors.toMap(ComicGenResultDTO::getSceneIndex, c -> c, (a, b) -> a));
        Map<Integer, TTSResultDTO> ttsMap = ttsResults.stream()
                .collect(Collectors.toMap(TTSResultDTO::getSceneIndex, t -> t, (a, b) -> a));

        List<MediaCompositeDTO.SceneCompositeItem> items = scenes.stream()
                .map(scene -> {
                    ComicGenResultDTO comic = comicMap.get(scene.getIndex());
                    TTSResultDTO tts = ttsMap.get(scene.getIndex());
                    return MediaCompositeDTO.SceneCompositeItem.builder()
                            .index(scene.getIndex())
                            .imageUrl(comic != null ? comic.getImageUrl() : null)
                            .audioUrl(tts != null ? tts.getAudioUrl() : null)
                            .duration(scene.getDuration())
                            .build();
                })
                .collect(Collectors.toList());

        MediaCompositeDTO request = MediaCompositeDTO.builder()
                .taskId(taskId)
                .title(title)
                .style(artStyle.getDesc())
                .items(items)
                .build();

        Result<MediaCompositeResultDTO> result = mediaClient.compositeVideo(request);
        if (result == null || result.getCode() != 200) {
            return MediaCompositeResultDTO.builder()
                    .taskId(taskId)
                    .success(false)
                    .errorMsg(result != null ? result.getMsg() : "无响应")
                    .build();
        }
        return result.getData();
    }

    /**
     * 保存分镜JSON到DB
     */
    private void saveStoryboard(Long taskId, StoryboardDTO storyboard) {
        try {
            String json = objectMapper.writeValueAsString(storyboard);
            AiComicTask task = new AiComicTask();
            task.setId(taskId);
            task.setStoryboardJson(json);
            task.setUpdateTime(LocalDateTime.now());
            taskService.updateById(task);
        } catch (JsonProcessingException e) {
            log.error("[Director] 分镜JSON序列化失败, taskId={}", taskId, e);
        }
    }

    /**
     * 保存增强结果JSON到DB
     */
    private void saveEnhanceResults(Long taskId, List<ComicGenResultDTO> comicResults,
                                     List<TTSResultDTO> ttsResults) {
        try {
            AiComicTask task = new AiComicTask();
            task.setId(taskId);
            task.setComicImagesJson(objectMapper.writeValueAsString(comicResults));
            task.setTtsAudiosJson(objectMapper.writeValueAsString(ttsResults));
            task.setUpdateTime(LocalDateTime.now());
            taskService.updateById(task);
        } catch (JsonProcessingException e) {
            log.error("[Director] 增强结果JSON序列化失败, taskId={}", taskId, e);
        }
    }

    /**
     * 保存视频URL
     */
    private void saveVideoUrl(Long taskId, String videoUrl) {
        AiComicTask task = new AiComicTask();
        task.setId(taskId);
        task.setVideoUrl(videoUrl);
        task.setUpdateTime(LocalDateTime.now());
        taskService.updateById(task);
    }

    /**
     * 调用manju-work入库作品
     */
    private Long saveToWorkService(Long taskId, String title, String description, Long userId,
                                    ArtStyleEnum artStyle, String videoUrl, String coverUrl,
                                    String mainCharacters, Integer contentType) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("description", description);
        params.put("author", "AI漫剧");
        params.put("userId", userId);
        params.put("aiModel", "Liblib+AzureTTS");
        params.put("style", artStyle.getDesc());
        params.put("mainCharacters", mainCharacters);
        params.put("contentType", contentType != null ? contentType : 2);
        params.put("coverUrl", coverUrl);
        params.put("videoUrl", videoUrl);
        params.put("episodeCount", 1);
        params.put("generateStatus", 2);
        params.put("isPublic", 1);
        params.put("status", 1);

        Result<Long> result = workClient.saveComicDrama(params);
        if (result != null && result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        log.warn("[Director] manju-work入库失败, taskId={}", taskId);
        return null;
    }
}