//
// Join a vector of values and a vector of weights to make a weighted vector.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MakeWeightedVector.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import cdms.core.*;

/* Replace the old weights of a vector with a new set of weights. ([t],[continuous]) -> [t] */
public class MakeWeightedVector extends Value.Function
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1893355372418258666L;

	/** Static instance of sum. */
    public static MakeWeightedVector makeWeightVector = new MakeWeightedVector();

    /** The type of the functions parameter = ([t],[coutinuous])*/
    protected static Type.Structured inType = new Type.Structured( new Type[] 
	{Type.VECTOR, new Type.Vector(Type.CONTINUOUS)} );

    MakeWeightedVector() 
    { 
	super(new Type.Function( inType, Type.VECTOR ));
    }


    /** Replace the old weights of a vector with a new set of weights. ([t],[continuous]) -> [t] */ 
    public Value apply( Value v )
    {
	Value.Structured struct = (Value.Structured)v;
	Value.Vector vec = (Value.Vector)struct.cmpnt(0);
	Value.Vector weights = (Value.Vector)struct.cmpnt(1);

	return new WeightedVector2( vec, weights );
    }
}
