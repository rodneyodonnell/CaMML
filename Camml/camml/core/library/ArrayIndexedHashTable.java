//
// HashTable which accepts int[] as keys.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: ArrayIndexedHashTable.java
// Author: rodo@dgs.monash.edu.au
// Created on 11/02/2005

package camml.core.library;

import java.util.Hashtable;
import java.util.Map;

/** Specialised hashTable which accepts int[] as keys.  
 * int[] keys are automatically conveted into values with a useful hashCode() function */
public class ArrayIndexedHashTable extends Hashtable {

	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -331453650178042768L;

	/** standard constructor	 */
	public ArrayIndexedHashTable() { super(); }

	/** standard constructor */
	public ArrayIndexedHashTable(int initialCapacity) { super(initialCapacity); }

	/** standardConstructor */
	public ArrayIndexedHashTable(int initialCapacity, float loadFactor) { 
		super(initialCapacity, loadFactor);
	}

	/** Standard Constructor */
	public ArrayIndexedHashTable(Map t) { super(t);	}	
	
	/** get Object based on key */
	public Object get2( int[] key ) { return get(tempKey.setKey(key) ); }
		
	/** Does table contain key? */
	public boolean containsKey2( int[] key ) { return containsKey( tempKey.setKey(key) ); }

	/** put Object with given key */
	public void put2( int[] key, Object o ) { put( new Key(key), o); }
	
	/** Remove object from table */
	public Object remove2( int[] key ) { return remove( tempKey.setKey(key)); }
	
	/** Temporary key used to save reallocating memory each time a key is needed. */
	private Key tempKey = new Key(null);
	
	/** Key class is a wrapper for int[] containing hashCode() and equals() functions. */
	protected static class Key {
		private int[] key;
		public Key( int[] key ) { this.key = key;}
		
		/** Set key to a new value. */
		protected Key setKey( int[] key ) { this.key = key; return this;}
		
		/** Basic hash function which should always give unique hashed for up to 8 digits
		 *  each less than 16. Hash function is still reasonable without this restriction. */
		public int hashCode() { 
			int hash = 0;
			for ( int i = 0; i < key.length; i++ ) { hash = (hash << 4) + key[i]; }
			return hash;
		}
		
		/** Return true if both keys contain the same ints, in the same order.*/
		public boolean equals( Object o ) {
			Key key2 = (Key)o;
			if ( key.length != key2.key.length ) { return false; }
			for ( int i = 0; i < key.length; i++ ) { if (key[i] != key2.key[i]) { return false; } }
			return true;
		}
	}
}
