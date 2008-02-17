//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: VectorFN.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.core;

/** VectorFN contains useful functions pertaining to vectors, and useful implementations of
    Value.Vector.  Some vector implementations are used by the functions in VectorFN, and some
    are for general use with CDMS Java code.  A list of vector implementations follows:<BR>
    <UL>
    <LI> Empty Vector
    <LI> ConstantVector
    <LI> FastDiscreteVector
    <LI> FastContinuousVector
    <LI> UniformVector
    <LI> UniformDiscreteVector
    <LI> UniformContinuousVector
    <LI> FatVector
    <LI> FatWeightVector
    <LI> DiscreteVector
    <LI> ContinuousVector
    <LI> SubsetVector
    <LI> WeightedVector
    <LI> ListVector
    <LI> IotaN
    <LI> IotarN
    <LI> MultiCol
    <LI> MapFV
    <LI> AppendVector
    <LI> VSubVector
    <LI> VSumVector
    </UL>
    @see Value.Vector
    @see Type.Vector
*/
public class VectorFN extends Module.StaticFunctionModule
{
  public VectorFN()
  {
    super("Standard",Module.createStandardURL(VectorFN.class),VectorFN.class);
  }


  /** Differencing function for continuous vectors.  
      <code>Discrete -> [Continuous] -> [Continuous]</code> 
  */
  public static final Diff diff = new Diff();

  /** <code>Discrete -> [a] -> [a]</code><P>
      This function takes the differences between elements in a vector of continuous values.  
      The first parameter is the lag.  A lag of one takes differences between successive elements.
  */
  public static class Diff extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2960357693065956931L;
	public static final Type.Function TT = new Type.Function(Type.DISCRETE,Diff2.TT);

