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
// Vector implementation taking two Vectors.  A set of values and a set of weights.
//

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
            //        return weights[i];
            return super.weight(i);
    }
    
    public Value.Vector cmpnt(int i) {
        return new WeightedVector2( v.cmpnt(i), weights);
    }
}
