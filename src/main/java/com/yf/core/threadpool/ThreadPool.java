package com.yf.core.threadpool;

import com.yf.common.constant.Logo;
import com.yf.common.entity.PoolInfo;
import com.yf.common.entity.QueueInfo;
import com.yf.core.partition.Impl.partitioning.PartiFlow;
import com.yf.core.resource_manager.PartiResourceManager;
import com.yf.core.resource_manager.RSResourceManager;
import com.yf.core.resource_manager.SPResourceManager;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.partition.Partition;
import com.yf.core.workerfactory.WorkerFactory;
import com.yf.core.worker.Worker;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import static com.yf.common.constant.OfWorker.CORE;
import static com.yf.common.constant.OfWorker.EXTRA;


/**
 * @author yyf
 * @description
 */
@Getter
@Setter
public class ThreadPool {
    static {
        System.out.println(Logo.START_LOGO);
    }
    private WorkerFactory workerFactory;
    private Partition<Runnable> partition;
    private RejectStrategy rejectStrategy;
    private volatile Integer coreNums;//核心线程数
    private volatile Integer maxNums;//最大线程数
    private Set<Worker> coreList = ConcurrentHashMap.newKeySet();
    private Set<Worker> extraList = ConcurrentHashMap.newKeySet();
    private final AtomicInteger coreWorkerCount = new AtomicInteger(0); // 核心线程存活数
    private final AtomicInteger extraWorkerCount = new AtomicInteger(0); // 额外线程存活数

    private String name;//线程池名称

    public ThreadPool(
            Integer coreNums, Integer maxNums, String name,
            WorkerFactory threadFactory, Partition<Runnable> partition,
            RejectStrategy rejectStrategy
    ) {
        this.workerFactory = threadFactory;
        threadFactory.setThreadName(name + ":" + threadFactory.getThreadName());
        threadFactory.setThreadPool(this);
        this.partition = partition;
        this.rejectStrategy = rejectStrategy;
        this.coreNums = coreNums;
        this.maxNums = maxNums;
        this.name = name;
    }

    public void execute(Runnable task) {
        if (coreWorkerCount.get() < coreNums) {
            if (addWorker(task,true)) {
                return;
            }
        }
        if (partition.offer(task)) {
            if(coreNums==0){
                addWorker(task,false);
            }
            return;
        }
        if (addWorker(task,false)) {
            return;
        }
        rejectStrategy.reject(this,task);
    }


    public <T> Future<T> submit(Callable<T> callable) {
        if (callable == null) throw new NullPointerException();
        FutureTask<T> ftask = new FutureTask<>(callable);
        execute(ftask);
        return ftask;
    }


    private boolean addWorker(Runnable task, boolean isCore) {
        // 循环CAS重试：直到成功或确定无法创建线程
        while (true) {
            if (isCore) {
                int current = coreWorkerCount.get();
                if (current >= coreNums) {
                    return false; // 核心线程达到上限，退出
                }
                // CAS尝试更新
                if (coreWorkerCount.compareAndSet(current, current + 1)) {
                    break; // CAS成功,即将退出循环创建Worker
                }// CAS失败，继续循环重试
            } else {
                // 额外线程逻辑
                int current = extraWorkerCount.get();
                int extraMax = maxNums - coreNums;
                if (current >= extraMax) {
                    return false; // 额外线程达到上限，退出
                }
                if (extraWorkerCount.compareAndSet(current, current + 1)) {
                    break; // CAS成功，退出循环
                }// CAS失败，继续循环重试
            }
            Thread.yield();
        }
        Worker worker = null;
        try {
            worker = workerFactory.createWorker(isCore, task);
            if (isCore) {
                coreList.add(worker);
            } else {
                extraList.add(worker);
            }
            worker.startWorking();
            return true;
        } catch (Throwable e) {//异常回滚
            if (isCore) {
                coreWorkerCount.getAndDecrement();
            } else {
                extraWorkerCount.getAndDecrement();
            }
            if (worker != null) {
                if (isCore) {
                    coreList.remove(worker);
                } else {
                    extraList.remove(worker);
                }
            }
            return false;
        }
    }

//    =======================================================
//    以下是对于线程池相关参数的读写操作，用来提供监控信息和修改参数，不涉及到线程池运行过程的自动调控，所以读取信息全部无锁

