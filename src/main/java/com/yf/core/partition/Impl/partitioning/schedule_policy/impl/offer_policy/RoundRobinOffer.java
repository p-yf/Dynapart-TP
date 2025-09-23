package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/20 23:47
 * @description
 */
public class RoundRobinOffer extends OfferPolicy {
    private volatile boolean roundRobin = true;

    final AtomicInteger round = new AtomicInteger(0);

    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        return Math.abs(round.getAndIncrement()%partitions.length);
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
