package com.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 简单的 IR 指令列表与临时/标签生成器。
 */
public class IrList {

    private final List<IrInst> insts = new ArrayList<>();
    private int tmpId = 0;
    private int labelId = 0;
    private final java.util.List<String> temps = new ArrayList<>();

    public void add(IrInst inst) {
        insts.add(inst);
    }

    public void addAll(IrList other) {
        if (other == null) return;
        insts.addAll(other.insts);
    }

    public String newTemp() {
        String name = "t" + (tmpId++);
        temps.add(name);
        return name;
    }

    public String newLabel() {
        return "L" + (labelId++);
    }

    /** 返回本列表中最后生成的临时变量名，若无则返回 null。 */
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
<<<<<<< HEAD
}
=======
}
>>>>>>> dev-d-backend
