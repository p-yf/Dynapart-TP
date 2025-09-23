package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:00
 * @description
 */
public class HashOffer extends OfferPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions, Object element) {
        int hashCode = element.hashCode();
        // 处理负数：通过 & 0x7FFFFFFF 清除符号位（保证结果为非负）
        return (hashCode & 0x7FFFFFFF) % partitions.length;
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
