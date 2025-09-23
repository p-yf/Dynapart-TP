package com.yf.common.entity;

import lombok.Data;


/**
 * @author yyf
 * @description
 */
/**
 * 格式与ThreadProperties一致，另外创建这个的原因是为了与springboot相关的解耦，插拔式选择是否需要springboot集成
 */
@Data
public class PoolInfo {
    private  Integer coreNums;//核心线程数
    private  Integer maxNums;//最大线程数
    private  String poolName;//线程池名称
    private  String threadName;//线程名称
    private  Boolean isDaemon;//是否守护线程
    private  Boolean coreDestroy;//核心线程是否可销毁
    private  Integer aliveTime;//线程空闲时间
    private  String queueName;//队列名称
    private  String rejectStrategyName;//拒绝策略名称

}