    public Diff()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Diff2(((Value.Scalar) v).getDiscrete());
    }
  }

  public static class Diff2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6894548118724116123L;
	public static final Type.Vector CONTVECTOR = new Type.Vector(Type.CONTINUOUS);
    public static final Type.Function TT = new Type.Function(CONTVECTOR,CONTVECTOR);

    private int lag;

    public Diff2(int lag)
    {
      super(TT);
      this.lag = lag;
    }

    public Value apply(Value v)
    {
      return new DiffVector((Value.Vector) v);
    }

    public class DiffVector extends Value.Vector
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 7080519302396698776L;
	private double data[];

      public DiffVector(Value.Vector v)
      {
        super(CONTVECTOR);

        data = new double[v.length() - lag];
        for (int i = lag; i < v.length(); i++)
          data[i - lag] = v.doubleAt(i) - v.doubleAt(i - lag);
      }

      public int length()
      {
        return data.length;
      }

      public double doubleAt(int i)
      {
        return data[i];
      }

      public Value elt(int i)
      {
        return new Value.Continuous(data[i]);
      }
    }
  }

  /** <code>Discrete -> [a] -> ([a],[a])</code> */
  public static SplitAt splitAt = new SplitAt();

  /** <code>Discrete -> [a] -> ([a],[a])</code><P>
      This function splits a vector v of n elements at index i into a structure of two vectors, the
      first comprising elements 0 through i-1, and the second comprising elements i through n-1.
      It is a curried function which takes the discrete value i, and returns a function expecting
      the vector v as a parameter.
  */
  public static class SplitAt extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4549376115983704010L;
	public static final Type.Variable A = new Type.Variable();
    public static final Type.Vector AVECTOR = new Type.Vector(A);
    public static final Type.Function TT = new Type.Function(Type.DISCRETE,SplitAt2.TT);

    public SplitAt() 
    { 
      super(TT);
    }

    public Value apply(Value v)
    {
      return new SplitAt2(((Value.Scalar) v).getDiscrete());
    }
  }

  /** <code>[a] -> ([a],[a])</code><P>
      This function splits a vector v of n elements at index i into a structure of two vectors, the
      first comprising elements 0 through i-1, and the second comprising elements i through n-1.
      It is the result of a curried function which takes the discrete value i.  This function
      expects the vector v and returns the structured value.
      @see VectorFN.SplitAt
  */
  public static class SplitAt2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2683174919026624684L;

	public static final Type.Function TT = 
      new Type.Function(SplitAt.AVECTOR,new Type.Structured(new Type[] { SplitAt.AVECTOR,
        SplitAt.AVECTOR } ));

    protected int n;

    public SplitAt2(int n) 
    { 
      super(TT);
      this.n = n;
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector) v;
      if (n <= 0) return emptyVector;
      return new Value.DefStructured(new Value[] { vv.sub(0,n-1), vv.sub(n,vv.length()-1) });
    }
  }
  
  public static XValidation xValidation = new XValidation();
  
  /** Folds -> Training_Proportion -> Vector -> String -> boolean */
  public static class XValidation extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8811829823028224905L;
	public static Type.Function TT = new Type.Function(Type.DISCRETE, XValidation2.TT, false, false);

    public XValidation()
    {
      super(TT);
    }
  
    public Value apply(Value v)
    {  
      return new XValidation2(((Value.Scalar)v).getDiscrete());
    }
  }

  /** Training_Proportion -> Vector -> String -> boolean */
  public static class XValidation2 extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2466030254118583595L;
	public static Type.Function TT = new Type.Function(Type.CONTINUOUS, XValidation3.TT, false, false);
    private int folds;

    public XValidation2(int folds)
    {
      super(TT);
      this.folds = folds;
    }
  
    public Value apply(Value v)
    {
      return new XValidation3(((Value.Scalar)v).getContinuous(), folds);
    }
  }

  /** Vector -> String -> boolean */
  public static class XValidation3 extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 449888758619697888L;
	public static Type.Function TT = new Type.Function(Type.VECTOR, XValidation4.TT, false, false);
    private int folds;
    private double train;

    public XValidation3(double train, int folds)
    {
      super(TT);
      this.folds = folds;
      this.train = train;
    }
  
    public Value apply(Value v)
    {
      return new XValidation4((Value.Vector)v, train, folds);
    }
  }

  /** String -> boolean */
  public static class XValidation4 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8294694321783398411L;
	public static Type.Function TT = new Type.Function(Type.STRING, Type.BOOLEAN, false, false);
    private int folds;
    private double train;
    private Value.Vector v;
    private java.util.Random r;

    public XValidation4(Value.Vector v, double train, int folds)
    {
      super(TT);
      this.folds = folds;
      this.train = train;
      this.v = v;
      r = new java.util.Random(123);
    }
  
      /** Return an array containing all ints [0..length-1] in a random order. */
      public int[] getRandomArray( int length ) {
	  int[] array = new int[length];
	  for ( int i = 0; i < array.length; i++ ) array[i] = i;
	  for ( int i = 0; i < array.length; i++ ) {
	      int x = i + r.nextInt( length - i );
	      int temp = array[x];
	      array[x] = array[i];
	      array[i] = temp;
	  }
	  return array;
      }

      public Value apply(Value s)
      {
	  int numSamples = v.length();
  
	  final int trainLength = (int)(train * numSamples);
	  final int testLength = numSamples - trainLength;

	  // Arrays to store generated vectors into.
	  Value.Vector[] trainArray = new Value.Vector[folds];
	  Value.Vector[] testArray = new Value.Vector[folds];

	  // For each fold
	  for ( int i = 0; i < folds; i++ ) {
	      // First trainLength elemends of randomArray are training, remainder are test.
	      int[] randomArray = getRandomArray(numSamples);
	      int[] trainIndex = new int[trainLength];
	      int[] testIndex = new int[testLength];
	      
	      // Partition data into training and test sets.
	      for ( int j = 0; j < trainIndex.length; j++ ) {
		  trainIndex[j] = randomArray[j];
	      }
	      for ( int j = 0; j < testIndex.length; j++ ) {
		  testIndex[j] = randomArray[trainLength+j];
	      }

	      // Save vectors in train/test arrays.
	      trainArray[i] = new SubsetVector( v, trainIndex, trainLength );
	      testArray[i] = new SubsetVector( v, testIndex, testLength );

	  }

	  // return a ([[train]],[[test]])
	  Value.Vector trainVecVec = new VectorFN.FatVector(trainArray);
	  Value.Vector testVecVec = new VectorFN.FatVector(testArray);
	  return new Value.DefStructured( new Value[] {trainVecVec, testVecVec} );

      }
      
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  /** <code>(a -> b -> a) -> a -> [b] -> a</code> Fold left function. */
  public static FoldL foldl = new FoldL();

  /** <code>(a -> b -> a) -> a -> [b] -> a</code>
      <P>
      Fold left function.
  */
  public static class FoldL extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8719959895282377917L;
	public static final Type.Variable A = new Type.Variable();
    public static final Type.Variable B = new Type.Variable();

    public static final Type.Function TT = new Type.Function(new Type.Function(A,
      new Type.Function(B,A)),FoldL2.TT);

    public FoldL() 
    { 
      super(TT);
    }

    public Value apply(Value v)
    {
      return new FoldL2((Value.Function) v);
    }
  }

  /** <code>a -> [b] -> a</code>
      <P>
      @see VectorFN.FoldL
  */
  public static class FoldL2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3417059008492993190L;

	public static final Type.Function TT = new Type.Function(FoldL.A,FoldL3.TT);

    protected Value.Function opr;

    public FoldL2(Value.Function opr) 
    { 
      super(TT);
      this.opr = opr;
    }

    public Value apply(Value v)
    {
      return new FoldL3(opr,v);
    }
  }

  /** <code>[b] -> a</code>
      <P>
      @see VectorFN.FoldL
  */
  public static class FoldL3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4182071233861564936L;

	public static final Type.Function TT = new Type.Function(new Type.Vector(FoldL.B),FoldL.A);

    protected Value.Function opr;
    protected Value initial;

    public FoldL3(Value.Function opr,Value initial) 
    { 
      super(TT);
      this.opr = opr;
      this.initial = initial;
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector) v;
      Value c = initial;
      for (int i = 0; i < vv.length(); i++)
        c = ((Value.Function) opr.apply(c)).apply(vv.elt(i));

      // If you (the reader) have time, you could implement a faster version 
      // for vectors of double or ints using VariableContinuous and doubleAt... 

      return c;
    }

  }

  /** <code>[(a,b,c,...)] -> Discrete -> [a]</code>Get a column from a vector of structured. */
  public static VCmpnt vcmpnt = new VCmpnt();

  /** <code>[(a,c,c,...)] -> Discrete -> [a]</code><P>This function takes a vector v of structured
      values and returns a function f expecting a discrete parameter i.  The resultant function f
      returns column i of the (multi-column) vector v.
      @see VectorFN.VCmpnt2
  */
  public static class VCmpnt extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6319054252566950342L;
	public static final Type.Function TT = 
      new Type.Function(new Type.Vector(Type.STRUCTURED),VCmpnt2.TT);

    public VCmpnt() 
    { 
      super(TT);
    }

    public Value apply(Value v)
    {
      return new VCmpnt2((Value.Vector) v);
    }
  }

  /** <code>Discrete -> [a]</code><P>This function is the result of the (curried) function VCmpnt.
      VCmpnt is given a vector v and produces an instance of this function.  This function expects
      a discrete parameter i, and returns column i of the vector v (v should be a vector of
      structured).
  */
  public static class VCmpnt2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7950108527736417013L;

	public static final Type.Function TT = new Type.Function(Type.DISCRETE,Type.VECTOR);

    protected Value.Vector vec;

    public VCmpnt2(Value.Vector vec) 
    { 
      super(TT);
      this.vec = vec;
    }

    public Value apply(Value v)
    {
      return vec.cmpnt(((Value.Scalar) v).getDiscrete());
    }
  }

  /** <code>[t] -> [t] -> [t]</code>Subraction of two vectors */
  public static VSub vsub = new VSub();

  /** <code>[t] -> [t] -> [t]</code><P>This is a curried function, expecting a vector v of scalar
      values, and producing a VSub1 function expecting another vector w of scalar values.  The
      length of v and w should be equal.  The resultant VSub1 function returns a vector z of the
      same length as v and w, where z.elt(i) = v.elt(i) - w.elt(i).
      @see VectorFN.VSub1
      @see VectorFN.VSubVector
  */
  public static class VSub extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7401925820202762741L;
	public static final Type.Vector vectorType = 
      new Type.Vector(Type.TYPE,new Type.Variable(),false,false,false,false);
    public static final Type.Function thisType = 
      new Type.Function(vectorType,VSub1.thisType,false,false);

    public VSub() 
    { 
      super(thisType);
    }

    public Value apply(Value v)
    {
      return new VSub1((Value.Vector) v);
    }
  }

  /** <code>[t] -> [t]</code><P>This function is produced by the (curried) VSub function which
      passes it a vector v in the constructor.  This function expects a vector w (of the same
      length as v), and produces a vector z (also of the same length) where z.elt(i) = v.elt(i) - 
      w.elt(i).  The element type of the vectors v, w, and z should be (a sub-type of) Scalar.
      @see VectorFN.VSub
      @see VectorFN.VSubVector
  */
  public static class VSub1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7308667363025815525L;
	public static final Type.Function thisType = 
      new Type.Function(VSub.vectorType,VSub.vectorType,false,false);
    public Value.Vector v1;

    public VSub1(Value.Vector v1) 
    { 
      super(thisType);
      this.v1 = v1;
    }

    public Value apply(Value v)
    {
      return new VSubVector(v1,(Value.Vector) v);
    }
  }

  /** <code>[t] -> [t] -> [t]</code>Sum of two vectors */
  public static VSum vsum = new VSum();

  /** <code>[t] -> [t] -> [t]</code><P>This is a curried function, expecting a vector v of scalar
      values, and producing a VSum1 function expecting another vector w of scalar values.  The
      length of v and w should be equal.  The resultant VSub1 function returns a vector z of the
      same length as v and w, where z.elt(i) = v.elt(i) + w.elt(i).
      @see VectorFN.VSum1
      @see VectorFN.VSumVector
  */
  public static class VSum extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5687577981652676606L;
	public static final Type.Vector vectorType = 
      new Type.Vector(Type.TYPE,new Type.Variable(),false,false,false,false);
    public static final Type.Function thisType = 
      new Type.Function(vectorType,VSum1.thisType,false,false);

    public VSum() 
    { 
      super(thisType);
    }

    public Value apply(Value v)
    {
      return new VSum1((Value.Vector) v);
    }
  }

  /** <code>[t] -> [t]</code><P>This function is produced by the (curried) VSum function which
      passes it a vector v in the constructor.  This function expects a vector w (of the same
      length as v), and produces a vector z (also of the same length) where z.elt(i) = v.elt(i) + 
      w.elt(i).  The element type of the vectors v, w, and z should be (a sub-type of) Scalar.
      @see VectorFN.VSum
      @see VectorFN.VSumVector
  */
  public static class VSum1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3435408160901612228L;
	public static final Type.Function thisType = 
      new Type.Function(VSum.vectorType,VSum.vectorType,false,false);
    public Value.Vector v1;

    public VSum1(Value.Vector v1) 
    { 
      super(thisType);
      this.v1 = v1;
    }

    public Value apply(Value v)
    {
      return new VSumVector(v1,(Value.Vector) v);
    }
  }

  /** <code>[Scalar] -> Continuous</code>Computes the minimum of a vector of scalars. */
  public static Minimum minimum = new Minimum();

  /** <code>[Scalar] -> Continuous</code><P>Computes the minimum of a vector of scalars. */
  public static class Minimum extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7927153903120835581L;

	public Minimum() 
    { 
      super(new Type.Function(new Type.Vector(Type.TYPE,Type.SCALAR,false,false,false,false),
                              Type.CONTINUOUS,false,false)); 
    }

    public double applyDouble(Value v)
    {
      Value.Vector vector = (Value.Vector) v;
      double min = Double.MAX_VALUE;
      for (int i = 0; i < vector.length(); i++)
      {
        double d = vector.doubleAt(i);
        if (d < min) min = d;
      }
      return min;
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }

  }

  /** <code>[Scalar] -> Continuous</code>Computes the maximum of a vector of scalars. */
  public static Maximum maximum = new Maximum();

  /** <code>[Scalar] -> Continuous</code><P>Computes the maximum of a vector of scalars. */
  public static class Maximum extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1766895006105552913L;

	public Maximum() 
    { 
      super(new Type.Function(new Type.Vector(Type.TYPE,Type.SCALAR,false,false,false,false),
                              Type.CONTINUOUS,false,false)); 
    }

    public double applyDouble(Value v)
    {
      Value.Vector vector = (Value.Vector) v;
      double max = - Double.MAX_VALUE;
      for (int i = 0; i < vector.length(); i++)
      {
        double d = vector.doubleAt(i);
        if (d > max) max = d;
      }
      return max;
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }

  /** <code>[Scalar] -> (Continuous,Continuous)</code>Computes the minumum and maximum of a 
      vector of scalars. 
  */
  public static Minmax minmax = new Minmax();

  /** <code>[Scalar] -> (Continuous,Continuous)</code><P>Computes the minimum and maximum 
      of a vector of scalars. 
  */
  public static class Minmax extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3792264676779269773L;
	public static final Type.Structured RESULTT = 
      new Type.Structured(new Type[] { Type.CONTINUOUS, Type.CONTINUOUS });
    public static final Type.Function TT = 
      new Type.Function(new Type.Vector(Type.SCALAR),RESULTT);

    public Minmax() 
    { 
      super(TT);
    }

    public Value apply(Value v)
    {
      return new ResultPair((Value.Vector) v);
    }

    public class ResultPair extends Value.Structured
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -7409969336850848159L;
	private double max = - Double.MAX_VALUE;
      private double min = Double.MAX_VALUE;

      public ResultPair(Value.Vector v)
      {
        super(RESULTT);
        for (int i = 0; i < v.length(); i++)
        {
          double d = v.doubleAt(i);
          if (d > max) max = d;
          if (d < min) min = d;
        }
      }

      public int length() { return 2; }

      public Value cmpnt(int idx) { return new Value.Continuous(doubleCmpnt(idx)); }

      public double doubleCmpnt(int idx)
      {
        if (idx == 0) return min;
          else return max;
      }
    }

  }


  /** <code>List -> [t]</code>Converts a List to a Vector where 
      <code>List a = TRIV | (a,List a)</code> lazily.
  */
  public static MakeVector makeVector = new MakeVector();

  /** <code>List -> [t]</code><P>Converts a List to a Vector where <code>List a = TRIV | (a,List
      a)</code> lazily.
      @see VectorFN.ListVector
  */
  public static class MakeVector extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3483330136729180837L;
	Value.Structured elt[];

    public MakeVector()
    {
      super(new Type.Function(Type.TYPE,Type.TYPE,false,false));   // fix this.
    }

    public Value apply(Value v)
    {
      if (v == Value.TRIV) return new EmptyVector();
        else return new ListVector(v); 
    } 
  }

  /** <code>Discrete -> [Discrete]</code>iota(n) returns the Vector [0, 1, 2, 3, ..., n-1]. */
  public static Iota iota = new Iota();

  /** <code>Discrete -> [Discrete]</code><P>iota(n) returns the Vector [0, 1, 2, 3, ..., n-1]. 
      @see VectorFN.IotaN
  */
  public static class Iota extends Value.Function
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4576732389951906544L;

	public Iota()
    { 
      super(new Type.Function(Type.DISCRETE,
                new Type.Vector(Type.DISCRETE, Type.DISCRETE, true, true, false, false ), 
                    false, false ) );
    }
 
    public Value apply(Value n)
    { 
      return new IotaN( ((Value.Discrete)n).i ); 
    }
  }

  /** <code>Discrete -> [Continuous]</code>iotar(n) returns the Vector [0.0, 1.0, 2.0, 3.0, ...,
      n-1.0].
  */
  public static Iotar iotar = new Iotar();

  /** <code>Discrete -> [Continuous]</code><P>iotar(n) returns the Vector [0.0, 1.0, 2.0, 3.0, ...,
      n-1.0].
      @see VectorFN.IotarN
  */
  public static class Iotar extends Value.Function
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5134658365455088105L;

	public Iotar()
    { 
      super(new Type.Function(Type.DISCRETE,
                new Type.Vector(Type.DISCRETE, Type.CONTINUOUS, true, true, false, false ), 
                    false, false ) );
    }
 
    public Value apply(Value n)
    { 
      return new IotarN( ((Value.Discrete)n).i ); 
    }
  }

  /** <code>([a],[b],...,[c]) -> [(a,b,...,c)]</code>Function zip turns a Stuctured Value of Vectors
      into a Vector of Structured Values.
  */
  public static Zip zip = new Zip();

  /** <code>([a],[b],...,[c]) -> [(a,b,...,c)]</code><P>Function zip turns a Stuctured Value of
      Vectors into a Vector of Structured Values.
      @see VectorFN.MultiCol
  */
  public static class Zip extends Value.Function
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2138604626190486737L;

	public Zip()
    { 
      super(null);
      Type eltType = new Type.Variable();
      Type indexType = new Type.Variable();
      
      t = new Type.Function( new Type.Structured(new Type[] { eltType, eltType }, 
                                 new boolean[]{ false, false} ),
                             new Type.Vector(indexType,eltType,false,false,false,false), 
                             false, false ); 

    }

    public Value apply(Value v) 
    { 
      return new MultiCol(v); 
    }

    /** Utility to compute return type - MultiCol. */
    public static Type.Vector applyType(Type.Structured at)
    {
      Type[] cmpnts = new Type[at.cmpnts.length];
      String[] names = null;
      if (at.labels != null) names = new String[at.cmpnts.length];
      for (int i = 0; i < at.cmpnts.length; i++)
      {
        cmpnts[i] = ((Type.Vector)at.cmpnts[i]).elt;
        if (at.labels != null)
          names[i] = at.labels[i];
      }

      return new Type.Vector(Type.DISCRETE,
                             new Type.Structured(cmpnts,names,
                                   Value.DefStructured.booleanArray(false,cmpnts.length)),
                             false,false,false,false);
    }
  }

  /** <code>(a -> b) -> [a] -> [b]</code>Function map applies a Function f to every element of a
      Vector, returning a Vector of results (as in many functional programming languages).  Note
      that map takes one Function parameter f and returns a Function ({@link VectorFN.MapF}) which 
      can in turn be applied to a Vector v.
      @see Value.Vector
      @see VectorFN.MapF
      @see VectorFN.MapFV
  */
  public static Map map = new Map();

  /** <code>(a -> b) -> [a] -> [b]</code><P>Function map applies a Function f to every element of a
      Vector, returning a Vector of results (as in many functional programming languages).  Note
      that map takes one Function parameter f and returns a Function ({@link VectorFN.MapF}) which
      can in turn be applied to a Vector v.
      @see Value.Vector
      @see VectorFN.MapF
      @see VectorFN.MapFV
  */
  public static class Map extends Value.Function
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -652867981694161539L;

	public Map()
    { 
      super(null);

      Type tp = new Type.Variable();
      Type tr = new Type.Variable();
      Type ti = new Type.Variable();

      t = new Type.Function(
                  new Type.Function(tp,tr,false,false),    //  f :: tp -> tr 
                  new Type.Function(                       //  [tp] -> [tr]
                    new Type.Vector(ti,tp,false,false,false,false),
                    new Type.Vector(ti,tr,false,false,false,false),false,false),
                  false, false
                );
    }

    public Value apply(Value f) 
    { 
      return new MapF((Value.Function)f); 
    }
  }

  /** <code>[a] -> [b]</code><P>
      @see VectorFN.Map
      @see VectorFN.MapFV
  */
  public static class MapF extends Value.Function                      // (Value) map f
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5269306756268938510L;
	private Value.Function f;

    public MapF(Value.Function f)
    { 
      super(null);

      Type ti = new Type.Variable();

      t = new Type.Function(                       //  [tp] -> [tr]
              new Type.Vector(ti,((Type.Function)f.t).param,false,false,false,false),
              new Type.Vector(ti,((Type.Function)f.t).result,false,false,false,false),false,false);
             
      this.f = f;
    }

    public Value apply(Value v)// v must be a Vector.Value // apply(v)
    { 
      return new MapFV((Value.Vector)v, f); 
    }
  }

  /** <code>[[a]] -> [a]</code>The Concat function concatenates a vector of vectors (all of the
      same element type), to produce a single vector of that element type. 
      @see VectorFN.ConcatVector
  */
  public static Concat concat = new Concat();

  /** <code>[[a]] -> [a]</code><P>The Concat function concatenates a vector of vectors (all of the
      same element type), to produce a single vector ({@link VectorFN.ConcatVector}) of that element
      type. 
      @see VectorFN.ConcatVector
  */
  public static class Concat extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 835714353387523304L;

	public Concat()
    {
      super(null);
      Type.Vector vt = new Type.Vector(new Type.Variable(), new Type.Variable(), false, false,
        false, false);
      t = new Type.Function(new Type.Vector(new Type.Variable(), vt, false, false, false, false),
        vt, false, false);
    }

    public Value apply(Value v)
    {
      return new ConcatVector((Type.Vector)((Type.Vector)v.t).elt, (Value.Vector)v);
    }
  }

