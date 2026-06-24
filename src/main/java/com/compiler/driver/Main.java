package com.compiler.driver;

import com.compiler.backend.CodeGenerator;
import com.compiler.ir.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {

        IrList ir = new IrList();
        ir.add(IrInst.func("main"));

// a = 10
        ir.add(IrInst.constant("a", 10));

// b = 3
        ir.add(IrInst.constant("b", 3));

// c = a % b
        ir.add(IrInst.bin("c", "MOD", "a", "b"));

// d = a < b
        ir.add(IrInst.bin("d", "LT", "a", "b"));

// e = a > b
        ir.add(IrInst.bin("e", "GT", "a", "b"));

// f = a <= b
        ir.add(IrInst.bin("f", "LE", "a", "b"));

// g = a >= b
        ir.add(IrInst.bin("g", "GE", "a", "b"));

// h = a == b
        ir.add(IrInst.bin("h", "EQ", "a", "b"));

// i = a != b
        ir.add(IrInst.bin("i", "NE", "a", "b"));

// =====================
// IF test 1: if (d) goto L1
// =====================
        ir.add(IrInst.ifnz("d", "L1"));

        ir.add(IrInst.constant("x", 0));
        ir.add(IrInst.ggoto("L2"));

        ir.add(IrInst.label("L1"));
        ir.add(IrInst.constant("x", 1));

        ir.add(IrInst.label("L2"));

// =====================
// IF test 2: if (!h) goto L3
// =====================
        ir.add(IrInst.ifz("h", "L3"));
        ir.add(IrInst.constant("y", 100));
        ir.add(IrInst.ggoto("L4"));

        ir.add(IrInst.label("L3"));
        ir.add(IrInst.constant("y", 200));

        ir.add(IrInst.label("L4"));

// return x + y
        ir.add(IrInst.bin("t0", "ADD", "x", "y"));
        ir.add(IrInst.ret("t0"));

        ir.add(IrInst.endFunc());

        CodeGenerator codeGenerator=new CodeGenerator();
        System.out.println(codeGenerator.generate(ir));
    }
}