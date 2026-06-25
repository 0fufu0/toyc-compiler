package com.compiler.parser;

import com.compiler.ast.CompUnitNode;
import com.compiler.backend.CodeGenerator;
import com.compiler.ir.IrGenerator;
import com.compiler.ir.IrList;
import com.compiler.semantic.SemanticAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 前端联调测试：源码 → parse → 语义分析 → IR → RISC-V 汇编。
 */
class FrontendIntegrationTest {

    private void runPipeline(String source) {
        CompUnitNode ast = ToyCFrontend.parse(source);
        assertNotNull(ast);
        assertFalse(ast.items.isEmpty());

        SemanticAnalyzer semantic = new SemanticAnalyzer();
        semantic.analyze(ast);

        IrGenerator irGen = new IrGenerator();
        IrList ir = irGen.generate(ast);
        assertFalse(ir.asList().isEmpty());

        String asm = new CodeGenerator().generate(ir);
        assertFalse(asm.isBlank());
        assertTrue(asm.contains("main"), () -> "expected main in asm:\n" + asm);
    }

    @Test
    void pipelineMinimalReturn() {
        runPipeline("int main() { return 1 + 2; }");
    }

    @Test
    void pipelineGlobalAndWhile() {
        runPipeline("""
                const int N = 3;
                int acc = 0;
                int main() {
                    int i = 0;
                    while (i < N) {
                        acc = acc + i;
                        i = i + 1;
                    }
                    return acc;
                }
                """);
    }

    @Test
    void pipelineRecursion() {
        runPipeline("""
                int fib(int n) {
                    if (n <= 1) {
                        return n;
                    } else {
                        return fib(n - 1) + fib(n - 2);
                    }
                }
                int main() {
                    return fib(5);
                }
                """);
    }

    @ParameterizedTest
    @ValueSource(strings = {"add.tc", "global_while.tc", "recursion.tc"})
    void pipelineSmokeSamples(String fileName) throws IOException {
        Path path = Path.of("src/test/resources/smoke", fileName);
        String source = Files.readString(path);
        runPipeline(source);
    }
}
