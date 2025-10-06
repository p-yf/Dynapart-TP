package com.yf.springboot_integration.pool.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yyf
 * @description
 */
@Data
@ConditionalOnProperty(prefix = "yf.thread-pool.little-chief",name = "enabled",havingValue = "true")
@ConfigurationProperties(prefix = "yf.thread-pool.little-chief")
public class LittleChiefProperties {
    private boolean useVirtualThread;//是否使用虚拟线程
    private Integer coreNums;//核心线程数
    private Integer maxNums;//最大线程数
    private String threadName;//线程名称
    private boolean useDaemon;//是否守护线程
    private Integer aliveTime;//线程空闲时间
    private String rejectStrategyName;//拒绝策略名称
}
