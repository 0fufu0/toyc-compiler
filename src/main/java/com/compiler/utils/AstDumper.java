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

    private void line(AstNode node, String text) {
        String pos = node.line > 0 ? " @" + node.line + ":" + node.column : "";
        out.append("  ".repeat(indent)).append(text).append(pos).append('\n');
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
        line(node, "CompUnit");
        indent++;
        visitChildren(node.items);
        indent--;
        return null;
    }

    @Override
    public Void visitConstDecl(ConstDeclNode node) {
        line(node, "ConstDecl(" + node.name + ")");
        indent++;
        node.init.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitVarDecl(VarDeclNode node) {
        line(node, "VarDecl(" + node.name + ")");
        indent++;
        node.init.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitBlockStmt(BlockStmtNode node) {
        line(node, "Block");
        indent++;
        visitChildren(node.stmts);
        indent--;
        return null;
    }

    @Override
    public Void visitEmptyStmt(EmptyStmtNode node) {
        line(node, "EmptyStmt");
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmtNode node) {
        line(node, "ExprStmt");
        indent++;
        node.expr.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitAssignStmt(AssignStmtNode node) {
        line(node, "Assign(" + node.name + ")");
        indent++;
        node.value.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmtNode node) {
        line(node, "If");
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
        line(node, "While");
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
        line(node, "Break");
        return null;
    }

    @Override
    public Void visitContinueStmt(ContinueStmtNode node) {
        line(node, "Continue");
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmtNode node) {
        line(node, "Return");
        if (node.value != null) {
            indent++;
            node.value.accept(this);
            indent--;
        }
        return null;
    }

    @Override
    public Void visitFuncDef(FuncDefNode node) {
        line(node, "FuncDef(" + node.name + ", " + node.returnType + ")");
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
        line(node, "Param(" + node.name + ")");
        return null;
    }

    @Override
    public Void visitBinaryExpr(BinaryExprNode node) {
        line(node, "Binary(" + node.op + ")");
        indent++;
        node.left.accept(this);
        node.right.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitUnaryExpr(UnaryExprNode node) {
        line(node, "Unary(" + node.op + ")");
        indent++;
        node.operand.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visitId(IdNode node) {
        line(node, "Id(" + node.name + ")");
        return null;
    }

    @Override
    public Void visitIntLiteral(IntLiteralNode node) {
        line(node, "Int(" + node.value + ")");
        return null;
    }

    @Override
    public Void visitCallExpr(CallExprNode node) {
        line(node, "Call(" + node.funcName + ")");
        indent++;
        visitChildren(node.args);
        indent--;
        return null;
    }
}
