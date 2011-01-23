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
// Vector class able to only display selected parts.
//

// File: SelectedVector.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import cdms.core.*;

/* 
 * Create a new vector from an old one but hide certain columns.  This is useful in cases such
 * as a decision tree where a column is no longer required but making a copy of the dataset is
 * not appropriate. <br>
 *
 * Rows/Columns are selected by passing an int[] to the constructor.  This method could also be
 * used as a way of storing a sorted vector, or making a vector appear to contain duplicates
 * without the overhead of creating new objects. <br>
 *
 * NOTE:  null may be passed to constructor if all rows/columns are desired. <br>
 * NOTE2: For vectors which contain only a single column of data (eg. FastDiscreteVector) the value
 *        passed to column should always be null.
 */

// TODO: Check through files and make sure all comments are still valid.
public class SelectedVector extends Value.Vector
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -488363859887083793L;

    /** Original Vector all transformations are based on. */
    protected final Value.Vector originalVector;
    
    /** which rows should be visible */
    protected final int[] row;
    
    /** which columns should be visible */
    protected final int[] column;
    
    /** Accesor for row */
    public int[] getRows() 
    {
        return row;
    }
    
    /** Accesor for column */
    public int[] getColumns() 
    {
        return column;
    }
    
    /** Number of cmpnts if a multiCol vector */
    protected final int numCmpnts;
    
    /** List of cmpnts */
    protected final Value.Vector[] cmpntArray;
    
    /**
     * If vector is a vector of structured, return the number of columns.
     * If a vector of anything else, return -1;
     */
    public static int getNumCmpnts( Type.Vector vType ) {
        if ( vType.elt instanceof Type.Structured == false ) {
            return -1;
        }
        Type.Structured sType = (Type.Structured)vType.elt;
        return sType.cmpnts.length;
    }
    
    /** Create a vector which behaves in the same way as v, but with extra features. */
    public SelectedVector( Value.Vector v )
    {
        super( (Type.Vector)v.t );
        this.originalVector = v;
        
        // With this constructor, all columns are visible
        row = null;
        
        // With this constructor all rows are visible
        column = null;
        
        numCmpnts = getNumCmpnts( (Type.Vector)this.t );
        if ( numCmpnts != -1 ) cmpntArray = new Value.Vector[numCmpnts];
        else cmpntArray = null;
    }
    
    /** Create a vector which behaves in the same way as v, but with only the selected columns
     *  present.  The values in column[] should not be modified after passing to selectedVector,
     *  if modification is required pass a clone of column[].  If all columns are to be used, null
     *  may be passed in place of column <br>
     *
     *  row operates in the same manner specified above for column. <br>
     * 
     *  If v is a SelectedVector, row and column should be in terms of v.originalVector
     */
    public SelectedVector( Value.Vector v, final int[] row, final int[] column )
    {
        // make a new type from the selected columns
        super(  makeSelectedVectorType( (Type.Vector)v.t, column ) );
        
        // originalVector should (almost) always be a SelectedVector.
        if ( v instanceof SelectedVector ) {
            SelectedVector orig = (SelectedVector)v;
            this.originalVector = orig.originalVector;
            this.row = origToNewIndex( orig.row, row);
            this.column = origToNewIndex( orig.column, column);
        }
        else { 
            originalVector = new SelectedVector(v);
            this.row = row;
            this.column = column;    
        }
        
        
        numCmpnts = getNumCmpnts( (Type.Vector)this.t );
        if ( numCmpnts != -1 ) cmpntArray = new Value.Vector[numCmpnts];
        else cmpntArray = null;
    }
    
    /** Convert from new index to old index. usage: this.row = origToNewIndex( orig.row, row) */
    public static int[] origToNewIndex( int[] oldIndex, int[] newIndex) {
        if ( newIndex == null) {return oldIndex;}
        if ( oldIndex == null) {return newIndex;}
        
        int[] index = new int[newIndex.length];
        for ( int i = 0; i < index.length; i++) {
            index[i] = oldIndex[newIndex[i]];
        }
        return index;
    }
    
    /** return original.intAt([row[i]] ) */
    public int intAt( int i ) {
        if ( column != null ) { throw new RuntimeException("Cannot get int from multicol"); }
        
        // If some rows need to be removed, remove them.
        if ( row == null ) { 
            return originalVector.intAt(i); 
        }
        else { return originalVector.intAt( row[i] );}
        
    }
    
    /**  return original.elt([row[i]] ) */
    public Value elt( int i ) 
    {
        // Extract the appropriate element.
        Value element;
        if ( row == null ) { element = originalVector.elt(i); }
        else { element = originalVector.elt( row[i] );}
        
        // If some rows need to be removed, remove them.
        Value selectedElement;
        if ( column == null ) { selectedElement = element; }
        else { selectedElement = new SelectedStructure((Value.Structured)element, column ); }
        
        return selectedElement;
    }
    
    /** return the length of the vector */
    public int length()
    {
        int len;
        if ( row == null ) { len = originalVector.length(); }
        else { len = row.length; }
        return len;
    }
    
    /** If vector is a multicol vector, return the specified column */
    public Value.Vector cmpnt(int col)
    {         
        if ( row != null ) {
            if ( cmpntArray[col] == null ) {
                if ( column != null )
                    cmpntArray[col] = new SelectedVector(originalVector.cmpnt(column[col]), row, null);
                else
                    cmpntArray[col] = new SelectedVector( originalVector.cmpnt(col), row, null );
            }
            return cmpntArray[col];
        }
        else {
            //xxx++;
            //if (xxx%10000 == 0)    System.out.println("NULL! " + (xxx));
            if ( cmpntArray[col] == null ) {
                if ( column != null )
                    cmpntArray[col] = originalVector.cmpnt(column[col]);
                else
                    cmpntArray[col] = originalVector.cmpnt(col);
            }
            return cmpntArray[col];
            
        }
    }
    //static int xxx = 0;
    
    
    /** Use the makeSelectedStructure type function to create a new Vector type with the appropriate
     *  columns.
     */
    public static Type.Vector makeSelectedVectorType( Type.Vector vType, int[] col ) 
    {
        if ( col != null ) {
            Type.Structured sType = (Type.Structured)vType.elt;
            return new Type.Vector ( SelectedStructure.makeSelectedStructureType( sType, col) );
        }
        else {
            return vType;
        }
    }
            
    /**
     * create a new vector splitting it's columns in the same way as is done by this Vector <br>
     * This allows, for example, the splitVector function to be used on the output attributes
     *  of a DTree, then the splitting of the input attributes to easily match this.
     */
    public SelectedVector copyRowSplit( Value.Vector vec ) {
        if ( vec instanceof SelectedVector ) {
            SelectedVector sVec = (SelectedVector)vec;
            return new SelectedVector( sVec.originalVector, row, sVec.column );
        }
        else {
            return new SelectedVector( vec, row, null ); 
        }
    }
        
    /** Static version of split() */
    public static SelectedVector[] splitVector( Value.Vector vec, int col, boolean hideAttributes ){
        SelectedVector sVec;
        if ( vec instanceof SelectedVector ) { sVec = (SelectedVector)vec; }
        else { sVec = new SelectedVector(vec); }
        
        return sVec.split(col,hideAttributes);
    }

    // TODO: DTree needs to be tested with other version of splitVector
    /** Static version of split().  Only to be used with dTrees */
    public static SelectedVector[] dTreeSplitVector( Value.Vector vec, int col, boolean hideAttributes ){
        SelectedVector sVec;
        if ( vec instanceof SelectedVector ) { sVec = (SelectedVector)vec; }
        else { sVec = new SelectedVector(vec); }
        
        return sVec.dTreeSplit(col,hideAttributes);
    }

    // TODO: static variables hack in SelectedVector to be fixed.
    /** Cache for partially split vectors */
    public static SplitVectorCache splitCache = null;
    protected static Value.Vector splitCacheOrigVec = null;
    protected static SelectedVector emptyOrigVec = null;
    
    /** Recursively split on variables in origCol, Partially split vectors are cached
     * in splitCache. */
    public SelectedVector[] recursiveSplit( int[] origCol ) {

        // Base case, nothing left to split on.
        if ( origCol.length == 0 ) { return new SelectedVector[] {this}; }
        
        // if this.originalVector != splitCacheOrigVec overwrite the cache.
        // TODO: Cache deletion should be thought through better.
        if ( splitCache == null || 
             splitCacheOrigVec != ((SelectedVector)originalVector).originalVector ) {            
            // System.out.println("Creating new SplitCache");
            splitCache = new SplitVectorCache(originalVector);
            splitCacheOrigVec = ((SelectedVector)originalVector).originalVector;
            emptyOrigVec = new SelectedVector( splitCacheOrigVec, new int[0], null );
        }
            
        // Attempt to extract vector from cache
        SelectedVector[] splits = splitCache.getVec(origCol);
        
        // if splits not calcualted, calculate it and add to cache
        if ( splits == null ) {
            // Create array of length n-1 to split on
            int originalCurrentSplit = origCol[origCol.length-1];
            int[] origRecurSplits = new int[origCol.length-1];
            for ( int i = 0; i < origRecurSplits.length; i++ ) origRecurSplits[i] = origCol[i];                    

            // List of all vectors being split
            splits = recursiveSplit( origRecurSplits );

            // Figure out what originalSplit[n-1] maps onto in the current vector
            int currentSplit = -1;
            if ( splits[0].column == null ) { currentSplit = originalCurrentSplit; }
            else {
                for ( int i = 0; i < splits[0].column.length; i++ ) {
                    if ( splits[0].column[i] == originalCurrentSplit ) { currentSplit = i; }
                }
            }

            // result of individual splits
            SelectedVector[][] splits2 = null;
            
            // Allocate room to store each split
            splits2 = new SelectedVector[splits.length][];
            //  For all vectors waiting to be split, split and store result in split2
            for ( int j = 0; j < splits2.length; j++ ) {
                // Shortcut if no data present.
                if ( splits[j].length() == 0 ) {
                    // extract type information from vector
                    Type.Structured structType = (Type.Structured)((Type.Vector)splits[j].t).elt;
                    Type.Discrete attributeType = (Type.Discrete)structType.cmpnts[currentSplit];                    
                    
                    // find the arity of the selected type.
                    int upb = (int)attributeType.UPB;
                    int lwb = (int)attributeType.LWB;
                    int arity = upb - lwb + 1;

                    splits2[j] = new SelectedVector[arity];
                    for ( int k = 0; k < splits2[j].length; k++ ) { splits2[j][k] = emptyOrigVec; }
                }
                else {
                    splits2[j] = splits[j].split( currentSplit, false );
                }
            }
            // Collapse split2[][] back into split[] to allow splitting on next col.
            splits = new SelectedVector[splits2.length*splits2[0].length];
            for ( int j = 0; j < splits.length; j++ ) {
                splits[j] = splits2[j%splits2.length][j/splits2.length];
            }
            
            // Make sure the cache entry is based on the original with all columns present.
            for ( int j = 0; j < splits.length; j++ ) {
                splits[j] = new SelectedVector(splits[j].originalVector,splits[j].row,null);
            }
            
            splitCache.putVec(origCol,(SelectedVector[])splits.clone());
        }

        return splits;        
    }
    
    // Here, col is in terms of this vector.
    /** Return SelectedVec */
    public SelectedVector[] runRecursiveSplit( int[] col ) {
        // Recalculate col in terms of originalVec
        int[] origCol;
        if ( this.column == null ) { origCol = col; }
        else {
            origCol = new int[col.length];
            for ( int i = 0; i < col.length; i++ ) { origCol[i] = column[col[i]]; }
        }

        SelectedVector[] splits = (SelectedVector[])recursiveSplit(origCol).clone();
        
        // At this point, splits contains the correct row info, but the wrong col info
        // NOTE: We must use originalVector as the base vector so that copyRowSplit() etc
        //       funciton as expected.
        for ( int i = 0; i < splits.length; i++ ) {
            splits[i] = new SelectedVector(originalVector,splits[i].row,origCol);            
        }
        return splits;
    }
    
    /** Split vector based on all combinaitons of discrete attributes in cols[]*/
    public SelectedVector[] split( int[] col ) {
        if ( true ) return recursiveSplit( col );
        
        // No row-based splits have been performed on originalVec        
        //if ( row == null ) { System.out.println("Row NOT used? " + (++notUsed) ); }
        //if ( row != null ) { System.out.println("Row used?"); }
        //System.out.println( "OriginalVec.class = " + originalVector.getClass() );

        // If cache does not exist, create it.
        //SelectedVector original = (SelectedVector)originalVector;
        //if ( splitCache == null ) {
        //    if ( original.splitCache == null ) { 
        //        // System.out.println(original);
        //        System.out.println("Creating Split Cache.");
        //        original.splitCache = new SplitVectorCache(original);
        //        
        //    }
        //    splitCache = original.splitCache;
        //}
        if ( splitCache == null || splitCacheOrigVec != ((SelectedVector)originalVector).originalVector) {            
            System.out.println("Creating new SplitCache");
            splitCache = new SplitVectorCache(originalVector);
            splitCacheOrigVec = ((SelectedVector)originalVector).originalVector;
            
            //System.out.println( splitCache );
            //System.out.println( splitCacheOrigVec );
        }
        
        
        // Recalculate col in terms of originalVec
        int[] origCol;
        if ( this.column == null ) { origCol = col; }
        else {
            origCol = new int[col.length];
            for ( int i = 0; i < col.length; i++ ) { origCol[i] = column[col[i]]; }
        }
        
        // Extract vector from cache
        SelectedVector[] splits = splitCache.getVec(origCol);
        
        // if splits not calcualted, calculate it and add to cache
        if ( splits == null ) {                
            // List of all vectors being split
            // splits = new SelectedVector[] { original };
            splits = new SelectedVector[] { splitCache.fullVec };
            // result of individual splits
            SelectedVector[][] splits2 = null;
            
            for ( int i = 0; i < origCol.length; i++ ) {
                // Allocate room to store each split
                splits2 = new SelectedVector[splits.length][];
                //  For all vectors waiting to be split, split and store result in split2
                for ( int j = 0; j < splits2.length; j++ ) {
                    splits2[j] = splits[j].split( origCol[i], false );
                }
                // Collapse split2[][] back into split[] to allow splitting on next col.
                splits = new SelectedVector[splits2.length*splits2[0].length];
                for ( int j = 0; j < splits.length; j++ ) {
                    splits[j] = splits2[j%splits2.length][j/splits2.length];
                }
            }
            splitCache.putVec(origCol,splits);
        }
        
        // At this point, splits contains the correct row info, but the wrong col info
        // NOTE: We must use originalVector as the base vector so that copyRowSplit() etc
        //       funciton as expected.
        for ( int i = 0; i < splits.length; i++ ) {
            splits[i] = new SelectedVector(originalVector,splits[i].row,origCol);            
        }
        return splits;
    }


    // TODO dTreeSplit() should really be she same as split()
    /** original version of split() which works with DTree code     */
    public SelectedVector[] dTreeSplit( int col, boolean hideAttribute )
    {
        Value.Vector vec = this;
        
        // extract type information from vector
        Type.Structured structType = (Type.Structured)((Type.Vector)vec.t).elt;
        Type.Discrete attributeType = (Type.Discrete)structType.cmpnts[col];
        //int numVars = structType.cmpnts.length;
        
        // find the arity of the selected type.
        int upb = (int)attributeType.UPB;
        int lwb = (int)attributeType.LWB;
        int arity = upb - lwb + 1;
        
        if ( arity == 0 ) {
            throw new RuntimeException("Invalid type, arity = 0 : " + attributeType );
        }
        
        // we only need to call vec.length() once
        int vecLength = vec.length();
        
        // Allocate enough memory to store which partition each value should be placed in.
        // This will be med later.  Me ust also allocate an array to use as ain index into
        // the partition.  index[i] stores the number of elements currently in partition[i]
        int[][] partition = new int[arity][ vecLength ];  // ??? optimisation.  Make this static.
        int[] index = new int[arity];
        
        // Extract the single column vector from  vec
        Value.Vector columnVector = vec.cmpnt(col);
        
        
        // Partition the data.
        for ( int i = 0; i < vecLength; i++ ) {        
            int value = columnVector.intAt(i);   // find the value at column[i]
            try {
                partition[value][index[value]] = i;  // add 'i' to the list in the appropriate partition
            } catch ( java.lang.ArrayIndexOutOfBoundsException e ) {
                System.out.println( "-------- SelectVec -------------" );
                System.out.println( "vec = " + vec );
                System.out.println( "vec.t = " + vec.t );
                System.out.println( "col = " + col );
                System.out.println( "arity = " + arity );
                System.out.println( "--------------------------------" );
                throw e;
            }
            index[value]++;                      // increment the counter for appropriate partition
        }
        
        // Now we must trim the arrays to save memory wastage.
        for ( int i = 0; i < arity; i++ ) {
            int[] tempArray = new int[index[i]];
            for ( int j = 0; j < tempArray.length; j++ ) {
                tempArray[j] = partition[i][j];
            }
            partition[i] = tempArray;
        }
        
        
        // Work out which columns are to be used in the vector being returned.  If the column
        // is to be hidden, columnList is an array of column indexes missing the appropriate
        // index.  If column is not to be hidden, then null can be used.
        int[] columnList;
        if ( hideAttribute == true ) {
            columnList = new int[ structType.cmpnts.length - 1];
            for ( int i = 0; i < col; i++ ) { columnList[i] = i; }
            for ( int i = col; i < columnList.length; i++ ) { columnList[i] = i+1; }
        } 
        else {
            columnList = null;
        }
        
        // Create the list of vectors.  partition[i] is passed as the rows required.
        // columnList is passed as the list of columns required.
        SelectedVector[] vecList = new SelectedVector[arity];
        
        for ( int i = 0; i < vecList.length; i++) {
            vecList[i] = new SelectedVector( vec, partition[i], columnList);
        }
        
        return vecList;
    }
    
    
    /**
     *  The splitVector function splits the this vector based on the discrete attribute specified 
     *  by col.  The hideAttribute flag indicates if the column being split on should be removed
     *  from the dataset or left as is.
     **/
    public SelectedVector[] split( int col, boolean hideAttribute )
    {
        // extract type information from vector
        Type.Structured structType = (Type.Structured)((Type.Vector)this.t).elt;
        Type.Discrete attributeType = (Type.Discrete)structType.cmpnts[col];
        //int numVars = structType.cmpnts.length;
        
        // find the arity of the selected type.
        int upb = (int)attributeType.UPB;
        int lwb = (int)attributeType.LWB;
        int arity = upb - lwb + 1;
        
        if ( arity == 0 ) {
            throw new RuntimeException("Invalid type, arity = 0 : " + attributeType );
        }
        
        // we only need to call vec.length() once
        int vecLength = this.length();
        
        // Allocate enough memory to store which partition each value should be placed in.
        // This will be med later.  Me ust also allocate an array to use as ain index into
        // the partition.  index[i] stores the number of elements currently in partition[i]
        int[][] partition = new int[arity][ vecLength ];  // ??? optimisation.  Make this static.
        int[] index = new int[arity];
        
        // Extract the single column vector from  vec
        Value.Vector columnVector = this.cmpnt(col);
        
        
        // Partition the data.
        for ( int i = 0; i < vecLength; i++ ) {        
            int value = columnVector.intAt(i);   // find the value at column[i]
            try {
                partition[value][index[value]] = i;  // add 'i' to the list in the appropriate partition
            } catch ( java.lang.ArrayIndexOutOfBoundsException e ) {
                System.out.println( "-------- SelectVec -------------" );
                System.out.println( "vec = " + this );
                System.out.println( "vec.t = " + this.t );
                System.out.println( "col = " + col );
                System.out.println( "arity = " + arity );
                System.out.println( "--------------------------------" );
                throw e;
            }
            index[value]++;                      // increment the counter for appropriate partition
        }
        
        // Now we must trim the arrays to save memory wastage.
        for ( int i = 0; i < arity; i++ ) {
            int[] tempArray = new int[index[i]];
            for ( int j = 0; j < tempArray.length; j++ ) {
                tempArray[j] = partition[i][j];
            }
            partition[i] = tempArray;
        }
        
        
        // Work out which columns are to be used in the vector being returned.  If the column
        // is to be hidden, columnList is an array of column indexes missing the appropriate
        // index.  If column is not to be hidden, then null can be used.
        int[] columnList;
        if ( hideAttribute == true ) {
            columnList = new int[ structType.cmpnts.length - 1];
            for ( int i = 0; i < col; i++ ) { columnList[i] = i; }
            for ( int i = col; i < columnList.length; i++ ) { columnList[i] = i+1; }
        } 
        else {
            columnList = this.column;
        }
        
        // Row and Column indexes into originalVector
        final int[][] originalPartition;
        final int[] originalColumnList;
        
        // Check we have the correct indexes for partition.
        if ( this.row == null ) {
            originalPartition = partition;
        }
        else {
            originalPartition = new int[partition.length][];
            for ( int i = 0; i < originalPartition.length; i++ ) {
                originalPartition[i] = new int[partition[i].length];
                for ( int j = 0; j < partition[i].length; j++ ) {
                    originalPartition[i][j] = row[partition[i][j]];
                }
            }
        }
        
        //        //Check we have the correct columns.
        //        if ( columnList == null ) {
        originalColumnList = columnList;
        //        }
        //        else {
        //            originalColumnList = columnList;
        //            //originalColumnList = new int[columnList.length];
        //            //for ( int i = 0; i < columnList.length; i++ ) {
        //            //    originalColumnList[i] = columnList[column[i]];
        //            //}
        //        }
        //        
        
        
        // Create the list of vectors.  partition[i] is passed as the rows required.
        // columnList is passed as the list of columns required.
        SelectedVector[] vecList = new SelectedVector[arity];
                
        for ( int i = 0; i < vecList.length; i++) {
            // vecList[i] = new SelectedVector( this, partition[i], columnList);
            vecList[i] = new SelectedVector( this.originalVector, originalPartition[i], originalColumnList);
        }
        
        return vecList;
    }
    
    
    /** Static instance of selectCols class */
    public static SelectCols selectCols = new SelectCols();
    
    /**
     * multicol -> [int] -> multicol <br>
     * Select the specified columns from the original multicol vector.
     */
    public static class SelectCols extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -535664273473469482L;

        public SelectCols( ) { super ( new Type.Function(Type.VECTOR,SelectCols2.tt ) ); }
        public Value apply( Value v ) {
            return new SelectCols2( (Value.Vector)v );
        }
        
        public static class SelectCols2 extends Value.Function {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 5388847919704016526L;
            Value.Vector vec;
            static Type.Function tt = 
                new Type.Function( new Type.Vector(Type.DISCRETE), Type.VECTOR );
            public SelectCols2( Value.Vector vec) { super( tt ); this.vec = vec;}
            public Value apply( Value v ) {
                int[] column = new int[ ((Value.Vector)v).length() ];
                for ( int i = 0; i < column.length; i++ ) {
                    column[i] = ((Value.Vector)v).intAt(i);
                }
                return new SelectedVector(vec,null,column);
            }
        }        
    }
    
    
    /** Static instance of selectRows class */
    public static SelectRows selectRows = new SelectRows();
    
    /**
     * vec -> [int] -> vec <br>
     * Select the specified rows from the original vector.
     */
    public static class SelectRows extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 5762820015537559669L;

        public SelectRows( ) { super ( new Type.Function(Type.VECTOR,SelectRows2.tt ) ); }
        public Value apply( Value v ) {
            return new SelectRows2( (Value.Vector)v );
        }
        
        public static class SelectRows2 extends Value.Function {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -1696729167335312007L;
            Value.Vector vec;
            static Type.Function tt = 
                new Type.Function( new Type.Vector(Type.DISCRETE), Type.VECTOR );
            public SelectRows2( Value.Vector vec) { super( tt ); this.vec = vec;}
            public Value apply( Value v ) {
                System.out.println("v = " + v);
                System.out.println("v.t = " + v.t);
                Value.Vector indexVec = (Value.Vector)v;
                int[] row = new int[ indexVec.length() ];
                for ( int i = 0; i < row.length; i++ ) {
                    row[i] = indexVec.intAt(i);
                }
                return new SelectedVector(vec,row,null);
            }
        }
        
    }
    
    /** Static instance of SplitOn class */
    public static SplitOn splitOn = new SplitOn();
    
    /**
     * multicolVec -> bounded discrete -> [multiocolVec]
     * Split the original vector into N vectors where N is the arity of the bounded discrete vector.
     */
    public static class SplitOn extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -9121749933307745532L;

        public SplitOn( ) { super ( new Type.Function(Type.VECTOR,SplitOn2.tt ) ); }
        public Value apply( Value v ) {
            return new SplitOn2( (Value.Vector)v );
        }
        
        public static class SplitOn2 extends Value.Function {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -1536941734020737355L;
            Value.Vector vec;
            static Type.Function tt = 
                new Type.Function( Type.DISCRETE, Type.VECTOR );
            public SplitOn2( Value.Vector vec) { super( tt ); this.vec = vec;}
            public Value apply( Value v ) {
                
                // determine variable to be split upon
                int x = ((Value.Discrete)v).getDiscrete();
                
                // perform the split (no not hide any columns.)
                SelectedVector[] split = dTreeSplitVector( vec, x, false );
                
                return new VectorFN.FatVector( split );
            }
        }
        
    }
    
}
