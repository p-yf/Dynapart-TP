package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/20 23:49
 * @description
 */
public class RandomOffer extends OfferPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        return (int) (Math.random() * partitions.length);
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
