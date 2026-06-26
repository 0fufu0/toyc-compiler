package com.compiler.driver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.compiler.ast.CompUnitNode;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;

public class Main {

    public static void main(String[] args) throws IOException {
        boolean opt = false;
        java.util.List<String> files = new java.util.ArrayList<>();

        for (String arg : args) {
            if ("-opt".equals(arg)) {
                opt = true;
            } else {
                files.add(arg);
            }
        }

        String source;
        if (!files.isEmpty()) {
            source = Files.readString(Path.of(files.get(0)));
        } else {
            source = new String(System.in.readAllBytes());
        }

        CompUnitNode ast = ToyCFrontend.parse(source);

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        ast.accept(semanticAnalyzer);

        IrGenerator irGenerator = new IrGenerator();
        IrList ir = irGenerator.generate(ast);

        CodeGenerator codeGenerator = new CodeGenerator();
        String ans = codeGenerator.generate(ir);

        if (files.size() > 1) {
            Files.writeString(Path.of(files.get(1)), ans);
        } else {
            System.out.print(ans);
        }
    }
}
