package com.compiler.driver;

import com.compiler.ast.CompUnitNode;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.ir.Optimizer;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {

        boolean optimize = false;
        String sourcePath = null;
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-opt".equals(args[i])) {
                optimize = true;
            } else if (sourcePath == null) {
                sourcePath = args[i];
            } else {
                outputPath = args[i];
            }
        }

        String source;
        if (sourcePath != null) {
            source = Files.readString(Path.of(sourcePath));
        } else {
            source = new String(System.in.readAllBytes());
        }

        CompUnitNode ast = ToyCFrontend.parse(source);
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        ast.accept(semanticAnalyzer);

        IrGenerator irGenerator = new IrGenerator();
        IrList ir = irGenerator.generate(ast);

        if (optimize) {
            Optimizer optimizer = new Optimizer();
            ir = optimizer.optimize(ir);
        }

        CodeGenerator codeGenerator = new CodeGenerator();
        String ans = codeGenerator.generate(ir);

        if (outputPath != null) {
            Files.writeString(Path.of(outputPath), ans);
        } else {
            System.out.print(ans);
        }
    }
}
