package com.compiler.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.ir.IrInst;
import com.compiler.ir.IrList;

public class CodeGenerator {

    StringBuilder out = new StringBuilder();

    Map<String, Integer> offsetMap;
    int stackSize;
    int raOffset;
    Context ctx;
    int s0Offset;

    List<IrInst> currentFunctionIR;
    Map<String, Integer> paramOffset = new HashMap<>();
    List<String> paramNames = new ArrayList<>();

    boolean dataEmitted = false;
    Set<String> globalVars = new HashSet<>();
    List<String> argList = new ArrayList<>();

    boolean collecting = false;
    boolean textEmitted = false;
    boolean isGlobal = true;
    int tempRegCounter = 0;
    int paramBase = 0;

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

            case "FUNC" ->
                startFunc(inst);

            case "ENDFUNC" ->
                endFunc();

            case "GLOBAL" ->
                genGlobal(inst);

            default -> {
                if (collecting) {
                    currentFunctionIR.add(inst);
                } else {
                    genGlobal(inst);
                }
            }
        }
    }

    void startFunc(IrInst inst) {

        ctx = new Context();
        offsetMap = ctx.offsetMap;

        currentFunctionIR = new ArrayList<>();
        collecting = true;

        paramOffset.clear();
        paramBase = 0;

        if (!textEmitted) {
            emit(".text");
            textEmitted = true;
        }
        emit(".globl " + inst.dst);
        emit(inst.dst + ":");
    }

    void endFunc() {

        collecting = false;

        prepare(currentFunctionIR);
        emitPrologue();

        for (IrInst i : currentFunctionIR) {
            if ("PARAM".equals(i.op)) {
                continue;
            }
            generateInst(i);
        }

        if (currentFunctionIR.isEmpty() || !"RET".equals(currentFunctionIR.get(currentFunctionIR.size() - 1).op)) {
            emitEpilogue();
        }
    }

    void generateInst(IrInst i) {

        switch (i.op) {

            case "CONST" ->
                genConst(i);
            case "ASSIGN" ->
                genAssign(i);
            case "PARAM" ->
                genParam(i);
            case "ARG" ->
                genArg(i);

            case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV", "BIN_LT", "BIN_GT", "BIN_LE", "BIN_GE", "BIN_EQ", "BIN_NE", "BIN_MOD", "BIN_SLL" ->
                genBinary(i);
            case "BIN_AND", "BIN_OR" ->
                genBinary(i);
            case "BIN_NEG", "BIN_NOT" ->
                genUnary(i);
            case "LABEL" ->
                emit(i.dst + ":");
            case "GOTO" ->
                emit("j " + i.dst);

            case "IFZ" ->
                genIfz(i);
            case "IFNZ" ->
                genIfnz(i);

            case "CALL" ->
                genCall(i);
            case "RET" ->
                genRet(i);
            default ->
                throw new RuntimeException("Unknown op: " + i.op);
        }
    }

    void prepare(List<IrInst> insts) {

        ctx.offsetMap.clear();
        ctx.offset = 0;

        paramOffset.clear();
        paramNames.clear();

        for (IrInst inst : insts) {
            if ("PARAM".equals(inst.op)) {
                paramNames.add(inst.dst);
            }
        }

        for (IrInst inst : insts) {

            switch (inst.op) {

                case "PARAM" -> {
                    // Parameters are not allocated on stack, they use a0-a7 registers
                }

                case "CONST", "ASSIGN" -> {
                    if (!paramNames.contains(inst.dst)) {
                        alloc(inst.dst);
                    }
                    if (isVariable(inst.a) && !paramNames.contains(inst.a)) {
                        alloc(inst.a);
                    }
                }

                case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV", "BIN_LT", "BIN_GT", "BIN_LE", "BIN_GE", "BIN_EQ", "BIN_NE", "BIN_MOD", "BIN_SLL" -> {
                    alloc(inst.dst);
                }

                case "BIN_NEG", "BIN_NOT" -> {
                    alloc(inst.dst);
                }

                case "IFZ", "IFNZ" -> {
                    alloc(inst.a);
                }

                case "CALL" -> {
                    if (isVariable(inst.dst) && !isTemp(inst.dst)) {
                        alloc(inst.dst);
                    }
                }

                case "RET" -> {
                    if (isVariable(inst.a) && !isTemp(inst.a)) {
                        alloc(inst.a);
                    }
                }
            }
        }

        allocRA();
        allocS0();

        int outgoingArgBytes = computeOutgoingArgBytes(insts);
        stackSize = (-ctx.offset + outgoingArgBytes + 15) / 16 * 16;
    }

    int computeOutgoingArgBytes(List<IrInst> insts) {
        int currentArgCount = 0;
        int maxArgCount = 0;
        boolean hasCall = false;

        for (IrInst inst : insts) {
            if ("ARG".equals(inst.op)) {
                currentArgCount++;
            } else if ("CALL".equals(inst.op)) {
                hasCall = true;
                maxArgCount = Math.max(maxArgCount, currentArgCount);
                currentArgCount = 0;
            }
        }

        if (!hasCall) {
            return 0;
        }

        return maxArgCount * 4;
    }

    void allocParam(String name) {

        if (name == null) {
            return;
        }

        ctx.offset -= 4;
        paramOffset.put(name, ctx.offset);
    }

    String allocTemp() {
        return "t" + (tempRegCounter++ % 7); // t0-t6循环
    }

    void allocRA() {
        ctx.offset -= 4;
        raOffset = ctx.offset;
    }

    void allocS0() {
        ctx.offset -= 4;
        s0Offset = ctx.offset;
    }

    void alloc(String var) {
        if (var == null) {
            return;
        }

        if (!ctx.offsetMap.containsKey(var) && !globalVars.contains(var) && !paramNames.contains(var)) {
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
        
        int paramIdx = paramNames.indexOf(var);
        if (paramIdx >= 0) {
            String[] argRegs = {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
            if (paramIdx < argRegs.length) {
                emit("mv " + reg + ", " + argRegs[paramIdx]);
                return;
            }
        }

        if (offsetMap.containsKey(var)) {
            emitLw(reg, offsetMap.get(var), "s0");
            return;
        }

        if (globalVars.contains(var)) {
            emit("la t6, " + var);
            emit("lw " + reg + ", 0(t6)");
        }

    }

    void store(String reg, String var) {

        if (!isVariable(var)) {
            return;
        }

        if (paramNames.contains(var)) {
            return;
        }

        if (offsetMap.containsKey(var)) {
            emitSw(reg, offsetMap.get(var), "s0");
            return;
        }

        if (globalVars.contains(var)) {
            emit("la t6, " + var);
            emit("sw " + reg + ", 0(t6)");
        }
    }

    void emitPrologue() {
        emitAddi("sp", "sp", -stackSize);
        emitSw("s0", stackSize + s0Offset, "sp");
        emitAddi("s0", "sp", stackSize);
    }

    void emitEpilogue() {
        emitLw("s0", stackSize + s0Offset, "sp");
        emitAddi("sp", "sp", stackSize);
        emit("ret");
    }

    void genConst(IrInst i) {
        if (!isVariable(i.a)) {
            emit("li t0, " + i.a);
        } else {
            load(i.a, "t0");
        }
        store("t0", i.dst);
    }

    void genAssign(IrInst i) {
        if (!isVariable(i.a)) {
            emit("li t0, " + i.a);
        } else {
            load(i.a, "t0");
        }
        store("t0", i.dst);
    }
    
    boolean isTemp(String name) {
        return name != null && name.startsWith("tmp");
    }

    void genGlobal(IrInst i) {

        if (!dataEmitted) {
            emit(".data");
            dataEmitted = true;
        }

        emit(i.dst + ":");
        if (i.a != null) {
            emit(".word " + i.a);
        } else {
            emit(".word " + "0");
        }

        globalVars.add(i.dst);
    }

    void genArg(IrInst i) {
        argList.add(i.dst);
    }

    void genParam(IrInst i) {

    }

    void genBinary(IrInst i) {

        load(i.a, "t0");
        load(i.b, "t1");

        switch (i.op) {
            case "BIN_ADD" ->
                emit("add t2, t0, t1");
            case "BIN_SUB" ->
                emit("sub t2, t0, t1");
            case "BIN_MUL" ->
                emit("mul t2, t0, t1");
            case "BIN_DIV" ->
                emit("div t2, t0, t1");
            case "BIN_SLL" ->
                emit("sll t2, t0, t1");

            case "BIN_LT" ->
                emit("slt t2, t0, t1");

            case "BIN_GT" -> {
                emit("slt t2, t1, t0");
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
                emit("seqz t2, t2");
            }

            case "BIN_NE" -> {
                emit("sub t2, t0, t1");
                emit("snez t2, t2");
            }
            case "BIN_MOD" ->
                emit("rem t2, t0, t1");

            case "BIN_AND" -> {
                emit("snez t0, t0");
                emit("snez t1, t1");
                emit("and t2, t0, t1");
            }

            case "BIN_OR" -> {
                emit("snez t0, t0");
                emit("snez t1, t1");
                emit("or t2, t0, t1");
            }

            default ->
                throw new RuntimeException("Unknown op: " + i.op);
        }

        store("t2", i.dst);
    }

    void genUnary(IrInst i) {

        load(i.a, "t0");   // 只有一个操作数

        switch (i.op) {

            case "NEG", "BIN_NEG" -> {
                // t2 = -t0
                emit("sub t2, x0, t0");
            }

            case "NOT", "BIN_NOT" -> {
                // t2 = (t0 == 0)
                emit("seqz t2, t0");
            }

            default ->
                throw new RuntimeException("Unknown unary op: " + i.op);
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
        String[] argRegs = {"a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
        int pOffset = 0;
        for (int k = 0; k < argList.size(); k++) {
            String arg = argList.get(k);
            load(arg, "t0");
            if (k < argRegs.length) {
                emit("mv " + argRegs[k] + ", t0");
            } else {
                pOffset += 4;
                emitSw("t0", pOffset, "sp");
            }
        }
        argList.clear();
        emitSw("ra", raOffset, "s0");
        emit("call " + i.a);
        emitLw("ra", raOffset, "s0");
        store("a0", i.dst);
    }

    void genRet(IrInst i) {

        if (i.a != null) {
            load(i.a, "a0");
        }

        emitLw("s0", stackSize + s0Offset, "sp");
        emitAddi("sp", "sp", stackSize);
        emit("ret");
    }

    void emit(String s) {
        out.append(s).append("\n");
    }

    void emitAddi(String lhs, String rhs, int imm) {
        if (imm >= -2048 && imm <= 2047) {
            emit("addi " + lhs + ", " + rhs + ", " + imm);
        } else {
            emit("li t6, " + imm);
            emit("add " + lhs + ", " + rhs + ", t6");
        }
    }

    void emitLw(String rd, int offset, String base) {
        if (offset >= -2048 && offset <= 2047) {
            emit("lw " + rd + ", " + offset + "(" + base + ")");
        } else {
            emit("li t6, " + offset);
            emit("add t6, t6, " + base);
            emit("lw " + rd + ", 0(t6)");
        }
    }

    void emitSw(String rs2, int offset, String base) {
        if (offset >= -2048 && offset <= 2047) {
            emit("sw " + rs2 + ", " + offset + "(" + base + ")");
        } else {
            emit("li t6, " + offset);
            emit("add t6, t6, " + base);
            emit("sw " + rs2 + ", 0(t6)");
        }
    }
}
