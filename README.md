# DynaPart-TP Dynamic Partition Thread Pool

<img src="src/main/resources/static/logo/logo1.png" alt="Logo" width="200" height="200">
<img src="src/main/resources/static/logo/logo2.png" alt="Logo" width="600" height="190">

> **中文文档**: [README_zh.md](README_zh.md)

---

## One-Line Summary

DynaPart-TP is a high-performance dynamic thread pool framework that reduces lock contention through **partitioned queues** and supports **runtime hot-swapping**.

---

## Key Highlights

| Highlight | Description |
|-----------|-------------|
| **Partitioned Queue** | Multiple partitions with independent locks, significantly reduces lock contention |
| **3D Scheduling** | Independent offer/poll/remove policies |
| **Three-Layer Fallback** | Safe Worker exit and old queue GC during switching |
| **Annotation-Based Resources** | @ResourceScan auto-scans and registers, zero-config custom components |
| **Runtime Hot Deploy** | Dynamic Java code compilation, update without restart |
| **Real-time Monitoring** | REST API + WebSocket dashboard |

---

## Overall Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                         Application                                          │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
                                              │
                    ┌─────────────────────────┬─────────────────────────┐
                    │                         │                         │
                    ▼                         ▼                         ▼
┌─────────────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────┐
│           ThreadPool            │ │   UnifiedTPRegulator      │ │    ResourceContainer      │
│                                 │ │                           │ │                           │
│  Worker threads ← Queue ← Reject│ │  Dynamic regulator:       │ │  Resource managers:       │
│                                 │ │  register/switch/monitor  │ │  @ResourceScan scanning   │
└─────────────────────────────────┘ └───────────────────────────┘ └───────────────────────────┘

                         ┌──────────────────────────────────────────────────────┐
                         │                  ResourceContainer                    │
                         │                                                       │
                         │   ResourceScanner ───→ @ResourceScan scans packages  │
                         │         │                                           │
                         │         ├── @PartiResource ──→ PartiResourceManager   │
                         │         ├── @SPResource ────→ SPResourceManager       │
                         │         ├── @RSResource ────→ RSResourceManager       │
                         │         └── @GCTResource ───→ GCTaskManager          │
                         └──────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| **ThreadPool** | Core thread pool, manages Worker lifecycle |
| **Worker** | Polls tasks from queue and executes |
| **Partition** | Queue abstraction (single/partitioned) |
| **Partitioning** | Partitioned queue interface (PartiFlow/PartiStill) |
| **OfferPolicy/PollPolicy/RemovePolicy** | 3D scheduling policies |
| **RejectStrategy** | Rejection strategies |
| **UnifiedTPRegulator** | Global thread pool registry + dynamic control |
| **ResourceScanner** | Annotation scanning and resource registration |
| **GCTaskManager** | GC cleanup task management during queue switching |

---

## Partitioning Mechanism (Queue + Scheduling Policies)

This is the **core mechanism** of DynaPart-TP, covering the complete flow from task submission to task execution.

### Problem: Lock Contention in Traditional Thread Pools

Traditional thread pools use a single queue, all threads compete for one lock:

```
┌─────────────────────────────────────────────────────────────┐
│                 Traditional Single Queue Thread Pool          │
│                                                             │
│   ThreadPool                                                 │
│   ┌───────────────────┐                                     │
│   │   Single Queue    │ ←── All threads compete for same lock│
│   │     (1 lock)      │                                     │
│   └───────────────────┘                                     │
│         ↓                                                    │
│   ThreadA ──→ [BLOCKED]                                      │
│   ThreadB ──→ [BLOCKED]                                      │
│   ThreadC ──→ [BLOCKED]                                      │
└─────────────────────────────────────────────────────────────┘
```

**Problem**: Under high concurrency, synchronization overhead becomes the bottleneck.

