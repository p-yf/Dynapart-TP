package com.yf.core.partitioning.schedule_policy;

import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/10/6 14:12
 * @description 调度规则的顶级接口,由于分区队列需要调度规则选择分区，而大部分调度规则会运用到取余这一计算，所以为了保证性能，
 * 建议分区数量为2的幂次数，这样取余运算性能会高一些。
 */
public interface SchedulePolicy {
    int selectPartition(Partition[] partitions,Object o);
}
