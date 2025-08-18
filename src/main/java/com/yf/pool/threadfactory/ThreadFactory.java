package com.yf.pool.threadfactory;

import com.yf.pool.threadpool.ThreadPool;
import com.yf.pool.worker.Worker;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author yyf
 * @description
 */
@Data
public class ThreadFactory {
    private ThreadPool threadPool;
    private String threadName;// 线程名称
    private Boolean isDaemon;//是否守护线程
    private Boolean coreDestroy;//是否销毁核心线程
    private Integer aliveTime;//空闲存活时间 :null代表不销毁


    public ThreadFactory(String threadName,Boolean isDaemon,Boolean coreDestroy,Integer aliveTime){
        this.threadName = threadName;
        this.isDaemon = isDaemon;
        this.coreDestroy = coreDestroy;
        this.aliveTime = aliveTime;
    }

    public Worker createWorker(Boolean isCore, Runnable task){
        return new Worker(threadPool,isCore,threadName,isDaemon,coreDestroy,aliveTime,task);
    }

}
