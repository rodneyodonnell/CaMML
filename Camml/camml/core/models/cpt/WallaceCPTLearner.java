//
// Functions to learn CPT's from raw data.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: WallaceCPTLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.cpt;

import cdms.core.*;
import cdms.core.FN.LogFactorial;

import camml.core.models.ModelLearner;
import camml.core.models.multinomial.MultinomialLearner;

/**
 * WallaceCPTLearner is a standard module for parameterizing and costing CPTs. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br> 
 * <br>
 * A CPT is learned by splitting the data based on it's parents, then using a "leafLearner" 
 * to parameterize the date given each parent combination (This is the same as each leaf of a fully
 * split decision tree).  The ModelLearner passed in the constructor is used to parameterize each 
 * leaf and must have a "shared space" (aka. parentSpace or z) of Value.TRIV
 */
public class WallaceCPTLearner extends ModelLearner.DefaultImplementation
{
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5478602280691749409L;

	/** Static instance of CPT using Multistates in the leaves */
	public static WallaceCPTLearner multinomialCPTLearner = new WallaceCPTLearner( );
	
	/** Static instance of CPT using Multistates in the leaves */
	public static WallaceCPTLearner mekldLearner = 
		new WallaceCPTLearner( new MultinomialLearner(1.0) );
	
	public String getName() { return "WallaceCPTLearner"; }    
	
	ModelLearner leafModelLearner;
	
	public WallaceCPTLearner( ModelLearner leafModelLearner )
	{
		super( makeModelType(), Type.TRIV );
		this.leafModelLearner = leafModelLearner;
	}
	
	public WallaceCPTLearner( )
	{
		this(MultinomialLearner.multinomialLearner);
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
		CPT cptModel;
		Value sufficientStats;
		try {
			cptModel = makeCPT( x, z );
			sufficientStats = cptModel.getSufficient(x,z);
		}
		catch ( CPT.ExcessiveCombinationsException e ) {
			throw new LearnerException( e );
		}
		
		
		return sParameterize(cptModel, sufficientStats );
	}
	
	/** Using the input and output types, create a CPT */
	public CPT makeCPT( Value.Vector x, Value.Vector z )
	{
		// determine the upper and lower bounds of parents based on data
		// Also work out the number of combinations of parent states possible
		Type.Structured zType = (Type.Structured)((Type.Vector)z.t).elt;
		int[] lwbArray = new int[zType.cmpnts.length];
		int[] upbArray = new int[zType.cmpnts.length];
		
		for ( int i = 0; i < zType.cmpnts.length; i++ ) {
			Type.Discrete zEltType = (Type.Discrete)zType.cmpnts[i];	
			lwbArray[i] = (int)zEltType.LWB;
			upbArray[i] = (int)zEltType.UPB;
		}
		
		Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
		Value.Model childModel = MultinomialLearner.getMultinomialModel((int)xType.LWB,(int)xType.UPB);
		
		// We now have enough data to create an unparameterized CPT model.
		// The child model may be treated as null because we are only using this cptModel to decode
		// parent states and not deal with data.
		CPT cptModel = new CPT( childModel, lwbArray, upbArray );
		
		return cptModel;
	}
	
	
	/** Parameterise and cost data all in one hit.   */
	public double parameterizeAndCost( Value info, Value.Vector x, Value.Vector z )
	throws LearnerException
	{
		CPT cptModel;
		try {
			cptModel = makeCPT(x,z);
		}
		catch ( CPT.ExcessiveCombinationsException e ) {
			throw new LearnerException( e );
		}
		int[][] stats = cptModel.getSufficientArray(x,z);
		
		
		int parentCombinations = stats[0].length;
		int nodeStates = stats.length;
		
		//FN.LogFactorial logF = new FN.LogFactorial();
		
		double total = 0;
		
		for (int i = 0; i < parentCombinations; i ++) {
			int stateCount = 0;
			for (int j = 0; j < nodeStates; j++) {
				int count = stats[j][i];
				stateCount += count;
				total -= LogFactorial.logFactorial(count);
			}
			
			total += LogFactorial.logFactorial( stateCount + nodeStates - 1);
			total -= LogFactorial.logFactorial( nodeStates - 1 );
		}
		
		total += parentCombinations * (nodeStates - 1) * 0.17649;
		
		return total;
	}
	
