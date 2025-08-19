package com.yf.clustering.node;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author yyf
 * @date 2025/8/20 0:17
 * @description
 */
@SpringBootApplication
@EnableScheduling
public class Start {
    public static void main(String[] args) {
        SpringApplication.run(Start.class, args);
    }
}
