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
// Functions to learn Duals CPt/DTree models from raw data.
//

// File: DualLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.dual;

import cdms.core.*;
import camml.core.models.ModelLearner;
import camml.core.models.cpt.*;
import camml.core.models.dTree.*;
import camml.core.models.logit.LogitLearner;
import camml.core.models.multinomial.*;

/**
 * DualLearner chooses between a Decision Tree and a CPT (possibly more in future) and returns the
 * model which best fits the data. <br>
 *
 * To do this is requires two ModelLearner instances (passed in constructor). To choose a model
 * Dual parameterizes and costs the data with both ModelLearner functions and selects the model
 * with the lower cost.  <br>
 *
 * To get a true MML cost we need to add ln(2) nits to the final cost to represent the selection
 * between the two competing models (uniform prior assumed).  In the case where there are no parent
 * variables there is no need to add this constant as a CPT and DTree are identical for this case.
 */
public class DualLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -5173499146899569056L;

    /** 'Dual' learner using CPT, DTree and Logit. */
    public final static ModelLearner dualCPTDTreeLogitLearner = new DualLearner(
                                                                                new ModelLearner[] {AdaptiveCodeLearner.mmlAdaptiveCodeLearner},
                                                                                new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner},
                                                                                new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner,
                                                                                                    ForcedSplitDTreeLearner.multinomialDTreeLearner,
                                                                                                    LogitLearner.logitLearner} );
    
    /** 'Dual' learner using CPT and DTree */
    public final static ModelLearner dualCPTDTreeLearner = new DualLearner(
                                                                           new ModelLearner[] {AdaptiveCodeLearner.mmlAdaptiveCodeLearner},
                                                                           new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner},
                                                                           new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner,
                                                                                               ForcedSplitDTreeLearner.multinomialDTreeLearner} );

    /** 'Dual' learner using DTree and Logit. */
    public final static ModelLearner dualCPTLogitLearner = new DualLearner(
                                                                           new ModelLearner[] {AdaptiveCodeLearner.mmlAdaptiveCodeLearner},
                                                                           new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner},
                                                                           new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner,
                                                                                               LogitLearner.logitLearner} );

    /** 'Dual' learner using DTree and Logit. */
    public final static ModelLearner dualDTreeLogitLearner = new DualLearner(
                                                                             new ModelLearner[] {AdaptiveCodeLearner.mmlAdaptiveCodeLearner},
                                                                             new ModelLearner[] {CPTLearner.mmlAdaptiveCPTLearner},
                                                                             new ModelLearner[] {ForcedSplitDTreeLearner.multinomialDTreeLearner,
                                                                                                 LogitLearner.logitLearner} );
        
    /** standard 'Dual' learner using CPT and DTree */
    public final static ModelLearner dualLearner = dualCPTDTreeLearner;
    
    public String getName() { return "DualLearner"; }    
    
    /** a list of all the different modelLearner used in the dual model. */
    //ModelLearner[] modelLearnerList;
    
    final ModelLearner[] zeroParentLearner;
    final ModelLearner[] singleParentLearner;
    final ModelLearner[] multiParentLearner;
    
    
    public DualLearner(ModelLearner[] zeroParentLearner,
                       ModelLearner[] singleParentLearner,
                       ModelLearner[] multiParentLearner) {
        super(makeModelType(), Type.TRIV );
        this.zeroParentLearner = zeroParentLearner;
        this.singleParentLearner = singleParentLearner;
        this.multiParentLearner = multiParentLearner;
    }
    
    /** Defualt constructor, same as DualLearner( CPTLearner.mmlAdaptiveCPTLearner,
     *                                         DTreeLearner.multinomialDTreeLearner );
     */
    public DualLearner( )
    {
        this( CPTLearner.mmlAdaptiveCPTLearner, 
              ForcedSplitDTreeLearner.multinomialDTreeLearner,
              AdaptiveCodeLearner.mmlAdaptiveCodeLearner );
        //super( makeModelType(), Type.TRIV );
        //this.modelLearnerList = new ModelLearner[] { CPTLearner.mmlAdaptiveCPTLearner,
        //        ForcedSplitDTreeLearner.multinomialDTreeLearner, 
        //        AdaptiveCodeLearner.mmlAdaptiveCodeLearner };

    }
    
    
    /** Create a dual (CPT DTree*/
    public DualLearner( ModelLearner cptLearner, ModelLearner dTreeLearner, ModelLearner leafModelLearner )
    {        
        this(    new ModelLearner[] {leafModelLearner},
                 new ModelLearner[] {cptLearner},
                 new ModelLearner[] {cptLearner,dTreeLearner} );
        //super( makeModelType(), Type.TRIV );
        //this.modelLearnerList = new ModelLearner[] { cptLearner, dTreeLearner, leafModelLearner };
        
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
        Value.Structured bestParameterization = null;
        double bestCost = Double.POSITIVE_INFINITY;
        
        Type.Structured inputType = (Type.Structured)((Type.Vector)z.t).elt;
        int numInputs = inputType.cmpnts.length;
        
        ModelLearner[] modelLearnerList;
        
        if (numInputs == 0) { modelLearnerList = zeroParentLearner; }
        else if (numInputs == 1) { modelLearnerList = singleParentLearner; }
        else { modelLearnerList = multiParentLearner; }
        
        /*
          if ( numInputs == 0 ) {
          // If there are no inputs, always use multinomialModelLearner.
          bestParameterization = modelLearnerList[2].parameterize( initialInfo, x, z );            
          }
          else if ( numInputs == 1 ) {
          // For a single input CPT and DTree are identical, so use a CPT.
          bestParameterization = modelLearnerList[0].parameterize( initialInfo, x, z );
          }
          else {
        */
        // loop through modelLearnerList and return the parameters with the best cost.
        // do not attempt to cost as a multinomial is numInputs > 0
        for ( int i = 0; i < modelLearnerList.length; i++ ) {
            Value.Structured params;
            double cost;
                
            // If one parameterization method fails (excessive CPT combinations, etc) then simply
            // try the other methods instead.
            try {
                params = modelLearnerList[i].parameterize( initialInfo, x, z );
                //cost =  modelLearnerList[i].msyCost( params );
                cost = modelLearnerList[i].parameterizeAndCost(initialInfo,x,z);
            }
            catch ( LearnerException e ) {
                params = null;
                cost = Double.POSITIVE_INFINITY;
            }
                
            if ( cost < bestCost ) {
                bestParameterization = params;
                bestCost = cost;
            }
        }    
            
        //}    
        if ( bestParameterization == null ) {
            throw new LearnerException("All parameterization attempts failed.");
        }
        
        return bestParameterization;
    }
    
    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        double bestCost = Double.POSITIVE_INFINITY;
        
        Type.Structured inputType = (Type.Structured)((Type.Vector)z.t).elt;
        int numInputs = inputType.cmpnts.length;
        
        ModelLearner[] modelLearnerList;
        if (numInputs == 0) { modelLearnerList = zeroParentLearner; }
        else if (numInputs == 1) { modelLearnerList = singleParentLearner; }
        else { modelLearnerList = multiParentLearner; }

        /*
          if ( numInputs == 0 ) {
          // If there are no inputs, always use multinomialModelLearner.
          bestCost = modelLearnerList[2].parameterizeAndCost( initialInfo, x, z );
          }
          else if ( numInputs == 1 ) {
          // For a single input CPT and DTree are identical, so use a CPT.
          bestCost = modelLearnerList[0].parameterizeAndCost( initialInfo, x, z );
          }
          else {
        */    
        // loop through modelLearnerList and return the parameters with the best cost.
        // do not attempt to cost as a multinomial is numInputs > 0
        for ( int i = 0; i < modelLearnerList.length; i++ ) {
            double cost;
                
            // If one parameterization method fails (excessive CPT combinations, etc) then 
            // simply try the other methods instead.
            try {
                cost = modelLearnerList[i].parameterizeAndCost(initialInfo,x,z);
            }
            catch ( LearnerException e ) {
                cost = Double.POSITIVE_INFINITY;
            }
                
            if ( cost < bestCost ) {
                bestCost = cost;
            }
        }            
        //}    
        return bestCost + Math.log(modelLearnerList.length);
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
        /*
          double cost;
          int numParents;
          if ( m instanceof CPT ) {
          cost =  modelLearnerList[0].sCost( m, s, y );
          numParents = ((CPT)m).getNumParents();
          }
          else if ( m instanceof DTree ) {
          cost =  modelLearnerList[1].sCost( m, s, y );
          numParents = ((DTree)m).getNumParents( s );
          }
          else if ( m instanceof cdms.plugin.model.Multinomial ) {
          cost = modelLearnerList[2].sCost( m, s, y );
          numParents = 0;
          }
          else {
          throw new RuntimeException("Dual does not recognise model : " + m);
          }
        
          // For 0 parents we always use Multinomial
          // For 1 parent DTree and CPT are identical, so use a CPT
          // For > 1 parents a decision must be made and it costs 1 bit (log(2) nits) to state result.
          if ( numParents > 1 ) {
          cost += Math.log(2);
          }
        
          return cost;
        */
    }
    
    /** DualLearner( [modelLearner[i].toString()] )*/
    public String toString() {
        ModelLearner modelLearnerList[] = multiParentLearner;
        String s = "DualLearner(";
        for ( int i = 0; i < modelLearnerList.length; i++ ) {
            if ( i != 0 ) 
                s += modelLearnerList[i].toString();
            else
                s += ", " + modelLearnerList[i].toString();
        } 
        s += ")";
        return s;
    }    
}

