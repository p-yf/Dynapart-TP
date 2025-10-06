package com.yf.springboot_integration.pool.auto_configuration;

import com.yf.common.constant.Logo;
import com.yf.springboot_integration.pool.post_processor.ResourceRegisterPostProcessor;
import com.yf.springboot_integration.pool.post_processor.TPRegisterPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author yyf
 * @date 2025/10/6 21:49
 * @description
 */
@Slf4j
@AutoConfiguration
public class PostProcessorAutoConfiguration {
    @Bean
    public ResourceRegisterPostProcessor registerPostProcessor() {
        log.info(Logo.LOG_LOGO+"资源扫描器已经装配");
        return new ResourceRegisterPostProcessor();
    }

    @Bean
    public TPRegisterPostProcessor tpRegisterPostProcessor(DefaultListableBeanFactory beanFactory) {
        log.info(Logo.LOG_LOGO+"线程池扫描器已经装配");
        return new TPRegisterPostProcessor(beanFactory);
    }
}
