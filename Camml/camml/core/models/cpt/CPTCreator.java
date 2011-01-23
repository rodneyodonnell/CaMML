/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lucas Hope, Lloyd Allison, Kevin Korb
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// CPT creator class
//

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
