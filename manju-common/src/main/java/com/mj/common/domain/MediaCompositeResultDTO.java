package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频合成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCompositeResultDTO {

    /** 任务ID */
    private Long taskId;

    /** 合成后的视频URL */
    private String videoUrl;

    /** 视频时长（秒） */
    private Double duration;

    /** 视频大小（字节） */
    private Long fileSize;

    /** 是否成功 */
    private Boolean success;

    /** 错误信息 */
    private String errorMsg;
}