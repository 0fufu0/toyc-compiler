package com.compiler.semantic;

import java.util.List;

import com.compiler.ast.ValueType;

/**
 * 语义分析器。
 *
 * 当前版本先提供语义分析所需的基础能力：
 * 1. 管理符号表
 * 2. 声明变量、常量、函数、参数
 * 3. 查找符号
 * 4. 抛出语义错误
 * 5. 预留 AST 分析入口
 * 6. 预留常量求值接口
 */
public class SemanticAnalyzer {

    private final SymbolTable symbolTable;

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
    }

    /**
     * 语义分析入口。
     *
     * 这里暂时使用 Object，避免提前依赖还没确认类名的 AST 节点。
     * 下一步确认 AST 类名后，可以改成：
     *
     * public void analyze(ASTNode root)
     */
    public void analyze(Object root) {
        if (root == null) {
            throw new SemanticError("AST root is null");
        }

        // TODO:
        // 下一步根据真实 AST 节点类型进行遍历。
        // 例如：
        // visitProgram(root);
        // visitFunction(...);
        // visitBlock(...);
        // visitStatement(...);
        // visitExpression(...);
    }

    /**
     * 进入一个新的作用域。
     *
     * 例如遇到：
     * {
     *     int a;
     * }
     */
    public void enterScope() {
        symbolTable.enterScope();
    }

    /**
     * 退出当前作用域。
     */
    public void exitScope() {
        symbolTable.exitScope();
    }

    /**
     * 声明普通变量。
     */
    public void declareVariable(String name, boolean isGlobal) {
        Symbol symbol = Symbol.variable(name, isGlobal);
        declareSymbol(symbol);
    }

    /**
     * 声明 const 常量。
     */
    public void declareConstant(String name, boolean isGlobal, int constValue) {
        Symbol symbol = Symbol.constant(name, isGlobal, constValue);
        declareSymbol(symbol);
    }

    /**
     * 声明函数形参。
     */
    public void declareParameter(String name) {
        Symbol symbol = Symbol.parameter(name);
        declareSymbol(symbol);
    }

    /**
     * 声明函数。
     */
    public void declareFunction(String name, ValueType returnType, List<ValueType> paramTypes) {
        Symbol symbol = Symbol.function(name, returnType, paramTypes);
        declareSymbol(symbol);
    }

    /**
     * 通用声明逻辑。
     *
     * 如果当前作用域已经存在同名符号，就报重复定义错误。
     */
    private void declareSymbol(Symbol symbol) {
        boolean success = symbolTable.putSymbol(symbol);

        if (!success) {
            throw new SemanticError("Duplicate declaration: " + symbol.name);
        }
    }

    /**
     * 查找符号。
     *
     * 如果找不到，就返回 null。
     */
    public Symbol lookupSymbol(String name) {
        return symbolTable.lookupSymbol(name);
    }

    /**
     * 查找必须存在的符号。
     *
     * 如果找不到，说明使用了未定义变量或函数。
     */
    public Symbol requireSymbol(String name) {
        Symbol symbol = symbolTable.lookupSymbol(name);

        if (symbol == null) {
            throw new SemanticError("Undefined symbol: " + name);
        }

        return symbol;
    }

    /**
     * 判断当前作用域中是否已经存在某个符号。
     */
    public boolean existsInCurrentScope(String name) {
        return symbolTable.existsInCurrentScope(name);
    }

    /**
     * 常量求值接口。
     *
     * 当前先预留。
     * 下一步接入 AST 表达式节点后，再具体实现：
     * 1 + 2 * 3 => 7
     * const int a = 5; a + 1 => 6
     *
     * 如果表达式不是编译期常量，则返回 null。
     */
    public Integer evalConst(Object expressionNode) {
        // TODO:
        // 下一步根据真实 AST 表达式节点类型实现。
        return null;
    }

    /**
     * 获取符号表。
     *
     * 后面测试或 IR 生成阶段可能会用到。
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}