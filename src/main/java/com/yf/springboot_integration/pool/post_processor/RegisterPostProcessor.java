package com.yf.springboot_integration.pool.post_processor;

import com.yf.common.constant.Logo;
import com.yf.core.resource_manager.GCTaskManager;
import com.yf.core.resource_manager.PartiResourceManager;
import com.yf.core.resource_manager.RSResourceManager;
import com.yf.core.resource_manager.SPResourceManager;
import com.yf.core.partition.Partition;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.springboot_integration.pool.annotation.GCTResource;
import com.yf.springboot_integration.pool.annotation.PartiResource;
import com.yf.springboot_integration.pool.annotation.RSResource;
import com.yf.springboot_integration.pool.annotation.SPResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RegisterPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 1. 获取Spring扫描到的所有Bean名称（复用Spring自身的扫描结果）
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        List<String> gctBeanNames = new ArrayList<>();
        for (String beanName : beanDefinitionNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            if (className == null) continue;
            try {
                // 2. 加载类，判断是否被@PartitionBean注解
                Class<?> clazz = Class.forName(className);
                PartiResource partiResource = clazz.getAnnotation(PartiResource.class);
                RSResource RSResource = clazz.getAnnotation(RSResource.class);
                SPResource SPResource = clazz.getAnnotation(SPResource.class);
                GCTResource gctResource = clazz.getAnnotation(GCTResource.class);
                if (partiResource != null) {//分区资源
                    // 3. 注册到自定义中心
                    PartiResourceManager.register(partiResource.value(), clazz.asSubclass(Partition.class));
                    log.info(Logo.LOG_LOGO +"开发者自定义队列："+ partiResource.value()+"注册成功！");
                    // 4. 从Spring容器中移除该Bean定义
                    registry.removeBeanDefinition(beanName);
                }
                else if (RSResource != null) {//拒绝策略资源
                    // 3. 注册到自定义中心
                    RSResourceManager.register(RSResource.value(), clazz.asSubclass(RejectStrategy.class));
                    log.info(Logo.LOG_LOGO +"开发者自定义拒绝策略："+ RSResource.value()+"注册成功！");
                    // 4. 从Spring容器中移除该Bean定义
                    registry.removeBeanDefinition(beanName);
                } else if (SPResource !=null) {//调度规则资源
                    // 3. 注册到自定义中心
                    SPResourceManager.register(SPResource.value(), clazz);
                    log.info(Logo.LOG_LOGO +"开发者自定义调度规则："+ SPResource.value()+"注册成功！");
                    registry.removeBeanDefinition(beanName);
                } else if (gctResource != null){//GC任务资源
                    //放到list中等到所有的bean扫描完了再处理
                    gctBeanNames.add(beanName);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        for (String gctBeanName : gctBeanNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(gctBeanName);
            String className = beanDefinition.getBeanClassName();
            if (className == null) continue;
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
                GCTResource gctResource = clazz.getAnnotation(GCTResource.class);

                //得到绑定资源的名称
                String resourceName = gctResource.value();
                //从自定义中心中获取对应的资源类
                Class<? extends Partition> resource = PartiResourceManager.getResource(resourceName);
                registerGCExecutorManager(resource,clazz,resourceName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void registerGCExecutorManager(Class resource, Class taskClazz,String name){
        if(resource ==null){
            return;
        }
        GCTaskManager.register(resource,taskClazz);
        log.info(Logo.LOG_LOGO +"开发者自定义GC任务注册成功，绑定资源名称："+name);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
