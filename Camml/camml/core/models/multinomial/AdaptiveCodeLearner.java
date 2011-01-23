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
// ModelLearner for Multinomial using adaptive code.
//

// File: AdaptiveCodeLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import cdms.core.*;
import camml.core.library.*;

import cdms.plugin.model.*;
import camml.core.models.*;

import cdms.core.FN.LogFactorial;

/**
 * Cost models using an adaptive code.<br>
 * Optionally : specify a bias to use for parameterization (+0,+0.5,+1.0, ect.) <br>
 *              Add (|x|-1)*log(Pi*e/6)/2 to make it a MML score (as done in CaMML) <br>
 */
public class AdaptiveCodeLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -4679307451046263387L;

    /** Adaptive Code Learner parameterized as (n+0.5)/(N+m/2) */
    public static AdaptiveCodeLearner adaptiveCodeLearner = 
        new AdaptiveCodeLearner( 0.5, false );
    
    /** Adaptive Code Learner with MML correction parameterized as (n+0.5)/(N+m/2) */
    public static AdaptiveCodeLearner mmlAdaptiveCodeLearner = 
        new AdaptiveCodeLearner( 0.5, true );
    
    /** Adaptive Code Learner parameterized as (n+1)/(N+m) */
    public static AdaptiveCodeLearner adaptiveCodeLearner2 = 
        new AdaptiveCodeLearner( 1.0, false );

    /** Adaptive Code Learner with MML corrextionparameterized as (n+1)/(N+m) */
    public static AdaptiveCodeLearner mmlAdaptiveCodeLearner2 = 
        new AdaptiveCodeLearner( 1.0, true);
    
    /** return "AdaptiveCodeLearner" */
    public String getName() { return "AdaptiveCodeLearner"; }    
    
    /** Bias used for parameterization, 0 = ML, 0.5 = MML, 1.0 = MEKLD, etc. */
    final double biasVal;
    
    /** should (|x|-1)*log(Pi*e/6)/2 be added to the cost as done in MML?   */
    final boolean useMMLScore;
    
    /** Constructer specifying bias and useMMLScore. */
    public AdaptiveCodeLearner( double biasVal, boolean useMMLScore )
    {
        super( new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED )
               , Type.TRIV ); 
        this.biasVal = biasVal;
        this.useMMLScore = useMMLScore;
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value i, Value.Vector x, Value.Vector z )
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        // Check if a multinomial with the current UPB and LWB already exists.
        Value.Model multinomialModel = 
            MultinomialLearner.getMultinomialModel((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        return sParameterize( multinomialModel, stats );    
    }     
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
    {
        Value.Structured stats = (Value.Structured)s;
        
        double params[] = new double[stats.length()];
        double total = (double)params.length * biasVal;
        
        for (int i = 0; i < params.length; i++) {        
            params[i] = stats.doubleCmpnt(i);
            total += params[i];
        }
        
        // estimate params[i]
        for (int i = 0; i < params.length; i++) {
            params[i] = (params[i] + biasVal) / total;
        }
        
        // return Value.Structured containing (model,stats,params)
        return new Value.DefStructured( new Value[] {
                model, stats, new StructureFN.FastContinuousStructure(params) } );
    }
    
    /** return cost of adaptive code, parameters are ignored. */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value y)
    {
        return parameterizeAndCost( i, x, z );
    }
    
    /** return cost, parameters are ignored. */
    public double sCost( Value.Model m, Value stats, Value params )
    {
        return sParameterizeAndCost( m, stats );
    }

    /** Parameterise and cost data all in one hit.   */
    public double parameterizeAndCost( Value i, Value.Vector x, Value.Vector z )
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = 
            MultinomialLearner.getMultinomialModel((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        
        return sParameterizeAndCost( multinomialModel, stats );
    }
    
    /** Parameterise and cost data all in one hit.   */
    public double sParameterizeAndCost( Value.Model m, Value s )
    {
        Value.Structured stats = (Value.Structured)s;
        
        // Extract tallys from stats. 
        int[] tally = new int[stats.length()];
        int totalTally = 0;
        for (int i = 0; i < tally.length; i++) {        
            tally[i] = stats.intCmpnt(i);
            totalTally += tally[i];
        }
        
        // adaptive code cost = log( (N+|x|-1)!/((|x|-1)!*product(x_i)) )
        // = log!(N+|x|-1) - log!(|x|-1) - sum log!(x_i)
        // LogFactorial.logFactorial(arity-1)    
        
        double cost = 0;
        for (int i = 0; i < tally.length; i++) {        
            cost -= LogFactorial.logFactorial( tally[i] );
        }
        cost += LogFactorial.logFactorial( totalTally + tally.length - 1);
        cost -= LogFactorial.logFactorial( tally.length - 1 );
        
        if ( useMMLScore ) {
            cost += (tally.length - 1) * 0.17649;
        }
        return cost;
    }
    
    
    /** Multinomial2 is the same as Multinomial but implementing GetNumParams */
    public static class Multinomial2 extends Multinomial implements GetNumParams
    { 
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 3082932627565542774L;

        public Multinomial2(int lwb, int upb) { super(lwb,upb); }
        public Multinomial2(Type.Discrete dataSpace) { super( dataSpace ); }
        
        /** return the number of free parameters. */
        public int getNumParams( Value params ) {
            return (int)(upb - lwb);
        }
    }
    
    /** Return "AdaptiveCodeLearner" */
    public String toString() { return "AdaptiveCodeLearner(" + biasVal+",mml="+useMMLScore+")"; }
    
    
    
    

}

