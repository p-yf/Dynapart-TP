package com.yf.springboot_integration.pool.condition;

import com.yf.springboot_integration.pool.annotation.PartitionBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;


/**
 * @author yyf
 * @description
 */
public class TaskQueueCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        //当配置中是可以更换拒绝策略的时候，全部策略都注册成bean
        String replaceable = context.getEnvironment().getProperty("yf.thread-pool.monitor.qReplaceable");
        if(replaceable==null||replaceable.equals("true")){
            return true;
        }
        // 1. 获取注解的属性
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(PartitionBean.class.getName());
        if (attributes == null) {
            return false;
        }
        String queueName = (String) attributes.getFirst("value");

        // 2. 固定读取配置文件
        String configValue = context.getEnvironment().getProperty("yf.thread-pool.queueName");

        // 3. 配置值与注解name匹配则生效
        return queueName.equals(configValue);
    }
}
