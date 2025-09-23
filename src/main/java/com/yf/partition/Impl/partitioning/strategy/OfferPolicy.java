package com.yf.partition.Impl.partitioning.strategy;

import com.yf.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:04
 * @description
 */
public abstract class OfferPolicy {
    public abstract int selectPartition(Partition[] partitions, Object object);
    public abstract boolean getRoundRobin();
    public abstract void setRoundRobin(boolean roundRobin);
}
