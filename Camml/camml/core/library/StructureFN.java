//
// CDMS : Functions performed on Structures (similar to VectorFN)
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
// note : code based on cdms.core.VectorFN.java
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: StructureFN.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import java.util.Arrays;

import cdms.core.*;

/**
   StructureFN contains useful functions pertaining to structures, and useful implementations of
   Value.Structure.  Some vector implementations are used by the functions in StructureFN, and some
   are for general use with CDMS Java code.  A list of structure implementations follows:<BR>
   <UL>
   <LI> EmptyStructure
   <LI> FastContinuousStructure
   </UL>
   @see Value.Structure
   @see Type.Structure
 */

public class StructureFN extends Module.StaticFunctionModule
{
    public StructureFN()
    {
	super("Standard",Module.createStandardURL(StructureFN.class),StructureFN.class);
    }

    /** <code>(t,t,t) -> [t]</code> Turns a struct (all of the same type) into a vector. */
    public static StructToVector structToVector = new StructToVector();

    /** <code>(t,t,t) -> [t]</code> Turns a struct (all of the same type) into a vector. */
    public static class StructToVector extends Value.Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 930648534569106753L;

	public StructToVector() 
	{ 
	    super(new Type.Function( Type.STRUCTURED, Type.VECTOR ));
	}
	
