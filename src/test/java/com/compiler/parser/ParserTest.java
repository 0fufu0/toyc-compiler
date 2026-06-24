package com.compiler.parser;

import com.compiler.ast.*;
import com.compiler.utils.AstDumper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void parseSimpleMainReturn() {
        String source = "int main() { return 1 + 2; }";
        CompUnitNode ast = ToyCFrontend.parse(source);

        assertEquals(1, ast.items.size());
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        assertEquals("main", main.name);
        assertEquals(ValueType.INT, main.returnType);
        assertTrue(main.params.isEmpty());

        ReturnStmtNode ret = (ReturnStmtNode) main.body.stmts.get(0);
        BinaryExprNode add = (BinaryExprNode) ret.value;
        assertEquals(BinOp.ADD, add.op);
        assertEquals(1, ((IntLiteralNode) add.left).value);
        assertEquals(2, ((IntLiteralNode) add.right).value);
    }

    @Test
    void parseGlobalDeclsInSourceOrder() {
        String source = """
                const int a = 1;
                int b = 2;
                int main() { return a + b; }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);

        assertEquals(3, ast.items.size());
        assertInstanceOf(ConstDeclNode.class, ast.items.get(0));
        assertInstanceOf(VarDeclNode.class, ast.items.get(1));
        assertInstanceOf(FuncDefNode.class, ast.items.get(2));

        ConstDeclNode a = (ConstDeclNode) ast.items.get(0);
        VarDeclNode b = (VarDeclNode) ast.items.get(1);
        assertEquals("a", a.name);
        assertEquals("b", b.name);
    }

    @Test
    void parseIfElseAndOperatorPrecedence() {
        String source = """
                int main() {
                    if (1 < 2 && 3 || 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        IfStmtNode ifStmt = (IfStmtNode) main.body.stmts.get(0);

        BinaryExprNode cond = (BinaryExprNode) ifStmt.condition;
        assertEquals(BinOp.OR, cond.op);
        assertEquals(BinOp.AND, ((BinaryExprNode) cond.left).op);
        assertEquals(BinOp.LT, ((BinaryExprNode) ((BinaryExprNode) cond.left).left).op);
        assertNotNull(ifStmt.elseBranch);
    }

    @Test
    void parseWhileWithBreakAndContinue() {
        String source = """
                int main() {
                    while (1) {
                        break;
                        continue;
                    }
                    return 0;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        WhileStmtNode loop = (WhileStmtNode) main.body.stmts.get(0);
        BlockStmtNode body = (BlockStmtNode) loop.body;

        assertInstanceOf(BreakStmtNode.class, body.stmts.get(0));
        assertInstanceOf(ContinueStmtNode.class, body.stmts.get(1));
    }

    @Test
    void parseFunctionCallAndComments() {
        String source = """
                // compute sum
                int add(int x, int y) {
                    return x + y;
                }
                int main() {
                    return add(1, 2 * 3);
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);

        assertEquals(2, ast.items.size());
        FuncDefNode add = (FuncDefNode) ast.items.get(0);
        assertEquals(2, add.params.size());
        assertEquals("x", add.params.get(0).name);
        assertEquals("y", add.params.get(1).name);

        FuncDefNode main = (FuncDefNode) ast.items.get(1);
        ReturnStmtNode ret = (ReturnStmtNode) main.body.stmts.get(0);
        CallExprNode call = (CallExprNode) ret.value;
        assertEquals("add", call.funcName);
        assertEquals(2, call.args.size());
        assertInstanceOf(IntLiteralNode.class, call.args.get(0));
        assertInstanceOf(BinaryExprNode.class, call.args.get(1));

        String dump = AstDumper.dump(ast);
        assertTrue(dump.contains("FuncDef(add"));
        assertTrue(dump.contains("Call(add"));
    }

    @Test
    void astNodesHaveSourcePositions() {
        String source = "int main() { return 1 + 2; }";
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);

        assertTrue(main.line > 0);
        assertTrue(main.column > 0);
        assertTrue(main.body.line > 0);
    }

    @Test
    void syntaxErrorReportsLineAndColumn() {
        String source = "int main() { return 1 + ; }";

        ParseException ex = assertThrows(ParseException.class, () -> ToyCFrontend.parse(source));
        assertTrue(ex.line > 0, "expected line > 0, got: " + ex.getMessage());
        assertTrue(ex.column > 0, "expected column > 0, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(":"));
    }
}
