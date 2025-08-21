package com.yf.pool.partition;

import lombok.Getter;
import lombok.Setter;


/**
 * @author yyf
 * @description
 */
/**
 * 实现类需要保证线程安全
 */
@Getter
@Setter
public abstract class Partition<T> {


    /**
     * 添加任务
     *
     * @param t@return
     */
    public Boolean addEle(T t){
        Boolean offer = offer(t);
        warning();
        return offer;
    };

    /**
     * 添加任务的方法
     * @return
     */
    public abstract Boolean offer(T t);

    /**
     * 警告
     */
    public void warning(){};

    /**
     * 获取任务
      * @return
     */
    public abstract T getEle(Integer waitTime) throws InterruptedException;

    /**
     * 移除任务(用于丢弃策略)
     */
    public abstract Boolean removeEle();


    /**
     * 获取任务数量
     */
    public abstract int getEleNums();//获取任务数量，无锁

    /**
     * 获取全局锁
     * @return
     */
    public abstract void lockGlobally();

    public abstract void unlockGlobally();

    public abstract Integer getCapacity();

    public abstract void setCapacity(Integer capacity);
}
