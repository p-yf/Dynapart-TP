package com.yf.springboot_integration.pool.annotation;

import com.yf.springboot_integration.pool.condition.TaskQueueCondition;
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
@Conditional(TaskQueueCondition.class)  // 阻塞队列专属条件判断
public @interface PartitionBean {
    String value();// 队列名称
}
