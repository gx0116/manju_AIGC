package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文生视频生成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenResultDTO {

    /** 任务ID */
    private Long taskId;

    /** 镜头序号 */
    private Integer sceneIndex;

    /** 生成的视频文件URL（本地路径或CDN地址） */
    private String videoUrl;

    /** 视频时长（秒） */
    private Integer duration;

    /** 视频宽度 */
    private Integer width;

    /** 视频高度 */
    private Integer height;

    /** 生成耗时（毫秒） */
    private Long costTime;
}
