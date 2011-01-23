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
// Wrapper function for learning with interventional and observational data
//

// File: InterventionLearner.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.intervention;

import cdms.core.*;
import camml.core.library.SelectedVector;
import camml.core.models.ModelLearner;
import camml.core.models.cpt.*;
import camml.core.models.dual.DualLearner;

/**
 * InterventionLearner acts as a wrapper around any ModelLearner to allow it to learn
 * given interventional data.  Essentially, all datums with an intervention on the 
 * target node are filtered out as their values are known a priori and as such
 * need not be costed.
 */
public class InterventionLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -4671434843121864321L;

    /** 'intervention' learner using MML CPTs as a sublearner */
    public final static ModelLearner mmlInterventionCPTLearner = 
        new InterventionLearner(CPTLearner.mmlAdaptiveCPTLearner);
    
    /** 'intervention' learner using ML CPTs as a sublearner */
    public final static ModelLearner mlInterventionCPTLearner = 
        new InterventionLearner(CPTLearner.mlMultinomialCPTLearner);
            
    /** 'intervention' learner using hyrbid models */
    public final static ModelLearner mmlInterventionHybridLearner = 
        new InterventionLearner(DualLearner.dualCPTDTreeLogitLearner);

    public String getName() { return "interventionLearner"; }    
    
    final ModelLearner subModelLearner;
    
    
    public InterventionLearner(ModelLearner subModelLearner ) {
        super(makeModelType(), Type.TRIV );
        this.subModelLearner = subModelLearner;
    }
        
    protected static Type.Model makeModelType( )
    {
        //Type.Model subModelType = Type.MODEL;
        Type dataSpace = Type.DISCRETE;
        Type paramSpace = Type.VECTOR;
        Type sharedSpace = Type.STRUCTURED;
        Type sufficientSpace = new Type.Structured( new Type[] {dataSpace, sharedSpace} );
        
        return new Type.Model(dataSpace, paramSpace, sharedSpace, sufficientSpace);        
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        // Filter out interventions on x and run subModelLearner
        int[] rows = filterX(x);
        Value.Vector x2 = new SelectedVector(x,rows,null);
        Value.Vector z2 = new SelectedVector(z,rows,null);
        return subModelLearner.parameterize(initialInfo, x2, z2);
    }
    
    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        // Filter out interventions on x and run subModelLearner
        int[] rows = filterX(x);
        Value.Vector x2 = new SelectedVector(x,rows,null);
        Value.Vector z2 = new SelectedVector(z,rows,null);
        return subModelLearner.parameterizeAndCost(initialInfo, x2, z2);
    }
    
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
        throws LearnerException
    {    
        // What is the best way to do this?
        throw new RuntimeException("Not Implemented");
    }
    
    /** return cost */
    public double cost(Value.Model m, Value initialInfo, Value.Vector x, Value.Vector z, Value y)
        throws LearnerException
    {
        return sCost(m, m.getSufficient(x,z), y);
    } 
    
    /**
     * sCost uses the Value.Model m to determine which costing metric needs to be used.  
     * ie. if m is a CPT, then CPTLearner is used, if m is a DTree then DTreeLearner is used.
     * if m is neither, an exception is thrown.
     */
    public double sCost( Value.Model m, Value s, Value y )
        throws LearnerException
    {
        throw new RuntimeException("sCost not implemented.");
    }
    
    /** interventionLearner( [modelLearner[i].toString()] )*/
    public String toString() {
        return "interventionLearner(" + subModelLearner + ")";
    }    
    
    /** Return array or rows containing non-intervention values for x.
     *  null is returned if no interventions found */
    public static int[] filterX( Value.Vector x) {
        
        int[] rows = new int[x.length()];
        int ii = 0;
        for (int i = 0; i < rows.length; i++) {
            ValueStatus status = x.elt(i).status();
            if ( status == Value.S_PROPER) { 
                rows[ii] = i;
                ii++;
            }
            else if ( status == Value.S_INTERVENTION) {
                // Do nothing
            }
            else {
                throw new RuntimeException("Unhandled Value Status.");
            }
        }
        
        // If no interventions in 'x'
        if (ii == rows.length) {
            return null;
        }
        else {
            int[] rows2 = new int[ii];
            System.arraycopy(rows,0,rows2,0,ii);
            return rows2;
        }        
    }        
}

