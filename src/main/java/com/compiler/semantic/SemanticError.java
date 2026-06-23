package com.compiler.semantic;

/**
 * 语义分析错误。
 *
 * 例如：
 * 1. 变量重复定义
 * 2. 使用未定义变量
 * 3. const 常量被修改
 * 4. 函数参数数量不匹配
 * 5. return 类型错误
 */
public class SemanticError extends RuntimeException {

    public SemanticError(String message) {
        super(message);
    }

    public SemanticError(String message, Throwable cause) {
        super(message, cause);
    }
}