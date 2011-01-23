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
// Functions to learn best model given several learning options.
//

// File: MultiLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.dual;

import cdms.core.*;
import camml.core.models.*;

/**
 * MultiLearner acts as a more flexible replacement for DualLearner.
 *
 * MultiLearner tests if each model is validly applicable to the dataset before attempting to learn
 *  with it and pays log(m) bits to state the learner used where m is the number of valid learning
 *  metrics available.
 */
public class MultiLearner extends ModelLearner.DefaultImplementation
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 2841973782152954630L;

    public String getName() { return "MultiLearner"; }    
    
    /** a list of all the different modelLearner used in the multi model. */
    final ModelLearner[] modelLearnerList;
    
    final Value.Function priorFN;
    
    /** Create a multi (CPT DTree*/
    public MultiLearner( ModelLearner[] modelLearnerList, Value.Function priorFN )
    {
        super( makeModelType(), Type.TRIV );
        this.modelLearnerList = modelLearnerList;
        this.priorFN = priorFN;
    }
    
    /** Create a (very generic) type */
    protected static Type.Model makeModelType( )
    {
        //Type.Model subModelType = Type.MODEL;
        Type dataSpace = Type.TYPE;
        Type paramSpace = Type.TYPE;
        Type sharedSpace = Type.TYPE;
        Type sufficientSpace = new Type.Structured( new Type[] {dataSpace, sharedSpace} );
        
        return new Type.Model(dataSpace, paramSpace, sharedSpace, sufficientSpace);
        
    }
    
    /**
     * Call the getPrior function to find prior over search heuristic space given (i,x,z) <br>
     * if (priorFN == null) a uniform prior is returned.
     */
    protected double[] getPrior( Value ii, Value.Vector x, Value.Vector z ) {
        // Initialise prior.
        double[] prior = new double[modelLearnerList.length];
        if ( priorFN == null ) { 
            for ( int i = 0; i < prior.length; i++ ) { 
                prior[i] = 1.0/prior.length;
            }
        }
        else {
            Value.Vector priorVec = 
                (Value.Vector)priorFN.apply( new Value.DefStructured(new Value[] {ii,x,z}) );
            if ( prior.length != modelLearnerList.length ) { 
                throw new RuntimeException("prior length mismatch in MultiLearner");
            }
            for ( int i = 0; i < prior.length; i++ ) {        
                prior[i] = priorVec.doubleAt(i);
            }
        }
        return prior;
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        double[] prior = getPrior( initialInfo,x,z );
        
        Value.Structured bestMSY = null;
        double bestCost = Double.POSITIVE_INFINITY;
        
        for ( int i = 0; i < modelLearnerList.length; i++ ) {
            // if ( modelLearnerList[i].isParameterizable(initialInfo,x,z) ) {
            
            // if (prior != 0) 
            if ( (prior[i] != 0) ) {
                Value.Structured msy;
                double cost;
                try {
                    msy = modelLearnerList[i].parameterize( initialInfo, x, z );
                    cost = modelLearnerList[i].msyCost( msy ) - Math.log( prior[i] );
                }
                catch ( LearnerException e ) {
                    msy = null;
                    cost = Double.POSITIVE_INFINITY;
                }
                
                if ( cost < bestCost ) {
                    bestMSY = msy;
                    bestCost = cost;
                }
            }
        }
        
        if ( bestMSY == null ) {
            throw new LearnerException("All parameterization attempts failed.");
        }
        
        return bestMSY;
    }
    
    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        double[] prior = getPrior( initialInfo,x,z );
        double bestCost = Double.POSITIVE_INFINITY;
        
        for ( int i = 0; i < modelLearnerList.length; i++ ) {
            if ( prior[i] != 0) {
                double cost;
                try {
                    cost = modelLearnerList[i].parameterizeAndCost( initialInfo, x, z ) -
                        Math.log( prior[i] );
                }
                catch ( LearnerException e ) {
                    cost = Double.POSITIVE_INFINITY;
                }
                
                if ( cost < bestCost ) {
                    bestCost = cost;
                }
            }
        }
        
        if ( bestCost == Double.POSITIVE_INFINITY ) {
            throw new LearnerException("All parameterization attempts failed.");
        }
        
        return bestCost;
    }
    
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
        throws LearnerException
    {    
        throw new RuntimeException("Not implemented.");
    }
    
    /** return cost */
    public double cost(Value.Model m, Value initialInfo, Value.Vector x, Value.Vector z, Value y)
        throws LearnerException
    {
        throw new RuntimeException("Not implemented");
    } 
    
    /**
     * sCost uses the Value.Model m to determine which costing metric needs to be used.  
     * ie. if m is a CPT, then CPTLearner is used, if m is a DTree then DTreeLearner is used.
     * if m is neither, an exception is thrown.
     */
    public double sCost( Value.Model m, Value s, Value y )
        throws LearnerException
    {
        throw new RuntimeException("Not implemented");
    }
    
    /** MultiLearner( [modelLearner[i].toString()] )*/
    public String toString() { 
        String s = "MultiLearner(";
        for ( int i = 0; i < modelLearnerList.length; i++ ) {
            if ( i != 0 ) 
                s += modelLearnerList[i].toString();
            else
                s += ", " + modelLearnerList[i].toString();
        } 
        s += ")";
        return s;
    }    
    
    
    
    
    /** Default implementation of makeMultiLearner */
    public static final MakeMultiLearner makeMultiLearner = new MakeMultiLearner();
    
    /** MakeMultiLearner returns a MultiLearner given a options. */
    public static class MakeMultiLearner extends MakeModelLearner
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2561326934344955923L;

        public MakeMultiLearner( ) { }
        
        /** Shortcut apply method */
        public ModelLearner _apply( String[] option, Value[] optionVal ) {  
            
            Value.Function priorFN = null;
            ModelLearner[] learnerList;
            
            if ( option.length < 1 ) {  
                throw new RuntimeException("MultiLearner with no options impossible.");
            }
            
            // Search options for overrides.
            if ( option[0].equals("learner") ) {
                Value.Vector val = (Value.Vector)optionVal[0];
                learnerList = new ModelLearner[ val.length() ];
                for ( int i = 0; i < learnerList.length; i++ ) {
                    learnerList[i] = (ModelLearner)((FunctionStruct)val.elt(i)).getLearner();
                }
            }
            else { throw new RuntimeException("First argument must be \"learner\""); }
            
            
            // priorFN: (i,x,z) -> [prob]
            if ( (option.length >= 2) && option[1].equals("prior") ) {
                priorFN = (Value.Function)optionVal[1];
            }
            
            return new MultiLearner( learnerList, priorFN );                 
        }
        
        public String[] getOptions() { return new String[] {
                "learner - A [learnerStruct] is required",
                "priot - (i,x,z) -> [continuous], if no function is passed, uniform prior is assumed."
            }; }
    }
}

