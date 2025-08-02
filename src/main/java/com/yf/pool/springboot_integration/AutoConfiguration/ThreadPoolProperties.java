package com.yf.pool.springboot_integration.AutoConfiguration;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yyf
 * @description
 */
@Data
@ConditionalOnProperty(prefix = "fy.thread-pool",name = "enabled",havingValue = "true")
@ConfigurationProperties(prefix = "fy.thread-pool")
public class ThreadPoolProperties {
    private Integer coreNums;//核心线程数
    private Integer maxNums;//最大线程数
    private String poolName;//线程池名称
    private String threadName;//线程名称
    private Boolean isDaemon;//是否守护线程
    private Boolean coreDestroy;//核心线程是否可销毁
    private Integer aliveTime;//线程空闲时间
    private String queueName;//队列名称
    private Integer queueCapacity;//队列容量    可以为null，null代表无界
    private String rejectStrategyName;//拒绝策略名称
}