### Solution: Partitioned Queue + 3D Scheduling Policies

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Partitioned Thread Pool (Complete Flow)            │
│                                                                     │
│  【Task Submission Flow】                                           │
│   execute(task)                                                     │
│       ↓                                                             │
│   OfferPolicy.selectPartition()  ──→ "Which partition should task enter?"│
│       ↓                                                             │
│   Partition.offer(task)      ──→ Task enters partition's sub-queue  │
│                                                                     │
│  【Worker Task Polling Flow】                                       │
│   Worker.run()                                                      │
│       ↓                                                             │
│   PollPolicy.selectPartition()  ──→ "Which partition to poll from?"│
│       ↓                                                             │
│   Partition.poll()              ──→ Poll task from partition         │
│                                                                     │
│  【Task Rejection Flow】                                            │
│   Queue full / Thread pool full                                    │
│       ↓                                                             │
│   RemovePolicy.selectPartition()  ──→ "Which partition to discard?"│
│       ↓                                                             │
│   Partition.removeEle()         ──→ Remove task from partition       │
└─────────────────────────────────────────────────────────────────────┘
```

### Queue Implementations

#### Single Queue: LinkedBlockingQ (Dual Lock Separation)

Producers and consumers use **independent locks**, never blocking each other:

```java
public class LinkedBlockingQ<T> extends Partition<T> {
    private final Lock headLock = new ReentrantLock();  // Consumer lock
    private final Lock tailLock = new ReentrantLock();  // Producer lock
    private final Condition notEmpty = headLock.newCondition();
}
```

#### Partitioned Queue: PartiFlow (Dynamic Partition)

```
┌────────────────────────────────────────────────────────────────┐
│                        PartiFlow                                 │
│                  (Partitioned Queue, implements Partitioning)    │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Scheduling Policy Layer                  │   │
│  │   ┌────────────┐  ┌────────────┐  ┌────────────┐       │   │
│  │   │OfferPolicy │  │PollPolicy  │  │RemovePolicy│       │   │
│  │   │  (Offer)   │  │   (Poll)   │  │  (Remove)   │       │   │
│  │   └─────┬──────┘  └─────┬──────┘  └─────┬──────┘       │   │
│  └─────────┼───────────────┼───────────────┼────────────────┘   │
│            └───────────────┼───────────────┘                    │
│                            ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Partition Layer                       │   │
│  │   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐       │   │
│  │   │ Part 0 │  │ Part 1 │  │ Part 2 │  │ Part 3 │ ...  │   │
│  │   │(sub-queue)│ (sub-queue)│ (sub-queue)│ (sub-queue)│   │   │
│  │   └────────┘  └────────┘  └────────┘  └────────┘       │   │
│  │                                                          │   │
│  │   Each partition has independent lock:                    │   │
│  │   Lock0 / Lock1 / Lock2 / Lock3                           │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

**Two Partitioned Queue Types**:

| Type | Class | Characteristics |
|------|-------|-----------------|
| Dynamic | `PartiFlow` | Post-scheduling round-robin: try next if partition full, more flexible |
| Static | `PartiStill` | Direct routing: return false if partition full, higher performance |

### Built-in Scheduling Policies

#### Offer Policy (OfferPolicy)

| Config Value | Principle | Characteristics |
|-------------|-----------|-----------------|
| `round_robin` | Atomic counter round-robin | Load balancing |
| `random` | Random selection | Simple implementation |
| `plain_hash` | Hash-based selection | Same task same partition |
| `balanced_hash` | Hash perturbation optimization | Uniform distribution |
| `valley_filling` | Select partition with fewest tasks | Dynamic balancing |
| `priority` | Priority tasks select partition by getPriority() value; non-Priority tasks degrade to round-robin | **Degradable Priority** |

#### Poll Policy (PollPolicy)

| Config Value | Principle | Characteristics |
|-------------|-----------|-----------------|
| `round_robin` | Atomic counter round-robin | Fair |
| `random` | Random selection | Decentralized |
| `thread_binding` | ThreadLocal bound to thread | **High cache hit rate** |
| `peek_shaving` | Poll from busiest partition | Peak shaving |
| `priority` | Prefer partitions with tasks (low-index first), degrades to round-robin when all empty | **Degradable Priority** |

