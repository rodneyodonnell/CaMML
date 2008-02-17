//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Normal.java
// Author: leighf@csse.monash.edu.au

package cdms.plugin.ml;

import cdms.core.*;

/**
  Maximum likelihood estimator the Normal distribution.
*/
public class Normal
{
  
  /** <code>(n,sum,sumsqr) -> (mu,sigma)</code> Maximum likelihood Normal estimator function. */
  public static final NormalEstimator normalEstimator = new NormalEstimator();

  /** <code>(n,sum,sumsqr) -> (mu,sigma)</code> 
      <p>
      Maximum likelihood Normal estimator function.
  */
  public static class NormalEstimator extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4341049189463539775L;
	public static final Type.Function TT = 
      new Type.Function(cdms.plugin.model.Normal.SUFFICIENTSPACE,
                        cdms.plugin.model.Normal.PARAMSPACE);

    public NormalEstimator()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      int n = sv.intCmpnt(0);
      double sum = sv.doubleCmpnt(1);
      double sumsqr = sv.doubleCmpnt(2);
      double sumsqrdiff = sumsqr - (sum * sum) / (double) n;
      double sd = sumsqrdiff > 0 ? Math.sqrt(sumsqrdiff / (double) n) : 0;
      return new NormalParams(sum / (double) n, sd);
    }

    public class NormalParams extends Value.Structured
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 327164460918785954L;
	private double mean, sd;

      public NormalParams(double mean, double sd)
      {
        super(cdms.plugin.model.Normal.PARAMSPACE);
        this.mean = mean;
        this.sd = sd;
      }

      public int length() 
      {
        return 2;
      }

      public double doubleCmpnt(int idx)
      {
        if (idx == 0) return mean; else return sd;
      }

      public Value cmpnt(int idx)
      {
        return new Value.Continuous(doubleCmpnt(idx));
      }
    }
  }

}
