package com.compiler.driver;

import com.compiler.ast.CompUnitNode;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
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
}
