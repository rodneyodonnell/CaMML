//
// Sum a vector of continuous or discrete together.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: Sum.java
// Author: rodo@csse.monash.edu.au


package camml.core.library;

import cdms.core.*;

/* Sum a vector.  This may take three forms. <br>
 * [Continuous] -> Continuous <br>
 * [Discrete] -> Discrete <br>
 * [(a,b,c)]  -> (sum a, sum b, sum c)
 */
public class Sum extends Value.Function
{

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7951237933631001373L;

	/** Can be either Continuous, Discrete, or a Structure of continuous or discretes. */
    public static Type.Union unionType = new Type.Union(new Type[] {Type.CONTINUOUS, 
								    Type.DISCRETE, 
								    Type.STRUCTURED} );

    /** Static instance of sum. */
    public static Sum sum = new Sum();

    Sum() 
    { 
	super( new Type.Function(new Type.Vector(unionType), unionType) ); 
    }


    /** [Discrete] -> Discrete, or [t] -> Continuous, or [(a,b,c)] -> (sum a, sum b, sum c) */
    public Value apply( Value v )
    {
	Type eltType = ((Type.Vector)v.t).elt;
	if ( eltType instanceof Type.Structured ) {
	    
 	    int arity = ((Type.Structured)eltType).cmpnts.length;
	    Value sums[] = new Value[arity]; 
	    for (int i = 0 ; i < arity; i++) {
		sums[i] = apply( ((Value.Vector)v).cmpnt(i) );
	    }
	    return new Value.DefStructured((Type.Structured)eltType, sums);
	    
	}
	else if (eltType instanceof Type.Discrete) {
	    return new Value.Discrete(applyInt(v));
	}
	else {
	    return new Value.Continuous(applyDouble(v));
	}

    }
    
    public double applyDouble( Value v )
    {
	Value.Vector vec = (Value.Vector)v;
	int len = vec.length();
	double sum = 0;
	for (int i = 0; i < len; i++)
	    sum += vec.doubleAt(i);
	return sum;
    }
    
    public int applyInt(Value v)
    {
	return (int)applyDouble(v);
    }
    
}
