package com.compiler.ir;

import com.compiler.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests: 验证 IR 生成的语义正确性
 */
public class IrIntegrationTests {

    private IrList gen(AstNode node) {
        IrGenerator g = new IrGenerator();
        if (node instanceof CompUnitNode) return g.generate((CompUnitNode) node);
        return g.generate(new CompUnitNode(List.of(node)));
    }

    private List<IrInst> insts(IrList ir) {
        return ir.asList();
    }

    private long countInst(IrList ir, String op) {
        return insts(ir).stream().filter(i -> i.op.equals(op)).count();
    }

    // ==================== 短路求值测试 ====================

    @Test
    public void testShortCircuitAnd_HasConditionalJump() {
        // int main() { int a=1; int b=2; return a && b; }
        VarDeclNode a = new VarDeclNode("a", new IntLiteralNode(1));
        VarDeclNode b = new VarDeclNode("b", new IntLiteralNode(2));
        BinaryExprNode and = new BinaryExprNode(BinOp.AND, new IdNode("a"), new IdNode("b"));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(),
            new BlockStmtNode(List.of(a, b, new ReturnStmtNode(and))));
        IrList ir = gen(main);

        // AND 应该生成 IFZ 条件跳转
        assertTrue(countInst(ir, "IFZ") >= 1, "短路 AND 应生成 IFZ 指令");
        // AND 应该生成 GOTO 跳转
        assertTrue(countInst(ir, "GOTO") >= 1, "短路 AND 应生成 GOTO 指令");
        // AND 应该生成标签
        assertTrue(countInst(ir, "LABEL") >= 2, "短路 AND 应生成 2 个标签");
    }

    @Test
    public void testShortCircuitOr_HasConditionalJump() {
        // int main() { int a=0; int b=1; return a || b; }
        VarDeclNode a = new VarDeclNode("a", new IntLiteralNode(0));
        VarDeclNode b = new VarDeclNode("b", new IntLiteralNode(1));
        BinaryExprNode or = new BinaryExprNode(BinOp.OR, new IdNode("a"), new IdNode("b"));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(),
            new BlockStmtNode(List.of(a, b, new ReturnStmtNode(or))));
        IrList ir = gen(main);

        // OR 应该生成 IFNZ 条件跳转
        assertTrue(countInst(ir, "IFNZ") >= 1, "短路 OR 应生成 IFNZ 指令");
        // OR 应该生成 GOTO 跳转
        assertTrue(countInst(ir, "GOTO") >= 1, "短路 OR 应生成 GOTO 指令");
        // OR 应该生成标签
        assertTrue(countInst(ir, "LABEL") >= 2, "短路 OR 应生成 2 个标签");
    }

    @Test
    public void testShortCircuitAnd_LeftFalse_ShortCircuits() {
        // int main() { int a=0; int b=1; return a && b; }
        // 期望：IFZ a, L_false (跳过 b 的计算)
        VarDeclNode a = new VarDeclNode("a", new IntLiteralNode(0));
        VarDeclNode b = new VarDeclNode("b", new IntLiteralNode(1));
        BinaryExprNode and = new BinaryExprNode(BinOp.AND, new IdNode("a"), new IdNode("b"));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(),
            new BlockStmtNode(List.of(a, b, new ReturnStmtNode(and))));
        IrList ir = gen(main);
        List<IrInst> list = insts(ir);

        // 找到 IFZ 指令
        IrInst ifz = list.stream().filter(i -> i.op.equals("IFZ")).findFirst().orElse(null);
        assertNotNull(ifz, "应该有 IFZ 指令");
        assertNotNull(ifz.dst, "IFZ 应该有目标标签");
    }

    @Test
    public void testShortCircuitOr_LeftTrue_ShortCircuits() {
        // int main() { int a=1; int b=0; return a || b; }
        // 期望：IFNZ a, L_true (跳过 b 的计算)
        VarDeclNode a = new VarDeclNode("a", new IntLiteralNode(1));
        VarDeclNode b = new VarDeclNode("b", new IntLiteralNode(0));
        BinaryExprNode or = new BinaryExprNode(BinOp.OR, new IdNode("a"), new IdNode("b"));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(),
            new BlockStmtNode(List.of(a, b, new ReturnStmtNode(or))));
        IrList ir = gen(main);
        List<IrInst> list = insts(ir);

        // 找到 IFNZ 指令
        IrInst ifnz = list.stream().filter(i -> i.op.equals("IFNZ")).findFirst().orElse(null);
        assertNotNull(ifnz, "应该有 IFNZ 指令");
        assertNotNull(ifnz.dst, "IFNZ 应该有目标标签");
    }

    // ==================== 常量折叠测试 ====================

    @Test
    public void testConstantFolding_IntLiteral() {
        // int main() { return 42; }
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(new IntLiteralNode(42))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        List<IrInst> list = insts(ir);

        // 返回常量应该直接返回 42，而不是计算
        IrInst ret = list.stream().filter(i -> i.op.equals("RET")).findFirst().orElse(null);
        assertNotNull(ret, "应该有 RET 指令");
        assertEquals("42", ret.a, "RET 应该返回常量 42");
    }

    @Test
    public void testConstantFolding_Add() {
        // int main() { return 1 + 2; }
        // IR 应该包含 BIN_ADD 指令，因为是运行时计算
        BinaryExprNode expr = new BinaryExprNode(BinOp.ADD, new IntLiteralNode(1), new IntLiteralNode(2));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);

        // 当前实现不会折叠常量，所以会生成 BIN_ADD
        assertTrue(countInst(ir, "BIN_ADD") >= 1, "应该生成 ADD 指令");
    }

    @Test
    public void testNoTempCollision() {
        // int main() { return 1 + 2; }
        // 验证左右操作数使用不同的临时变量
        BinaryExprNode expr = new BinaryExprNode(BinOp.ADD, new IntLiteralNode(1), new IntLiteralNode(2));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        List<IrInst> list = insts(ir);

        // 找到 BIN_ADD
        IrInst binAdd = list.stream().filter(i -> i.op.equals("BIN_ADD")).findFirst().orElse(null);
        assertNotNull(binAdd, "应该有 BIN_ADD 指令");
        assertNotNull(binAdd.a, "BIN_ADD 应该有左操作数");
        assertNotNull(binAdd.b, "BIN_ADD 应该有右操作数");
        assertNotEquals(binAdd.a, binAdd.b, "左右操作数应该是不同的临时变量（无冲突）");
    }

    // ==================== 复杂表达式测试 ====================

    @Test
    public void testNestedAnd() {
        // int main() { return 1 && 0 && 1; }
        BinaryExprNode e1 = new BinaryExprNode(BinOp.AND, new IntLiteralNode(1), new IntLiteralNode(0));
        BinaryExprNode e2 = new BinaryExprNode(BinOp.AND, e1, new IntLiteralNode(1));
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(e2)));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);

        // 嵌套 AND 应该生成多个 IFZ
        assertTrue(countInst(ir, "IFZ") >= 2, "嵌套 AND 应生成至少 2 个 IFZ");
    }

    @Test
    public void testMixedAndOr() {
        // int main() { return 1 || 0 && 1; }
        // OR 优先级更高，所以是 (1 || 0) && 1
        BinaryExprNode innerAnd = new BinaryExprNode(BinOp.AND, new IntLiteralNode(0), new IntLiteralNode(1));
        BinaryExprNode expr = new BinaryExprNode(BinOp.OR, new IntLiteralNode(1), innerAnd);
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(expr)));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);

        // 混合表达式应该包含 OR 和 AND
        assertTrue(countInst(ir, "IFNZ") >= 1, "应该有 IFNZ");
        assertTrue(countInst(ir, "IFZ") >= 1, "应该有 IFZ");
    }

    // ==================== 控制流测试 ====================

    @Test
    public void testIfElse_HasBothBranches() {
        // int main() { if(1) return 2; else return 3; }
        IfStmtNode ifn = new IfStmtNode(new IntLiteralNode(1),
            new ReturnStmtNode(new IntLiteralNode(2)),
            new ReturnStmtNode(new IntLiteralNode(3)));
        BlockStmtNode body = new BlockStmtNode(List.of(ifn));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);

        // IF 语句应该生成条件跳转
        assertTrue(countInst(ir, "IFZ") >= 1, "应该有 IFZ");
        assertTrue(countInst(ir, "GOTO") >= 1, "应该有 GOTO");
    }

    @Test
    public void testWhile_HasLoopStructure() {
        // int main() { while(1) { break; } return 0; }
        WhileStmtNode w = new WhileStmtNode(new IntLiteralNode(1),
            new BlockStmtNode(List.of(new BreakStmtNode())));
        BlockStmtNode body = new BlockStmtNode(List.of(w, new ReturnStmtNode(new IntLiteralNode(0))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);

        // while 应该生成标签和跳转
        assertTrue(countInst(ir, "LABEL") >= 2, "应该有循环标签");
        assertTrue(countInst(ir, "GOTO") >= 1, "应该有跳转");
    }

    @Test
    public void testBreak_InWhile() {
        // int main() { while(1) { break; } return 0; }
        WhileStmtNode w = new WhileStmtNode(new IntLiteralNode(1),
            new BlockStmtNode(List.of(new BreakStmtNode())));
        BlockStmtNode body = new BlockStmtNode(List.of(w, new ReturnStmtNode(new IntLiteralNode(0))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);

        // break 应该生成跳转到循环外的 GOTO
        assertTrue(countInst(ir, "GOTO") >= 1, "break 应生成 GOTO");
    }

    // ==================== 函数调用测试 ====================

    @Test
    public void testFunctionCall() {
        // int foo() { return 1; } int main() { return foo(); }
        FuncDefNode foo = new FuncDefNode(ValueType.INT, "foo", List.of(),
            new BlockStmtNode(List.of(new ReturnStmtNode(new IntLiteralNode(1)))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(),
            new BlockStmtNode(List.of(new ReturnStmtNode(new CallExprNode("foo", List.of())))));
        CompUnitNode cu = new CompUnitNode(List.of(foo, main));
        IrList ir = gen(cu);

        assertTrue(countInst(ir, "CALL") >= 1, "应该有 CALL 指令");
        assertTrue(countInst(ir, "FUNC") >= 2, "应该有 2 个 FUNC 指令");
    }

    @Test
    public void testRecursiveCall() {
        // int fact(int n) { if(n==0) return 1; return n*fact(n-1); }
        ParamNode p = new ParamNode("n");
        IfStmtNode ifn = new IfStmtNode(
            new BinaryExprNode(BinOp.EQ, new IdNode("n"), new IntLiteralNode(0)),
            new ReturnStmtNode(new IntLiteralNode(1)),
            null);
        BinaryExprNode rec = new BinaryExprNode(BinOp.MUL,
            new IdNode("n"),
            new CallExprNode("fact", List.of(new BinaryExprNode(BinOp.SUB, new IdNode("n"), new IntLiteralNode(1)))));
        BlockStmtNode fb = new BlockStmtNode(List.of(ifn, new ReturnStmtNode(rec)));
        FuncDefNode fact = new FuncDefNode(ValueType.INT, "fact", List.of(p), fb);
        IrList ir = gen(fact);

        assertTrue(countInst(ir, "CALL") >= 1, "递归调用应生成 CALL");
    }
}
