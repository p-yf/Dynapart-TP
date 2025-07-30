package com.yf.pool.springboot_integration.annotation;

import com.yf.pool.springboot_integration.condition.RejectStrategyCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Conditional(RejectStrategyCondition.class)  // 拒绝策略专属条件判断
public @interface RejectStrategyBean {
    String value();//拒绝策略名称
}
