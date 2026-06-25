package com.compiler.parser;

import com.compiler.ast.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *文法全覆盖测试：逐条验证 ToyC.g4 中尚未被 ParserTest 覆盖的规则。
 */
class ParserCoverageTest {

    @Test
    void parseAllRelationalOperators() {
        String source = """
                int main() {
                    int a = 1;
                    int b = 2;
                    int r = 0;
                    if (a < b) { r = r + 1; }
                    if (a > b) { r = r + 1; }
                    if (a <= b) { r = r + 1; }
                    if (a >= b) { r = r + 1; }
                    if (a == b) { r = r + 1; }
                    if (a != b) { r = r + 1; }
                    return r;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        assertEquals(10, main.body.stmts.size()); // 3 decl + 6 if + return
        for (int i = 3; i < 9; i++) {
            IfStmtNode ifStmt = (IfStmtNode) main.body.stmts.get(i);
            BinaryExprNode cond = (BinaryExprNode) ifStmt.condition;
            assertInstanceOf(BinaryExprNode.class, cond);
        }
    }

    @Test
    void parseMulDivMod() {
        String source = """
                int main() {
                    return 6 * 7 / 2 % 5;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        ReturnStmtNode ret = (ReturnStmtNode) main.body.stmts.get(0);
        BinaryExprNode mod = (BinaryExprNode) ret.value;
        assertEquals(BinOp.MOD, mod.op);
        BinaryExprNode div = (BinaryExprNode) mod.left;
        assertEquals(BinOp.DIV, div.op);
        BinaryExprNode mul = (BinaryExprNode) div.left;
        assertEquals(BinOp.MUL, mul.op);
    }

    @Test
    void parseUnaryOperators() {
        String source = """
                int main() {
                    int a = 1;
                    int b = 2;
                    return +a + -b + !0;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        ReturnStmtNode ret = (ReturnStmtNode) main.body.stmts.get(2);
        BinaryExprNode sum = (BinaryExprNode) ret.value;
        assertEquals(BinOp.ADD, sum.op);
        UnaryExprNode notZero = (UnaryExprNode) sum.right;
        assertEquals(UnaryOp.NOT, notZero.op);
        assertEquals(0, ((IntLiteralNode) notZero.operand).value);
        BinaryExprNode inner = (BinaryExprNode) sum.left;
        UnaryExprNode negB = (UnaryExprNode) inner.right;
        assertEquals(UnaryOp.MINUS, negB.op);
        UnaryExprNode posA = (UnaryExprNode) inner.left;
        assertEquals(UnaryOp.PLUS, posA.op);
    }

    @Test
    void parseNegativeNumberLiteral() {
        String source = "int main() { return -42; }";
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        ReturnStmtNode ret = (ReturnStmtNode) main.body.stmts.get(0);
        IntLiteralNode lit = (IntLiteralNode) ret.value;
        assertEquals(-42, lit.value);
    }

    @Test
    void parseBlockComment() {
        String source = """
                /* global counter */
                int g = 0;
                int main() {
                    /* increment */
                    g = g + 1;
                    return g;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        assertEquals(2, ast.items.size());
        assertInstanceOf(VarDeclNode.class, ast.items.get(0));
    }

    @Test
    void parseVoidFunctionAndBareReturn() {
        String source = """
                void log(int x) {
                    x = x + 1;
                    return;
                }
                int main() {
                    log(1);
                    return 0;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode log = (FuncDefNode) ast.items.get(0);
        assertEquals(ValueType.VOID, log.returnType);
        ReturnStmtNode ret = (ReturnStmtNode) log.body.stmts.get(1);
        assertNull(ret.value);

        FuncDefNode main = (FuncDefNode) ast.items.get(1);
        ExprStmtNode call = (ExprStmtNode) main.body.stmts.get(0);
        assertInstanceOf(CallExprNode.class, call.expr);
    }

    @Test
    void parseEmptyStatementAndIfWithoutElse() {
        String source = """
                int main() {
                    ;
                    if (1) {
                        return 1;
                    }
                    return 0;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        assertInstanceOf(EmptyStmtNode.class, main.body.stmts.get(0));
        IfStmtNode ifStmt = (IfStmtNode) main.body.stmts.get(1);
        assertNull(ifStmt.elseBranch);
    }

    @Test
    void parseNestedBlockWithLocalDecl() {
        String source = """
                int main() {
                    int x = 1;
                    {
                        int y = 2;
                        x = x + y;
                    }
                    return x;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        BlockStmtNode outer = main.body;
        BlockStmtNode inner = (BlockStmtNode) outer.stmts.get(1);
        assertInstanceOf(VarDeclNode.class, inner.stmts.get(0));
        assertInstanceOf(AssignStmtNode.class, inner.stmts.get(1));
    }

    @Test
    void parseParenthesizedExpression() {
        String source = "int main() { return (1 + 2) * 3; }";
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode main = (FuncDefNode) ast.items.get(0);
        ReturnStmtNode ret = (ReturnStmtNode) main.body.stmts.get(0);
        BinaryExprNode mul = (BinaryExprNode) ret.value;
        assertEquals(BinOp.MUL, mul.op);
        BinaryExprNode add = (BinaryExprNode) mul.left;
        assertEquals(BinOp.ADD, add.op);
    }

    @Test
    void parseFunctionWithoutParamList() {
        String source = """
                int f() { return 1; }
                int main() { return f(); }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        FuncDefNode f = (FuncDefNode) ast.items.get(0);
        assertTrue(f.params.isEmpty());
        FuncDefNode main = (FuncDefNode) ast.items.get(1);
        CallExprNode call = (CallExprNode) ((ReturnStmtNode) main.body.stmts.get(0)).value;
        assertTrue(call.args.isEmpty());
    }

    @Test
    void parseGrammarKitchenSink() {
        String source = """
                const int LIMIT = 3;
                int total = 0;

                int step(int x) {
                    return x * 2 + 1;
                }

                void bump() {
                    total = total + 1;
                }

                int main() {
                    int i = 0;
                    while (i < LIMIT && total >= 0 || i != 0) {
                        if (i == 0) {
                            bump();
                        } else {
                            total = total + step(i);
                        }
                        if (i >= LIMIT) {
                            break;
                        }
                        i = i + 1;
                        continue;
                    }
                    return total % 10 / 2 - !0;
                }
                """;
        CompUnitNode ast = ToyCFrontend.parse(source);
        assertEquals(5, ast.items.size());
        assertInstanceOf(ConstDeclNode.class, ast.items.get(0));
        assertInstanceOf(VarDeclNode.class, ast.items.get(1));
        assertInstanceOf(FuncDefNode.class, ast.items.get(2));
        assertInstanceOf(FuncDefNode.class, ast.items.get(3));
        assertInstanceOf(FuncDefNode.class, ast.items.get(4));
        assertTrue(ast.items.stream().anyMatch(n -> n instanceof FuncDefNode f && "main".equals(f.name)));
    }
}
