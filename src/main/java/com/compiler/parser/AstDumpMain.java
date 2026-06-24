package com.compiler.parser;

import com.compiler.utils.AstDumper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 前端冒烟工具：读取 ToyC 源码并输出 AST 树形转储。
 * <p>
 * 供成员 D 在完整流水线未就绪时独立验证解析器。
 *
 * <pre>
 *   java ... AstDumpMain input.tc
 *   type input.tc | java ... AstDumpMain
 * </pre>
 */
public final class AstDumpMain {

    private AstDumpMain() {
    }

    public static void main(String[] args) throws Exception {
        String source;
        if (args.length > 0) {
            source = Files.readString(Path.of(args[0]));
        } else {
            source = new String(System.in.readAllBytes());
        }
        System.out.println(AstDumper.dump(ToyCFrontend.parse(source)));
    }
}
