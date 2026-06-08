package com.yf.common.glue;

import java.util.Map;

/**
 * 从内存字节码加载类，避免读取磁盘class文件
 */
public class MemoryClassLoader extends ClassLoader {
    // 第9-10行: 成员变量，存储类全名到字节码的映射
    // 由MemoryFileManager.getCompiledByteCodes()传入
    private final Map<String, byte[]> compiledByteCodes;

    // 第12-13行: 构造函数
    // @param compiledByteCodes 编译好的类字节码Map
    public MemoryClassLoader(Map<String, byte[]> compiledByteCodes) {
        this.compiledByteCodes = compiledByteCodes;
    }

    /**
     * 第16-23行: 核心方法 - 从内存字节码查找并加载类
     * findClass是ClassLoader的核心方法，当父加载器无法找到类时调用
     * ClassLoader.loadClass()的查找顺序：findLoadedClass() → 父加载器loadClass() → findClass()
     * @param className 要加载的类全名
     * @return Class对象
     * @throws ClassNotFoundException 当字节码中找不到该类时抛出
     */
    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        // 第18行: 从Map中查找对应类名的字节码
        byte[] byteCode = compiledByteCodes.get(className);
        if (byteCode != null) {
            // 第20行: defineClass将字节码数组转换为Class<?>对象
            // 参数: 类名、字节码数组、起始位置（0）、长度（byteCode.length）
            // 这是ClassLoader的核心方法，将字节码转换为运行时Class对象
            return defineClass(className, byteCode, 0, byteCode.length);
        }
        // 第21行: 字节码不存在，委托给父ClassLoader
        // 通常父加载器也找不到，会抛出ClassNotFoundException
        return super.findClass(className);
    }
}