	/** Parameterise and cost data all in one hit.   */
	public double sParameterizeAndCost( Value.Model m, Value s )
	throws LearnerException
	{
		Value.Vector stats = (Value.Vector)s;
		int parentCombinations = stats.length();
		int nodeStates = ((Value.Structured)stats.elt(0)).length();
		
		//FN.LogFactorial logF = new FN.LogFactorial();
		
		double total = 0;
		
		for (int i = 0; i < parentCombinations; i ++) {
			int stateCount = 0;
			for (int j = 0; j < nodeStates; j++) {
				int count = stats.cmpnt(j).intAt(i);
				// int count = ((Value.Structured)stats.elt(i)).intCmpnt(j);
				stateCount += count;
				total -= LogFactorial.logFactorial(count);
			}
			
			total += LogFactorial.logFactorial( stateCount + nodeStates - 1);
			total -= LogFactorial.logFactorial( nodeStates - 1 );
		}
		
		total += parentCombinations * (nodeStates - 1) * 0.17649;
		
		return total;
	}
	
	
	/** Parameterize and return (m,s,y) */
	public Value.Structured sParameterize( Value.Model model, Value s )
	throws LearnerException
	{	
		CPT cptModel = (CPT)model;
		Value.Vector statVector = (Value.Vector)s;
		
		Value[] paramArray = new Value[cptModel.numCombinations];
		
		for (int i = 0; i < cptModel.numCombinations; i++ ) {
			// 	    paramArray[i] = leafModelLearner.sParameterize( cptModel.childModel, statVector.elt(i) );
			Value.Structured leafMSY =
				leafModelLearner.sParameterize( cptModel.childModel, statVector.elt(i) );
			paramArray[i] = new Value.DefStructured( new Value[] {leafMSY.cmpnt(0),
					leafMSY.cmpnt(2)} );	    
		}
		
		Value.Vector paramVector = new VectorFN.FatVector( paramArray );
		
		return new Value.DefStructured( new Value[] {cptModel, statVector, paramVector} );
	}
	
	
	
	/** return cost */
	public double cost(Value.Model m, Value initialInfo, Value.Vector x, Value.Vector z, Value y)
	throws LearnerException
	{
		return sCost(m, m.getSufficient(x,z), y);
	} 
	
	/**
	 * This function ignores the parameters given and costs using the costing method from
	 * Camml Classic discrete. <br>
	 *
	 *
	 */
	public double sCost( Value.Model m, Value s, Value y )
	throws LearnerException
	{
		Value.Vector stats = (Value.Vector)s;
		int parentCombinations = stats.length();
		int nodeStates = ((Value.Structured)stats.elt(0)).length();
		
		//FN.LogFactorial logF = new FN.LogFactorial();
		
		double total = 0;
		
		for (int i = 0; i < parentCombinations; i ++) {
			int stateCount = 0;
			for (int j = 0; j < nodeStates; j++) {
				int count = ((Value.Structured)stats.elt(i)).intCmpnt(j);
				stateCount += count;
				total -= LogFactorial.logFactorial(count);
			}
			
			total += LogFactorial.logFactorial( stateCount + nodeStates - 1);
			total -= LogFactorial.logFactorial( nodeStates - 1 );
		}
		
		total += parentCombinations * (nodeStates - 1) * 0.17649;
		
		return total;
		
	}
	
	public String toString() { return "WallaceCPTLearner("+leafModelLearner+")"; }    
}

