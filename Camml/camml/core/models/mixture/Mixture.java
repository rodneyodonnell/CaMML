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
// Mixture model class
//

// File: Mixture.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.mixture;

import java.util.Random;

import cdms.core.*;

import camml.core.library.*;
import camml.core.models.multinomial.MultinomialLearner;

/**
   Mixture model. <br>
 
   A Mixture model represents a mixture of models. <br>
   Each model must have the same type for S,X,Z but Y (parameters) may differ <br>
 
   Mixture Parameters form a vector [ (Proportion, Model, Params) ] <br>
*/
public class Mixture extends Value.Model
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7332470888744071810L;
    /** Static instance of mixture model. */
    public static final Mixture mixture = new Mixture();
    
    /** Mixture Constructor */
    public Mixture( )
    {
        super(new Type.Model(new Type.Variable(),                  // input space (x)
                             new Type.Vector(Type.STRUCTURED),     // parameter space (y)  
                             new Type.Variable(),                  // shared space (z)
                             new Type.Variable()) );               // sufficient space
        
        
    }
    
    
    /** 
     * logP(X|Y,Z) <br>
     * Calculate logP based on subModel[].logP() and proportions.
     * Some trickery is used to avoid underflow.
     */
    public double logP(Value x, Value y, Value z)
    {
        // We need to calculate logP(x|y,z) = log( sum{P(x|y_i,z)*prop_i} )
        //                                  = log( sum{e^{logP(x|y_i,z)+log(prop_i)}} )
        // letting len[i] = logP(x|y_i,z)+log(prop_i),
        //  logP(x|y,z) = log( sum{e^(len[i])} )
        //              = maxLen + log( sum{e^(len[i] - maxLen)} )
        //
        // We perform calculations in this roundabout way to avoid underflow.    
        
        // Calculate logP(x|y,z) for each submodel.
        // len[i] = logP( P(x|y[i],z) * proportion[i] )
        Value.Vector params = (Value.Vector)y;
        if ( checkParams( params ) ) { 
            
            throw new RuntimeException("Invalid Parameters : "+params+" Sum(proportion)="+totalProportion);
        };
        double[] len = new double[params.length()];
        double maxLen = -Double.POSITIVE_INFINITY;
        for ( int i = 0; i < len.length; i++ ) {
            Value.Structured elt = (Value.Structured)params.elt(i);
            double proportion = elt.doubleCmpnt(0);
            Value.Model model = (Value.Model)elt.cmpnt(1);
            Value subParams = elt.cmpnt(2);
            
            len[i] = model.logP(x,subParams,z) + Math.log(proportion);
            if ( len[i] > maxLen ) { maxLen = len[i]; }
        }
        
        // we keep track of maxLen to avoid underflow errors for Math.exp(len[])
        double total = 0;
        for ( int i = 0; i < len.length; i++ ) {
            total += Math.exp(len[i]-maxLen);
        }
        
        return maxLen + Math.log(total);
    }
    
    /** logP(X|Y,Z) where v = (X,Y,Z) */
    public double logP(Value.Structured v)
    {
        return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }
    
    
    /** 
     *  return logP of output vector given input vector and params. <br>
     *  We are forced to calculate this one value at a time and may be slow for some models.
     */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
        double total = 0;
        
        for ( int i = 0; i < x.length(); i++ ) {
            total += logP( x.elt(i), y, z.elt(i) );
        }
        
        return total;
    }
    
    
    
    /** Not implemented.  Is this possible in the general case for Mixtures? */
    public double logPSufficient(Value s, Value y)
    {
        throw new RuntimeException("Not implemented");
    }
    
    
    
    
    /**
       Returns a stochastic vector of elements from the data-space conditional on Y,Z.
       This normalises the Mixture.
    */
    public Value.Vector generate(Random rand, int n, Value y, Value z)
    {
        return generate( rand, y, new VectorFN.UniformVector( n, z ) );
    }
    
    /**
     * Does the real work of the generate function.  We use a java.util.Random as a seed instead
     * of an int.
     */
    public Value.Vector generate( java.util.Random rand, Value y, Value.Vector z )
    {
        
        Value.Vector yVec = (Value.Vector)y;
        
        if (checkParams( yVec )) { throw new RuntimeException("Invalid Parameters"); };
        
        double[] mixParams = new double[ yVec.length() ];
        for ( int i = 0; i < mixParams.length; i++ ) {
            mixParams[i] = ((Value.Structured)yVec.elt(i)).doubleCmpnt(0);
        }
        Value.Structured mixParamStruct = new StructureFN.FastContinuousStructure( mixParams );
        Value.Model model = MultinomialLearner.getMultinomialModel( 0, mixParamStruct.length()-1 );
        
        Value.Vector cmpntSelection = 
            model.generate( rand, z.length(), mixParamStruct, Value.TRIV );
        
        int[] tally = new int[mixParamStruct.length()];
        for ( int i = 0; i < cmpntSelection.length(); i++ ) {
            tally[cmpntSelection.intAt(i)]++;       
        }
        int[][] splits = new int[tally.length][];
        for ( int i = 0; i < splits.length; i++ ) {
            splits[i] = new int[ tally[i] ];
        }
        int[] count = new int[tally.length];
        for ( int i = 0; i < cmpntSelection.length(); i++ ) {
            int x = cmpntSelection.intAt(i);
            splits[x][count[x]] = i;
            count[x]++;
        }
        
        Value.Vector[] subVec = new Value.Vector[ tally.length ];
        for ( int i = 0; i < subVec.length; i++ ) {
            Value.Structured elt = (Value.Structured)yVec.elt(i);
            Value.Model subModel = (Value.Model)elt.cmpnt(1);
            Value subParams = elt.cmpnt(2);
            Value.Vector subInputVec = new SelectedVector( z, splits[i], null );
            subVec[i] = subModel.generate( rand, subParams, subInputVec );
        }
        
        return new MergedVector( subVec, splits );
    }
    
    
    /** predict returns the value x which has the maximum probability given y (hence the least 
        logprob) if two values have the same logprob, it will return the first such.
    */
    public Value predict(Value y, Value z)
    {
        throw new RuntimeException("Not implemented");
    } 
    
    public Value.Vector predict(Value y, Value.Vector z) {
        return new VectorFN.UniformVector(z.length(), predict(y, z));
    }
    
    /**
     *  The sufficient statistics of a Mixture is a list of sufficient stats of its elements.
     *  Each paremt combination maps to a given entry in the sufficient vector.  This mapping is
     *  defined by Mixture.decodeParents() <br>
     *
     *  NOTE: Is this possible for Mixture Models?
     */
    public Value getSufficient(Value.Vector x, Value.Vector z)
    {
        throw new RuntimeException("Not implemented");
    }
    
    /** Check parameters are valid. True is returned for falty parameters */
    public static boolean checkParams( Value.Vector params ) {
        Value.Vector proportion = params.cmpnt(0);
        double total = 0;
        for ( int i = 0; i < proportion.length(); i++ ) {
            try {
                total += proportion.doubleAt(i);
            } catch ( RuntimeException e ) {
                System.out.println("proporiton = " + proportion );
                System.out.println("params = " + params);
                throw e;
            }
        }    
        if ( total > 1.0001 || total < 0.9999 ) { 
            totalProportion = total;
            return true; 
        }
        return false;
    }
    private static double totalProportion;
    
    /** returns a representation of the Mixture */
    public String toString()
    {
        return "Mixture";
    } 
    
}
