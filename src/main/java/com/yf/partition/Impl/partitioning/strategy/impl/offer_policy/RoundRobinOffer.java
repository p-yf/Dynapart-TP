package com.yf.partition.Impl.partitioning.strategy.impl.offer_policy;

import com.yf.partition.Impl.partitioning.strategy.OfferPolicy;
import com.yf.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/20 23:47
 * @description
 */
public class RoundRobinOffer extends OfferPolicy {
    final AtomicInteger round = new AtomicInteger(0);
    private volatile boolean roundRobin = true;

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
