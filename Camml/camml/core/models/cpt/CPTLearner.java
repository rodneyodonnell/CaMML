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
import camml.core.models.*;
import camml.core.models.multinomial.*;
import camml.core.models.normal.NormalLearner;

/**
 * CPTLearner is a standard module for parameterizing and costing CPTs. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br> 
 * <br>
 * A CPT is learned by splitting the data based on it's parents, then using a "leafLearner" 
 * to parameterize the date given each parent combination (This is the same as each leaf of a fully
 * split decision tree).  The ModelLearner passed in the constructor is used to parameterize each 
 * leaf and should have a "shared space" (aka. parentSpace or z) of Value.TRIV
 */
public class CPTLearner extends ModelLearner.DefaultImplementation
{
    //////////////////////////////////////////////////////////////////////////////////////
    // Static instances of CPTLearner.  The four adaptiveCPTLearners use adaptive code. //
    // Those marked as mml contain a correction of (|x|-1)*log(Pi*e/6)/2 when costing.  //
    // I believe this corresponds to a bound on the strict MML estimate, and is the     //
    // formula used in oldcamml.  Those marked with a 2 use (n+1)/(N+m) for parameters  //
    // as opposed to (n+0.5)/(N+m/2).                                                   //
    //////////////////////////////////////////////////////////////////////////////////////
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 8820841968948657964L;

    /** Learn CPT using Adaptive Code, +0.5 in parameterization */
    public static CPTLearner adaptiveCPTLearner = 
        new CPTLearner( AdaptiveCodeLearner.adaptiveCodeLearner );
    
    /** Learn CPTs using Adaptive code with MML correction, +0.5 in parameterization */
    public static CPTLearner mmlAdaptiveCPTLearner = 
        new CPTLearner( AdaptiveCodeLearner.mmlAdaptiveCodeLearner );
    
    /** Learn CPTs using Adaptive code, +1.0 in parameterization*/
    public static CPTLearner adaptiveCPTLearner2 = 
        new CPTLearner( AdaptiveCodeLearner.adaptiveCodeLearner2 );
    
    /** Learn CPTs using Adaptive code with MML correction, +1.0 in parameterization*/
    public static CPTLearner mmlAdaptiveCPTLearner2 = 
        new CPTLearner( AdaptiveCodeLearner.mmlAdaptiveCodeLearner2 );
    
    /** Learn CPTs using cdms.mml87.multinomialParameterizer */
    public static CPTLearner multinomialCPTLearner = 
        new CPTLearner( MultinomialLearner.multinomialLearner );
    
    /** Learn CPTs using Maximum Likelyhood */
    public static CPTLearner mlMultinomialCPTLearner = 
        new CPTLearner( MLMultinomialLearner.mlMultinomialLearner );
    
    /** Learn CPTs with Normal Distributions in the leaves */
    public static CPTLearner normalCPTLearner = new CPTLearner( NormalLearner.normalLearner );
    
    /**
     * The Learning and costing functions used in each leaf of the CPT. (where Leaf = all data for
     * a single combination of parent states).
     */
    protected ModelLearner leafModelLearner; 

    /** Constructor : mmlAdaptiveCodeLearner is default leafModelLearner */
    public CPTLearner( )
    {
        super( makeModelType( AdaptiveCodeLearner.mmlAdaptiveCodeLearner ), Type.TRIV );
        this.leafModelLearner = AdaptiveCodeLearner.mmlAdaptiveCodeLearner;
    }
    
    /** Constructor using alternate leafModelLearner */
    public CPTLearner( ModelLearner leafModelLearner )
    {
        super( makeModelType(leafModelLearner), Type.TRIV );        
        this.leafModelLearner = leafModelLearner;
    }
    
    /** Create model type for CPT */
    protected static Type.Model makeModelType( ModelLearner leafModelLearner )
    {
        Type.Model subModelType = leafModelLearner.getModelType();
        Type dataSpace = subModelType.dataSpace;
        Type paramSpace = new Type.Vector( subModelType.paramSpace );
        Type sharedSpace = Type.STRUCTURED;
        Type sufficientSpace = new Type.Structured( new Type[] {dataSpace, sharedSpace} );
        
        return new Type.Model(dataSpace, paramSpace, sharedSpace, sufficientSpace);        
    }

    /** Extract upper and lower bounds from vec of type [(D,D,D)] <br>
     *  ret[0] = LWBs, ret[1] = UPBs */
    public static int[][] getBounds(Value.Vector vec) {
        Type.Structured zType = (Type.Structured)((Type.Vector)vec.t).elt;
        int[] lwbArray = new int[zType.cmpnts.length];
        int[] upbArray = new int[zType.cmpnts.length];
        
        for ( int i = 0; i < zType.cmpnts.length; i++ ) {
            Type.Discrete zEltType = (Type.Discrete)zType.cmpnts[i];    
            lwbArray[i] = (int)zEltType.LWB;
            upbArray[i] = (int)zEltType.UPB;
        }
        return new int[][] {lwbArray,upbArray};
    }
    