    /**
     * 获取线程池中线程的信息
     *
     * @return ：String代表线程类别，Thread.State代表线程状态，Integer代表对应类别的状态的数量
     */
    public Map<String, Map<Thread.State, Integer>> getThreadsInfo() {
        Map<String, Map<Thread.State, Integer>> result = new HashMap<>();
        Map<Thread.State, Integer> coreMap = new HashMap<>();
        Map<Thread.State, Integer> extraMap = new HashMap<>();
        for (Worker worker : coreList) {
            Thread.State state = worker.getThreadState();
            if (coreMap.containsKey(state)) {
                coreMap.put(state, coreMap.get(state) + 1);
            } else {
                coreMap.put(state, 1);
            }
        }
        for (Worker worker : extraList) {
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
    public PoolInfo getThreadPoolInfo() {
        PoolInfo info = new PoolInfo();
        info.setPoolName(name);
        info.setAliveTime(workerFactory.getAliveTime());
        info.setThreadName(workerFactory.getThreadName());
        info.setCoreDestroy(workerFactory.isCoreDestroy());
        info.setDaemon(workerFactory.isUseDaemonThread());
        info.setMaxNums(maxNums);
        info.setCoreNums(coreNums);
        //获取当前队列名字
        for(Map.Entry entry : PartiResourceManager.getResources().entrySet()){
            if (entry.getValue() == partition.getClass()) {
                info.setQueueName(entry.getKey().toString());
                break;
            }
        }
        for(Map.Entry entry : RSResourceManager.REJECT_STRATEGY_MAP.entrySet()){
            if (entry.getValue() == rejectStrategy.getClass()) {
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
    public int getTaskNums() {
        return partition.getEleNums();
    }

    /**
     * 获取每个分区的任务数量
     * @return
     */
    public Map<Integer,Integer> getPartitionTaskNums(){
        Map<Integer,Integer> map = new HashMap<>();
        if(!(partition instanceof PartiFlow)){//不是分区队列
            map.put(0,partition.getEleNums());
        }else {//是分区队列
            Partition<Runnable>[] partitions = ((PartiFlow<Runnable>) partition).getPartitions();
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
    public QueueInfo getQueueInfo(){
        QueueInfo queueInfo = new QueueInfo();
        queueInfo.setCapacity(partition.getCapacity());
        if(partition instanceof PartiFlow<Runnable>){
            for(Map.Entry entry : PartiResourceManager.getResources().entrySet()){
                if (entry.getValue() == ((PartiFlow<Runnable>) partition).getPartitions()[0].getClass()) {
                    queueInfo.setQueueName(entry.getKey().toString());
                    break;
                }
            }
            queueInfo.setPartitionNum(((PartiFlow<Runnable>) partition).getPartitions().length);
            queueInfo.setPartitioning(true);
            //找到offer的名字
            for(Map.Entry entry : SPResourceManager.getOfferResources().entrySet()){
                if (entry.getValue() == ((PartiFlow<Runnable>) partition).getOfferPolicy().getClass()) {
                    queueInfo.setOfferPolicy(entry.getKey().toString());
                    break;
                }
            }
            //找到poll的名字
            for(Map.Entry entry : SPResourceManager.getPollResources().entrySet()){
                if (entry.getValue() == ((PartiFlow<Runnable>) partition).getPollPolicy().getClass()) {
                    queueInfo.setPollPolicy(entry.getKey().toString());
                    break;
                }
            }
            //找到remove的名字
            for(Map.Entry entry : SPResourceManager.getRemoveResources().entrySet()){
                if (entry.getValue() == ((PartiFlow<Runnable>) partition).getRemovePolicy().getClass()) {
                    queueInfo.setRemovePolicy(entry.getKey().toString());
                    break;
                }
            }
        }
        //获取队列的名字
        for(Map.Entry entry : PartiResourceManager.getResources().entrySet()){
            if (entry.getValue() == partition.getClass()) {
                queueInfo.setQueueName(entry.getKey().toString());
                break;
            }
        }
        return queueInfo;
    }


    /**
     * 获取所有的队列的名称
     */
    public List<String> getAllQueueName(){
        return new ArrayList<>(PartiResourceManager.getResources().keySet());
    }

    /**
     * 获取所有的拒绝策略的名称
     */
    public List<String> getAllRejectStrategyName(){
        return new ArrayList<>(RSResourceManager.getResources().keySet());
    }

    /**
     * 改变worker相关参数，直接赋值就好，会动态平衡的
     */
    public Boolean changeWorkerParams(Integer coreNums, Integer maxNums, Boolean coreDestroy, Integer aliveTime, Boolean isDaemon) {
        if (maxNums!=null&&coreNums!=null&&maxNums < coreNums) {
            return false;
        }
        int oldCoreNums = this.coreNums;
        int oldMaxNums = this.maxNums;
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
            this.maxNums = maxNums;//无论如何非核心线程都能直接改变
        }
        if(coreNums!=null) {
            this.coreNums = coreNums;
        }
        if(coreNums==null){
            if(maxNums!=null) {
                destroyWorkers(0, (oldMaxNums - oldCoreNums) - (maxNums - coreNums));
            }
        }else{
            if(maxNums==null) {
                destroyWorkers(oldCoreNums - coreNums, 0);
            }else{
                destroyWorkers(oldCoreNums - coreNums, (oldMaxNums - oldCoreNums) - (maxNums - coreNums));
            }
        }
        if (aliveTime != null) {
            this.workerFactory.setAliveTime(aliveTime);
        }
        if (coreDestroy != null) {
            this.workerFactory.setCoreDestroy(coreDestroy);
        }
        if (isDaemon != null && isDaemon != this.workerFactory.isUseDaemonThread()) {
            this.workerFactory.setUseDaemonThread(isDaemon);
            for (Worker worker : getCoreList()) {
                worker.setDaemon(isDaemon);
            }
            for (Worker worker : getExtraList()) {
                worker.setDaemon(isDaemon);
            }
        }
        return true;
    }

    //销毁线程
    public void destroyWorkers(int coreNums, int extraNums) {//销毁的数量
        if (coreNums > 0) {
            int i = 0;
            for (Worker worker : getCoreList()) {
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
            for (Worker worker : getExtraList()) {
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
    public Boolean changeQueue(Partition<Runnable> q, String qName){
        if(q == null|| qName == null){
            return false;
        }
        Partition<Runnable> oldQ = partition;
        try {
            oldQ.lockGlobally();
            while(oldQ.getEleNums() > 0){
                Runnable task = oldQ.poll(1000);
                q.offer(task);
            }
            this.partition = q;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            oldQ.unlockGlobally();
        }
        return true;
    }

    /**
     * 改变拒绝策略,默认与拒绝策略无共享变量需要争抢，所以线程安全，不需要加锁
     */
    public Boolean changeRejectStrategy(RejectStrategy rejectStrategy,String rejectStrategyName){
        if(rejectStrategy == null|| rejectStrategyName == null){
            return false;
        }
        this.rejectStrategy = rejectStrategy;
        return true;
    }




}
