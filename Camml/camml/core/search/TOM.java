//
// TOM for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell and Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TOM.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;


import java.util.BitSet;

import camml.core.models.ModelLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;

/**
 TOM is a Totally Ordered Model, it is a causal model where the time ordering of the nodes is
 completely determined. This is a stronger definition than a Causal Network, as a causal
 network implies only a partial time ordering on the nodes.
 <p>
 TOM-space varies in time ordering of nodes, and connection between nodes, and the TOM class is
 designed for easy searching of TOM-space. It can also be scored using some evaluation metric.    
 */
public class TOM implements Cloneable
{
	/** Maximum number of parents a node is allowed. */
	protected int maxNumParents = 7;
	public int getMaxNumParents() { return maxNumParents; }
	public void setMaxNumParents( int maxNumParents ) { this.maxNumParents = maxNumParents; }
	
	/** This contains all the nodes, in the order of the input datafile.    */
	protected Node[] node;
	
	/**
	 * This is an index into the node array. It could be changed to an array of nodes itself,
	 *	but we believe this is more meaningful. <br>
	 * 
	 * totalOrder[i] = i'th variable in total order <br>
	 * @see variablePlace.       
	 */
	protected int[] totalOrder;
	
	/**
	 * Stores where in the total ordering each variable is.  This is required to easily determing
	 * if A is before B in the total ordering.  Otherwise a linear search of totalOrder must be
	 * performed. <br>
	 *
	 * For the single straight line connected network (2) -> (1) -> (3) -> (0) -> (4) <br>
	 * totalOrder    = { 2, 1, 3, 0, 4 } <br>
	 * variablePlace = { 3, 1, 0, 2, 4 } <br>
	 * 
	 * @see totalOrder
	 */
	protected int[] variablePlace;
	
	
	/**
	 * The edges between the nodes. The array is upper triangular, with supplied accessors
	 * which don't care about the order of the nodes. The ordering of edge follows that of node.
	 */
	protected BitSet[] edge;
	
	/**  Keep track of how many edges (links) are present in this TOM   */
	protected int numEdges;
	
	/** standard CDMS way of representing a dataset */
	protected final Value.Vector data;
	
	/** caseInfo contains information about the current problem */
	public final CaseInfo caseInfo;
	
	/**
	 Note that the type of the data is IMPORTANT! It should be a Type.Structured, with each type
	 component specifying the dataspace of that variable. The node types are generated from
	 */
	public TOM( CaseInfo caseInfo )
	{
		this.data = caseInfo.data;
		this.caseInfo = caseInfo;
		
		// create initial nodes, edges and ordering
		Type.Structured structure = (Type.Structured)((Type.Vector)data.t).elt;
		int len = structure.cmpnts.length;
		node = new Node[len];
		totalOrder = new int[len];
		variablePlace = new int[len];
		edge = new BitSet[len - 1];
		numEdges = 0;
		
		// iterate over the structure.
		for(int i = 0; i < len; i ++) {
			// create initial ordering
			totalOrder[i] = i;
			variablePlace[i] = i;
			
			// create node
			node[i] = new Node(i);
			
			// create initial edge array.
			if(i == len - 1)
			{ break; }
			edge[i] = new BitSet(i+1);
			for(int j = 0; j < i + 1; j ++) {
				edge[i].set(j,false);
			}
		}
	}
	
	/** Create a TOM usable by functions not requiring CaseInfo. 
	 * NOTE: This constructor should be avoided as some TOM functions may not function correctly. */
	public TOM( Value.Vector data )
	{
		this( new CaseInfo( null, null, data, null, null, -1, null ) );
	}    
	

	/** Create a TOM using a fake dataset. 
	 * NOTE: This constructor should be avoided as some TOM functions may not function correctly. */
	public TOM( int numNodes ) {
		this( SearchDataCreator.generateData(1,numNodes) );
	}
	
	/** get the data being evaluated */
	public Value.Vector getData()
	{
		return data;
	}
	
	/** return number of variables in data. */
	public int getNumNodes()
	{
		return node.length;
	}
	
	/** return the number of edges present in this TOM*/
	public int getNumEdges()
	{
		return numEdges;
	}
	
