package com.compiler.ir;

import java.util.*;

public class Optimizer {

    public IrList optimize(IrList ir) {
        IrList optimized = new IrList();
        optimized.addAll(ir);

        List<IrInst> insts = new ArrayList<>(optimized.asList());
        List<IrInst> result = new ArrayList<>();

        int i = 0;
        while (i < insts.size()) {
            IrInst inst = insts.get(i);

            if ("FUNC".equals(inst.op)) {
                int j = i + 1;
                while (j < insts.size() && !"ENDFUNC".equals(insts.get(j).op)) {
                    j++;
                }
                if (j < insts.size()) {
                    List<IrInst> funcBody = insts.subList(i, j + 1);
                    List<IrInst> optimizedFunc = optimizeFunction(funcBody);
                    result.addAll(optimizedFunc);
                    i = j + 1;
                    continue;
                }
            }

            result.add(inst);
            i++;
        }

        optimized = new IrList();
        for (IrInst inst : result) {
            optimized.add(inst);
        }

        return optimized;
    }

    private List<IrInst> optimizeFunction(List<IrInst> funcInsts) {
        List<IrInst> result = new ArrayList<>(funcInsts);
        result = constantPropagation(result);
        result = deadCodeElimination(result);
        result = peepholeOptimization(result);
        result = commonSubexpressionElimination(result);
        result = reorderBlocks(result);
        return result;
    }

    private List<IrInst> constantPropagation(List<IrInst> insts) {
        Map<String, Integer> constMap = new HashMap<>();
        List<IrInst> result = new ArrayList<>();

        for (IrInst inst : insts) {
            if ("CONST".equals(inst.op)) {
                try {
                    int val = Integer.parseInt(inst.a);
                    constMap.put(inst.dst, val);
                } catch (NumberFormatException e) {
                    constMap.remove(inst.dst);
                }
                result.add(inst);
            } else if ("ASSIGN".equals(inst.op)) {
                if (constMap.containsKey(inst.a)) {
                    result.add(IrInst.constant(inst.dst, constMap.get(inst.a)));
                    constMap.put(inst.dst, constMap.get(inst.a));
                } else if (!isVariable(inst.a)) {
                    try {
                        int val = Integer.parseInt(inst.a);
                        result.add(IrInst.constant(inst.dst, val));
                        constMap.put(inst.dst, val);
                    } catch (NumberFormatException e) {
                        constMap.remove(inst.dst);
                        result.add(inst);
                    }
                } else {
                    constMap.remove(inst.dst);
                    result.add(inst);
                }
            } else if (inst.op.startsWith("BIN_")) {
                String op = inst.op.substring(4);
                Integer aVal = constMap.get(inst.a);
                Integer bVal = constMap.get(inst.b);

                if (!isVariable(inst.a)) {
                    try {
                        aVal = Integer.parseInt(inst.a);
                    } catch (NumberFormatException e) {}
                }
                if (!isVariable(inst.b)) {
                    try {
                        bVal = Integer.parseInt(inst.b);
                    } catch (NumberFormatException e) {}
                }

                if (aVal != null && bVal != null) {
                    int resultVal = evaluateBinOp(op, aVal, bVal);
                    result.add(IrInst.constant(inst.dst, resultVal));
                    constMap.put(inst.dst, resultVal);
                } else if (aVal != null && "NEG".equals(op)) {
                    result.add(IrInst.constant(inst.dst, -aVal));
                    constMap.put(inst.dst, -aVal);
                } else if (aVal != null && "NOT".equals(op)) {
                    result.add(IrInst.constant(inst.dst, aVal == 0 ? 1 : 0));
                    constMap.put(inst.dst, aVal == 0 ? 1 : 0);
                } else {
                    constMap.remove(inst.dst);
                    IrInst newInst = new IrInst(inst.op, inst.dst,
                            aVal != null ? Integer.toString(aVal) : inst.a,
                            bVal != null ? Integer.toString(bVal) : inst.b);
                    result.add(newInst);
                }
            } else if ("IFZ".equals(inst.op)) {
                Integer condVal = constMap.get(inst.a);
                if (condVal != null) {
                    if (condVal == 0) {
                        result.add(IrInst.ggoto(inst.dst));
                    }
                } else {
                    result.add(inst);
                }
            } else if ("IFNZ".equals(inst.op)) {
                Integer condVal = constMap.get(inst.a);
                if (condVal != null) {
                    if (condVal != 0) {
                        result.add(IrInst.ggoto(inst.dst));
                    }
                } else {
                    result.add(inst);
                }
            } else if ("PARAM".equals(inst.op)) {
                constMap.remove(inst.dst);
                result.add(inst);
            } else if ("CALL".equals(inst.op)) {
                constMap.clear();
                result.add(inst);
            } else {
                result.add(inst);
            }
        }

        return result;
    }

