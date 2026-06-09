package com.mj.common.enums;

import lombok.Getter;

/**
 * 漫剧任务状态枚举
 */
@Getter
public enum TaskStatusEnum implements BaseEnum {
    PENDING(0, "待处理"),
    SCRIPT_GENERATING(10, "分镜生成中"),
    SCRIPT_COMPLETED(15, "分镜完成"),
    ENHANCING(20, "增强生成中(漫画+TTS)"),
    ENHANCE_COMPLETED(25, "增强生成完成"),
    COMPOSITING(30, "视频合成中"),
    COMPLETED(40, "已完成"),
    FAILED(-1, "失败");

    private final int value;
    private final String desc;

    TaskStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return this.name();
    }
}