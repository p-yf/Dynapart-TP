package com.yf.pool.rejectstrategy;

import com.yf.pool.threadpool.ThreadPool;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Getter
@Setter
public abstract class  RejectStrategy {
    private ThreadPool threadPool;
    public abstract void reject (Runnable task);//处理普通任务的
    public abstract  Future reject (FutureTask task);//处理futureTask的，有返回值是为了判断任务是否被丢弃,更高的灵活性

}
