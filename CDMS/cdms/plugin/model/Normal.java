//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Normal.java
// Author: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.model;

import java.io.*;
import java.util.Random;

import cdms.core.*;

/** Gaussian model parameterised by the mean and standard deviation. */
public class Normal extends Value.Model
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2343289605203585345L;

public static final double logr2PI = 0.5 * Math.log(2.0 * Math.PI);

  public static final Type DATASPACE = Type.CONTINUOUS;

  public static final Type.Structured PARAMSPACE = 
    new Type.Structured(new Type[] { Type.CONTINUOUS, Type.CONTINUOUS },
                                     new String[] { "mean", "sd" });

  public static final Type.Structured SUFFICIENTSPACE = 
    new Type.Structured(new Type[] { Type.DISCRETE, Type.CONTINUOUS, Type.CONTINUOUS },
                        new String[] { "N", "SUM", "SUMSQR" });

  public static final Type.Model TT = 
    new Type.Model(DATASPACE,PARAMSPACE,Type.TRIV,SUFFICIENTSPACE); 

  public static final Normal normal = new Normal();

  /** The sufficient statistics for a sample from the Normal distribution. */
  public static class Sufficient extends Value.Structured implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5151127688640404126L;
	public int n;
    public double sum;
    public double sumsqr;

    public Sufficient(int n, double sum, double sumsqr)
    {
      super(SUFFICIENTSPACE);
      this.n = n;
      this.sum = sum;
      this.sumsqr = sumsqr;
    }

    public Sufficient(Value.Vector v)
    {
      super(SUFFICIENTSPACE);
      n = v.length();
      sum = 0;
      sumsqr = 0;
      for (int i = 0; i < n; i++)
      {
        double vi = v.doubleAt(i);
        sum += vi;
        sumsqr += vi * vi; 
      }
    }

    public Value cmpnt(int i)
    {
      switch(i)
      {
        case 0 : return new Value.Discrete(n);
        case 1 : return new Value.Continuous(sum);
        case 2 : return new Value.Continuous(sumsqr);
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
        case 2 : return sumsqr;
        default : throw new RuntimeException("Structured index out of range.");
      }
    }

    public int intCmpnt(int i)
    {
      if (i == 0) return n;
      throw new RuntimeException("Structured index out of range.");
    }
  }

  public static double kl(double tMean, double tSd, double iMean, double iSd)
  {
    double t1 = Math.log(iSd / tSd);
    double t2 = (1.0 / (2.0 * iSd * iSd)) * 
                (tMean - iMean) * (tMean - iMean);
    double t3 = - (1.0 / 2.0) * (1.0 - (tSd * tSd) / (iSd*iSd));
    return t1 + t2 + t3;
  }

  public Normal()
  {
    super(TT);
  }
  
  public Normal(Type.Model normalType)
  {
    super(normalType);
  }

    /** logP returns a probability density for this area. */
  public double logP(Value x, Value y, Value z)
  {
    double xval = ((Value.Scalar) x).getContinuous();
    double mean = ((Value.Structured) y).doubleCmpnt(0);
    double sd = ((Value.Structured) y).doubleCmpnt(1);
    return - logr2PI - Math.log(sd) - 
           (mean - xval) * (mean - xval) / (2.0 * sd * sd);
  }

  public Value.Vector generate(Random rand, int n, Value y, Value z)
  {
    double mean = ((Value.Structured) y).doubleCmpnt(0);
    double sd = ((Value.Structured) y).doubleCmpnt(1);
    return new NormalDataVector(mean,sd,rand,n);
  }

    /** This generate function must be instantiated or a crash occurs when z.length() == 0*/
    public Value.Vector generate(Random rand, Value y, Value.Vector z)
    {
	return generate( rand, z.length(), y, Value.TRIV );
    }

  public Value predict(Value y, Value z)
  {
    return ((Value.Structured) y).cmpnt(0);
  }

  public Value.Vector predict(Value y, Value.Vector z)
  {
    Value mean = ((Value.Structured) y).cmpnt(0);
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


    /**
     * This returns the probability density at a given point, and as such may be greater than 0.
     * To get the probability of a cetrain region the probability density must be integrated over.
     */
  public double logPSufficient(Value s, Value y)
  {
    int n = ((Value.Structured) s).intCmpnt(0);
    double sum = ((Value.Structured) s).doubleCmpnt(1);
    double sumsqr = ((Value.Structured) s).doubleCmpnt(2);
    double mean = ((Value.Structured) y).doubleCmpnt(0);
    double sd = ((Value.Structured) y).doubleCmpnt(1);
    double sumsqrdiff = sumsqr - (2 * mean * sum) + n * mean * mean;
       
    return - n * (logr2PI + Math.log(sd)) - sumsqrdiff / (2.0 * sd * sd);
  }

  public String toString()
  {
    return "Normal(x|mu,sigma)";
  }

  public static class NormalDataVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1220284657756013859L;

	public static final Type.Vector TT = new Type.Vector(DATASPACE);

    protected double data[];
    protected Value v[];

    public NormalDataVector(double mean, double sd, Random rand, int length)
    {
      super(TT);
      data = new double[length];
      v = new Value[length];
      for (int i = 0; i < length; i++)
        data[i] = rand.nextGaussian() * sd + mean;
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
