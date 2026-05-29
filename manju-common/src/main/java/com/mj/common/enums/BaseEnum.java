package com.mj.common.enums;

/**
 * 枚举基类接口
 * <p>
 * 所有业务枚举实现此接口，统一提供 {@code value}（数值标识）和 {@code desc}（描述）的访问方法。
 * </p>
 */
public interface BaseEnum {

    /**
     * 获取枚举数值标识
     */
    int getValue();

    /**
     * 获取枚举描述
     */
    String getDesc();
}
