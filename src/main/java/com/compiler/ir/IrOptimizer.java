package com.compiler.ir;

import java.util.*;

public class IrOptimizer {

    private List<IrInst> insts;
    private Map<String, String> constMap;
    private Set<String> usedVars;

    public IrList optimize(IrList ir) {
        insts = new ArrayList<>(ir.asList());
        
        boolean changed;
        do {
            changed = false;
            changed |= constantPropagation();
            changed |= deadCodeElimination();
            changed |= peepholeOptimization();
        } while (changed);
        
        IrList result = new IrList();
        for (IrInst inst : insts) {
            result.add(inst);
        }
        return result;
    }

    private boolean constantPropagation() {
        constMap = new HashMap<>();
        boolean changed = false;
        
        for (int i = 0; i < insts.size(); i++) {
            IrInst inst = insts.get(i);
            
            if ("CONST".equals(inst.op) && inst.dst != null && !isTemp(inst.dst)) {
                constMap.put(inst.dst, inst.a);
            }
            
            if ("ASSIGN".equals(inst.op) && inst.dst != null && constMap.containsKey(inst.a)) {
                String constValue = constMap.get(inst.a);
                insts.set(i, IrInst.constant(inst.dst, Integer.parseInt(constValue)));
                constMap.put(inst.dst, constValue);
                changed = true;
            }
            
            if (inst.op.startsWith("BIN_") && constMap.containsKey(inst.a) && constMap.containsKey(inst.b)) {
                int valA = Integer.parseInt(constMap.get(inst.a));
                int valB = Integer.parseInt(constMap.get(inst.b));
                int result = evaluateBinOp(inst.op, valA, valB);
                insts.set(i, IrInst.constant(inst.dst, result));
                constMap.put(inst.dst, Integer.toString(result));
                changed = true;
            }
        }
        return changed;
    }

    private boolean deadCodeElimination() {
        usedVars = new HashSet<>();
        boolean changed = false;
        
        for (int i = insts.size() - 1; i >= 0; i--) {
            IrInst inst = insts.get(i);
            
            if ("LABEL".equals(inst.op) || "GOTO".equals(inst.op) || 
                "IFZ".equals(inst.op) || "IFNZ".equals(inst.op) ||
                "CALL".equals(inst.op) || "RET".equals(inst.op)) {
                if (inst.a != null) usedVars.add(inst.a);
                if (inst.b != null) usedVars.add(inst.b);
                continue;
            }
            
            if (inst.op.startsWith("BIN_") || "ASSIGN".equals(inst.op)) {
                if (usedVars.contains(inst.dst)) {
                    if (inst.a != null) usedVars.add(inst.a);
                    if (inst.b != null) usedVars.add(inst.b);
                } else {
                    insts.remove(i);
                    changed = true;
                }
            }
            
            if ("CONST".equals(inst.op)) {
                if (usedVars.contains(inst.dst)) {
                    if (inst.a != null) usedVars.add(inst.a);
                } else {
                    insts.remove(i);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean peepholeOptimization() {
        boolean changed = false;
        
        for (int i = 0; i < insts.size() - 1; i++) {
            IrInst inst1 = insts.get(i);
            IrInst inst2 = insts.get(i + 1);
            
            if ("CONST".equals(inst1.op) && "ASSIGN".equals(inst2.op) && 
                inst1.dst.equals(inst2.a) && isTemp(inst2.dst)) {
                insts.set(i, IrInst.constant(inst2.dst, Integer.parseInt(inst1.a)));
                insts.remove(i + 1);
                changed = true;
                continue;
            }
            
            if ("ASSIGN".equals(inst1.op) && "ASSIGN".equals(inst2.op) && 
                inst1.dst.equals(inst2.a) && isTemp(inst1.dst)) {
                insts.set(i, IrInst.assign(inst2.dst, inst1.a));
                insts.remove(i + 1);
                changed = true;
                continue;
            }
            
            if ("CONST".equals(inst1.op) && inst1.op.startsWith("BIN_") && 
                inst1.dst.equals(inst2.a)) {
                insts.set(i + 1, new IrInst(inst2.op, inst2.dst, inst1.a, inst2.b));
                changed = true;
            }
        }
        return changed;
    }

    private int evaluateBinOp(String op, int a, int b) {
        return switch (op) {
            case "BIN_ADD" -> a + b;
            case "BIN_SUB" -> a - b;
            case "BIN_MUL" -> a * b;
            case "BIN_DIV" -> a / b;
            case "BIN_LT" -> a < b ? 1 : 0;
            case "BIN_GT" -> a > b ? 1 : 0;
            case "BIN_LE" -> a <= b ? 1 : 0;
            case "BIN_GE" -> a >= b ? 1 : 0;
            case "BIN_EQ" -> a == b ? 1 : 0;
            case "BIN_NE" -> a != b ? 1 : 0;
            case "BIN_MOD" -> a % b;
            case "BIN_SLL" -> a << b;
            default -> 0;
        };
    }

    private boolean isTemp(String name) {
        return name != null && name.startsWith("tmp");
    }
}
