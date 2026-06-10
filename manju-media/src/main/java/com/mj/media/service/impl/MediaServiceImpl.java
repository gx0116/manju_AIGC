package com.mj.media.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.mj.common.domain.MediaCompositeDTO;
import com.mj.common.domain.MediaCompositeResultDTO;
import com.mj.media.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    /** CDN基础URL（或本地静态资源路径，以 file: 开头表示本地路径） */
    @Value("${mj.media.cdn-base-url:https://cdn.example.com}")
    private String cdnBaseUrl;

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

        try {
            // 1. 下载图片和音频到本地
            String imgExt = getExtension(item.getImageUrl(), ".png");
            String audioExt = getExtension(item.getAudioUrl(), ".mp3");
            String localImage = workDir + File.separator + "scene_" + index + imgExt;
            String localAudio = workDir + File.separator + "scene_" + index + audioExt;

            transferFile(item.getImageUrl(), localImage);
            transferFile(item.getAudioUrl(), localAudio);
            log.info("[MediaService] 镜头{}文件下载完成, image={}, audio={}", index, localImage, localAudio);

            // 2. FFmpeg: 图片+音频 → 视频片段
            double duration = item.getDuration() != null ? item.getDuration() : 5.0;
            String segmentPath = workDir + File.separator + "segment_" + index + ".mp4";

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-loop"); cmd.add("1");
            cmd.add("-i"); cmd.add(localImage);
            cmd.add("-i"); cmd.add(localAudio);
            cmd.add("-c:v"); cmd.add("libx264");
            cmd.add("-t"); cmd.add(String.format("%.1f", duration));
            cmd.add("-pix_fmt"); cmd.add("yuv420p");
            cmd.add("-vf"); cmd.add(String.format("scale=%d:%d", videoWidth, videoHeight));
            cmd.add("-r"); cmd.add(String.valueOf(videoFps));
            cmd.add("-y");
            cmd.add(segmentPath);

            executeFFmpegCommand(cmd);

            log.info("[MediaService] 镜头{}处理完成, taskId={}, segment={}", index, taskId, segmentPath);
        } catch (Exception e) {
            log.error("[MediaService] 镜头{}处理失败, taskId={}", index, taskId, e);
            throw new RuntimeException("镜头" + index + "处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 合并所有视频片段（使用 FFmpeg concat demuxer）
     */
    private void concatSegments(String workDir, String outputPath) {
        log.info("[MediaService] 合并视频片段 → {}", outputPath);

        // 1. 查找所有segment文件并按文件名排序
        List<File> segmentFiles = FileUtil.loopFiles(workDir, file ->
                file.getName().startsWith("segment_") && file.getName().endsWith(".mp4"));
        if (segmentFiles.isEmpty()) {
            throw new RuntimeException("未找到任何视频片段进行合并");
        }
        segmentFiles.sort(Comparator.comparing(File::getName));
        log.info("[MediaService] 找到 {} 个视频片段", segmentFiles.size());

        // 2. 创建concat文件列表（FFmpeg concat demuxer格式）
        String listFilePath = workDir + File.separator + "concat_list.txt";
        List<String> lines = segmentFiles.stream()
                .map(f -> "file '" + f.getAbsolutePath().replace("\\", "/").replace("'", "'\\''") + "'")
                .collect(Collectors.toList());
        FileUtil.writeUtf8Lines(lines, listFilePath);
        log.info("[MediaService] concat列表已创建: {}", listFilePath);

        // 3. FFmpeg concat合并
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-f"); cmd.add("concat");
        cmd.add("-safe"); cmd.add("0");
        cmd.add("-i"); cmd.add(listFilePath);
        cmd.add("-c"); cmd.add("copy");
        cmd.add("-y");
        cmd.add(outputPath);

        executeFFmpegCommand(cmd);
        log.info("[MediaService] 视频合并完成 → {}", outputPath);
    }

    /**
     * 上传视频到CDN/OSS
     * <p>
     * 支持两种模式：
     * - 本地模式（cdn-base-url 以 file: 开头）：复制到本地目录
     * - 远程CDN模式：返回CDN URL（需集成对应OSS SDK）
     */
    private String uploadToCDN(String localPath, Long taskId) {
        String remotePath = "manju-video/" + taskId + "/output.mp4";

        // 本地模式：复制文件到指定目录
        if (cdnBaseUrl.startsWith("file:")) {
            String localDir = cdnBaseUrl.substring(5);
            FileUtil.mkdir(localDir + File.separator + "manju-video" + File.separator + taskId);
            String destPath = localDir + File.separator + remotePath.replace("/", File.separator);
            FileUtil.copy(new File(localPath), new File(destPath), true);
            log.info("[MediaService] 视频已复制到本地: {}", destPath);
            return destPath;
        }

        // TODO: 集成实际OSS/CDN SDK（如阿里云OSS、腾讯云COS、MinIO）
        // 例如：ossClient.putObject("bucket-name", remotePath, new File(localPath));

        String videoUrl = cdnBaseUrl + "/" + remotePath;
        log.info("[MediaService] 视频CDN地址: {}", videoUrl);
        return videoUrl;
    }

    // ==================== 辅助方法 ====================

    /**
     * 传输文件到本地工作目录
     * <p>
     * 支持两种模式：
     * - HTTP/HTTPS URL：通过 HTTP 下载（禁用系统代理）
     * - 本地路径：直接复制文件
     *
     * @param sourceUrl 源文件URL或本地路径
     * @param destPath  本地目标路径
     */
    private void transferFile(String sourceUrl, String destPath) {
        if (sourceUrl == null) {
            throw new RuntimeException("文件源路径为空");
        }
        // 统一路径分隔符（兼容Windows混合斜杠路径）
        String normalizedSource = sourceUrl.replace("\\", "/");

        if (StrUtil.startWithIgnoreCase(normalizedSource, "http://")
                || StrUtil.startWithIgnoreCase(normalizedSource, "https://")) {
            // HTTP模式：下载文件（禁用系统代理避免代理不可达问题）
            log.info("[MediaService] HTTP下载文件: {} → {}", sourceUrl, destPath);
            HttpUtil.createGet(normalizedSource).setProxy(Proxy.NO_PROXY).execute().writeBody(FileUtil.file(destPath));
        } else {
            // 本地模式：直接复制文件
            File srcFile = new File(normalizedSource);
            if (!srcFile.exists()) {
                throw new RuntimeException("本地文件不存在: " + normalizedSource);
            }
            log.info("[MediaService] 复制本地文件: {} → {}", normalizedSource, destPath);
            FileUtil.copy(srcFile, new File(destPath), true);
        }
    }

    /**
     * 执行FFmpeg命令
     *
     * @param command 命令及参数列表（每个元素独立，支持含空格的路径）
     */
    private void executeFFmpegCommand(List<String> command) {
        log.info("[MediaService] 执行FFmpeg命令: {}", String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[FFmpeg] {}", line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg执行失败，退出码: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("FFmpeg执行被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("FFmpeg执行异常: " + e.getMessage(), e);
        }
    }

    /**
     * 从URL中提取文件扩展名
     *
     * @param url        文件URL
     * @param defaultExt 默认扩展名（无法识别时返回）
     * @return 扩展名，包含点号，如 ".png"
     */
    private String getExtension(String url, String defaultExt) {
        if (url == null || !url.contains(".")) {
            return defaultExt;
        }
        String ext = url.substring(url.lastIndexOf("."));
        // 去掉URL查询参数
        if (ext.contains("?")) {
            ext = ext.substring(0, ext.indexOf("?"));
        }
        return ext;
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