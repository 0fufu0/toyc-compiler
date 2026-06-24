package com.compiler.backend;

import java.util.*;
import com.compiler.ir.*;

public class CodeGenerator {

    StringBuilder out = new StringBuilder();

    Map<String, Integer> offsetMap;
    int stackSize;
    int raOffset;
    Context ctx;

    List<IrInst> currentFunctionIR;

    boolean dataEmitted = false;
    Set<String> globalVars = new HashSet<>();

    boolean collecting = false;
    boolean textEmitted = false;
    int tempRegCounter = 0;


    static class Context {
        Map<String, Integer> offsetMap = new HashMap<>();
        int offset = 0;
    }

    public String generate(IrList irList) {

        out.setLength(0);
        textEmitted = false;

        List<IrInst> insts = irList.asList();

        for (IrInst inst : insts) {
            dispatch(inst);
        }

        return out.toString();
    }

    void dispatch(IrInst inst) {

        switch (inst.op) {

            case "GLOBAL" -> genGlobal(inst);

            case "FUNC" -> startFunc(inst);

            case "ENDFUNC" -> endFunc();

            default -> {
                if (collecting) {
                    currentFunctionIR.add(inst);
                } else {
                    // global scope (暂时忽略)
                }
            }
        }
    }

    void startFunc(IrInst inst) {

        ctx = new Context();
        offsetMap = ctx.offsetMap;

        currentFunctionIR = new ArrayList<>();
        collecting = true;

        if (!textEmitted) {
            emit(".text");
            textEmitted = true;
            emit(".globl _start");
            emit("_start:");
            emit("call main");
            emit("li a7, 10");
            emit("ecall");
        }
        emit(inst.dst + ":");
    }

    void endFunc() {

        collecting = false;

        prepare(currentFunctionIR);
        emitPrologue();

        for (IrInst i : currentFunctionIR) {
            generateInst(i);
        }

        if (currentFunctionIR.isEmpty() || !"RET".equals(currentFunctionIR.get(currentFunctionIR.size() - 1).op)) {
            emitEpilogue();
        }
    }

    void generateInst(IrInst i) {

        switch (i.op) {

            case "CONST" -> genConst(i);
            case "ASSIGN" -> genAssign(i);

            case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV","BIN_LT", "BIN_GT","BIN_LE","BIN_GE","BIN_EQ","BIN_NE","BIN_MOD"-> genBinary(i);
            case "NEG", "NOT" -> genUnary(i);
            case "LABEL" -> emit(i.dst + ":");
            case "GOTO" -> emit("j " + i.dst);

            case "IFZ" -> genIfz(i);
            case "IFNZ" -> genIfnz(i);

            case "CALL" -> genCall(i);
            case "RET" -> genRet(i);
            default -> throw new RuntimeException("Unknown op: " + i.op);
        }
    }

    void prepare(List<IrInst> insts) {

        ctx.offsetMap.clear();
        ctx.offset = 0;

        for (IrInst inst : insts) {

            switch (inst.op) {

                case "CONST", "ASSIGN" -> {
                    alloc(inst.dst);
                    if (isVariable(inst.a)) alloc(inst.a);
                }

                case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV","BIN_LT", "BIN_GT","BIN_LE","BIN_GE","BIN_EQ","BIN_NE","BIN_MOD"-> {
                    alloc(inst.dst);
                    alloc(inst.a);
                    alloc(inst.b);
                }
                case "NEG", "NOT" -> {
                    alloc(inst.dst);
                    alloc(inst.a);
                }
                case "IFZ", "IFNZ" -> alloc(inst.a);

                case "CALL" -> {
                    if (isVariable(inst.dst)) alloc(inst.dst);
                }

                case "RET" -> {
                    if (isVariable(inst.a)) alloc(inst.a);
                }


            }
        }

