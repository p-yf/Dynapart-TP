package com.yf.monitor.controller;

import com.yf.pool.constant.OfQueue;
import com.yf.pool.constant.OfRejectStrategy;
import com.yf.pool.entity.PoolInfo;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.taskqueue.TaskQueue;
import com.yf.pool.threadpool.ThreadPool;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;

/**
 * @author yyf
 * @description
 */
@RestController
@RequestMapping("/monitor")
@AllArgsConstructor
public class MonitorController {

    private ThreadPool threadPool;
    private ApplicationContext context;


    /**
     * 获取线程池的信息
     * return PoolInfo 线程池信息
     */
    @GetMapping("/pool")
    public PoolInfo getThreadPoolInfo() {
        return threadPool.getThreadPoolInfo();
    }

    /**
     * 获取队列中任务数量
     * return int 队列中任务数量
     */
    @GetMapping("/tasks")
    public int getQueueSize() {
        return threadPool.getQueueSize();
    }


    /**
     * 更改worker相关的参数
      * return true/false 表示成功与否
     */
    @PutMapping("/worker")
    public Boolean changeWorkerParams(Integer coreNums,
                                      Integer maxNums,
                                      Boolean coreDestroy,
                                      Integer aliveTime,
                                      Boolean isDaemon) {
        return threadPool.changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime,isDaemon);
    }

    /**
     * 改变队列
     * return true/false 示成功与否
     */
    @PutMapping("/queue")
    public Boolean changeQ(String qName,Integer qCapacity) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if(qName==null|| qName.isEmpty()){
            return false;
        }
        TaskQueue q;
        try {
            q = (TaskQueue) context.getBean(qName);
        }catch(NoSuchBeanDefinitionException e){
            if(!OfQueue.TASK_QUEUE_MAP.containsKey(qName)){
                return false;
            }else{
                q = (TaskQueue) OfQueue.TASK_QUEUE_MAP.get(qName).getConstructor(Integer.class).newInstance(qCapacity);
            }
        }
        return threadPool.changeQueue(q,qName);
    }

    /**
     * 改变拒绝策略
     * return true/false 表示成功与否
     */
    @PutMapping("/rejectStrategy")
    public Boolean changeRS(String rsName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if(rsName == null|| rsName.isEmpty()){
            return false;
        }
        RejectStrategy rs;
        try {
            rs = (RejectStrategy) context.getBean(rsName);
        }catch(NoSuchBeanDefinitionException e){
            if(!OfRejectStrategy.REJECT_STRATEGY_MAP.containsKey(rsName)){
                return false;
            }else{
                rs = (RejectStrategy) OfRejectStrategy.REJECT_STRATEGY_MAP.get(rsName).getConstructor().newInstance();
            }
        }
        return threadPool.changeRejectStrategy(rs,rsName);
    }

}
