package com.yf.test_or_explore_springboot_integration.thread_pool;

import com.yf.common.constant.Constant;
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
                Constant.IO,
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
                Constant.CPU,
                5,
                10,
                "TP-ThreadPool2",
                new WorkerFactory("", false, true, 10),
                new LinkedBlockingQ<Runnable>(50),
                new CallerRunsStrategy()
        );
    }

}
