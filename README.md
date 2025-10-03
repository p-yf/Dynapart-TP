# DynaPart-TP

<img src="src/main/resources/static/logo/logo.png" alt="Logo" width="200" height="200">  <!-- 项目Logo -->

## 🚀 项目介绍

DynaPart-TP 是一个高性能、可动态调整的轻量级线程池框架，专为需要精细控制并发任务执行的Java应用程序设计。该框架提供了丰富的线程池管理功能，包括参数动态调整、实时监控和灵活的任务调度策略。

前置知识：“资源”指的就是项目中的队列、拒绝策略、调度规则等。“分区化”是项目的重点创新，是一种队列降低锁粒度的方式

### 核心功能
- **动态参数调整**：无需重启应用，实时调整核心线程数、最大线程数，甚至是任务队列等参数
- **灵活自定义**：可自定义各种组件资源，如：任务队列、拒绝策略、调度规则等
- **实时监控**：通过REST API和WebSocket实时监控线程池状态
- **Spring Boot集成**：与Spring Boot框架无缝集成，支持自动配置，同时利用注解驱动的开发思想
- **轻松扩展**：只需要实现接口就能轻松自定义任务队列、拒绝策略
- **分区化队列模型**：支持分区化队列（PartiFlow和PartiStill用来实现分区化），降低锁粒度，提高吞吐量，提升稳定性

## 💡 实现亮点

### 1. 动态参数调整机制
框架允许在运行时调整线程池的核心参数，包括核心线程数、最大线程数、线程空闲时间等。这些调整会立即生效，无需重启应用。

### 2. 实时监控系统
通过WebSocket实现了线程池状态的实时推送，客户端可以实时获取线程池的工作状态、任务队列长度等信息。同时提供了REST API用于查询和调整线程池参数。

### 3. 可插拔的组件设计
- **任务队列**：支持多种队列实现，可根据业务需求动态切换，支持自定义
- **拒绝策略**：提供多种拒绝策略（如CallerRuns、DiscardOldest等），支持自定义
- **调度规则**：支持多种出入队调度规则，如轮询、随机、哈希、填谷等，支持自定义

### 4. 线程生命周期管理
精细控制线程的创建和销毁，支持线程的动态调整和自动回收。

### 5. 良好的架构设计
1 遵循了开闭原则：所以无论是使用者还是开发者进行扩展都会很轻松，心旷神怡。
2 资源管理中心：注册表设计模式，项目中有三个资源管理中心分别管理分区（就是队列）、拒绝策略、调度规则，方便开发者扩展资源
3 合理利用springboot机制：在springboot环境下能够实现自动装配和通过注解来注册资源
4 利用组合模式实现了分区（队列）的自由分区与否

### 6. 分区化队列模型（Partition）
分区化是框架的核心特性之一，它将队列抽象为一种分区表现形式。任何队列只要实现了`Partition`接口，就可以自由选择成为分区队列或者单个队列。

- **灵活的分区策略**：支持轮询、随机、哈希、填谷等多种任务入队策略
- **高效的任务出队**：提供轮询、随机、削峰、线程绑定等出队策略
- **细粒度控制**：可根据业务需求动态调整分区数量和容量
- **高性能设计**：通过多分区并行处理提高吞吐量，减少锁竞争


## 🚀 性能对比

通过测试对比，可看到在锁竞争激烈的情况下DynaPart-TP线程池与JDK线程池相比具有明显性能优势

### 性能优势（测试仅限锁竞争激烈的情况）
1. **更高吞吐量**：在相同配置下，DynaPart-TP处理任务的速度远高于JDK线程池
2. **更好的资源利用**：通过分区化设计，减少线程等待时间，提高CPU利用率
3. **更稳定的性能**：在高并发场景下，DynaPart-TP的性能波动更小

## 📚 使用方法

### 1. Spring Boot环境集成(test_springboot_integration包就是用来测试springboot集成的)

#### 1.1配置文件
在`application.yml`中添加以下配置：

