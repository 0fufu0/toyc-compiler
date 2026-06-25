package com.compiler.driver;

import com.compiler.ast.*;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 编译器驱动程序 - 验证A、B、C模块能否正确运行
 */
public class CompilerMain {

    public static void main(String[] args) throws IOException {

        String source;
        if (args.length > 0) {
            source = Files.readString(Path.of(args[0]));
        } else {
            source = new String(System.in.readAllBytes());
        }
        CompUnitNode ast = ToyCFrontend.parse(source);
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        ast.accept(semanticAnalyzer);
        IrGenerator irGenerator=new IrGenerator();
        IrList ir = irGenerator.generate(ast);
        CodeGenerator codeGenerator=new CodeGenerator();
        String ans = codeGenerator.generate(ir);
        if (args.length > 1) {
            Files.writeString(Path.of(args[1]), ans);
        } else {
            System.out.print(ans);
        }
    }
    /**
     * 测试1: int main() { return 42; }
     */

    private static void testMinimalProgram() {
        System.out.println("Test 1: Minimal Program (return 42)");

        // 构造AST
        BlockStmtNode body = new BlockStmtNode(List.of(
            new ReturnStmtNode(new IntLiteralNode(42))
        ));
        FuncDefNode mainFunc = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode ast = new CompUnitNode(List.of(mainFunc));

        // 运行A、B、C模块
        runModules(ast);
    }

    /**
     * 测试2: int main() { int x = 10; x = 20; return x; }
     */
    private static void testAssignment() {
        System.out.println("\nTest 2: Variable Assignment");

        // 构造AST
        BlockStmtNode body = new BlockStmtNode(List.of(
            new VarDeclNode("x", new IntLiteralNode(10)),
            new VarDeclNode("y",new IntLiteralNode(20)),
            new AssignStmtNode("x", new IntLiteralNode(20)),
            new ReturnStmtNode(new IdNode("x"))
        ));
        FuncDefNode mainFunc = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode ast = new CompUnitNode(List.of(mainFunc));

        // 运行A、B、C模块
        runModules(ast);
    }

    /**
     * 测试3: int main() { if (1) return 2; else return 3; }
     */
    private static void testIfElse() {
        System.out.println("\nTest 3: If-Else Statement");

        // 构造AST
        BlockStmtNode body = new BlockStmtNode(List.of(
            new IfStmtNode(
                new IntLiteralNode(1),
                new ReturnStmtNode(new IntLiteralNode(2)),
                new ReturnStmtNode(new IntLiteralNode(3))
            )
        ));
        FuncDefNode mainFunc = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode ast = new CompUnitNode(List.of(mainFunc));

        // 运行A、B、C模块
        runModules(ast);
    }

    /**
     * 测试4: int main() { int sum = 0; int i = 1; while (i <= 10) { sum = sum + i; i = i + 1; } return sum; }
     */
    private static void testWhileLoop() {
        System.out.println("\nTest 4: While Loop");

        // 构造AST
        BlockStmtNode loopBody = new BlockStmtNode(List.of(
            new AssignStmtNode("sum", new BinaryExprNode(BinOp.ADD, new IdNode("sum"), new IdNode("i"))),
            new AssignStmtNode("i", new BinaryExprNode(BinOp.ADD, new IdNode("i"), new IntLiteralNode(1)))
        ));

        BlockStmtNode body = new BlockStmtNode(List.of(
            new VarDeclNode("sum", new IntLiteralNode(0)),
            new VarDeclNode("i", new IntLiteralNode(1)),
            new WhileStmtNode(
                new BinaryExprNode(BinOp.LE, new IdNode("i"), new IntLiteralNode(3)),
                loopBody
            ),
            new ReturnStmtNode(new IdNode("sum"))
        ));

        FuncDefNode mainFunc = new FuncDefNode(ValueType.INT, "main", List.of(), body);
        CompUnitNode ast = new CompUnitNode(List.of(mainFunc));

        // 运行A、B、C模块
        runModules(ast);
    }

    /**
     * 测试5: int add(int a, int b) { return a + b; } int main() { return add(3, 4); }
     */
    private static void testFunctionCall() {
        System.out.println("\nTest 5: Function Call");

        // 构造AST
        ParamNode paramA = new ParamNode("a");
        ParamNode paramB = new ParamNode("b");

        BlockStmtNode addBody = new BlockStmtNode(List.of(
            new ReturnStmtNode(new BinaryExprNode(BinOp.ADD, new IdNode("a"), new IdNode("b")))
        ));
        FuncDefNode addFunc = new FuncDefNode(ValueType.INT, "add", List.of(paramA, paramB), addBody);

        BlockStmtNode mainBody = new BlockStmtNode(List.of(
                new VarDeclNode("x", new IntLiteralNode(10)),
                new VarDeclNode("y",new IntLiteralNode(20)),
            new ReturnStmtNode(new CallExprNode("add", List.of(new BinaryExprNode(BinOp.ADD,new IdNode("x"),new IdNode("y")), new IntLiteralNode(4))))
        ));
        FuncDefNode mainFunc = new FuncDefNode(ValueType.INT, "main", List.of(), mainBody);

        CompUnitNode ast = new CompUnitNode(List.of(addFunc, mainFunc));

        // 运行A、B、C模块
        runModules(ast);
    }

    /**
     * 运行A、B、C模块并输出结果
     */
    private static void runModules(CompUnitNode ast) {
        try {
            // A模块: AST已经构造完成（模拟Parser输出）

            // B模块: 语义分析
            System.out.println("  [B Module] Running Semantic Analysis...");
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            ast.accept(semanticAnalyzer);
            System.out.println("  [B Module] Semantic Analysis Passed ✓");

            // C模块: IR生成
            System.out.println("  [C Module] Generating IR...");
            IrGenerator irGenerator = new IrGenerator();
            IrList ir = irGenerator.generate(ast);
            System.out.println("  [C Module] IR Generated ✓");

            // D模块： risc-v代码生成
            System.out.println("  [D Module] Generating risc-v...");
            CodeGenerator codeGenerator = new CodeGenerator();
            String riscv=codeGenerator.generate(ir);
            System.out.println("  [D Module] risc-v Generated ✓");
            // 输出IR
            System.out.println("  Generated risc-v:");
            //System.out.println(ir.toString().lines().map(line -> "    " + line).collect(java.util.stream.Collectors.joining("\n")));

            System.out.println(riscv);
            System.out.println("  Test Passed ✓");

        } catch (Exception e) {
            System.out.println("  Test Failed ✗: " + e.getMessage());
            e.printStackTrace();
        }
    }
}