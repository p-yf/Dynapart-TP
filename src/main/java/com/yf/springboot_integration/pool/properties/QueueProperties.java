package com.yf.springboot_integration.pool.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yyf
 * @date 2025/8/21 17:40
 * @description
 */
@ConditionalOnProperty(prefix = "yf.thread-pool.pool",name = "enabled",havingValue = "true")
@ConfigurationProperties(prefix = "yf.thread-pool.queue")
@Data
public class QueueProperties {
    // 是否分区化(如果是false，只需要读取capacity和queueName)
    private boolean partitioning;

    // 分区数量
    private int partitionNum;

    // 队列容量（null表示无界）
    private Integer capacity;

    // 队列名称
    private String queueName;

    // 入队策略
    private String offerStrategy;

    // 出队策略
    private String pollStrategy;

    // 移除策略
    private String removeStrategy;

}
