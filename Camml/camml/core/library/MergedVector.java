//
// Vector class able to Merge together multiple vectors.
//
// Copyright (C) 2002 Rodney O'Donnell  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MergedVector.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import cdms.core.*;

/* 
 * MergeVector is analguous to SelectedVector but merges are performed instead of splits.
 */

public class MergedVector extends Value.Vector
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6349044037736521880L;

	/** Original Vector all transformations are based on. */
    protected final Value.Vector[] originalVector;
    
    /** which vector element i belongs to */
    protected final int[] vecNum;

    /** which element in originalVecotr[vecNum[i]] element i is  */
    protected final int[] index;

    /** Create a vector which behaves in the same way as v, but is selectable. */
    protected MergedVector( Value.Vector[] originalVector, int[] vecNum, int[] index )
    {
	super( (Type.Vector)originalVector[0].t );

	this.originalVector = originalVector;
	this.vecNum = vecNum;
	this.index = index;
    }

    /** 
     * Create a merged vector.  <br>
     * order[i][j] represents the position in the merged vector that vec[i].elt(j) should occupy.
     *  the should not be any overlap in the numbers specified.
     */
    public MergedVector( Value.Vector[] originalVector, int[][] order ) 	
    {
	super( (Type.Vector)originalVector[0].t );

	int len = 0;
	for ( int i = 0; i < order.length; i++ ) {
	    len += order[i].length;
	}
	
	int[] vecNum = new int[len];
	int[] index = new int[len];
	
	for ( int i = 0; i < order.length; i++ ) {
	    for ( int j = 0; j < order[i].length; j++ ) {
		int val = order[i][j];
		vecNum[val] = i;
		index[val] = j;
	    }
	}

	this.originalVector = originalVector;
	this.vecNum = vecNum;
	this.index = index;
    }

     public int intAt( int i ) {
	 return originalVector[vecNum[i]].intAt(index[i]);
     }

    /** return the i'th element in the vector */
    public Value elt( int i ) 
    {
	return originalVector[vecNum[i]].elt(index[i]);
    }

    /** return the length of the vector*/
    public int length()
    {
	return vecNum.length;
    }
    
    /** If vector is a multicol vector, return the specified column */
     public Value.Vector cmpnt(int col)
     {		
	 Value.Vector[] array = new Value.Vector[ originalVector.length ];

	 for ( int i = 0; i < array.length; i++ ) {
	     array[i] = originalVector[i].cmpnt(col);
	 }

	 return new MergedVector( array, vecNum, index );
     }
}
