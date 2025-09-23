package com.yf.springboot_integration.monitor.controller;

import com.yf.pool.constant_or_registry.QueueManager;
import com.yf.pool.constant_or_registry.RejectStrategyManager;
import com.yf.pool.constant_or_registry.SchedulePolicyManager;
import com.yf.pool.entity.PoolInfo;
import com.yf.pool.entity.QueueInfo;
import com.yf.partition.Impl.partitioning.PartiFlow;
import com.yf.partition.Impl.partitioning.strategy.OfferPolicy;
import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.partition.Partition;
import com.yf.pool.threadpool.ThreadPool;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

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
     * 获取队列中总任务数量
     * return int 队列中任务数量
     */
    @GetMapping("/tasks")
    public int getQueueSize() {
        return threadPool.getTaskNums();
    }

    /**
     * 获取各个分区任务数量
     * @return
     */
    @GetMapping("/partitionTaskNums")
    public Map<Integer,Integer> getPartitionTaskNums() {
        return threadPool.getPartitionTaskNums();
    }

    /**
     * 获取队列信息
     */
    @GetMapping("/queue")
    public QueueInfo getQueueInfo() {
        return threadPool.getQueueInfo();
    }

    /**
     * 获取所有队列名称
     */
    @GetMapping("/queueName")
    public List<String> getAllQueueName() {
        return threadPool.getAllQueueName();
    }

    /**
     * 获取所有拒绝策略名称
     */
    @GetMapping("/rejectStrategyName")
    public List<String> getAllRejectStrategyName() {
        return threadPool.getAllRejectStrategyName();
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
        return threadPool.changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime, isDaemon);
    }

    /**
     * 改变队列
     * return true/false 示成功与否
     */
    @PostMapping("/queue")
    public Boolean changeQ(@RequestBody QueueInfo queueInfo) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (queueInfo.getQueueName() == null || queueInfo.getQueueName().isEmpty()) {
            return false;
        }
        Partition q;
        if (!QueueManager.getResources().containsKey(queueInfo.getQueueName())) {//队列不存在
            return false;
        } else {//队列存在
            if(!queueInfo.isPartitioning()) {//不分区
                q = QueueManager.getResources().get(queueInfo.getQueueName()).getConstructor(Integer.class).newInstance(queueInfo.getCapacity());
            }else{//分区
                q = new PartiFlow(queueInfo.getPartitionNum(), queueInfo.getCapacity(), queueInfo.getQueueName(),
                        (OfferPolicy) SchedulePolicyManager.getOfferResource(queueInfo.getOfferPolicy()).getConstructor().newInstance(),
                        (PollPolicy) SchedulePolicyManager.getPollResource(queueInfo.getPollPolicy()).getConstructor().newInstance(),
                        (RemovePolicy) SchedulePolicyManager.getRemoveResource(queueInfo.getRemovePolicy()).getConstructor().newInstance());
            }
        }
        return threadPool.changeQueue(q, queueInfo.getQueueName());
    }

    /**
     * 改变拒绝策略
     * return true/false 表示成功与否
     */
    @PutMapping("/rejectStrategy")
    public Boolean changeRS(String rsName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (rsName == null || rsName.isEmpty()) {
            return false;
        }
        RejectStrategy rs;
        try {
            rs = (RejectStrategy) context.getBean(rsName);
        } catch (NoSuchBeanDefinitionException e) {
            if (!RejectStrategyManager.REJECT_STRATEGY_MAP.containsKey(rsName)) {
                return false;
            } else {
                rs = (RejectStrategy) RejectStrategyManager.REJECT_STRATEGY_MAP.get(rsName).getConstructor().newInstance();
            }
        }
        return threadPool.changeRejectStrategy(rs, rsName);
    }

}
