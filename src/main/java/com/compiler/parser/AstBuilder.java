package com.compiler.parser;

import com.compiler.ast.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/**
 * ANTLR ParseTree → 自定义 AST 的转换器。
 */
public class AstBuilder extends ToyCBaseVisitor<AstNode> {

    @Override
    public AstNode visitCompUnit(ToyCParser.CompUnitContext ctx) {
        List<AstNode> items = new ArrayList<>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof ToyCParser.DeclContext declCtx) {
                items.add(visitDecl(declCtx));
            } else if (child instanceof ToyCParser.FuncDefContext funcCtx) {
                items.add(visitFuncDef(funcCtx));
            }
        }
        return new CompUnitNode(items);
    }

    @Override
    public AstNode visitDecl(ToyCParser.DeclContext ctx) {
        if (ctx.constDecl() != null) {
            return visitConstDecl(ctx.constDecl());
        }
        return visitVarDecl(ctx.varDecl());
    }

    @Override
    public AstNode visitConstDecl(ToyCParser.ConstDeclContext ctx) {
        String name = ctx.ID().getText();
        ExprNode init = (ExprNode) visit(ctx.expr());
        return new ConstDeclNode(name, init);
    }

    @Override
    public AstNode visitVarDecl(ToyCParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        ExprNode init = (ExprNode) visit(ctx.expr());
        return new VarDeclNode(name, init);
    }

    @Override
    public AstNode visitStmt(ToyCParser.StmtContext ctx) {
        if (ctx.block() != null) {
            return visitBlock(ctx.block());
        }
        if (ctx.IF() != null) {
            ExprNode cond = (ExprNode) visit(ctx.expr());
            StmtNode thenBranch = (StmtNode) visit(ctx.stmt(0));
            StmtNode elseBranch = ctx.ELSE() != null ? (StmtNode) visit(ctx.stmt(1)) : null;
            return new IfStmtNode(cond, thenBranch, elseBranch);
        }
        if (ctx.WHILE() != null) {
            ExprNode cond = (ExprNode) visit(ctx.expr());
            StmtNode body = (StmtNode) visit(ctx.stmt(0));
            return new WhileStmtNode(cond, body);
        }
        if (ctx.BREAK() != null) {
            return new BreakStmtNode();
        }
        if (ctx.CONTINUE() != null) {
            return new ContinueStmtNode();
        }
        if (ctx.RETURN() != null) {
            ExprNode value = ctx.expr() != null ? (ExprNode) visit(ctx.expr()) : null;
            return new ReturnStmtNode(value);
        }
        if (ctx.decl() != null) {
            return visitDecl(ctx.decl());
        }
        if (ctx.ID() != null && ctx.ASSIGN() != null) {
            String name = ctx.ID().getText();
            ExprNode value = (ExprNode) visit(ctx.expr());
            return new AssignStmtNode(name, value);
        }
        if (ctx.expr() != null) {
            return new ExprStmtNode((ExprNode) visit(ctx.expr()));
        }
        return new EmptyStmtNode();
    }

    @Override
    public AstNode visitBlock(ToyCParser.BlockContext ctx) {
        List<AstNode> stmts = new ArrayList<>();
        for (ToyCParser.StmtContext stmtCtx : ctx.stmt()) {
            stmts.add(visitStmt(stmtCtx));
        }
        return new BlockStmtNode(stmts);
    }

    @Override
    public AstNode visitFuncDef(ToyCParser.FuncDefContext ctx) {
        ValueType returnType = ctx.VOID() != null ? ValueType.VOID : ValueType.INT;
        String name = ctx.ID().getText();
        List<ParamNode> params = new ArrayList<>();
        if (ctx.paramList() != null) {
            for (ToyCParser.ParamContext paramCtx : ctx.paramList().param()) {
                params.add((ParamNode) visitParam(paramCtx));
            }
        }
        BlockStmtNode body = (BlockStmtNode) visitBlock(ctx.block());
        return new FuncDefNode(returnType, name, params, body);
    }

    @Override
    public AstNode visitParam(ToyCParser.ParamContext ctx) {
        return new ParamNode(ctx.ID().getText());
    }

    @Override
    public AstNode visitExpr(ToyCParser.ExprContext ctx) {
        return visit(ctx.lOrExpr());
    }

    @Override
    public AstNode visitLOrExpr(ToyCParser.LOrExprContext ctx) {
        if (ctx.lOrExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.lOrExpr());
            ExprNode right = (ExprNode) visit(ctx.lAndExpr());
            return new BinaryExprNode(BinOp.OR, left, right);
        }
        return visit(ctx.lAndExpr());
    }

    @Override
    public AstNode visitLAndExpr(ToyCParser.LAndExprContext ctx) {
        if (ctx.lAndExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.lAndExpr());
            ExprNode right = (ExprNode) visit(ctx.relExpr());
            return new BinaryExprNode(BinOp.AND, left, right);
        }
        return visit(ctx.relExpr());
    }

    @Override
    public AstNode visitRelExpr(ToyCParser.RelExprContext ctx) {
        if (ctx.relExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.relExpr());
            ExprNode right = (ExprNode) visit(ctx.addExpr());
            BinOp op = relOp(ctx);
            return new BinaryExprNode(op, left, right);
        }
        return visit(ctx.addExpr());
    }

    @Override
    public AstNode visitAddExpr(ToyCParser.AddExprContext ctx) {
        if (ctx.addExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.addExpr());
            ExprNode right = (ExprNode) visit(ctx.mulExpr());
            BinOp op = ctx.PLUS() != null ? BinOp.ADD : BinOp.SUB;
            return new BinaryExprNode(op, left, right);
        }
        return visit(ctx.mulExpr());
    }

    @Override
    public AstNode visitMulExpr(ToyCParser.MulExprContext ctx) {
        if (ctx.mulExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.mulExpr());
            ExprNode right = (ExprNode) visit(ctx.unaryExpr());
            BinOp op;
            if (ctx.STAR() != null) {
                op = BinOp.MUL;
            } else if (ctx.SLASH() != null) {
                op = BinOp.DIV;
            } else {
                op = BinOp.MOD;
            }
            return new BinaryExprNode(op, left, right);
        }
        return visit(ctx.unaryExpr());
    }

    @Override
    public AstNode visitUnaryExpr(ToyCParser.UnaryExprContext ctx) {
        if (ctx.unaryExpr() != null) {
            UnaryOp op;
            if (ctx.PLUS() != null) {
                op = UnaryOp.PLUS;
            } else if (ctx.MINUS() != null) {
                op = UnaryOp.MINUS;
            } else {
                op = UnaryOp.NOT;
            }
            ExprNode operand = (ExprNode) visit(ctx.unaryExpr());
            return new UnaryExprNode(op, operand);
        }
        return visit(ctx.primaryExpr());
    }

    @Override
    public AstNode visitPrimaryExpr(ToyCParser.PrimaryExprContext ctx) {
        if (ctx.NUMBER() != null) {
            return new IntLiteralNode(Integer.parseInt(ctx.NUMBER().getText()));
        }
        if (ctx.expr() != null) {
            return visit(ctx.expr());
        }
        String name = ctx.ID().getText();
        if (ctx.LPAREN() != null) {
            List<ExprNode> args = new ArrayList<>();
            if (ctx.argList() != null) {
                for (ToyCParser.ExprContext argCtx : ctx.argList().expr()) {
                    args.add((ExprNode) visit(argCtx));
                }
            }
            return new CallExprNode(name, args);
        }
        return new IdNode(name);
    }

    private static BinOp relOp(ToyCParser.RelExprContext ctx) {
        if (ctx.LT() != null) {
            return BinOp.LT;
        }
        if (ctx.GT() != null) {
            return BinOp.GT;
        }
        if (ctx.LE() != null) {
            return BinOp.LE;
        }
        if (ctx.GE() != null) {
            return BinOp.GE;
        }
        if (ctx.EQ() != null) {
            return BinOp.EQ;
        }
        return BinOp.NE;
    }
}
