package com.yf.pool.partition.Impl.parti_flow.strategy;

import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 21:04
 * @description
 */
public interface OfferPolicy {
    int selectPartition(Partition[] partitions, Object object);
}
