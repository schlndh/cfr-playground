grammar ConfigKey;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines
INT : ([-+])?[0-9]+ ;
FLOAT :  INT ('.' [0-9]+)? ([Ee] ([-+])? [0-9]+)? ;
BOOL : 'true' | 'false' ;
NULL : 'null' ;
ID : [_a-zA-Z] [-+_a-zA-Z0-9]* ;

objectName : ID ;

expr : configKey
    | NULL
    | FLOAT
    | INT
    | STRING
    | BOOL
    ;

posParam : expr ;
posParams : posParam (',' posParams)? ;

kvKey : ID ;
kvVal : expr;
kvParam : kvKey '=' kvVal ;
kvParams: kvParam ','?
    | kvParam ',' kvParams
    ;

paramList : posParams
    | kvParams
    | posParams ',' kvParams
    ;

params : ('{' paramList? '}')? ;

configKey : objectName params ;

// Taken from https://github.com/antlr/grammars-v4/blob/master/json/JSON.g4 and simplified
STRING
   : '"' (ESC | SAFECODEPOINT)* '"'
   ;


fragment ESC
   : '\\' ["\\bfnrt]
   ;

fragment SAFECODEPOINT
   : ~ ["\\\u0000-\u001F]
   ;
