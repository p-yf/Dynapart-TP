package com.yf.partition.Impl.partitioning.strategy.impl.remove_policy;

import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/21 0:05
 * @description
 */
public class RoundRobinRemove extends RemovePolicy {
    final AtomicInteger round = new AtomicInteger(0);
    @Override
    public int selectPartition(Partition[] partitions) {
        return Math.abs(round.getAndIncrement()%partitions.length);
    }

}
