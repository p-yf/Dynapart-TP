package com.yf.pool.constant;

import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;

import java.util.HashMap;
import java.util.Map;

public class OfRejectStrategy {
    public final static String CALLER_RUNS = "callerRuns";//调用线程运行
    public static Map<String, Class<?>> REJECT_STRATEGY_MAP = new HashMap<>();
    static {
        REJECT_STRATEGY_MAP.put(CALLER_RUNS, CallerRunsStrategy.class);
    }
}
