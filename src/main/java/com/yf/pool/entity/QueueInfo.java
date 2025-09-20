package com.yf.pool.entity;

import lombok.Data;

/**
 * @author yyf
 * @date 2025/9/19 13:27
 * @description
 */
@Data
public class QueueInfo {
    // 是否分区化(如果是false，只需要读取capacity和queueName)
    private boolean partitioning;

    // 分区数量
    private Integer partitionNum;

    // 队列容量（null表示无界）
    private Integer capacity;

    // 队列名称
    private String queueName;

    // 入队策略
    private String offerPolicy;

    // 出队策略
    private String pollPolicy;

    // 移除策略
    private String removePolicy;

}