	///////////////////////
	// utility functions //
	///////////////////////
	/** return whether an arc exists between two node indices.  */
	public boolean isArc(int x, int y)
	{
		if(x == y)
		{ return false; }
		
		int first, second;
		if(x > y) {
			first = x - 1;
			second = y;
		}
		else {
			first = y - 1;
			second = x;
		}
		
		return edge[first].get(second);
	}
	
	/**  set an arc.    */
	protected void setArc(int x, int y, boolean arcValue)
	{
		if(x == y)
		{ return; }
		
		int first, second;
		if(x > y) {
			first = x - 1;
			second = y;
		}
		else {
			first = y - 1;
			second = x;
		}
		boolean oldEdge = edge[first].get(second);	
		edge[first].set(second,arcValue);
		
		int child, parent;
		if ( before(x,y) ) { parent = x; child = y; }
		else { child = x; parent = y; }
		
		if ( !oldEdge && arcValue ) { 
			numEdges ++; 
			if ( node[child].parent.length >= maxNumParents ) {
				throw new Node.ExcessiveArcsException(
						"MaxParents already reached, cannot add another.");
			}
			node[child].addParent(parent);
			}
		else if ( oldEdge && !arcValue ) {
			numEdges --; 
			node[child].removeParent(parent);
			}
		else {
			System.out.println("--- Link already present/absent?? ---");
		}       
	}
	
	/**
	 add an arc by index.
	 Returns true if an operation was performed.
	 */
	public boolean addArc(int x, int y)
	{
		if(isArc(x, y) || x == y)
		{ return false; }
		else {
			setArc(x, y, true);
			return true;
		}
	}
	
	/**
	 remove an arc by index. 
	 Returns true if an operation was performed.
	 */
	public boolean removeArc(int x, int y)
	{
		if(!isArc(x, y))
		{ return false; }
		else {
			setArc(x, y, false);
			return true;
		}
	}
	
	/** returns true if variable i is before variable j in the total ordering */
	public boolean before( int nodeI, int nodeJ)
	{
		if ( variablePlace[nodeI] < variablePlace[nodeJ] ) {
			return true;
		}
		return false;
	}
	
	
	/** Swap nodeX and nodeY in the total ordering */
	public void swapOrder( int x, int y, boolean updateNodes ) {
		
		// Swap variables in total ordering
		int tmp;
		tmp = variablePlace[x];
		variablePlace[x] = variablePlace[y];
		variablePlace[y] = tmp;
		totalOrder[variablePlace[x]] = x;
		totalOrder[variablePlace[y]] = y;
		
		// Update parents in node[] if required.
		if ( updateNodes ) {
			// Make sure posX < posY
			int posX = getNodePos(x);
			int posY = getNodePos(y);
			if ( posX > posY ) {
				int temp = posX;  int temp2 = x;
				posX = posY; x = y;	    
				posY = temp; y = temp2;
			}
			
			for ( int i = posX; i < posY; i++ ) {
				if ( isArc( totalOrder[i], y) ) { 
					node[totalOrder[i]].removeParent(y); 
					if ( node[y].parent.length >= maxNumParents ) {
						throw new Node.ExcessiveArcsException(
								"MaxParents already reached, cannot add another.");
					}
					
					node[y].addParent(totalOrder[i]); 
					}
				if ( isArc( totalOrder[i], x) ) { 
					node[totalOrder[i]].addParent(x); 
					node[x].removeParent(totalOrder[i]); 
					}

				}		
			}
		}
	
	
	/* randomise the total order */
	public void randomOrder(java.util.Random rand) {
		
		if ( caseInfo.regression ) {
			// Start with nodes in fixed order (for regression).
			for ( int i = 0; i < node.length; i++ ) {
				swapOrder( nodeAt(i), i, true );
			}
		}	
		
		// Randomly permute total order
		for ( int i = node.length-1; i > 0; i-- ) {
			int j = (int)(rand.nextDouble() * (i+1));
			swapOrder( nodeAt(i),nodeAt(j),true);
		}       	
	}
	
