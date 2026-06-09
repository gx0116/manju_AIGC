package com.mj.media.controller;

import com.mj.common.domain.MediaCompositeDTO;
import com.mj.common.domain.MediaCompositeResultDTO;
import com.mj.common.domain.Result;
import com.mj.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 媒体处理控制器 - FFmpeg合成漫剧视频
 */
@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * FFmpeg合成漫剧视频
     * 将分镜图片+对白音频合成为完整视频
     */
    @PostMapping("/composite")
    public Result<MediaCompositeResultDTO> compositeVideo(@RequestBody MediaCompositeDTO request) {
        log.info("[MediaController] 收到视频合成请求, taskId={}, itemCount={}",
                request.getTaskId(),
                request.getItems() != null ? request.getItems().size() : 0);
        MediaCompositeResultDTO result = mediaService.compositeVideo(request);
        return Result.success(result);
    }

    /**
     * 查询合成进度
     */
    @PostMapping("/composite/progress")
    public Result<Integer> getCompositeProgress(@RequestBody Long taskId) {
        Integer progress = mediaService.getCompositeProgress(taskId);
        return Result.success(progress);
    }
}