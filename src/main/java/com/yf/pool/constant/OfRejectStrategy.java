package com.yf.pool.constant;

import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.pool.rejectstrategy.impl.DiscardOldestStrategy;
import com.yf.pool.rejectstrategy.impl.DiscardStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description
 */
public class OfRejectStrategy {
    public final static String CALLER_RUNS = "callerRuns";//调用当前线程运行
    public final static String DISCARD_OLDEST = "discardOldest";//丢弃最老的
    public final static String DISCARD = "discard";
    public static final Map<String, Class<?>> REJECT_STRATEGY_MAP = new HashMap<>();
    static {
        REJECT_STRATEGY_MAP.put(CALLER_RUNS, CallerRunsStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD_OLDEST, DiscardOldestStrategy.class);
        REJECT_STRATEGY_MAP.put(DISCARD, DiscardStrategy.class);
    }
}
