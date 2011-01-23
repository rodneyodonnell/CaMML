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

// File: MultinomialGlue.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import cdms.core.*;
import camml.core.library.*;

import cdms.plugin.model.*;

import camml.core.models.*;

/**
 * MultinomialLearner is a wrapper class of type ModelLearner. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br>
 */
public class MultinomialLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 6399666127784362130L;
    /** Static instance of class */
    public static MultinomialLearner multinomialLearner = new MultinomialLearner();
    
    public String getName() { return "MultinomialLearner"; }    
    
    final double biasVal;
    
    public MultinomialLearner()
    {
        super( new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED )
               , Type.TRIV ); 
        this.biasVal = 0.5;
    }
    
    
    public MultinomialLearner( double biasVal )
    {
        // super( modelType, iType )
        super( new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED )
               , Type.TRIV ); 
        this.biasVal = biasVal;
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value i, Value.Vector x, Value.Vector z )
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = getMultinomialModel((int)xType.LWB, (int)xType.UPB);
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
        
        // find MML estimate of params[i]
        for (int i = 0; i < params.length; i++) {
            params[i] = (params[i] + biasVal) / total;
        }
        
        return new Value.DefStructured( new Value[] {
                model, stats, new StructureFN.FastContinuousStructure(params) } );
    }
    
    
    
    /** return cost */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value y)
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = getMultinomialModel((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        
        return sCost( m, stats, (Value.Structured)y );
    }
    
    /** Parameterise and cost data all in one hit.   */
    public double parameterizeAndCost( Value i, Value.Vector x, Value.Vector z )
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = getMultinomialModel((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        
        return sParameterizeAndCost( multinomialModel, stats );
    }
    
    /** Parameterise and cost data all in one hit.   */
    public double sParameterizeAndCost( Value.Model m, Value s )
    {
        // sParameterize
        Value.Structured stats = (Value.Structured)s;
        
        double paramArray[] = new double[stats.length()];
        double total = (double)paramArray.length * biasVal;
        
        for (int i = 0; i < paramArray.length; i++) {        
            paramArray[i] = stats.doubleCmpnt(i);
            total += paramArray[i];
        }
        
        // find MML estimate of paramArray[i]
        for (int i = 0; i < paramArray.length; i++) {
            paramArray[i] = (paramArray[i] + biasVal) / total;
        }
        
        int arity = stats.length();
        double total2 = 0;
        double cost = 0;
        
        for(int i = 0; i < arity; i++) {        
            total2 += stats.doubleCmpnt(i);
        }
        
        if(total2 > 0) {
            // Multinomial Distribution MML Cost ... 
            
            // Hypothesis cost... 
            double h = Math.exp(cdms.core.FN.LogFactorial.logFactorial(arity-1));
            double f = 1.0 / paramArray[arity-1];
            
            for(int i  = 0; i < arity - 1; i++) {
                f *= total2;
                f /= paramArray[i];
            }
            
            cost = 0.5 * java.lang.Math.log(1 + (f/(java.lang.Math.pow(12,arity-1) * h * h)));
            cost += 0.5 * paramArray.length * Math.log(2);
            
            double logLH = 0;
            for(int i = 0; i < arity; i++)
                {
                    logLH -= stats.doubleCmpnt(i) * Math.log( paramArray[i] );
                }
            
            cost += logLH;
        }
        return cost;
    }
    
    /** return cost */
    public double sCost( Value.Model m, Value stats, Value params )
    {
        /*
         * What do I put here?
         * stats are already given.
         * params are already given
         * but upb and lwb of the Model are unknown...
         */
        
        // Ripped straight from Josh's code... (then modified)
        
        Value.Structured s = (Value.Structured)stats;
        Value.Structured modelParams = (Value.Structured)params;
        
        int arity = s.length();
        double total = 0;
        double cost = 0;
        
        for(int i = 0; i < arity; i++) {        
            total += s.doubleCmpnt(i);
        }
        
        if(total > 0) {
            // Multinomial Distribution MML Cost ... 
            
            // Hypothesis cost... 
            double h = Math.exp(cdms.core.FN.LogFactorial.logFactorial(arity-1));
            double f = 1.0 / ((Value.Scalar)modelParams.cmpnt(arity - 1)).getContinuous();
            for(int i  = 0; i < arity - 1; i++) {
                f *= total;
                f /= ((Value.Scalar)modelParams.cmpnt(i)).getContinuous();
            }
            
            cost = 0.5 * java.lang.Math.log(1 + (f/(java.lang.Math.pow(12,arity-1) * h * h)));
            cost += 0.5 * modelParams.length() * Math.log(2);
            
            double logLH = 0;
            for(int i = 0; i < arity; i++)
                {
                    logLH -= s.doubleCmpnt(i) * Math.log(modelParams.doubleCmpnt(i));
                }
            
            cost += logLH;
        }
        return cost;
    }
    
    /** Multinomial2 is the same as Multinomial but implementing GetNumParams */
    public static class Multinomial2 extends Multinomial implements GetNumParams
    { 
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -5565755932077386728L;

        public Multinomial2(int lwb, int upb) { super(lwb,upb); }
        public Multinomial2(Type.Discrete dataSpace) { super( dataSpace ); }
        
        /** return the number of free parameters. */
        public int getNumParams( Value params ) {
            return (int)(upb - lwb);
        }
    }
    
    /**
     * Lazily create instances of Multinomial.  Without this a new instance of Multinomial if
     * created every time parameterize is called.
     */
    public static Value.Model getMultinomialModel( int lwb, int upb ) {
        //tempKey.set(lwb,upb);
        Key tempKey = new Key(lwb,upb);
        Value.Model model = (Value.Model)modelHash.get( tempKey );
        if ( model == null ) {
            model = new Multinomial2(lwb,upb);
            modelHash.put( tempKey.clone(), model );
        }
        return model;
    }
    
    /**
     * Lazily create instances of Multinomial.  Without this a new instance of Multinomial if
     * created every time parameterize is called.
     */
    public static Value.Model getMultinomialModel( Type.Discrete t ) {
        //tempKey.set(lwb,upb);
        Value.Model model = (Value.Model)modelHash.get( t );
        if ( model == null ) {
            model = new Multinomial2(t);
            modelHash.put( t, model );
        }
        return model;
    }

    
    /**
     * modelHash is a hashtable containing instances of modelHash with various [lwb,upb] pairs 
     */
    protected static java.util.Hashtable modelHash = new java.util.Hashtable();
    
    /** Class used as key to modelHash */
    private static class Key implements Cloneable { 
        final int[] key = new int[2]; 
        public Key( int key1, int key2 ) { key[0] = key1; key[1] = key2; }
        public int hashCode() { return key[0] * key[1]; }
        public boolean equals( Object o ) { 
            Key k = (Key)o; 
            if (k.key[0]==this.key[0] && k.key[1]==this.key[1] ) return true; else return false; 
        }
        public Object clone() { return new Key(key[0],key[1]);    }
        public void set( int lwb, int upb ) { key[0] = lwb; key[1] = upb; }
    }
    
    public String toString() { return "MultinomialLearner"; }
    
    
    
    
    /** Default implementation of makeMultinomialLearner */
    public static final MakeMultinomialLearner makeMultinomialLearner = 
        new MakeMultinomialLearner();
    
    /** MakeMultinomialLearner returns a MultinomialLearner given a options. */
    public static class MakeMultinomialLearner extends MakeModelLearner
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2872870963528036888L;

        public MakeMultinomialLearner( ) { }
        
        /** Shortcut apply method */
        public ModelLearner _apply( String[] option, Value[] optionVal ) {  
            
            
            if ( option.length < 1 ) {  
                throw new RuntimeException("First argument must be adaptive or bde");
            }
            
            // Search options for overrides.
            if ( option[0].equals("adaptive") ) {
                return MakeAdaptiveCodeLearner.makeAdaptiveCodeLearner._apply( remove(option,0),
                                                                               remove(optionVal,0) );
            }
            else if ( option[0].equals("bde") ) {
                return BDELearner.makeBDELearner._apply( remove(option,0), remove(optionVal,0) );
            }
            else { throw new RuntimeException("First argument must be adaptive or bde");}
            
        }
        
        public String[] getOptions() { 
            String[] adaptiveOptions = MakeAdaptiveCodeLearner.makeAdaptiveCodeLearner.getOptions();
            String[] bdeOptions = BDELearner.makeBDELearner.getOptions();
            
            String[] options = new String[ adaptiveOptions.length + bdeOptions.length + 2];
            
            int x = 0;
            options[x] = "adaptive - Use adaptive Code learner with options as below:";
            x++;
            for ( int i = 0; i < adaptiveOptions.length; i++ ) {
                options[x] = "\t" + adaptiveOptions[i];
                x++;
            }
            
            options[x] = "bde - Use BDEe learner with options as below:";
            x++;
            for ( int i = 0; i < bdeOptions.length; i++ ) {
                options[x] = "\t" + bdeOptions[i];
                x++;
            }
            
            return options;
        }
        
    }
    
}

