package com.compiler.driver;

import com.compiler.backend.CodeGenerator;
import com.compiler.ir.*;

import java.util.*;

public class Main {

    public static void main(String[] args) {

        IrList ir = new IrList();

        /* =========================
         * 全局变量
         * ========================= */

// int g1 = 10
        ir.add(IrInst.constant("g1", 10));

// int g2 = 20
        ir.add(IrInst.constant("g2", 20));

// int g3 = 0
        ir.add(IrInst.constant("g3", 0));




        ir.add(IrInst.func("main"));

        /* 局部变量 */
        ir.add(IrInst.constant("x", 5));
        ir.add(IrInst.constant("y", 3));

        /* z = g1 + x */
        ir.add(IrInst.bin("t0", "ADD", "g1", "x"));
        ir.add(IrInst.assign("z", "t0"));

        /* if (z > g2) g3 = z else g3 = g2 */

        ir.add(IrInst.bin("t1", "GT", "z", "g2"));
        ir.add(IrInst.ifnz("t1", "L1"));

        ir.add(IrInst.constant("g3", 0));
        ir.add(IrInst.ggoto("L2"));

        ir.add(IrInst.label("L1"));
        ir.add(IrInst.assign("g3", "z"));

        ir.add(IrInst.label("L2"));

        /* y = y + g3 */
        ir.add(IrInst.bin("t2", "ADD", "y", "g3"));
        ir.add(IrInst.assign("y", "t2"));

        /* return y */
        ir.add(IrInst.ret("y"));

        ir.add(IrInst.endFunc());

        CodeGenerator codeGenerator=new CodeGenerator();
        System.out.println(codeGenerator.generate(ir));
    }
}