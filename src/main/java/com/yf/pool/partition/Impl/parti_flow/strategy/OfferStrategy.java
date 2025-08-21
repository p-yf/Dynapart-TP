package com.yf.pool.partition.Impl.parti_flow.strategy;


import com.yf.pool.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/8/9 0:43
 * @description
 */
public enum OfferStrategy {

    /**
     * 轮询
     */
    ROUND_ROBIN{
        AtomicInteger round = new AtomicInteger(0);
        @Override
        public int selectPartition(Partition[] partitions, Object object) {
            return round.getAndIncrement()%partitions.length;
        }
    },

    /**
     * 随机
     */
    RANDOM{
        @Override
        public int selectPartition(Partition[] partitions, Object object) {
             return (int) (Math.random() * partitions.length);
        }
    },

    /**
     * Hash
     */
    HASH{
        @Override
        public int selectPartition(Partition[] partitions, Object element) {
            int hashCode = element.hashCode();
            // 处理负数：通过 & 0x7FFFFFFF 清除符号位（保证结果为非负）
            return (hashCode & 0x7FFFFFFF) % partitions.length;
        }
    },

    /**
     * 填谷
     */
    VALLEY_FILLING{//填谷
        @Override
        public int selectPartition(Partition[] partitions,Object object) {
            int minIndex = 0;
            for(int i = 0; i < partitions.length; i++){
                if(partitions[i].getEleNums() < partitions[minIndex].getEleNums()){
                    minIndex = i;
                }
            }
            return minIndex;
        }
    };

    public abstract int selectPartition(Partition[] partitions, Object object);

    public static OfferStrategy fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            // 利用枚举自带的valueOf()方法，名称需完全匹配（大小写敏感）
            return OfferStrategy.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ROUND_ROBIN;
        }
    }
}
