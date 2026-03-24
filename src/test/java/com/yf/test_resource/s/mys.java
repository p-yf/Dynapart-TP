package com.yf.test_resource.s;

import com.yf.core.rejectstrategy.RejectStrategy;
import com.yf.core.threadpool.ThreadPool;
import com.yf.core.resource_container.scanned_annotation.RSResource;

/**
 * @author yyf
 * @description 测试用自定义拒绝策略
 */
@RSResource("mys")
public class mys extends RejectStrategy {
    @Override
    public void reject(ThreadPool threadPool,Runnable task) {
    }

}
