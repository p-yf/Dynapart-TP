package com.yf.pool.constant_or_registry;

import com.yf.pool.rejectstrategy.RejectStrategy;
import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.pool.rejectstrategy.impl.DiscardOldestStrategy;
import com.yf.pool.rejectstrategy.impl.DiscardStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description
 */
public class RejectStrategyManager {
    public final static String CALLER_RUNS = "callerRuns";//调用当前线程运行
    public final static String DISCARD_OLDEST = "discardOldest";//丢弃最老的
    public final static String DISCARD = "discard";
    public static final Map<String, Class<? extends RejectStrategy>> REJECT_STRATEGY_MAP = new HashMap<>();
    static {
        REJECT_STRATEGY_MAP.put(CALLER_RUNS, CallerRunsStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD_OLDEST, DiscardOldestStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD, DiscardStrategy.class);
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
