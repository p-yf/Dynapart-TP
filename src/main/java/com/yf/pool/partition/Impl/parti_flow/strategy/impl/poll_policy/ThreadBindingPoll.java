package com.yf.pool.partition.Impl.parti_flow.strategy.impl.poll_policy;

import com.yf.pool.partition.Impl.parti_flow.strategy.PollPolicy;
import com.yf.pool.partition.Partition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yyf
 * @date 2025/9/21 0:08
 * @description
 */
public class ThreadBindingPoll implements PollPolicy {
    final AtomicInteger round = new AtomicInteger(0);
    final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
    @Override
    public int selectPartition(Partition[] partitions) {
        if(threadLocal.get()==null){
            threadLocal.set(round.getAndIncrement()%partitions.length);
        }
        return threadLocal.get();
    }
}
