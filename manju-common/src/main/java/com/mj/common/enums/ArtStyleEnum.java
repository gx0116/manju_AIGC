package com.mj.common.enums;

import lombok.Getter;

/**
 * 漫画画风枚举
 */
@Getter
public enum ArtStyleEnum implements BaseEnum {
    ACG(1, "二次元", "anime_style"),
    REALISTIC(2, "写实", "realistic_style"),
    CHINESE(3, "国风", "chinese_style"),
    CARTOON(4, "卡通", "cartoon_style");

    private final int value;
    private final String desc;
    /** 对应的Prompt配置key */
    private final String promptKey;

    ArtStyleEnum(int value, String desc, String promptKey) {
        this.value = value;
        this.desc = desc;
        this.promptKey = promptKey;
    }

    /**
     * 根据value获取枚举
     */
    public static ArtStyleEnum fromValue(int value) {
        for (ArtStyleEnum style : values()) {
            if (style.value == value) {
                return style;
            }
        }
        return ACG; // 默认二次元
    }

    @Override
    public String toString() {
        return this.name();
    }
}