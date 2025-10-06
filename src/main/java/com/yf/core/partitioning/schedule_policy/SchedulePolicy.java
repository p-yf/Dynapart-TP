package com.yf.core.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/10/6 14:12
 * @description 调度规则的顶级接口
 */
public interface SchedulePolicy {
    int selectPartition(Partition[] partitions,Object o);
}
