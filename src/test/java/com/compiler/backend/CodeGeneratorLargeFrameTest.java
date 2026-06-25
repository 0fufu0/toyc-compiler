package com.compiler.backend;

import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.parser.ToyCFrontend;
import com.compiler.semantic.SemanticAnalyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeGeneratorLargeFrameTest {

    @Test
    void manyLocalVariablesUseLegalImmediates() {
        StringBuilder sb = new StringBuilder("int main() {\n");
        for (int i = 0; i < 600; i++) {
            sb.append("  int v").append(i).append(" = ").append(i).append(";\n");
        }
        sb.append("  return v599;\n}\n");

        var ast = ToyCFrontend.parse(sb.toString());
        new SemanticAnalyzer().analyze(ast);
        IrList ir = new IrGenerator().generate(ast);
        String asm = new CodeGenerator().generate(ir);

        assertFalse(asm.contains("addi sp, sp, -"), () -> "large stack should not use addi:\n" + asm);
        assertTrue(asm.contains("li t6, -"), () -> "expected li for large stack adjustment:\n" + asm);
        assertFalse(asm.matches("(?s).*l[ws] t\\d+, -\\d{4,}\\(sp\\).*"),
                () -> "found illegal large sp offset:\n" + asm);
    }
}
