package com.yf.core.tp_regulator;

import com.yf.common.entity.PoolInfo;
import com.yf.common.entity.QueueInfo;
import com.yf.core.partition.Partition;
import com.yf.core.partitioning.Partitioning;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.resource_manager.GCTaskManager;
import com.yf.core.resource_manager.PartiResourceManager;
import com.yf.core.resource_manager.RSResourceManager;
import com.yf.core.resource_manager.SPResourceManager;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.worker.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yf.common.constant.OfWorker.CORE;
import static com.yf.common.constant.OfWorker.EXTRA;

/**
 * @author yyf
 * @date 2025/10/5 12:50
 * @description ：线程池动态调节类，解耦线程池核心逻辑和调节逻辑，未来将实现用单个调节对象统一管控所有的线程池
 */
public class UnifiedTPRegulator {

    private final static Map<String,ThreadPool> threadPoolMap = new HashMap<>();

    /**
     * 注册线程池
     * @param name：线程池名字
     * @param threadPool: 线程池
     */
    public static void register(String name, ThreadPool threadPool) {
        threadPoolMap.put(name, threadPool);
    }

    /**
     * 获取线程池
     * @param name:线程池名字
     * @return 线程池
     */
    public static ThreadPool getResource(String name){
        return threadPoolMap.get(name);
    }


//    =============================线程池信息和调控====================================
    /**
     * 获取线程池中线程的信息
     *
     * @return ：String代表线程类别，Thread.State代表线程状态，Integer代表对应类别的状态的数量
     */
    public static Map<String, Map<Thread.State, Integer>> getThreadsInfo(String tpName) {
        ThreadPool threadPool = threadPoolMap.get(tpName);
        Map<String, Map<Thread.State, Integer>> result = new HashMap<>();
        Map<Thread.State, Integer> coreMap = new HashMap<>();
        Map<Thread.State, Integer> extraMap = new HashMap<>();
        for (Worker worker : threadPool.getCoreList()) {
            Thread.State state = worker.getThreadState();
            if (coreMap.containsKey(state)) {
                coreMap.put(state, coreMap.get(state) + 1);
            } else {
                coreMap.put(state, 1);
            }
        }
        for (Worker worker : threadPool.getExtraList()) {
            Thread.State state = worker.getThreadState();
            if (extraMap.containsKey(state)) {
                extraMap.put(state, extraMap.get(state) + 1);
            } else {
                extraMap.put(state, 1);
            }
        }
        result.put(CORE, coreMap);
        result.put(EXTRA, extraMap);
        return result;
    }

    /**
     * 获取线程池信息
     *
     * @return
     */
    public static PoolInfo getThreadPoolInfo(String tpName) {
        ThreadPool threadPool = threadPoolMap.get(tpName);
        PoolInfo info = new PoolInfo();
        info.setPoolName(threadPool.getName());
        info.setAliveTime(threadPool.getWorkerFactory().getAliveTime());
        info.setThreadName(threadPool.getWorkerFactory().getThreadName());
        info.setCoreDestroy(threadPool.getWorkerFactory().isCoreDestroy());
        info.setDaemon(threadPool.getWorkerFactory().isUseDaemonThread());
        info.setMaxNums(threadPool.getMaxNums());
        info.setCoreNums(threadPool.getCoreNums());
        //获取当前队列名字
        for(Map.Entry entry : PartiResourceManager.getResources().entrySet()){
            if (entry.getValue() == threadPool.getPartition().getClass()) {
                info.setQueueName(entry.getKey().toString());
                break;
            }
        }
        for(Map.Entry entry : RSResourceManager.REJECT_STRATEGY_MAP.entrySet()){
            if (entry.getValue() == threadPool.getRejectStrategy().getClass()) {
                info.setRejectStrategyName(entry.getKey().toString());
                break;
            }
        }
        return info;
    }

    /**
     * 获取队列的任务数量
     * @return
     */
    public static int getTaskNums(String tpName) {
        ThreadPool threadPool = threadPoolMap.get(tpName);
        return threadPool.getPartition().getEleNums();
    }

    /**
     * 获取每个分区的任务数量
     * @return
     */
    public static Map<Integer,Integer> getPartitionTaskNums(String tpName){
        ThreadPool threadPool = threadPoolMap.get(tpName);
        Map<Integer,Integer> map = new HashMap<>();
        if(!(threadPool.getPartition() instanceof PartiFlow)){//不是分区队列
            map.put(0,threadPool.getPartition().getEleNums());
        }else {//是分区队列
            Partition<Runnable>[] partitions = ((PartiFlow<Runnable>) threadPool.getPartition()).getPartitions();
            for(int i = 0;i<partitions.length;i++){
                map.put(i,partitions[i].getEleNums());
            }
        }
        return map;
    }

    /**
     * 获取队列信息
     * @return
     */
    public static QueueInfo getQueueInfo(String tpName){
        ThreadPool threadPool = threadPoolMap.get(tpName);
        QueueInfo queueInfo = new QueueInfo();
        queueInfo.setCapacity(threadPool.getPartition().getCapacity());
        if(threadPool.getPartition() instanceof Partitioning<?>){
            Partitioning<Runnable> partition = (Partitioning<Runnable>) threadPool.getPartition();
            for(Map.Entry entry : PartiResourceManager.getResources().entrySet()){
                if (entry.getValue() == partition.getPartitions()[0].getClass()) {
                    queueInfo.setQueueName(entry.getKey().toString());
                    break;
                }
            }
            queueInfo.setPartitionNum(partition.getPartitions().length);
            queueInfo.setPartitioning(true);
            //找到offer的名字
            for(Map.Entry entry : SPResourceManager.getOfferResources().entrySet()){
                if (entry.getValue() == partition.getOfferPolicy().getClass()) {
                    queueInfo.setOfferPolicy(entry.getKey().toString());
                    break;
                }
            }
            //找到poll的名字
            for(Map.Entry entry : SPResourceManager.getPollResources().entrySet()){
                if (entry.getValue() == partition.getPollPolicy().getClass()) {
                    queueInfo.setPollPolicy(entry.getKey().toString());
                    break;
                }
            }
            //找到remove的名字
            for(Map.Entry entry : SPResourceManager.getRemoveResources().entrySet()){
                if (entry.getValue() == partition.getRemovePolicy().getClass()) {
                    queueInfo.setRemovePolicy(entry.getKey().toString());
                    break;
                }
            }
        }
        //获取队列的名字
        for(Map.Entry entry : PartiResourceManager.getResources().entrySet()){
            if (entry.getValue() == threadPool.getPartition().getClass()) {
                queueInfo.setQueueName(entry.getKey().toString());
                break;
            }
        }
        return queueInfo;
    }


