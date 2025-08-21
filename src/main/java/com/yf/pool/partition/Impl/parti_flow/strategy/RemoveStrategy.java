package com.yf.pool.partition.Impl.parti_flow.strategy;


import com.yf.pool.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/8/9 9:54
 * @description
 */
public enum RemoveStrategy {

    /**
     * 轮询
     */
    ROUND_ROBIN{
        AtomicInteger round = new AtomicInteger(0);
        @Override
        public int selectPartition(Partition[] partitions) {
            return round.getAndIncrement()%partitions.length;
        }
    },

    /**
     * 随机
     */
    RANDOM{
        @Override
        public int selectPartition(Partition[] partitions) {
            return (int) (Math.random() * partitions.length);
        }
    },

    /**
     * 削峰
     */
    PEEK_SHAVING{//削峰
        @Override
        public int selectPartition(Partition[] partitions) {
            int maxIndex = 0;
            for(int i = 0; i < partitions.length; i++){
                if(partitions[i].getEleNums() > partitions[maxIndex].getEleNums()){
                    maxIndex = i;
                }
            }
            return maxIndex;
        }
    };

    public abstract int selectPartition(Partition[] partitions);

    public static RemoveStrategy fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            // 利用枚举自带的valueOf()方法，名称需完全匹配（大小写敏感）
            return RemoveStrategy.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ROUND_ROBIN;
        }
    }

}
