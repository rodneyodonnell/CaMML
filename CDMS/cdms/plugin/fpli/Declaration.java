//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// class Declaration the abstract syntax (parse tree) of [rec] x1=e1, x2=e2,...

package cdms.plugin.fpli;

public class Declaration implements java.io.Serializable
{ 
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2327687139846635269L;
public final String id;        
  public final Expression e;
  public final Declaration next; 
  public final boolean isRec;

  public Declaration(boolean isRec, String id, Expression e, Declaration next)
  { 
    this.isRec = isRec; 
    this.id = id; 
    this.e = e; 
    this.next = next; 
  }

  public void appendSB(StringBuffer sb) // for printing - efficiency!
  { 
    Declaration d = this;
    sb.append( isRec ? Lexical.Symbol[Lexical.recSy]+" " : "" );
    while( true )
    { 
      sb.append( d.id + " = " );   d.e.appendSB(sb);
      d = d.next;  
      if( d == null ) break;
      sb.append(",\n");
    }  
  }
}//Declaration class


// L. Allison, November 2000,
// School of Computer Science and Software Engineering,
// Monash University, Australia 3168
// see http://www.csse.monash.edu.au/~lloyd/tildeFP/Lambda/
