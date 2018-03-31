/**
 * Pattern matcher for phone numbers and
 * email addresses using JFlex
 * @author aaulabaugh@gmail.com
 */
package ner.annotation.extraction;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;

%%
%public
%final
%class PhoneEmailExtractorImpl
%unicode
%char
%caseless
%line
%column
%function getNextToken
%type EntityAnnotation

Whitespace = [ \n\t]+
WORD = [A-Za-z0-9]("-"|"_"|[A-Za-z0-9])*
DIGIT = [0-9]
EMAIL = {WORD}("@"|" at "|"[at]"){WORD}(("."|" dot "){WORD})+
PHONE = ("("?{DIGIT}{DIGIT}{DIGIT}")"?)?{DIGIT}{DIGIT}{DIGIT}("-")?{DIGIT}{DIGIT}{DIGIT}{DIGIT}

%{

  EntityCatalog catalog;
  
  /** Defines which entities to use */
  public final void setCatalog(EntityCatalog c)
  {
    catalog = c;
  }

  /** Character count processed so far */
  public final int yychar()
  {
    return yychar;
  }
  
  /**
   * Sets the scanner buffer size in chars
   *
   public final void setBufferSize(int numChars) {
     ZZ_BUFFERSIZE = numChars;
     char[] newZzBuffer = new char[ZZ_BUFFERSIZE];
     System.arraycopy(zzBuffer, 0, newZzBuffer, 0, Math.min(zzBuffer.length, ZZ_BUFFERSIZE));
     zzBuffer = newZzBuffer;
   }
   */
%}

%%
{EMAIL}
  {
  	EntityAnnotation emailAnnotation = new EntityAnnotation();
  	emailAnnotation.setContent(yytext());
  	emailAnnotation.setPosition(yychar);
  	emailAnnotation.setSource("JFLEX");
  	emailAnnotation.addType(catalog.getEntityType("EMAIL"));
  	//System.out.println("Found EMAIL: " + yytext() + " at " + yychar);
  	return emailAnnotation;
  }
{PHONE}
  {
    EntityAnnotation phoneAnnotation = new EntityAnnotation();
  	phoneAnnotation.setContent(yytext());
  	phoneAnnotation.setPosition(yychar);
  	phoneAnnotation.setSource("JFLEX");
  	phoneAnnotation.addType(catalog.getEntityType("EMAIL"));
  	//System.out.println("Found PHONE: " + yytext() + " at " + yychar);
  	return phoneAnnotation;
  }
{Whitespace}
  {/*do nothing*/}
.
  {/*do nothing*/}
/* error fallback */
[^]
  { /*do nothing*/ }
