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
// Join a vector of values and a vector of weights to make a weighted vector.
//

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
