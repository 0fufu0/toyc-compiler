package com.compiler.ir;

/**
 * 简单的三地址码指令表示。
 */
public class IrInst {

    public final String op; // e.g., CONST, BIN, ASSIGN, LABEL, GOTO, IFZ, IFNZ, CALL, RET, FUNC, END
    public final String dst;
    public final String a;
    public final String b;

    public IrInst(String op, String dst, String a, String b) {
        this.op = op;
        this.dst = dst;
        this.a = a;
        this.b = b;
    }

    public static IrInst label(String name) {
        return new IrInst("LABEL", name, null, null);
    }

    public static IrInst ggoto(String label) {
        return new IrInst("GOTO", label, null, null);
    }

    public static IrInst ifz(String cond, String label) {
        return new IrInst("IFZ", label, cond, null);
    }

    public static IrInst ifnz(String cond, String label) {
        return new IrInst("IFNZ", label, cond, null);
    }

    public static IrInst assign(String dst, String src) {
        return new IrInst("ASSIGN", dst, src, null);
    }

    public static IrInst constant(String dst, int value) {
        return new IrInst("CONST", dst, Integer.toString(value), null);
    }

    public static IrInst bin(String dst, String op, String a, String b) {
        return new IrInst("BIN_" + op, dst, a, b);
    }

    public static IrInst call(String dst, String func) {
        return new IrInst("CALL", dst, func, null);
    }

    public static IrInst ret(String src) {
        return new IrInst("RET", null, src, null);
    }

    public static IrInst func(String name) {
        return new IrInst("FUNC", name, null, null);
    }

    public static IrInst endFunc() {
        return new IrInst("ENDFUNC", null, null, null);
    }

    @Override
    public String toString() {
        switch (op) {
            case "LABEL":
                return labelString();
            case "GOTO":
                return "GOTO " + dst;
            case "IFZ":
                return "IFZ " + a + " GOTO " + dst;
            case "IFNZ":
                return "IFNZ " + a + " GOTO " + dst;
            case "ASSIGN":
                return dst + " = " + a;
            case "CONST":
                return dst + " = CONST(" + a + ")";
            case "CALL":
                return dst + " = CALL " + a;
            case "RET":
                return "RET " + a;
            case "FUNC":
                return "FUNC " + dst + ":";
            case "ENDFUNC":
                return "ENDFUNC";
            default:
                if (op.startsWith("BIN_")) {
                    String bop = op.substring(4);
                    return dst + " = (" + a + " " + bop + " " + b + ")";
                }
                return op + " " + dst + " " + a + " " + b;
        }
    }

    private String labelString() {
        return dst + ":";
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> dev-d-backend
