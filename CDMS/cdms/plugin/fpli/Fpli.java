//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.fpli;

import cdms.core.*;
import java.io.*;

/** Lambda Script module. */
public class Fpli extends Module.StaticFunctionModule
{
  public Fpli()
  {
    super("Fpli",Module.createStandardURL(Fpli.class),Fpli.class);
  }

  public static Type.Function interpreterType = new Type.Function(Type.STRING,Type.TYPE,false,false);

  /** <code>String -> t</code> Interpreters the parameters using the Lambda Script interpreter. */
  public static Value.Function interpreter = new Interpreter();

  /** <code>String -> t</code> 
      <p>
      Interpreters the parameters using the Lambda Script interpreter. 
  */
  public static class Interpreter extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6021816807864449597L;

	public Interpreter()
    {
      super(interpreterType);
    }

    public Value apply(Value v)
    {
      try
      {
        Syntax syn = new Syntax( new Lexical( 
                         new ByteArrayInputStream(((Value.Str) v).getString().getBytes()) ) 
                     );
        Expression e = syn.exp();    // parse the Expression
        return e.eval(new Environment(new Environment(null)));  // Don't use boot environment.
      }
      catch (Exception e)
      {
        e.printStackTrace();
        return new Value.Str(e.toString());
      }     
    }
  }

  /** <code>String -> t</code> Performs syntax and type checking on the parameter. */
  public static Value.Function checker = new Checker();

  /** <code>String -> t</code> 
      <p>
      Performs syntax and type checking on the parameter.
  */
  public static class Checker extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8080828818346060618L;

	public Checker()
    {
      super(interpreterType);
    }

    public Value apply(Value v)
    {
      return new Value.Str("The syntax and type checker does not exist.");
    }
  }


}
