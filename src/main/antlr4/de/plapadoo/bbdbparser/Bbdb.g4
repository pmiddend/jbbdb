grammar Bbdb;

fragment
SCharSequence
    :   SChar+
    ;

fragment
SChar
    :   ~["\\\r\n]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   '\\' ['"?abfnrtv\\]
    ;

String : '"' SCharSequence? '"' ;

Whitespace
    :   [ \t]+
        -> skip
    ;

Integer : [0-9]+ ;

LispIdentifier : [a-zA-Z-]+ ;

lispIdentifier : LispIdentifier ;

nil : 'nil' ;

integer : Integer ;

string : String ;

vector : '[' object* ']' ;

alistEntry : '(' lispIdentifier '.' object ')' ;

alist : '(' alistEntry+ ')' ;

list : '(' object* ')' ;

object : alist
       | list
       | vector
       | string
       | integer
       | nil ;

bbdbList : vector ('\n' vector)* ;
