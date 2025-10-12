package com.yf.springboot_integration.pool.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author yyf
 * @date 2025/10/6 17:18
 * @description: GCTaskResource:用来标识
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GCTResource {
    String bindingPartiResource() default  "";//绑定的分区的名称
    String bindingSPResource() default  "";//绑定的调度规则的名称
    String spType() default  "";//绑定的调度规则的类型
}
