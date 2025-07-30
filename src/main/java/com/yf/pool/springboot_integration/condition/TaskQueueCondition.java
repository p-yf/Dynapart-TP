package com.yf.pool.springboot_integration.condition;

import com.yf.pool.springboot_integration.annotation.TaskQueueBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

public class TaskQueueCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 1. 获取注解的属性
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(TaskQueueBean.class.getName());
        if (attributes == null) {
            return false;
        }
        String queueName = (String) attributes.getFirst("value");

        // 2. 固定读取配置文件
        String configValue = context.getEnvironment().getProperty("fy.thread-pool.queueName");

        // 3. 配置值与注解name匹配则生效
        return queueName.equals(configValue);
    }
}
