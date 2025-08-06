package com.yf.springboot_integration.pool.condition;

import com.yf.springboot_integration.pool.annotation.RejectStrategyBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;

import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.MultiValueMap;


/**
 * @author yyf
 * @description
 */
public class RejectStrategyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        //当配置中是可以更换拒绝策略的时候，全部被扫描到的策略都注册成bean
        String replaceable = context.getEnvironment().getProperty("yf.thread-pool.monitor.rsReplaceable");
        if(replaceable==null||replaceable.equals("true")){
            return true;
        }
        // 1. 获取@注解的属性
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(RejectStrategyBean.class.getName());
        if (attributes == null) {
            return false;
        }
        String strategyName = (String) attributes.getFirst("value");

        // 2. 固定读取配置文件
        String configValue = context.getEnvironment().getProperty("yf.thread-pool.rejectStrategyName");

        // 3. 配置值与注解name匹配则生效
        return strategyName.equals(configValue);
    }

}
