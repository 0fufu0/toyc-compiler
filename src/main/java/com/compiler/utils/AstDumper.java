package com.compiler.utils;

import com.compiler.ast.*;

/**
 * 将 AST 以缩进树形文本输出，便于调试与单元测试断言。
 */
public final class AstDumper implements AstVisitor<Void> {

    private final StringBuilder out = new StringBuilder();
    private int indent;

    private AstDumper() {
    }

    public static String dump(AstNode root) {
        AstDumper dumper = new AstDumper();
        root.accept(dumper);
        return dumper.out.toString().trim();
    }

    private void line(String text) {
        out.append("  ".repeat(indent)).append(text).append('\n');
    }

    private void visitChildren(Iterable<? extends AstNode> nodes) {
        for (AstNode node : nodes) {
            node.accept(this);
        }
    }

    @Override
    public Void visitCompUnit(CompUnitNode node) {
        line("CompUnit");
        indent++;
        visitChildren(node.items);
        indent--;
        return null;
    }

    @Override
    public Void visitConstDecl(ConstDeclNode node) {
        line("ConstDecl(" + node.name + ")");
        indent++;
        node.init.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitVarDecl(VarDeclNode node) {
        line("VarDecl(" + node.name + ")");
        indent++;
        node.init.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitBlockStmt(BlockStmtNode node) {
        line("Block");
        indent++;
        visitChildren(node.stmts);
        indent--;
        return null;
    }

    @Override
    public Void visitEmptyStmt(EmptyStmtNode node) {
        line("EmptyStmt");
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmtNode node) {
        line("ExprStmt");
        indent++;
        node.expr.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitAssignStmt(AssignStmtNode node) {
        line("Assign(" + node.name + ")");
        indent++;
        node.value.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmtNode node) {
        line("If");
        indent++;
        line("cond");
        indent++;
        node.condition.accept(this);
        indent--;
        line("then");
        indent++;
        node.thenBranch.accept(this);
        indent--;
        if (node.elseBranch != null) {
            line("else");
            indent++;
            node.elseBranch.accept(this);
            indent--;
        }
        indent--;
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmtNode node) {
        line("While");
        indent++;
        line("cond");
        indent++;
        node.condition.accept(this);
        indent--;
        line("body");
        indent++;
        node.body.accept(this);
        indent--;
        indent--;
        return null;
    }

    @Override
    public Void visitBreakStmt(BreakStmtNode node) {
        line("Break");
        return null;
    }

    @Override
    public Void visitContinueStmt(ContinueStmtNode node) {
        line("Continue");
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmtNode node) {
        line("Return");
        if (node.value != null) {
            indent++;
            node.value.accept(this);
            indent--;
        }
        return null;
    }

    @Override
    public Void visitFuncDef(FuncDefNode node) {
        line("FuncDef(" + node.name + ", " + node.returnType + ")");
        indent++;
        for (ParamNode param : node.params) {
            param.accept(this);
        }
        node.body.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitParam(ParamNode node) {
        line("Param(" + node.name + ")");
        return null;
    }

    @Override
    public Void visitBinaryExpr(BinaryExprNode node) {
        line("Binary(" + node.op + ")");
        indent++;
        node.left.accept(this);
        node.right.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitUnaryExpr(UnaryExprNode node) {
        line("Unary(" + node.op + ")");
        indent++;
        node.operand.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitId(IdNode node) {
        line("Id(" + node.name + ")");
        return null;
    }

    @Override
    public Void visitIntLiteral(IntLiteralNode node) {
        line("Int(" + node.value + ")");
        return null;
    }

    @Override
    public Void visitCallExpr(CallExprNode node) {
        line("Call(" + node.funcName + ")");
        indent++;
        visitChildren(node.args);
        indent--;
        return null;
    }
}
