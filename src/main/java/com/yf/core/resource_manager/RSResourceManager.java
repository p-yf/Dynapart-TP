package com.yf.core.resource_manager;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.core.rejectstrategy.impl.DiscardOldestStrategy;
import com.yf.core.rejectstrategy.impl.DiscardStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description : 拒绝策略资源管理器(RejectStrategyResourceManager)
 */
public class RSResourceManager {
    public final static String CALLER_RUNS = "callerRuns";//调用当前线程运行
    public final static String DISCARD_OLDEST = "discardOldest";//丢弃最老的
    public final static String DISCARD = "discard";//直接丢弃
    public static final Map<String, Class<? extends RejectStrategy>> REJECT_STRATEGY_MAP = new HashMap<>();
    static {
        register(CALLER_RUNS, CallerRunsStrategy.class);
        register(DISCARD_OLDEST, DiscardOldestStrategy.class);
        register(DISCARD, DiscardStrategy.class);
    }

    public static void  register(String rejectStrategyName, Class<? extends RejectStrategy> rejectStrategyClass) {
        REJECT_STRATEGY_MAP.put(rejectStrategyName, rejectStrategyClass);
    }

    public static Class<? extends RejectStrategy> getResource(String name) {
        return REJECT_STRATEGY_MAP.get(name);
    }

    public static Map<String,Class<? extends RejectStrategy>> getResources(){
        return REJECT_STRATEGY_MAP;
    }
}
