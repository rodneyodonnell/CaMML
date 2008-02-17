//
// TOM hashing class for CaMML
//
// Copyright (C) 2002 Lucas Hope, Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TOMHash.java
// Author: {rodo,lhope}@csse.monash.edu.au

package camml.core.search;


/**
 A unique TOM hash is formed by 
 1) Creating a N*N array of random numbers
 2) For each arc present, sum the values of these numbers
 
 This differs from a SEC hashing in that the direction of the arcs should distinguish
 between two TOMs.  This method does not require a ML scors.
 */
public class TOMHash extends ModelHash {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4220042542582625181L;

	/** Random number generator */
	private java.util.Random rand;
	
	/** Random n*n matrix used to create hash */ 
	private long[][] matrix;
	
	/** Constructor */
	public TOMHash(java.util.Random rand, int numNodes) {
		this.rand = rand;
		init(numNodes);
	}
	
	/** Initialise 64bit random matrix. */
	protected void init(int numNodes) {
		matrix = new long[numNodes][numNodes];
		for(int i = 0; i < matrix.length; i ++) {
			for(int j = 0; j < matrix[i].length; j ++)
			{ matrix[i][j] = rand.nextLong(); }
		}
	}
	
	/** Accessor function used so hash can be calculated incrementally in NodeCache*/
	public long getRandom(int x, int y) {
		return matrix[x][y];
	}   
	
	/** hash = sum_{directedArcs}( matrix[i][j] ) */
	public long hash(TOM tom, double logL) {
		long skelHash = 0L;
		int numNodes = tom.getNumNodes();
		
		// The calculateion of tomHash is now performes locally in nodes (the sum is the tomHash)
		for (int i = 0; i < numNodes; i++) {
			int[] parent = tom.node[i].parent;
			for ( int j = 0; j < parent.length; j++) {
				skelHash += getRandom(parent[j],i);
			}
		}
		
		return skelHash;
	}
	
	
}
