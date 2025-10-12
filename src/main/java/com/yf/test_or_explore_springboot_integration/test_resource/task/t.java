package com.yf.test_or_explore_springboot_integration.test_resource.task;

import com.yf.common.constant.Constant;
import com.yf.common.task.GCTask;
import com.yf.core.partition.Partition;
import com.yf.core.threadpool.ThreadPool;
import com.yf.springboot_integration.pool.annotation.GCTResource;

/**
 * @author yyf
 * @date 2025/10/6 19:07
 * @description
 */
@GCTResource(bindingPartiResource = "myq",bindingSPResource = "thread_binding",spType = Constant.POLL)
public class t extends GCTask {


    @Override
    public void run() {
        ThreadPool threadPool = getThreadPool();
        Partition<?> partition = getPartition();
    }

}
