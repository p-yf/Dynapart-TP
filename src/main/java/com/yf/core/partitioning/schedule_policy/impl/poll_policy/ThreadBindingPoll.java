package com.yf.core.partitioning.schedule_policy.impl.poll_policy;

import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partition.Partition;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yyf
 * @date 2025/9/21 0:08
 * @description 线程绑定策略，使得每个线程能够固定消费各自的分区，所以默认关闭轮询
 */
public class ThreadBindingPoll extends PollPolicy {
    private volatile boolean roundRobin = false;

    final AtomicLong round = new AtomicLong(0);
    private final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    @Override
    public int selectPartition(Partition[] partitions) {
        if(threadLocal.get()==null){
            threadLocal.set((int)round.getAndIncrement()%partitions.length);
        }
        return threadLocal.get();
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
