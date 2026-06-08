package com.yf.common.glue;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理编译过程中的源码输入和字节码输出，全部在内存中完成
 */
public class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    // 第14-15行: 成员变量，key是类全名如"com.example.Test"，value是对应的ByteCodeFile对象
    // 存储所有编译产生的字节码文件
    private final Map<String, ByteCodeFile> byteCodeFiles = new HashMap<>();

    // 第17-18行: 构造函数
    // 参数parent是标准文件管理器，由compiler.getStandardFileManager()创建
    // ForwardingJavaFileManager会将parent代理起来，其他文件操作仍然委托给parent
    public MemoryFileManager(JavaFileManager parent) {
        super(parent);
    }

    /**
     * 第22-29行: 核心方法 - 为编译器提供编译输出位置
     * 当编译器需要写入.class文件时调用此方法
     * @param location 文件位置（如SOURCE_PATH、CLASS_OUTPUT等）
     * @param className 类全名
     * @param kind 文件类型（.java、.class、.jar等），此处用的是Kind.CLASS
     * @param sibling 兄弟文件对象（用于关联，可为null）
     * @return 返回一个ByteCodeFile作为编译输出目标
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        // 第26行: 创建一个内存字节码文件ByteCodeFile
        // 这个ByteCodeFile内部包装了ByteArrayOutputStream，用于接收编译器写入的字节码
        ByteCodeFile byteCodeFile = new ByteCodeFile(className);
        // 第27行: 存入map，以类全名为key，方便后续取出字节码
        byteCodeFiles.put(className, byteCodeFile);
        // 第28行: 返回给编译器，编译器会调用openOutputStream()写入字节码
        return byteCodeFile;
    }

    /**
     * 第32-38行: 编译完成后获取所有字节码
     * @return Map，key是类全名，value是字节码数组
     */
    public Map<String, byte[]> getCompiledByteCodes() {
        Map<String, byte[]> result = new HashMap<>();
        // 第36行: 遍历所有ByteCodeFile，调用getByteCode()提取字节码
        byteCodeFiles.forEach((className, file) -> result.put(className, file.getByteCode()));
        return result;
    }
}