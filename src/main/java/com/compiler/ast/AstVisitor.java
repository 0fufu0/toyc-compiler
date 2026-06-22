package com.compiler.ast;

/**
 * AST Visitor 接口。B/C 分别实现语义分析与 IR 生成。
 *
 * @param <T> 访问返回值类型
 */
public interface AstVisitor<T> {

    T visitCompUnit(CompUnitNode node);

    T visitConstDecl(ConstDeclNode node);

    T visitVarDecl(VarDeclNode node);

    T visitBlockStmt(BlockStmtNode node);

    T visitEmptyStmt(EmptyStmtNode node);

    T visitExprStmt(ExprStmtNode node);

    T visitAssignStmt(AssignStmtNode node);

    T visitIfStmt(IfStmtNode node);

    T visitWhileStmt(WhileStmtNode node);

    T visitBreakStmt(BreakStmtNode node);

    T visitContinueStmt(ContinueStmtNode node);

    T visitReturnStmt(ReturnStmtNode node);

    T visitFuncDef(FuncDefNode node);

    T visitParam(ParamNode node);

    T visitBinaryExpr(BinaryExprNode node);

    T visitUnaryExpr(UnaryExprNode node);

    T visitId(IdNode node);

    T visitIntLiteral(IntLiteralNode node);

    T visitCallExpr(CallExprNode node);
}
