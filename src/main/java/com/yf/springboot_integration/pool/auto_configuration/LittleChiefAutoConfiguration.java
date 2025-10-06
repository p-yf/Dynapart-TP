package com.yf.springboot_integration.pool.auto_configuration;

import com.yf.common.constant.OfPool;
import com.yf.core.resource_manager.GCTaskManager;
import com.yf.core.resource_manager.PartiResourceManager;
import com.yf.core.resource_manager.RSResourceManager;
import com.yf.core.resource_manager.SPResourceManager;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partition.Partition;
import com.yf.core.workerfactory.WorkerFactory;
import com.yf.core.threadpool.ThreadPool;
import com.yf.springboot_integration.pool.post_processor.ResourceRegisterPostProcessor;
import com.yf.springboot_integration.pool.post_processor.TPRegisterPostProcessor;
import com.yf.springboot_integration.pool.properties.LittleChiefProperties;
import com.yf.springboot_integration.pool.properties.QueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * @author yyf
 * @description
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({LittleChiefProperties.class, QueueProperties.class})
@ConditionalOnProperty(prefix = "yf.thread-pool.little-chief", name = "enabled", havingValue = "true")
public class LittleChiefAutoConfiguration {


    /**
     * 创建GC管理者中的littleChief
     */
    @Bean(OfPool.LITTLE_CHIEF)
    public ThreadPool threadPool(LittleChiefProperties threadPoolProperties, QueueProperties queueProperties) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String queueName = queueProperties.getQueueName();
        String rejectStrategyName = threadPoolProperties.getRejectStrategyName();
        Partition partition;
        RejectStrategy rejectStrategy;
        if (!queueProperties.isPartitioning()) {//非分区化
            Class<?> taskQueueClass = PartiResourceManager.getResources().get(queueName);
            Constructor<?> queueClassConstructor = taskQueueClass.getConstructor();
            partition = (Partition) queueClassConstructor.newInstance();
            partition.setCapacity(queueProperties.getCapacity());
        } else {//分区化
            partition = new PartiFlow
                    (queueProperties.getPartitionNum(), queueProperties.getCapacity(), queueProperties.getQueueName(),
                    SPResourceManager.getOfferResource(queueProperties.getOfferPolicy()).getConstructor().newInstance(),
                    SPResourceManager.getPollResource(queueProperties.getPollPolicy()).getConstructor().newInstance(),
                    SPResourceManager.getRemoveResource(queueProperties.getRemovePolicy()).getConstructor().newInstance());
        }

        Class<?> rejectStrategyClass = RSResourceManager.REJECT_STRATEGY_MAP.get(rejectStrategyName);
        Constructor<?> rejectStrategyClassConstructor = rejectStrategyClass.getConstructor();
        rejectStrategy = (RejectStrategy) rejectStrategyClassConstructor.newInstance();

        ThreadPool littleChief = new ThreadPool(threadPoolProperties.getCoreNums(),
                threadPoolProperties.getMaxNums(),
                OfPool.LITTLE_CHIEF,
                new WorkerFactory(threadPoolProperties.getThreadName(),
                        threadPoolProperties.isUseDaemon(),
                        true,//必须支持销毁核心线程
                        threadPoolProperties.getAliveTime(),
                        threadPoolProperties.isUseVirtualThread()
                        ),
                partition, rejectStrategy);
        GCTaskManager.setLittleChief(littleChief);
        return littleChief;
    }


}
