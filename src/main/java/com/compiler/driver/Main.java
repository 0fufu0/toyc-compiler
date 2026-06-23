package com.compiler.driver;

import com.compiler.backend.CodeGenerator;
import com.compiler.ir.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {


        CodeGenerator cg = new CodeGenerator();
        String asm = cg.generate(irList);

        // =========================
        // 3. 输出 RISC-V
        // =========================
        System.out.println(asm);
    }
}