package com.mj.media.service;

import com.mj.common.domain.MediaCompositeDTO;
import com.mj.common.domain.MediaCompositeResultDTO;

/**
 * 媒体处理服务 - FFmpeg视频合成
 */
public interface MediaService {

    /**
     * FFmpeg合成漫剧视频
     *
     * @param request 合成请求（图片+音频列表）
     * @return 合成结果
     */
    MediaCompositeResultDTO compositeVideo(MediaCompositeDTO request);

    /**
     * 获取合成进度
     */
    Integer getCompositeProgress(Long taskId);
}