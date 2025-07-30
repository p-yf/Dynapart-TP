package com.yf.pool.springboot_integration.condition;

import com.yf.pool.springboot_integration.annotation.RejectStrategyBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

public class RejectStrategyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 1. 获取@注解的属性
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(RejectStrategyBean.class.getName());
        if (attributes == null) {
            return false;
        }
        String strategyName = (String) attributes.getFirst("value");

        // 2. 固定读取配置文件
        String configValue = context.getEnvironment().getProperty("fy.thread-pool.rejectStrategy");

        // 3. 配置值与注解name匹配则生效
        return strategyName.equals(configValue);
    }
}
