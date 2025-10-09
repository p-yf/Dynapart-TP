package com.yf.test_or_explore_springboot_integration;

import com.yf.core.threadpool.ThreadPool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/**
 * @author yyf
 * @description
 */
@EnableScheduling
@SpringBootApplication
public class Start {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Start.class, args);
        //命令行启动的流程  springboot环境不建议，建议用可视化界面。
        Map<String, ThreadPool> beansOfType = run.getBeansOfType(ThreadPool.class);
//        new Thread(()->{
//            for(ThreadPool threadPool:beansOfType.values()){
//                if(threadPool.getName().equals("TP-ThreadPool1")){
//                    //io类型线程池,设置io任务,一次100个任务
//                    while(true) {
//                        for (int i = 0; i < 100; i++) {
//                            threadPool.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        Thread.sleep(700);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            }
//        },"io线程池").start();
//        new Thread(()->{
//            for(ThreadPool threadPool:beansOfType.values()){
//                if(threadPool.getName().equals("TP-ThreadPool2")){
//                    //cpu类型线程池,设置cpu任务,一次50个任务
//                    while(true) {
//                        for (int i = 0; i < 50; i++) {
//                            threadPool.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    // 模拟CPU密集型计算（如复杂运算、数据处理）
//                                    long result = 0;
//                                    for (long j = 0; j < 10_000_000L; j++) { // 大量循环计算
//                                        result += j;
//                                    }
//                                    // 避免JIT优化掉无意义计算（可选）
//                                    if (result < 0) {
//                                        System.out.print("");
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            }
//        },"cpu线程池").start();
//
//        PoolCommandHandler poolCommandHandler = new PoolCommandHandler();
//        poolCommandHandler.start();

    }
}