	/**
	 * Add every possible edge in this TOM to a maximum of maxParents. <br>
	 * If the addition of extra nodes would give the node an infinite MML cost then it is not added.
	 */
	public void fillArcs( int maxParents )
	{
		// Add up to maxParents arcs starting with nodes just before current node in total ordering.
		for ( int child = 0; child < node.length; child++ ) {
			for ( int parentIndex = getNodePos(child)-1; (parentIndex >= 0) && 
					(node[child].parent.length < maxNumParents); parentIndex-- ) {
				
				addArc( nodeAt(parentIndex), child );
				
				// If adding this arc makes the cost infinite, then don't add it.
				// Removing this can leave the tom in a nasty state of having an infinite cost.
				// This most commonly occurs when we try and build a CPT with way to many states
				// and an exception is thrown upstream.
				
				if ( caseInfo.nodeCache != null && 
					 Double.isInfinite( caseInfo.nodeCache.getMMLCost( node[child] ) ) ) {
					removeArc( nodeAt(parentIndex), child );
					// break;
				}
				
			}
		}	
	}
	
	
	
	/** remove all arcs from this TOM */
	public void clearArcs() {
		for ( int i = 0; i < node.length; i++ ) {
			for ( int j : node[i].parent ) {
				removeArc(i,j); 		
			}
		}
	}
	
	
	/**
	 set all arcs randomly (0.5 prob of an arc)
	 */
	public void randomArcs(java.util.Random generator) {
		randomArcs(generator,0.5);
	}
	
	/**
	 set all arcs randomly (p prob of an arc)
	 */
	public void randomArcs(java.util.Random generator, double p) {
		for(int i = 0; i < node.length - 1; i ++) {
			for(int j = i + 1; j < node.length; j ++) {
				boolean state = (generator.nextDouble() < p);
				if ( (state && !isArc(i,j)) ||
					 (!state && isArc(i,j))) { 
					setArc(i, j, state); 
				}
			}
		}
	}
	
	/** return the Nth node in the total ordering. */
	public int nodeAt( int node )
	{
		return totalOrder[node];
	}
	
	/** Return the position in the total ordering of node n */
	public int getNodePos( int node ) 
	{
		return variablePlace[node];
	}
	
	/** Accesor function for node[] */
	public Node getNode(int n)
	{
		return node[n];
	}
	
	/** Randomize the total ordering then fix it so it is consistent with arcs present. */
	public void buildOrder( java.util.Random rand )	
	{
		TOM tempTOM = (TOM)this.clone();
		
		BitSet[] ancestors = getAncestorBits();
		final int n = node.length;
		
		clearArcs();
		randomOrder( rand );
		
		// do a topological sort to get nodes back to correct order.
		int numChanges = 1;
		while ( numChanges != 0 ) {
			numChanges = 0;
			for (int i = 0; i < n - 1; i++) {
				for (int j = i+1; j < n; j++) {
					int varI = nodeAt(i); 
					int varJ = nodeAt(j); 
					
					// if VarJ is before VarI, swap them.	  
					if (ancestors[varI].get(varJ)) {
						numChanges ++;
						swapOrder( varI, varJ, false);
					}
				}
			}	    
		}
		
		// Add all original arcs to TOM
		for ( int i = 0; i < n; i++ ) {
			for ( int j = i+1; j < n; j++ ) {
				if (tempTOM.isArc(i,j)) {
					addArc(i,j);
				}
			}
		}
	}
	
	/** Calculate an array of BitSets where (x[i].get(j) = true) implies i <= j exists. */ 
	public BitSet[] getAncestorBits(  ) {
		return getAncestorBits( -1, new BitSet[node.length]);
	}

