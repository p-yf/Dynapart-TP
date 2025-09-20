package com.yf.pool.partition.Impl.parti_flow.strategy.impl.offer_policy;

import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 23:49
 * @description
 */
public class RandomOffer {
    public int selectPartition(Partition[] partitions, Object object) {
        return (int) (Math.random() * partitions.length);
    }
}
