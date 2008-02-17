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

/** This kind of mixture model takes an array of models in the constructor.  Each object of this 
class represents the group of all models which are a mixture of one or more of each of the elements 
of this array.  e.g. The MixtureModel constructed with [Normal, Uniform] represents the "class" of
models made up of a mixture of at least one Normal and at least one Uniform pdf.  The parameters to
the mixture model are of the form:  <BR>
([(w1_Normal, p1_Normal), ..., (wi_Normal, pi_Normal)], [(w1_Uniform, p1_Uniform), ..., (wj_Uniform, pj_Uniform)])<BR>
where the mixture model is a mixture of i Normal pdf's and j Uniform pdf's.
<BR> The dataSpace of the mixture model is the most specific of the dataSpaces of all the models in the constructor.
The sharedSpace of the mixture model is the most specific of the sharedSpaces of all the models in the constructor.
The sufficientSpace of the mixture model takes the form:<BR>
(model_1_sufficientSpace, ..., model_k_sufficientSpace)<BR>
or continuing the above example...<BR>
(Normal_sufficientSpace, Uniform_sufficientSpace).*/

public class MixtureModel extends Value.Model
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8611874044871196450L;
private Value.Model[] models;

  public MixtureModel(Value.Model[] models)
  {
    super(makeModelType(models));
    this.models = models;
  }

  private static Type.Model makeModelType(Value.Model[] models)
  {
// Here we need to determine the most specific of all the models' dataSpaces, and use
// this for the dataSpace of the resultant Type.Model.
// Same deal for the sharedSpaces.

    Type dataSpace = new Type.Variable();
    Type sharedSpace = new Type.Variable();

    Type.Model[] tm = new Type.Model[models.length];
    Type.Structured[] ts = new Type.Structured[models.length];
    Type.Vector[] tv = new Type.Vector[models.length];
    Type[] sufficientSpace = new Type[models.length];
    String[] s = new String[models.length];
    String[] s2 = new String[models.length];
    boolean[] b = new boolean[models.length];
    int count;
    for(count = 0; count < tm.length; count++)
    {
      tm[count] = (Type.Model)models[count].t;
      ts[count] = new Type.Structured(new Type[]{Type.CONTINUOUS, tm[count].paramSpace}, 
                                      new String[]{"Weight","Parameters"}, 
                                      new boolean[]{false, false});
      tv[count] = new Type.Vector(Type.TYPE, ts[count], false, false, false, false);
      sufficientSpace[count] = tm[count].sufficientSpace;
      s[count] = models[count] + " weights and parameters";
      s2[count] = models[count] + " sufficient statistics";
      b[count] = false;
    }
    Type paramSpace = new Type.Structured(tv, s, b);
    Type totalSufficientSpace = new Type.Structured(sufficientSpace, s2, b);
    return new Type.Model(dataSpace, paramSpace, sharedSpace, totalSufficientSpace);
  }

  private double[] getNormalisedWeights(Value y)
  {
    Value.Structured ys = (Value.Structured)y;
    Value.Vector vv;
    int count, count2, count3, size;
    for(count = 0, size = 0; count < ys.length(); count++)
    {
      size += ((Value.Vector)ys.cmpnt(count)).length();
    }
    double[] weights = new double[size];
    double sum = 0;
    for(count = 0, count3 = 0; count < ys.length(); count++)
    {
      vv = (Value.Vector)ys.cmpnt(count);
      for(count2 = 0; count2 < vv.length(); count2++, count3++)
      {
        weights[count3] = ((Value.Structured)vv.elt(count2)).doubleCmpnt(0);
        sum += weights[count3];
      }
    }
    for(count = 0; count < weights.length; count++)
    {
      weights[count] /= sum;
    }
    return weights;
  }

  private Value.Vector mixtureModelParams2GeneralMixtureModelParams(Value.Structured mixtureModelParams)
  {
    Value.Model[] newModelArray;
    Value[] weights;
    Value[] params;
    int count, count2, count3;
    int sum = 0;
    for(count = 0; count < mixtureModelParams.length(); count++)
    {
      sum += ((Value.Vector)mixtureModelParams.cmpnt(count)).length();
    }
    newModelArray = new Value.Model[sum]; 
    weights = new Value[sum];
    params = new Value[sum];
    Value.Structured[] vs = new Value.Structured[sum];
    Type.Structured structuredType = new Type.Structured(new Type[]{new Type.Model(((Type.Model)t).dataSpace, 
                                                                                   Type.TYPE, 
                                                                                   ((Type.Model)t).sharedSpace,
                                                                                   Type.TYPE),
                                                                    Type.CONTINUOUS, 
                                                                    Type.TYPE}, 
                                                         new String[]{"Model","Weight","Parameters"}, 
                                                         new boolean[]{false, false, false});
    for(count = 0, count3 = 0; count < mixtureModelParams.length(); count++)
    {
      for(count2 = 0; count2 < ((Value.Vector)mixtureModelParams.cmpnt(count)).length(); count2++, count3++)
      {
        newModelArray[count3] = models[count];
        weights[count3] = ((Value.Structured)((Value.Vector)mixtureModelParams.cmpnt(count)).elt(count2)).cmpnt(0);
        params[count3] = ((Value.Structured)((Value.Vector)mixtureModelParams.cmpnt(count)).elt(count2)).cmpnt(1);
        vs[count3] = new Value.DefStructured(structuredType, new Value[]{newModelArray[count3], weights[count3], params[count3]});
      }
    }
    return new VectorFN.FatVector(vs);
  }

  // logP(X|Y,Z)
  /** Get the probability of x|(y,z) for each model, multiplied by that model's weight.
      Add these together, and return the natural logarithm of the result. */
  public double logP(Value x, Value y, Value z)
  {
    return new GeneralMixtureModel().logP(x, mixtureModelParams2GeneralMixtureModelParams((Value.Structured)y), z);
  }

  // Returns a vector of elements from the data-space conditional on Y,Z.
  public Value.Vector generate(Random rand, int n, Value y, Value z)
  {
    return new GeneralMixtureModel().generate(rand, n, mixtureModelParams2GeneralMixtureModelParams((Value.Structured)y), z);
  }

  public Value predict(Value y, Value z)
  {
    throw new RuntimeException("Error - basic mixture model is not able to predict.");
  }

  public Value.Vector predict(Value y, Value.Vector z)
  {
    throw new RuntimeException("Error - basic mixture model is not able to predict.");
  }

  // Returns sufficient statistics for this model of the data.
  public Value getSufficient(Value.Vector x, Value.Vector z)
  {
    Value[] array = new Value[models.length];
    int count;
    for(count = 0; count < array.length; count++)
    {
      array[count] = models[count].getSufficient(x, z);
    }
    return new Value.DefStructured(array);
  }

  // logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2)... where s is a sufficient statistic of X&Z for Y.
  public double logPSufficient(Value s, Value y)
  {
    Value.Structured ys = (Value.Structured)y;
    Value.Vector vv;
    double sum = 0;
    double prob;
    double[] weights = getNormalisedWeights(y);
    int count, count2, count3;
    for(count = 0, count3 = 0; count < ys.length(); count++)
    {
      vv = (Value.Vector)ys.cmpnt(count);
      for(count2 = 0; count2 < vv.length(); count2++, count3++)
      {
        prob = java.lang.Math.exp(logPSufficient(((Value.Structured)s).cmpnt(count), ((Value.Structured)vv.elt(count2)).cmpnt(1)));
        sum += weights[count3] * prob;
      }
    }
    return java.lang.Math.log(sum);
  }

  public String toString()
  {
    String s = "Mixture Model (" + models[0];
    int count;
    for(count = 1; count < models.length; count++)
    {
      s = s + ", " + models[count];
    }
    s = s + ")";
    return s;
  }
}
