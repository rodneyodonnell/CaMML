/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lucas Hope, Lloyd Allison, Kevin Korb
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
// Vector class able to only display selected parts.
//

// File: HiddenColumnVector.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import cdms.core.*;


/**
 *  SelectedStructure works in the same way as SelectedVector, accept for Structures <br>
 */
public class SelectedStructure extends Value.Structured 
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -1221386789786428651L;

    /** Original structure being masked. */
    protected final Value.Structured originalStruct;
    
    /** Mask of which elements are visible. */
    protected final int[] col;
    
    /** Initialise SelectedStructure with a structure and cols. */
    public SelectedStructure( Value.Structured s, int[] col ) {
        // make a structure type with the appropriate columns
        super( makeSelectedStructureType( (Type.Structured)s.t, col ) );
        this.originalStruct = s;
        this.col = col;
    }

    /** return the i'th component (according to the mask) */
    public Value cmpnt( int i ) {
        return originalStruct.cmpnt( col[i] );
    }

    /** return the i'th component (according to the mask) */
    public int intCmpnt( int i ) {
        return originalStruct.intCmpnt( col[i] );
    }

    /** return the i'th component (according to the mask) */
    public double doubleCmpnt( int i ) {
        return originalStruct.doubleCmpnt( col[i] );
    }

    /** return the number of components in this structure */
    public int length() {
        return col.length;
    }

    /** make a structure type based on an existing structure type and a list of columns.*/
    public static Type.Structured makeSelectedStructureType( Type.Structured sType, int[] col ) 
    {
        // If we don't have initial componenet types. we can't continue.
        if ( sType.cmpnts == null ) {
            throw new RuntimeException("cmpnts type not specified in makeSelectedStructureType.");
        }

        // list of cmpnts to be used.
        Type[] cmpnt;
    
        // names of cmpnts
        String[] name;
    
        // the new type being created
        Type.Structured newType;

    
        // If there are labels, make a new type from labels and cmpnts.
        if (sType.labels != null) {
            name = new String[ col.length ];
            cmpnt = new Type[ col.length ];
            for ( int i = 0; i < col.length; i++ ) {
                name[i] = sType.labels[col[i]];
                cmpnt[i] = sType.cmpnts[col[i]];
            }
            newType = new Type.Structured( cmpnt, name );
        }
        // if there are no labels, make an unlabeled type.
        else {
            cmpnt = new Type[ col.length ];
            for ( int i = 0; i < col.length; i++ ) {
                cmpnt[i] = sType.cmpnts[col[i]];
            }
            newType = new Type.Structured( cmpnt );
        }

        // return the newly created type.
        return newType;
    }


}



