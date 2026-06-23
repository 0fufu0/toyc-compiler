package com.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    // 作用域栈：每一层 Map 表示一个作用域
    private final Stack<Map<String, Symbol>> scopeStack;

    public SymbolTable() {
        this.scopeStack = new Stack<>();

        // 默认进入全局作用域
        this.enterScope();
    }

    // 进入新的作用域
    public void enterScope() {
        this.scopeStack.push(new HashMap<>());
    }

    // 离开当前作用域
    public void exitScope() {
        // 保留最外层全局作用域，避免整个符号表被清空
        if (this.scopeStack.size() > 1) {
            this.scopeStack.pop();
        }
    }

    // 将新符号加入当前作用域
    // 返回 false 表示当前作用域中已经有同名符号
    public boolean putSymbol(Symbol newSymbol) {
        Map<String, Symbol> currentScope = this.scopeStack.peek();

        if (currentScope.containsKey(newSymbol.name)) {
            return false;
        }

        currentScope.put(newSymbol.name, newSymbol);
        return true;
    }

    // 查找符号：从内层作用域向外层作用域查找
    public Symbol lookupSymbol(String targetName) {
        for (int i = this.scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = this.scopeStack.get(i);

            if (scope.containsKey(targetName)) {
                return scope.get(targetName);
            }
        }

        return null;
    }

    // 判断当前作用域是否已经存在某个符号
    public boolean existsInCurrentScope(String name) {
        Map<String, Symbol> currentScope = this.scopeStack.peek();
        return currentScope.containsKey(name);
    }
}