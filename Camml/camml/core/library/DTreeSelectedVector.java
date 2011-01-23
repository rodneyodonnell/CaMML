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
// Vector class for compatibility with DTrees.
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
 * used as a way of storeing a sorted vector, or making a vector appear to contain duplicates
 * without the overhead of creating new objects. <br>
 *
 * NOTE:  null may be passed to constructor if all rows/columns are desired. <br>
 * NOTE2: For vectors which contain only a single column of data (eg. FastDiscreteVector) the value
 *        passed to column should always be null.
 */

public class DTreeSelectedVector extends SelectedVector
                                         //public class DTreeSelectedVector extends Value.Vector
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 8607496249958056407L;

    /** Original Vector all transformations are based on. */
    protected final Value.Vector d_originalVector;
    
    /** which rows should be visible */
    protected final int[] d_row;
    
    /** which columns should be visible */
    protected final int[] d_column;
    
    public int[] d_getRows() 
    {
        return d_row;
    }
    
    public int[] d_getColumns() 
    {
        return d_column;
    }
    
    public final int numCmpnts;
    protected final Value.Vector[] cmpntArray;
    
    /**
     * If vector is a vector of structured, return the number of columns.
     * If a vector of anything else, return -1;
     */
    public static int d_getNumCmpnts( Type.Vector vType ) {
        if ( vType.elt instanceof Type.Structured == false ) {
            return -1;
        }
        Type.Structured sType = (Type.Structured)vType.elt;
        return sType.cmpnts.length;
    }
    
    /** Create a vector which behaves in the same way as v, but is selectable. */
    public DTreeSelectedVector( Value.Vector v )
    {
        //super(null); 
        super( v );
        this.d_originalVector = v;
        
        //if ( originalVector instanceof SelectedVector ) { System.out.println("Orig=selected"); }
        
        // With this constructor, all columns are visible
        d_row = null;
        
        // With this constructor all rows are visible
        d_column = null;
        
        numCmpnts = d_getNumCmpnts( (Type.Vector)this.t );
        if ( numCmpnts != -1 ) cmpntArray = new Value.Vector[numCmpnts];
        else cmpntArray = null;
    }
    
    /** Create a vector which behaves in the same way as v, but with only the selected columns
     *  present.  The values in column[] should not be modified after passing to selectedVector,
     *  if modification is required pass a clone of column[].  If all columns are to be used, null
     *  may be passed in place of column <br>
     *
     *  row operates in the same manner specified above for column.
     */
    public DTreeSelectedVector( Value.Vector v, final int[] row, final int[] column )
    {
        // make a new type from the selected columns
        //super(null);
        super(  v, row, column );
        this.d_originalVector = v;
        //if ( originalVector instanceof SelectedVector ) { System.out.println("Orig=selected"); }
        
        // Specify visible rows.
        this.d_row = row;
        
        // Specify visible columns
        this.d_column = column;    
        
        numCmpnts = d_getNumCmpnts( (Type.Vector)this.t );
        if ( numCmpnts != -1 ) cmpntArray = new Value.Vector[numCmpnts];
        else cmpntArray = null;
    }
    
    
    public int intAt( int i ) {
        if ( d_column != null ) { throw new RuntimeException("Cannot get int from multicol"); }
        
        // If some rows need to be removed, remove them.
        if ( d_row == null ) { return d_originalVector.intAt(i); }
        else { return d_originalVector.intAt( d_row[i] );}
        
    }
    
    public double weight(int i) { 
        if ( d_row == null ) { return d_originalVector.weight(i); }
        else { return d_originalVector.weight( d_row[i] );}
    }
    
    /** return the i'th element in the vector */
    public Value elt( int i ) 
    {
        // Extract the appropriate element.
        Value element;
        if ( d_row == null ) { element = d_originalVector.elt(i); }
        else { element = d_originalVector.elt( d_row[i] );}
        
        // If some rows need to be removed, remove them.
        Value selectedElement;
        if ( d_column == null ) { selectedElement = element; }
        else { selectedElement = new SelectedStructure((Value.Structured)element, d_column ); }
        
        return selectedElement;
    }
    
    /** return the length of the vector*/
    public int length()
    {
        int len;
        if ( d_row == null ) { len = d_originalVector.length(); }
        else { len = d_row.length; }
        return len;
    }
    
    /** If vector is a multicol vector, return the specified column */
    public Value.Vector cmpnt(int col)
    {         
        if ( cmpntArray[col] == null ) {
            if ( d_column != null )
                cmpntArray[col] = new DTreeSelectedVector(d_originalVector.cmpnt(d_column[col]), d_row, null);
            else
                cmpntArray[col] = new DTreeSelectedVector( d_originalVector.cmpnt(col), d_row, null );
        }
        return cmpntArray[col];
    }
    
    
    
    
    
    /** function to return a string based on an int[] (does not belong here!)*/
    public static String d_arrayToString( int[] x ) { 
        if ( x == null ) { return null; }
        String s = "[";
        for (int i = 0; i < x.length; i++) {
            s += x[i];
            if ( i != x.length - 1) {
                s += ", ";
            }
        }
        s += "]";
        return s;
    }
    
    /** function to return a string based on an Object[] (does not belong here!)*/
    public static String d_arrayToString( Object[] x ) { 
        if ( x == null ) { return null; }
        String s = "[";
        for (int i = 0; i < x.length; i++) {
            s += x[i];
            if ( i != x.length - 1) {
                s += ", ";
            }
        }
        s += "]";
        return s;
    }
    
    
    
    
    
    
    /** Use the makeSelectedStructure type function to create a new Vector type with the appropriate
     *  columns.
     *
     *  If columns is specified as null then we do not make the assumption that vType is from a
     *  multicol vector.
     */
    public static Type.Vector d_makeSelectedVectorType( Type.Vector vType, int[] col ) 
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
    public DTreeSelectedVector d_copyRowSplit( Value.Vector vec ) {
        return new DTreeSelectedVector( vec, d_row, null );
    }
    
    
    /** Static version of split() */
    public static DTreeSelectedVector[] d_splitVector( Value.Vector vec, int col, boolean hideAttributes ){
        DTreeSelectedVector sVec;
        if ( vec instanceof DTreeSelectedVector ) { sVec = (DTreeSelectedVector)vec; }
        else { sVec = new DTreeSelectedVector(vec); }
        
        return sVec.d_split(col,hideAttributes);
    }
    
    /** Cache for partially split vectors */
    //protected SplitVectorCache splitCache = null;
    
    /** Split vector based on all combinaitons of discrete attributes in cols[]*/
    //public DTreeSelectedVector split( int[] cols ) {
    //    return null;
    //}
    
    /**
     *  The splitVector function splits the this vector based on the discrete attribute specified 
     *  by col.  The hideAttribute flag indicates if the column being split on should be removed
     *  from the dataset or left as is.
     */
    public DTreeSelectedVector[] d_split( int col, boolean hideAttribute )
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
        DTreeSelectedVector[] vecList = new DTreeSelectedVector[arity];
        
        for ( int i = 0; i < vecList.length; i++) {
            vecList[i] = new DTreeSelectedVector( vec, partition[i], columnList);
        }
        
        return vecList;
    }
    
    
    /** Static instance of selectCols class */
    public static D_SelectCols d_selectCols = new D_SelectCols();
    
    /**
     * multicol -> [int] -> multicol <br>
     * Select the specified columns from the original multicol vector.
     */
    public static class D_SelectCols extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 2392102906025754098L;

        public D_SelectCols( ) { super ( new Type.Function(Type.VECTOR,D_SelectCols2.tt ) ); }
        public Value apply( Value v ) {
            return new D_SelectCols2( (Value.Vector)v );
        }
        
        public static class D_SelectCols2 extends Value.Function {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -820121797708824050L;
            
            Value.Vector vec;
            static Type.Function tt = 
                new Type.Function( new Type.Vector(Type.DISCRETE), Type.VECTOR );
            public D_SelectCols2( Value.Vector vec) { super( tt ); this.vec = vec;}
            public Value apply( Value v ) {
                int[] column = new int[ ((Value.Vector)v).length() ];
                for ( int i = 0; i < column.length; i++ ) {
                    column[i] = ((Value.Vector)v).intAt(i);
                }
                return new DTreeSelectedVector(vec,null,column);
            }
        }        
    }
    
    
    /** Static instance of selectRows class */
    public static D_SelectRows selectRows = new D_SelectRows();
    
    /**
     * vec -> [int] -> vec <br>
     * Select the specified rows from the original vector.
     */
    public static class D_SelectRows extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2731733444194157929L;

        public D_SelectRows( ) { super ( new Type.Function(Type.VECTOR,D_SelectRows2.tt ) ); }
        public Value apply( Value v ) {
            return new D_SelectRows2( (Value.Vector)v );
        }
        
        public static class D_SelectRows2 extends Value.Function {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 3993790147914288376L;
            
            Value.Vector vec;
            static Type.Function tt = 
                new Type.Function( new Type.Vector(Type.DISCRETE), Type.VECTOR );
            public D_SelectRows2( Value.Vector vec) { super( tt ); this.vec = vec;}
            public Value apply( Value v ) {
                System.out.println("v = " + v);
                System.out.println("v.t = " + v.t);
                Value.Vector indexVec = (Value.Vector)v;
                int[] row = new int[ indexVec.length() ];
                for ( int i = 0; i < row.length; i++ ) {
                    row[i] = indexVec.intAt(i);
                }
                return new DTreeSelectedVector(vec,row,null);
            }
        }
        
    }
    
    /** Static instance of SplitOn class */
    public static D_SplitOn d_splitOn = new D_SplitOn();
    
    /**
     * multicolVec -> bounded discrete -> [multiocolVec]
     * Split the original vector into N vectors where N is the arity of the bounded discrete vector.
     */
    public static class D_SplitOn extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 3014973535567643952L;

        public D_SplitOn( ) { super ( new Type.Function(Type.VECTOR,D_SplitOn2.tt ) ); }
        public Value apply( Value v ) {
            return new D_SplitOn2( (Value.Vector)v );
        }
        
        public static class D_SplitOn2 extends Value.Function {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -660548570454923837L;
            
            Value.Vector vec;
            static Type.Function tt = 
                new Type.Function( Type.DISCRETE, Type.VECTOR );
            public D_SplitOn2( Value.Vector vec) { super( tt ); this.vec = vec;}
            public Value apply( Value v ) {
                
                // determine variable to be split upon
                int x = ((Value.Discrete)v).getDiscrete();
                
                // perform the split (no not hide any columns.)
                DTreeSelectedVector[] split = d_splitVector( vec, x, false );
                
                return new VectorFN.FatVector( split );
            }
        }
        
    }
    
}
