//
// Wrapper for cdms.mml87.multinomialParameterizer
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MLMultinomialLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import cdms.core.*;
import camml.core.library.*;

import cdms.plugin.model.*;
// import cdms.plugin.mml87.*;
import camml.core.models.ModelLearner;

/**
 * MLMultinomialLearner is a wrapper class of type ModelLearner. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br>
 */
public class MLMultinomialLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 377380353677126451L;
    /** Static instance of class */
    public static MLMultinomialLearner mlMultinomialLearner = new MLMultinomialLearner();
    
    public String getName() { return "MLMultinomialLearner"; }    
    
    
    public MLMultinomialLearner()
    {
        // super( modelType, iType )
        super( new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED )
               , Type.TRIV ); 
    }
    
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
        Value.Structured stats = (Value.Structured)s;
        
        double params[] = new double[stats.length()];
        double total = 0;
        
        for (int i = 0; i < params.length; i++) {        
            params[i] = stats.doubleCmpnt(i);
            total += params[i];
        }
        
        // find MML estimate of params[i]
        for (int i = 0; i < params.length; i++) {
            params[i] = (params[i]) / total;
        }
        
        return new Value.DefStructured( new Value[] {
                model, stats, new StructureFN.FastContinuousStructure(params) } );
    }
    
    
    
    /** return cost */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value y)
    {
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        Value.Model multinomialModel = new Multinomial((int)xType.LWB, (int)xType.UPB);
        Value.Structured stats = (Value.Structured)multinomialModel.getSufficient(x,z);
        
        return sCost( m, stats, (Value.Structured)y );
    }
    
    /** return cost */
    public double sCost( Value.Model m, Value stats, Value params )
    {
        Value.Structured s = (Value.Structured)stats;
        Value.Structured y = (Value.Structured)params;
        
        double total = 0;
        for ( int i = 0; i < s.length(); i++ ) {
            int count = s.intCmpnt(i);
            
            if ( count != 0 ) {
                total -= java.lang.Math.log( y.doubleCmpnt(i) ) * count;
            }
        }
        return total;
        
    }
    
    public String toString() { return "MLMultinomialLearner";}
}

