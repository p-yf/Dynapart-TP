package com.yf.springboot_integration.pool.auto_configuration;

import com.yf.pool.constant.OfQueue;
import com.yf.pool.constant.OfRejectStrategy;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.taskqueue.TaskQueue;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.threadpool.ThreadPool;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * @author yyf
 * @description
 */
@AutoConfiguration
@EnableConfigurationProperties(ThreadPoolProperties.class)
@ConditionalOnProperty(prefix = "yf.thread-pool", name = "enabled", havingValue = "true")
public class ThreadPoolConfiguration {

    private final ApplicationContext context;

    public ThreadPoolConfiguration(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 创建线程池
     */
    @Bean
    public ThreadPool threadPool(ThreadPoolProperties threadPoolProperties) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String queueName = threadPoolProperties.getQueueName();
        String rejectStrategyName = threadPoolProperties.getRejectStrategyName();
        TaskQueue taskQueue; RejectStrategy rejectStrategy;
        try {//尝试从容器中获取，没有的话从作者默认实现中获取
            taskQueue = (TaskQueue) context.getBean(queueName);
            taskQueue.setCapacity(threadPoolProperties.getQueueCapacity());
        }catch (NoSuchBeanDefinitionException e){
            Class<?> taskQueueClass = OfQueue.TASK_QUEUE_MAP.get(queueName);
            Constructor<?> queueClassConstructor = taskQueueClass.getConstructor();
            taskQueue = (TaskQueue) queueClassConstructor.newInstance();
            taskQueue.setCapacity(threadPoolProperties.getQueueCapacity());
        }
        try {
            rejectStrategy = (RejectStrategy) context.getBean(rejectStrategyName);
        }catch (NoSuchBeanDefinitionException e){
            Class<?> rejectStrategyClass = OfRejectStrategy.REJECT_STRATEGY_MAP.get(rejectStrategyName);
            Constructor<?> rejectStrategyClassConstructor = rejectStrategyClass.getConstructor();
            rejectStrategy = (RejectStrategy) rejectStrategyClassConstructor.newInstance();
        }
        ThreadPool threadPool = new ThreadPool(threadPoolProperties.getCoreNums(),
                threadPoolProperties.getMaxNums(),
                threadPoolProperties.getPoolName(),
                new ThreadFactory(threadPoolProperties.getThreadName(),
                        threadPoolProperties.getIsDaemon(),
                        threadPoolProperties.getCoreDestroy(),
                        threadPoolProperties.getAliveTime()),
                taskQueue, rejectStrategy);
        threadPool.setQueueName(queueName);
        threadPool.setRejectStrategyName(rejectStrategyName);
        return threadPool;
    }

}