//----------------------------------------------VECTOR IMPLEMENTATIONS------------------------------
//  Empty Vector
//  ConstantVector
//  FastDiscreteVector
//  FastContinuousVector
//  UniformVector
//  UniformDiscreteVector
//  UniformContinuousVector
//  FatVector
//  FatWeightVector
//  DiscreteVector
//  ContinuousVector
//  SubsetVector
//  WeightedVector
//  ListVector
//  IotaN
//  IotarN
//  MultiCol
//  MapFV
//  AppendVector
//  VSubVector
//  VSumVector

  /** A static instance of the EmptyVector class, with variable index and element types. 
      @see VectorFN.EmptyVector
  */
  public static EmptyVector emptyVector = new EmptyVector();

  /** A class for empty vectors (length = 0).  Although such vectors contain no elements, they
      can still have an index type and element type.
  */
  public static class EmptyVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6494196672720263154L;

	/** This constructor creates an empty vector with variable index and element type. */
    public EmptyVector() 
    { 
      super(new Type.Vector(new Type.Variable(), new Type.Variable(), false, false, false, false));
    }

    /** Use this constructor to specify the type information of the empty vector. */
    public EmptyVector(Type.Vector t)
    { 
      super(t);
    }

    /** The length of an empty vector is 0. */
    public int length()
    { 
      return 0;
    }

    /** Rather than throw an exception, empty vectors return Value.TRIV for any element (even though
        no elements really exist).
    */
    public Value elt(int i) 
    { 
      return Value.TRIV;
    }

    /** If the element type of this empty vector is a structured, this method returns a new
        empty vector, with the same index type as this empty vector, and with element type
        equal to this.t.elt.cmpnts[col].  i.e. it creates a new empty vector with an element
        type matching the type of the desired (empty) column of data.<P>
        If the element type of this vector is not structured, a class cast exception will occur.
    */
    public Value.Vector cmpnt(int col) 
    {
      Type.Vector tv = (Type.Vector)t;
      return new EmptyVector(new Type.Vector(tv.index,((Type.Structured)tv.elt).cmpnts[col],
        tv.ckIsSequence,tv.isSequence,false,false));
    }
  }

  /** This class is a cheap implementation for a vector where every element is the same value v.  It
      can be created simply by supplying this value v, and the desired length of the vector. 
      elt(i) returns v for any i.
  */
  public static class ConstantVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7702445453944578113L;

	/** The length of the vector. */
    protected int length;

    /** The value in every element. */
    protected Value v;

    public ConstantVector(int length, Value v) 
    { 
      super(new Type.Vector(Type.SCALAR,v.t,false,false,false,false));
      this.length = length;
      this.v = v;
    }

    public int length() { return length; }

    public Value elt(int i) { return v; }
  }

  /** This is a fast cheap implementation of a vector of discrete values.  The element type is
      Type.DISCRETE, and the elements are stored as an array of Java int's which are converted
      to discrete values upon request in the elt(i) method.  Use the intAt(i) method to access
      element i as a Java int.
  */
  public static class FastDiscreteVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7781028119970408045L;

	public static final Type.Vector thisType = 
      new Type.Vector(Type.SCALAR,Type.DISCRETE,false,false,false,false);

    final protected int data[];

    /** Shortcut method to extract data. <br>
     *  If (clone==true) a copy of the data is made, if not the original
     *  is returned.  Non-cloned data should not be modified as all CDMS
     *  values should be immutable (and as such messes with the assumptions
     *  made by many functions). <br> */
    public int[] getData( boolean clone ) {
    	if (clone) { return (int[])data.clone(); }
    	else { return data; }
    }
    
    /** Create a FastDiscreteVector by supplying the discrete values as an array of Java int's. */
    public FastDiscreteVector(int data[], Type.Discrete eltType) 
    { 
      super(new Type.Vector(eltType));
      this.data = data;
    }

    /** Create a FastDiscreteVector by supplying the discrete values as an array of Java int's. */
    public FastDiscreteVector(int data[]) 
    { 
    	super(thisType);
    	this.data = data;
    }

    /** Create a FastDiscreteVector copying all intAt() values from vec. */
    public FastDiscreteVector(Value.Vector vec) 
    { 
      super((Type.Vector)vec.t);
      this.data = new int[vec.length()];
      for ( int i = 0; i < data.length; i++) { data[i] = vec.intAt(i);}
    }

    
    public int length()
    {
      return data.length;
    }

    public Value elt(int i)
    {
      return new Value.Discrete((Type.Discrete)((Type.Vector)t).elt,data[i]);
    }

    public int intAt(int i)
    {
      return data[i];
    }
  }


  public static final Type.Vector FASTCONTINUOUSVECTOR = new Type.Vector(Type.CONTINUOUS);

  /** This is a fast cheap implementation of a vector of continuous values.  The element type is
      Type.CONTINUOUS, and the elements are stored as an array of Java doubles which are converted
      to continuous values upon request in the elt(i) method.  Use the doubleAt(i) method to access
      element i as a Java double.
  */
  public static class FastContinuousVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3637005225929460530L;
	protected double data[];

    /** Create a FastContinuousVector by supplying the continuous values as an array of Java
    doubles.
    */
    public FastContinuousVector(double data[],Type.Continuous eltType) 
    { 
    	super(new Type.Vector(eltType));
    	this.data = data;
    }

    /** Create a FastContinuousVector by supplying the continuous values as an array of Java
        doubles.
    */
    public FastContinuousVector(double data[]) 
    { 
      super(FASTCONTINUOUSVECTOR);
      this.data = data;
    }

    public int length()
    {
      return data.length;
    }

    public Value elt(int i)
    {
      return new Value.Continuous(data[i]);
    }

    public double doubleAt(int i)
    {
      return data[i];
    }
  }


  /**  This class is a cheap way to implement the perculiar kind of vector where every element holds
       the same value.  e.g.  [x,x,x,x,x,x] where x is any Value.
  */  
  public static class UniformVector extends Value.Vector 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5438215668174076491L;

	/** The length of the vector. */
    private int lngth;

    /** The value of every element. */
    private Value value;

    /** Use this constructor if the element type of the vector is not value.t. */
    public UniformVector(Type.Vector t, int length, Value value)
    {
      super(t);
      this.value = value;
      this.lngth = length;
    }

    /** Use this constructor to create a UniformVector with the default type - Scalar index type and
        element type equal to value.t. 
    */
    public UniformVector(int length, Value value)
    {
      super(new Type.Vector(Type.DISCRETE, value.t, false, false, false, false));
      this.value = value;
      this.lngth = length;
    }

    public int length()                                   // length()
    {
      return lngth;
    }

    public Value elt(int i)                              // elt(i)
    {
      return value;
    }
  }

  /**  This class is a cheap way to implement the perculiar kind of vector where every element
       holds the same discrete value.  e.g.  the vector [8,8,8,8,8,8,8,8,8,8,8,8] or [103,103].
  */  
  public static class UniformDiscreteVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7562953212834998908L;

	/** The length of the vector. */
    private int lngth;

    /** The integer that every element is equal to. */
    private int value;

    public UniformDiscreteVector(Type.Vector t, int length, int value)
    {
      super(t);
      this.value = value;
      this.lngth = length;
    }

    public int length()                                   // length()
    {
      return lngth;
    }

    public Value elt(int i)                              // elt(i)
    {
      return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt), value);
    }

    public int intAt(int i) 
    {  
      return value; 
    }

    public double doubleAt(int i)                              // doubleAt(i)
    { 
      return (double)value; 
    }

    public double doubleValueAt(int i)                         // doubleValueAt(i)
    { 
      return (double)value;
    }
  }

  /** This class is a cheap way to implement the perculiar kind of vector where every element holds
      the same continuous value.  e.g.  the vector [8.3,8.3,8.3,8.3,8.3,8.3,8.3] or [103.0,103.0].
  */  
  public static class UniformContinuousVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6752788542845687519L;

	/** The length of the vector. */
    private int lngth;

    /** The continuous value that each element is equal to. */
    private double value;

    public UniformContinuousVector(Type.Vector t, int length, double value)
    {
      super(t);
      this.value = value;
      this.lngth = length;
    }

    public int length()
    {
      return lngth;
    }

    public Value elt(int i)
    {
      return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt), value);
    }

    public int intAt(int i)
    {
      return (int)value;
    }    

    public double doubleAt(int i)
    {
      return value;
    }

    public double doubleValueAt(int i)
    {
      return (double)value;
    }
  }

  /** This class is the default (and big and ugly) implementation of Value.Vector.  It can handle
      any kind of values, but stores them as an array of heavy-weight values.  The weight of any
      element is 1.  If no type is supplied in the constructor, the Type of the first value in the
      value array is used (perhaps wrongly) to determine the elt of the vector.  For an
      implementation where each weight is not 1, see {@link VectorFN.FatWeightVector}.
  */
  public static class FatVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3467233685394100946L;
	Value[] vals;

    public FatVector(Value[] vals)
    {
      super(new Type.Vector(Type.DISCRETE, vals[0].t, false, false, false, false));
      this.vals = vals;
    }

    public FatVector(Value[] vals, Type.Vector t)
    {
      super(t);
      this.vals = vals;
    }

    public int length()                                   // length()
    {
      return this.vals.length;
    }

    public Value elt(int i)                               // elt(i)
    {
      return vals[i];
    }
  }

  /** This class is an extension of FatVector ({@link VectorFN.FatVector}), which allows each
      element of the vector to have a different "weight".  This is useful for partial assignment
      situations. 
  */
  public static class FatWeightVector extends Value.Vector 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7808844301109061153L;
	private Value[] vals;
    private double[] weights;

    /** The array weights should be the same length as the array vals. */
    public FatWeightVector(Value[] vals, double[] weights)
    {
      super(new Type.Vector(Type.DISCRETE, vals[0].t, false, false, false, false));
      this.vals = vals;
      this.weights = new double[weights.length];
      int count;
      for(count = 0; count < weights.length; count++)
      {
        this.weights[count] = weights[count];
      }
    }

    public int length()                                   // length()
    {
      return this.vals.length;
    }

    public double weight(int i)
    {
      return this.weights[i];
    }

    public Value elt(int i)                               // elt(i)
    {
      return vals[i];
    }
  }

  /** This class is for vectors of discrete values, where the element type is not necessarily
      Type.DISCRETE, or the status of every element is not necessarily Value.S_PROPER.  For
      vectors of discrete values where the element type IS Type.DISCRETE, and the status of each
      element IS Value.S_PROPER, the FastDiscreteVector class should be used (see {@link
      VectorFN.FastDiscreteVector}).  The discrete values are stored as an array of Java int's, and
      the status of each element is stored as an array of Java bytes.  
      @see VectorFN.FastDiscreteVector
  */
  public static class DiscreteVector extends Value.Vector 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8911450443240960416L;
	private int[] elements;
    private byte[] statii;
    private boolean elementStatusDefined = false;
