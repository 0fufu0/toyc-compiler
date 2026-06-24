package com.compiler.semantic;

import com.compiler.ast.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SemanticAnalyzerTest {

    private void analyze(CompUnitNode ast) {
        new SemanticAnalyzer().analyze(ast);
    }

    private CompUnitNode program(AstNode... items) {
        return new CompUnitNode(List.of(items));
    }

    private FuncDefNode intMainWith(AstNode... stmts) {
        return new FuncDefNode(
                ValueType.INT,
                "main",
                List.of(),
                new BlockStmtNode(List.of(stmts))
        );
    }

    @Test
    public void validProgramShouldPass() {
        CompUnitNode ast = program(
                intMainWith(
                        new VarDeclNode("a", new IntLiteralNode(1)),
                        new ReturnStmtNode(new IdNode("a"))
                )
        );

        assertDoesNotThrow(() -> analyze(ast));
    }

    @Test
    public void duplicateVariableShouldFail() {
        CompUnitNode ast = program(
                intMainWith(
                        new VarDeclNode("a", new IntLiteralNode(1)),
                        new VarDeclNode("a", new IntLiteralNode(2)),
                        new ReturnStmtNode(new IdNode("a"))
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void undefinedVariableShouldFail() {
        CompUnitNode ast = program(
                intMainWith(
                        new ReturnStmtNode(new IdNode("a"))
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void assignConstShouldFail() {
        CompUnitNode ast = program(
                intMainWith(
                        new ConstDeclNode("a", new IntLiteralNode(1)),
                        new AssignStmtNode("a", new IntLiteralNode(2)),
                        new ReturnStmtNode(new IdNode("a"))
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void breakOutsideLoopShouldFail() {
        CompUnitNode ast = program(
                intMainWith(
                        new BreakStmtNode(),
                        new ReturnStmtNode(new IntLiteralNode(0))
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void continueOutsideLoopShouldFail() {
        CompUnitNode ast = program(
                intMainWith(
                        new ContinueStmtNode(),
                        new ReturnStmtNode(new IntLiteralNode(0))
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void missingMainShouldFail() {
        FuncDefNode foo = new FuncDefNode(
                ValueType.INT,
                "foo",
                List.of(),
                new BlockStmtNode(List.of(
                        new ReturnStmtNode(new IntLiteralNode(1))
                ))
        );

        CompUnitNode ast = program(foo);

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void voidMainShouldFail() {
        FuncDefNode main = new FuncDefNode(
                ValueType.VOID,
                "main",
                List.of(),
                new BlockStmtNode(List.of())
        );

        CompUnitNode ast = program(main);

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void mainWithParameterShouldFail() {
        FuncDefNode main = new FuncDefNode(
                ValueType.INT,
                "main",
                List.of(new ParamNode("a")),
                new BlockStmtNode(List.of(
                        new ReturnStmtNode(new IdNode("a"))
                ))
        );

        CompUnitNode ast = program(main);

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void intFunctionWithoutReturnShouldFail() {
        FuncDefNode foo = new FuncDefNode(
                ValueType.INT,
                "foo",
                List.of(),
                new BlockStmtNode(List.of(
                        new VarDeclNode("a", new IntLiteralNode(1))
                ))
        );

        CompUnitNode ast = program(
                foo,
                intMainWith(
                        new ReturnStmtNode(new IntLiteralNode(0))
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void functionArgumentCountMismatchShouldFail() {
        FuncDefNode add = new FuncDefNode(
                ValueType.INT,
                "add",
                List.of(new ParamNode("a"), new ParamNode("b")),
                new BlockStmtNode(List.of(
                        new ReturnStmtNode(
                                new BinaryExprNode(
                                        BinOp.ADD,
                                        new IdNode("a"),
                                        new IdNode("b")
                                )
                        )
                ))
        );

        CompUnitNode ast = program(
                add,
                intMainWith(
                        new ReturnStmtNode(
                                new CallExprNode(
                                        "add",
                                        List.of(new IntLiteralNode(1))
                                )
                        )
                )
        );

        assertThrows(SemanticError.class, () -> analyze(ast));
    }

    @Test
    public void constExpressionShouldPass() {
        CompUnitNode ast = program(
                intMainWith(
                        new ConstDeclNode(
                                "a",
                                new BinaryExprNode(
                                        BinOp.ADD,
                                        new IntLiteralNode(1),
                                        new IntLiteralNode(2)
                                )
                        ),
                        new ReturnStmtNode(new IdNode("a"))
                )
        );

        assertDoesNotThrow(() -> analyze(ast));
    }

    @Test
    public void idNodeShouldBindSymbolRef() {
        IdNode idA = new IdNode("a");

        CompUnitNode ast = program(
                intMainWith(
                        new VarDeclNode("a", new IntLiteralNode(1)),
                        new ReturnStmtNode(idA)
                )
        );

        analyze(ast);

        assertDoesNotThrow(() -> {
            if (idA.symbolRef == null) {
                throw new RuntimeException("symbolRef should not be null");
            }

            if (!"a".equals(idA.symbolRef.name)) {
                throw new RuntimeException("symbolRef name should be a");
            }
        });
    }

    @Test
    public void constExpressionShouldFillConstValue() {
        BinaryExprNode constExpr = new BinaryExprNode(
                BinOp.ADD,
                new IntLiteralNode(1),
                new IntLiteralNode(2)
        );

        IdNode idA = new IdNode("a");

        CompUnitNode ast = program(
                intMainWith(
                        new ConstDeclNode("a", constExpr),
                        new ReturnStmtNode(idA)
                )
        );

        analyze(ast);

        assertEquals(3, constExpr.constValue);
        assertEquals(3, idA.constValue);
        assertNotNull(idA.symbolRef);
        assertEquals(3, idA.symbolRef.constValue);
    }
}
