package com.yf.common.task;

import com.yf.core.partition.Partition;
import com.yf.core.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yyf
 * @date 2025/10/6 15:34
 * @description
 */
@Getter
@Setter
public abstract class GCTask implements Runnable{
    ThreadPool threadPool;
    Partition<?> partition;
    public GCTask build(ThreadPool tp,Partition<?> partition){
        setThreadPool(tp);
        setPartition(partition);
        return this;
    }
}
