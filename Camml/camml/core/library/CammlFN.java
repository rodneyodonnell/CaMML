//
// Various usefull functions for camml
//
// Copyright (C) 2002 Rodney O'Donnell, Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: CammlFN.java
// Author: {rodo,lhope}@csse.monash.edu.au

package camml.core.library;

import cdms.core.*;


/**
 * This class contains useful functions which should probably become part of CDMS and be used by
 * camml.
 */
public class CammlFN
{
    /** static instance of View */
    public static View view = new View();

    /** View returns a view of a data set.  That is to say, when given a data set it masks out some 
     *  of the columns and only shows the remaining entries.  This saves the need to create copies 
     *  of large data structures to pass to function which may only need to deal with a subset of 
     *  the columns. <br>
     *
     *  [(t1,t2..tn)] -> [discrete] -> [(ti...tn)]   <br>
     *  eg. [(x0,x1,x2,x3)] -> [0,2] -> [(x0,x2)]    <br>
     */
    public static class View extends Value.Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -9156289605705170006L;
	public static Type.Function tt = 
	    new Type.Function(new Type.Vector(Type.STRUCTURED), View2.tt);

	public View() 
	{
	    super(tt);
	}

	public Value apply( Value v )
	{
	    return new View2( (Value.Vector)v );
	}
    }
    
    /**
     * Takes a vector of discrete and returns the appropriate columns from the data set given in 
     * the constructor.  The same data vector may be specified multiple times or not at all if this 
     * is required. <br>
     */
    public static class View2 extends Value.Function
    {	
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -1625312507793423480L;
	/** The original unprocessed data*/
	protected Value.Vector fullData;
	public static Type.Function tt = 
	    new Type.Function(new Type.Vector(Type.DISCRETE), new Type.Vector(Type.STRUCTURED));

	public View2( Value.Vector fullData)
	{
	    super(tt);
	    this.fullData = fullData;
	}

	/** Extract all columns and return them in a FixedLengthMultiCol vector*/
	public Value apply( Value v )
	{
	    Value.Vector valueMask = (Value.Vector)v;
	    int[] mask = new int[valueMask.length()];
	    for ( int i = 0; i < mask.length; i++ ) {
		mask[i] = valueMask.intAt(i);
	    }
	    return new SelectedVector( fullData, null, mask );
	}
    }
    
    /** 
     * This implementation of Multicol allows vectors with zero components to still have a valid 
     * length.  Regular MultiCol vectors do not give the correct length of an empty vector.
     */
    public static class FixedLengthMultiCol extends VectorFN.MultiCol
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 763540375966316577L;
	private int vectorLength;
	public FixedLengthMultiCol( Value.Structured v, int vectorLength)
	{
	    super(v);
	    this.vectorLength = vectorLength;
	}
	
	public int length()
	{
	    return vectorLength;
	}
    }


}
