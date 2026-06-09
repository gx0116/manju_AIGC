package com.mj.media.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.mj.common.domain.MediaCompositeDTO;
import com.mj.common.domain.MediaCompositeResultDTO;
import com.mj.media.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体处理服务实现 - 基于FFmpeg进行视频合成
 * <p>
 * 合成流程：
 * 1. 下载每个镜头的图片和音频
 * 2. 逐镜头使用FFmpeg将图片+音频合成为片段
 * 3. 使用FFmpeg concat合并所有片段
 * 4. 添加背景音乐（可选）
 * 5. 输出最终视频文件
 */
@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    /** 工作目录 */
    @Value("${mj.media.work-dir:/tmp/manju-media}")
    private String workDir;

    /** FFmpeg可执行文件路径 */
    @Value("${mj.media.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    /** 输出视频宽度 */
    @Value("${mj.media.video-width:1920}")
    private int videoWidth;

    /** 输出视频高度 */
    @Value("${mj.media.video-height:1080}")
    private int videoHeight;

    /** 输出视频帧率 */
    @Value("${mj.media.video-fps:24}")
    private int videoFps;

    /** 合成进度缓存 */
    private final Map<Long, Integer> progressCache = new ConcurrentHashMap<>();

    @Override
    public MediaCompositeResultDTO compositeVideo(MediaCompositeDTO request) {
        Long taskId = request.getTaskId();
        log.info("[MediaService] 开始视频合成, taskId={}, title={}, style={}",
                taskId, request.getTitle(), request.getStyle());

        try {
            progressCache.put(taskId, 0);

            // 1. 创建工作目录
            String taskWorkDir = workDir + File.separator + "task_" + taskId;
            FileUtil.mkdir(taskWorkDir);
            log.info("[MediaService] 工作目录创建: {}", taskWorkDir);

            progressCache.put(taskId, 10);

            // 2. 处理每个镜头：图片+音频→视频片段
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                int totalItems = request.getItems().size();
                for (int i = 0; i < totalItems; i++) {
                    MediaCompositeDTO.SceneCompositeItem item = request.getItems().get(i);
                    processSceneSegment(taskId, taskWorkDir, item, i + 1, totalItems);
                    progressCache.put(taskId, 10 + (int) ((i + 1) * 60.0 / totalItems));
                }
            }

            progressCache.put(taskId, 70);

            // 3. 合并所有片段为最终视频
            String outputFileName = buildOutputFileName(request.getTitle(), taskId);
            String outputPath = taskWorkDir + File.separator + outputFileName;
            concatSegments(taskWorkDir, outputPath);

            progressCache.put(taskId, 90);

            // 4. 上传到CDN/OSS（模拟）
            String videoUrl = uploadToCDN(outputPath, taskId);

            progressCache.put(taskId, 100);

            log.info("[MediaService] 视频合成完成, taskId={}, url={}", taskId, videoUrl);

            return MediaCompositeResultDTO.builder()
                    .taskId(taskId)
                    .videoUrl(videoUrl)
                    .duration(calculateTotalDuration(request))
                    .fileSize(FileUtil.size(new File(outputPath)))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[MediaService] 视频合成失败, taskId={}", taskId, e);
            progressCache.remove(taskId);
            return MediaCompositeResultDTO.builder()
                    .taskId(taskId)
                    .success(false)
                    .errorMsg("视频合成失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public Integer getCompositeProgress(Long taskId) {
        return progressCache.getOrDefault(taskId, 0);
    }

    // ==================== 私有方法 ====================

    /**
     * 处理单个镜头：图片+音频 → 视频片段
     */
    private void processSceneSegment(Long taskId, String workDir,
                                      MediaCompositeDTO.SceneCompositeItem item,
                                      int index, int total) {
        log.info("[MediaService] 处理镜头 {}/{}, taskId={}, imageUrl={}, audioUrl={}",
                index, total, taskId, item.getImageUrl(), item.getAudioUrl());

        // TODO: 实际的FFmpeg命令执行
        // 1. 下载图片和音频到本地
        // String localImage = downloadFile(item.getImageUrl(), workDir + "/scene_" + index + ".png");
        // String localAudio = downloadFile(item.getAudioUrl(), workDir + "/scene_" + index + ".mp3");

        // 2. FFmpeg: 图片+音频 → 视频片段
        // ffmpeg -loop 1 -i scene_1.png -i scene_1.mp3 -c:v libx264 -t {duration}
        //        -pix_fmt yuv420p -vf "scale=1920:1080" -r 24 scene_1.mp4
        //
        // String segmentPath = workDir + "/segment_" + index + ".mp4";
        // String cmd = String.format(
        //     "%s -loop 1 -i %s -i %s -c:v libx264 -t %.1f -pix_fmt yuv420p " +
        //     "-vf \"scale=%d:%d\" -r %d -y %s",
        //     ffmpegPath, localImage, localAudio, item.getDuration(),
        //     videoWidth, videoHeight, videoFps, segmentPath
        // );
        // executeFFmpegCommand(cmd);

        log.info("[MediaService] 镜头{}处理完成 (模拟), taskId={}", index, taskId);
    }

    /**
     * 合并所有视频片段
     */
    private void concatSegments(String workDir, String outputPath) {
        log.info("[MediaService] 合并视频片段 → {}", outputPath);

        // TODO: 实际FFmpeg执行
        // 1. 创建文件列表 concat_list.txt
        // 2. FFmpeg concat:
        //    ffmpeg -f concat -safe 0 -i concat_list.txt -c copy -y output.mp4

        log.info("[MediaService] 视频合并完成 (模拟)");
    }

    /**
     * 上传视频到CDN/OSS
     */
    private String uploadToCDN(String localPath, Long taskId) {
        // TODO: 实际OSS/CDN上传逻辑
        // OSSClient.upload(localPath, "manju-video/" + taskId + "/output.mp4");
        return "https://cdn.example.com/manju-video/" + taskId + "/output.mp4";
    }

    /**
     * 构建输出文件名
     */
    private String buildOutputFileName(String title, Long taskId) {
        String safeTitle = title != null ? title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") : "output";
        return safeTitle + "_" + taskId + ".mp4";
    }

    /**
     * 计算总时长
     */
    private double calculateTotalDuration(MediaCompositeDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return 0;
        }
        return request.getItems().stream()
                .mapToDouble(item -> item.getDuration() != null ? item.getDuration() : 5.0)
                .sum();
    }
}