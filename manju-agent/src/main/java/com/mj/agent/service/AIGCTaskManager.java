package com.mj.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.common.domain.ComicGenResultDTO;
import com.mj.common.domain.TTSResultDTO;
import com.mj.common.domain.VideoGenResultDTO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AIGC 异步任务管理器
 * <p>
 * 使用 Redis 存储长耗时任务（漫画生成/TTS合成）的状态和结果，
 * 支持"提交→立即返回→后台处理→轮询获取结果"的异步模式，
 * 解决 HTTP 客户端因等待时间过长而超时断开的问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIGCTaskManager {

    private static final String KEY_PREFIX = "AIGC:TASK:";
    private static final long TTL_HOURS = 24;  // 结果保留24小时

    private final ObjectMapper objectMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 状态读写 ====================

    /**
     * 设置任务为处理中
     */
    public void markProcessing(String taskType, Long taskId) {
        String key = buildKey(taskType, taskId);
        stringRedisTemplate.opsForHash().put(key, "status", "PROCESSING");
        stringRedisTemplate.opsForHash().put(key, "startTime", String.valueOf(System.currentTimeMillis()));
        stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 保存漫画生成成功结果
     */
    public void saveComicResult(Long taskId, List<ComicGenResultDTO> results) {
        String key = buildKey("comic", taskId);
        stringRedisTemplate.opsForHash().put(key, "status", "COMPLETED");
        stringRedisTemplate.opsForHash().put(key, "endTime", String.valueOf(System.currentTimeMillis()));
        try {
            stringRedisTemplate.opsForHash().put(key, "result", objectMapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            log.error("[AIGCTaskManager] 漫画结果序列化失败, taskId={}", taskId, e);
            stringRedisTemplate.opsForHash().put(key, "status", "FAILED");
            stringRedisTemplate.opsForHash().put(key, "error", "结果序列化失败: " + e.getMessage());
        }
        stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 保存视频生成成功结果
     */
    public void saveVideoResult(Long taskId, List<VideoGenResultDTO> results) {
        String key = buildKey("video", taskId);
        stringRedisTemplate.opsForHash().put(key, "status", "COMPLETED");
        stringRedisTemplate.opsForHash().put(key, "endTime", String.valueOf(System.currentTimeMillis()));
        try {
            stringRedisTemplate.opsForHash().put(key, "result", objectMapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            log.error("[AIGCTaskManager] 视频结果序列化失败, taskId={}", taskId, e);
            stringRedisTemplate.opsForHash().put(key, "status", "FAILED");
            stringRedisTemplate.opsForHash().put(key, "error", "结果序列化失败: " + e.getMessage());
        }
        stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 保存TTS成功结果
     */
    public void saveTTSResult(Long taskId, List<TTSResultDTO> results) {
        String key = buildKey("tts", taskId);
        stringRedisTemplate.opsForHash().put(key, "status", "COMPLETED");
        stringRedisTemplate.opsForHash().put(key, "endTime", String.valueOf(System.currentTimeMillis()));
        try {
            stringRedisTemplate.opsForHash().put(key, "result", objectMapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            log.error("[AIGCTaskManager] TTS结果序列化失败, taskId={}", taskId, e);
            stringRedisTemplate.opsForHash().put(key, "status", "FAILED");
            stringRedisTemplate.opsForHash().put(key, "error", "结果序列化失败: " + e.getMessage());
        }
        stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 保存失败信息
     */
    public void markFailed(String taskType, Long taskId, String error) {
        String key = buildKey(taskType, taskId);
        stringRedisTemplate.opsForHash().put(key, "status", "FAILED");
        stringRedisTemplate.opsForHash().put(key, "error", error);
        stringRedisTemplate.opsForHash().put(key, "endTime", String.valueOf(System.currentTimeMillis()));
        stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    // ==================== 状态查询 ====================

    /**
     * 获取任务状态（PENDING / PROCESSING / COMPLETED / FAILED / null）
     */
    public String getStatus(String taskType, Long taskId) {
        String key = buildKey(taskType, taskId);
        Object status = stringRedisTemplate.opsForHash().get(key, "status");
        return status != null ? status.toString() : null;
    }

    /**
     * 获取漫画结果
     */
    public List<ComicGenResultDTO> getComicResult(Long taskId) {
        String key = buildKey("comic", taskId);
        Object json = stringRedisTemplate.opsForHash().get(key, "result");
        if (json == null) return null;
        try {
            return objectMapper.readValue(json.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ComicGenResultDTO.class));
        } catch (JsonProcessingException e) {
            log.error("[AIGCTaskManager] 漫画结果反序列化失败, taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 获取视频结果
     */
    public List<VideoGenResultDTO> getVideoResult(Long taskId) {
        String key = buildKey("video", taskId);
        Object json = stringRedisTemplate.opsForHash().get(key, "result");
        if (json == null) return null;
        try {
            return objectMapper.readValue(json.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, VideoGenResultDTO.class));
        } catch (JsonProcessingException e) {
            log.error("[AIGCTaskManager] 视频结果反序列化失败, taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 获取TTS结果
     */
    public List<TTSResultDTO> getTTSResult(Long taskId) {
        String key = buildKey("tts", taskId);
        Object json = stringRedisTemplate.opsForHash().get(key, "result");
        if (json == null) return null;
        try {
            return objectMapper.readValue(json.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TTSResultDTO.class));
        } catch (JsonProcessingException e) {
            log.error("[AIGCTaskManager] TTS结果反序列化失败, taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 获取错误信息
     */
    public String getError(String taskType, Long taskId) {
        String key = buildKey(taskType, taskId);
        Object error = stringRedisTemplate.opsForHash().get(key, "error");
        return error != null ? error.toString() : null;
    }

    // ==================== 辅助方法 ====================

    private String buildKey(String taskType, Long taskId) {
        return KEY_PREFIX + taskType.toUpperCase() + ":" + taskId;
    }
}