//    private static final byte NA = 0;
//    private static final byte PROPER = 1;
//    private static final byte INVALID = 2;
//    private static final byte UNOBSERVED = 3;
//    private static final byte IRRELEVANT = 4;
    private ValueStatus status;

    /** This constructor creates a vector of Type t.  Each element of the parameter v must
        also be of Type t.  The value status of the vector is given by the parameter s.
    */
    public DiscreteVector(Type.Vector t, ValueStatus s, Value.Discrete[] v)
    {
      super(t);
      this.status = s;
      this.elementStatusDefined = true;
      this.elements = new int[v.length];
      this.statii = new byte[v.length];
      int count;
      for(count = 0; count < v.length; count++)
      {
        this.elements[count] = v[count].i;
        if(v[count].status() == Value.S_NA)
        {
          this.statii[count] = (byte)0;
        }
        else
        {
          if(v[count].status() == Value.S_PROPER)
          {
            this.statii[count] = (byte)1;
          }
          else
          {
            if(v[count].status() == Value.S_INVALID)
            {
              this.statii[count] = (byte)2;
            }
            else
            {
              if(v[count].status() == Value.S_UNOBSERVED)
              {
                this.statii[count] = (byte)3;
              }
              else
              {
                if(v[count].status() == Value.S_IRRELEVANT)
                {
                  this.statii[count] = (byte)4;
                }
                else
                {
                  this.statii[count] = (byte)0;
                }
              }
            }
          }
        }
      }
    }

    /** This constructor creates a vector of Type t.  Each element of the parameter v must
        also be of Type t.  The value status of the vector is S_PROPER.
    */
    public DiscreteVector(Type.Vector t, Value.Discrete[] v)
    {
      this(t, Value.S_PROPER, v);
    }

    /** This constructor creates a vector of Type t.  Although the values for the elements are
        provided as integers, the method "Value elt(int i)" interprets these as values of Type 
        (Type.Discrete)t.elt - not necessarily as values of Type.DISCRETE.  The value status of
        the vector is given by the parameter 's'. 
    */
    public DiscreteVector(Type.Vector t, ValueStatus s, int[] i)
    {
      super(t);
      this.status = s;
      this.elements = new int[i.length];
      this.statii = new byte[0];
      int count;
      for(count = 0; count < i.length; count++)
      {
        this.elements[count] = i[count];
      }
    }

    /** This constructor creates a vector of Type t.  Although the values for the elements are
        provided as integers, the method "Value elt(int i)" interprets these as values of Type 
        (Type.Discrete)t.elt - not necessarily as values of Type.DISCRETE. The value status of
        the vector is S_PROPER. 
    */
    public DiscreteVector(Type.Vector t, int[] i)
    {
      this(t, Value.S_PROPER, i);
    }

    /** This constructor creates an empty vector of Type 't' with value status s. */
    public DiscreteVector(Type.Vector t, ValueStatus s)
    {
      super(t);
      this.status = s;
      this.elements = new int[0];
      this.statii = new byte[0];
    }

    /** This constructor creates an empty vector of Type 't' with value status S_PROPER. */
    public DiscreteVector(Type.Vector t)
    {
      this(t, Value.S_PROPER);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type v[0].t.  The
    value status of the vector is given by the parameter 's'. */
    public DiscreteVector(Value.Discrete[] v, ValueStatus s)
    {
      this(new Type.Vector(Type.DISCRETE, v[0].t, false, false, false, false), s, v);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type v[0].t.  The
    value status of the vector is S_PROPER. */
    public DiscreteVector(Value.Discrete[] v)
    {
      this(new Type.Vector(Type.DISCRETE, v[0].t, false, false, false, false), Value.S_PROPER, v);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type Type.DISCRETE.
        The value status of the vector is given by the parameter 's'.
    */
    public DiscreteVector(int[] i, ValueStatus s)
    {
      this(new Type.Vector(Type.DISCRETE, Type.DISCRETE, false, false, false, false), s, i);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type Type.DISCRETE.
        The value status of the vector is S_PROPER. 
    */
    public DiscreteVector(int[] i)
    {
      this(new Type.Vector(Type.DISCRETE, Type.DISCRETE, false, false, false, false),
               Value.S_PROPER, i);
    }

    public int length()                                   // length()
    {
      return elements.length;
    }

    public Value elt(int i)                               // elt(i)
    {
      if(this.elementStatusDefined)
      {
        if(this.statii[i] == 0)
        {
          return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt),
                     Value.S_NA,elements[i]);
        }
        else
        {
          if(this.statii[i] == 1)
          {
            return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt),
                       Value.S_PROPER,elements[i]);
          }
          else
          {
            if(this.statii[i] == 2)
            {
              return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt), 
                         Value.S_INVALID ,elements[i]);
            }
            else
            {
              if(this.statii[i] == 3)
              {
                return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt),
                           Value.S_UNOBSERVED ,elements[i]);
              }
              else
              {
                if(this.statii[i] == 4)
                {
                  return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt),
                             Value.S_IRRELEVANT ,elements[i]);
                }
                else
                {
                  return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt),
                             Value.S_PROPER ,elements[i]);
                }
              }
            }
          }
        }
      }
      else
      {          // assume valueStatus is proper.
        return new Value.Discrete(((Type.Discrete)((Type.Vector)this.t).elt),elements[i]); 
      }
    }

    public ValueStatus status()
    {
      return this.status;
    }

    public int intAt(int i) 
    {  
      return elements[i]; 
    }

    public String toString()
    {
      int count;
      String res = "";
      for(count = 0; count < elements.length; count++)
      {
        res  = res + elements[count] + "\n";
      }
      return res;
    }
  }

  /** This class is for vectors of continuous values, where the element type is not necessarily
      Type.CONTINUOUS, or the status of every element is not necessarily Value.S_PROPER.  For
      vectors of continuous values where the element type IS Type.CONTINUOUS, and the status of each
      element IS Value.S_PROPER, the FastContinuousVector class should be used (see {@link
      VectorFN.FastContinuousVector}).  The discrete values are stored as an array of Java int's,
      and the status of each element is stored as an array of Java bytes.  
      @see VectorFN.FastContinuousVector
  */
  public static class ContinuousVector extends Value.Vector  
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7854038396742917334L;
	private double[] elements; 
    private byte[] statii;
    private boolean elementStatusDefined = false;
    private static final byte NA = 0;
    private static final byte PROPER = 1;
    private static final byte INVALID = 2;
    private static final byte UNOBSERVED = 3;
    private static final byte IRRELEVANT = 4;
    private ValueStatus status;

    /** This constructor creates a vector of Type t.  Each element of the parameter v must
        also be of Type t.  The value status of the vector is given by the parameter s.
    */
    public ContinuousVector(Type.Vector t, ValueStatus s, Value.Continuous[] v)
    {
      super(t);
      this.status = s;
      this.elementStatusDefined = true;
      this.elements = new double[v.length];
      this.statii = new byte[v.length];
      int count;
      for(count = 0; count < v.length; count++)
      {
        this.elements[count] = v[count].x;
        if(v[count].status() == Value.S_NA)
        {
          this.statii[count] = NA;
        }
        else
        {
          if(v[count].status() == Value.S_PROPER)
          {
            this.statii[count] = PROPER;
          }
          else
          {
            if(v[count].status() == Value.S_INVALID)
            {
              this.statii[count] = INVALID;
            }
            else
            {
              if(v[count].status() == Value.S_UNOBSERVED)
              {
                this.statii[count] = UNOBSERVED;
              }
              else
              {
                if(v[count].status() == Value.S_IRRELEVANT)
                {
                  this.statii[count] = IRRELEVANT;
                }
                else
                {
                  this.statii[count] = NA;
                }
              }
            }
          }
        }
      }
    }

    /** This constructor creates a vector of Type t.  Each element of the parameter v must
        also be of Type t.  The value status of the vector is S_PROPER. 
    */
    public ContinuousVector(Type.Vector t, Value.Continuous[] v)
    {
      this(t, Value.S_PROPER, v);
    }

    /** This constructor creates a vector of Type t.  Although the values for the elements are
        provided as doubles, the method "Value elt(int i)" interprets these as values of Type 
        (Type.Continuous)t.elt - not necessarily as values of Type.CONTINUOUS.  The value status of
        the vector is given by the parameter 's'. 
    */
    public ContinuousVector(Type.Vector t, ValueStatus s, double[] d)
    {
      super(t);
      this.status = s;
      this.elements = new double[d.length];
      this.statii = new byte[0];
      int count;
      for(count = 0; count < d.length; count++)
      {
        this.elements[count] = d[count];
      }
    }

    /** This constructor creates a vector of Type t.  Although the values for the elements are
        provided as doubles, the method "Value elt(int i)" interprets these as values of Type 
        (Type.Continuous)t.elt - not necessarily as values of Type.CONTINUOUS. The value status of
        the vector is S_PROPER. 
    */
    public ContinuousVector(Type.Vector t, double[] d)
    {
      this(t, Value.S_PROPER, d);
    }

    /** This constructor creates an empty vector of Type 't' with value status s. */
    public ContinuousVector(Type.Vector t, ValueStatus s)
    {
      super(t);
      this.status = s;
      this.elements = new double[0];
      this.statii = new byte[0];
    }

    /** This constructor creates an empty vector of Type 't' with value status S_PROPER. */
    public ContinuousVector(Type.Vector t)
    {
      this(t, Value.S_PROPER);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type v[0].t.  The
        value status of the vector is given by the parameter 's'. 
    */
    public ContinuousVector(Value.Continuous[] v, ValueStatus s)
    {
      this(new Type.Vector(Type.DISCRETE, v[0].t, false, false, false, false), s, v);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type v[0].t.  The
        value status of the vector is S_PROPER. 
    */
    public ContinuousVector(Value.Continuous[] v)
    {
      this(new Type.Vector(Type.DISCRETE, v[0].t, false, false, false, false), Value.S_PROPER, v);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type
        Type.CONTINUOUS.  The value status of the vector is given by the parameter 's'. 
    */
    public ContinuousVector(double[] d, ValueStatus s)
    {
      this(new Type.Vector(Type.DISCRETE, Type.CONTINUOUS, false, false, false, false), s, d);
    }

    /** This constructor creates a vector indexed by Type.DISCRETE, with element type
        Type.CONTINUOUS.  The value status of the vector is S_PROPER.
    */
    public ContinuousVector(double[] d)
    {
      this(new Type.Vector(Type.DISCRETE, Type.CONTINUOUS, false, false, false, false),
               Value.S_PROPER, d);
    }

    public int length()  
    {
      return elements.length;
    }

    public Value elt(int i) 
    {
      if(this.elementStatusDefined)
      {
        if(this.statii[i] == NA)
        {
          return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt), 
                     Value.S_NA ,elements[i]);
        }
        else
        {
          if(this.statii[i] == PROPER)
          {
            return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt), 
                       Value.S_PROPER ,elements[i]);
          }
          else
          {
            if(this.statii[i] == INVALID)
            {
              return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt),
                         Value.S_INVALID ,elements[i]);
            }
            else
            {
              if(this.statii[i] == UNOBSERVED)
              {
                return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt),
                           Value.S_UNOBSERVED ,elements[i]);
              }
              else
              {
                if(this.statii[i] == IRRELEVANT)
                {
                  return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt),
                             Value.S_IRRELEVANT ,elements[i]);
                }
                else
                {
                  return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt),
                             Value.S_PROPER ,elements[i]);
                }
              }
            }
          }
        }
      }
      else
      {                   // assume valueStatus is proper.
        return new Value.Continuous(((Type.Continuous)((Type.Vector)this.t).elt),elements[i]); 
      }
    }

    public ValueStatus status()
    {
      return this.status;
    }

    public double doubleAt(int i) 
    {  
      return elements[i]; 
    }

    public String toString()
    {
      int count;
      String res = "";
      for(count = 0; count < elements.length; count++)
      {
        res  = res + elements[count] + "\n";
      }
      return res;
    }
  }

  /** This vector is constructed from (sparse) elements of an existing vector.  indexElements
      is a list of indices of elements of the existing vector to be included in the new vector.
      This means the only space required for this vector is an array of n integers (where n is
      the length of the vector), and a pointer to the original vector. 
  */
  public static class SubsetVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2575432059533795339L;
	private int[] elts;
    private Value.Vector v;

    public SubsetVector(Value.Vector v, int[] indexElements, int length)
    {
      super((Type.Vector)v.t);
      int count;
      elts = new int[length];
      for(count = 0; count < length; count++)
      {
        elts[count] = indexElements[count];
      }
      this.v = v;
    } 

    public int length()
    {
      return elts.length;
    }

    public Value elt(int i)
    {
      return v.elt(elts[i]);
    }

      public int intAt( int i ) {
	  return v.intAt(elts[i]);
      }

      public double doubleAt( int i ) {
	  return v.doubleAt(elts[i]);
      }


      public Vector cmpnt(int col)
      { 
	  return new SubsetVector( v.cmpnt(col), elts, elts.length); //Cmpnt(this,col); 
      } 


    public double weight(int i)
    {
      return v.weight(elts[i]);
    }
  }


  /** This class allows non-1 weights to be used for elements of any vector.  It is constructed 
      using an existing vector v, and an array of Java doubles "weights", which should be the same
      length as v.  This means that any implementation of Value.Vector can be used for v, and
      weights can be used with it, and the only space required is a pointer to v, and the weights
      array.  The length, intAt, doubleAt, and elt methods all directly call the corresponding
      methods of v.
  */
  public static class WeightedVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 682543869475773981L;
	protected Value.Vector v;    // Make protected so this can be accessed by the extending 
                                 // MultiplicativeWeightedVector class.
    protected double[] weights;  // Likewise with the weights array.

    public WeightedVector(Value.Vector v, double[] weights)
    {
      super((Type.Vector)v.t);
      this.v = v;
      this.weights = weights;
    }

    public int length()
    {
      return v.length();
    }

    public Value elt(int i)
    {
      return v.elt(i);
    }

    public double weight(int i)
    {
      return weights[i];
    }

    public double doubleAt(int i) 
    {  
      return v.doubleAt(i); 
    }

    public int intAt(int i) 
    {  
      return v.intAt(i); 
    }
  }

  /** This class is the same as the WeightedVector class, except the weights supplied in the
      constructor are multiplied with the corresponding weights of the vector supplied in the
      constructor.  e.g.  A MultiplicativeWeightedVector constructed with a vector having 
      weights [1, 1, 0.5, 0.9] and a weights array of [0.5, 1, 0.5, 0.3] would have overall 
      weights of [0.5, 1, 0.25, 0.27].  By contrast, a WeightedVector constructed with the same
      parameters would have overall weights [0.5, 1, 0.5, 0.3] because in that class the weights
      array OVER-RIDES the weights of the vector.
  */
  public static class MultiplicativeWeightedVector extends WeightedVector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2888717476612298560L;

	public MultiplicativeWeightedVector(Value.Vector v, double[] weights)
    {
      super(v, weights);
    }

    public double weight(int i)
    {
      return weights[i] * v.weight(i);
    }
  }

  /** This class behaves like the MultiplicativeWeightedVector class, except any elements with
      weight 0 are eliminated.  This takes a lot longer to construct than a MultiplicativeWeightedVector,
      but is benificial when many weights are 0, as it results in a shorter vector.  This saves other
      functions the task of filtering-out any 0-weight elements.
  */
  public static class NonZeroMultiplicativeWeightedVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1407771184994063454L;
	private double[] weights;
    private Value.Vector v;
    private int[] mapping;

    public NonZeroMultiplicativeWeightedVector(Value.Vector v, double[] weights)
    {
      super((Type.Vector)v.t);
      this.v = v;
      this.weights = weights;
      int count, count2;
      int[] tmpmapping = new int[weights.length];
      for(count = 0, count2 = 0; count < v.length(); count++)
      {
        if(weights[count] * v.weight(count) > 0)
        {
          tmpmapping[count2] = count;
          count2++;
        }
      }
      mapping = new int[count2];
      for(count = 0; count < mapping.length; count++)
      {
        mapping[count] = tmpmapping[count];
      }
    }

    public Value elt(int i)
    {
      return v.elt(mapping[i]);
    }

    public double weight(int i)
    {
      return weights[mapping[i]] * v.weight(mapping[i]);
    }

    public int length()
    {
      return mapping.length;
    }
  }

  /** This class is used by the MakeVector function. 
      @see VectorFN.MakeVector 
  */
  public static class ListVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -734661874515851848L;
	java.util.Vector<Value> vec = new java.util.Vector<Value>();

    public ListVector(Value list)
    {
      super(new Type.Vector(Type.DISCRETE,
                ((Type.Structured)list.t).cmpnts[0],true,true,false,false));

      while (list != Value.TRIV)
      {
        vec.add( ((Value.Structured) list).cmpnt(0) );
        list = ((Value.Structured) list).cmpnt(1);
      } 
    }

    public int length()
    {
      return vec.size();
    }

    public Value elt(int i)
    {
      return vec.elementAt(i);
    }
  }

  /** This class is for vectors with discrete elements, where elt(i) = i.  e.g. the vector [0,1,2]
      or [0,1,2,3,4,5,6,7,8].  It is used by the Iota function.
      @see VectorFN.Iota
  */
  public static class IotaN extends Value.Vector
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8931140622244130049L;
	private int n;

    public IotaN(int n)
    { 
      super((Type.Vector) ((Type.Function)(iota.t)).result);
      this.n = n;
    }

    public int length() 
    { 
      return n; 
    }

    public Value elt(int i)
    { 
      return new Value.Discrete(i); 
    }

    public double doubleAt(int i)
    {
      return i;
    }

    public int intAt(int i) 
    { 
      return i; 
    }
  }

  /** This class is for vectors with continuous elements, where elt(i) = i.  e.g. the vector 
      [0.0,1.0,2.0] or [0.0,1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0].  It is used by the Iotar function.
      @see VectorFN.Iotar
  */
  public static class IotarN extends Value.Vector
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8667241617855864374L;
	private int n;

    public IotarN(int n)
    { 
      super((Type.Vector) ((Type.Function)(iotar.t)).result);
      this.n = n;
    }

    public int length() 
    { 
      return n; 
    }

    public Value elt(int i)
    { 
      return new Value.Continuous(Type.CONTINUOUS,i); 
    }

    public double doubleAt(int i) 
    { 
      return i; 
    }
  }

