package com.compiler.ir;

import com.compiler.ast.*;

/**
 * 将 AST 转换为三地址码的简单生成器实现（模块 C 的基础实现）。
 */
public class IrGenerator implements AstVisitor<IrList> {

    private final IrList env = new IrList();

    public IrList generate(CompUnitNode unit) {
        IrList r = visitCompUnit(unit);
        env.addAll(r);
        return env;
    }

    @Override
    public IrList visitCompUnit(CompUnitNode node) {
        IrList res = new IrList();
        for (AstNode it : node.items) {
            if (it instanceof FuncDefNode) {
                res.addAll(((FuncDefNode) it).accept(this));
            } else if (it instanceof DeclNode) {
                // top-level decls: translate to global initialization as ASSIGN
                res.addAll(((DeclNode) it).accept(this));
            }
        }
        return res;
    }

    @Override
    public IrList visitConstDecl(ConstDeclNode node) {
        IrList res = new IrList();
        IrList initList = node.init.accept(this);
        res.addAll(initList);
        String resultTemp = initList.lastTemp();
        if (node.init.constValue != null) {
            res.add(IrInst.constant(node.name, node.init.constValue));
        } else if (resultTemp != null) {
            res.add(IrInst.assign(node.name, resultTemp));
        }
        return res;
    }

    @Override
    public IrList visitVarDecl(VarDeclNode node) {
        IrList res = new IrList();
        IrList init = node.init.accept(this);
        res.addAll(init);
        String resultTemp = init.lastTemp();
        if (node.init.constValue != null) {
            res.add(IrInst.constant(node.name, node.init.constValue));
        } else if (resultTemp != null) {
            res.add(IrInst.assign(node.name, resultTemp));
        }
        return res;
    }

    @Override
    public IrList visitBlockStmt(BlockStmtNode node) {
        IrList res = new IrList();
        for (AstNode n : node.stmts) {
            if (n instanceof StmtNode) {
                res.addAll(((StmtNode) n).accept(this));
            } else if (n instanceof DeclNode) {
                res.addAll(((DeclNode) n).accept(this));
            }
        }
        return res;
    }

    @Override
    public IrList visitEmptyStmt(EmptyStmtNode node) {
        return new IrList();
    }

    @Override
    public IrList visitExprStmt(ExprStmtNode node) {
        return node.expr.accept(this);
    }

    @Override
    public IrList visitAssignStmt(AssignStmtNode node) {
        IrList res = new IrList();
        IrList val = node.value.accept(this);
        res.addAll(val);
        String tmp = val.lastTemp();
        if (node.value.constValue != null) {
            res.add(IrInst.constant(node.name, node.value.constValue));
        } else {
            res.add(IrInst.assign(node.name, tmp));
        }
        return res;
    }

    @Override
    public IrList visitIfStmt(IfStmtNode node) {
        IrList res = new IrList();
        IrList cond = node.condition.accept(this);
        res.addAll(cond);
        String condTmp = cond.lastTemp();

        String labelElse = res.newLabel();
        String labelEnd = res.newLabel();

        res.add(IrInst.ifz(condTmp, labelElse));
        res.addAll(node.thenBranch.accept(this));
        res.add(IrInst.ggoto(labelEnd));
        res.add(IrInst.label(labelElse));
        if (node.elseBranch != null) {
            res.addAll(node.elseBranch.accept(this));
        }
        res.add(IrInst.label(labelEnd));
        return res;
    }

    @Override
    public IrList visitWhileStmt(WhileStmtNode node) {
        IrList res = new IrList();
        String labelBegin = res.newLabel();
        String labelEnd = res.newLabel();
        res.add(IrInst.label(labelBegin));
        IrList cond = node.condition.accept(this);
        res.addAll(cond);
        String condTmp = cond.lastTemp();
        res.add(IrInst.ifz(condTmp, labelEnd));
        res.addAll(node.body.accept(this));
        res.add(IrInst.ggoto(labelBegin));
        res.add(IrInst.label(labelEnd));
        return res;
    }

    @Override
    public IrList visitBreakStmt(BreakStmtNode node) {
        IrList res = new IrList();
        // caller/CodeGenerator负责填充正确的 label 名称；使用 GOTO BREAK as placeholder
        res.add(IrInst.ggoto("__BREAK"));
        return res;
    }

    @Override
    public IrList visitContinueStmt(ContinueStmtNode node) {
        IrList res = new IrList();
        res.add(IrInst.ggoto("__CONTINUE"));
        return res;
    }

