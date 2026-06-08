package com.yf.common.glue;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * 封装待编译的Java源码字符串，适配编译器输入规范
 */
public class SourceFile extends SimpleJavaFileObject {
    // 第10行: 存储完整的Java源码字符串
    private final String sourceCode;

    /**
     * @param className 类全名（如com.example.Test）
     * @param sourceCode 完整的Java类代码
     */
    public SourceFile(String className, String sourceCode) {
        // 第17行: 调用父类构造函数，创建一个虚拟URI
        // className.replace('.', '/') 将点号替换为斜杠
        // 例如: "com.example.Test" → "string:///com/example/Test.java"
        // "string:///" 是虚拟协议，表示内存中的源码，不对应真实文件
        super(URI.create("string:///" + className.replace('.', '/') + ".java"), Kind.SOURCE);
        // 第18行: 保存源码字符串
        this.sourceCode = sourceCode;
    }

    /**
     * 第21-24行: 重写方法，JDK编译器从此方法获取源码内容
     * @param ignoreEncodingErrors 是否忽略编码错误（此处忽略）
     * @return 源码字符串，编译器每次编译都调用此方法读取源码
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return sourceCode;
    }
}