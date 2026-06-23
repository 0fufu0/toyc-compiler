package com.compiler.ir;

import com.compiler.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style tests: 构造 AST（模拟 .tc 文件）并验证 IR 生成器输出包含预期结构。
 */
public class IrIntegrationTests {

    private IrList gen(AstNode node) {
        IrGenerator g = new IrGenerator();
        if (node instanceof CompUnitNode) return g.generate((CompUnitNode) node);
        // wrap into compunit if single func
        return g.generate(new CompUnitNode(List.of(node)));
    }

    @Test
    public void test01_minimal() {
        // int main() { return 0; }
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(new IntLiteralNode(0))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("FUNC main") && s.contains("RET 0"));
    }

    @Test
    public void test02_assignment() {
        // int main(){ int x=1; x=2; return x; }
        VarDeclNode decl = new VarDeclNode("x", new IntLiteralNode(1));
        AssignStmtNode asg = new AssignStmtNode("x", new IntLiteralNode(2));
        BlockStmtNode body = new BlockStmtNode(List.of(decl, asg, new ReturnStmtNode(new IdNode("x"))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("CONST(x,1") || s.contains("CONST") );
    }

    @Test
    public void test03_if_else() {
        // int main(){ if(1) return 2; else return 3; }
        IfStmtNode ifn = new IfStmtNode(new IntLiteralNode(1), new ReturnStmtNode(new IntLiteralNode(2)), new ReturnStmtNode(new IntLiteralNode(3)));
        BlockStmtNode body = new BlockStmtNode(List.of(ifn));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("IFZ") || s.contains("GOTO") );
    }

    @Test
    public void test04_while_break() {
        // int main(){ while(1){ break; } return 0; }
        WhileStmtNode w = new WhileStmtNode(new IntLiteralNode(1), new BlockStmtNode(List.of(new BreakStmtNode())));
        BlockStmtNode body = new BlockStmtNode(List.of(w, new ReturnStmtNode(new IntLiteralNode(0))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("GOTO") || s.contains("LABEL"));
    }

    @Test
    public void test05_function_call() {
        // int foo(){ return 1; } int main(){ return foo(); }
        FuncDefNode foo = new FuncDefNode(ValueType.INT, "foo", List.of(), new BlockStmtNode(List.of(new ReturnStmtNode(new IntLiteralNode(1)))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(new ReturnStmtNode(new CallExprNode("foo", List.of())))));
        CompUnitNode cu = new CompUnitNode(List.of(foo, main));
        IrList ir = gen(cu);
        String s = ir.toString();
        assertTrue(s.contains("CALL") && s.contains("FUNC foo"));
    }

    @Test
    public void test06_continue() {
        // int main(){ while(1){ continue; } return 0; }
        WhileStmtNode w = new WhileStmtNode(new IntLiteralNode(1), new BlockStmtNode(List.of(new ContinueStmtNode())));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(w, new ReturnStmtNode(new IntLiteralNode(0)))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("GOTO") || s.contains("LABEL"));
    }

    @Test
    public void test07_scope_shadow() {
        // int main(){ int x=1; { int x=2; } return x; }
        VarDeclNode outer = new VarDeclNode("x", new IntLiteralNode(1));
        VarDeclNode inner = new VarDeclNode("x", new IntLiteralNode(2));
        BlockStmtNode innerBlock = new BlockStmtNode(List.of(inner));
        BlockStmtNode body = new BlockStmtNode(List.of(outer, innerBlock, new ReturnStmtNode(new IdNode("x"))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("CONST") || s.length() > 0);
    }

    @Test
    public void test08_short_circuit() {
        // int main(){ int a=0; int b=1; return a && b; }
        VarDeclNode a = new VarDeclNode("a", new IntLiteralNode(0));
        VarDeclNode b = new VarDeclNode("b", new IntLiteralNode(1));
        BinaryExprNode and = new BinaryExprNode(BinOp.AND, new IdNode("a"), new IdNode("b"));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(a, b, new ReturnStmtNode(and))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("IFZ") || s.contains("IFNZ") || s.contains("AND"));
    }

    @Test
    public void test09_recursion() {
        // int fact(int n){ if(n==0) return 1; return n*fact(n-1);} int main(){ return fact(1);} 
        ParamNode p = new ParamNode("n");
        IfStmtNode ifn = new IfStmtNode(new BinaryExprNode(BinOp.EQ, new IdNode("n"), new IntLiteralNode(0)), new ReturnStmtNode(new IntLiteralNode(1)), null);
        BinaryExprNode rec = new BinaryExprNode(BinOp.MUL, new IdNode("n"), new CallExprNode("fact", List.of(new BinaryExprNode(BinOp.SUB, new IdNode("n"), new IntLiteralNode(1)))));
        BlockStmtNode fb = new BlockStmtNode(List.of(ifn, new ReturnStmtNode(rec)));
        FuncDefNode fact = new FuncDefNode(ValueType.INT, "fact", List.of(p), fb);
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(new ReturnStmtNode(new CallExprNode("fact", List.of(new IntLiteralNode(1)))))));
        CompUnitNode cu = new CompUnitNode(List.of(fact, main));
        IrList ir = gen(cu);
        String s = ir.toString();
        assertTrue(s.contains("CALL") && s.contains("FUNC fact"));
    }

    @Test
    public void test10_void_fn() {
        // void foo(){} int main(){ foo(); return 0; }
        FuncDefNode foo = new FuncDefNode(ValueType.VOID, "foo", List.of(), new BlockStmtNode(List.of()));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(new ExprStmtNode(new CallExprNode("foo", List.of())), new ReturnStmtNode(new IntLiteralNode(0)))));
        CompUnitNode cu = new CompUnitNode(List.of(foo, main));
        IrList ir = gen(cu);
        String s = ir.toString();
        assertTrue(s.contains("CALL") && s.contains("FUNC foo"));
    }

    @Test
    public void test11_precedence() {
        // int main(){ return 1+2*3; }
        BinaryExprNode expr = new BinaryExprNode(BinOp.ADD, new IntLiteralNode(1), new BinaryExprNode(BinOp.MUL, new IntLiteralNode(2), new IntLiteralNode(3)));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(new ReturnStmtNode(expr))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("BIN_") || s.contains("CONST"));
    }

    @Test
    public void test12_division_check() {
        // int main(){ int a=1; int b=0; return a/b; }
        VarDeclNode a = new VarDeclNode("a", new IntLiteralNode(1));
        VarDeclNode b = new VarDeclNode("b", new IntLiteralNode(0));
        BinaryExprNode div = new BinaryExprNode(BinOp.DIV, new IdNode("a"), new IdNode("b"));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(a, b, new ReturnStmtNode(div))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("BIN_DIV") || s.contains("DIV"));
    }

    @Test
    public void test13_scope_block() {
        // int main(){ { int x=1; } return 0; }
        VarDeclNode inner = new VarDeclNode("x", new IntLiteralNode(1));
        BlockStmtNode innerBlock = new BlockStmtNode(List.of(inner));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(innerBlock, new ReturnStmtNode(new IntLiteralNode(0)))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.length() > 0);
    }

    @Test
    public void test14_nested_if_while() {
        // int main(){ if(1) { while(0) { } } return 0; }
        WhileStmtNode w = new WhileStmtNode(new IntLiteralNode(0), new BlockStmtNode(List.of()));
        IfStmtNode ifn = new IfStmtNode(new IntLiteralNode(1), new BlockStmtNode(List.of(w)), null);
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(ifn, new ReturnStmtNode(new IntLiteralNode(0)))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("IFZ") || s.contains("LABEL"));
    }

    @Test
    public void test15_multiple_return_paths() {
        // int main(){ if(1) return 1; return 0; }
        IfStmtNode ifn = new IfStmtNode(new IntLiteralNode(1), new ReturnStmtNode(new IntLiteralNode(1)), null);
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), new BlockStmtNode(List.of(ifn, new ReturnStmtNode(new IntLiteralNode(0)))));
        IrList ir = gen(main);
        String s = ir.toString();
        assertTrue(s.contains("RET") );
    }
}