	public Value apply(Value v)
	{
	    return new VectoredStructure((Value.Structured)v);
	}
	
    }

    /** <code>[t] -> (..t..)</code> Turns a vector into a struct (all of the same type)  */
    public static VectorToStruct vectorToStruct = new VectorToStruct();

    /** <code>[t] -> (..t..)</code> Turns a vector into a struct (all of the same type)  */
    public static class VectorToStruct extends Value.Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 7320092335749105942L;

	public VectorToStruct() 
	{ 
	    super(new Type.Function(  Type.VECTOR, Type.STRUCTURED ));
	}
	
	public Value apply(Value v)
	{
	    return new StructuredVector((Value.Vector)v);
	}
	
    }



    ///////////////////////////// STRUCTURE IMPLEMENTATIONS ////////////////////////////////////////


    /**
     * FastContinuousStructure is a fast and lightweight way to store a set of continuous values.
     * Values are stored as an array of doubles, and only converted to values when cmpnt() is used.
     * Use doubleCmpnt(i) to access values as doubles. <br>
     * Using a structure of doubles is (sometimes) better than using a vector when typechecking
     * is required on the length of the structure.
     */
    public static class FastContinuousStructure extends Value.Structured
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 1611874962661245144L;
	protected double data[];
	
	/** Make the type of the structure.  Type is a structure of continuous of a given length */
	protected static Type.Structured makeType( int length )
	{
	    Type.Continuous typeArray[] = new Type.Continuous[ length ];
	    for (int i = 0; i < length; i++)
		typeArray[i] = Type.CONTINUOUS;
	    return new Type.Structured(typeArray);
	}

	public FastContinuousStructure( double data[] )
	{
	    super( makeType(data.length) );
	    this.data = data;
	}
		
	public Value cmpnt(int i)
	{
	    try {
		return new Value.Continuous( data[i] );
	    }
	    catch ( ArrayIndexOutOfBoundsException e ) {
		System.out.println("element " + i + " not available in " + toString() );
		throw e;
	    }
	}

	/** shortcut to doubles. */
	public double doubleCmpnt( int i ) 
	{
	    return data[i];
	}

	/** shortcut to ints. */
	public int intCmpnt( int i )
	{
	    return (int)data[i];
	}

	public int length()
	{
	    return data.length;
	}
    }

    /**
     * FastDiscreteStructure is analguous to FastContinuousStructure, naturally working only with
     * discrete values.
     * @see FastContinuousStructure
     */
    public static class FastDiscreteStructure extends Value.Structured
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -8709490104663072832L;
	
		protected final int data[];
	
	/** Make the type of the structure.  Type is a structure of continuous of a given length */
	protected static Type.Structured makeType( int length )
	{
	    Type.Discrete typeArray[] = new Type.Discrete[ length ];
	    for (int i = 0; i < length; i++)
		typeArray[i] = Type.DISCRETE;
	    return new Type.Structured(typeArray);
	}

	public FastDiscreteStructure( int data[] )
	{
	    super( makeType(data.length) );
	    this.data = data;
	}

	public FastDiscreteStructure( Type.Structured t, int data[] )
	{
	    super( t );
	    this.data = data;
	}

	public Value cmpnt(int i)
	{
	    try {	    	
		return new Value.Discrete( (Type.Discrete)((Type.Structured)t).cmpnts[i], data[i] );
	    }
	    catch ( ArrayIndexOutOfBoundsException e ) {
		System.out.println("element " + i + " not available in " + toString() );
		throw e;
	    }
	}

	/** shortcut to doubles. */
	public double doubleCmpnt( int i ) 
	{
	    return (double)data[i];
	}

	/** shortcut to ints. */
	public int intCmpnt( int i )
	{
	    return data[i];
	}

	public int length()
	{
	    return data.length;
	}
	
    public boolean equals(Object o)
    {
      if(o instanceof FastDiscreteStructure)
      {
        FastDiscreteStructure s = (FastDiscreteStructure)o;
        return Arrays.equals(data, s.data);
      }
      else { return super.equals(o); }
    }

    public int hashCode() { 
    	// "optimised" hashcode function.
    	return Arrays.hashCode(data);
    }

    }

    /**
     * Takes a structure (all of the same type) and returns a vector containing equivelent data.
     * IllegalArgumentException is thrown if components are of different types.
     */
    public static class VectoredStructure extends Value.Vector
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -4519418602995172707L;

	/** Make the type of the structure.  Type is a structure of continuous of a given length */
	protected static Type.Vector makeType( Type.Structured structType )
	{
	    Type vecSubType = structType.cmpnts[0];
	    for ( int i = 0; i < structType.cmpnts.length; i++ )
		if ( vecSubType != structType.cmpnts[i] )
		    throw new IllegalArgumentException("Types do not match in VectoredStructure :" +
						       "A structure must have a constant type to " +
						       "bt turned into a vector.");
	    return new Type.Vector( vecSubType );
	}


	Value.Structured struct;

	public VectoredStructure( Value.Structured struct )
	{
	    super(makeType((Type.Structured)struct.t));
	    this.struct = struct;
	}
	
	public int length()
	{
	    return struct.length();
	}

	public Value elt( int i ) 
	{
	    return struct.cmpnt(i);
	}

	public double doubleAt(int i)
	{
	    return struct.doubleCmpnt(i);
	}
	
	public int intAt(int i) 
	{ 
	    return struct.intCmpnt(i);
	}

    }

    /**
     * Takes a vector and returns a structure containing equivelent data.
     */
    public static class StructuredVector extends Value.Structured
    {
	
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -5502483328292942389L;

	/** Make the type of the structure.  Type is a structure of components of a given length */
	protected static Type.Structured makeType( Type cmpntType, int length )
	{
	    Type typeArray[] = new Type[ length ];
	    for (int i = 0; i < length; i++)
		typeArray[i] = cmpntType;
	    return new Type.Structured(typeArray);
	}

	Value.Vector vec;

	public StructuredVector( Value.Vector vec )
	{
	    super(makeType((Type.Vector)vec.t, vec.length()));
	    this.vec = vec;
	}
	
	public int length()
	{
	    return vec.length();
	}

	public Value cmpnt( int i ) 
	{
	    return vec.elt(i);
	}

	public double doubleCmpnt(int i)
	{
	    return vec.doubleAt(i);
	}
	
	public int intCmpnt(int i) 
	{ 
	    return vec.intAt(i);
	}

    }

}

