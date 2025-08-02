package com.yf.monitor.springboot_integration;

import com.yf.monitor.ws.SchedulePushInfoService;
import com.yf.monitor.ws.ThreadPoolWebSocketHandler;
import com.yf.pool.springboot_integration.AutoConfiguration.ThreadPoolConfiguration;
import com.yf.pool.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@EnableScheduling
@EnableWebSocket
@AutoConfiguration
@AutoConfigureAfter(ThreadPoolConfiguration.class)
@ConditionalOnProperty(prefix = "fy.thread-pool.monitor", name = "enabled", havingValue = "true")
public class WsAutoConfiguration implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ThreadPoolWebSocketHandler(), "/monitor/threads")
                .setAllowedOrigins("*");
    }

    @Bean
    public SchedulePushInfoService schedulePushInfoService(ThreadPool threadPool){
        return new SchedulePushInfoService(threadPool);
    }
}
