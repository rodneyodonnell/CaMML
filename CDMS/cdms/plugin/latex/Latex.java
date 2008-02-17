//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Latex.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.latex;

import cdms.core.*;

/** Latex module. */ 
public class Latex extends Module.StaticFunctionModule
{
  public Latex()
  {
    super("Latex",Module.createStandardURL(Latex.class),Latex.class);
  }
 
  /** <code>[(...)] -> Str</code> Creates a Latex tabular from a vector of structured data. */
  public static Value.Function tabular = new Tabular();

  /** <code>[(...)] -> Str</code> 
      <p>
      Creates a Latex tabular from a vector of structured data.
  */
  public static class Tabular extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3158243596054761557L;

	public static final String CR = System.getProperty("line.separator");

    public static final Type.Function TT = 
      new Type.Function(new Type.Vector(Type.STRUCTURED),Type.STRING);

    public Tabular()
    {
      super(TT);
    }

    public Value apply(Value param)
    {
      Value.Vector vv = (Value.Vector) param;
      Type.Structured eltt = (Type.Structured) ((Type.Vector) vv.t).elt;

      StringBuffer res = new StringBuffer();
      res.append("\\begin{table}[htp] " + CR + "\\center" + CR + "\\caption{}" + CR);
      res.append("\\begin{tabular}{|");
      // All columns centered for now.
      for (int j = 0; j < eltt.cmpnts.length; j++) res.append("c");
      res.append("|} \\hline" + CR);
      for (int i = 0; i < vv.length(); i++)
      {
        Value.Structured elti = (Value.Structured) vv.elt(i);
        for (int j = 0; j < elti.length(); j++)
        {
          Value cmpntj = elti.cmpnt(j);
          if (cmpntj instanceof Value.Str)
            res.append(((Value.Str) cmpntj).getString());
          else res.append(cmpntj.toString());
          if (j != elti.length() - 1)
          {
            res.append("&"); 
          }
          else 
          {
            if (i != vv.length() - 1) res.append("\\tabularnewline" + CR);
              else res.append("\\tabularnewline \\hline" + CR);
          }
        }      
      }
      res.append("\\end{tabular}" + CR + "\\end{table}");

      return new Value.Str(res.toString());
    }
  } 

} 
