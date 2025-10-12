package com.yf.core.resource_manager;


import com.yf.core.partitioning.schedule_policy.OfferPolicy;
import com.yf.core.partitioning.schedule_policy.PollPolicy;
import com.yf.core.partitioning.schedule_policy.RemovePolicy;
import com.yf.core.partitioning.schedule_policy.SchedulePolicy;
import com.yf.core.partitioning.schedule_policy.impl.offer_policy.*;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.PeekShavingPoll;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.RandomPoll;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.RoundRobinPoll;
import com.yf.core.partitioning.schedule_policy.impl.poll_policy.ThreadBindingPoll;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.PeekShavingRemove;
import com.yf.core.partitioning.schedule_policy.impl.remove_policy.RoundRobinRemove;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yyf
 * @date 2025/9/20 21:29
 * @description : 调度规则资源管理(SchedulePolicyResourceManager)
 */
public class SPResourceManager {
    //以下三个是各自调度规则的map
    private static final Map<String,Class<? extends OfferPolicy>> OFFER_POLICY_MAP = new HashMap<>();
    private static final Map<String,Class<? extends PollPolicy>> POLL_POLICY_MAP = new HashMap<>();
    private static final Map<String,Class<? extends RemovePolicy>> REMOVE_POLICY_MAP = new HashMap<>();

    static {
        //Offer
        register("round_robin", RoundRobinOffer.class);
        register("random", RandomOffer.class);
        register("plain_hash", PlainHashOffer.class);
        register("balanced_hash", BalancedHashOffer.class);
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
    //调度资源注册
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


    //获取调度资源
    public static Class<? extends OfferPolicy> getOfferResource(String name){
        return OFFER_POLICY_MAP.get(name);
    }
    public static Class<? extends PollPolicy> getPollResource(String name){
        return POLL_POLICY_MAP.get(name);
    }
    public static Class<? extends RemovePolicy> getRemoveResource(String name){
        return REMOVE_POLICY_MAP.get(name);
    }



    //获取所有调度资源
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
