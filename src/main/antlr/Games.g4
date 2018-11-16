grammar Games;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines
POSINT: [0-9]+ ;
INT : ([-+])?[0-9]+ ;
LEDUC : 'LeducPoker' ;

game :  leducPoker
    | iiGoofspiel
    | rps
    | kriegTTT
    | perfRecall
    ;

leducPoker : 'LeducPoker' '{' POSINT ',' POSINT '}' ;
iiGoofspiel : 'IIGoofspiel' '{' POSINT '}' ;
rps : 'RockPaperScissors' '{' POSINT '}' ;
kriegTTT : 'KriegTicTacToe' ;
perfRecall : 'PerfectRecall' '{' game '}' ;