package com.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 简单的 IR 指令列表与临时/标签生成器。
 */
public class IrList {

    private final List<IrInst> insts = new ArrayList<>();
    static private int tmpId = 0;
    private int labelId = 0;
    private final java.util.List<String> temps = new ArrayList<>();

    public static int getTmpId() {
        return tmpId;
    }

    public void add(IrInst inst) {
        insts.add(inst);
    }

    public void addAll(IrList other) {
        if (other == null) return;
        // 重命名 other 中的标签避免冲突
        java.util.Map<String, String> labelMap = new java.util.HashMap<>();
        for (IrInst inst : other.insts) {
            if ("LABEL".equals(inst.op) && inst.dst != null && inst.dst.startsWith("L")) {
                if (!labelMap.containsKey(inst.dst)) {
                    String newName = "L" + (this.labelId++);
                    labelMap.put(inst.dst, newName);
                }
            }
        }
        for (IrInst inst : other.insts) {
            IrInst renamed = remapLabels(inst, labelMap);
            insts.add(renamed);
        }
        temps.addAll(other.temps);
        this.tmpId = Math.max(this.tmpId, other.tmpId);
    }

    private static IrInst remapLabels(IrInst inst, java.util.Map<String, String> labelMap) {
        if (labelMap.isEmpty()) return inst;
        String newDst = (inst.dst != null && labelMap.containsKey(inst.dst)) ? labelMap.get(inst.dst) : inst.dst;
        String newA = (inst.a != null && labelMap.containsKey(inst.a)) ? labelMap.get(inst.a) : inst.a;
        String newB = (inst.b != null && labelMap.containsKey(inst.b)) ? labelMap.get(inst.b) : inst.b;
        if (newDst == inst.dst && newA == inst.a && newB == inst.b) return inst;
        return new IrInst(inst.op, newDst, newA, newB);
    }

    public String newTemp() {
        String name = "t" + (tmpId++);
        temps.add(name);
        return name;
    }

    public String newLabel() {
        return "L" + (labelId++);
    }

    /**
     * 返回本列表中最后生成的临时变量名，若无则返回 null。
     */
    public String lastTemp() {
        if (temps.isEmpty()) return null;
        return temps.get(temps.size() - 1);
    }

    public List<IrInst> asList() {
        return Collections.unmodifiableList(insts);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IrInst inst : insts) {
            sb.append(inst.toString()).append('\n');
        }
        return sb.toString();
    }
}