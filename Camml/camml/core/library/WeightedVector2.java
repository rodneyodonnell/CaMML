//
// Vector implementation taking two Vectors.  A set of values and a set of weights.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: WeightedVector.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import cdms.core.*;

/* 
 * Replace the old weights of a vector with a new set of weights. ([t],[continuous]) -> [t] <br> 
 * The only differente between WeightedVector2 and WeightedVector is the addition of a second
 * constructor for WeightedVector2 allowing weights to be a vector, instead of just an array.
 */

public class WeightedVector2 extends VectorFN.WeightedVector
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2815287981554489766L;

	boolean weightVectorUsed = false;

    protected Value.Vector weightVector;
    public WeightedVector2( Value.Vector v, Value.Vector weightVector )
    {
	super(v,null);
	this.weightVector = weightVector;
	weightVectorUsed = true;
    }

    public WeightedVector2( Value.Vector v, double[] weights )
    {
	super(v,weights);
    }

    public double weight(int i)
    {
	
	if (weightVectorUsed == true)
	    return weightVector.doubleAt(i);
	else
	    //  Had to do this as weights is private...
	    //	    return weights[i];
	    return super.weight(i);
    }
    
    public Value.Vector cmpnt(int i) {
    	return new WeightedVector2( v.cmpnt(i), weights);
    }
}
