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
     * 新增：获取所有线程池基本信息列表
     */
    @GetMapping("/pools")
    public List<PoolInfo> getAllPools() {
        return UnifiedTPRegulator.getAllThreadPoolInfo();
    }

    /**
     * 新增：获取所有线程池名称
     */
    @GetMapping("/poolNames")
    public List<String> getAllPoolNames() {
        return UnifiedTPRegulator.getAllThreadPoolNames();
    }


    /**
     * 获取线程池的信息
     * return PoolInfo 线程池信息
     */
    @GetMapping("/pool")
    public PoolInfo getThreadPoolInfo(@RequestParam(required = true) String tpName) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null ? threadPool.getThreadPoolInfo() : null;
    }

    /**
     * 获取队列中总任务数量
     * return int 队列中任务数量
     */
    @GetMapping("/tasks")
    public int getQueueSize(@RequestParam(required = true) String tpName) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null ? threadPool.getTaskNums() : 0;
    }

    /**
     * 获取各个分区任务数量
     * @return
     */
    @GetMapping("/partitionTaskNums")
    public Map<Integer,Integer> getPartitionTaskNums(@RequestParam(required = true) String tpName) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null ? threadPool.getPartitionTaskNums() : null;
    }

    /**
     * 获取队列信息
     */
    @GetMapping("/queue")
    public QueueInfo getQueueInfo(@RequestParam(required = true) String tpName) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null ? threadPool.getQueueInfo() : null;
    }

    /**
     * 获取所有队列名称
     */
    @GetMapping("/queueName")
    public List<String> getAllQueueName(@RequestParam(required = true) String tpName) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null ? threadPool.getAllQueueName() : null;
    }

    /**
     * 获取所有拒绝策略名称
     */
    @GetMapping("/rejectStrategyName")
    public List<String> getAllRejectStrategyName(@RequestParam(required = true) String tpName) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null ? threadPool.getAllRejectStrategyName() : null;
    }


    /**
     * 更改worker相关的参数
     * return true/false 表示成功与否
     */
    @PutMapping("/worker")
    public Boolean changeWorkerParams(
            @RequestParam(required = true) String tpName,
            Integer coreNums,
            Integer maxNums,
            Boolean coreDestroy,
            Integer aliveTime,
            Boolean isDaemon) {
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        return threadPool != null && threadPool.changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime, isDaemon);
    }

    /**
     * 改变队列（修复：将Post改为Put，与文档一致）
     * return true/false 示成功与否
     */
    @PutMapping("/queue")
    public Boolean changeQ(
            @RequestParam(required = true) String tpName,
            @RequestBody QueueInfo queueInfo) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (queueInfo.getQueueName() == null || queueInfo.getQueueName().isEmpty()) {
            return false;
        }
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        if (threadPool == null) {
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
                         SPResourceManager.getOfferResource(queueInfo.getOfferPolicy()).getConstructor().newInstance(),
                         SPResourceManager.getPollResource(queueInfo.getPollPolicy()).getConstructor().newInstance(),
                         SPResourceManager.getRemoveResource(queueInfo.getRemovePolicy()).getConstructor().newInstance());
            }
        }
        return threadPool.changeQueue(q, queueInfo.getQueueName());
    }

    /**
     * 改变拒绝策略
     * return true/false 表示成功与否
     */
    @PutMapping("/rejectStrategy")
    public Boolean changeRS(
            @RequestParam(required = true) String tpName,
            @RequestParam(required = true) String rsName) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (rsName == null || rsName.isEmpty()) {
            return false;
        }
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName);
        if (threadPool == null) {
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
        return threadPool.changeRejectStrategy(rs, rsName);
    }

}
