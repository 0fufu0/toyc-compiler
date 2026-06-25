package com.compiler.driver;

import com.compiler.ast.CompUnitNode;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            if (!"-opt".equals(arg)) {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        String source = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        CompUnitNode ast = ToyCFrontend.parse(source);
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        ast.accept(semanticAnalyzer);
        IrGenerator irGenerator = new IrGenerator();
        IrList ir = irGenerator.generate(ast);
        CodeGenerator codeGenerator = new CodeGenerator();
        String ans = codeGenerator.generate(ir);
        System.out.print(ans);
    }
}
