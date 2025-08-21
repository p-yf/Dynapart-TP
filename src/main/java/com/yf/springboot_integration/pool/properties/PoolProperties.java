package com.yf.springboot_integration.pool.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yyf
 * @description
 */
@Data
@ConditionalOnProperty(prefix = "yf.thread-pool.pool",name = "enabled",havingValue = "true")
@ConfigurationProperties(prefix = "yf.thread-pool.pool")
public class PoolProperties {
    private Integer coreNums;//核心线程数
    private Integer maxNums;//最大线程数
    private String poolName;//线程池名称
    private String threadName;//线程名称
    private Boolean isDaemon;//是否守护线程
    private Boolean coreDestroy;//核心线程是否可销毁
    private Integer aliveTime;//线程空闲时间
    private String rejectStrategyName;//拒绝策略名称
}
