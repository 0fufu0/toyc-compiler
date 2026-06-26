package com.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import com.compiler.ast.AssignStmtNode;
import com.compiler.ast.AstNode;
import com.compiler.ast.BinOp;
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
import com.compiler.ast.StmtNode;
import com.compiler.ast.UnaryExprNode;
import com.compiler.ast.UnaryOp;
import com.compiler.ast.ValueType;
import com.compiler.ast.VarDeclNode;
import com.compiler.ast.WhileStmtNode;

/**
 * ANTLR ParseTree → 自定义 AST 的转换器。
 */
public class AstBuilder extends ToyCBaseVisitor<AstNode> {

    private static <T extends AstNode> T withPos(T node, ParserRuleContext ctx) {
        if (ctx != null && ctx.start != null) {
            node.line = ctx.start.getLine();
            node.column = ctx.start.getCharPositionInLine() + 1;
        }
        return node;
    }

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
        return withPos(new CompUnitNode(items), ctx);
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
        return withPos(new ConstDeclNode(name, init), ctx);
    }

    @Override
    public AstNode visitVarDecl(ToyCParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        ExprNode init = (ExprNode) visit(ctx.expr());
        return withPos(new VarDeclNode(name, init), ctx);
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
            return withPos(new IfStmtNode(cond, thenBranch, elseBranch), ctx);
        }
        if (ctx.WHILE() != null) {
            ExprNode cond = (ExprNode) visit(ctx.expr());
            StmtNode body = (StmtNode) visit(ctx.stmt(0));
            return withPos(new WhileStmtNode(cond, body), ctx);
        }
        if (ctx.BREAK() != null) {
            return withPos(new BreakStmtNode(), ctx);
        }
        if (ctx.CONTINUE() != null) {
            return withPos(new ContinueStmtNode(), ctx);
        }
        if (ctx.RETURN() != null) {
            ExprNode value = ctx.expr() != null ? (ExprNode) visit(ctx.expr()) : null;
            return withPos(new ReturnStmtNode(value), ctx);
        }
        if (ctx.decl() != null) {
            return visitDecl(ctx.decl());
        }
        if (ctx.ID() != null && ctx.ASSIGN() != null) {
            String name = ctx.ID().getText();
            ExprNode value = (ExprNode) visit(ctx.expr());
            return withPos(new AssignStmtNode(name, value), ctx);
        }
        if (ctx.expr() != null) {
            return withPos(new ExprStmtNode((ExprNode) visit(ctx.expr())), ctx);
        }
        return withPos(new EmptyStmtNode(), ctx);
    }

    @Override
    public AstNode visitBlock(ToyCParser.BlockContext ctx) {
        List<AstNode> stmts = new ArrayList<>();
        for (ToyCParser.StmtContext stmtCtx : ctx.stmt()) {
            stmts.add(visitStmt(stmtCtx));
        }
        return withPos(new BlockStmtNode(stmts), ctx);
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
        return withPos(new FuncDefNode(returnType, name, params, body), ctx);
    }

    @Override
    public AstNode visitParam(ToyCParser.ParamContext ctx) {
        return withPos(new ParamNode(ctx.ID().getText()), ctx);
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
            return withPos(new BinaryExprNode(BinOp.OR, left, right), ctx);
        }
        return visit(ctx.lAndExpr());
    }

    @Override
    public AstNode visitLAndExpr(ToyCParser.LAndExprContext ctx) {
        if (ctx.lAndExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.lAndExpr());
            ExprNode right = (ExprNode) visit(ctx.relExpr());
            return withPos(new BinaryExprNode(BinOp.AND, left, right), ctx);
        }
        return visit(ctx.relExpr());
    }

    @Override
    public AstNode visitRelExpr(ToyCParser.RelExprContext ctx) {
        if (ctx.relExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.relExpr());
            ExprNode right = (ExprNode) visit(ctx.addExpr());
            BinOp op = relOp(ctx);
            return withPos(new BinaryExprNode(op, left, right), ctx);
        }
        return visit(ctx.addExpr());
    }

    @Override
    public AstNode visitAddExpr(ToyCParser.AddExprContext ctx) {
        if (ctx.addExpr() != null) {
            ExprNode left = (ExprNode) visit(ctx.addExpr());
            ExprNode right = (ExprNode) visit(ctx.mulExpr());
            BinOp op = ctx.PLUS() != null ? BinOp.ADD : BinOp.SUB;
            return withPos(new BinaryExprNode(op, left, right), ctx);
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
            return withPos(new BinaryExprNode(op, left, right), ctx);
        }
        return visit(ctx.unaryExpr());
    }

    @Override
    public AstNode visitUnaryExpr(ToyCParser.UnaryExprContext ctx) {
        if (ctx.unaryExpr() != null) {
            // 把 -42 这种负数字面量折叠成 IntLiteralNode(-42)
            // 这样既能避免 x-1 被词法器识别错，又能兼容原来的测试。
            if (ctx.MINUS() != null) {
                ToyCParser.UnaryExprContext child = ctx.unaryExpr();

                if (child != null
                        && child.primaryExpr() != null
                        && child.primaryExpr().NUMBER() != null) {
                    String text = child.primaryExpr().NUMBER().getText();

                    if ("2147483648".equals(text)) {
                        return withPos(new IntLiteralNode(Integer.MIN_VALUE), ctx);
                    }

                    long value = Long.parseLong(text);
                    if (value <= Integer.MAX_VALUE) {
                        return withPos(new IntLiteralNode((int) -value), ctx);
                    }
                }
            }

            UnaryOp op;
            if (ctx.PLUS() != null) {
                op = UnaryOp.PLUS;
            } else if (ctx.MINUS() != null) {
                op = UnaryOp.MINUS;
            } else {
                op = UnaryOp.NOT;
            }

            ExprNode operand = (ExprNode) visit(ctx.unaryExpr());
            return withPos(new UnaryExprNode(op, operand), ctx);
        }

        return visit(ctx.primaryExpr());
    }

    @Override
    public AstNode visitPrimaryExpr(ToyCParser.PrimaryExprContext ctx) {
        if (ctx.NUMBER() != null) {
            return withPos(new IntLiteralNode(Integer.parseInt(ctx.NUMBER().getText())), ctx);
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
            return withPos(new CallExprNode(name, args), ctx);
        }
        return withPos(new IdNode(name), ctx);
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
