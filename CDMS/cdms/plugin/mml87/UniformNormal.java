//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: UniformNormal.java
// Author: leighf@csse.monash.edu.au

package cdms.plugin.mml87;

import cdms.core.*;
import cdms.plugin.model.Normal;
import cdms.plugin.model.WeightedNormal;

/**
  MML87 estimator and coster for the Normal distribution with a uniform prior on mu and log(sigma).
*/
public class UniformNormal
{
  /** <code>prec -> (n,sum,sumsqr) -> (mu,sigma)</code> MML87 Normal estimator function
      using a uniform prior on mu and log(sigma).
  */
  public static final NormalEstimator normalEstimator = new NormalEstimator();

  /** <code>prec -> (n,sum,sumsqr) -> (mu,sigma)</code> 
      <p>
      MML87 Normal estimator function using a uniform prior on mu and log(sigma).
  */
  public static class NormalEstimator extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1889659458643870067L;
	public static final Type.Function TT = new Type.Function(Type.CONTINUOUS,NormalEstimator2.TT);

    public NormalEstimator()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new NormalEstimator2(((Value.Continuous) v).getContinuous());
    }
  }
 
  /** <code>(n,sum,sumsqr) -> (mu,sigma)</code> 
      <p>
      @see UniformNormal.NormalEstimator
  */
  public static class NormalEstimator2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 900687552308663320L;

	public static final Type.Function TT = new Type.Function(Normal.SUFFICIENTSPACE,Normal.PARAMSPACE);

    private double prec;

    public NormalEstimator2(double prec)
    {
      super(TT);
      this.prec = prec;
    }

    public Value apply(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      int n = sv.intCmpnt(0);
      double sum = sv.doubleCmpnt(1);
      double sumsqr = sv.doubleCmpnt(2);
      double sumsqrdiff = sumsqr - (sum * sum) / (double) n;

      double var;

      if (n > 1) 
      {
        var = sumsqrdiff / (double) (n - 1.0);
        if (var < prec*prec) var = prec*prec;
      }
      else 
      {
        var = prec*prec;
      }

      return new NormalParams(sum / (double) n, Math.sqrt(var));
    }

    public static class NormalParams extends Value.Structured
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -285595078975997556L;
	private double mean, sd;

      public NormalParams(double mean, double sd)
      {
        super(Normal.PARAMSPACE);
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
  
  /** <code>prec -> (n,sum,sumsqr) -> (mu,sigma)</code> MML87 Normal estimator function
      using a uniform prior on mu and log(sigma).
  */
  public static final WeightedNormalEstimator weightedNormalEstimator = new WeightedNormalEstimator();

  /** <code>prec -> (n,sum,sumsqr) -> (mu,sigma)</code> 
      <p>
      MML87 Normal estimator function using a uniform prior on mu and log(sigma).
  */
  public static class WeightedNormalEstimator extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5236263180073284423L;
	public static final Type.Function TT = new Type.Function(Type.CONTINUOUS,WeightedNormalEstimator2.TT);

    public WeightedNormalEstimator()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new WeightedNormalEstimator2(((Value.Continuous) v).getContinuous());
    }
  }
 
  /** <code>(n,sum,sumsqr) -> (mu,sigma)</code> 
      <p>
      @see UniformNormal.NormalEstimator
  */
  public static class WeightedNormalEstimator2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7281709396418050782L;

	public static final Type.Function TT = new Type.Function(WeightedNormal.SUFFICIENTSPACE,Normal.PARAMSPACE);

    private double prec;

    public WeightedNormalEstimator2(double prec)
    {
      super(TT);
      this.prec = prec;
    }

    public Value apply(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      double n = sv.doubleCmpnt(0);
      double sum = sv.doubleCmpnt(1);
      double sumsqr = sv.doubleCmpnt(2);
      double sumsqrdiff = sumsqr - (sum * sum) / n;
      double mean;
      
      double var;

      if (n > 1) 
      {
        var = sumsqrdiff / (n - 1.0);
        if (var < prec*prec) var = prec*prec;
        mean = sum/n;
      }
      else 
      {
        var = prec*prec;
        if(n > 0)
          mean = sum;
        else
          mean = 0;
      }
      return new NormalEstimator2.NormalParams(mean, Math.sqrt(var));
    }
  }

  /** <code>prec -> (muRange, sigmaRange) -> (n, sum, sumSqr) -> (Mu, Sigma) -> Double</code> */
  public static final ArbitraryNormalCoster arbitraryNormalCoster = new ArbitraryNormalCoster(false);

  /** <code>prec -> (muRange, sigmaRange) -> (n, sum, sumSqr) -> (Mu, Sigma) -> Double</code> */
  public static final ArbitraryNormalCoster arbitraryNormalCosterHypothesisOnly = new ArbitraryNormalCoster(true);
  
  public static class ArbitraryNormalCoster extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7021702823895942987L;

	public static final Type.Function TT = new Type.Function(Type.CONTINUOUS, ArbitraryNormalCoster2.TT, false, false);

    private boolean hOnly;
  
    public ArbitraryNormalCoster(boolean hOnly)
    {
      super(TT);
      this.hOnly = hOnly;
    }
    
    public Value apply(Value v)
    {
      return new ArbitraryNormalCoster2(((Value.Scalar)v).getContinuous(), hOnly);
    }
  }

  /** <code>(muRange, sigmaRange) -> (n, sum, sumSqr) -> (Mu, Sigma) -> Double</code> */
  public static class ArbitraryNormalCoster2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 324066827490738293L;
	private double prec;
    private boolean hOnly; 
    public static final Type.Function TT = new Type.Function(new Type.Structured(new Type[]{Type.CONTINUOUS, Type.CONTINUOUS}, 
                                                                                 new String[]{"Mu range", "Sigma range"},
                                                                                 new boolean[]{false, false}), 
                                                             ArbitraryNormalCoster3.TT, false, false);
  
    public ArbitraryNormalCoster2(double prec, boolean hOnly)
    {
      super(TT);
      this.prec = prec;
      this.hOnly = hOnly;
    }
    
    public Value apply(Value v)
    {
      return new ArbitraryNormalCoster3(prec, ((Value.Scalar)((Value.Structured)v).cmpnt(0)).getContinuous(), ((Value.Scalar)((Value.Structured)v).cmpnt(1)).getContinuous(), hOnly);
    }
  }

  /** <code>(n, sum, sumSqr) -> (Mu, Sigma) -> Double</code> */
  public static class ArbitraryNormalCoster3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6080403719904144136L;
	private double prec, muRange, sigmaRange;
    private boolean hOnly; 
    public static final Type.Function TT = new Type.Function(WeightedNormal.SUFFICIENTSPACE, ArbitraryNormalCoster4.TT, false, false);
  
    public ArbitraryNormalCoster3(double prec, double muRange, double sigmaRange, boolean hOnly)
    {
      super(TT);
      this.prec = prec;
      this.muRange = muRange;
      this.sigmaRange = sigmaRange;
      this.hOnly = hOnly;
    }
    
    public Value apply(Value v)
    {
      return new ArbitraryNormalCoster4(prec, muRange, sigmaRange, (Value.Structured)v, hOnly);
    }
  }

  /** <code>(Mu, Sigma) -> Double</code> */
  public static class ArbitraryNormalCoster4 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1566574184910467443L;
	private double prec, muRange, sigmaRange;
    Value.Structured sufficient;
    private boolean hOnly;
  
    public static final Type.Function TT = new Type.Function(Normal.PARAMSPACE, Type.CONTINUOUS, false, false);
    public static final double lcsqr = Math.pow(5.0 / (36.0 * Math.sqrt(3.0)) ,2);

    public ArbitraryNormalCoster4(double prec, double muRange, double sigmaRange, Value.Structured sufficient, boolean hOnly)
    {
      super(TT);
      this.prec = prec;
      this.muRange = muRange;
      this.sigmaRange = sigmaRange;
      this.sufficient = sufficient;
      this.hOnly = hOnly;
    }
    
    public Value apply(Value v)
    {
      Value.Structured params = (Value.Structured)v;
      double inferredMu = ((Value.Scalar)params.cmpnt(0)).getContinuous();
      double inferredSd = ((Value.Scalar)params.cmpnt(1)).getContinuous();
      double muSigmaRange = muRange * sigmaRange;
      double n = sufficient.doubleCmpnt(0);
      double sum = sufficient.doubleCmpnt(1);
      double sumsqr = sufficient.doubleCmpnt(2);
      double ssd = sumsqr - (2 * inferredMu * sum) + (n * inferredMu * inferredMu);
      double mlen;
      if (n > 1)
      {
        double var = inferredSd * inferredSd;
        double f = (2.0 * n * n) / (var * var);
        double h = 1.0 / (inferredSd * muSigmaRange);
        double ll;
        if(hOnly) ll = 0;
        else ll  = - n * Math.log(inferredSd) - ssd / (2.0 * inferredSd * inferredSd) - (n / 2.0) * Math.log(2.0 * Math.PI) + n * Math.log(prec);
        mlen = 0.5 * Math.log(1 + (f*lcsqr) / (h*h)) - ll + 1.0;
      }
      else
      {
        mlen = Math.log(muRange) - Math.log(prec);    // Encoded using the uniform prior on mu.
      }

      return new Value.Continuous(mlen);
    }
  }

  /** <code>prec -> (Double,Double) -> (n,sum,sumsqr) -> (mu,sigma)</code> MML87 Normal 
      coster function using a uniform prior on mu and log(sigma).
  */
  public static final NormalCoster normalCoster = new NormalCoster();

  /** <code>prec -> (muRange,sigmaRange) -> (n,sum,sumsqr) -> (mu,sigma)</code>
      <p>
      MML87 Normal coster function using a uniform prior on mu and log(sigma).
  */
  public static class NormalCoster extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -66919325155621351L;
	public static final Type.Function TT = new Type.Function(Type.CONTINUOUS,NormalCoster2.TT);

    public NormalCoster()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new NormalCoster2(((Value.Continuous) v).getContinuous());
    }
  }
 
  /** <code>(muRange,sigmaRange) -> (n,sum,sumsqr) -> (mu,sigma)</code>
      <p>
      @see UniformNormal.NormalCoster
  */
  public static class NormalCoster2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6508252808664169547L;

	public static final Type.Function TT = new Type.Function(
      new Type.Structured(new Type[] { Type.CONTINUOUS, Type.CONTINUOUS },
                          new String[] { "MuRange", "SigmaRange" }),NormalCoster3.TT);

    private double prec;

    public NormalCoster2(double prec)
    {
      super(TT);
      this.prec = prec;
    }

    public Value apply(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      return new NormalCoster3(prec,sv.doubleCmpnt(0),sv.doubleCmpnt(1));
    }
  }

  /** <code>(n,sum,sumsqr) -> (mu,sigma)</code>
      <p>
      @see UniformNormal.NormalCoster
  */
  public static class NormalCoster3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6433275100085249724L;

	public static final Type.Function TT = new Type.Function(Normal.SUFFICIENTSPACE,Type.CONTINUOUS);

    private double muSigmaRange;
    private double muRange;
    private double prec;

    public NormalCoster3(double prec, double muRange, double sigmaRange)
    {
      super(TT);
      this.prec = prec;
      this.muRange = muRange;
      muSigmaRange = muRange * sigmaRange;
    }

    public static final double lcsqr = Math.pow(5.0 / (36.0 * Math.sqrt(3.0)) ,2);

    public double applyDouble(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      //      double n = sv.intCmpnt(0);
      double n = sv.doubleCmpnt(0);
      double sum = sv.doubleCmpnt(1);
      double sumsqr = sv.doubleCmpnt(2);
      double ssd = sumsqr - ((sum*sum) / (double) n);

      if (ssd <= 0) ssd = prec*prec;

      double mlen;

      if (n > 1)
      {
        double var = ssd / (double) (n - 1.0);
        if (var < prec*prec) var = prec*prec;
        double sd = Math.sqrt(var);

        double f = (2.0 * n * n) / (var * var);
        double h = 1.0 / (sd * muSigmaRange);
        double ll = - n * Math.log(sd) - ssd / (2.0 * sd * sd) - (n / 2.0) * Math.log(2.0 * Math.PI) + n * Math.log(prec);
        mlen = 0.5 * Math.log(1 + (f*lcsqr) / (h*h)) - ll + 1.0;
      }
      else
      {
        mlen = Math.log(muRange) - Math.log(prec);    // Encoded using the uniform prior on mu.
      }

      if (Double.isNaN(mlen))
      {
        System.out.println("Error in NormalCoster: message length is NaN.  " + 
                           "n=" + n + " sum=" + sum + " sumsqr=" + sumsqr + " ssd=" + ssd +
                           " muSigmaRange=" + muSigmaRange);
      }

      return mlen;
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }
  
}
