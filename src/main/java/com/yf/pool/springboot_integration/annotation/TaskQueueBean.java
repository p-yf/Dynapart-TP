package com.yf.pool.springboot_integration.annotation;

import com.yf.pool.springboot_integration.condition.TaskQueueCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Conditional(TaskQueueCondition.class)  // 阻塞队列专属条件判断
public @interface TaskQueueBean {
    String value();// 队列名称
}
