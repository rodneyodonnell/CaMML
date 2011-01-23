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
// HashTable which accepts int[] as keys.
//

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

    /** standard constructor     */
    public ArrayIndexedHashTable() { super(); }

    /** standard constructor */
    public ArrayIndexedHashTable(int initialCapacity) { super(initialCapacity); }

    /** standardConstructor */
    public ArrayIndexedHashTable(int initialCapacity, float loadFactor) { 
        super(initialCapacity, loadFactor);
    }

    /** Standard Constructor */
    public ArrayIndexedHashTable(Map t) { super(t);    }    
    
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
