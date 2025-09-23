package com.yf.core.partition.Impl.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:04
 * @description
 */
public abstract class OfferPolicy {
    /**
     * 选择分区
     * @param partitions：分区数组
     * @param object:元素
     * @return 分区索引
     */
    public abstract int selectPartition(Partition[] partitions, Object object);

    /**
     * 获取是否轮询
     * @return
     */
    public abstract boolean getRoundRobin();

    /**
     * 设置是否轮询
     * @param roundRobin
     */
    public abstract void setRoundRobin(boolean roundRobin);
}
