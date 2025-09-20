package com.yf.pool.partition.Impl.parti_flow.strategy.impl.remove_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.RemovePolicy;
import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:06
 * @description
 */
public class RandomRemove implements RemovePolicy {
    @Override
    public int selectPartition(Partition[] partitions) {
        return (int) (Math.random() * partitions.length);
    }
}
