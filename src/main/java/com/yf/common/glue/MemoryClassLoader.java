package com.yf.common.glue;

import java.util.Map;

/**
 * 从内存字节码加载类，避免读取磁盘class文件
 */
public class MemoryClassLoader extends ClassLoader {
    // 编译好的类字节码（类全名 -> 字节数组）
    private final Map<String, byte[]> compiledByteCodes;

    public MemoryClassLoader(Map<String, byte[]> compiledByteCodes) {
        this.compiledByteCodes = compiledByteCodes;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        byte[] byteCode = compiledByteCodes.get(className);
        if (byteCode != null) {
            return defineClass(className, byteCode, 0, byteCode.length);
        }
        return super.findClass(className);
    }
}