    /** Determine type of model used my ModelLearner with given inputs.     */
    public static Value.Model getChildModel(Value.Vector x, ModelLearner l) 
        throws ModelLearner.LearnerException {
        // We must figure out what the sub model type is.
        // This is not necesaritly as easy as it sounds as we do not know what
        // type of model leafModelLearner will return.
        
        // CPTs are usually used with multinomial types.
        if ( l instanceof AdaptiveCodeLearner ||
             l instanceof BDELearner ||
             l instanceof MultinomialLearner) {
            Type.Discrete xType = (Type.Discrete)(((Type.Vector)x.t).elt);
            return MultinomialLearner.getMultinomialModel(xType);
        }
        
        // If ModelLearner is something unexpected, the only way to work it out
        // is by running learner.
        Value.Vector emptyVector = new VectorFN.FatVector( new Value[] {}, (Type.Vector)x.t);
        Value.Structured tempMSY = l.parameterize(Value.TRIV, emptyVector, emptyVector);
        Value.Model childModel = (Value.Model)tempMSY.cmpnt(0);
        return childModel;
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        // determine upper and lower bounds of z
        int[][] bounds = getBounds(z);
        int[] lwbArray = bounds[0];
        int[] upbArray = bounds[1];                    
        
        // Create child model used in CPT cells.
        Value.Model childModel = getChildModel(x,leafModelLearner);
        
        try {
            CPT cptModel = new CPT( childModel, lwbArray, upbArray );
            Value sufficientStats = cptModel.getSufficient(x,z);
            return sParameterize(cptModel, sufficientStats );
        }
        catch ( CPT.ExcessiveCombinationsException e ) {
            throw new LearnerException( e );
        }
        
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value stats )
        throws LearnerException
    {    
        // Explicitly typecast variables to true types.
        CPT cptModel = (CPT)model;
        Value.Vector statVector = (Value.Vector)stats;
        
        // Loop through all parent combinations from stats and use leafModelLearner to parameterize
        Value[] paramArray = new Value[cptModel.numCombinations];        
        for (int i = 0; i < cptModel.numCombinations; i++ ) {
            Value.Structured leafMSY = 
                leafModelLearner.sParameterize( cptModel.childModel, statVector.elt(i) );
            paramArray[i] = new Value.DefStructured( new Value[] {leafMSY.cmpnt(0),
                                                                  leafMSY.cmpnt(2)} );
        }        
        Value.Vector paramVector = new VectorFN.FatVector( paramArray );
        
        // return (m,s,y)
        return new Value.DefStructured( new Value[] {cptModel, statVector, paramVector} );
    }
    
    /** return cost */
    public double cost(Value.Model m, Value initialInfo, Value.Vector x, Value.Vector z, Value y)
        throws LearnerException
    {
        return sCost(m, m.getSufficient(x,z), y);
    } 
    
    /** return cost */
    public double sCost( Value.Model m, Value s, Value y )
        throws LearnerException
    {
        CPT cptModel = (CPT)m;
        Value.Vector statVector = (Value.Vector)s;
        Value.Vector paramVector = (Value.Vector)y;
        
        double totalCost = 0;
        
        for (int i = 0; i < cptModel.numCombinations; i++ ) {
            totalCost += 
                leafModelLearner.sCost( cptModel.childModel, statVector.elt(i), 
                                        ((Value.Structured)paramVector.elt(i)).cmpnt(1) );
        }
        
        return totalCost;}

    /** Parameterize and cost data all in one hit.   */
    public double parameterizeAndCost( Value info, Value.Vector x, Value.Vector z )
        throws LearnerException {
        // determine upper and lower bounds of z
        int[][] bounds = getBounds(z);
        int[] lwbArray = bounds[0];
        int[] upbArray = bounds[1];                    
        
        // Create child model used in CPT cells.
        Value.Model childModel = getChildModel(x,leafModelLearner);
                    
        try {
            CPT cptModel = new CPT( childModel, lwbArray, upbArray );
            Value sufficientStats = cptModel.getSufficient(x,z);
            return sParameterizeAndCost( cptModel, sufficientStats );
        }
        catch ( CPT.ExcessiveCombinationsException e ) {
            throw new LearnerException( e );
        }
    }
    
    /** Parameterize and cost data all in one hit.   */
    public double sParameterizeAndCost( Value.Model m, Value s )
        throws LearnerException {
        
        CPT cptModel = (CPT)m;
        Value.Vector stats = (Value.Vector)s;
        
        // calculate total cost using leafModelLearner.
        double totalCost = 0;        
        for (int i = 0; i < cptModel.numCombinations; i++ ) {
            // Each element of stats vector cottesponds to sufficient stats for subModel.
            totalCost += 
                leafModelLearner.sParameterizeAndCost( cptModel.childModel, stats.elt(i) );            
        }
        
        return totalCost;    
    }

    /** return "CPTLearner(leafModelLearner)"*/
    public String toString() { return "CPTLearner("+leafModelLearner+")"; }
    
    /** return "CPTLearner" */
    public String getName() { return "CPTLearner"; }    
    
}
