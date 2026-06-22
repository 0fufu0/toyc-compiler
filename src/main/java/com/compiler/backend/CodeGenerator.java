package com.compiler.backend;

import java.util.Stack;
import com.compiler.ir.*;

public class CodeGenerator {
    StringBuilder out = new StringBuilder();

    Stack<String> breakLabels = new Stack<>();
    Stack<String> continueLabels = new Stack<>();

    int labelId = 0;

    String newLabel() {
        return "L" + (labelId++);
    }

    void emit(String s) {
        out.append(s).append("\n");
    }

    void emitLi(String reg, int imm) {
        emit("li " + reg + ", " + imm);
    }

    void emitMv(String dst, String src) {
        emit("mv " + dst + ", " + src);
    }

    void emitAdd(String dst, String a, String b) {
        emit("add " + dst + ", " + a + ", " + b);
    }

    void emitSub(String dst, String a, String b) {
        emit("sub " + dst + ", " + a + ", " + b);
    }

    void emitMul(String dst, String a, String b) {
        emit("mul " + dst + ", " + a + ", " + b);
    }

    void emitDiv(String dst, String a, String b) {
        emit("div " + dst + ", " + a + ", " + b);
    }

    void emitLw(String dst, int offset, String base) {
        emit("lw " + dst + ", " + offset + "(" + base + ")");
    }

    void emitSw(String src, int offset, String base) {
        emit("sw " + src + ", " + offset + "(" + base + ")");
    }

    void emitJ(String label) {
        emit("j " + label);
    }

    void emitBeqz(String reg, String label) {
        emit("beqz " + reg + ", " + label);
    }

    void emitBnez(String reg, String label) {
        emit("bnez " + reg + ", " + label);
    }

    void emitCall(String func) {
        emit("call " + func);
    }

    void emitRet() {
        emit("ret");
    }
}
