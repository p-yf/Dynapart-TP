package com.yf.test_springboot_integration.task;

import com.yf.common.task.GCTask;
import com.yf.core.threadpool.ThreadPool;
import com.yf.springboot_integration.pool.annotation.GCTResource;

/**
 * @author yyf
 * @date 2025/10/6 19:07
 * @description
 */
@GCTResource(bindingResource = "myq")
public class t extends GCTask {


    @Override
    public void run() {

    }

    @Override
    public GCTask build(ThreadPool tp) {
        return this;
    }
}
