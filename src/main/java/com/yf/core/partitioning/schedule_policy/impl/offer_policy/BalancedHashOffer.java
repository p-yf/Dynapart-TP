package com.yf.core.partitioning.schedule_policy.impl.offer_policy;

import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/10/4 22:11
 * @description  借鉴了hashmap的hash计算逻辑，优化了哈希分布，以减少hash冲突
 */
public class BalancedHashOffer extends OfferPolicy {
    private volatile boolean roundRobin = false;
    @Override
    public int selectPartition(Partition[] partitions, Object element) {
        int ps = partitions.length;
        int h = hash(element);
        if ((ps & (ps - 1)) == 0) {
            // 分区数量为2的幂时，用&运算
            return h & (ps - 1);
        } else {
            return (h & 0x7FFFFFFF) % ps;
        }
    }

    /**
     * 哈希搅动方法：混合高低位信息，优化哈希分布
     */
    private int hash(Object element) {
        if (element == null) {
            return 0;
        }
        int h = element.hashCode();
        // 核心搅动：高16位与低16位异或，混合高低位信息
        h ^= h >>> 16;
        return h;
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
