package com.compiler.semantic;

import java.util.ArrayList;
import java.util.List;

import com.compiler.ast.AssignStmtNode;
import com.compiler.ast.AstNode;
import com.compiler.ast.AstVisitor;
import com.compiler.ast.BinaryExprNode;
import com.compiler.ast.BlockStmtNode;
import com.compiler.ast.BreakStmtNode;
import com.compiler.ast.CallExprNode;
import com.compiler.ast.CompUnitNode;
import com.compiler.ast.ConstDeclNode;
import com.compiler.ast.ContinueStmtNode;
import com.compiler.ast.EmptyStmtNode;
import com.compiler.ast.ExprNode;
import com.compiler.ast.ExprStmtNode;
import com.compiler.ast.FuncDefNode;
import com.compiler.ast.IdNode;
import com.compiler.ast.IfStmtNode;
import com.compiler.ast.IntLiteralNode;
import com.compiler.ast.ParamNode;
import com.compiler.ast.ReturnStmtNode;
import com.compiler.ast.UnaryExprNode;
import com.compiler.ast.ValueType;
import com.compiler.ast.VarDeclNode;
import com.compiler.ast.WhileStmtNode;

/**
 * 语义分析器。
 *
 * 主要职责： 1. 建立并维护符号表 2. 检查重复定义 3. 检查未定义变量/函数 4. 检查 const 是否被赋值 5. 检查
 * break/continue 是否在循环内 6. 检查 return 和函数返回类型是否匹配 7. 为 IdNode 填写 symbolRef 8.
 * 为常量表达式填写 constValue
 */
public class SemanticAnalyzer implements AstVisitor<Void> {

    private final SymbolTable symbolTable;

