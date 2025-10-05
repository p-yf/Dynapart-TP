package com.yf.common.exception;

/**
 * @author yyf
 * @date 2025/10/6 0:09
 * @description  用来解决队列切换不能及时感知，线程无法及时切换队列的问题，抛出异常就是用来让线程感知到队列的切换
 */
public class SwitchedException extends RuntimeException{
}
