//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.fpli;


public class FP
{
  public static void main(String[] argv)
  { 
    System.out.println("Lambda Calculus Interpreter based on L.Allison Example");

    Syntax syn = new Syntax( new Lexical(System.in) );
    Expression e = syn.exp();                          // parse an Expression
    e.eval(new Environment(null));
  }
}

