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
    Map<String, Integer> paramOffset = new HashMap<>();

    boolean dataEmitted = false;
    Set<String> globalVars = new HashSet<>();
    List<String> argList = new ArrayList<>();

    boolean collecting = false;
    boolean textEmitted = false;
    boolean isGlobal=true;
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

            case "FUNC" -> startFunc(inst);

            case "ENDFUNC" -> endFunc();

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
            case "PARAM" -> genParam(i);
            case "ARG" -> genArg(i);


            case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV","BIN_LT", "BIN_GT","BIN_LE","BIN_GE","BIN_EQ","BIN_NE","BIN_MOD"-> genBinary(i);
            case "BIN_AND", "BIN_OR" -> genBinary(i);
            case "BIN_NEG", "BIN_NOT" -> genUnary(i);
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

        paramOffset.clear();
        paramBase = 0;

        for (IrInst inst : insts) {

            switch (inst.op) {

                case "PARAM" -> {
                    //System.out.println("HERE IS PARAM, NAME : "+ inst.dst);
                    allocParam(inst.dst);
                }


                case "CONST", "ASSIGN" -> {
                    alloc(inst.dst);
                    if (isVariable(inst.a)) alloc(inst.a);
                }

                case "BIN_ADD", "BIN_SUB", "BIN_MUL", "BIN_DIV",
                     "BIN_LT", "BIN_GT", "BIN_LE", "BIN_GE",
                     "BIN_EQ", "BIN_NE", "BIN_MOD" -> {
                    alloc(inst.dst);
                }

                case "NEG", "NOT" -> {
                    alloc(inst.dst);
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
        stackSize = (-ctx.offset + 15 + paramBase) / 16 * 16;
    }

    void allocParam(String name) {

        if (name == null) return;

        //System.out.println(name);

        paramBase += 4;
        paramOffset.put(name, paramBase);
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

        if (!ctx.offsetMap.containsKey(var) && !globalVars.contains(var) && !paramOffset.containsKey(var)) {
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
        if (paramOffset!=null && paramOffset.containsKey(var)) {
            //System.out.println("HIT PARAM");
            emit("lw " + reg + ", " + paramOffset.get(var) + "(s0)");
            return;
        }

        if(offsetMap.containsKey(var)){
            emit("lw " + reg + ", " +
                    offsetMap.get(var) + "(sp)");
            return;
        }

        if (globalVars.contains(var)) {

            emit("la t6, " + var);
            emit("lw " + reg + ", 0(t6)");
        }


    }

    void store(String reg, String var) {

        if (!isVariable(var)) return;

        if (paramOffset.containsKey(var)) {
            emit("sw " + reg + ", " + paramOffset.get(var) + "(s0)");
            return;
        }

        if (offsetMap.containsKey(var)) {
            emit("sw " + reg + ", " + offsetMap.get(var) + "(sp)");
            return;
        }


        if (globalVars.contains(var)) {
            emit("la t6, " + var);
            emit("sw " + reg + ", 0(t6)");
        }
    }

    void emitPrologue() {

        emit("addi sp, sp, -" + stackSize);
        emit("sw s0, 0(sp)");
        emit("mv s0, sp");

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
        if(i.a!=null) {
            emit(".word " + i.a);
        } else{
            emit(".word " + "0");
        }

        globalVars.add(i.dst);
    }

    void genArg(IrInst i) {
        argList.add(i.dst);
    }

    void genParam(IrInst i){



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

            case "BIN_AND" -> {
                // t2 = t0 & t1 (非短路)
                emit("snez t0, t0");
                emit("snez t1, t1");
                emit("and t2, t0, t1");
            }

            case "BIN_OR" -> {
                // t2 = t0 | t1 (非短路)
                emit("snez t0, t0");
                emit("snez t1, t1");
                emit("or t2, t0, t1");
            }

            default -> throw new RuntimeException("Unknown op: " + i.op);
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
        int pOffset=0;
        //System.out.println(argList.size());
        for (int k = 0; k < argList.size(); k++) {
            load(argList.get(k), "t0");
            pOffset+=4;
            emit("sw t0, " + pOffset + "(sp)");
        }
        argList.clear();
        emit("sw ra, " + raOffset + "(sp)");
        emit("call " + i.a);
        emit("lw ra, " + raOffset + "(sp)");
        store("a0", i.dst);
    }

    void genRet(IrInst i) {

        if (i.a != null) {
            load(i.a, "a0");
        }

        emit("lw s0, 0(sp)");
        emit("addi sp, sp, " + stackSize);
        emit("ret");
    }

    void emit(String s) {
        out.append(s).append("\n");
    }
}