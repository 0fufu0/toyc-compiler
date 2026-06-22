package com.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {

    // 作用域堆疊，完美契合你的 lowerCamelCase 命名習慣
    private final Stack<Map<String, Symbol>> scopeStack;

    public SymbolTable() {
        this.scopeStack = new Stack<>();
        // 系統啟動時，預設進入全域作用域
        this.enterScope();
    }

    // 進入新的作用域 (遇到 '{' 時呼叫)
    public void enterScope() {
        this.scopeStack.push(new HashMap<>());
    }

    // 離開當前作用域 (遇到 '}' 時呼叫)
    public void exitScope() {
        if (this.scopeStack.size() > 1) {
            this.scopeStack.pop();
        }
    }

    // 將新符號加入當前作用域
    // 回傳 boolean 代表是否加入成功 (用來檢查變數是否重複宣告)
    public boolean putSymbol(Symbol newSymbol) {
        Map<String, Symbol> currentScope = this.scopeStack.peek();

        // 如果當前作用域已經有同名變數，回傳 false 報錯
        if (currentScope.containsKey(newSymbol.symbolName)) {
            return false;
        }

        currentScope.put(newSymbol.symbolName, newSymbol);
        return true;
    }

    // 查找符號 (由內層作用域向外層全域查找)
    // 就像你匹配候選人一樣，逐層遍歷尋找符合條件的結果
    public Symbol lookupSymbol(String targetName) {
        for (int i = this.scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = this.scopeStack.get(i);
            if (scope.containsKey(targetName)) {
                return scope.get(targetName);
            }
        }
        // 如果所有層級都找不到，回傳 null，代表使用了未宣告的變數
        return null;
    }
}