    @Override
    public IrList visitReturnStmt(ReturnStmtNode node) {
        IrList res = new IrList();
        if (node.value != null) {
            IrList ev = node.value.accept(this);
            res.addAll(ev);
            String tmp = ev.lastTemp();
            if (node.value.constValue != null) {
                res.add(IrInst.ret(Integer.toString(node.value.constValue)));
            } else {
                res.add(IrInst.ret(tmp));
            }
        } else {
            res.add(IrInst.ret(null));
        }
        return res;
    }

    @Override
    public IrList visitFuncDef(FuncDefNode node) {
        IrList res = new IrList();
        res.add(IrInst.func(node.name));
        // params are handled by semantic layer; just generate body
        res.addAll(node.body.accept(this));
        res.add(IrInst.endFunc());
        return res;
    }

    @Override
    public IrList visitParam(ParamNode node) {
        return new IrList();
    }

    @Override
    public IrList visitBinaryExpr(BinaryExprNode node) {
        IrList res = new IrList();
        if (node.op == BinOp.AND) {
            // short-circuit AND
            IrList left = node.left.accept(this);
            res.addAll(left);
            String ltmp = left.newTemp();
            String lblFalse = res.newLabel();
            String lblEnd = res.newLabel();
            String dest = res.newTemp();
            res.add(IrInst.ifz(ltmp, lblFalse));
            IrList right = node.right.accept(this);
            res.addAll(right);
            String rtmp = right.newTemp();
            res.add(IrInst.bin(dest, "AND", ltmp, rtmp));
            res.add(IrInst.ggoto(lblEnd));
            res.add(IrInst.label(lblFalse));
            res.add(IrInst.constant(dest, 0));
            res.add(IrInst.label(lblEnd));
            return res;
        } else if (node.op == BinOp.OR) {
            // short-circuit OR
            IrList left = node.left.accept(this);
            res.addAll(left);
            String ltmp = left.newTemp();
            String lblTrue = res.newLabel();
            String lblEnd = res.newLabel();
            String dest = res.newTemp();
            res.add(IrInst.ifnz(ltmp, lblTrue));
            IrList right = node.right.accept(this);
            res.addAll(right);
            String rtmp = right.newTemp();
            res.add(IrInst.bin(dest, "OR", ltmp, rtmp));
            res.add(IrInst.ggoto(lblEnd));
            res.add(IrInst.label(lblTrue));
            res.add(IrInst.constant(dest, 1));
            res.add(IrInst.label(lblEnd));
            return res;
        } else {
            IrList left = node.left.accept(this);
            IrList right = node.right.accept(this);
            res.addAll(left);
            res.addAll(right);
            String a = left.newTemp();
            String b = right.newTemp();
            String dest = res.newTemp();
            res.add(IrInst.bin(dest, node.op.name(), a, b));
            return res;
        }
    }

    @Override
    public IrList visitUnaryExpr(UnaryExprNode node) {
        IrList res = new IrList();
        IrList val = node.operand.accept(this);
        res.addAll(val);
        String t = val.lastTemp();
        String dst = res.newTemp();
        switch (node.op) {
            case MINUS:
                res.add(IrInst.bin(dst, "NEG", t, null));
                break;
            case NOT:
                res.add(IrInst.bin(dst, "NOT", t, null));
                break;
            case PLUS:
            default:
                res.add(IrInst.assign(dst, t));
        }
        return res;
    }

    @Override
    public IrList visitId(IdNode node) {
        IrList res = new IrList();
        if (node.constValue != null) {
            String dst = res.newTemp();
            res.add(IrInst.constant(dst, node.constValue));
            return res;
        }
        String dst = res.newTemp();
        // load variable by name (ASSIGN temp = varName)
        res.add(IrInst.assign(dst, node.name));
        return res;
    }

    @Override
    public IrList visitIntLiteral(IntLiteralNode node) {
        IrList res = new IrList();
        String dst = res.newTemp();
        res.add(IrInst.constant(dst, node.value));
        return res;
    }

    @Override
    public IrList visitCallExpr(CallExprNode node) {
        IrList res = new IrList();
        // evaluate args
        for (ExprNode e : node.args) {
            res.addAll(e.accept(this));
        }
        String dst = res.newTemp();
        res.add(IrInst.call(dst, node.funcName));
        return res;
    }
}