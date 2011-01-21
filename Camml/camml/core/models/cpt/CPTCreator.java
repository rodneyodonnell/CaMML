//
// CPT creator class
//
// Copyright (C) 2002 Rodney O'Donnell, Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: CPT.java
// Author: {rodo,lhope}@csse.monash.edu.au

package camml.core.models.cpt;

import cdms.core.Type;
import cdms.core.Value;


/**
   CPTCreator creates CPTs :).
 
   Parameters:<br>
   Note that symbolic types are lowerbounded at 0                                           <br>
   a vector of discrete, representing the lowerbound of each of Pa(X) (the parents X)       <br>
   a vector of discrete, representing the upperbound of each of Pa(X) (the parents X)       <br>
   (the order is important, and must be kept constant. It defines how to index the table.)  <br>
   a vector of vectors of continuous, representing the logP of each value of X, indexed by
   Pa(X).
   <p>
   all wrapped inside a structured type.
*/
public class CPTCreator extends Value.Function
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -8258812353637066316L;

    private static Type.Structured paramType =
        new Type.Structured(new Type[]{new Type.Vector(Type.DISCRETE),
                                       new Type.Vector(Type.DISCRETE) },
            new String[]{"Parent lowerbounds", "Parent upperbounds" },
            new boolean[]{ false, false} );
    
    private static Type.Model resultType =
        new Type.Model(new Type.Variable(),     // x = x can be any type of value
                       Type.STRUCTURED,         // y = (...bounded discrete...)
                       Type.VECTOR,             // z = [(model,(...probabilty...))]
                       Type.STRUCTURED);        // s = (x,z)
    
    public static CPTCreator cptCreator = new CPTCreator();
    
    public CPTCreator()
    {
        super(new Type.Function(paramType, resultType, false, false));
    }
    
    public Value apply(Value v)
    {
        // decode v
        Value.Structured s = (Value.Structured)v;
        Value.Model childModel = (Value.Model)s.cmpnt(0);
        Value.Vector lwbVector = (Value.Vector)s.cmpnt(1);
        Value.Vector upbVector = (Value.Vector)s.cmpnt(2);
        
        // translate lwbVector and upbVector
        int[] lwbs = new int[lwbVector.length()];
        int[] upbs = new int[upbVector.length()];
        for(int i = 0; i < lwbs.length; i ++) {
            lwbs[i] = lwbVector.intAt(i);
            upbs[i] = upbVector.intAt(i);
        } 
        
        return new CPT( childModel, lwbs, upbs );
    } 
}