	/** Get bits for given index, if index == -1 get all bits. */
	private BitSet[] getAncestorBits( int index, BitSet[] bits ) {
		// if index == -1, calculate all ancestors.
		if (index == -1) {
			for ( int i = 0; i < bits.length; i++) {
				if (bits[i] == null) { getAncestorBits(i,bits); } 
			}
		}
		// if ancestors already calculated, nothing to do. Return.
		else if (bits[index] != null) {
			return bits;			
		}
		// Calculate ancestors of ancestors.  And them together to get this node's ancestors.
		else {			
			int[] parents = node[index].parent;
			BitSet ancestors = new BitSet(bits.length); 			
			
			for ( int parent : parents) {
				// get Parent's ancestors.
				BitSet parAnc = bits[parent];
				if (parAnc == null) { parAnc = getAncestorBits(parent,bits)[parent]; }
				ancestors.or( parAnc );
				ancestors.set(parent);
			}
			bits[index] = ancestors;
		}
		
		return bits;
	}

	
	/** Does a directed arc exist from i to j? */
	public boolean isDirectedArc( int i, int j ) {
		return isArc(i,j) && before(i,j);
	}
	
	/**
	 *  Two DAGs are considered equal if they have identical arcs, and those arcs are in the same
	 *  direction.  Defining DAG equality here may be more useful than TOM equality.
	 */
	public boolean equals( Object o )
	{
		TOM tom1 = this;
		TOM tom2 = (TOM)o;
		
		for (int i = 0; i < edge.length; i++) {
			if (tom1.edge[i].equals( tom2.edge[i]) == false) {return false;}
			for (int j = 0; j < edge[i].size(); j++) {
				if ( tom1.edge[i].get(j) ) {
					if ( tom1.variablePlace[i+1] < tom1.variablePlace[j] !=
						tom2.variablePlace[i+1] < tom2.variablePlace[j] )
						return false;
				}
			}
		}
		return true;
	}
	
	/**
	 *  Using the current list of connections, create the parameter list required to interact with
	 *  a BNet model.  <br>
	 *  The format is : [ ( [parents], (submodel,subparams) ) ]
	 */
	public Value.Vector makeParameters( ModelLearner modelLearner )
	throws ModelLearner.LearnerException
	{
		int numVars = getNumNodes();
		
		// Create arrays to hold initial structures.
		String name[] = new String[ numVars ];
		Value.Vector subParents[] = new Value.Vector[numVars];
		Value.Model[] subModel = new Value.Model[ numVars ];
		Value subModelParam[] = new Value[ numVars ];
		Value.Structured subParam[] = new Value.Structured[ numVars ];
		Value.Structured localStructure[] = new Value.Structured[ numVars ];
		
		
		
		// initialise name
		Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
		for ( int i = 0; i < name.length; i++ ) {
			if ( dataType.labels != null ) {
				name[i] = dataType.labels[i];
			}
			else {
				name[i] = "var(" + i + ")";
			}
		}
		
		// set value of parents.
		for ( int i = 0; i < subParents.length; i++ ) {
			subParents[i] = new VectorFN.FastDiscreteVector( node[i].parent.clone() );
		}       		
		
		// set CPT models and parameters for nodes.
		for ( int i = 0; i < subModel.length; i++ ) {
			Value.Structured msy = node[i].learnModel( modelLearner, data );
			subModel[i] = (Value.Model)msy.cmpnt(0);
			subModelParam[i] = msy.cmpnt(2);
		}	
		
		// ( subModel, subParam )
		for ( int i = 0; i < subParam.length; i++ ) {
			subParam[i] = new Value.DefStructured( new Value[] {subModel[i], subModelParam[i]} ); 
		}
		
		
		// ( [parants], ( subModel, subParam ) )
		for ( int i = 0; i < localStructure.length; i++ ) {
			localStructure[i] = new Value.DefStructured( new Value[] {new Value.Str(name[i]), 
					subParents[i], 
					subParam[i]} );
		}
		
		
		return new VectorFN.FatVector( localStructure  );
	}

	public int[][] getParentArrays( Value.Vector params )
	{
		int parents[][] = new int[params.length()][];
		for (int i = 0; i < params.length(); i++) {
			Value.Vector arcVec = (Value.Vector)params.cmpnt(1).elt(i); 
			parents[i] = new int[arcVec.length()];
			for (int j = 0; j < arcVec.length(); j++) {
				parents[i][j] = arcVec.intAt(j);
			}
		}
		return parents;
	}

	
	/** Set the current node ordering and edges based on params <br>
	 *  Calling setStructure( makeParams( xx ) ) should leave 
	 *  the original TOM in tact.
	 */
	public void setStructure( Value.Vector params ) {
		setStructure( getParentArrays(params) );
	}
	
