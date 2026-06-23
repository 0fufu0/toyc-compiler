package com.compiler.driver;

import com.compiler.backend.CodeGenerator;
import com.compiler.ir.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {


        IrList ir = new IrList();


        ir.add(IrInst.func("mul"));

        ir.add(IrInst.constant("x", 3));
        ir.add(IrInst.constant("y", 4));
        ir.add(IrInst.bin("t", "MUL", "x", "y"));
        ir.add(IrInst.ret("t"));

        ir.add(IrInst.endFunc());


        ir.add(IrInst.func("add"));

        ir.add(IrInst.call("m", "mul"));   // ❗嵌套 call
        ir.add(IrInst.constant("a", 10));
        ir.add(IrInst.bin("t", "ADD", "m", "a"));
        ir.add(IrInst.ret("t"));

        ir.add(IrInst.endFunc());


        ir.add(IrInst.func("main"));

        ir.add(IrInst.call("c", "add"));
        ir.add(IrInst.ret("c"));

        ir.add(IrInst.endFunc());

        CodeGenerator cg = new CodeGenerator();
        String asm = cg.generate(ir);

        System.out.println(asm);
    }
}