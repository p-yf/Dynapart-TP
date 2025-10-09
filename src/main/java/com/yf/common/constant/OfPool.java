package com.yf.common.constant;
//worker相关的常量

/**
 * @author yyf
 * @description
 */
public class OfPool {
    public final static String CORE = "core";//核心线程
    public final static String EXTRA = "extra";//非核心线程
    public final static String VIRTUAL = "virtual:";//虚拟线程
    public final static String PLATFORM = "platform:";//平台线程

    public final static String LITTLE_CHIEF = "littleChief";//轻量级GC线程池
    public final static String IO = "io";
    public final static String CPU = "cpu";
}