	/** Set the current node ordering and edges based on arcs <br> */
	public void setStructure( int[][] arcs ) {
		// Remove all arcs from current TOM
		this.clearArcs();

		// Use simplistic (and possibly slow) algorithm to ensure
		// this TOM has an ordering consistent with the arc
		// structure in params.
		int changes = 1;
		while ( changes != 0 ) {
			changes = 0;
			// for each arc
			for ( int i = 0; i < arcs.length; i++ ) {
				for ( int j = 0; j < arcs[i].length; j++ ) {

					// if TOM ordering inconsistent with param
					// ordering, swap ordering in TOM.
					int nodeI = i;
					int nodeJ = arcs[i][j];

					if( before(nodeI,nodeJ) ) {
						swapOrder(nodeI,nodeJ,true);
						changes ++;
					}
				}
			}
		}

		// Add required arcs to TOM.
		for ( int i = 0; i < arcs.length; i++ ) {
			for ( int j = 0; j < arcs[i].length; j++ ) {
				this.addArc(i,arcs[i][j]);
			}
		}
		

	}
	
	/** Set the current node ordering and edges to those of tom2 */
	public void setStructure( TOM tom2 )
	{
		clearArcs();
		
		// set the ordering to be the same as tom2
		for ( int i = 0; i < node.length; i++ ) {
			this.swapOrder( nodeAt(i), tom2.nodeAt(i), false );	    
		}
		
		// set arcs to be the same as 
		for ( int i = 0; i < node.length; i++ ) {
			for ( int j : tom2.getNode(i).parent ) {
				this.addArc(i,j);
			}
		}
	}

	
	
	/** Set the total ordering of the tom to order. */
	public void setOrder( int[] order ) {
		if ( order.length != totalOrder.length ) {
			throw new IllegalArgumentException("Invalid Ordering specified");
		} 

		for ( int i = 0; i < totalOrder.length; i++ ) {
			this.swapOrder( nodeAt(i), order[i], true );
		}

		// If order[] is not a valid ordering (eg. duplicate values)
		// the total ordering may not match.
		for ( int i = 0; i < totalOrder.length; i++ ) {
			if ( order[i] != totalOrder[i] ) {
				throw new RuntimeException("totalOrder != specified order");
			}
		}
	}

	
	/**
	 *  Clone the TOM.
	 *  - Deep copy of Node[] node <br>
	 *  - Deep copy of int[] totalOrder
	 *  - Deep copy of int[][] edge
	 *  - Shallow copy of Value.Vector data
	 */
	public Object clone() 
	{
		TOM tempTOM = new TOM( caseInfo );
		
		tempTOM.totalOrder = new int[totalOrder.length];
		tempTOM.variablePlace = new int[variablePlace.length];
		for (int i = 0; i < totalOrder.length; i++) {
			tempTOM.totalOrder[i] = totalOrder[i];	    
			tempTOM.variablePlace[i] = variablePlace[i];
		}
		
		tempTOM.node = new Node[node.length];
		
		for (int i = 0; i < node.length; i++) {
			tempTOM.node[i] = (Node)node[i].clone();
		}
		
		tempTOM.edge = new BitSet[edge.length];
		for (int i = 0; i < edge.length; i++) {
			tempTOM.edge[i] = (BitSet)edge[i].clone();			
		}
		
		tempTOM.numEdges = numEdges;
		
		
		return tempTOM;
	}
	
	public String toNodeString(String[] name, int i)
	{
		StringBuffer s = new StringBuffer();
		s.append( name[i] + " : ");
		for (int j = 0; j < node[i].parent.length; j++)
			s.append( " <- " + name[node[i].parent[j]] );
		return s.toString();
	}
	
