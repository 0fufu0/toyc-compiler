package com.compiler.ir;

import com.compiler.ast.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

public class IrGeneratorTest {

    @Test
    public void testSimpleReturn() {
        // int main() { return 0; }
        BlockStmtNode body = new BlockStmtNode(List.of(new ReturnStmtNode(new IntLiteralNode(0))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode cu = new CompUnitNode(List.of(main));

        IrGenerator g = new IrGenerator();
        IrList ir = g.generate(cu);
        String out = ir.toString();
        assertTrue(out.contains("RET 0") || out.contains("RET null") == false);
    }

    @Test
    public void testShortCircuitAnd() {
        // int main() { int a = 1; int b = 2; return a && b; }
        VarDeclNode da = new VarDeclNode("a", new IntLiteralNode(1));
        VarDeclNode db = new VarDeclNode("b", new IntLiteralNode(2));
        BinaryExprNode andExpr = new BinaryExprNode(BinOp.AND, new IdNode("a"), new IdNode("b"));
        BlockStmtNode body = new BlockStmtNode(List.of(da, db, new ReturnStmtNode(andExpr)));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode cu = new CompUnitNode(List.of(main));

        IrGenerator g = new IrGenerator();
        IrList ir = g.generate(cu);
        String out = ir.toString();
        assertTrue(out.contains("IFZ") || out.contains("AND") || out.contains("GOTO") );
    }

    @Test
    public void testWhileLoop() {
        // int main() { while(0) { break; } return 0; }
        WhileStmtNode w = new WhileStmtNode(new IntLiteralNode(0), new BlockStmtNode(List.of(new BreakStmtNode())));
        BlockStmtNode body = new BlockStmtNode(List.of(w, new ReturnStmtNode(new IntLiteralNode(0))));
        FuncDefNode main = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode cu = new CompUnitNode(List.of(main));

        IrGenerator g = new IrGenerator();
        IrList ir = g.generate(cu);
        String out = ir.toString();
        assertTrue(out.contains("LABEL") || out.contains("GOTO") || out.contains("IFZ"));
    }
}
