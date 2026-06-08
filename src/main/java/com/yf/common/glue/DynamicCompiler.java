package com.yf.common.glue;

import com.yf.common.exception.DynamicCompileException;

import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 动态编译入口类，提供Java代码字符串到Class对象的编译能力
 */
public class DynamicCompiler {

    /**
     * 第14-22行: 核心方法 - 将Java源码字符串编译为Class对象
     * @param className 类全名（必须与代码中的类名一致）
     * @param javaCode 完整的Java类代码（必须包含public类声明）
     * @return 编译后的Class对象
     * @throws DynamicCompileException 编译失败时抛出（语法错误等）
     * @throws ClassNotFoundException 类加载失败时抛出
     */
    public Class<?> compileToClass(String className, String javaCode)
            throws DynamicCompileException, ClassNotFoundException {
        // 第23-27行: 第1步 - 获取JDK内置的JavaCompiler
        // ToolProvider.getSystemJavaCompiler() 是JDK提供的编译器入口
        // 注意: JRE没有这个类，必须使用JDK运行才有效
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new DynamicCompileException("未找到Java编译器，请使用JDK运行（非JRE）");
        }

        // 第29-32行: 第2步 - 创建内存文件管理器
        // compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
        // 获取标准文件管理器，用于读取依赖类的源码信息
        // MemoryFileManager 继承自 ForwardingJavaFileManager，代理标准文件管理器
        // 重写了 getJavaFileForOutput()，将编译输出定向到内存而非磁盘
        MemoryFileManager fileManager = new MemoryFileManager(
                compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
        );

        // 第34-35行: 第3步 - 创建诊断收集器
        // DiagnosticCollector 会收集编译过程中的所有错误和警告信息
        // 用于编译失败时向用户报告具体的错误位置和原因
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // 第37-45行: 第4步 - 创建编译任务
        // compiler.getTask() 创建一个可中断的编译任务
        // 参数1: Writer - 编译器输出的地方，null表示不输出
        // 参数2: JavaFileManager - 管理输入输出的文件对象，此处用MemoryFileManager接管输出
        // 参数3: DiagnosticCollector - 收集错误和警告
        // 参数4: Iterable<String> - 编译选项，-source 21 -target 21表示编译到Java 21
        // 参数5: Iterable<String> - 要处理的注解（暂不需要，传入null）
        // 参数6: Iterable<JavaFileObject> - 要编译的源码输入，此处传入包装后的SourceFile
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                Arrays.asList("-source", "21", "-target", "21"),
                null,
                Arrays.asList(new SourceFile(className, javaCode))
        );

        // 第47-56行: 第5步 - 执行编译
        // task.call() 阻塞等待编译完成，返回true表示成功，false表示失败
        boolean compileSuccess = task.call();
        if (!compileSuccess) {
            // 编译失败，遍历所有诊断信息，构建错误消息
            StringBuilder errorMsg = new StringBuilder("编译失败：\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                // diagnostic.getMessage(null) 包含行号、列号、错误描述
                errorMsg.append(diagnostic.getMessage(null)).append("\n");
            }
            throw new DynamicCompileException(errorMsg.toString());
        }

        // 第58-60行: 第6步 - 加载编译后的类
        // fileManager.getCompiledByteCodes() 获取Map<String, byte[]>
        // 键是类全名，值是编译后的字节码数组
        MemoryClassLoader classLoader = new MemoryClassLoader(fileManager.getCompiledByteCodes());
        // loadClass是ClassLoader的方法，触发findClass从内存字节码加载类
        return classLoader.loadClass(className);
    }


}