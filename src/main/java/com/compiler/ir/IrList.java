package com.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 简单的 IR 指令列表与临时/标签生成器。
 */
public class IrList {

    private final List<IrInst> insts = new ArrayList<>();
    private static int tmpId = 0;
    private static int labelId = 0;  // 改为 static，全局统一标签编号
    private final List<String> temps = new ArrayList<>();

    public static int getTmpId() {
        return tmpId;
    }

    public void add(IrInst inst) {
        insts.add(inst);
    }

    public void addAll(IrList other) {
        if (other == null) return;
        // 不需要重命名标签，因为 labelId 是全局统一的
        for (IrInst inst : other.insts) {
            insts.add(inst);
        }
        temps.addAll(other.temps);
    }

    public String newTemp() {
        String name = "tmp" + (tmpId++);
        temps.add(name);
        return name;
    }

    public String newLabel() {
        return "L" + (labelId++);
    }

    /**
     * 重置计数器（用于新编译单元）
     */
    public static void reset() {
        tmpId = 0;
        labelId = 0;
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