```yaml
#线程池配置
yf:
  thread-pool:
    pool:
      enabled: true
      useVirtualThread: false #是否使用虚拟线程
      coreNums: 10    #线程池核心线程数
      maxNums: 50    #线程池最大线程数
      poolName: yf-thread-pool   #线程池名称
      threadName: yf-thread      #线程名称
      isDaemon: true      #是否是守护线程
      coreDestroy: false  #核心线程是否销毁
      aliveTime: 5000        #线程存活时间（单位：ms）
      rejectStrategyName: discard   #拒绝策略名称
    queue:     #(由于queue比较重要，所以与pool和monitor一个层级)
      partitioning: false  #是否分区化(如果是false，只需要读取capacity和queueName)
      partitionNum: 10      #分区数量
      capacity: 10000         #队列容量（不写代表null，为无界）
      queueName: linked     #队列名称
      offerPolicy: ROUND_ROBIN       #入队策略
      pollPolicy: THREAD_BINDING      #出队策略
      removePolicy: ROUND_ROBIN     #移除策略
    monitor:
      enabled: true       #是否开启监控
      fixedDelay: 1000    #后台像前端推送线程状态信息的间隔时间(单位：ms)
    service-registry:      #是否开启服务注册
      enabled: false
      heartBeat: 10000   #心跳间隔时间(单位：ms)
      expireTime: 12000  #注册数据失效时间(单位：ms)
```

#### 1.2使用线程池

注入`ThreadPool`实例并使用：

```java
@Autowired
private ThreadPool threadPool;

// 执行任务
threadPool.execute(() -> {
    // 任务逻辑
});
//如果是使用优先级队列，那么应传入PriorityTask对象能手动指定优先级（数字越大优先级越高），示例：
//Runnable r = () -> {};        //任务，返回值 ，优先级
//PriorityTask pt = new PriorityTask(r, null,10);
//threadPool.executeThreadFirst(pt);


// 提交有返回值的任务
Future<?> future = threadPool.submit(() -> {
    // 任务逻辑
    return result;
});
//如果是使用优先级队列，那么应传入PriorityTask对象能手动指定优先级（数字越大优先级越高），示例：
//Callable c = () -> {};        //任务，返回值 ，优先级
//PriorityTask pt = new PriorityTask(c,10);
//threadPool.executeThreadFirst(pt);
```

### 1.3监控线程池

#### REST API
- 获取线程池信息：`GET /monitor/pool`
- 获取队列任务数量：`GET /monitor/tasks`
- 调整线程参数：`PUT /monitor/worker`
- 切换队列：`PUT /monitor/queue`
- 切换拒绝策略：`PUT /monitor/rejectStrategy`
- ......


### 2. 非Spring Boot环境使用

```java
import com.yf.core.workerfactory.WorkerFactory;

// 创建worker工厂
WorkerFactory workerFactory = new WorkerFactory("worker", false, false, 6000,false);
// 线程名称，是否守护线程，核心线程是否销毁，空闲时间（单位：ms）


singleQueue.

setCapacity(100); // 设置队列容量，如果不设置则为无界队列

// 或创建分区化队列
PartiFlow<Runnable> partitionedQueue = new PartiFlow<>(
        10, // 分区数量
        1000, // 总容量
        "linked_plus", // 队列名称
        OfferStrategy.ROUND_ROBIN, // 入队策略
        PollStrategy.ROUND_ROBIN, // 出队策略
        RemoveStrategy.ROUND_ROBIN // 移除策略
);

// 创建拒绝策略
RejectStrategy rejectStrategy = new CallerRunsStrategy();

// 创建线程池                            
ThreadPool threadPool = new ThreadPool(
        5, 20, // 核心线程数，最大线程数
        "DynaPartPool", // 线程池名称
        workerFactory, // worker工厂
        singleQueue, // 任务队列（或使用partitionedQueue）
        rejectStrategy // 拒绝策略
);

// 使用线程池
threadPool.execute(() ->{
        // 任务逻辑
        });

Future<?> future = threadPool.submit(() -> {
    // 任务逻辑
    return "Result";
});
```
### 2.1 命令行

