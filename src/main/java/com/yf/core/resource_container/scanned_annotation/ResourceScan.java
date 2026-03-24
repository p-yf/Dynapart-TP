package com.yf.core.resource_container.scanned_annotation;

import java.lang.annotation.*;

/**
 * @author yyf
 * @description 资源扫描注解
 *
 * 标记在入口类上，启用该类所在包及子包下的资源自动扫描注册
 *
 * 使用方式：
 * <pre>
 * // 在SpringBootApplication入口类上添加注解即可自动扫描
 * &#64;ResourceScan
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * 或者在非Spring环境：
 * <pre>
 * &#64;ResourceScan
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         // 首次访问任何DynaPart组件时自动完成扫描
 *         ThreadPool tp = new ThreadPool(...);
 *     }
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResourceScan {
}
