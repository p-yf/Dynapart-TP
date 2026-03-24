package com.yf.springboot_integration.monitor.controller;

import com.yf.common.constant.Constant;
import com.yf.common.glue.DynamicCompiler;
import com.yf.common.task.GCTask;
import com.yf.core.resource_container.resource_manager.GCTaskManager;
import com.yf.core.resource_container.resource_manager.PartiResourceManager;
import com.yf.core.resource_container.resource_manager.RSResourceManager;
import com.yf.core.resource_container.resource_manager.SPResourceManager;
import com.yf.common.entity.PoolInfo;
import com.yf.common.entity.QueueInfo;
import com.yf.core.partitioning.impl.PartiFlow;
import com.yf.core.partitioning.impl.PartiStill;
import com.yf.core.partitioning.schedule_policy.SchedulePolicy;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.partition.Partition;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.tp_regulator.UnifiedTPRegulator;
import com.yf.core.resource_container.scanned_annotation.GCTResource;
import com.yf.core.resource_container.scanned_annotation.PartiResource;
import com.yf.core.resource_container.scanned_annotation.RSResource;
import com.yf.core.resource_container.scanned_annotation.SPResource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yyf
 * @description 监控控制器 - 提供线程池监控API
 */
@RestController
@RequestMapping("/monitor")
@AllArgsConstructor
@Slf4j
public class MonitorController {

    private ApplicationContext context;

    /**
     * 校验线程池名称
     */
    private ThreadPool validateAndGetThreadPool(String tpName) {
        if (tpName == null || tpName.trim().isEmpty()) {
            log.warn("Empty thread pool name requested");
            return null;
        }
        ThreadPool threadPool = UnifiedTPRegulator.getResource(tpName.trim());
        if (threadPool == null) {
            log.warn("Thread pool not found: {}", tpName);
        }
        return threadPool;
    }