//   /** This class is used by the Zip function, to create a vector of structured values from
//       a structured value of vectors.  e.g. ([a],[b],[c]) -> [(a,b,c)]
//   */
//   public static class MultiCol extends Value.Vector
//   { 
//     private Value.Structured val; // i.e. the columns

//     public MultiCol(Value v)
//     { 
//       super(Zip.applyType((Type.Structured) v.t));
//       val = ((Value.Structured) v);
//     }

//     public int length()
//     { 
//       int len = ((Value.Vector) val.cmpnt(0)).length(); // cautious code!
//       for (int col = 0; col < val.length(); col++) // each cmpnt
//       { 
//         int li = ((Value.Vector) val.cmpnt(col)).length();
//         if (li < len) len = li; // one possible semantics
//       }
//       return len;
//     }


//       /** Shortcut class to make elt more efficient. */
//       protected static class MultiColElt extends Value.Structured
//       {
// 	  /** Index into vector */
// 	  int index;
// 	  Value.Structured vec;

// 	  public MultiColElt( Value.Structured vec, int index ) {
// 	      // super((Type.Structured) ((Type.Vector)(vec.t)).elt );
// 	      super ( Type.STRUCTURED );
// 	      this.index = index;
// 	      this.vec = vec;
// 	  }

// 	  public Value cmpnt(int i)
// 	  {
// 	      Value.Vector col = (Value.Vector)vec.cmpnt(i);
// 	      return col.elt(index);
// 	  }
	  
