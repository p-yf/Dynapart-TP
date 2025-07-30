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
        ThreadPool threadPool = new ThreadPool(new ThreadFactory("yf",false,false,2),
                new LinkedBlockingQueue(5),new CallerRunsStrategy(),5,10,"yf");
        for(int i = 0;i<16;i++){
            new Thread(() -> {
                Future fu = threadPool.submit(() -> {
                    System.out.println(Thread.currentThread().getName());
                    return 1;
                });
                list.add(fu);
            }).start();
        }
        Thread.sleep(10000);
        System.out.println("main输出");
        for(int i = 0;i<100;i++){
            System.out.println(list.get(i).get());
        }
    }
}
