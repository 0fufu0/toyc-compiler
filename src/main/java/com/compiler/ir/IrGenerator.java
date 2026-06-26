package com.compiler.ir;

import java.util.Stack;

import com.compiler.ast.AssignStmtNode;
import com.compiler.ast.AstNode;
import com.compiler.ast.AstVisitor;
import com.compiler.ast.BinOp;
import com.compiler.ast.BinaryExprNode;
import com.compiler.ast.BlockStmtNode;
import com.compiler.ast.BreakStmtNode;
import com.compiler.ast.CallExprNode;
import com.compiler.ast.CompUnitNode;
import com.compiler.ast.ConstDeclNode;
import com.compiler.ast.ContinueStmtNode;
import com.compiler.ast.DeclNode;
import com.compiler.ast.EmptyStmtNode;
import com.compiler.ast.ExprStmtNode;
import com.compiler.ast.FuncDefNode;
import com.compiler.ast.IdNode;
import com.compiler.ast.IfStmtNode;
import com.compiler.ast.IntLiteralNode;
import com.compiler.ast.ParamNode;
import com.compiler.ast.ReturnStmtNode;
import com.compiler.ast.StmtNode;
import com.compiler.ast.UnaryExprNode;
import com.compiler.ast.VarDeclNode;
import com.compiler.ast.WhileStmtNode;

/**
 * 将 AST 转换为三地址码的简单生成器实现（模块 C 的基础实现）。
 */
public class IrGenerator implements AstVisitor<IrList> {

    private final IrList env = new IrList();

    /**
     * 循环标签栈，用于处理 break 和 continue 语句 每个元素是一个数组，[0] = break目标标签, [1] =
     * continue目标标签
     */
    private final Stack<String[]> loopLabels = new Stack<>();

    private String irName(com.compiler.semantic.Symbol symbol, String fallback) {
        if (symbol != null && symbol.irName != null) {
            return symbol.irName;
        }
        return fallback;
    }

    public IrList generate(CompUnitNode unit) {
        IrList.resetCounters();

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
            res.add(IrInst.constant(irName(node.symbolRef, node.name), node.init.constValue));
        } else if (resultTemp != null) {
            res.add(IrInst.assign(irName(node.symbolRef, node.name), resultTemp));
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
            res.add(IrInst.constant(irName(node.symbolRef, node.name), node.init.constValue));
        } else if (resultTemp != null) {
            res.add(IrInst.assign(irName(node.symbolRef, node.name), resultTemp));
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
            res.add(IrInst.constant(irName(node.symbolRef, node.name), node.value.constValue));
        } else {
            res.add(IrInst.assign(irName(node.symbolRef, node.name), tmp));
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

        // 将循环标签压入栈，供 break/continue 使用
        loopLabels.push(new String[]{labelEnd, labelBegin});

        res.add(IrInst.label(labelBegin));
        IrList cond = node.condition.accept(this);
        res.addAll(cond);
        String condTmp = cond.lastTemp();
        res.add(IrInst.ifz(condTmp, labelEnd));
        res.addAll(node.body.accept(this));
        res.add(IrInst.ggoto(labelBegin));
        res.add(IrInst.label(labelEnd));

        // 循环结束，弹出标签栈
        loopLabels.pop();

        return res;
    }

    @Override
    public IrList visitBreakStmt(BreakStmtNode node) {
        IrList res = new IrList();
        // 从栈中获取当前循环的 break 目标标签
        if (!loopLabels.isEmpty()) {
            String breakLabel = loopLabels.peek()[0];
            res.add(IrInst.ggoto(breakLabel));
        }
        return res;
    }

    @Override
    public IrList visitContinueStmt(ContinueStmtNode node) {
        IrList res = new IrList();
        // 从栈中获取当前循环的 continue 目标标签
        if (!loopLabels.isEmpty()) {
            String continueLabel = loopLabels.peek()[1];
            res.add(IrInst.ggoto(continueLabel));
        }
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
        for (int i = 0; i < node.params.size(); i++) {
            ParamNode p = node.params.get(i);
            res.add(IrInst.param(irName(p.symbolRef, p.name)));
        }
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

            String leftResult = res.newTemp();
            res.add(IrInst.assign(leftResult, left.lastTemp()));

            String lblFalse = res.newLabel();
            String lblEnd = res.newLabel();

            res.add(IrInst.ifz(leftResult, lblFalse));

            IrList right = node.right.accept(this);
            res.addAll(right);

            String rightResult = res.newTemp();
            res.add(IrInst.assign(rightResult, right.lastTemp()));

            // 注意：dest 必须放在 rightResult 后面 newTemp，
            // 这样 res.lastTemp() 才会是 dest
            String dest = res.newTemp();

            res.add(IrInst.bin(dest, "AND", leftResult, rightResult));
            res.add(IrInst.ggoto(lblEnd));

            res.add(IrInst.label(lblFalse));
            res.add(IrInst.constant(dest, 0));

            res.add(IrInst.label(lblEnd));
            return res;
        }

        if (node.op == BinOp.OR) {
            // short-circuit OR
            IrList left = node.left.accept(this);
            res.addAll(left);

            String leftResult = res.newTemp();
            res.add(IrInst.assign(leftResult, left.lastTemp()));

            String lblTrue = res.newLabel();
            String lblEnd = res.newLabel();

            res.add(IrInst.ifnz(leftResult, lblTrue));

            IrList right = node.right.accept(this);
            res.addAll(right);

            String rightResult = res.newTemp();
            res.add(IrInst.assign(rightResult, right.lastTemp()));

            // 注意：dest 必须放在 rightResult 后面 newTemp，
            // 这样 res.lastTemp() 才会是 dest
            String dest = res.newTemp();

            res.add(IrInst.bin(dest, "OR", leftResult, rightResult));
            res.add(IrInst.ggoto(lblEnd));

            res.add(IrInst.label(lblTrue));
            res.add(IrInst.constant(dest, 1));

            res.add(IrInst.label(lblEnd));
            return res;
        }

        IrList left = node.left.accept(this);
        res.addAll(left);

        String leftResult = res.newTemp();
        res.add(IrInst.assign(leftResult, left.lastTemp()));

        IrList right = node.right.accept(this);
        res.addAll(right);

        String rightResult = res.newTemp();
        res.add(IrInst.assign(rightResult, right.lastTemp()));

        String dest = res.newTemp();
        res.add(IrInst.bin(dest, node.op.name(), leftResult, rightResult));

        return res;
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
        res.add(IrInst.assign(dst, irName(node.symbolRef, node.name)));
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
        for (int i = 0; i < node.args.size(); i++) {
            res.addAll(node.args.get(i).accept(this));
            res.add(IrInst.arg("t" + (IrList.getTmpId() - 1)));
        }
        String dst = res.newTemp();
        res.add(IrInst.call(dst, node.funcName));
        return res;
    }

}
