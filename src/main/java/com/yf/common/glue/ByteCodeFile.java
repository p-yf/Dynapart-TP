package com.yf.common.glue;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * 存储编译生成的字节码，避免写入磁盘
 */
public class ByteCodeFile extends SimpleJavaFileObject {
    // 第12行: 核心成员变量 - ByteArrayOutputStream
    // 所有写入此对象的字节码都存储在内存字节数组中
    // final修饰，确保整个生命周期内都使用同一个输出流
    private final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

    /**
     * 第14-16行: 构造函数
     * @param className 类全名，用于构造虚拟URI
     */
    public ByteCodeFile(String className) {
        // 第18行: 调用父类，URI格式如"bytes:///com/example/Test.class"
        // "bytes:///"是虚拟协议，表示内存中的字节码，不对应真实文件
        // className.replace('.', '/') 将点号替换为斜杠
        super(URI.create("bytes:///" + className.replace('.', '/') + ".class"), Kind.CLASS);
    }

    /**
     * 第21-23行: 重写输出流方法
     * 编译器写入字节码时调用此方法，数据直接写入byteOut
     * @return ByteArrayOutputStream，编译器通过此流写入字节码
     */
    @Override
    public OutputStream openOutputStream() {
        // 第22行: 返回内存输出流，编译器每写一个字节都进入byteOut
        return byteOut;
    }

    /**
     * 第26-31行: 获取编译后的字节码数组
     * 在编译完成后调用此方法提取字节码
     * @return 字节码数组，供MemoryClassLoader加载类使用
     */
    public byte[] getByteCode() {
        // 第29行: ByteArrayOutputStream.toByteArray()返回内部字节数组副本
        return byteOut.toByteArray();
    }
}