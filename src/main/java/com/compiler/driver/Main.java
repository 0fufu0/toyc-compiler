package com.compiler.driver;

import com.compiler.ast.CompUnitNode;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            // 从标准输入读取 ToyC 源代码
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            StringBuilder sourceCode = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sourceCode.append(line).append("\n");
            }
            reader.close();

            String source = sourceCode.toString();

            // A模块: 词法/语法分析
            CompUnitNode ast = ToyCFrontend.parse(source);

            // B模块: 语义分析
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            ast.accept(semanticAnalyzer);

            // C模块: IR生成
            IrGenerator irGenerator = new IrGenerator();
            IrList ir = irGenerator.generate(ast);

            // D模块: RISC-V汇编生成
            CodeGenerator codeGenerator = new CodeGenerator();
            String riscv = codeGenerator.generate(ir);

            // 向标准输出输出汇编代码
            System.out.print(riscv);

        } catch (Exception e) {
            // 错误信息输出到标准错误流，不影响标准输出的汇编代码
            System.err.println("Compilation error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}