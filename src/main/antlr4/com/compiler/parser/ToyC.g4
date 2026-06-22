grammar ToyC;

// ========== 语法规则 ==========

compUnit
    : (decl | funcDef)+ EOF
    ;

decl
    : constDecl
    | varDecl
    ;

constDecl
    : CONST INT ID ASSIGN expr SEMI
    ;

varDecl
    : INT ID ASSIGN expr SEMI
    ;

stmt
    : block
    | SEMI
    | expr SEMI
    | ID ASSIGN expr SEMI
    | decl
    | IF LPAREN expr RPAREN stmt (ELSE stmt)?
    | WHILE LPAREN expr RPAREN stmt
    | BREAK SEMI
    | CONTINUE SEMI
    | RETURN expr? SEMI
    ;

block
    : LBRACE stmt* RBRACE
    ;

funcDef
    : (INT | VOID) ID LPAREN paramList? RPAREN block
    ;

paramList
    : param (COMMA param)*
    ;

param
    : INT ID
    ;

expr
    : lOrExpr
    ;

lOrExpr
    : lAndExpr
    | lOrExpr OR lAndExpr
    ;

lAndExpr
    : relExpr
    | lAndExpr AND relExpr
    ;

relExpr
    : addExpr
    | relExpr (LT | GT | LE | GE | EQ | NE) addExpr
    ;

addExpr
    : mulExpr
    | addExpr (PLUS | MINUS) mulExpr
    ;

mulExpr
    : unaryExpr
    | mulExpr (STAR | SLASH | MOD) unaryExpr
    ;

unaryExpr
    : primaryExpr
    | (PLUS | MINUS | NOT) unaryExpr
    ;

primaryExpr
    : ID
    | NUMBER
    | LPAREN expr RPAREN
    | ID LPAREN argList? RPAREN
    ;

argList
    : expr (COMMA expr)*
    ;

// ========== 词法规则 ==========

CONST   : 'const';
INT     : 'int';
VOID    : 'void';
IF      : 'if';
ELSE    : 'else';
WHILE   : 'while';
BREAK   : 'break';
CONTINUE: 'continue';
RETURN  : 'return';

OR      : '||';
AND     : '&&';
EQ      : '==';
NE      : '!=';
LE      : '<=';
GE      : '>=';
LT      : '<';
GT      : '>';

ASSIGN  : '=';
PLUS    : '+';
MINUS   : '-';
STAR    : '*';
SLASH   : '/';
MOD     : '%';
NOT     : '!';

LPAREN  : '(';
RPAREN  : ')';
LBRACE  : '{';
RBRACE  : '}';
COMMA   : ',';
SEMI    : ';';

NUMBER
    : '-'? ('0' | [1-9][0-9]*)
    ;

ID
    : [_a-zA-Z] [_a-zA-Z0-9]*
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' (BLOCK_COMMENT | ~[*] | '*' ~[/])* '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
