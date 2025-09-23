package com.yf.pool.constant_or_registry;


import com.yf.partition.Impl.partitioning.strategy.OfferPolicy;
import com.yf.partition.Impl.partitioning.strategy.PollPolicy;
import com.yf.partition.Impl.partitioning.strategy.RemovePolicy;
import com.yf.partition.Impl.partitioning.strategy.impl.offer_policy.HashOffer;
import com.yf.partition.Impl.partitioning.strategy.impl.offer_policy.RandomOffer;
import com.yf.partition.Impl.partitioning.strategy.impl.offer_policy.RoundRobinOffer;
import com.yf.partition.Impl.partitioning.strategy.impl.offer_policy.ValleyFillingOffer;
import com.yf.partition.Impl.partitioning.strategy.impl.poll_policy.PeekShavingPoll;
import com.yf.partition.Impl.partitioning.strategy.impl.poll_policy.RandomPoll;
import com.yf.partition.Impl.partitioning.strategy.impl.poll_policy.RoundRobinPoll;
import com.yf.partition.Impl.partitioning.strategy.impl.poll_policy.ThreadBindingPoll;
import com.yf.partition.Impl.partitioning.strategy.impl.remove_policy.PeekShavingRemove;
import com.yf.partition.Impl.partitioning.strategy.impl.remove_policy.RoundRobinRemove;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @date 2025/9/20 21:29
 * @description
 */
public class SchedulePolicyManager {
    private static Map<String,Class<? extends OfferPolicy>> OFFER_POLICY_MAP = new HashMap<>();
    private static Map<String,Class<? extends PollPolicy>> POLL_POLICY_MAP = new HashMap<>();
    private static Map<String,Class<? extends RemovePolicy>> REMOVE_POLICY_MAP = new HashMap<>();
    static {
        //offer
        register("round_robin", RoundRobinOffer.class);
        register("random", RandomOffer.class);
        register("hash", HashOffer.class);
        register("valley_filling", ValleyFillingOffer.class);

        //poll
        register("round_robin", RoundRobinPoll.class);
        register("peek_shaving", PeekShavingPoll.class);
        register("thread_binding", ThreadBindingPoll.class);
        register("random", RandomPoll.class);

        //remove
        register("round_robin", RoundRobinRemove.class);
        register("peek_shaving", PeekShavingRemove.class);
        register("random", PeekShavingRemove.class);

    }

    public static void register(String name, Class policyClass) {
        if(policyClass.getSuperclass()==OfferPolicy.class) {
            OFFER_POLICY_MAP.put(name, policyClass);
        }
        if(policyClass.getSuperclass()==PollPolicy.class) {
            POLL_POLICY_MAP.put(name, policyClass);
        }
        if (policyClass.getSuperclass() == RemovePolicy.class) {
            REMOVE_POLICY_MAP.put(name, policyClass);
        }
    }

    public static Class<? extends OfferPolicy> getOfferResource(String name){
        return OFFER_POLICY_MAP.get(name);
    }
    public static Class<? extends PollPolicy> getPollResource(String name){
        return POLL_POLICY_MAP.get(name);
    }
    public static Class<? extends RemovePolicy> getRemoveResource(String name){
        return REMOVE_POLICY_MAP.get(name);
    }
    public static Map<String,Class<? extends OfferPolicy>> getOfferResources(){
        return OFFER_POLICY_MAP;
    }
    public static Map<String,Class<? extends PollPolicy>> getPollResources(){
        return POLL_POLICY_MAP;
    }
    public static Map<String,Class<? extends RemovePolicy>> getRemoveResources(){
        return REMOVE_POLICY_MAP;
    }
}