    private int evaluateBinOp(String op, int a, int b) {
        return switch (op) {
            case "ADD" -> a + b;
            case "SUB" -> a - b;
            case "MUL" -> a * b;
            case "DIV" -> a / b;
            case "MOD" -> a % b;
            case "LT" -> a < b ? 1 : 0;
            case "GT" -> a > b ? 1 : 0;
            case "LE" -> a <= b ? 1 : 0;
            case "GE" -> a >= b ? 1 : 0;
            case "EQ" -> a == b ? 1 : 0;
            case "NE" -> a != b ? 1 : 0;
            case "AND" -> (a != 0 && b != 0) ? 1 : 0;
            case "OR" -> (a != 0 || b != 0) ? 1 : 0;
            default -> 0;
        };
    }

    private List<IrInst> deadCodeElimination(List<IrInst> insts) {
        Set<String> usedVars = new HashSet<>();
        List<IrInst> result = new ArrayList<>();

        for (int i = insts.size() - 1; i >= 0; i--) {
            IrInst inst = insts.get(i);

            if ("RET".equals(inst.op)) {
                if (inst.a != null && isVariable(inst.a)) {
                    usedVars.add(inst.a);
                }
                result.add(inst);
            } else if ("CALL".equals(inst.op)) {
                if (inst.dst != null && isVariable(inst.dst)) {
                    usedVars.add(inst.dst);
                }
                result.add(inst);
            } else if ("IFZ".equals(inst.op) || "IFNZ".equals(inst.op)) {
                if (inst.a != null && isVariable(inst.a)) {
                    usedVars.add(inst.a);
                }
                result.add(inst);
            } else if ("GOTO".equals(inst.op)) {
                result.add(inst);
            } else if ("LABEL".equals(inst.op)) {
                result.add(inst);
            } else if ("FUNC".equals(inst.op) || "ENDFUNC".equals(inst.op) || "PARAM".equals(inst.op)) {
                result.add(inst);
            } else if (inst.op.startsWith("BIN_")) {
                if (isVariable(inst.dst) && usedVars.contains(inst.dst)) {
                    if (isVariable(inst.a)) {
                        usedVars.add(inst.a);
                    }
                    if (isVariable(inst.b)) {
                        usedVars.add(inst.b);
                    }
                    result.add(inst);
                }
            } else if ("CONST".equals(inst.op) || "ASSIGN".equals(inst.op)) {
                if (isVariable(inst.dst) && usedVars.contains(inst.dst)) {
                    if (isVariable(inst.a)) {
                        usedVars.add(inst.a);
                    }
                    result.add(inst);
                }
            } else {
                result.add(inst);
            }
        }

        Collections.reverse(result);
        return result;
    }

    private boolean isVariable(String x) {
        return x != null && !x.matches("-?\\d+");
    }

    private List<IrInst> unreachableCodeElimination(List<IrInst> insts) {
        Set<Integer> reachablePositions = new HashSet<>();
        Queue<Integer> workQueue = new LinkedList<>();

        for (int i = 0; i < insts.size(); i++) {
            IrInst inst = insts.get(i);
            if ("FUNC".equals(inst.op)) {
                reachablePositions.add(i);
                workQueue.add(i);
                break;
            }
        }

        while (!workQueue.isEmpty()) {
            Integer pos = workQueue.poll();
            if (pos >= insts.size()) continue;

            for (int i = pos; i < insts.size(); i++) {
                IrInst inst = insts.get(i);
                
                if (!reachablePositions.contains(i)) {
                    reachablePositions.add(i);
                    workQueue.add(i);
                }

                if ("GOTO".equals(inst.op)) {
                    for (int j = 0; j < insts.size(); j++) {
                        if ("LABEL".equals(insts.get(j).op) && inst.dst.equals(insts.get(j).dst)) {
                            if (!reachablePositions.contains(j)) {
                                reachablePositions.add(j);
                                workQueue.add(j);
                            }
                            break;
                        }
                    }
                    break;
                }

                if ("IFZ".equals(inst.op) || "IFNZ".equals(inst.op)) {
                    for (int j = 0; j < insts.size(); j++) {
                        if ("LABEL".equals(insts.get(j).op) && inst.dst.equals(insts.get(j).dst)) {
                            if (!reachablePositions.contains(j)) {
                                reachablePositions.add(j);
                                workQueue.add(j);
                            }
                            break;
                        }
                    }
                    if (i + 1 < insts.size()) {
                        if (!reachablePositions.contains(i + 1)) {
                            reachablePositions.add(i + 1);
                            workQueue.add(i + 1);
                        }
                    }
                    break;
                }

                if ("RET".equals(inst.op) || "ENDFUNC".equals(inst.op)) {
                    break;
                }
            }
        }

        List<IrInst> result = new ArrayList<>();
        for (int i = 0; i < insts.size(); i++) {
            if (reachablePositions.contains(i)) {
                result.add(insts.get(i));
            }
        }

        return result;
    }

