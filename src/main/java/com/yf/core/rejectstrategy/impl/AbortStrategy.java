package com.yf.core.rejectstrategy.impl;

import com.yf.common.constant.Logo;
import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author yyf
 * @date 2025/10/1 14:20
 * @description
 */
@Slf4j
public class AbortStrategy extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool, Runnable task) {
        throw new RejectedExecutionException(Logo.LOG_LOGO +"任务被拒绝：" + task);
    }
}
