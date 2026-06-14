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

    /**
     * 将本队列中的所有元素迁移到目标队列。
     * 设计目的:队列被 markAsSwitched 之后,普通 offer/poll 都会抛 SwitchedException,
     * 此时如果还想把残留任务从旧队列转移到新队列,就需要一个不检查 switched 的专用通道。
     *
     * 语义约定:
     * 1. 不检查 switched 状态 —— 即使本队列已被切换,也能继续迁移
     * 2. 一次性尽量把当前所有元素转移到 target
     * 3. 若 target 已满(返回 false),则把元素重新放回本队列头部并停止迁移(不丢失任务)
     * 4. 返回实际成功迁移的元素个数
     * 5. 实现需保证线程安全
     *
     * @param target 接收迁移元素的目标队列
     * @return 实际迁移成功的元素个数
     */
    public abstract int drainTo(Partition<T> target);

}
