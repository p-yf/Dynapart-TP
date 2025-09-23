package com.yf.core.partition.Impl.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:24
 * @description
 */
public abstract class RemovePolicy {
    public abstract int selectPartition(Partition[] partitions);
}
