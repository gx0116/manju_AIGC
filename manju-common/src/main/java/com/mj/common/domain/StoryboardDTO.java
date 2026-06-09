package com.mj.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分镜结果DTO - ScriptAgent生成标准化分镜
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardDTO {

    /** 任务ID */
    private Long taskId;

    /** 总镜头数 */
    private Integer sceneCount;

    /** 分镜场景列表 */
    private List<StoryboardScene> scenes;

    /**
     * 单个分镜场景
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryboardScene {

        /** 镜头序号 1~N */
        private Integer index;

        /** 场景标题 */
        private String title;

        /** 场景描述/剧情 */
        private String description;

        /** 角色对白/旁白文本 */
        private String dialogue;

        /** 画面描述（用于文生图） */
        private String imagePrompt;

        /** 镜头类型：近景/中景/远景/特写 */
        private String cameraType;

        /** 情绪/氛围 */
        private String atmosphere;

        /** 预估时长（秒） */
        private Double duration;
    }
}