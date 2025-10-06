package com.yf.test_springboot_integration.thread_pool;

import com.yf.core.partition.Impl.LinkedBlockingQ;
import com.yf.core.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.workerfactory.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yyf
 * @date 2025/10/6 20:34
 * @description
 */
@Configuration
public class TPConfig {
    @Bean
    public ThreadPool threadPool1(){
        return new ThreadPool(
                5,
                10,
                "TP-ThreadPool1",
                new WorkerFactory("", false, true, 10),
                new LinkedBlockingQ<Runnable>(50),
                new CallerRunsStrategy()
        );
    }
    @Bean
    public ThreadPool threadPool2(){
        return new ThreadPool(
                5,
                10,
                "TP-ThreadPool2",
                new WorkerFactory("", false, true, 10),
                new LinkedBlockingQ<Runnable>(50),
                new CallerRunsStrategy()
        );
    }
    @Bean
    public ThreadPool threadPool3(){
        return new ThreadPool(
                5,
                10,
                "TP-ThreadPool3",
                new WorkerFactory("", false, true, 10),
                new LinkedBlockingQ<Runnable>(50),
                new CallerRunsStrategy()
        );
    }
    @Bean
    public ThreadPool threadPool4(){
        return new ThreadPool(
                5,
                10,
                "TP-ThreadPool4",
                new WorkerFactory("", false, true, 10),
                new LinkedBlockingQ<Runnable>(50),
                new CallerRunsStrategy()
        );
    }
}
