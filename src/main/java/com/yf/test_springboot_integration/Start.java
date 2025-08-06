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
        PoolCommandHandler poolCommandHandler = new PoolCommandHandler(run.getBean(com.yf.pool.threadpool.ThreadPool.class));
        poolCommandHandler.start();
    }
}
