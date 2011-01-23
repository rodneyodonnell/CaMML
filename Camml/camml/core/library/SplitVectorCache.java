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
// SplitVectorCache is a HashTable indexed by splits int[].  SoftReferences used to save memory.
//

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