// 	  public int length()
// 	  {
// 	      return vec.length();
// 	  }

// 	  public double doubleCmpnt(int i)
// 	  {
// 	      Value.Vector col = (Value.Vector)vec.cmpnt(i);
// 	      return col.doubleAt(index);
// 	  }

// 	  public int intCmpnt(int i)
// 	  {
// 	      Value.Vector col = (Value.Vector)vec.cmpnt(i);
// 	      return col.intAt(index);
// 	  }
//       }


//     public Value elt(int i)
//     { 
// 	return new MultiColElt(val,i);

// 	/*
// 	  Value[] parts = new Value[val.length()];
// 	  for (int col = 0; col < parts.length; col++) // each cmpnt
// 	  parts[col] = ((Value.Vector) val.cmpnt(col)).elt(i);
// 	  Type.Vector tv = (Type.Vector)t; // cast it
// 	  return new Value.DefStructured((Type.Structured) tv.elt, parts);
// 	*/
//     }

//     public double weight(int i)
//     {
//       double product = 1;
//       for (int col = 0; col < val.length(); col++) // each cmpnt
//         product *= ((Value.Vector) val.cmpnt(col)).weight(i);
//       return product;
//     }

//     public Vector cmpnt(int col)
//     { 
//       return (Value.Vector) val.cmpnt(col); 
//     }
//   }



  /** This class is used by the Zip function, to create a vector of structured values from
      a structured value of vectors.  e.g. ([a],[b],[c]) -> [(a,b,c)]
  */
  public static class MultiCol extends Value.Vector
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4047385002907612305L;
	private Value.Structured val; // i.e. the columns

    public MultiCol(Value v)
    { 
      super(Zip.applyType((Type.Structured) v.t));
      val = ((Value.Structured) v);
    }

    /** Lazily calculated length */
    private int length = -1;
    public int length()
    { 
    	if (length != -1) {return length;}
	if ( val.length() == 0 ) {
	    throw new 
		RuntimeException("Cannot find vector length when vector width = 0 in MultiCol");
	}

      int len = ((Value.Vector) val.cmpnt(0)).length(); // cautious code!
      for (int col = 0; col < val.length(); col++) // each cmpnt
      { 
        int li = ((Value.Vector) val.cmpnt(col)).length();
        if (li < len) len = li; // one possible semantics
      }
      this.length = len;
      return len;
    }

	  
	  /** Shortcut class to make elt more efficient. */
	  protected class MultiColElt extends Value.Structured
	  {
		  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 7710670058502319661L;
		/** Index into vector */
		  int index;
		  
		  public MultiColElt( Type.Structured t, int index ) {
			  super( t );
			  //super( (Type.Structured) ((Type.Vector)(vec.t)).elt );
			  //super ( Type.STRUCTURED );
			  this.index = index;
		  }
		  
		  public Value cmpnt(int i)
		  {
			  Value.Vector col = (Value.Vector)val.cmpnt(i);
			  return col.elt(index);
		  }
		  
		  public int length() { return val.length(); }
		  
		  public double doubleCmpnt(int i)
		  {
			  Value.Vector col = (Value.Vector)val.cmpnt(i);
			  return col.doubleAt(index);
		  }
		  
		  public int intCmpnt(int i)
		  {
			  Value.Vector col = (Value.Vector)val.cmpnt(i);
			  return col.intAt(index);
		  }
	  }
	  
	  

    public Value elt(int i)
    { 
		/*
		  Value[] parts = new Value[val.length()];
		  for (int col = 0; col < parts.length; col++) // each cmpnt
		  parts[col] = ((Value.Vector) val.cmpnt(col)).elt(i);
		  Type.Vector tv = (Type.Vector)t; // cast it
		  return new Value.DefStructured((Type.Structured) tv.elt, parts);
		*/
		Type.Vector vt = (Type.Vector)t;
		return new MultiColElt( (Type.Structured)vt.elt, i );

    }

    public Vector cmpnt(int col)
    { 
      return (Value.Vector) val.cmpnt(col); 
    }
  }




