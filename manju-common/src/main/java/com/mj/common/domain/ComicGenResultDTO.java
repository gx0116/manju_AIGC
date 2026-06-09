package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 漫画图片生成结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicGenResultDTO {

    /** 任务ID */
    private Long taskId;

    /** 镜头序号 */
    private Integer sceneIndex;

    /** 生成的漫画图片URL */
    private String imageUrl;

    /** 图片宽度 */
    private Integer width;

    /** 图片高度 */
    private Integer height;

    /** 生成耗时（毫秒） */
    private Long costTime;
}