    /**
     * 获取所有的队列的名称
     */
    public static List<String> getAllQueueName(){
        return new ArrayList<>(PartiResourceManager.getResources().keySet());
    }

    /**
     * 获取所有的拒绝策略的名称
     */
    public static List<String> getAllRejectStrategyName(){
        return new ArrayList<>(RSResourceManager.getResources().keySet());
    }

    /**
     * 改变worker相关参数，直接赋值就好，会动态平衡的
     */
    public static Boolean changeWorkerParams(String tpName,Integer coreNums, Integer maxNums, Boolean coreDestroy, Integer aliveTime, Boolean isDaemon) {
        ThreadPool threadPool = threadPoolMap.get(tpName);
        if (maxNums!=null&&coreNums!=null&&maxNums < coreNums) {
            return false;
        }
        int oldCoreNums = threadPool.getCoreNums();
        int oldMaxNums = threadPool.getMaxNums();
        if(coreNums!=null){
            if(maxNums==null){
                if(coreNums > oldMaxNums){
                    return false;
                }
            }
        }else{
            if(maxNums!=null&&maxNums < oldCoreNums){
                return false;
            }
        }
        if(maxNums!=null) {
            threadPool.setMaxNums(maxNums);//无论如何非核心线程都能直接改变
        }
        if(coreNums!=null) {
            threadPool.setCoreNums(coreNums);
        }
        if(coreNums==null){
            if(maxNums!=null) {
                destroyWorkers(tpName, 0, (oldMaxNums - oldCoreNums) - (maxNums - coreNums));
            }
        }else{
            if(maxNums==null) {
                destroyWorkers(tpName, oldCoreNums - coreNums, 0);
            }else{
                destroyWorkers(tpName, oldCoreNums - coreNums, (oldMaxNums - oldCoreNums) - (maxNums - coreNums));
            }
        }
        if (aliveTime != null) {
            threadPool.getWorkerFactory().setAliveTime(aliveTime);
        }
        if (coreDestroy != null) {
            threadPool.getWorkerFactory().setCoreDestroy(coreDestroy);
        }
        if (isDaemon != null && isDaemon != threadPool.getWorkerFactory().isUseDaemonThread()) {
            threadPool.getWorkerFactory().setUseDaemonThread(isDaemon);
            for (Worker worker : threadPool.getCoreList()) {
                worker.setDaemon(isDaemon);
            }
            for (Worker worker : threadPool.getExtraList()) {
                worker.setDaemon(isDaemon);
            }
        }
        return true;
    }

    //销毁worker
    public static void destroyWorkers(String tpName,int coreNums, int extraNums) {//销毁的数量
        ThreadPool threadPool = threadPoolMap.get(tpName);
        if (coreNums > 0) {
            int i = 0;
            for (Worker worker : threadPool.getCoreList()) {
                worker.setFlag(false);
                worker.lock();//防止任务执行过程中被中断
                worker.interruptWorking();
                worker.unlock();
                i++;
                if (i == coreNums) {
                    break;
                }
            }
        }
        if (extraNums > 0) {
            int j = 0;
            for (Worker worker : threadPool.getExtraList()) {
                worker.setFlag(false);
                worker.lock();
                worker.interruptWorking();
                worker.unlock();
                j++;
                if (j == extraNums) {
                    break;
                }
            }
        }
    }

    /**
     * 改变队列
     */
    public static Boolean changeQueue(String tpName,Partition<Runnable> q){
        ThreadPool threadPool = threadPoolMap.get(tpName);
        if(q == null){
            return false;
        }
        Partition<Runnable> oldQ = threadPool.getPartition();
        try {
            oldQ.lockGlobally();
            oldQ.markAsSwitched();
            threadPool.setPartition(q);
            GCTaskManager.clean(threadPool,oldQ);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            oldQ.unlockGlobally();
        }
        try {
            if (!(oldQ instanceof Partitioning)) {
                //非分区队列
                while (oldQ.getEleNums() > 0) {
                    Runnable task = oldQ.poll(1);
                    q.offer(task);
                }
            } else {
                //分区队列
                Partition<Runnable>[] partitions = ((Partitioning<Runnable>) oldQ).getPartitions();
                for (Partition<Runnable> partition : partitions) {
                    while (partition.getEleNums() > 0) {
                        Runnable task = partition.poll(1);
                        q.offer(task);
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 改变拒绝策略,默认与拒绝策略无共享变量需要争抢，所以线程安全，不需要加锁
     */
    public static Boolean changeRejectStrategy(String tpName,RejectStrategy rejectStrategy, String rejectStrategyName){
        ThreadPool threadPool = threadPoolMap.get(tpName);
        if(rejectStrategy == null|| rejectStrategyName == null){
            return false;
        }
        threadPool.setRejectStrategy(rejectStrategy);
        return true;
    }

}
