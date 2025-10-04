package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;
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
        return (int)round.getAndIncrement()%partitions.length;
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
