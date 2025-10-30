package com.yf.common.glue;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * 存储编译生成的字节码，避免写入磁盘
 */
public class ByteCodeFile extends SimpleJavaFileObject {
    private final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

    /**
     * @param className 类全名（与源码类名对应）
     */
    public ByteCodeFile(String className) {
        super(URI.create("bytes:///" + className.replace('.', '/') + ".class"), Kind.CLASS);
    }

    @Override
    public OutputStream openOutputStream() {
        return byteOut;
    }

    /**
     * 获取编译后的字节码数组
     */
    public byte[] getByteCode() {
        return byteOut.toByteArray();
    }
}
