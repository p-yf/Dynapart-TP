package com.yf.pool.partition.Impl.parti_flow.strategy.impl.offer_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.OfferPolicy;
import com.yf.pool.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/20 23:47
 * @description
 */
public class RoundRobinOffer implements OfferPolicy {
    AtomicInteger round = new AtomicInteger(0);
    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        return round.getAndIncrement()%partitions.length;
    }
}