    private ValueType currentFunctionReturnType;
    private boolean currentFunctionHasReturn;
    private int loopDepth;
    private int irSymbolId = 0;

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.currentFunctionReturnType = null;
        this.currentFunctionHasReturn = false;
        this.loopDepth = 0;
    }

    /**
     * 语义分析入口。
     */
    public void analyze(AstNode root) {
        if (root == null) {
            throw new SemanticError("AST root is null");
        }

        root.accept(this);
    }

    /**
     * 兼容之前的 Object 版本入口。
     */
    public void analyze(Object root) {
        if (!(root instanceof AstNode)) {
            throw new SemanticError("Root is not an AST node");
        }

        analyze((AstNode) root);
    }

    /**
     * 编译单元。
     *
     * 顶层 items 里可能有： 1. 全局变量声明 2. 全局常量声明 3. 函数定义
     */
    @Override
    public Void visitCompUnit(CompUnitNode node) {
        /*
     * 第一遍：先收集所有函数名。
     *
     * 这样可以允许函数之间互相调用。
     * 例如 main 在前面调用 foo，而 foo 定义在后面。
         */
        FuncDefNode mainFunction = null;

        for (AstNode item : node.items) {
            if (item instanceof FuncDefNode) {
                FuncDefNode func = (FuncDefNode) item;

                List<ValueType> paramTypes = new ArrayList<>();
                for (ParamNode ignored : func.params) {
                    paramTypes.add(ValueType.INT);
                }

                declareFunction(func.name, func.returnType, paramTypes);

                if ("main".equals(func.name)) {
                    mainFunction = func;
                }
            }
        }

        /*
     * 检查 main 函数。
     *
     * ToyC 程序入口要求：
     * int main()
         */
        if (mainFunction == null) {
            throw new SemanticError("Missing main function");
        }

        if (mainFunction.returnType != ValueType.INT) {
            throw new SemanticError("main function must return int");
        }

        if (!mainFunction.params.isEmpty()) {
            throw new SemanticError("main function should not have parameters");
        }

        /*
     * 第二遍：真正检查全局声明和函数体。
         */
        for (AstNode item : node.items) {
            item.accept(this);
        }

        return null;
    }

    /**
     * 常量声明：const int name = init;
     */
    @Override
    public Void visitConstDecl(ConstDeclNode node) {
        if (node.init == null) {
            throw new SemanticError("Const declaration must have initializer: " + node.name);
        }

        Integer value = evalConst(node.init);

        if (value == null) {
            throw new SemanticError("Const initializer must be constant: " + node.name);
        }

        boolean isGlobal = currentFunctionReturnType == null;
        node.symbolRef = declareConstant(node.name, isGlobal, value);

        return null;
    }

    /**
     * 变量声明：int name = init;
     */
    @Override
    public Void visitVarDecl(VarDeclNode node) {
        if (node.init != null) {
            node.init.accept(this);
        }

        boolean isGlobal = currentFunctionReturnType == null;
        node.symbolRef = declareVariable(node.name, isGlobal);

        return null;
    }

    /**
     * 代码块：{ ... }
     */
    @Override
    public Void visitBlockStmt(BlockStmtNode node) {
        visitBlockStmt(node, true);
        return null;
    }

    /**
     * 内部辅助方法。
     *
     * createNewScope 为 true：普通 block，需要新作用域。 createNewScope 为 false：函数体
     * block，参数和函数体共享同一层作用域。
     */
    private void visitBlockStmt(BlockStmtNode node, boolean createNewScope) {
        if (createNewScope) {
            symbolTable.enterScope();
        }

        try {
            for (AstNode stmt : node.stmts) {
                stmt.accept(this);
            }
        } finally {
            if (createNewScope) {
                symbolTable.exitScope();
            }
        }
    }

    /**
     * 空语句：;
     */
    @Override
    public Void visitEmptyStmt(EmptyStmtNode node) {
        return null;
    }

    /**
     * 表达式语句：expr;
     */
    @Override
    public Void visitExprStmt(ExprStmtNode node) {
        if (node.expr != null) {
            node.expr.accept(this);
        }

        return null;
    }

    /**
     * 赋值语句：name = value;
     */
    @Override
    public Void visitAssignStmt(AssignStmtNode node) {
        Symbol symbol = requireSymbol(node.name);

        if (symbol.isConst) {
            throw new SemanticError("Cannot assign to const: " + node.name);
        }

        node.symbolRef = symbol;

        if (node.value != null) {
            node.value.accept(this);
        }

        return null;
    }

    /**
     * if 语句。
     */
    @Override
    public Void visitIfStmt(IfStmtNode node) {
        if (node.condition != null) {
            node.condition.accept(this);
        }

        if (node.thenBranch != null) {
            node.thenBranch.accept(this);
        }

        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
        }

        return null;
    }

    /**
     * while 语句。
     */
    @Override
    public Void visitWhileStmt(WhileStmtNode node) {
        if (node.condition != null) {
            node.condition.accept(this);
        }

        loopDepth++;

        try {
            if (node.body != null) {
                node.body.accept(this);
            }
        } finally {
            loopDepth--;
        }

        return null;
    }

    /**
     * break 语句。
     */
    @Override
    public Void visitBreakStmt(BreakStmtNode node) {
        if (loopDepth <= 0) {
            throw new SemanticError("break statement not within loop");
        }

        return null;
    }

    /**
     * continue 语句。
     */
    @Override
    public Void visitContinueStmt(ContinueStmtNode node) {
        if (loopDepth <= 0) {
            throw new SemanticError("continue statement not within loop");
        }

        return null;
    }

    /**
     * return 语句。
     */
    @Override
    public Void visitReturnStmt(ReturnStmtNode node) {
        if (currentFunctionReturnType == null) {
            throw new SemanticError("return statement not within function");
        }

        currentFunctionHasReturn = true;

        if (currentFunctionReturnType == ValueType.VOID) {
            if (node.value != null) {
                throw new SemanticError("void function should not return a value");
            }
        } else if (currentFunctionReturnType == ValueType.INT) {
            if (node.value == null) {
                throw new SemanticError("int function should return a value");
            }

            node.value.accept(this);
        }

        return null;
    }

    /**
     * 函数定义。
     */
    @Override
    public Void visitFuncDef(FuncDefNode node) {
        ValueType previousReturnType = currentFunctionReturnType;
        boolean previousHasReturn = currentFunctionHasReturn;

        currentFunctionReturnType = node.returnType;
        currentFunctionHasReturn = false;

        /*
     * 函数作用域：
     * 参数和函数体第一层变量在同一个作用域内。
         */
        symbolTable.enterScope();

        try {
            for (ParamNode param : node.params) {
                param.symbolRef = declareParameter(param.name);
            }

            if (node.body != null) {
                visitBlockStmt(node.body, false);
            }

            if (node.returnType == ValueType.INT && !currentFunctionHasReturn) {
                throw new SemanticError("int function must have a return statement: " + node.name);
            }
        } finally {
            symbolTable.exitScope();
            currentFunctionReturnType = previousReturnType;
            currentFunctionHasReturn = previousHasReturn;
        }

        return null;
    }

    /**
     * 函数参数。
     *
     * 目前 FuncDefNode 里已经手动声明参数了， 这个方法保留给以后直接 visitParam 时使用。
     */
    @Override
    public Void visitParam(ParamNode node) {
        node.symbolRef = declareParameter(node.name);
        return null;
    }

    /**
     * 二元表达式。
     */
    @Override
    public Void visitBinaryExpr(BinaryExprNode node) {
        node.left.accept(this);
        node.right.accept(this);

        Integer leftValue = node.left.constValue;
        Integer rightValue = node.right.constValue;

        if (leftValue == null || rightValue == null) {
            node.constValue = null;
            return null;
        }

        int result;

        switch (node.op) {
            case ADD:
                result = leftValue + rightValue;
                break;

            case SUB:
                result = leftValue - rightValue;
                break;

            case MUL:
                result = leftValue * rightValue;
                break;

            case DIV:
                if (rightValue == 0) {
                    throw new SemanticError("Division by zero in constant expression");
                }
                result = leftValue / rightValue;
                break;

            case MOD:
                if (rightValue == 0) {
                    throw new SemanticError("Modulo by zero in constant expression");
                }
                result = leftValue % rightValue;
                break;

            case LT:
                result = leftValue < rightValue ? 1 : 0;
                break;

            case GT:
                result = leftValue > rightValue ? 1 : 0;
                break;

            case LE:
                result = leftValue <= rightValue ? 1 : 0;
                break;

            case GE:
                result = leftValue >= rightValue ? 1 : 0;
                break;

            case EQ:
                result = leftValue.equals(rightValue) ? 1 : 0;
                break;

            case NE:
                result = !leftValue.equals(rightValue) ? 1 : 0;
                break;

            case AND:
                result = (leftValue != 0 && rightValue != 0) ? 1 : 0;
                break;

            case OR:
                result = (leftValue != 0 || rightValue != 0) ? 1 : 0;
                break;

            default:
                node.constValue = null;
                return null;
        }

        node.constValue = result;
        return null;
    }

    /**
     * 一元表达式。
     */
    @Override
    public Void visitUnaryExpr(UnaryExprNode node) {
        node.operand.accept(this);

        Integer value = node.operand.constValue;

        if (value == null) {
            node.constValue = null;
            return null;
        }

        switch (node.op) {
            case PLUS:
                node.constValue = value;
                break;

            case MINUS:
                node.constValue = -value;
                break;

            case NOT:
                node.constValue = value == 0 ? 1 : 0;
                break;

            default:
                node.constValue = null;
                break;
        }

        return null;
    }

    /**
     * 标识符表达式。
     */
    @Override
    public Void visitId(IdNode node) {
        Symbol symbol = requireSymbol(node.name);

        if (symbol.symbolType == SymbolType.FUNCTION) {
            throw new SemanticError("Function name used as variable: " + node.name);
        }

        node.symbolRef = symbol;

        if (symbol.isConst) {
            node.constValue = symbol.constValue;
        } else {
            node.constValue = null;
        }

        return null;
    }

    /**
     * 整数字面量。
     */
    @Override
    public Void visitIntLiteral(IntLiteralNode node) {
        node.constValue = node.value;
        return null;
    }

    /**
     * 函数调用表达式。
     */
    @Override
    public Void visitCallExpr(CallExprNode node) {
        Symbol symbol = requireSymbol(node.funcName);

        if (symbol.symbolType != SymbolType.FUNCTION) {
            throw new SemanticError("Symbol is not a function: " + node.funcName);
        }

        int expectedArgCount = symbol.paramTypes == null ? 0 : symbol.paramTypes.size();
        int actualArgCount = node.args == null ? 0 : node.args.size();

        if (expectedArgCount != actualArgCount) {
            throw new SemanticError(
                    "Function argument count mismatch: " + node.funcName
                    + ", expected " + expectedArgCount
                    + ", got " + actualArgCount
            );
        }

        for (ExprNode arg : node.args) {
            arg.accept(this);
        }

        node.constValue = null;
        return null;
    }

    /**
     * 声明普通变量。
     */
    public Symbol declareVariable(String name, boolean isGlobal) {
        Symbol symbol = Symbol.variable(name, isGlobal);
        if (!isGlobal) {
            symbol.irName = "__local_" + (irSymbolId++) + "_" + name;
        }
        declareSymbol(symbol);
        return symbol;
    }

    /**
     * 声明 const 常量。
     */
    public Symbol declareConstant(String name, boolean isGlobal, int constValue) {
        Symbol symbol = Symbol.constant(name, isGlobal, constValue);
        if (!isGlobal) {
            symbol.irName = "__local_" + (irSymbolId++) + "_" + name;
        }
        declareSymbol(symbol);
        return symbol;
    }

    /**
     * 声明函数参数。
     */
    public Symbol declareParameter(String name) {
        Symbol symbol = Symbol.parameter(name);
        symbol.irName = "__param_" + (irSymbolId++) + "_" + name;
        declareSymbol(symbol);
        return symbol;
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
     */
    private void declareSymbol(Symbol symbol) {
        boolean success = symbolTable.putSymbol(symbol);

        if (!success) {
            throw new SemanticError("Duplicate declaration: " + symbol.name);
        }
    }

    /**
     * 查找符号。
     */
    public Symbol lookupSymbol(String name) {
        return symbolTable.lookupSymbol(name);
    }

    /**
     * 查找必须存在的符号。
     */
    public Symbol requireSymbol(String name) {
        Symbol symbol = symbolTable.lookupSymbol(name);

        if (symbol == null) {
            throw new SemanticError("Undefined symbol: " + name);
        }

        return symbol;
    }

    /**
     * 计算常量表达式。
     *
     * 如果不是常量表达式，返回 null。
     */
    public Integer evalConst(ExprNode expressionNode) {
        if (expressionNode == null) {
            return null;
        }

        expressionNode.accept(this);
        return expressionNode.constValue;
    }

    /**
     * 兼容 Object 版本。
     */
    public Integer evalConst(Object expressionNode) {
        if (expressionNode == null) {
            return null;
        }

        if (!(expressionNode instanceof ExprNode)) {
            throw new SemanticError("Node is not an expression");
        }

        return evalConst((ExprNode) expressionNode);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}
