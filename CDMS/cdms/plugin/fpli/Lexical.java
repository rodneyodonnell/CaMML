//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.fpli;

import java.io.*;


// Lexical analyser, also see Syntax.java


public class Lexical implements java.io.Serializable
{ 
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4964447055206473654L;

InputStream inp;

  int sy = -1;                                      // lexical state variables
  char ch = ' ';  
  String theWord = "<?>";  
  int theInt = -1;
  double theDouble = -1;
  boolean eof = false;

  public static final int  // symbol codes...
   word            =  0, /* ident */ 
   numeral_integer =  1,
   numeral_double  =  2, 
   string          =  3,
   triv            =  4, /* () */
   nilSy           =  5,
   trueSy          =  6,
   falseSy         =  7,
   open            =  8,
   close           =  9,
   sqopen          = 10,
   sqclose         = 11,
   curlopen        = 12,
   curlclose       = 13,
   letSy           = 14,
   recSy           = 15,
   inSy            = 16,
   comma           = 17,
   colon           = 18,
   ifSy            = 19,
   thenSy          = 20,
   elseSy          = 21,
   lambdaSy        = 22,
   dot             = 23,
   consSy          = 24,
   orSy            = 25,
   andSy           = 26,
   eq              = 27,
   ne              = 28,
   lt              = 29,
   le              = 30,
   gt              = 31,
   ge              = 32,
   plus            = 33,
   minus           = 34,
   times           = 35,
   over            = 36,
   nullSy          = 37,
   hdSy            = 38,
   tlSy            = 39,
   notSy           = 40,
   eofSy           = 41;

  public static final String[] Symbol = new String[]
  { "<word>", "<integer>","<double>","<string>",
    "()", "nil", "true", "false",
    "(", ")", "[", "]", "{", "}",
    "let", "rec", "in", ",", ":",
    "if", "then", "else", "lambda", ".",
    ":", "or", "and",
    "=", "<>", "<", "<=", ">", ">=",
    "+", "-", "*", "/",
    "null", "hd", "tl", "not",
    "<eof>"
  };

  public Lexical(InputStream inp)                               // constructor
  { 
    this.inp = inp; 
    insymbol(); 
  }

  public int     sy()         { return sy; }           // define
  public boolean eoi()        { return sy == eofSy; }  // what a
  public String  theWord()    { return theWord; }      // Lexical
  public int     theInt()     { return theInt; }       // object is
  public double  theDouble()  { return theDouble; }       

  public void insymbol()                                           // insymbol
  // get the next symbol from the input stream
  { 
    if(sy == eofSy) return;
    do
    { 
      while( ch == ' ' && !eof ) getch(); // skip white space
      if( ch == '{' )             // skip comment
      { 
        do
        { 
          getch(); 
        } while( ch != '}' && ! eof );
        getch();
      }
    } while( (ch == ' ' || ch == '{') && !eof  );

    if( eof ) sy = eofSy;

    else if( Character.isLetter(ch) )               // words
    { 
      StringBuffer w = new StringBuffer();
      while( (Character.isLetterOrDigit(ch) || ch == '.' || ch == '_') && !eof)
      { 
        w.append(ch); 
        getch(); 
      }
      theWord = w.toString();  
      sy = word;
      for(int i = 0; i < eofSy; i++) // maybe speed up one day?
      { 
        if(Symbol[i].compareTo(theWord) == 0) 
        { 
          sy = i; 
          break; 
        } 
      }
    }

    else if( Character.isDigit(ch) )                // numbers
    { 
      StringBuffer w = new StringBuffer();
      while( Character.isDigit(ch) && !eof)
      { 
        w.append(ch);
        getch(); 
      }

      if (ch != '.')                        // int
      {
        theInt = Integer.parseInt(w.toString()); 
        sy = numeral_integer;
      }
      else                                  // double 
      {   
        w.append(ch);
        getch();
        while( Character.isDigit(ch) && !eof)
        { 
          w.append(ch);
          getch(); 
        }
        theDouble = Double.parseDouble(w.toString());
        sy = numeral_double;
      }
    }

    else if( ch == '\'' || ch == '"' )             // strings
    { 
      char start = ch;
      StringBuffer w = new StringBuffer();
      getch();
      while( start != ch && !eof)
      { 
        w.append(ch); 
        getch(); 
      }
      theWord = w.toString();  
      sy = string;
      getch();
    }

    else switch( ch )                               // special symbols
    { 
      case '<': getch();
        if( ch == '=' ) { getch(); sy = le; break; }
        if( ch == '>' ) { getch(); sy = ne; break; }
        sy = lt; 
        break;
      case '>': getch();
        if( ch == '=' ) { getch(); sy = ge; break; }
        sy = gt; 
        break;
      case '(': getch();
        if( ch == ')' ) { getch(); sy = triv; break; }
        sy = open; 
        break;
      case ':': getch();
	sy = consSy; 
        break;

      case '+': case '-': case '*': case '/': case '=':
      case ')': case '.': case ',':
      case '[': case ']': case '{': case '}': 
        int ch2 = ch; getch();
        switch( ch2 )
        { 
          case '+': sy = plus;    break;  
          case '-': sy = minus;   break;
	  case '*': sy = times;   break;  
          case '/': sy = over;    break;
	  case '=': sy = eq;      break;  
          case ')': sy = close;   break;
          case '.': sy = dot;     break;  
          case ',': sy = comma;   break;
          case '[': sy = sqopen;  break;  
          case ']': sy = sqclose; break;
          case '{': sy =curlopen; break;  
          case '}': sy =curlclose;break;
          default: error("should not be here!"); break;
        }
        break;

      default: error("bad symbol");
    }
  }

  // Changes variable ch as a side-effect.
  void getch()                                                        // getch
  { 
    byte[] buffer = new byte[1];  

    ch = '.';
    if( sy == eofSy ) return;
    try 
    { 
      int n = 0;
      if( inp.available() > 0 ) n = inp.read(buffer);
      if( n <= 0 ) eof = true; else ch = (char)buffer[0];
    }
    catch(Exception e){ System.out.println(e.toString() + "caught in getch()"); }
    //if(ch == '\n' || ch == '\t') ch = ' ';
    if (Character.isWhitespace(ch)) { ch = ' '; }
  }

  void skipRest()
  { 
    if( ! eof ) System.out.print("skipping to end of input...");
    int n = 0;
    while( ! eof )
    { 
      if( n%80 == 0 ) System.out.println(); // break line
      System.out.print(ch);  
      n++;  
      getch();
    }
    System.out.println();
  }

  public void error(String msg)                                       // error
  { 
    throw new RuntimeException("\nError: " + msg +
                       " sy=" + sy + " ch=" + ch +
                       " theWord=" + theWord + " theInt=" + theInt);
  }


  // the following main() allows Lexical to be tested in isolation
  public static void  main(String[] argv)
  { 
    System.out.println("--- FP ---");
    for(int i=0; i < argv.length; i++) // command line params if any
      System.out.print("argv[" + i + "]=" + argv[i] + "\n");

    Lexical lex = new Lexical(System.in);
    while( ! lex.eoi() )
    { 
      int sy = lex.sy();
      System.out.print(sy + "(" + Symbol[sy] + ") : ");
      if(sy == Lexical.word)    System.out.print(lex.theWord());
      if(sy == Lexical.numeral_integer) System.out.print(lex.theInt());
      if(sy == Lexical.numeral_double) System.out.print(lex.theDouble());

      System.out.println("");
      lex.insymbol();
    }

    System.out.println("--- end ---");
  }

}


