package com.mj.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口返回值不进行统一响应包装
 * <p>
 * 用于 SSE 流式接口等场景，跳过 {@link com.mj.common.domain.Result} 自动包装。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoWrapper {
}
