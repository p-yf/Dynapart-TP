package com.yf.core.resource_container.scanned_annotation;

import java.lang.annotation.*;

/**
 * @author yyf
 * @description 分区队列资源注解
 *
 * 用于标识自定义分区队列实现类，注册到PartiResourceManager
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PartiResource {
    String value();// 队列名称
}
