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
public @interface RejectStrategyBean {
    String value();//拒绝策略名称
}