    private List<IrInst> commonSubexpressionElimination(List<IrInst> insts) {
        Map<String, String> exprMap = new HashMap<>();
        List<IrInst> result = new ArrayList<>();

        for (IrInst inst : insts) {
            if (inst.op.startsWith("BIN_")) {
                String op = inst.op.substring(4);
                String exprKey = op + "," + inst.a + "," + inst.b;

                if (exprMap.containsKey(exprKey)) {
                    result.add(IrInst.assign(inst.dst, exprMap.get(exprKey)));
                } else {
                    exprMap.put(exprKey, inst.dst);
                    result.add(inst);
                }
            } else if ("ASSIGN".equals(inst.op)) {
                String exprKey = "ASSIGN," + inst.a;
                if (exprMap.containsKey(exprKey)) {
                    result.add(IrInst.assign(inst.dst, exprMap.get(exprKey)));
                } else {
                    exprMap.put(exprKey, inst.dst);
                    result.add(inst);
                }
            } else if ("CONST".equals(inst.op)) {
                String exprKey = "CONST," + inst.a;
                exprMap.put(exprKey, inst.dst);
                result.add(inst);
            } else if ("PARAM".equals(inst.op)) {
                exprMap.put("ASSIGN," + inst.dst, inst.dst);
                result.add(inst);
            } else if ("CALL".equals(inst.op)) {
                exprMap.clear();
                result.add(inst);
            } else {
                result.add(inst);
            }
        }

        return result;
    }

    private List<IrInst> peepholeOptimization(List<IrInst> insts) {
        List<IrInst> result = new ArrayList<>();

        for (int i = 0; i < insts.size(); i++) {
            IrInst inst = insts.get(i);

            if (inst.op.startsWith("BIN_")) {
                String op = inst.op.substring(4);

                if ("ADD".equals(op) && "0".equals(inst.b)) {
                    result.add(IrInst.assign(inst.dst, inst.a));
                    continue;
                }

                if ("SUB".equals(op) && "0".equals(inst.b)) {
                    result.add(IrInst.assign(inst.dst, inst.a));
                    continue;
                }

                if ("MUL".equals(op)) {
                    try {
                        int bVal = Integer.parseInt(inst.b);
                        if (bVal == 1) {
                            result.add(IrInst.assign(inst.dst, inst.a));
                            continue;
                        }
                        if (bVal == 2) {
                            result.add(new IrInst("BIN_SLL", inst.dst, inst.a, "1"));
                            continue;
                        }
                        if (bVal == 4) {
                            result.add(new IrInst("BIN_SLL", inst.dst, inst.a, "2"));
                            continue;
                        }
                        if (bVal == 8) {
                            result.add(new IrInst("BIN_SLL", inst.dst, inst.a, "3"));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                    }

                    try {
                        int aVal = Integer.parseInt(inst.a);
                        if (aVal == 1) {
                            result.add(IrInst.assign(inst.dst, inst.b));
                            continue;
                        }
                        if (aVal == 2) {
                            result.add(new IrInst("BIN_SLL", inst.dst, inst.b, "1"));
                            continue;
                        }
                        if (aVal == 4) {
                            result.add(new IrInst("BIN_SLL", inst.dst, inst.b, "2"));
                            continue;
                        }
                        if (aVal == 8) {
                            result.add(new IrInst("BIN_SLL", inst.dst, inst.b, "3"));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                    }
                }

                if ("AND".equals(op)) {
                    try {
                        int bVal = Integer.parseInt(inst.b);
                        if (bVal == 0) {
                            result.add(IrInst.constant(inst.dst, 0));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                    }
                }

                if ("OR".equals(op)) {
                    try {
                        int bVal = Integer.parseInt(inst.b);
                        if (bVal != 0) {
                            result.add(IrInst.constant(inst.dst, 1));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            if ("GOTO".equals(inst.op) && i + 1 < insts.size()) {
                IrInst next = insts.get(i + 1);
                if ("LABEL".equals(next.op) && inst.dst.equals(next.dst)) {
                    result.add(next);
                    i++;
                    continue;
                }
            }

            result.add(inst);
        }

        return result;
    }

    private List<IrInst> reorderBlocks(List<IrInst> insts) {
        Map<String, Integer> labelMap = new HashMap<>();
        List<List<IrInst>> blocks = new ArrayList<>();
        List<IrInst> currentBlock = new ArrayList<>();

        for (int i = 0; i < insts.size(); i++) {
            IrInst inst = insts.get(i);

            if ("LABEL".equals(inst.op)) {
                if (!currentBlock.isEmpty()) {
                    blocks.add(currentBlock);
                }
                currentBlock = new ArrayList<>();
                currentBlock.add(inst);
                labelMap.put(inst.dst, blocks.size());
            } else {
                currentBlock.add(inst);
            }
        }
        if (!currentBlock.isEmpty()) {
            blocks.add(currentBlock);
        }

        List<IrInst> result = new ArrayList<>();
        for (List<IrInst> block : blocks) {
            result.addAll(block);
        }

        return result;
    }
}