package com.yf.springboot_integration.pool.annotation;

import com.yf.springboot_integration.pool.condition.RejectStrategyCondition;
import org.springframework.context.annotation.Conditional;
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
@Conditional(RejectStrategyCondition.class)  // 拒绝策略专属条件判断
public @interface RejectStrategyBean {
    String value();//拒绝策略名称
}
