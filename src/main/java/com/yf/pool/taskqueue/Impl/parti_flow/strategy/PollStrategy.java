package com.yf.pool.taskqueue.Impl.parti_flow.strategy;

import com.yf.pool.taskqueue.Impl.parti_flow.Partition;
import com.yf.pool.worker.Worker;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/8/9 9:53
 * @description
 */
public enum PollStrategy {

    /**
     * 轮询
     */
    ROUND_ROBIN{
        final AtomicInteger round = new AtomicInteger(0);
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
                if(partitions[i].getElementNums() > partitions[maxIndex].getElementNums()){
                    maxIndex = i;
                }
            }
            return maxIndex;
        }
    },

    /**
     *线程绑定
     */
    THREAD_BINDING{
        final AtomicInteger round = new AtomicInteger(0);
        final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
        @Override
        public int selectPartition(Partition[] partitions) {
            if(threadLocal.get()==null){
                threadLocal.set(round.getAndIncrement()%partitions.length);
            }
            return threadLocal.get();
        }
    };

    public abstract int selectPartition(Partition[] partitions);

}