#### Remove Policy (RemovePolicy)

| Config Value | Principle |
|-------------|-----------|
| `round_robin` | Round-robin |
| `random` | Random |
| `peek_shaving` | Remove from busiest partition |
| `priority` | Remove from high-index partitions first (preserve low-index/high-priority), returns last when all empty |

### Special Scheduling Policies (Optional Deep Dive)

These policies have special implementations for specific scenarios:

**1. Thread Binding (thread_binding)**
- Binds each thread to a fixed partition via ThreadLocal
- Advantage: High cache hit rate, suitable for long-running tasks
- Note: Must cooperate with GCTask mechanism (cleanup old Workers on queue switch)

**2. Balanced Hash (balanced_hash)**
- Hash perturbation optimization for more uniform distribution
- More uniform than plain_hash, slightly more computation

**3. Priority Policies (priority)**
- Offer: Priority tasks select partition by `getPriority()` value; non-Priority tasks degrade to round-robin
- Poll: Prefer partitions with tasks (low-index first), degrades to round-robin when all partitions empty
- Remove: Remove from high-index partitions first (free up low-priority partitions first), returns last partition when all empty

### Post-Scheduling Round-Robin (roundRobin Property)

Each policy has a `roundRobin` property:
- `false`: Only operate on the partition selected by the policy
- `true`: If selected partition fails, automatically try next

```
Example: valley_filling + roundRobin=true
Policy selects partition 0, but partition 0 is full → Try partition 1, success
```

**Note**: `thread_binding` must have `roundRobin=false`, otherwise one thread would operate on multiple partitions, destroying cache locality.

---

## Queue Switching Mechanism (Three-Layer Fallback + GCTask)

### Problem: Challenges When Dynamically Switching Queues

Two problems must be solved during queue switching:
1. **Old Workers sense the switch and exit**: Cannot poll from old queue anymore
2. **Old queue resources are recycled**: Old queue should be GC-able

### Solution: Three-Layer Fallback + GCTask

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    Queue Switching Complete Flow                                  │
│                                                                                 │
│  Calling UnifiedTPRegulator.changeQueue("poolName", newQueue)                     │
│                                                                                 │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ Layer 1: Synchronous Mark (Old queue switched)                             │  │
│  │         oldQ.markAsSwitched()  →  switched=true on all partitions         │  │
│  │         → New tasks offer() detect switched, throw SwitchedException      │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                    │                                            │
│                                    ▼                                            │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ Layer 2: Lock-Check-Exception (Old Workers exit)                           │  │
│  │         Old Workers in poll():                                             │  │
│  │         1. Acquire lock first (lockGlobally)                               │  │
│  │         2. Check switched flag                                             │  │
│  │         3. If true, throw SwitchedException, Worker exits                 │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                    │                                            │
│                                    ▼                                            │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ Layer 3: GCTask Fallback (Old queue GC)                                    │  │
│  │         When edge cases (ThreadLocal binding, lock-free queues) can't be    │  │
│  │         handled by first two layers:                                       │  │
│  │         → GCTaskManager.clean() executes fallback cleanup                   │  │
│  │         → Destroy Workers holding old references → New Workers new bindings│  │
│  │         → Old queue has no references → Can be GC'd                        │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                 │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ Async Migration: GCTaskManager.execute() migrates remaining tasks         │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### GCTask Mechanism

**Why is GCTask needed?**

When using `thread_binding` poll policy, Workers bind to **partition index** via ThreadLocal:
- On first partition selection, `threadLocal.set(index)` stores the partition index
- Subsequent polls always return the same partition index

**Problem**: ThreadLocal's key is a **weak reference** (collected by GC), but value (Integer index) is a **strong reference** (not collected)

```
Before GC: ThreadLocalMap: { ThreadLocal(key) → Integer(index=value) }
                      key is weak ref, collected by GC
After GC: ThreadLocalMap: { null → Integer }  ← Integer cannot be GC'd (memory leak)
```

