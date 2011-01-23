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
// Functions to learn CPT's from raw data.
//

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
