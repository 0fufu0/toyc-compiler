package com.compiler.ir;

import com.compiler.ast.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class IrGeneratorTest {

    private IrList generateIR(BlockStmtNode body) {
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode cu = new CompUnitNode(List.of(main));
        IrGenerator g = new IrGenerator();
        return g.generate(cu);
    }

    private List<IrInst> instructions(IrList ir) {
        return ir.asList();
    }

    private IrInst findInst(List<IrInst> insts, String op) {
        return insts.stream().filter(i -> i.op.equals(op)).findFirst().orElse(null);
    }

    private IrInst findInst(List<IrInst> insts, String op, String dst) {
        return insts.stream().filter(i -> i.op.equals(op) && dst.equals(i.dst)).findFirst().orElse(null);
    }

    @Test
    public void testSimpleConst() {
        // int main() { return 42; }
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(new IntLiteralNode(42))));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        // 应该只有一条 CONST 指令用于返回值
        IrInst ret = findInst(insts, "RET");
        assertNotNull(ret, "应该有 RET 指令");
        assertEquals("42", ret.a, "返回值应该是 42");
    }

    @Test
    public void testAddConst() {
        // int main() { return 1 + 2; }
        BinaryExprNode expr = new BinaryExprNode(BinOp.ADD, new IntLiteralNode(1), new IntLiteralNode(2));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        // 应该有 CONST t0=1, CONST t0=2, BIN_ADD tN = t0 + t0, RET tN
        IrInst binAdd = findInst(insts, "BIN_ADD");
        assertNotNull(binAdd, "应该有 BIN_ADD 指令");
        // 左右操作数应该不同（不同的临时变量）
        assertNotEquals(binAdd.a, binAdd.b, "两个操作数应该是不同的临时变量");
    }

    @Test
    public void testSubConst() {
        // int main() { return 5 - 3; }
        BinaryExprNode expr = new BinaryExprNode(BinOp.SUB, new IntLiteralNode(5), new IntLiteralNode(3));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        IrInst binSub = findInst(insts, "BIN_SUB");
        assertNotNull(binSub, "应该有 BIN_SUB 指令");
    }

    @Test
    public void testMulConst() {
        // int main() { return 3 * 4; }
        BinaryExprNode expr = new BinaryExprNode(BinOp.MUL, new IntLiteralNode(3), new IntLiteralNode(4));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        IrInst binMul = findInst(insts, "BIN_MUL");
        assertNotNull(binMul, "应该有 BIN_MUL 指令");
    }

    @Test
    public void testDivConst() {
        // int main() { return 8 / 2; }
        BinaryExprNode expr = new BinaryExprNode(BinOp.DIV, new IntLiteralNode(8), new IntLiteralNode(2));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        IrInst binDiv = findInst(insts, "BIN_DIV");
        assertNotNull(binDiv, "应该有 BIN_DIV 指令");
    }

    @Test
    public void testUnaryMinus() {
        // int main() { return -5; }
        UnaryExprNode expr = new UnaryExprNode(UnaryOp.MINUS, new IntLiteralNode(5));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        IrInst binNeg = findInst(insts, "BIN_NEG");
        assertNotNull(binNeg, "应该有 BIN_NEG 指令");
    }

    @Test
    public void testIdReference() {
        // int main() { int a = 10; return a; }
        VarDeclNode decl = new VarDeclNode("a", new IntLiteralNode(10));
        IdNode id = new IdNode("a");
        BlockStmtNode body = new BlockStmtNode(List.of(decl, new ReturnStmtNode(id)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        // 应该有 ASSIGN 指令将变量加载到临时变量
        IrInst assign = findInst(insts, "ASSIGN");
        assertNotNull(assign, "应该有 ASSIGN 指令");
        assertEquals("a", assign.a, "ASSIGN 的源应该是变量名 'a'");
    }

    @Test
    public void testNestedBinaryExpr() {
        // int main() { return 1 + 2 * 3; }
        BinaryExprNode inner = new BinaryExprNode(BinOp.MUL, new IntLiteralNode(2), new IntLiteralNode(3));
        BinaryExprNode outer = new BinaryExprNode(BinOp.ADD, new IntLiteralNode(1), inner);
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(outer)));
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);

        // 应该有 ADD 和 MUL 指令
        assertNotNull(findInst(insts, "BIN_ADD"), "应该有 ADD");
        assertNotNull(findInst(insts, "BIN_MUL"), "应该有 MUL");
    }

    @Test
    public void testBreakContinue() {
        // while (i < 10) { if (i == 5) { break; } if (i == 3) { continue; } }
        VarDeclNode iDecl = new VarDeclNode("i", new IntLiteralNode(0));
        BinaryExprNode cond = new BinaryExprNode(BinOp.LT, new IdNode("i"), new IntLiteralNode(10));
        
        // if (i == 5) { break; }
        BinaryExprNode cond1 = new BinaryExprNode(BinOp.EQ, new IdNode("i"), new IntLiteralNode(5));
        IfStmtNode if1 = new IfStmtNode(cond1, new BreakStmtNode(), null);
        
        // if (i == 3) { continue; }
        BinaryExprNode cond2 = new BinaryExprNode(BinOp.EQ, new IdNode("i"), new IntLiteralNode(3));
        IfStmtNode if2 = new IfStmtNode(cond2, new ContinueStmtNode(), null);
        
        WhileStmtNode whileStmt = new WhileStmtNode(cond, new BlockStmtNode(List.of(if1, if2)));
        BlockStmtNode body = new BlockStmtNode(List.of(iDecl, whileStmt, new ReturnStmtNode(new IntLiteralNode(0))));
        
        IrList ir = generateIR(body);
        List<IrInst> insts = instructions(ir);
        
        // 验证 break 和 continue 的跳转目标
        IrInst gotoBreak = insts.stream().filter(i -> i.op.equals("GOTO")).findFirst().orElse(null);
        assertNotNull(gotoBreak, "应该有 GOTO (break)");
    }
}