As queue switches happen repeatedly, more and more Integer objects cannot be collected → **memory leak**.

**GCTask Solution**: Destroy old Workers → Workers' ThreadLocals destroyed with Workers → No leak

### GCTaskManager Architecture

```java
public class GCTaskManager {
    // Dedicated async thread pool (executes GCTasks, cannot block main switching flow)
    private static volatile ThreadPool littleChief;

    // SchedulePolicy → GCTask mapping
    private static Map<Class<? extends SchedulePolicy>,
                      Class<? extends GCTask>> SCHEDULE_TASK_MAP = new HashMap<>();

    // Partition type → GCTask mapping
    private static Map<Class<? extends Partition<?>>,
                      Class<? extends GCTask>> PARTI_TASK_MAP = new HashMap<>();

    static {
        // ThreadBindingPoll uses ThreadLocal, needs cleanup
        register(ThreadBindingPoll.class, TBPollCleaningTask.class);
    }
}
```

### littleChief Thread Pool

**What it is**: littleChief is a dedicated thread pool inside GCTaskManager for executing GCTasks, responsible for asynchronously executing cleanup tasks during queue switching.

**Why singleton**:
- GCTask is just cleanup tasks, short execution time, no need to create new thread pool for each switch
- One small thread pool is enough to handle all GCTask cleanup tasks
- Avoids thread pool accumulation and resource waste during multiple switches
- Easy for unified management and monitoring

**Three configuration methods**:

#### 1. No Configuration (Use Default)

If not configured, littleChief uses **lazy singleton** default implementation, created automatically on first call to `GCTaskManager.execute()`:

```java
// Default configuration
ThreadPool littleChief = new ThreadPool(
    "littleChief",           // Name
    5,                       // Core threads
    10,                      // Max threads
    "littleChief",           // Thread name prefix
    new WorkerFactory("", false, true, 10),  // Non-daemon, core destroyable
    new LinkedBlockingQ<>(50),   // Queue capacity 50
    new CallerRunsStrategy()     // Rejection strategy
);
```

#### 2. yml Configuration (Spring Boot Auto-configuration)

Configure littleChief parameters in `application.yml`, Spring Boot will automatically create and inject:

```yaml
yf:
  thread-pool:
    little-chief:  # Dedicated thread pool for GC async tasks
      enabled: true
      useVirtualThread: false  # Use virtual threads
      coreNums: 10              # Core thread count
      maxNums: 50              # Max thread count
      threadName: yf-thread    # Thread name
      useDaemon: true          # Daemon thread
      aliveTime: 5000          # Keep-alive time (ms)
      rejectStrategyName: discard  # Rejection strategy
```

#### 3. Manual Configuration

Manually set via `GCTaskManager.setLittleChief(ThreadPool tp)`:

```java
// Create custom littleChief thread pool
ThreadPool myLittleChief = new ThreadPool(
    "my-gc-pool", 5, 10, "gc-worker",
    new WorkerFactory("gc", false, true, 5000),
    new LinkedBlockingQ<>(100),
    new CallerRunsStrategy()
);

// Manual injection (recommended to set early at application startup)
GCTaskManager.setLittleChief(myLittleChief);
```

**Note**: `setLittleChief()` can only be set once, subsequent calls have no effect.

### GCTask-Related Annotations

| Annotation | Purpose | Registers To |
|------------|---------|--------------|
| `@GCTResource` | Bind custom GCTask to specific policy/queue | GCTaskManager |

