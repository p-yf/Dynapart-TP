package com.yf.springboot_integration.pool.auto_configuration;

import com.yf.pool.constant_or_registry.QueueManager;
import com.yf.pool.constant_or_registry.RejectStrategyManager;
import com.yf.pool.constant_or_registry.SchedulePolicyManager;
import com.yf.partition.Impl.partitioning.strategy.OfferPolicy;
import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.partition.Impl.partitioning.PartiFlow;
import com.yf.partition.Partition;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.threadpool.ThreadPool;
import com.yf.springboot_integration.pool.post_processor.RegisterPostProcessor;
import com.yf.springboot_integration.pool.properties.PoolProperties;
import com.yf.springboot_integration.pool.properties.QueueProperties;
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
@AutoConfiguration
@EnableConfigurationProperties({PoolProperties.class, QueueProperties.class})
@ConditionalOnProperty(prefix = "yf.thread-pool.pool", name = "enabled", havingValue = "true")
public class ThreadPoolAutoConfiguration {


    /**
     * 创建线程池
     */
    @Bean
    public ThreadPool threadPool(PoolProperties threadPoolProperties, QueueProperties queueProperties) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String queueName = queueProperties.getQueueName();
        String rejectStrategyName = threadPoolProperties.getRejectStrategyName();
        Partition partition;
        RejectStrategy rejectStrategy;
        if (!queueProperties.isPartitioning()) {//非分区化
            Class<?> taskQueueClass = QueueManager.getResources().get(queueName);
            Constructor<?> queueClassConstructor = taskQueueClass.getConstructor();
            partition = (Partition) queueClassConstructor.newInstance();
            partition.setCapacity(queueProperties.getCapacity());
        } else {//分区化
            partition = new PartiFlow(queueProperties.getPartitionNum(), queueProperties.getCapacity(), queueProperties.getQueueName(),
                    (OfferPolicy) SchedulePolicyManager.getOfferResource(queueProperties.getOfferPolicy()).getConstructor().newInstance(),
                    (PollPolicy) SchedulePolicyManager.getPollResource(queueProperties.getPollPolicy()).getConstructor().newInstance(),
                    (RemovePolicy) SchedulePolicyManager.getRemoveResource(queueProperties.getRemovePolicy()).getConstructor().newInstance());

        }

        Class<?> rejectStrategyClass = RejectStrategyManager.REJECT_STRATEGY_MAP.get(rejectStrategyName);
        Constructor<?> rejectStrategyClassConstructor = rejectStrategyClass.getConstructor();
        rejectStrategy = (RejectStrategy) rejectStrategyClassConstructor.newInstance();
        ThreadPool threadPool = new ThreadPool(threadPoolProperties.getCoreNums(),
                threadPoolProperties.getMaxNums(),
                threadPoolProperties.getPoolName(),
                new ThreadFactory(threadPoolProperties.getThreadName(),
                        threadPoolProperties.getIsDaemon(),
                        threadPoolProperties.getCoreDestroy(),
                        threadPoolProperties.getAliveTime()),
                partition, rejectStrategy);
        return threadPool;
    }

    @Bean
    public RegisterPostProcessor registerPostProcessor() {
        return new RegisterPostProcessor();
    }

}
