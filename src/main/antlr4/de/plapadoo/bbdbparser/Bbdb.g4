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

STRING : '"' SCharSequence? '"'  {setText(getText().substring(1,getText().length()-1));} ;

Whitespace
    :   [ \t]+
        -> skip
    ;

Integer : [0-9]+ ;

LispIdentifier : [a-zA-Z-]+ ;

lispIdentifier : LispIdentifier ;

nil : 'nil' ;

integer : Integer ;

string : STRING ;

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