        allocRA();
        stackSize = (-ctx.offset + 15) / 16 * 16; // 16-byte align（关键）
    }

    String allocTemp() {
        return "t" + (tempRegCounter++ % 7); // t0-t6循环
    }

    void allocRA() {
        ctx.offset -= 4;
        raOffset = ctx.offset;
    }

    void alloc(String var) {
        if (var == null) return;

        if (!ctx.offsetMap.containsKey(var)) {
            ctx.offset -= 4;
            ctx.offsetMap.put(var, ctx.offset);
        }
    }

    boolean isVariable(String x) {
        return x != null && !x.matches("-?\\d+");
    }

    void load(String var, String reg) {

        if (!isVariable(var)) {
            emit("li " + reg + ", " + var);
            return;
        }

        if (globalVars.contains(var)) {

            emit("la t6, " + var);
            emit("lw " + reg + ", 0(t6)");

        } else {

            emit("lw " + reg + ", " +
                    offsetMap.get(var) + "(sp)");
        }
    }

    void store(String reg, String var) {

        if (!isVariable(var))
            return;

        if (globalVars.contains(var)) {

            emit("la t6, " + var);
            emit("sw " + reg + ", 0(t6)");

        } else {

            emit("sw " + reg + ", " +
                    offsetMap.get(var) + "(sp)");
        }
    }

    void emitPrologue() {
        emit("addi sp, sp, -" + stackSize);
    }

    void emitEpilogue() {
        emit("addi sp, sp, " + stackSize);
        emit("ret");
    }

    void genConst(IrInst i) {
        load(i.a, "t0");
        store("t0", i.dst);
    }

    void genAssign(IrInst i) {
        load(i.a, "t0");
        store("t0", i.dst);
    }

    void genGlobal(IrInst i) {

        if (!dataEmitted) {
            emit(".data");
            dataEmitted = true;
        }

        emit(i.dst + ":");
        emit(".word " + i.a);

        globalVars.add(i.dst);
    }

    void genBinary(IrInst i) {

        load(i.a, "t0");
        load(i.b, "t1");

        switch (i.op) {
            case "BIN_ADD" -> emit("add t2, t0, t1");
            case "BIN_SUB" -> emit("sub t2, t0, t1");
            case "BIN_MUL" -> emit("mul t2, t0, t1");
            case "BIN_DIV" -> emit("div t2, t0, t1");

            case "BIN_LT" -> emit("slt t2, t0, t1"); // t0 < t1

            case "BIN_GT" -> {
                emit("slt t2, t1, t0"); // t0 > t1
            }

            case "BIN_LE" -> {
                emit("slt t2, t1, t0");
                emit("xori t2, t2, 1");
            }

            case "BIN_GE" -> {
                emit("slt t2, t0, t1");
                emit("xori t2, t2, 1");
            }

            case "BIN_EQ" -> {
                emit("sub t2, t0, t1");
                emit("seqz t2, t2"); // t2==0 -> 1
            }

            case "BIN_NE" -> {
                emit("sub t2, t0, t1");
                emit("snez t2, t2"); // t2!=0 -> 1
            }
            case "BIN_MOD" -> emit("rem t2, t0, t1");

            default -> throw new RuntimeException("Unknown op: " + i.op);
        }

        store("t2", i.dst);
    }

    void genUnary(IrInst i) {

        load(i.a, "t0");   // 只有一个操作数

        switch (i.op) {

            case "NEG" -> {
                // t2 = -t0
                emit("sub t2, x0, t0");
            }

            case "NOT" -> {
                // t2 = (t0 == 0)
                emit("seqz t2, t0");
            }

            default -> throw new RuntimeException("Unknown unary op: " + i.op);
        }

        store("t2", i.dst);
    }

    void genIfz(IrInst i) {
        load(i.a, "t0");
        emit("beqz t0, " + i.dst);
    }

    void genIfnz(IrInst i) {
        load(i.a, "t0");
        emit("bnez t0, " + i.dst);
    }

    void genCall(IrInst i) {
        emit("sw ra, " + raOffset + "(sp)");
        emit("call " + i.a);
        emit("lw ra, " + raOffset + "(sp)");
        store("a0", i.dst);
    }

    void genRet(IrInst i) {

        if (i.a != null) {
            load(i.a, "a0");
        }

        emit("addi sp, sp, " + stackSize);
        emit("ret");
    }

    void emit(String s) {
        out.append(s).append("\n");
    }
}