**@GCTResource Annotation Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| `bindingPartiResource` | String | Bound queue resource name (corresponds to @PartiResource's name) |
| `bindingSPResource` | String | Bound scheduling policy name (corresponds to @SPResource's name) |
| `spType` | String | Policy type: `poll:` (poll), `offer:` (offer), `remove:` (remove) |

**Example**:

```java
// Custom GCTask, bound to poll policy named "myPoll"
@GCTResource(bindingSPResource = "myPoll", spType = "poll:")
public class MyGCTask extends GCTask {
    @Override
    public void run() {
        // Custom cleanup logic
    }
}
```

### Built-in GCTask: TBPollCleaningTask

When switching a queue using `thread_binding` poll policy, this cleanup task executes automatically:

```java
public class TBPollCleaningTask extends GCTask {
    @Override
    public void run() {
        // Destroy all Workers, new Workers will be created based on new queue
        UnifiedTPRegulator.destroyWorkers(
            threadPool.getName(),
            coreList.size(),
            extraList.size()
        );
    }
}
```

---

## Resource Scanning and Container Management

### Core Annotations

| Annotation | Purpose | Registers To |
|------------|---------|--------------|
| `@ResourceScan` | Enable package scanning (scan entry class's package and subpackages) | - |
| `@PartiResource("name")` | Custom queue | PartiResourceManager |
| `@SPResource("name")` | Custom scheduling policy | SPResourceManager |
| `@RSResource("name")` | Custom reject strategy | RSResourceManager |
| `@GCTResource(...)` | Custom GCTask | GCTaskManager |

### Scanning Flow

```
Application starts
    │
    ▼
Discover entry class with @ResourceScan annotation
    │
    ▼
ResourceScanner.scan(entry class)
    │
    ├── Scan all .class files in entry class's package and subpackages
    │
    ├── Find @PartiResource ──→ PartiResourceManager.register(name, class)
    ├── Find @SPResource ────→ SPResourceManager.register(name, class)
    ├── Find @RSResource ───→ RSResourceManager.register(name, class)
    └── Find @GCTResource ───→ GCTaskManager.register(binding, class)
```

### Usage Example

```java
@ResourceScan  // Enable scanning
public class MyApplication {
    public static void main(String[] args) {
        // Scanning auto-completes
    }
}

// Custom queue
@PartiResource("myQueue")
public class MyQueue extends LinkedBlockingQ<Runnable> { ... }

// Custom poll policy
@SPResource("myPoll")
public class MyPoll extends PollPolicy { ... }

// Custom GCTask
@GCTResource(bindingSPResource = "myPoll", spType = "poll:")
public class MyGCTask extends GCTask { ... }
```

---

## Hot Deployment (Glue Mode)

Dynamically compile Java code strings into Class files at runtime.

### REST API

```
POST /monitor/hotDeploy?className=com.example.MyTask
Body: Java code string
```

Compiled classes are automatically detected for annotations and registered to corresponding resource managers.

### Code Usage

```java
DynamicCompiler compiler = new DynamicCompiler();
Class<?> clazz = compiler.compileToClass("com.example.MyTask", javaCodeString);
Runnable task = (Runnable) clazz.getDeclaredConstructor().newInstance();
```

---

## Quick Start

### Spring Boot Integration (Recommended)

**1. Add @ResourceScan to entry class**
```java
@ResourceScan
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**2. Configure application.yml**
```yaml
yf:
  thread-pool:
    little-chief:  # Configure littleChief thread pool itself
      enabled: true
      coreNums: 10
      maxNums: 50
      threadName: worker
      useDaemon: false
      aliveTime: 60000
      rejectStrategyName: callerRuns
    queue:  # Configure internal queue used by littleChief
      partitioning: true
      partitionNum: 8
      capacity: 10000
      queueName: linked
      offerPolicy: valley_filling
      pollPolicy: round_robin
      removePolicy: round_robin
    monitor:
      enabled: true
      fixedDelay: 1000
```

**3. Use**
```java
@Autowired
private ThreadPool threadPool;

threadPool.execute(() -> System.out.println("Task executed"));
```

### Standalone (No Spring)

```java
// 1. Manually invoke resource scanning (if custom resources need registration)
ResourceScanner.scan(YourApplication.class);

// 2. Create partitioned queue
Partition<Runnable> queue = new PartiFlow<>(
    8, 10000, "linked",
    new ValleyFillingOffer(),
    new ThreadBindingPoll(),
    new RoundRobinRemove()
);

// 3. Create thread pool
ThreadPool threadPool = new ThreadPool(
    10, 50, "my-pool",
    new WorkerFactory("worker", false, false, 60000),
    queue,
    new CallerRunsStrategy()
);

// 4. Register
UnifiedTPRegulator.register("my-pool", threadPool);

// 5. Use
threadPool.execute(() -> {});
```

**Note**: If not using @ResourceScan annotation or no custom resources, manually call `ResourceScanner.scan(EntryClass)` for resource scanning and registration.

### Hot Switching Examples

```java
// Dynamically adjust thread parameters
UnifiedTPRegulator.changeWorkerParams("poolName", 20, 100, null, null, null);

// Dynamically switch queue
Partition<Runnable> newQueue = new LinkedBlockingQ<>(20000);
UnifiedTPRegulator.changeQueue("poolName", newQueue);

// Dynamically switch reject strategy
UnifiedTPRegulator.changeRejectStrategy("poolName", new AbortStrategy(), "abort");
```

---

## Configuration Parameters

### little-chief (GC Async Task Thread Pool)

The `little-chief` config node configures **the littleChief thread pool itself**, which is the dedicated thread pool inside GCTaskManager for executing GCTasks.

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| enabled | boolean | Enable/disable | true |
| useVirtualThread | boolean | Use virtual threads | false |
| coreNums | int | Core thread count | 5 |
| maxNums | int | Max thread count | 10 |
| threadName | String | Thread name | littleChief |
| useDaemon | boolean | Daemon thread | false |
| aliveTime | long | Keep-alive time (ms) | 10000 |
| rejectStrategyName | String | Rejection strategy | callerRuns |

### queue (littleChief Internal Queue)

The `queue` config node configures **the internal queue used by littleChief thread pool**.

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| partitioning | boolean | Enable partitioning | false |
| partitionNum | int | Partition count (recommend power of 2) | 4 |
| capacity | Integer | Capacity, null=unbounded | - |
| queueName | String | Queue type | linked |
| offerPolicy | String | Offer policy | round_robin |
| pollPolicy | String | Poll policy | round_robin |
| removePolicy | String | Remove policy | round_robin |

**Queue Types (queueName)**:

| Value | Class | Characteristics |
|-------|-------|-----------------|
| linked | LinkedBlockingQ | Dual lock separation, bounded/unbounded |
| linkedS | LinkedBlockingQS | CAS optimized version |
| priority | PriorityBlockingQ | Priority queue |

---

## REST API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/monitor/pool?tpName=xxx` | Thread pool info |
| GET | `/monitor/tasks?tpName=xxx` | Task count |
| GET | `/monitor/partitionTaskNums?tpName=xxx` | Task count per partition |
| GET | `/monitor/threadInfo?tpName=xxx` | Thread status |
| PUT | `/monitor/worker?tpName=xxx` | Adjust thread parameters |
| PUT | `/monitor/queue?tpName=xxx` | Switch queue |
| PUT | `/monitor/rejectStrategy?tpName=xxx&rsName=xxx` | Switch reject strategy |
| POST | `/monitor/hotDeploy?className=xxx` | Hot deploy |

---

## Web UI Screenshots

### Dashboard

![Dashboard](pic/index.png)

### Partition Monitoring

![Partition Monitor 1/2](pic/monitor1.png)
![Partition Monitor 2/2](pic/monitor2.png)

### Dynamic Configuration

![Config 1/2](pic/config1.png)
![Config 2/2](pic/config2.png)

### Hot Deployment

![Hot Deploy 1/2](pic/hotdeploy1.png)
![Hot Deploy 2/2](pic/hotdeploy2.png)

---

## Project Structure

```
src/main/java/com/yf/
├── common/                    # Common components
│   ├── constant/              # Constants
│   ├── entity/                # Entities (PoolInfo, QueueInfo)
│   ├── exception/             # Exceptions (SwitchedException)
│   ├── glue/                  # Dynamic compilation (Glue Mode)
│   │   ├── DynamicCompiler.java
│   │   ├── MemoryClassLoader.java
│   │   ├── MemoryFileManager.java
│   │   └── SourceFile.java / ByteCodeFile.java
│   └── task/                  # Task classes
│       ├── GCTask.java        # GC cleanup task base class
│       └── impl/
│           └── TBPollCleaningTask.java  # ThreadBinding cleanup task
│
├── core/                      # Core components
│   ├── partition/             # Queue abstraction and implementations
│   │   ├── Partition.java     # Abstract base class
│   │   └── Impl/
│   │       ├── LinkedBlockingQ.java    # Dual-lock single queue
│   │       ├── LinkedBlockingQS.java   # CAS-optimized single queue
│   │       └── PriorityBlockingQ.java  # Priority queue
│   │
│   ├── partitioning/          # Partitioned queues
│   │   ├── Partitioning.java  # Partitioning interface
│   │   ├── impl/
│   │   │   ├── PartiFlow.java     # Dynamic partitioning
│   │   │   └── PartiStill.java    # Static partitioning
│   │   └── schedule_policy/
│   │       ├── OfferPolicy.java    # Offer policy interface
│   │       ├── PollPolicy.java    # Poll policy interface
│   │       ├── RemovePolicy.java  # Remove policy interface
│   │       └── impl/
│   │           ├── offer/          # Offer policy implementations
│   │           ├── poll/           # Poll policy implementations
│   │           └── remove/         # Remove policy implementations
│   │
│   ├── rejectstrategy/        # Reject strategies
│   │   ├── RejectStrategy.java
│   │   └── impl/
│   │       ├── CallerRunsStrategy.java
│   │       ├── AbortStrategy.java
│   │       ├── DiscardStrategy.java
│   │       └── DiscardOldestStrategy.java
│   │
│   ├── resource_container/    # Resource container
│   │   ├── ResourceScanner.java   # Scanner
│   │   ├── scanned_annotation/    # Annotation definitions
│   │   │   ├── ResourceScan.java
│   │   │   ├── PartiResource.java
│   │   │   ├── SPResource.java
│   │   │   ├── RSResource.java
│   │   │   └── GCTResource.java
│   │   └── resource_manager/       # Resource managers
│   │       ├── GCTaskManager.java
│   │       ├── PartiResourceManager.java
│   │       ├── SPResourceManager.java
│   │       └── RSResourceManager.java
│   │
│   ├── threadpool/            # Thread pool core
│   │   └── ThreadPool.java
│   ├── tp_regulator/         # Dynamic regulator
│   │   └── UnifiedTPRegulator.java
│   ├── worker/               # Worker thread
│   │   └── Worker.java
│   └── workerfactory/        # Thread factory
│       └── WorkerFactory.java
│
└── springboot_integration/    # Spring Boot integration
    ├── monitor/               # Monitor module
    │   ├── auto_configuration/
    │   ├── controller/
    │   │   └── MonitorController.java  # REST API
    │   └── ws/
    │       ├── ThreadPoolWebSocketHandler.java
    │       └── SchedulePushInfoService.java
    └── pool/                  # Auto configuration
        └── auto_configuration/
            ├── LittleChiefAutoConfiguration.java
            └── ResourceAutoConfiguration.java
```

---

## FAQ

### How to choose partition count?

**Recommend power of 2** (8/16/32/64), enabling bit operations instead of modulo:

```java
// When partition count is power of 2
return r & (ps - 1);  // Bit operation, single cycle

// When not power of 2
return r % ps;       // Division, dozens of cycles
```

### When does partitioning show advantages?

- High concurrency, large task volume
- Lock contention becomes bottleneck
- Need high cache hit rate (using thread_binding policy)

### How to choose scheduling policies?

| Scenario | Offer Policy | Poll Policy |
|----------|--------------|-------------|
| High concurrency short tasks | valley_filling | round_robin |
| Long-running tasks (cache needed) | plain_hash | thread_binding |
| Peak shaving | valley_filling | peek_shaving |

---

## License

MIT
