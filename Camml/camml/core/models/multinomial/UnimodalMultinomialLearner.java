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
// Wrapper for cdms.mml87.multinomialParameterizer
//

// File: UnimodalMultinomialLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import camml.core.library.*;

import cdms.core.*;
import cdms.plugin.model.*;

import camml.core.models.ModelLearner;

/**
 * UnimodalMultinomialLearner is a wrapper class of type ModelLearner. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br>
 */
public class UnimodalMultinomialLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 6981698869254014399L;
    /** Static instance of class */
    public static UnimodalMultinomialLearner unimodalMultinomialLearner = 
        new UnimodalMultinomialLearner();
    
    public String getName() { return "UnimodalMultinomialLearner"; }    
    
    
    public UnimodalMultinomialLearner()
    {
        super( new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED ) 
               , Type.TRIV ); 
    }
    
    
    //     /** Install functions into the cdms environment. */
    //     public void install(Value params) throws Exception
    //     {
    //     super.install(params);
    //     add( "isUnimodal", isUnimodal, "Return true if (...) is unimodal.  (...) -> boolean" );
    //     }
    
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value i, Value.Vector x, Value.Vector z )
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = new Multinomial((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        return sParameterize( multinomialModel, stats );
    } 
    
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
    {
        // get Sufficient stats from the data.
        Value.Structured stats = (Value.Structured)s;
        
        double bestCost = Double.POSITIVE_INFINITY;
        //double bestMode = 0;
        Value.Structured bestParams = null;
        
        int arity = stats.length();
        int total = 0;
        
        for (int i = 0; i < arity; i++) {
            total += stats.doubleCmpnt(i);
        }
        
        
        for (int mode = 0; mode < arity; mode++) {
            Value.Structured params = smoothBumpsAndParameterize( model, stats, mode, total );
            double cost = sCost( model, stats, params );
            
            if (cost < bestCost ) {
                bestCost = cost;
                bestParams = params;
                //bestMode = mode;
            }
        }  
        
        return new Value.DefStructured( new Value[] {model, stats, bestParams} );
    }
    
    /** Get sufficient stats and call sCost */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value y)
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = new Multinomial((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        
        return sCost( m, stats, (Value.Structured)y );
    }
    
    /**  Check for unimodality, return multinomialCost - saving <br>
     *   throws IllegalArgumentException if argument is not unimodal.
     */
    public double sCost( Value.Model m, Value stats, Value params )
    {
        // quick check to make sure params is actually unimodal.
        if ( isUnimodal.apply(params) == Value.FALSE )
            throw new IllegalArgumentException("parameters passed are not unimodal in " + 
                                               "UnimodalMultinomialLearner.sCost");
        
        int arity = ((Value.Structured)params).length();
        double cost = MultinomialLearner.multinomialLearner.sCost(m, stats, params);
        double saving = FN.LogFactorial.logFactorial(arity) - Math.log( Math.pow(2,arity-1));
        return cost - saving;
    }
    
    /** Function checks if parameters are unimodal <br>
     *  ContinuousStructured -> boolean
     */
    public static IsUnimodal isUnimodal = new IsUnimodal();
    public static class IsUnimodal extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3926741069172463974L;
        public static Type.Function tt = new Type.Function( Type.STRUCTURED, Type.SYMBOLIC );
        
        public IsUnimodal()
        {
            super(tt);
        }
        
        /** If function is unimodal return true, else return false. */
        public Value apply( Value v )
        {
            boolean modeFound = false;
            Value.Structured params = (Value.Structured)v;
            
            for (int i = 1; i < params.length(); i++) {
                // if no mode has been found and function decreaces, then the mode has been found
                if ( !modeFound && (params.doubleCmpnt(i) < params.doubleCmpnt(i-1))) {
                    modeFound = true;
                }
                // if a mode has been found, but the function continues to rise, then the function 
                // can not be unimodal.
                if ( modeFound &&  (params.doubleCmpnt(i) > params.doubleCmpnt(i-1))) {
                    return Value.FALSE;
                }
            }
            return Value.TRUE;
        }
    }                       
    
    /** Given the sufficient Statistics of a model, smoothBumpsAndParameterize returns the 
     * optimal(?) probability distribution for the given mode.     
     */
    protected static Value.Structured smoothBumpsAndParameterize( Value.Model m, 
                                                                  Value.Structured stats,
                                                                  int mode, 
                                                                  double total ) 
    {
        double tally[] = new double[ stats.length() ];
        for ( int i = 0; i < tally.length; i++ ) {
            tally[i] = stats.doubleCmpnt(i);
        }
        
        int arity = tally.length;
        for(int i = mode-1; i >= 0; i--) { // from mode to the left        
            double sum = tally[i]; 
            int width = 1;
            while((i+width < arity) && sum/width > tally[i+width]) // size of bump
                { sum += tally[i+width]; width++; }
            for(int k = 0; k < width; k++)
                tally[i+k] = sum/width;  // smooth out bump
        }
        
        for(int i = mode+1; i < arity; i++) { // from mode to the right        
            double sum = tally[i];
            int width = 1;
            while((i-width > 0) && sum/width > tally[i-width]) // size of bump
                { sum += tally[i-width]; width++; }
            for(int k = 0; k < width; k++)
                tally[i-k] = sum/width;  // smooth out bump
        }
        
        // Recalculate total ... it seems to need this.
        total = 0;
        for (int i = 0; i < arity; i++) {
            total += tally[i] + 0.5;
        }
        
        // MML Estimate of probabilities from tallies
        for (int i = 0; i < arity; i++) {
            tally[i] = (tally[i] + 0.5) / total; 
        }
        
        //Type.Structured paramSpace = (Type.Structured)((Type.Model)m.t).paramSpace;
        //    return new ContinuousStructured( paramSpace, tally );
        return new StructureFN.FastContinuousStructure(tally);
    }
    
    
    public String toString() { return "UnimodalMultinomialLearner"; }
}

