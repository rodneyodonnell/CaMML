//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.mml87;

import cdms.core.*;

/** MML87 Multinomial coster function using a uniform prior. */
public class UniformMultinomialCoster extends Value.Function
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -359645859123189152L;
public static final Type.Discrete modelDataSpace = 
    new Type.Discrete(Integer.MIN_VALUE+1,Integer.MAX_VALUE-1, false, false, false, false);
  public static final UniformMultinomialCoster uniformMultinomialCoster = new UniformMultinomialCoster();

  /** Need a standard type for coster functions, maybe a combination of  
      (Model, Parameters, Data) according to some metric 
      (e.g. MML, AIC, ML, sum squared error...).  They take the form:<BR>
      (Model, Parameters, Vector of data) -> Continuous<BR> This class is just for 
      convenience - it creates the correct function type given the type of the model.
      Everything can be derived from the type of the model using its dataSpace, paramSpace etc.
      Perhaps another similar kind of function could use<BR>
      (Model, Parameters, Sufficient) -> Continuous<BR>*/
  public static Type.Function calcType(Type.Model tm)
  {
    return new Type.Function(new Type.Structured(new Type[]{tm, tm.paramSpace,
             new Type.Vector(new Type.Variable(), tm.dataSpace, false, false, false, false)},
                             new String[]{"Model", "Parameters", "Data"},
                             new boolean[]{false, false, false}),
                   Type.CONTINUOUS,
                   false,
                   false);
  }

  public UniformMultinomialCoster()
  {
    super(calcType(new Type.Model(modelDataSpace, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED)));
  }

  public String toString()
  {
    return "MML (uniform prior) multinomial coster";
  }

  public Value apply(Value v)
  {
    Value.Vector data = (Value.Vector)((Value.Structured)v).cmpnt(2);
    Value.Structured modelParams = (Value.Structured)((Value.Structured)v).cmpnt(1);
    cdms.plugin.model.Multinomial model = (cdms.plugin.model.Multinomial)((Value.Structured)v).cmpnt(0);
    int upb = model.upb;
    int lwb = model.lwb;
    double[] tallies = new double[upb - lwb + 1];
    int count;
    for(count = 0; count < data.length(); count++)
    {
      tallies[data.intAt(count) - lwb] += data.weight(count);
    }
    double cost = 0;
    double total = 0;
    double h, f;
    for(count = 0; count < tallies.length; count++)
    {
      total += tallies[count];
    }
    if(total > 0)
    {
      // Multinomial Distribution MML Cost ... 
		
      // Hypothesis cost... 
      h = Math.exp(cdms.core.FN.LogFactorial.logFactorial(modelParams.length()-1));
      f = 1.0 / ((Value.Scalar)modelParams.cmpnt(modelParams.length() - 1)).getContinuous();
      for(count = 0; count < modelParams.length() - 1; count++)
      {
        f *= total;
        f /= ((Value.Scalar)modelParams.cmpnt(count)).getContinuous();
      }
      cost = 0.5 * java.lang.Math.log(1 + (f/(java.lang.Math.pow(12,modelParams.length()-1) * h * h)));
      cost += 0.5 * (modelParams.length()-1);
      for(count = 0; count < upb - lwb + 1; count++)
      {
        cost -= tallies[count] * Math.log(((Value.Scalar)modelParams.cmpnt(count)).getContinuous());
      }
    }
    return new Value.Continuous(cost);
  }
}
