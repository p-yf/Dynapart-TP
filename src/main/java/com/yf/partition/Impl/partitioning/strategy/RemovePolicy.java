package com.yf.partition.Impl.partitioning.strategy;

import com.yf.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:24
 * @description
 */
public abstract class RemovePolicy {
    public abstract int selectPartition(Partition[] partitions);
}
