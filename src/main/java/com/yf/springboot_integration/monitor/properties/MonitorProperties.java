package com.yf.springboot_integration.monitor.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author yyf
 * @date 2025/8/19 23:11
 * @description
 */
@Data
@ConditionalOnProperty(prefix = "yf.thread-pool.pool",name = "enabled",havingValue = "true")
@ConfigurationProperties(prefix = "yf.thread-pool.monitor")
public class MonitorProperties {
    private boolean enabled;
    private Integer fixedDelay;
}
