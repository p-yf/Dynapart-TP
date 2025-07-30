package com.yf.monitor.springboot_integration;

import com.yf.monitor.controller.MonitorController;
import com.yf.pool.springboot_integration.AutoConfiguration.ThreadPoolConfiguration;
import com.yf.pool.springboot_integration.AutoConfiguration.ThreadPoolProperties;
import com.yf.pool.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@AutoConfigureAfter(ThreadPoolConfiguration.class)
@ConditionalOnProperty(prefix = "fy.thread-pool.monitor", name = "enabled", havingValue = "true")
public class WebAutoConfiguration {
    @Bean
    public MonitorController monitorController(ThreadPool threadPool, ThreadPoolProperties threadPoolProperties){
        return new MonitorController(threadPool,threadPoolProperties);
    }
}
