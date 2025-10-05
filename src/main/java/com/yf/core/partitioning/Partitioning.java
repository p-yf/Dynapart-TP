package com.yf.core.partitioning;

import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partition.Partition;

/**
 * @author yyf
 * @date 2025/10/5 11:15
 * @description  分区化队列的接口，用来判断队列是否为分区队列
 */
public interface Partitioning<E> {
    Partition<E>[] getPartitions();
    OfferPolicy getOfferPolicy();
    PollPolicy getPollPolicy();
    RemovePolicy getRemovePolicy();
}
