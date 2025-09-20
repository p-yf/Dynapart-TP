package com.yf.pool.partition.Impl.parti_flow.strategy.impl.offer_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.OfferPolicy;
import com.yf.pool.partition.Partition;

/**
 * @author yyf
 * @date 2025/9/21 0:00
 * @description
 */
public class HashOffer implements OfferPolicy {
    @Override
    public int selectPartition(Partition[] partitions, Object element) {
        int hashCode = element.hashCode();
        // 处理负数：通过 & 0x7FFFFFFF 清除符号位（保证结果为非负）
        return (hashCode & 0x7FFFFFFF) % partitions.length;
    }
}
