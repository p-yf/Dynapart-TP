package com.yf.springboot_integration.pool.post_processor;

import com.yf.pool.constant_or_registry.QueueRegistry;
import com.yf.pool.constant_or_registry.RejectStrategyRegistry;
import com.yf.pool.partition.Partition;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.springboot_integration.pool.annotation.PartitionBean;
import com.yf.springboot_integration.pool.annotation.RejectStrategyBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

@Slf4j
public class RegisterPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 1. 获取Spring扫描到的所有Bean名称（复用Spring自身的扫描结果）
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();

        for (String beanName : beanDefinitionNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            if (className == null) continue;
            try {
                // 2. 加载类，判断是否被@PartitionBean注解
                Class<?> clazz = Class.forName(className);
                PartitionBean partitionBean = clazz.getAnnotation(PartitionBean.class);
                RejectStrategyBean rejectStrategyBean = clazz.getAnnotation(RejectStrategyBean.class);
                if (partitionBean != null) {
                    // 3. 注册到自定义中心
                    QueueRegistry.register(partitionBean.value(), clazz.asSubclass(Partition.class));
                    log.info("开发者自定义队列："+partitionBean.value()+"注册成功！");
                    // 4. 从Spring容器中移除该Bean定义
                    registry.removeBeanDefinition(beanName);
                }
                else if (rejectStrategyBean != null) {
                    // 3. 注册到自定义中心
                    RejectStrategyRegistry.register(rejectStrategyBean.value(), clazz.asSubclass(RejectStrategy.class));
                    log.info("开发者自定义拒绝策略："+rejectStrategyBean.value()+"注册成功！");
                    // 4. 从Spring容器中移除该Bean定义
                    registry.removeBeanDefinition(beanName);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 无需实现
    }
}
