//
// Node definition for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell and Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File:   Node.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au

package camml.core.search;

import cdms.core.*;
import camml.core.library.*;

import camml.core.models.ModelLearner;

/**
 Each node in the model is described by the Node class.  This class also
 contains information for the calculation of message lengths, and the
 parameters of the model. <br>
 
 There are no get/set methods for the parent and child nodes, this will be performed from
 the TOM class. <br>
 
 By default, node costs and clean Nodes (@see cleanNode) are cached, but parameters are not.
 Initial time taken to perform these operations will vary depending on the modelLearner used, but
 should be (fairly) quick to extract from the cache once these initial calculations are done.
 
 @see Node.cleanNode
 */
public class Node implements Cloneable
{   
	
	
	/** RuntimeException for when maxNumParents exceeded. */
	public static class ExcessiveArcsException extends RuntimeException {
		/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 4454810752701023416L;

		/**	Standard construcotr */
		public ExcessiveArcsException(String message) { super(message);	}
	}
			
	/** Make a deep copy of the current node. */
	public Object clone() 
	{
		return new Node( var, (int[])parent.clone() );
	}
	
	/** Constructor sets dependant variable to var, and parents to empty.*/
	public Node(int var)
	{
		this.var = var;
		parent = new int[0];
	}
	
	/** Constructor sets current variable and its parents. */
	protected Node( int var, int[] parent )
	{
		this.var = var;
		this.parent = parent;	
	}
	
	/** which node is this in a TOM */ 
	public final int var;
	
	/** List of Parents of this node. */
	protected int parent[];
	
	public int getNumParents() { return parent.length; }
	
	/** add a single parent to this node. If maxParents is already reached, throw an exception */
	public void addParent( int node )
	{
//		if ( parent.length >= maxNumParents ) {
//			throw new ExcessiveArcsException("MaxParents already reached, cannot add another.");
//		}
		int[] newParent = new int[parent.length + 1];
		//boolean inserted = false;
		int i = 0;
		while ( i < parent.length && parent[i] < node ) {
			newParent[i] = parent[i];
			i++;
		}
		newParent[i] = node;
		i++;
		while ( i < newParent.length ) {
			newParent[i] = parent[i-1];
			i++;
		}
		parent = newParent;
	}
	
	/** remove a single parent from this node. */
	public void removeParent( int node )
	{
		int[] newParent = new int[parent.length - 1];
		int i = 0;
		while ( parent[i] != node ) { 
			newParent[i] = parent[i]; 
			i++; 
		}	
		while ( i < newParent.length ) { 
			newParent[i] = parent[i+1]; 
			i++; 
		}
		parent = newParent;
	}
	
	/**
	 *  learnModel strips the appropriate columns out of data, and passed it to modelLearner to 
	 *  learn a model.  This is returned as a cdms Structure (Model,sufficientStats,parameters).  
	 */
	public Value.Structured learnModel( ModelLearner modelLearner, Value.Vector data)
	throws ModelLearner.LearnerException
	{	
		// Create vectors containing the dependant variable and it's parents.
		Value.Vector myData = dependentVector(data);
		Value.Vector parentData = parentView(data);
		
		// Currently the role of additionalInfo is not really defined, this may change in future
		// versions of cdms.  For now, it can just be TRIV.
		Value additionalInfo = Value.TRIV;
		
		// Parameterise(data) = msy = (model,stats,params)
		// modelLearner.Parameterise always returns a (m,s,y) structure.  This gives us all 
		// essential information from the parameterisation process.
		Value.Structured msy = modelLearner.parameterize(additionalInfo,myData,parentData);
		
		return msy;
	}
	
	private static int warningsPrinted = 0;
	/**
	 *  cost calls learnModel to learn a model/parameters then uses modelLearner to cost the 
	 *  resulting model/parameters.  The result of cost() is cached so all accesses after the 
	 *  first are faster
	 */
	public double cost(ModelLearner modelLearner, Value.Vector data)
	{
		try {
			double cost = modelLearner.parameterizeAndCost(Value.TRIV,  // additional input
					dependentVector(data),  // output (x)
					parentView(data));      // input (y)
			return cost;
		}
		// Problem with learning model.  For example CPT has too many states and runs out of memory.
		catch ( ModelLearner.LearnerException e ) {
			if ( warningsPrinted < 10) {
				System.err.println("WARNING: Costing node " + this + " failed." +
									"ModelLearner = " + modelLearner + 
									"\terr = " + e);			
			}
			else if ( warningsPrinted == 10 ) {
				System.err.println("WARNING: Too many Node costing warnings, reporting disabled.");
			}
			warningsPrinted++;
			return Double.POSITIVE_INFINITY;
		}
	}
	
	/** This returns a vector of just the dependant variable. */
	protected Value.Vector dependentVector(Value.Vector data) {
		return data.cmpnt(var);
	}
	
	/** This returns a view of the data containing only parents of this node.  */
	protected Value.Vector parentView(Value.Vector data) {
//		Value.Function dataView = (Value.Function)CammlFN.view.apply( data );
//		Value.Vector parentVector = new VectorFN.FastDiscreteVector( parent );
//		return (Value.Vector)dataView.apply( parentVector );
		return new SelectedVector(data,null,parent);
	}
	
	/** Print a little ascii version of a node's connections.   Looks like  : "1  : <- 2 <- 3" */
	public String toString()
	{
		String s = "" + var + " : ";
		for (int i = 0; i < parent.length; i++)
			s += " <- " + parent[i];
		
		return s;
	}
};
