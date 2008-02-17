//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Gamma.java
// Author: leighf@csse.monash.edu.au

package cdms.plugin.model;

import java.io.*;
import java.util.Random;

import cdms.core.*;

/** Gamma model parameterised by alpha and beta. */
public class Gamma extends Value.Model
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2503750835700532589L;

/** Dataspace is (0,inf). */
  public static final Type DATASPACE = new Type.Continuous(0 + Double.MIN_VALUE,Double.MAX_VALUE,true,false);

  public static final Type.Structured PARAMSPACE = 
    new Type.Structured(new Type[] { Type.CONTINUOUS, Type.CONTINUOUS },
                                     new String[] { "alpha", "beta" });

  public static final Type.Structured SUFFICIENTSPACE = 
    new Type.Structured(new Type[] { Type.DISCRETE, Type.CONTINUOUS, Type.CONTINUOUS },
                        new String[] { "N", "LOGPRODUCT", "SUM" });

  public static final Type.Model TT = 
    new Type.Model(DATASPACE,PARAMSPACE,Type.TRIV,SUFFICIENTSPACE); 

  public static final Gamma gamma = new Gamma();

  /** The sufficient statistics for a sample from the Gamma distribution. */
  public static class Sufficient extends Value.Structured implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2187585606409773090L;
	public int n;
    public double sum, logprod;

    public Sufficient(int n, double sum, double logprod)
    {
      super(SUFFICIENTSPACE);
      this.n = n;
      this.sum = sum;
      this.logprod = logprod;
    }

    public Sufficient(Value.Vector v)
    {
      super(SUFFICIENTSPACE);
      n = v.length();
      sum = 0;
      logprod = 0;
      for (int i = 0; i < n; i++)
      {
        double vi = v.doubleAt(i);
        sum += vi;
        logprod += Math.log(vi);
      }
    }

    public Value cmpnt(int i)
    {
      switch(i)
      {
        case 0 : return new Value.Discrete(n);
        case 1 : return new Value.Continuous(sum);
        case 2 : return new Value.Continuous(logprod);
        default : throw new RuntimeException("Structured index out of range.");
      }
    }

    public int length()
    {
      return 3;
    }

    public double doubleCmpnt(int i)
    {
      switch(i)
      {
        case 0 : return n;
        case 1 : return sum;
        case 2 : return logprod;
        default : throw new RuntimeException("Structured index out of range.");
      }
    }

    public int intCmpnt(int i)
    {
      if (i == 0) return n;
      throw new RuntimeException("Structured index out of range.");
    }
  }

  public Gamma()
  {
    super(TT);
  }

  public static double logGamma(double x)
  {
    double coef[] = { 76.18009173, -86.50532033, 24.01409822, -1.231739516, 0.00120858003, -0.00000536382 };
    double step = 2.50662827465;
    double fpf = 5.5;
    double t, tmp, ser;
    t = x - 1;
    tmp = t + fpf;
    tmp = (t + 0.5) * Math.log(tmp) - tmp;
    ser = 1;
    for (int i = 1; i <= 6; i++)
    {
      t = t + 1;
      ser = ser + coef[i - 1] / t;
    }
    return tmp + Math.log(step * ser);
  }

  public double logP(Value x, Value y, Value z)
  {
    double xval = ((Value.Scalar) x).getContinuous();
    double alpha = ((Value.Structured) y).doubleCmpnt(0);
    double beta = ((Value.Structured) y).doubleCmpnt(1);
    return alpha * Math.log(beta) - logGamma(alpha) + (alpha - 1) * Math.log(xval) - beta * xval;
  }

  public Value.Vector generate(Random rand, int n, Value y, Value z)
  {
    double alpha = ((Value.Structured) y).doubleCmpnt(0);
    double beta = ((Value.Structured) y).doubleCmpnt(1);
    return new GammaDataVector(alpha,beta,rand,n);
  }

  /** Returns the mean of a gamma: alpha/beta. */
  public Value predict(Value y, Value z)
  {
    return new Value.Continuous(((Value.Structured) y).doubleCmpnt(0) / ((Value.Structured) y).doubleCmpnt(1));
  }

  public Value.Vector predict(Value y, Value.Vector z)
  {
    Value mean = new Value.Continuous(((Value.Structured) y).doubleCmpnt(0) / ((Value.Structured) y).doubleCmpnt(1));
    if (z == null) return VectorFN.emptyVector;
      else return new VectorFN.ConstantVector(z.length(),mean);
  }

  public double logP(Value.Vector x, Value y, Value.Vector z)
  {
    return logPSufficient(new Sufficient(x),y);
  }

  public Value getSufficient(Value.Vector x, Value.Vector z)
  {
    return new Sufficient(x);
  }

  public double logPSufficient(Value s, Value y)
  {
    int n = ((Value.Structured) s).intCmpnt(0);
    double sum = ((Value.Structured) s).doubleCmpnt(1);
    double logprod = ((Value.Structured) s).doubleCmpnt(2);
    double alpha = ((Value.Structured) y).doubleCmpnt(0);
    double beta = ((Value.Structured) y).doubleCmpnt(1);
    return n * (alpha * Math.log(beta) - logGamma(alpha)) + (alpha - 1) * logprod - beta * sum;
  }

  public String toString()
  {
    return "Gamma(x|alpha,beta)";
  }


  /** GS Algorith (Ahrens and Dieter (1974)), based on acceptance-rejection
      technique.  See Simulation Modeling and Analysis 1991 pg. 488.
  */
  public static double genGamma(Random r, double alpha, double beta)
  {
    if (alpha < 1)
    {
      double b = (Math.E + alpha) / Math.E;

      while (true)
      {
        double u1 = r.nextDouble();
        double p = b * u1;
        if (p > 1)
        {
          double y = - Math.log((b - p) / alpha);
          double u2 = r.nextDouble();
          if (u2 <= Math.pow(y,alpha - 1)) return y/beta;
        }
        else
        {
          double y = Math.pow(p,1.0 / alpha);
          double u2 = r.nextDouble(); 
          if (u2 <= Math.exp(- y)) return y/beta;
        }
      }
    }
    else
    {
      double a = 1.0 / Math.sqrt(2.0 * alpha - 1.0);
      double b = alpha - Math.log(4);
      double q = alpha + 1.0 / a;
      double theta = 4.5;
      double d = 1 + Math.log(theta);

      while (true)
      {
        double u1 = r.nextDouble();
        double u2 = r.nextDouble();

        double V = a * Math.log(u1 / (1.0 - u1));
        double Y = alpha * Math.exp(V);
        double Z = u1 * u1 * u2;
        double W = b + q*V - Y;

        if (W + d - theta * Z >= 0) return Y/beta;
        if (W >= Math.log(Z)) return Y/beta;
      }
    }
  }

  public static class GammaDataVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6584112548643608680L;

	public static final Type.Vector TT = new Type.Vector(DATASPACE);

    protected double data[];
    protected Value v[];

    public GammaDataVector(double alpha, double beta, Random rand, int length)
    {
      super(TT);
      data = new double[length];
      v = new Value[length];
      for (int i = 0; i < length; i++)
        data[i] = genGamma(rand,alpha,beta);
    }

    public double doubleAt(int i)
    {
      return data[i];
    }

    public int length() 
    { 
      return data.length; 
    }

    public Value elt(int i) 
    {
      if (v[i] == null)
        v[i] = new Value.Continuous(data[i]);
      return v[i];
    }
  }

}
