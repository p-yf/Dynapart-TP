package com.yf.springboot_integration.pool.auto_configuration;

import com.yf.pool.constant_or_registry.QueueRegistry;
import com.yf.pool.constant_or_registry.RejectStrategyRegistry;
import com.yf.pool.partition.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.partition.Impl.parti_flow.strategy.RemoveStrategy;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.partition.Impl.parti_flow.PartiFlow;
import com.yf.pool.partition.Partition;
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
public class ThreadPoolConfiguration {


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
            Class<?> taskQueueClass = QueueRegistry.TASK_QUEUE_MAP.get(queueName);
            Constructor<?> queueClassConstructor = taskQueueClass.getConstructor();
            partition = (Partition) queueClassConstructor.newInstance();
            partition.setCapacity(queueProperties.getCapacity());
        } else {//分区化
            partition = new PartiFlow(queueProperties.getPartitionNum(), queueProperties.getCapacity()
                    , queueName
                    , OfferStrategy.fromName(queueProperties.getOfferStrategy())
                    , PollStrategy.fromName(queueProperties.getPollStrategy())
                    , RemoveStrategy.fromName(queueProperties.getRemoveStrategy()));

        }

        Class<?> rejectStrategyClass = RejectStrategyRegistry.REJECT_STRATEGY_MAP.get(rejectStrategyName);
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
        threadPool.setQueueName(queueName);
        threadPool.setRejectStrategyName(rejectStrategyName);
        return threadPool;
    }

    @Bean
    public RegisterPostProcessor registerPostProcessor() {
        return new RegisterPostProcessor();
    }

}
