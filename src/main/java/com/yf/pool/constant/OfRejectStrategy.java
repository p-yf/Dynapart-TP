package com.yf.pool.constant;

import com.yf.pool.rejectstrategy.impl.CallerRunsStrategy;
import com.yf.pool.rejectstrategy.impl.DiscardOldestStrategy;
import com.yf.pool.rejectstrategy.impl.DiscardStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @description   //接下来要做的就是实现自动将map自动装满，也就是扫描类路径下所有的策略类并且有策略注解的策略类，这样就能解决在非 springboot环境下无法自动发现扩展的策略的问题
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
