package com.yf.core.partitioning.schedule_policy.impl.remove_policy;

import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yyf
 * @date 2025/9/21 0:05
 * @description
 */
public class RoundRobinRemove extends RemovePolicy {
    final AtomicLong round = new AtomicLong(0);
    @Override
    public int selectPartition(Partition[] partitions,Object o) {
        int ps = partitions.length;
        int r = (int)round.getAndIncrement()%partitions.length;
        if ((ps & (ps - 1)) == 0) {
            // 分区数量为2的幂时，用&运算
            return r & (ps - 1);
        } else {
            return r % ps;
        }
    }

}
