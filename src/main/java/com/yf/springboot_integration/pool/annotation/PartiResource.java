package com.yf.springboot_integration.pool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


/**
 * @author yyf
 * @description springboot环境下，扩展组件资源只需要加上当前包的注解就行了。
 *  但是注意：虽然这个注解用了@Component但是并不意味着这个类会被spring容器，而是会注册到作者写的对应的各种resource_manager中
 *  例如被当前这个注解标注就会自动在PartiResourceManager中注册，其原理是利用@Component注解借助springboot的扫描，使用了
 *  工厂bean后处理器，用来获取资源的beanDefinition，然后注册到作者的资源管理器中，并且移除这个beanDefinition，这样springboot
 *  就不会去创建和管理这个bean了
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PartiResource {//分区资源注解
    String value();// 队列名称
}
