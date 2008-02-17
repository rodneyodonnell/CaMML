//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.mml87;

import cdms.core.*;
import cdms.plugin.model.*;

/** MML87 Multinomial estimator using a prior. */
public class UniformMultinomialEstimator extends Value.Function
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5635618470333630887L;
public static Type.Discrete modelDataSpace = 
    new Type.Discrete(Integer.MIN_VALUE + 1,Integer.MAX_VALUE - 1,false,false,false,false);
  public static Value.Function uniformMultinomialEstimator = new UniformMultinomialEstimator();

  public UniformMultinomialEstimator() 
  {
    super(new Type.Function(new Type.Vector(new Type.Variable(), modelDataSpace, false, false, false, false), 
                            Type.STRUCTURED, 
                            false, 
                            false));
  }

  public String toString()
  {
    return "MML (uniform prior) multinomial estimator";
  }

  public Value apply(Value v)
  {
    Type.Discrete td = (Type.Discrete)((Type.Vector)v.t).elt;
    Value.Vector vv = (Value.Vector)v;
    int lwb = (int)td.LWB;
    int upb = (int)td.UPB;
    int count;
    double total = 0;
    double[] tallies = new double[upb-lwb+1];
    Value[] tallyVals = new Value[upb - lwb + 1];
    for(count = 0; count < tallies.length; count++) tallies[count] = 0.5;
    for(count = 0; count < vv.length(); count++)
    {
      total += vv.weight(count);
      tallies[vv.intAt(count) - lwb] += vv.weight(count);
    }
    for(count = 0; count < tallies.length; count++)
    {
      tallies[count] /= (total + (0.5 * tallies.length));
      tallyVals[count] = new Value.Continuous(tallies[count]);
    }
    return new Value.DefStructured(Multinomial.makeParamSpace(lwb, upb), tallyVals);
  }
}
