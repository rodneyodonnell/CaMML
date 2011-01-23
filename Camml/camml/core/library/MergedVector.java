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
// Vector class able to Merge together multiple vectors.
//

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
