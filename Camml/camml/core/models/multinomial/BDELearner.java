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
// ModelLearner for Multinomial using BDE code.
//

// File: BDELearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import cdms.core.*;
import camml.core.library.*;


import camml.core.models.*;

import cdms.plugin.model.Gamma;


/**
 * Cost models using an BDE code.<br>
 */
public class BDELearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 6038596144648560788L;
    public static BDELearner bdeLearner = new BDELearner( 5 );
    
    public String getName() { return "BDELearner"; }    
    
    /**
     * ess (Equivelent Sample Size) is a prior used by BDE. <br>
     * ess is effectively a sample viewed prior to parameterization and biases accordingly. <br>
     * Here we assume ess is distributed equally amongst values, but this need not be the case.
     */
    final double ess;
    
    /** Constructer specifying Equivelent Sample Size. */
    public BDELearner( double ess )
    {
        super( new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED )
               , Type.TRIV ); 
        this.ess = ess;
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value i, Value.Vector x, Value.Vector z )
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
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
        double total = 0;
        
        for (int i = 0; i < params.length; i++) {        
            params[i] = stats.doubleCmpnt(i) + (ess / params.length);
            total += params[i];
        }
        
        // find estimate of params[i]
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i]  / total;
        }
        
        return new Value.DefStructured( new Value[] {
                model, stats, new StructureFN.FastContinuousStructure(params) } );
    }
    
    /** return cost */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value y)
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = 
            MultinomialLearner.getMultinomialModel((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        
        return sCost( m, stats, (Value.Structured)y );
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
        
        // Convert from Struct -> int[]
        int[] tally = new int[stats.length()];
        int totalTally = 0;
        for (int i = 0; i < tally.length; i++) {        
            tally[i] = stats.intCmpnt(i);
            totalTally += tally[i];
        }
        
        double cost = Gamma.logGamma(ess+totalTally) - Gamma.logGamma(ess);
        for ( int i = 0; i < tally.length; i++ ) {
            cost += Gamma.logGamma( ess / tally.length ) - 
                Gamma.logGamma( ess/tally.length + tally[i] );
        }
        
        return cost;
    }
    
    /** return cost, parameters are ignored. */
    public double sCost( Value.Model m, Value stats, Value params )
    {
        return sParameterizeAndCost( m, stats );
    }
    
    
    public String toString() { return "BDELearner"; }
    
    
    
    
    /** Default implementation of makeBDELearner */
    public static final MakeBDELearner makeBDELearner = 
        new MakeBDELearner();
    
    /** MakeBDELearner returns a BDELearner given a options. */
    public static class MakeBDELearner extends MakeModelLearner
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 940966622924155715L;

        public MakeBDELearner( ) { }
        
        /** Shortcut apply method */
        public ModelLearner _apply( String[] option, Value[] optionVal ) {  
            
            // Set default values.
            double ess = 5.0;
            
            // Search options for overrides.
            for ( int i = 0; i < option.length; i++ ) {
                if ( option[i].equals("ess") ) {
                    ess = ((Value.Scalar)optionVal[i]).getContinuous();
                }
                else { throw new RuntimeException("Unknown option : " + option[i] );}
            }
            
            return new BDELearner( ess );
        }
        
        public String[] getOptions() { return new String[] {
                "ess - equivelent sample size"
            }; }
        
    }
    
}

