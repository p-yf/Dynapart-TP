package com.yf.pool.partition.Impl.parti_flow.strategy;

import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:24
 * @description
 */
public interface RemovePolicy {
    int selectPartition(Partition[] partitions);
}
