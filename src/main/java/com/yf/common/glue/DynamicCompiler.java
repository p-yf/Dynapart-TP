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
     * 编译Java代码字符串为Class对象
     * @param className 类全名（必须与代码中的类名一致）
     * @param javaCode 完整的Java类代码（public类）
     * @return 编译后的Class对象
     * @throws DynamicCompileException 编译失败时抛出
     * @throws ClassNotFoundException 类加载失败时抛出
     */
    public Class<?> compileToClass(String className, String javaCode) throws DynamicCompileException, ClassNotFoundException {
        // 1. 获取系统编译器（仅JDK环境可用）
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new DynamicCompileException("未找到Java编译器，请使用JDK运行（非JRE）");
        }

        // 2. 初始化内存文件管理器
        MemoryFileManager fileManager = new MemoryFileManager(
                compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
        );

        // 3. 收集编译诊断信息（错误/警告）
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // 4. 构建编译任务
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                Arrays.asList("-source", "21", "-target", "21"), // 需与运行环境JDK版本匹配
                null,
                Arrays.asList(new SourceFile(className, javaCode))
        );

        // 5. 执行编译
        boolean compileSuccess = task.call();
        if (!compileSuccess) {
            // 收集编译错误信息
            StringBuilder errorMsg = new StringBuilder("编译失败：\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMsg.append(diagnostic.getMessage(null)).append("\n");
            }
            throw new DynamicCompileException(errorMsg.toString());
        }

        // 6. 加载编译后的类
        MemoryClassLoader classLoader = new MemoryClassLoader(fileManager.getCompiledByteCodes());
        return classLoader.loadClass(className);
    }


}