    /**
     * 获取所有线程池基本信息列表
     */
    @GetMapping("/pools")
    public List<PoolInfo> getAllPools() {
        try {
            return UnifiedTPRegulator.getAllThreadPoolInfo();
        } catch (Exception e) {
            log.error("Failed to get all pools: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取所有线程池名称
     */
    @GetMapping("/poolNames")
    public List<String> getAllPoolNames() {
        try {
            return UnifiedTPRegulator.getAllThreadPoolNames();
        } catch (Exception e) {
            log.error("Failed to get all pool names: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取线程池的信息
     */
    @GetMapping("/pool")
    public PoolInfo getThreadPoolInfo(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            return threadPool != null ? threadPool.getThreadPoolInfo() : null;
        } catch (Exception e) {
            log.error("Failed to get thread pool info for [{}]: {}", tpName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取线程池的线程状态信息
     */
    @GetMapping("/threadInfo")
    public Map<String, Map<Thread.State, Integer>> getThreadInfo(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            if (threadPool == null) {
                return Map.of("core", Map.of(), "extra", Map.of());
            }
            return threadPool.getThreadsInfo();
        } catch (Exception e) {
            log.error("Failed to get thread info for [{}]: {}", tpName, e.getMessage(), e);
            return Map.of("core", Map.of(), "extra", Map.of());
        }
    }

    /**
     * 获取队列中总任务数量
     */
    @GetMapping("/tasks")
    public Map<String, Object> getQueueSize(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            int count = threadPool != null ? threadPool.getTaskNums() : 0;
            return Map.of("tpName", tpName, "count", count, "success", true);
        } catch (Exception e) {
            log.error("Failed to get queue size for [{}]: {}", tpName, e.getMessage(), e);
            return Map.of("tpName", tpName, "count", 0, "success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取各个分区任务数量
     */
    @GetMapping("/partitionTaskNums")
    public Map<String, Object> getPartitionTaskNums(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            if (threadPool == null) {
                return Map.of("tpName", tpName, "data", Map.of(), "success", false, "error", "Thread pool not found");
            }
            return Map.of("tpName", tpName, "data", threadPool.getPartitionTaskNums(), "success", true);
        } catch (Exception e) {
            log.error("Failed to get partition task nums for [{}]: {}", tpName, e.getMessage(), e);
            return Map.of("tpName", tpName, "data", Map.of(), "success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取队列信息
     */
    @GetMapping("/queue")
    public QueueInfo getQueueInfo(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            return threadPool != null ? threadPool.getQueueInfo() : null;
        } catch (Exception e) {
            log.error("Failed to get queue info for [{}]: {}", tpName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取所有队列名称
     */
    @GetMapping("/queueName")
    public List<String> getAllQueueName(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            return threadPool != null ? threadPool.getAllQueueName() : List.of();
        } catch (Exception e) {
            log.error("Failed to get queue names for [{}]: {}", tpName, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取所有拒绝策略名称
     */
    @GetMapping("/rejectStrategyName")
    public List<String> getAllRejectStrategyName(@RequestParam(required = true) String tpName) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            return threadPool != null ? threadPool.getAllRejectStrategyName() : List.of();
        } catch (Exception e) {
            log.error("Failed to get reject strategy names for [{}]: {}", tpName, e.getMessage(), e);
            return List.of();
        }
    }


    /**
     * 更改worker相关的参数
     */
    @PutMapping("/worker")
    public Map<String, Object> changeWorkerParams(
            @RequestParam(required = true) String tpName,
            Integer coreNums,
            Integer maxNums,
            Boolean coreDestroy,
            Integer aliveTime,
            Boolean isDaemon) {
        try {
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            if (threadPool == null) {
                return Map.of("success", false, "error", "Thread pool not found");
            }
            boolean result = threadPool.changeWorkerParams(coreNums, maxNums, coreDestroy, aliveTime, isDaemon);
            return Map.of("success", result, "tpName", tpName);
        } catch (Exception e) {
            log.error("Failed to change worker params for [{}]: {}", tpName, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 改变队列
     */
    @PutMapping("/queue")
    public Map<String, Object> changeQ(
            @RequestParam(required = true) String tpName,
            @RequestBody QueueInfo queueInfo) {
        try {
            if (queueInfo.getQueueName() == null || queueInfo.getQueueName().isEmpty()) {
                return Map.of("success", false, "error", "Queue name is required");
            }
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            if (threadPool == null) {
                return Map.of("success", false, "error", "Thread pool not found");
            }
            Partition q;
            if (!PartiResourceManager.getResources().containsKey(queueInfo.getQueueName())) {
                return Map.of("success", false, "error", "Queue type not found: " + queueInfo.getQueueName());
            }
            if (!queueInfo.isPartitioning()) {
                q = PartiResourceManager.getResources().get(queueInfo.getQueueName())
                        .getConstructor(Integer.class).newInstance(queueInfo.getCapacity());
            } else {
                String partitionType = queueInfo.getPartitionType() != null ? queueInfo.getPartitionType() : "parti_flow";
                if ("parti_still".equals(partitionType)) {
                    q = new PartiStill(queueInfo.getPartitionNum(), queueInfo.getCapacity(), queueInfo.getQueueName(),
                            SPResourceManager.getOfferResource(queueInfo.getOfferPolicy()).getConstructor().newInstance(),
                            SPResourceManager.getPollResource(queueInfo.getPollPolicy()).getConstructor().newInstance(),
                            SPResourceManager.getRemoveResource(queueInfo.getRemovePolicy()).getConstructor().newInstance());
                } else {
                    q = new PartiFlow(queueInfo.getPartitionNum(), queueInfo.getCapacity(), queueInfo.getQueueName(),
                            SPResourceManager.getOfferResource(queueInfo.getOfferPolicy()).getConstructor().newInstance(),
                            SPResourceManager.getPollResource(queueInfo.getPollPolicy()).getConstructor().newInstance(),
                            SPResourceManager.getRemoveResource(queueInfo.getRemovePolicy()).getConstructor().newInstance());
                }
            }
            boolean result = threadPool.changeQueue(q, queueInfo.getQueueName());
            return Map.of("success", result, "tpName", tpName);
        } catch (Exception e) {
            log.error("Failed to change queue for [{}]: {}", tpName, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 改变拒绝策略
     */
    @PutMapping("/rejectStrategy")
    public Map<String, Object> changeRS(
            @RequestParam(required = true) String tpName,
            @RequestParam(required = true) String rsName) {
        try {
            if (rsName == null || rsName.isEmpty()) {
                return Map.of("success", false, "error", "Reject strategy name is required");
            }
            ThreadPool threadPool = validateAndGetThreadPool(tpName);
            if (threadPool == null) {
                return Map.of("success", false, "error", "Thread pool not found");
            }
            RejectStrategy rs;
            try {
                rs = (RejectStrategy) context.getBean(rsName);
            } catch (NoSuchBeanDefinitionException e) {
                if (!RSResourceManager.REJECT_STRATEGY_MAP.containsKey(rsName)) {
                    return Map.of("success", false, "error", "Reject strategy not found: " + rsName);
                }
                rs = (RejectStrategy) RSResourceManager.REJECT_STRATEGY_MAP.get(rsName).getConstructor().newInstance();
            }
            boolean result = threadPool.changeRejectStrategy(rs, rsName);
            return Map.of("success", result, "tpName", tpName);
        } catch (Exception e) {
            log.error("Failed to change reject strategy for [{}]: {}", tpName, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 动态编译并热部署资源
     */
    @PostMapping("/hotDeploy")
    public Map<String, Object> hotDeploy(@RequestParam String className, @RequestBody String javaCode) {
        try {
            log.info("Starting hot deploy for class: {}", className);
            DynamicCompiler compiler = new DynamicCompiler();
            Class<?> clazz = compiler.compileToClass(className, javaCode);

            PartiResource partiResource = clazz.getAnnotation(PartiResource.class);
            RSResource rsResource = clazz.getAnnotation(RSResource.class);
            SPResource spResource = clazz.getAnnotation(SPResource.class);
            GCTResource gctResource = clazz.getAnnotation(GCTResource.class);

            boolean success = false;

            if (partiResource != null) {
                PartiResourceManager.register(partiResource.value(), clazz.asSubclass(Partition.class));
                log.info("Hot deployed custom queue: {}", partiResource.value());
                success = true;
            }

            if (rsResource != null) {
                RSResourceManager.register(rsResource.value(), clazz.asSubclass(RejectStrategy.class));
                log.info("Hot deployed custom reject strategy: {}", rsResource.value());
                success = true;
            }

            if (spResource != null) {
                SPResourceManager.register(spResource.value(), clazz);
                log.info("Hot deployed custom schedule policy: {}", spResource.value());
                success = true;
            }

            if (gctResource != null) {
                String partiName = gctResource.bindingPartiResource();
                String spName = gctResource.bindingSPResource();
                String spType = gctResource.spType();

                if (partiName != null && !partiName.isEmpty()) {
                    Class<? extends Partition> partiClass = PartiResourceManager.getResource(partiName);
                    if (partiClass != null) {
                        GCTaskManager.register(partiClass, clazz.asSubclass(GCTask.class));
                        log.info("Hot deployed GC task bound to partition: {}", partiName);
                        success = true;
                    }
                }

                if (spName != null && !spName.isEmpty()) {
                    String type = spType != null ? spType : "";
                    if (!type.endsWith(":")) type += ":";

                    Class<? extends SchedulePolicy> spPolicyClass = null;
                    if (Constant.POLL.equals(type)) {
                        spPolicyClass = SPResourceManager.getPollResource(spName);
                    } else if (Constant.OFFER.equals(type)) {
                        spPolicyClass = SPResourceManager.getOfferResource(spName);
                    } else if (Constant.REMOVE.equals(type)) {
                        spPolicyClass = SPResourceManager.getRemoveResource(spName);
                    }

                    if (spPolicyClass != null) {
                        GCTaskManager.register(spPolicyClass, clazz.asSubclass(GCTask.class));
                        log.info("Hot deployed GC task bound to schedule policy: {} ({})", spName, type);
                        success = true;
                    }
                }
            }

            if (!success) {
                log.warn("Class {} compiled successfully but no valid resource annotations found", className);
            }
            return Map.of("success", success, "className", className);

        } catch (Exception e) {
            log.error("Hot deploy failed for class [{}]: {}", className, e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取热部署模板列表
     */
    @GetMapping("/hotDeploy/templates")
    public Map<String, Object> getHotDeployTemplates() {
        Map<String, Object> result = new HashMap<>();

        // Queue templates
        List<Map<String, String>> queueTemplates = new java.util.ArrayList<>();
        queueTemplates.add(createTemplate("基于LinkedBlockingQ的队列", "queue_linked",
            "package com.yf;\n\nimport com.yf.core.partition.Impl.LinkedBlockingQ;\nimport com.yf.core.resource_container.scanned_annotation.PartiResource;\n\n@PartiResource(\"自定义队列名称\")\npublic class CustomQueue extends LinkedBlockingQ<Runnable> {\n    public CustomQueue(Integer capacity) {\n        super(capacity);\n    }\n}",
            "com.yf.CustomQueue"));
        queueTemplates.add(createTemplate("基于Partition的队列", "queue_partition",
            "package com.yf;\n\nimport com.yf.core.partition.Partition;\nimport com.yf.core.resource_container.scanned_annotation.PartiResource;\nimport java.util.concurrent.BlockingQueue;\nimport java.util.concurrent.LinkedBlockingQueue;\nimport java.util.concurrent.TimeUnit;\n\n@PartiResource(\"自定义队列名称\")\npublic class CustomQueue implements Partition<Runnable> {\n    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();\n\n    @Override\n    public boolean offer(Runnable task) {\n        return queue.offer(task);\n    }\n\n    @Override\n    public Runnable poll(Integer waitTime) throws InterruptedException {\n        return queue.poll(waitTime, TimeUnit.MILLISECONDS);\n    }\n\n    @Override\n    public Runnable removeEle() {\n        return queue.poll();\n    }\n\n    @Override\n    public int getEleNums() {\n        return queue.size();\n    }\n\n    @Override\n    public void lockGlobally() {}\n\n    @Override\n    public void unlockGlobally() {}\n\n    @Override\n    public Integer getCapacity() {\n        return Integer.MAX_VALUE;\n    }\n\n    @Override\n    public void setCapacity(Integer capacity) {}\n\n    @Override\n    public void markAsSwitched() {}\n}",
            "com.yf.CustomQueue"));

        // RejectStrategy templates
        List<Map<String, String>> rsTemplates = new java.util.ArrayList<>();
        rsTemplates.add(createTemplate("拒绝策略", "rejectstrategy",
            "package com.yf;\n\nimport com.yf.core.rejectstrategy.RejectStrategy;\nimport com.yf.core.threadpool.ThreadPool;\nimport com.yf.core.resource_container.scanned_annotation.RSResource;\n\n@RSResource(\"自定义拒绝策略名称\")\npublic class CustomRejectStrategy extends RejectStrategy {\n    @Override\n    public void reject(ThreadPool threadPool, Runnable task) {\n        // 自定义拒绝逻辑\n    }\n}",
            "com.yf.CustomRejectStrategy"));

        // SchedulePolicy templates - Offer
        List<Map<String, String>> offerTemplates = new java.util.ArrayList<>();
        offerTemplates.add(createTemplate("Offer策略", "sp_offer",
            "package com.yf;\n\nimport com.yf.core.partitioning.schedule_policy.OfferPolicy;\nimport com.yf.core.partition.Partition;\nimport com.yf.core.resource_container.scanned_annotation.SPResource;\nimport java.util.concurrent.atomic.AtomicLong;\n\n@SPResource(\"自定义Offer策略名称\")\npublic class CustomOfferPolicy extends OfferPolicy {\n    private volatile boolean roundRobin = true;\n    private final AtomicLong round = new AtomicLong(0);\n\n    @Override\n    public int selectPartition(Partition[] partitions, Object object) {\n        int ps = partitions.length;\n        int r = (int) round.getAndIncrement() % partitions.length;\n        if ((ps & (ps - 1)) == 0) {\n            return r & (ps - 1);\n        }\n        return r % ps;\n    }\n\n    @Override\n    public boolean getRoundRobin() {\n        return roundRobin;\n    }\n\n    @Override\n    public void setRoundRobin(boolean roundRobin) {\n        this.roundRobin = roundRobin;\n    }\n}",
            "com.yf.CustomOfferPolicy"));

        // SchedulePolicy templates - Poll
        List<Map<String, String>> pollTemplates = new java.util.ArrayList<>();
        pollTemplates.add(createTemplate("Poll策略", "sp_poll",
            "package com.yf;\n\nimport com.yf.core.partitioning.schedule_policy.PollPolicy;\nimport com.yf.core.partition.Partition;\nimport com.yf.core.resource_container.scanned_annotation.SPResource;\nimport java.util.concurrent.atomic.AtomicLong;\nimport java.util.concurrent.ThreadLocalRandom;\n\n@SPResource(\"自定义Poll策略名称\")\npublic class CustomPollPolicy extends PollPolicy {\n    private volatile boolean roundRobin = false;\n    private final AtomicLong round = new AtomicLong(0);\n\n    @Override\n    public int selectPartition(Partition[] partitions) {\n        int ps = partitions.length;\n        if (roundRobin) {\n            int r = (int) round.getAndIncrement() % ps;\n            if ((ps & (ps - 1)) == 0) {\n                return r & (ps - 1);\n            }\n            return r;\n        }\n        return ThreadLocalRandom.current().nextInt(ps);\n    }\n\n    @Override\n    public boolean getRoundRobin() {\n        return roundRobin;\n    }\n\n    @Override\n    public void setRoundRobin(boolean roundRobin) {\n        this.roundRobin = roundRobin;\n    }\n}",
            "com.yf.CustomPollPolicy"));

        // SchedulePolicy templates - Remove
        List<Map<String, String>> removeTemplates = new java.util.ArrayList<>();
        removeTemplates.add(createTemplate("Remove策略", "sp_remove",
            "package com.yf;\n\nimport com.yf.core.partitioning.schedule_policy.RemovePolicy;\nimport com.yf.core.partition.Partition;\nimport com.yf.core.resource_container.scanned_annotation.SPResource;\nimport java.util.concurrent.atomic.AtomicLong;\n\n@SPResource(\"自定义Remove策略名称\")\npublic class CustomRemovePolicy extends RemovePolicy {\n    private volatile boolean roundRobin = true;\n    private final AtomicLong round = new AtomicLong(0);\n\n    @Override\n    public int selectPartition(Partition[] partitions) {\n        int ps = partitions.length;\n        int r = (int) round.getAndIncrement() % ps;\n        if ((ps & (ps - 1)) == 0) {\n            return r & (ps - 1);\n        }\n        return r % ps;\n    }\n\n    @Override\n    public boolean getRoundRobin() {\n        return roundRobin;\n    }\n\n    @Override\n    public void setRoundRobin(boolean roundRobin) {\n        this.roundRobin = roundRobin;\n    }\n}",
            "com.yf.CustomRemovePolicy"));

        // GCTask templates
        List<Map<String, String>> gcTaskTemplates = new java.util.ArrayList<>();
        gcTaskTemplates.add(createTemplate("GCTask-绑定分区", "gctask_parti",
            "package com.yf;\n\nimport com.yf.common.task.GCTask;\nimport com.yf.core.partition.Partition;\nimport com.yf.core.threadpool.ThreadPool;\nimport com.yf.core.resource_container.scanned_annotation.GCTResource;\n\n@GCTResource(bindingPartiResource = \"linked\")\npublic class CustomGCTask extends GCTask {\n    @Override\n    public void run() {\n        ThreadPool threadPool = getThreadPool();\n        Partition<?> partition = getPartition();\n        // 自定义GC任务逻辑\n    }\n}",
            "com.yf.CustomGCTask"));
        gcTaskTemplates.add(createTemplate("GCTask-绑定Poll策略", "gctask_poll",
            "package com.yf;\n\nimport com.yf.common.task.GCTask;\nimport com.yf.core.partition.Partition;\nimport com.yf.core.threadpool.ThreadPool;\nimport com.yf.core.partitioning.schedule_policy.PollPolicy;\nimport com.yf.core.resource_container.scanned_annotation.GCTResource;\n\n@GCTResource(bindingSPResource = \"thread_binding\", spType = \"poll:\")\npublic class CustomGCTaskPoll extends GCTask {\n    @Override\n    public void run() {\n        ThreadPool threadPool = getThreadPool();\n        PollPolicy pollPolicy = getPollPolicy();\n        // 自定义GC任务逻辑\n    }\n}",
            "com.yf.CustomGCTaskPoll"));
        gcTaskTemplates.add(createTemplate("GCTask-绑定Offer策略", "gctask_offer",
            "package com.yf;\n\nimport com.yf.common.task.GCTask;\nimport com.yf.core.partition.Partition;\nimport com.yf.core.threadpool.ThreadPool;\nimport com.yf.core.partitioning.schedule_policy.OfferPolicy;\nimport com.yf.core.resource_container.scanned_annotation.GCTResource;\n\n@GCTResource(bindingSPResource = \"round_robin\", spType = \"offer:\")\npublic class CustomGCTaskOffer extends GCTask {\n    @Override\n    public void run() {\n        ThreadPool threadPool = getThreadPool();\n        OfferPolicy offerPolicy = getOfferPolicy();\n        // 自定义GC任务逻辑\n    }\n}",
            "com.yf.CustomGCTaskOffer"));

        // Build result
        result.put("queue", queueTemplates);
        result.put("rejectstrategy", rsTemplates);
        result.put("sp_offer", offerTemplates);
        result.put("sp_poll", pollTemplates);
        result.put("sp_remove", removeTemplates);
        result.put("gctask", gcTaskTemplates);

        return result;
    }

    private Map<String, String> createTemplate(String name, String type, String code, String defaultClassName) {
        Map<String, String> template = new HashMap<>();
        template.put("name", name);
        template.put("type", type);
        template.put("code", code);
        template.put("defaultClassName", defaultClassName);
        return template;
    }

}
