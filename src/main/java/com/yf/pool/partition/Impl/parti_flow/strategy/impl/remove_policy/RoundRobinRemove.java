package com.yf.pool.partition.Impl.parti_flow.strategy.impl.remove_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.RemovePolicy;
import com.yf.pool.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/21 0:05
 * @description
 */
public class RoundRobinRemove implements RemovePolicy {
    final AtomicInteger round = new AtomicInteger(0);
    @Override
    public int selectPartition(Partition[] partitions) {
        return round.getAndIncrement()%partitions.length;
    }

}
