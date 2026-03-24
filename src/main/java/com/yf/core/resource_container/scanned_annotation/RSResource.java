package com.yf.core.resource_container.scanned_annotation;

import java.lang.annotation.*;

/**
 * @author yyf
 * @description 拒绝策略资源注解
 *
 * 用于标识自定义拒绝策略，注册到RSResourceManager
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RSResource {
    String value();//拒绝策略名称
}
