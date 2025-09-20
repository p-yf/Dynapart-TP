package com.yf.springboot_integration.pool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


/**
 * @author yyf
 * @description
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PartiResource {//分区资源注解
    String value();// 队列名称
}
