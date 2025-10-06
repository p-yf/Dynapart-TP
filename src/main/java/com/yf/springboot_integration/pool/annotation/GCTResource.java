package com.yf.springboot_integration.pool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yyf
 * @date 2025/10/6 17:18
 * @description: GCTaskResource:用来标识
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GCTResource {
    String value();//绑定的队列或者调度规则的名称
}
