package com.compiler.ast;

/**
 * 声明节点基类（全局/局部变量或常量声明）。
 *
 * ToyC 文法中 stmt 可以直接是 decl：
 * stmt -> decl
 * 所以 DeclNode 也应该属于 StmtNode，否则 if/while 后面直接接声明时，
 * AstBuilder 会把 VarDeclNode / ConstDeclNode 强转为 StmtNode 并抛出 ClassCastException。
 */
public abstract class DeclNode extends StmtNode {
}