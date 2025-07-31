package com.yf.pool.entity;

import lombok.Data;

/**
 * 格式与ThreadProperties一致，另外创建这个的原因是为了与springboot相关的解耦，插拔式选择是否需要springboot集成
 */
@Data
public class PoolInfo {
    public  Integer coreNums;//核心线程数
    public  Integer maxNums;//最大线程数
    public  String poolName;//线程池名称
    public  String threadName;//线程名称
    public  Boolean isDaemon;//是否守护线程
    public  Boolean coreDestroy;//核心线程是否可销毁
    public  Integer aliveTime;//线程空闲时间
    public  String queueName;//队列名称
    public  Integer queueCapacity;//队列容量
    public  String rejectStrategyName;//拒绝策略名称

}