//   /** This class is used by the Zip function, to create a vector of structured values from
//       a structured value of vectors.  e.g. ([a],[b],[c]) -> [(a,b,c)]
//       NOTE: Type information is not fully implemented in this version.
//   */
//   public static class MultiCol extends Value.Vector
//   { 
//     private Value.Structured val; // i.e. the columns

//     public MultiCol(Value v)
//     { 
//       super(Zip.applyType((Type.Structured) v.t));
//       val = ((Value.Structured) v);
//     }

//     public int length()
//     { 
//       int len = ((Value.Vector) val.cmpnt(0)).length(); // cautious code!
//       for (int col = 0; col < val.length(); col++) // each cmpnt
//       { 
//         int li = ((Value.Vector) val.cmpnt(col)).length();
//         if (li < len) len = li; // one possible semantics
//       }
//       return len;
//     }


//       /** Shortcut class to make elt more efficient. */
//       protected class MultiColElt extends Value.Structured
//       {
// 	  /** Index into vector */
// 	  int index;

// 	  public MultiColElt( int index ) {
// // 	      super((Type.Structured) ((Type.Vector)(vec.t)).elt );
// 	      super ( Type.STRUCTURED );
// 	      this.index = index;
// 	  }

// 	  public Value cmpnt(int i)
// 	  {
// 	      Value.Vector col = (Value.Vector)val.cmpnt(i);
// 	      return col.elt(index);
// 	  }
	  
