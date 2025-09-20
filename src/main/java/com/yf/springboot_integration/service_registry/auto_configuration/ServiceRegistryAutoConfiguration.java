package com.yf.springboot_integration.service_registry.auto_configuration;

import com.yf.pool.threadpool.ThreadPool;
import com.yf.springboot_integration.service_registry.ServiceRegistryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author yyf
 * @date 2025/9/20 19:46
 * @description
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "yf.thread-pool.service-registry",name = "enabled",havingValue = "true")
@EnableScheduling
public class ServiceRegistryAutoConfiguration {

    @Bean
    public ServiceRegistryHandler serviceRegistryHandler
            (StringRedisTemplate srt, ResourceLoader rl, ThreadPool tp, ServerProperties sp){
        System.out.println("))"+srt);
        System.out.println(rl);
        System.out.println(sp);
        return new ServiceRegistryHandler(srt,rl,tp,sp);
    }
}
