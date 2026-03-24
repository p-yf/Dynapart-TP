package com.yf.core.resource_container.scanned_annotation;

import java.lang.annotation.*;

/**
 * @author yyf
 * @description GCTask资源注解
 *
 * 用于标识自定义GCTask实现类，注册到GCTaskManager
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GCTResource {
    String bindingPartiResource() default  "";//绑定的分区队列的名称
    String bindingSPResource() default  "";//绑定的调度策略的名称
    String spType() default  "";//绑定的调度策略的类型
}