	/** Create ascii version of TOM */
	public String toString()
	{
		String[] name = new String[node.length];
		Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
		for ( int i = 0; i < name.length; i++ ) {
			if ( dataType.labels != null ) {
				name[i] = dataType.labels[i];
			}
			else {
				name[i] = "var(" + i + ")";
			}
		}
		
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < node.length; i++) {
			//	    updateParents(i);
			s.append( toNodeString(name, i) );
			s.append('\n');
		}
		return s.toString();       
	}
	
	/** Remove insignificant arcs from TOM */
	synchronized public void clean( )
	{
		caseInfo.tomCleaner.cleanTOM(this);
	}
	
	
	/** Calculate cost of TOM. */
	// NOTE: Changes made here should also be made in BNetSearch.costNodes
	public double getCost( )
	{
		double structureCost = caseInfo.tomCoster.cost(this);
		
		double totalCost = 0;
		
		// for each node
		for (int i = 0; i < getNumNodes(); i++) {
			
			Node currentNode = node[i];	    
			double tempNodeCost = caseInfo.nodeCache.getMMLCost( currentNode );
			totalCost += tempNodeCost;
		}
		
		// return linkCost + totalOrderCost + totalCost;
		return structureCost + totalCost;
	}
	
	
	/** is a an ancestor of x? (I think its O(numnodes^2), might be O(nnumnodes^3) */
	public boolean isAncestor(int ancestorNode, int descendantNode)
	{
		// quick check if a is not before descendantNode. 
		// Also catches case when ancestorNode == descendantNode.
		if( !before(ancestorNode,descendantNode) )
		{ return false; }
		
		boolean[] checked = new boolean[node.length];
		return isAncestor( ancestorNode, descendantNode, checked );
	}
	
	/** returns true if 'a' is an ancestor of 'd' */
	protected boolean isAncestor( int a, int d, boolean[] checked ) {
		int[] parent = node[d].parent;
		for ( int i = 0; i < parent.length; i++ ) {
			if ( parent[i] == a ) { return true; }
			if ( !checked[parent[i]] ) { 
				checked[parent[i]] = true;
				if ( isAncestor(a,parent[i],checked) ) { return true;}
			}
		}
		return false;
	}
	
	/** is d a descendant of x? */
	public boolean isDescendant(int d, int x)
	{
		return isAncestor( x, d );
	}

	/** Return true if node1 and node2 have are correlated, that is
	 *  NOT d-seperated. Correlation occurs when one node is an descendant
	 *  of the other, or they have a common ancestor.*/
	public boolean isCorrelated(int node1, int node2)
	{
		boolean a1[] = flagAncestors(node1, new boolean[getNumNodes()]);
		boolean a2[] = flagAncestors(node2, new boolean[getNumNodes()]);
		a1[node1] = true;
		a2[node2] = true;
		for ( int i = 0; i < a1.length; i++) {
			if (a1[i] && a2[i]) return true;
		}
		return false;
	}
	
	/** set flagged[i] = true if node[i] is an ancestor of node[n] */
	protected boolean[] flagAncestors( int n, boolean[] flagged ) {
		int[] parents = node[n].parent;
		for ( int i = 0; i < parents.length; i++ ) {
			if ( !flagged[parents[i]] ) { 
				flagAncestors( parents[i], flagged );
			}
		}
		flagged[n] = true;
		return flagged;
	}

	
	/** Return tomHash(tom=this, ml=0, clean=false) as an positive integer */
	public int hashCode() {
		long hash = caseInfo.tomHash.hash(this,0); 
		return 	((int)hash);
	}

	
	/** Enumeration of elements returned by edit distance {add,del,rev,correct} */
	enum EditDistance { add, del, rev, correct};
	
	/** Return Edit Distance between two TOMs. ED indexed by EditDistance enum. */
	public int[] editDistance( TOM t2) {
		int[] ed = new int[EditDistance.values().length];
		for (int i = 0; i < getNumNodes(); i++) {
			for (int j = 0; j < getNumNodes(); j++) {
				if (this.isDirectedArc(i,j)) {
					if (t2.isDirectedArc(i,j)) ed[EditDistance.correct.ordinal()]++;
					else if (t2.isDirectedArc(j,i)) ed[EditDistance.rev.ordinal()]++;
					else ed[EditDistance.del.ordinal()]++;
				}
				else if ( t2.isDirectedArc(i,j) && !this.isDirectedArc(j,i)) {
					ed[EditDistance.add.ordinal()]++;
				}
			}
		}
		return ed;
	}
};
