//
// SplitVectorCache is a HashTable indexed by splits int[].  SoftReferences used to save memory.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: SplitVectorCache.java
// Author: rodo@dgs.monash.edu.au
// Created on 11/02/2005

package camml.core.library;
import java.lang.ref.SoftReference;
import cdms.core.Value;

/**
 * This class uses SoftReferences to cache a partially split vector.
 */
public class SplitVectorCache extends ArrayIndexedHashTable {

	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7738286380935470396L;
	/** Full vector all split vectors are based on. */
	final SelectedVector fullVec;
	
	/** Constructor accepting Vector */
	public SplitVectorCache( Value.Vector vec ) {	
		super();  
		if ( vec instanceof SelectedVector ) fullVec = (SelectedVector)vec;
		else fullVec = new SelectedVector(vec,null,null);
	}

	/** Return Vector from hashTable. */
	public SelectedVector[] getVec( int[] split ) {
		// If no reference exists, return null
		SoftReference ref = (SoftReference)get2( split );
		if ( ref == null ) { return null; }
		// If vector has already been deleted, delete reference and return null.
		SelectedVector[] vec = (SelectedVector[])ref.get();
		if ( vec == null ) { this.remove2( split ); }
		// Return vector.
		return vec;
	}
	
	/** Add SelectedVector to hashTablt */
	public void putVec( int[] split, SelectedVector[] vec ) {
		put2( split, new SoftReference(vec) );
	}
}
