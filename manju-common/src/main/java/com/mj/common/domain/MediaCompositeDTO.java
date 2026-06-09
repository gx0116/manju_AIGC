package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 媒体合成请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCompositeDTO {

    /** 任务ID */
    private Long taskId;

    /** 漫剧标题（用于输出文件名） */
    private String title;

    /** 画风 */
    private String style;

    /** 分镜合成列表（按镜头顺序排列） */
    private List<SceneCompositeItem> items;

    /**
     * 单个镜头的合成数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneCompositeItem {

        /** 镜头序号 */
        private Integer index;

        /** 漫画图片URL */
        private String imageUrl;

        /** 对白音频URL */
        private String audioUrl;

        /** 该镜头时长（秒） */
        private Double duration;
    }
}