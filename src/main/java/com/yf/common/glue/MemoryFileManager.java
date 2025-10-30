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
    // 存储类全名与字节码文件的映射
    private final Map<String, ByteCodeFile> byteCodeFiles = new HashMap<>();

    public MemoryFileManager(JavaFileManager parent) {
        super(parent);
    }

    /**
     * 为编译器提供输出目标（内存中的字节码文件）
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        ByteCodeFile byteCodeFile = new ByteCodeFile(className);
        byteCodeFiles.put(className, byteCodeFile);
        return byteCodeFile;
    }

    /**
     * 获取所有编译生成的字节码（类全名 -> 字节数组）
     */
    public Map<String, byte[]> getCompiledByteCodes() {
        Map<String, byte[]> result = new HashMap<>();
        byteCodeFiles.forEach((className, file) -> result.put(className, file.getByteCode()));
        return result;
    }
}
