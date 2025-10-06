package com.yf.springboot_integration.monitor.controller;

import com.yf.core.resource_manager.PartiResourceManager;
import com.yf.core.resource_manager.RSResourceManager;
import com.yf.core.resource_manager.SPResourceManager;
import com.yf.common.entity.PoolInfo;
import com.yf.common.entity.QueueInfo;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.partition.Partition;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
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

    private ApplicationContext context;
    /**
     * 获取线程池的信息
     * return PoolInfo 线程池信息
     */
    @GetMapping("/pool")
    public PoolInfo getThreadPoolInfo(String tpName) {
        return UnifiedTPRegulator.getResource(tpName).getThreadPoolInfo();
    }

    /**
     * 获取队列中总任务数量
     * return int 队列中任务数量
     */
    @GetMapping("/tasks")
    public int getQueueSize(String tpName) {
        return UnifiedTPRegulator.getResource(tpName).getTaskNums();
    }

    /**
     * 获取各个分区任务数量
     * @return
     */
    @GetMapping("/partitionTaskNums")
    public Map<Integer,Integer> getPartitionTaskNums(String tpName) {
        return UnifiedTPRegulator.getResource(tpName).getPartitionTaskNums();
    }

    /**
     * 获取队列信息
     */
    @GetMapping("/queue")
    public QueueInfo getQueueInfo(String tpName) {
        return UnifiedTPRegulator.getResource(tpName).getQueueInfo();
    }

    /**
     * 获取所有队列名称
     */
    @GetMapping("/queueName")
    public List<String> getAllQueueName(String tpName) {
        return UnifiedTPRegulator.getResource(tpName).getAllQueueName();
    }

    /**
     * 获取所有拒绝策略名称
     */
    @GetMapping("/rejectStrategyName")
    public List<String> getAllRejectStrategyName(String tpName) {
        return UnifiedTPRegulator.getResource(tpName).getAllRejectStrategyName();
    }


    /**
     * 更改worker相关的参数
     * return true/false 表示成功与否
     */
    @PutMapping("/worker")
    public Boolean changeWorkerParams(String tpName,
                                      Integer coreNums,
                                      Integer maxNums,
                                      Boolean coreDestroy,
                                      Integer aliveTime,
                                      Boolean isDaemon) {
        return UnifiedTPRegulator.getResource(tpName).changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime, isDaemon);
    }

    /**
     * 改变队列
     * return true/false 示成功与否
     */
    @PostMapping("/queue")
    public Boolean changeQ(String tpName,@RequestBody QueueInfo queueInfo) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (queueInfo.getQueueName() == null || queueInfo.getQueueName().isEmpty()) {
            return false;
        }
        Partition q;
        if (!PartiResourceManager.getResources().containsKey(queueInfo.getQueueName())) {//队列不存在
            return false;
        } else {//队列存在
            if(!queueInfo.isPartitioning()) {//不分区
                q = PartiResourceManager.getResources().get(queueInfo.getQueueName()).getConstructor(Integer.class).newInstance(queueInfo.getCapacity());
            }else{//分区
                q = new PartiFlow(queueInfo.getPartitionNum(), queueInfo.getCapacity(), queueInfo.getQueueName(),
                        (OfferPolicy) SPResourceManager.getOfferResource(queueInfo.getOfferPolicy()).getConstructor().newInstance(),
                        (PollPolicy) SPResourceManager.getPollResource(queueInfo.getPollPolicy()).getConstructor().newInstance(),
                        (RemovePolicy) SPResourceManager.getRemoveResource(queueInfo.getRemovePolicy()).getConstructor().newInstance());
            }
        }
        return UnifiedTPRegulator.getResource(tpName).changeQueue(q, queueInfo.getQueueName());
    }

    /**
     * 改变拒绝策略
     * return true/false 表示成功与否
     */
    @PutMapping("/rejectStrategy")
    public Boolean changeRS(String tpName,String rsName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (rsName == null || rsName.isEmpty()) {
            return false;
        }
        RejectStrategy rs;
        try {
            rs = (RejectStrategy) context.getBean(rsName);
        } catch (NoSuchBeanDefinitionException e) {
            if (!RSResourceManager.REJECT_STRATEGY_MAP.containsKey(rsName)) {
                return false;
            } else {
                rs = (RejectStrategy) RSResourceManager.REJECT_STRATEGY_MAP.get(rsName).getConstructor().newInstance();
            }
        }
        return UnifiedTPRegulator.getResource(tpName).changeRejectStrategy(rs, rsName);
    }

}
