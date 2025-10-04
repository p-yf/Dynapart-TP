package com.yf.core.partition.Impl.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partition.Impl.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:00
 * @description
 */
public class PlainHashOffer extends OfferPolicy {
    private volatile boolean roundRobin = true;

    @Override
    public int selectPartition(Partition[] partitions, Object element) {
        int ps = partitions.length;
        int h = element.hashCode();
        if ((ps & (ps - 1)) == 0) {
            // 分区数量为2的幂时，用&运算
            return h & (ps - 1);
        } else {
            // 处理负数：通过 & 0x7FFFFFFF 清除符号位（保证结果为非负）
            return (h & 0x7FFFFFFF) % ps;
        }
    }
//    @Override
//    public int selectPartition(Partition[] partitions, Object element) {
//        return (partitions.hashCode() & 0x7FFFFFFF) % partitions.length;
//    }

    @Override
    public boolean getRoundRobin() {
        return roundRobin;
    }

    @Override
    public void setRoundRobin(boolean roundRobin) {
        this.roundRobin = roundRobin;
    }
}
