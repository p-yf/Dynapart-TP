package com.yf.pool;

import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.pool.taskqueue.Impl.LinkedBlockingQueue;
import com.yf.pool.threadfactory.ThreadFactory;
import com.yf.pool.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {
    static List<Future> list = new ArrayList<>();
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ThreadPool threadPool = new ThreadPool(
                5,
                10,
                "yf-thread-pool",
                new ThreadFactory("yf-thread",true,true,5),
                new LinkedBlockingQueue(1000),
                new CallerRunsStrategy()
        );
        for(int i = 0;i<16;i++){
            threadPool.execute(()->{
                System.out.println(Thread.currentThread().getName());
            });
            Thread.sleep(6000);
        }
        Thread.sleep(10000);
        System.out.println("main输出");

    }
}
