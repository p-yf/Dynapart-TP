package com.yf.springboot_integration.monitor.auto_configuration;

import com.yf.springboot_integration.monitor.ws.SchedulePushInfoService;
import com.yf.springboot_integration.monitor.ws.ThreadPoolWebSocketHandler;
import com.yf.springboot_integration.pool.auto_configuration.ThreadPoolConfiguration;
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


/**
 * @author yyf
 * @description
 */
@Slf4j
@EnableScheduling
@EnableWebSocket
@AutoConfiguration
@AutoConfigureAfter(ThreadPoolConfiguration.class)
@ConditionalOnProperty(prefix = "yf.thread-pool.monitor", name = "enabled", havingValue = "true")
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
