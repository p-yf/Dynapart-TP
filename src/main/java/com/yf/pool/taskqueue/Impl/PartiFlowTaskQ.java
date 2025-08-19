package com.yf.pool.taskqueue.Impl;

import com.yf.pool.taskqueue.Impl.parti_flow.PartiFlow;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.OfferStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.PollStrategy;
import com.yf.pool.taskqueue.Impl.parti_flow.strategy.RemoveStrategy;
import com.yf.pool.taskqueue.TaskQueue;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yyf
 * @date 2025/8/9 10:49
 * @description
 */
@Getter
@Setter
public class PartiFlowTaskQ extends TaskQueue {
    private PartiFlow<Runnable> partiFlow;

    public PartiFlowTaskQ(Integer partitionNum, Integer capacity, OfferStrategy offerStrategy, PollStrategy pollStrategy, RemoveStrategy removeStrategy) {
        partiFlow = new PartiFlow<>(partitionNum,capacity,offerStrategy,pollStrategy,removeStrategy);
    }

    public PartiFlowTaskQ(Integer partitionNum, Integer capacity) {
        partiFlow = new PartiFlow<>(partitionNum,capacity);
    }

    public PartiFlowTaskQ(Integer capacity) {
        partiFlow = new PartiFlow<>(capacity);
    }

    @Override
    public Boolean offer(Runnable task) {
        return partiFlow.offer( task);
    }

    @Override
    public Runnable getTask(Integer waitTime) throws InterruptedException {
        return partiFlow.poll(waitTime);
    }

    @Override
    public Boolean removeTask() {
        return partiFlow.removeElement();
    }

    @Override
    public int getExactTaskNums() {
        return partiFlow.getExactElementNums();
    }

    @Override
    public int getTaskNums() {
        return partiFlow.getElementNums();
    }

    @Override
    public void lockGlobally() {
        partiFlow.lockGlobally();
    }

    @Override
    public void unlockGlobally() {
        partiFlow.unlockGlobally();
    }

    @Override
    public Integer getCapacity() {
        return partiFlow.getCapacity();
    }

    @Override
    public void setCapacity(Integer capacity) {
        partiFlow.setCapacity(capacity);
    }


}
