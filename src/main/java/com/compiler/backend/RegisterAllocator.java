package com.compiler.backend;

import com.compiler.ir.IrInst;

import java.util.*;

public class RegisterAllocator {

    private static final List<String> AVAILABLE_REGS = Arrays.asList("t0", "t1", "t2", "t3", "t4", "t5", "t6");
    private Map<String, String> varToReg = new HashMap<>();
    private Set<String> usedRegs = new HashSet<>();
    private int nextStackOffset = 0;
    private Map<String, Integer> varToStackOffset = new HashMap<>();

    public void reset() {
        varToReg.clear();
        usedRegs.clear();
        nextStackOffset = 0;
        varToStackOffset.clear();
    }

    public String allocate(String var) {
        if (varToReg.containsKey(var)) {
            return varToReg.get(var);
        }

        if (usedRegs.size() < AVAILABLE_REGS.size()) {
            for (String reg : AVAILABLE_REGS) {
                if (!usedRegs.contains(reg)) {
                    usedRegs.add(reg);
                    varToReg.put(var, reg);
                    return reg;
                }
            }
        }

        String evictedVar = findEvictableVar();
        if (evictedVar != null) {
            String evictedReg = varToReg.remove(evictedVar);
            usedRegs.remove(evictedReg);
            varToStackOffset.put(evictedVar, nextStackOffset);
            nextStackOffset -= 4;
        }

        for (String reg : AVAILABLE_REGS) {
            if (!usedRegs.contains(reg)) {
                usedRegs.add(reg);
                varToReg.put(var, reg);
                return reg;
            }
        }

        varToStackOffset.put(var, nextStackOffset);
        nextStackOffset -= 4;
        return null;
    }

    private String findEvictableVar() {
        return varToReg.keySet().iterator().next();
    }

    public void release(String var) {
        String reg = varToReg.remove(var);
        if (reg != null) {
            usedRegs.remove(reg);
        }
    }

    public boolean isInRegister(String var) {
        return varToReg.containsKey(var);
    }

    public String getRegister(String var) {
        return varToReg.get(var);
    }

    public Integer getStackOffset(String var) {
        return varToStackOffset.get(var);
    }

    public int getStackSize() {
        return Math.abs(nextStackOffset);
    }

    public Map<String, Integer> getAllStackOffsets() {
        return new HashMap<>(varToStackOffset);
    }
}