// 	  public int length()
// 	  {
// 	      return val.length();
// 	  }

// 	  public double doubleCmpnt(int i)
// 	  {
// 	      Value.Vector col = (Value.Vector)val.cmpnt(i);
// 	      return col.doubleAt(index);
// 	  }

// 	  public int intCmpnt(int i)
// 	  {
// 	      Value.Vector col = (Value.Vector)val.cmpnt(i);
// 	      return col.intAt(index);
// 	  }
//       }




//     public Value elt(int i)
//     { 
// 	return new MultiColElt(i);

	
// // 	  Value[] parts = new Value[val.length()];
// // 	  for (int col = 0; col < parts.length; col++) // each cmpnt
// // 	  parts[col] = ((Value.Vector) val.cmpnt(col)).elt(i);
// // 	  Type.Vector tv = (Type.Vector)t; // cast it
// // 	  return new Value.DefStructured((Type.Structured) tv.elt, parts);
	
//     }

//     public double weight(int i)
//     {
//       double product = 1;
//       for (int col = 0; col < val.length(); col++) // each cmpnt
//         product *= ((Value.Vector) val.cmpnt(col)).weight(i);
//       return product;
//     }

//     public Vector cmpnt(int col)
//     { 
//       return (Value.Vector) val.cmpnt(col); 
//     }
//   }

  /** This is a lazy vector implementation used by the Map / MapF functions.  An element i is
      calculated (from a function f applied to element i of a vector v) only when first accessed,
      and then stored in an array of Values.
  */
  public static class MapFV extends Value.Vector 
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4725602190540204374L;
	private Value.Vector v;
    private Value[] res;
    private Value.Function f;

    public MapFV(Value.Vector v, Value.Function f)
    { 
      // Need unification - until then cheat by looking at first element.
      super(null);

      this.v = v;                                       // Value
      this.f = f;
      res = new Value[v.length()];

      if (length() >= 1)
      {
        this.t = new Type.Vector(elt(0).t);
      }
      else 
      {
        this.t = new Type.Vector( ((Type.Vector)v.t).index, 
                  ((Type.Function)f.t).result, false, false, false, false);
      }
    }

    public int length()
    { 
      return v.length();
    }  

    public Value elt(int i) 
    { 
      if (res[i] == null) res[i] = f.apply(v.elt(i));  // lazy.
      return res[i];
    } 
  }

  /** This vector class is used by the Concat function to create a single vector from a vector
      of vectors.
      @see VectorFN.Concat
  */
  public static class ConcatVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7851747594647790051L;
	private Value.Vector v;
    private int l = 0;

    public ConcatVector(Type.Vector t, Value.Vector v)
    {
      super(t);
      this.v = v;
      for(int count = 0; count < v.length(); count++)
      {
        l+=((Value.Vector)v.elt(count)).length();
      }
    }

    public Value elt(int i)
    {
      int count = 0;
      int sum = ((Value.Vector)v.elt(0)).length();
      int oldSum = 0;

      while (i >= sum)
      {
        count++;
        oldSum = sum;
        sum += ((Value.Vector)v.elt(count)).length();
      }
      return ((Value.Vector)v.elt(count)).elt(i - oldSum);
    }

    public int length()
    {
      return l;
    }

    public double weight(int i)
    {
      int count = 0;
      int sum = ((Value.Vector)v.elt(0)).length();
      int oldSum = 0;

      while (i >= sum)
      {
        count++;
        oldSum = sum;
        sum += ((Value.Vector)v.elt(count)).length();
      }
      return ((Value.Vector)v.elt(count)).weight(i - oldSum);
    }
  }

  /** A vector implementation used by the VSub function.
      @see VectorFN.VSub
  */
  public static class VSubVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1103256148431346243L;
	Value.Vector v1, v2;
 
    public VSubVector(Value.Vector v1, Value.Vector v2)
    {
      super(VSub.vectorType);
      this.v1 = v1;
      this.v2 = v2;
    }

    public int length()
    {
      return v1.length();
    }

    public double doubleAt(int i)
    {
      return v1.doubleAt(i) - v2.doubleAt(i);
    }

    public int intAt(int i)
    {
      return v1.intAt(i) - v2.intAt(i);
    }

    public Value elt(int i)
    {
      Value.Scalar v1elt = (Value.Scalar) v1.elt(i);
      Value.Scalar v2elt = (Value.Scalar) v2.elt(i);
      if (v1elt instanceof Value.Continuous || v2elt instanceof Value.Continuous)
        return new Value.Continuous(v1elt.getContinuous() - v2elt.getContinuous());
      else return new Value.Discrete(v1elt.getDiscrete() - v2elt.getDiscrete());
    }
  }

  /** A vector implementation used by the VSum function.
      @see VectorFN.VSum
  */
  public static class VSumVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -9100288984526662380L;
	Value.Vector v1, v2;
 
    public VSumVector(Value.Vector v1, Value.Vector v2)
    {
      super(VSum.vectorType);
      this.v1 = v1;
      this.v2 = v2;
    }

    public int length()
    {
      return v1.length();
    }

    public double doubleAt(int i)
    {
      return v1.doubleAt(i) + v2.doubleAt(i);
    }

    public int intAt(int i)
    {
      return v1.intAt(i) + v2.intAt(i);
    }

    public Value elt(int i)
    {
      Value.Scalar v1elt = (Value.Scalar) v1.elt(i);
      Value.Scalar v2elt = (Value.Scalar) v2.elt(i);
      if (v1elt instanceof Value.Continuous || v2elt instanceof Value.Continuous)
        return new Value.Continuous(v1elt.getContinuous() + v2elt.getContinuous());
      else return new Value.Discrete(v1elt.getDiscrete() + v2elt.getDiscrete());
    }
  }
}
// End of file.
