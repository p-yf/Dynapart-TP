package com.yf.pool.taskqueue.Impl.parti_flow;

import lombok.Data;

/**
 * @author yyf
 * @date 2025/8/8 22:39
 * @description
 */
@Data
public class Node <T>{
    private T value;

    private Node<T> next;

    public Node(T value) {
        this.value =  value;
    }
    public Node() {
    }
}
