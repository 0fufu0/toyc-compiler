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

    boolean collecting = false;
    boolean textEmitted = false;

    static class Context {
        Map<String, Integer> offsetMap = new HashMap<>();
        int offset = 0;
    }

    // =========================
    // ENTRY
    // =========================
    public String generate(IrList irList) {

        out.setLength(0);
        textEmitted = false;

        List<IrInst> insts = irList.asList();

        for (IrInst inst : insts) {
            dispatch(inst);
        }

        return out.toString();
    }

    // =========================
    // DISPATCH
    // =========================
    void dispatch(IrInst inst) {

        switch (inst.op) {

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

    // =========================
    // FUNC START
    // =========================
    void startFunc(IrInst inst) {

        ctx = new Context();
        offsetMap = ctx.offsetMap;

        currentFunctionIR = new ArrayList<>();
        collecting = true;

        if (!textEmitted) {
            emit(".text");
            textEmitted = true;
        }
        emit(".globl " + inst.dst);
        emit(inst.dst + ":");
    }

    // =========================
    // FUNC END
    // =========================
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

    // =========================
    // IR GENERATION
    // =========================
    void generateInst(IrInst i) {

        switch (i.op) {

            case "CONST" -> genConst(i);
            case "ASSIGN" -> genAssign(i);

            case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV" -> genBinary(i);

            case "LABEL" -> emit(i.dst + ":");
            case "GOTO" -> emit("j " + i.dst);

            case "IFZ" -> genIfz(i);
            case "IFNZ" -> genIfnz(i);

            case "CALL" -> genCall(i);
            case "RET" -> genRet(i);
        }
    }

    // =========================
    // PREPARE
    // =========================
    void prepare(List<IrInst> insts) {

        ctx.offsetMap.clear();
        ctx.offset = 0;

        for (IrInst inst : insts) {

            switch (inst.op) {

                case "CONST", "ASSIGN" -> {
                    alloc(inst.dst);
                    if (isVariable(inst.a)) alloc(inst.a);
                }

                case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV" -> {
                    alloc(inst.dst);
                    alloc(inst.a);
                    alloc(inst.b);
                }

                case "IFZ", "IFNZ" -> alloc(inst.a);

                case "CALL" -> {
                    if (isVariable(inst.dst)) alloc(inst.dst);
                }

                case "RET" -> {
                    if (isVariable(inst.a)) alloc(inst.a);
                }

                default -> {}
            }
        }

        allocRA();
        stackSize = (-ctx.offset + 15) / 16 * 16; // 16-byte align（关键）
    }

    void allocRA() {
        ctx.offset -= 4;
        raOffset = ctx.offset;
    }

    // =========================
    // ALLOC
    // =========================
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

    // =========================
    // LOAD / STORE
    // =========================
    void load(String var, String reg) {
        if (isVariable(var)) {
            emit("lw " + reg + ", " + ctx.offsetMap.get(var) + "(sp)");
        } else {
            emit("li " + reg + ", " + var);
        }
    }

    void store(String reg, String var) {
        if (isVariable(var)) {
            emit("sw " + reg + ", " + ctx.offsetMap.get(var) + "(sp)");
        }
    }

    void emitPrologue() {
        emit("addi sp, sp, -" + stackSize);
        emit("sw ra, " + raOffset + "(sp)");
    }

    void emitEpilogue() {
        emit("lw ra, " + raOffset + "(sp)");
        emit("addi sp, sp, " + stackSize);
        emit("ret");
    }

    // =========================
    // OPS
    // =========================
    void genConst(IrInst i) {
        load(i.a, "t0");
        store("t0", i.dst);
    }

    void genAssign(IrInst i) {
        load(i.a, "t0");
        store("t0", i.dst);
    }

    void genBinary(IrInst i) {

        load(i.a, "t0");
        load(i.b, "t1");

        switch (i.op) {
            case "BIN_ADD" -> emit("add t2, t0, t1");
            case "BIN_SUB" -> emit("sub t2, t0, t1");
            case "BIN_MUL" -> emit("mul t2, t0, t1");
            case "BIN_DIV" -> emit("div t2, t0, t1");
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
        emit("call " + i.a);
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