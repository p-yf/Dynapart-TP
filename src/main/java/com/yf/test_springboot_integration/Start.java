package com.yf.test_springboot_integration;

import com.yf.pool.command.PoolCommandHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author yyf
 * @description
 */
@SpringBootApplication
public class Start {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Start.class, args);
//        //命令行启动的流程  springboot环境不建议，建议用可视化界面。
//        PoolCommandHandler poolCommandHandler = new PoolCommandHandler(run.getBean(com.yf.pool.threadpool.ThreadPool.class));
//        poolCommandHandler.start();
    }
}
