package com.yf.core.resource_container.scanned_annotation;

import java.lang.annotation.*;

/**
 * @author yyf
 * @description 调度策略资源注解
 *
 * 用于标识自定义入队/出队/移除策略，注册到SPResourceManager
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SPResource {
    String value();
}
