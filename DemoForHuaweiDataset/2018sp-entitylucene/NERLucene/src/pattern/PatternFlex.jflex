package pattern;
import java_cup.runtime.*;
%%
%line
%column
%class PatternLexer
%cupsym PatternSymbol
%cup
%caseless
%public
%{
%}

LineTerminator 	=\r|\n|\r\n
WhiteSpace     	=({LineTerminator}|[\t\f]|" ")+
KEYWORD = ([^"^""#""!""?""("")""[""]""{""}"":""|"","" ""<"">"\r\n\t\f])+
ENTITY	= "#"{KEYWORD}

%%
<YYINITIAL>{
{WhiteSpace} 	{	}
":"				{	return new Symbol(PatternSymbol.COLON,yyline,yycolumn);	}
"("				{	return new Symbol(PatternSymbol.LPARAE,yyline,yycolumn);	}
")"				{	return new Symbol(PatternSymbol.RPARAE,yyline,yycolumn);	}
"["				{	return new Symbol(PatternSymbol.LBRACK,yyline,yycolumn);	}
"]"				{	return new Symbol(PatternSymbol.RBRACK,yyline,yycolumn);	}
"{"				{	return new Symbol(PatternSymbol.LBRACE,yyline,yycolumn);	}
"}"				{	return new Symbol(PatternSymbol.RBRACE,yyline,yycolumn);	}
"|"				{	return new Symbol(PatternSymbol.VERLINE,yyline,yycolumn);	}
"?"				{	return new Symbol(PatternSymbol.QUESTION,yyline,yycolumn);	}
","				{	return new Symbol(PatternSymbol.COMMA,yyline,yycolumn);	}
"<"				{	return new Symbol(PatternSymbol.LT,yyline,yycolumn);	}
">"				{	return new Symbol(PatternSymbol.RT,yyline,yycolumn);	}
"^"				{	return new Symbol(PatternSymbol.CARET,yyline,yycolumn);	}
{ENTITY}		{	return new Symbol(PatternSymbol.ENTITY,yyline,yycolumn,yytext().toLowerCase());	}
{KEYWORD}		{	return new Symbol(PatternSymbol.KEYWORD,yyline,yycolumn,yytext().toLowerCase());	}
}

