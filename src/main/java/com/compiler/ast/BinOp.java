package com.compiler.ast;

/**
 * 二元表达式运算符。
 */
public enum BinOp {
    OR,         // ||
    AND,        // &&
    LT, GT, LE, GE, EQ, NE,
    ADD, SUB,
    MUL, DIV, MOD
}
