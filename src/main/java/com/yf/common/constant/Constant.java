package com.yf.common.constant;
//worker相关的常量

/**
 * @author yyf
 * @description
 */
public class Constant {
    //线程相关
    public final static String CORE = "core";//核心线程
    public final static String EXTRA = "extra";//非核心线程
    public final static String VIRTUAL = "virtual:";//虚拟线程
    public final static String PLATFORM = "platform:";//平台线程

    //线程池相关
    public final static String LITTLE_CHIEF = "littleChief";//轻量级GC线程池
    public final static String IO = "io";
    public final static String CPU = "cpu";

    //gc任务相关---有关调度规则的类型
    public final static String OFFER = "offer:";
    public final static String POLL = "poll:";
    public final static String REMOVE = "remove:";
}
