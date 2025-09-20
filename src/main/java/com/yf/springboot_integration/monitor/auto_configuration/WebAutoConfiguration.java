package com.yf.springboot_integration.monitor.auto_configuration;

import com.yf.springboot_integration.monitor.controller.MonitorController;
import com.yf.springboot_integration.monitor.properties.MonitorProperties;
import com.yf.springboot_integration.pool.auto_configuration.ThreadPoolAutoConfiguration;
import com.yf.pool.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * @author yyf
 * @description
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MonitorProperties.class)
@AutoConfigureAfter(ThreadPoolAutoConfiguration.class)
@ConditionalOnProperty(prefix = "yf.thread-pool.monitor", name = "enabled", havingValue = "true")
public class WebAutoConfiguration implements WebMvcConfigurer{
    @Bean
    public MonitorController monitorController(ThreadPool threadPool, ApplicationContext context){
        return new MonitorController(threadPool,context);
    }
    //开启静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

}
