/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
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
// Sum a vector of continuous or discrete together.
//

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