#### 主要命令
- yf info pool //打印线程池信息
- yf info worker  //打印线程信息
- yf info taskNum  //打印队列任务数量
- yf change worker -coreNums 2 -maxNums 5 -coreDestroy true......(如果有参数没写就直接赋值为null)  //改变线程参数
- yf change queue linked(队列名称举例)  //改变队列
- yf change rejectstrategy callerRuns(拒绝策略名称举例)   //改变拒绝策略


## 🔧 开发者自定义扩展资源说明

### 只举调度策略自定义的例子，并且举的例子是入队规则的，队列和拒绝策略自定义的方法差不多
调度策略涉及了入队、出队和移除策略，所以共有三个Map来管理，key：资源名称，value：调度策略类
以下分别说明springboot环境和非springboot环境的使用方式，当然，springboot环境肯定是兼容非springboot环境的使用方法的

springboot环境：
```java
/**
 * @author yyf
 * @date 2025/9/21 0:57
 * @description
 */
@SPResource("mysp")//无论是出队还是入队还是移除都是使用这个注解，但是继承的类是不同的，注解value值是资源名称。
public class mysp extends OfferPolicy {

    @Override
    public int selectPartition(Partition[] partitions, Object object) {
        return 0;
    }

//    （只有入队和出队有轮询相关接口，移除没有，只有PartiFlow实现分区化才能够自由选择是否轮询，PartiStill无轮询相关功能）
//    这里的轮询指的是在调度策略执行后是否轮询下一个分区尝试出队或者入队，不要跟出入队调度规则中的轮询规则搞浑了
    @Override
    public boolean getRoundRobin() {//在入队失败后是否选择轮询接下来的分区
        return false;
    }

    @Override
    public void setRoundRobin(boolean roundRobin) {//设置是否轮询

    }
}
```

非springboot环境，在实现相关的类后还需要注册到注册中心，需要调用静态方法register
```java
/**
 * @author yyf
 * @date 2025/9/20 21:29
 * @description : 调度规则资源管理(SchedulePolicyResourceManager)
 */
public class SPResourceManager {
    private static final Map<String,Class<? extends OfferPolicy>> OFFER_POLICY_MAP = new HashMap<>();
    private static final Map<String,Class<? extends PollPolicy>> POLL_POLICY_MAP = new HashMap<>();
    private static final Map<String,Class<? extends RemovePolicy>> REMOVE_POLICY_MAP = new HashMap<>();
    static {
        //offer
        register("round_robin", RoundRobinOffer.class);
        register("random", RandomOffer.class);
        register("hash", HashOffer.class);
        register("valley_filling", ValleyFillingOffer.class);

        //poll
        register("round_robin", RoundRobinPoll.class);
        register("peek_shaving", PeekShavingPoll.class);
        register("thread_binding", ThreadBindingPoll.class);
        register("random", RandomPoll.class);

        //remove
        register("round_robin", RoundRobinRemove.class);
        register("peek_shaving", PeekShavingRemove.class);
        register("random", PeekShavingRemove.class);

    }

    public static void register(String name, Class policyClass) {
        if(policyClass.getSuperclass()==OfferPolicy.class) {
            OFFER_POLICY_MAP.put(name, policyClass);
        }
        if(policyClass.getSuperclass()==PollPolicy.class) {
            POLL_POLICY_MAP.put(name, policyClass);
        }
        if (policyClass.getSuperclass() == RemovePolicy.class) {
            REMOVE_POLICY_MAP.put(name, policyClass);
        }
    }

    public static Class<? extends OfferPolicy> getOfferResource(String name){
        return OFFER_POLICY_MAP.get(name);
    }
    public static Class<? extends PollPolicy> getPollResource(String name){
        return POLL_POLICY_MAP.get(name);
    }
    public static Class<? extends RemovePolicy> getRemoveResource(String name){
        return REMOVE_POLICY_MAP.get(name);
    }
    public static Map<String,Class<? extends OfferPolicy>> getOfferResources(){
        return OFFER_POLICY_MAP;
    }
    public static Map<String,Class<? extends PollPolicy>> getPollResources(){
        return POLL_POLICY_MAP;
    }
    public static Map<String,Class<? extends RemovePolicy>> getRemoveResources(){
        return REMOVE_POLICY_MAP;
    }
}

```


