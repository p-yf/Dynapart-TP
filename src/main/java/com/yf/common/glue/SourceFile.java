package com.yf.common.glue;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * 封装待编译的Java源码字符串，适配编译器输入规范
 */
public class SourceFile extends SimpleJavaFileObject {
    private final String sourceCode;

    /**
     * @param className 类全名（如com.example.Test）
     * @param sourceCode 完整的Java类代码
     */
    public SourceFile(String className, String sourceCode) {
        super(URI.create("string:///" + className.replace('.', '/') + ".java"), Kind.SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return sourceCode;
    }
}
