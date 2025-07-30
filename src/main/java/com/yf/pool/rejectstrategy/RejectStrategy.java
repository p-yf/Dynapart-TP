package com.yf.pool.rejectstrategy;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class RejectStrategy {
    private ThreadPool threadPool;
    public abstract void reject (Runnable task);
}
