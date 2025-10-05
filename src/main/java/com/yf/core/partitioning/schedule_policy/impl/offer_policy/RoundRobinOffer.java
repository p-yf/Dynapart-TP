package com.yf.core.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yyf
 * @date 2025/9/20 23:47
 * @description
 */
public class RoundRobinOffer extends OfferPolicy {
    private volatile boolean roundRobin = true;

    final AtomicLong round = new AtomicLong(0);

    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        int ps = partitions.length;
        int r = (int)round.getAndIncrement()%partitions.length;
        if ((ps & (ps - 1)) == 0) {
            // 分区数量为2的幂时，用&运算
            return r & (ps - 1);
        } else {
            return r % ps;
        }
    }

    @Override
    public boolean getRoundRobin() {
        return roundRobin;
    }

    @Override
    public void setRoundRobin(boolean roundRobin) {
        this.roundRobin = roundRobin;
    }
}
