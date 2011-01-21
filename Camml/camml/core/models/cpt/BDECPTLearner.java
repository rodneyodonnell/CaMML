//
// Functions to learn CPT's from raw data.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: CPTLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.cpt;

import cdms.core.*;
import camml.core.models.multinomial.*;

/**
 * BDECPTLearner is similar to CPTLearner but used BDE with an equivalent sample
 * size.  BDE is similar to adaptive code, but instead of using +1 as a bias for each
 * state we use +N'/s where N` is the "Equivalent Sample Size", and s is the number
 * of joint parent/child states.  Factorials in Adaptive Code are replaced by
 * gamma functions with the property f(n) = n*f(n-1) but are also defined for
 * non-integer values. <br>
 * 
 * See "Heckerman & Geiger '95" for a more detailed (and accurate?) summary. 
 * 
 * @see camml.core.models.cpt.CPTLearner
 */
public class BDECPTLearner extends CPTLearner
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7260117236072326925L;
    /** Equivalent sample size */
    public final double ess;
    
    /** Constructor : mmlAdaptiveCodeLearner is default leafModelLearner */
    public BDECPTLearner( double ess )
    {
        // This BDELearner specified in the constructor should never
        // be used as it has the wrong value for ess.
        // NaN chosen as it should produce NaN whenever
        // the BDELearner is queried.
        super( new BDELearner(Double.NaN) );
        
        this.ess = ess;
    }
            
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value stats )
        throws LearnerException
    {    
        // Explicitly typecast variables to true types.
        CPT cptModel = (CPT)model;
        Value.Vector statVector = (Value.Vector)stats;
        
        
        BDELearner l = new BDELearner(ess/cptModel.numCombinations);
        
        // Loop through all parent combinations from stats and use leafModelLearner to parameterize
        Value[] paramArray = new Value[cptModel.numCombinations];        
        for (int i = 0; i < cptModel.numCombinations; i++ ) {
            Value.Structured leafMSY = 
                l.sParameterize( cptModel.childModel, statVector.elt(i) );
            paramArray[i] = new Value.DefStructured( new Value[] {leafMSY.cmpnt(0),
                                                                  leafMSY.cmpnt(2)} );
        }        
        Value.Vector paramVector = new VectorFN.FatVector( paramArray );
        
        // return (m,s,y)
        return new Value.DefStructured( new Value[] {cptModel, statVector, paramVector} );
    }
    
    
    /** return cost */
    public double sCost( Value.Model m, Value s, Value y )
        throws LearnerException
    {
        CPT cptModel = (CPT)m;
        Value.Vector statVector = (Value.Vector)s;
        Value.Vector paramVector = (Value.Vector)y;
        
        double totalCost = 0;

        BDELearner l = new BDELearner(ess/cptModel.numCombinations);
        
        for (int i = 0; i < cptModel.numCombinations; i++ ) {
            totalCost += l.sCost( cptModel.childModel, statVector.elt(i), 
                                  ((Value.Structured)paramVector.elt(i)).cmpnt(1) );
        }
        
        return totalCost;}

    
    /** Parameterize and cost data all in one hit.   */
    public double sParameterizeAndCost( Value.Model m, Value s )
        throws LearnerException {
        
        CPT cptModel = (CPT)m;
        Value.Vector stats = (Value.Vector)s;

        BDELearner l = new BDELearner(ess/cptModel.numCombinations);
        
        // calculate total cost using leafModelLearner.
        double totalCost = 0;        
        for (int i = 0; i < cptModel.numCombinations; i++ ) {
            // Each element of stats vector cottesponds to sufficient stats for subModel.
            totalCost += l.sParameterizeAndCost( cptModel.childModel, stats.elt(i) );            
        }
        
        return totalCost;    
    }

    /** return "CPTLearner(leafModelLearner)"*/
    public String toString() { return "CPTLearner("+leafModelLearner+")"; }
    
    /** return "CPTLearner" */
    public String getName() { return "CPTLearner"; }    
    
}
