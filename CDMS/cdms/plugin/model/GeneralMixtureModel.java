//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.model;

import java.util.Random;

import cdms.core.*;

/** A mixture model parameterised by a set of models and weight. */
public class GeneralMixtureModel extends Value.Model
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2583970190852524864L;

public GeneralMixtureModel()
  {
    super(makeModelType());
  }

  private static Type.Model makeModelType()
  {
    Type dataSpace = new Type.Variable();
    Type sharedSpace = new Type.Variable();
    Type modelParamType = new Type.Model(dataSpace, Type.TYPE, sharedSpace, Type.TYPE);
    Type structuredType = new Type.Structured(new Type[]{modelParamType, Type.CONTINUOUS, Type.TYPE}, 
                                              new String[]{"Model","Weight","Parameters"}, 
                                              new boolean[]{false, false, false});
    Type paramSpace = new Type.Vector(Type.TYPE, structuredType, false, false, false, false);
    return new Type.Model(dataSpace, paramSpace, sharedSpace, Type.STRUCTURED);
  }

  private double[] normaliseWeights(Value.Vector y)
  {
    int count;
    double sum = 0;
    double[] result = new double[y.length()];
    for(count = 0; count < result.length; count++)
    {
      result[count] = ((Value.Structured)y.elt(count)).doubleCmpnt(1);
      sum += result[count];
    }
    for(count = 0; count < result.length; count++)
    {
      result[count] /= sum;
    }
    return result;
  }

  // logP(X|Y,Z)
  /** Get the probability of x|(y,z) for each model, multimplied by that model's weight.
      Add these together, and return the natural logarithm of the result. */
    // y = [(m,weight,y)]
  public double logP(Value x, Value y, Value z)
  {
    Value.Vector vy = (Value.Vector)y;
    Value.Model m;
    Value p;
    double[] weights = normaliseWeights(vy);
    double pr = 0;
    
//    double debug, debug2;
    
    for(int count = 0; count < weights.length; count++)
    {
      m = (Value.Model)((Value.Structured)vy.elt(count)).cmpnt(0);  // the model at vy.elt(count).
      p = ((Value.Structured)vy.elt(count)).cmpnt(2);               // the corresponding parameters.

//      debug2 = java.lang.Math.exp(m.logP(x, p, z));
//      debug = java.lang.Math.exp(m.logP(x, p, z)) * weights[count];
//      System.out.println("----------\nProb from model " + count + ": " + debug2 + " * " + weights[count] + " = " + debug);
      pr += java.lang.Math.exp(m.logP(x, p, z)) * weights[count];
    }
//    System.out.println("Weighted average prob: " + pr + "\n-----------");
    return java.lang.Math.log(pr);
  }

  // Returns a vector of elements from the data-space conditional on Y,Z.
  public Value.Vector generate(Random rand, int n, Value y, Value z)
  {
    double x = rand.nextDouble();
    int count;
    double sum;
    double[] weights = normaliseWeights((Value.Vector)y);
    for(count = 0, sum = weights[0]; sum < x; count++, sum += weights[count]);
    //count is now the index of the model from which to generate...
    Value params = ((Value.Structured)((Value.Vector)y).elt(count)).cmpnt(2);
    return ((Value.Model)((Value.Structured)((Value.Vector)y).elt(count)).cmpnt(0)).generate(rand, n, params, z);
  }

  /** This is wrong!  It's just a hack that will work correctly if all weights
      are 0 except for one.  It simply uses the model with the highest weight
      to predict.
  */
  public Value predict(Value y, Value z)
  {
//    throw new RuntimeException("Error - basic mixture model is not able to predict.");
    double[] weights = normaliseWeights((Value.Vector)y);
    int count, oneToUse = 0;
    double max = 0;
    for(count = 0; count < weights.length; count++)
    {
      if(weights[count] > max)
      {
        max = weights[count];
        oneToUse = count;
      }
    }
    Value.Model m = (Value.Model)((Value.Structured)((Value.Vector)y).elt(oneToUse)).cmpnt(0);
    Value params = ((Value.Structured)((Value.Vector)y).elt(oneToUse)).cmpnt(2);
    return m.predict(params, z);
  }

  public Value.Vector predict(Value y, Value.Vector z)
  {
    throw new RuntimeException("Error - basic mixture model is not able to predict.");
  }

  // Returns sufficient statistics for this model of the data.
  public Value getSufficient(Value.Vector x, Value.Vector z)
  {
    return new Value.DefStructured(new Value[]{x, z});
  }

  // logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2)... where s is a sufficient statistic of X&Z for Y.
  public double logPSufficient(Value s, Value y)
  {
    return logP(((Value.Structured)s).cmpnt(0), y, ((Value.Structured)s).cmpnt(1));
  }

  public String toString()
  {
    return "General Mixture Model";
  }
}
