package com.yf.core.partition;

import lombok.Getter;
import lombok.Setter;


/**
 * @author yyf
 * @description
 */
/**
 * 实现类需要保证线程安全
 */
public abstract class Partition<T> {


    /**
     * 添加
     *
     * @param t@return
     */
    public abstract boolean offer(T t);

    /**
     * 获取
      * @return
     */
    public abstract T poll(Integer waitTime) throws InterruptedException;

    /**
     * 移除任务(用于丢弃策略)
     */
    public abstract T removeEle();


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

    /**
     *这是用来保证队列在切换后的队列感知问题
     * 标记为被切换
     */
    public abstract void markAsSwitched();

}
