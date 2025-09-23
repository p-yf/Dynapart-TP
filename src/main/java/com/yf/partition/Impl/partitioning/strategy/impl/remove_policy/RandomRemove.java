package com.yf.partition.Impl.partitioning.strategy.impl.remove_policy;

import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:06
 * @description
 */
public class RandomRemove extends RemovePolicy {

    @Override
    public int selectPartition(Partition[] partitions) {
        return (int) (Math.random() * partitions.length);
    }
}
