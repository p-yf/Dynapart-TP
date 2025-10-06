package com.yf.springboot_integration.pool.post_processor;

import com.yf.common.constant.Logo;
import com.yf.common.constant.OfPool;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.Map;
/**
 * @author yyf
 * @description: 用来注册所有的线程池（会去除掉bean容器中的little chief小线程池，开发者定义的不会去除）
 */
@Slf4j
public class TPRegisterPostProcessor implements SmartInitializingSingleton {

    private final DefaultListableBeanFactory beanFactory;

    // 构造器注入BeanFactory（Spring自动提供）
    public TPRegisterPostProcessor(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, ThreadPool> tpBeans = beanFactory.getBeansOfType(ThreadPool.class);
        for(ThreadPool tp : tpBeans.values()){
            String tpName = tp.getName();
            if(tpName.equals(OfPool.LITTLE_CHIEF)){
                //说明是gc任务中的线程池，不能被springboot管理
                beanFactory.destroySingleton(tpName);
                // 3.2 删除Spring中的Bean定义（后续不会再创建）
                if (beanFactory.containsBeanDefinition(tpName)) {
                    beanFactory.removeBeanDefinition(tpName);
                }
            }else{
                //用户业务的线程池，可以被springboot管理
                UnifiedTPRegulator.register(tpName, tp);
                log.info(Logo.LOG_LOGO+"线程池："+tpName+" 注册成功");
            }
        }
    }
}
