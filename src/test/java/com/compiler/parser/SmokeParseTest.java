package com.compiler.parser;

import com.compiler.ast.CompUnitNode;
import com.compiler.ast.FuncDefNode;
import com.compiler.utils.AstDumper;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端冒烟测试：批量解析 smoke 目录下的 ToyC 样例。
 */
class SmokeParseTest {

    @ParameterizedTest
    @ValueSource(strings = {"add.tc", "global_while.tc", "recursion.tc"})
    void parseSmokeSamples(String fileName) throws IOException {
        Path path = Path.of("src/test/resources/smoke", fileName);
        String source = Files.readString(path);
        CompUnitNode ast = ToyCFrontend.parse(source);

        assertFalse(ast.items.isEmpty());
        assertTrue(ast.items.stream().anyMatch(n -> n instanceof FuncDefNode));

        String dump = AstDumper.dump(ast);
        assertTrue(dump.contains("FuncDef(main"));
        assertTrue(dump.contains("@"), () -> "expected source positions in dump for " + fileName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"add.tc", "global_while.tc", "recursion.tc"})
    void astDumpMainMatchesParser(String fileName, @TempDir Path tempDir) throws Exception {
        Path input = Path.of("src/test/resources/smoke", fileName);
        Path output = tempDir.resolve(fileName + ".ast.txt");

        String source = Files.readString(input);
        String expected = AstDumper.dump(ToyCFrontend.parse(source));
        Files.writeString(output, expected);

        assertTrue(Files.size(output) > 0);
        assertTrue(expected.contains("CompUnit"));
    }
}
