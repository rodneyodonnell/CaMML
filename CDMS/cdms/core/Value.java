//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Value.java
// Authors: {lloyd,leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Random;
import java.io.ObjectStreamException;

/** Values that can be manipulated by CDMS.  Classes Value and Type are crucial because they 
    define how data is represented and passed around CDMS.  Value and Type must be kept small, 
    clean and orthogonal in design; they can only be changed or extended after considerable thought.
    See also the parallel Type class.  Note that Functions and Models are first class Values - 
    just like Discrete, Structured, Str etc.
    @see Type
*/
public abstract class Value implements java.io.Serializable             
{ 
  /** t is the data type of the value.  It defines parameters such as upper/lower bounds, etc.
      @see Type
  */
  public Type t;

  /** The Value class (or child-class) has not implemented this function.  (i.e. a status is
      meaningless for this kind of value.)  This does not mean the value is 'bad' at all.
      @see ValueStatus
  */
  public static final ValueStatus S_NA = new ValueStatus("Not implemented or undefined.");

  /** A legitimate value.  (Relevant, observed, within-range)
      @see ValueStatus
  */
  public static final ValueStatus S_PROPER = new ValueStatus("Proper value.");  

  /** The Value is out of range (invalid).
      @see ValueStatus
  */
  public static final ValueStatus S_INVALID = new ValueStatus("Value out of range.");  

  /** Indicates a value that was never observed ('missing') as opposed to
      a value which is irrelevant or corrupt.
      @see ValueStatus
  */
  public static final ValueStatus S_UNOBSERVED = new ValueStatus("Unobserved value.");  

  /** The Value does not make sense or is irrelevant although it may exist
      and be in range.  (e.g. market value of person's car where person
      does not have a car.
      @see ValueStatus
  */
  public static final ValueStatus S_IRRELEVANT = 
    new ValueStatus("Irrelevant or nonsensical value.");  

  /** Value has been intervened upon.  In most contexts this will be the same as S_PROPER but in
   *   some cases (such as Bayesian Nets being interpreted Causally) there is a distinction.
   *  @see ValueStatus
   */
  public static final ValueStatus S_INTERVENTION = 
    new ValueStatus("A Causal intervention has occured.");  

  public Value()
  {
    t = Type.TRIV;
  }

  /** Constructor taking a Type parameter t which is the (data)Type of the value.
      @see Type
  */
  public Value(Type t)
  {
    this.t = t;
  }

  /** Default status checking function returning S_NA.  If status is meaningful for a child 
      class, the child class should over-ride this function.
      @see ValueStatus
  */
  public ValueStatus status()  // override if status is defined.
  { 
    return S_NA;
  }

  /** Returns the URL of a help page for the Value. */
  public java.net.URL getHelp()
  {
    try
    {
      String urlStr = "file:doc/api-docs/" + getClass().getName().replace('.','/');
      urlStr = urlStr.replace('$','.') + ".html";
      return new java.net.URL(urlStr);
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public Object clone()
  {
    return this;
  }


  /** A generic string class.  The INVALID flag cannot be used since there is no notion of range 
      in Type.Str;
      @see Type.Str
  */
  public static class Str extends Value                                  
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3683829350738293207L;

	/** this points to one of the ValueStati defined in Value, and indicates the status of the
        string.
        @see Value
    */
    protected ValueStatus status;

    /** This holds the string (actual value).
        @see Value.Str
    */
    protected String s;

    /** This constructor sets the type to the default string type 'STRING', sets the value to the
        string s, and the status to S_PROPER.
    */
    public Str(String s) 
    { 
      this(S_PROPER,s);
    }

    public Str(ValueStatus status,String s)
    {
      this(Type.STRING, status, s);
    }

    public Str(Type.Str t, String s)
    {
      this(t, S_PROPER, s);
    }

    public Str(Type.Str t, ValueStatus status, String s)
    {
      super(t);
      this.status = status;
      this.s = s;
    }

    /** This over-rides Value.status().
        @see Value#status()
    */
    public ValueStatus status()
    { 
      return status;
    }

    /** A method to retrieve the String field of this Value. */
    public String getString()
    {
      return s;
    }

    /** This returns the string field inside double quotes.  e.g. if the String is hello this method
        will return "hello".
    */
    public String toString()
    {
      return '"' + s + '"'; 
    }
    
    public boolean equals(Object o)
    {
      if(o instanceof Str)
      {
        return s.equals(((Str)o).getString());
      }
      return false;
    }

    public int hashCode() { 
    	return s.hashCode();
    }

  }


  /** This is the 'null' Value for CDMS.  It contains no information, and has Type TYPE.TRIV.  A
      static instance of this class exists as Value.TRIV.  TRIV has no value status information
      (i.e. the status of a Triv value is S_NA, meaning that the notion of status for this value is
      meaningless.)
  */
  public static class Triv extends Value                              
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6439212797110254441L;

	public Triv(Type t) 
    { 
      super(t); 
    }

    public String toString()
    {
      return "()";
    }

    public boolean equals(Object o)
    {
      return (o instanceof Triv);
    }

    public int hashCode() { 
    	return 123;
    }


	  /**
	   * readResolve() must be implemented to stop additional copies of Value.Triv appearing
	   * when using serialisation and RMI.  <br>
	   * This allows the use of "==" instead of ".equals()" when checking for TRIV. <br>
	   * Added by Rodney O'Donnell, 10/8/05 <br>
	   * see: http://java.sun.com/j2se/1.4.2/docs/api/java/io/Serializable.html <br>
	   */
	  public Object readResolve() throws ObjectStreamException {
		  return Value.TRIV;
	  }
 
  }


  /** The Scalar value class is abstract.  Any scalar value is actually an instance of the Discrete
      or Continuous value classes.  The Scalar value class mirrors the Type.Scalar class, and
      defines some common behaviour for all scalar values with the getContinuous() and getDiscrete()
      methods.  
      @see Type.Scalar
  */
  public static abstract class Scalar extends Value                 
  {
    /** The status for a Scalar value.  This can be any of S_PROPER, S_INVALID, S_UNOBSERVED,
        or S_IRRELEVANT.
    */
    protected ValueStatus status;

    public Scalar(Type t) 
    { 
      super(t); 
    }

    /** A convenience method to get the numeric value of a Scalar value as a double. */
    public abstract double getContinuous();

    /** A convenience method to get the numeric value of a Scalar value as an int.  (Continuous
        values simply cast the numeric value from a double to an int.) */
    public abstract int getDiscrete();
    
    /** A method for cyclic scalar values.  Returns the corresponding "in-range" value.
        For example, consider a cyclic discrete type with range [3,8].  getInRange() for
        a value of this type with value 9 would return 3.  If the value were 10 it would
        return 4.
        <P>
        Remember here that bounds are inclusive.  This is especially important in the
        continuous case!  For a continuous type with bounds [lwb, upb], getInRange() will
        map a value of (upb + Double.MIN_VALUE) to lwb.
        Similarly a value of (lwb - Double.MIN_VALUE) will be mapped to upb.
        <P>
        Note that the size of a discrete range [2,3] is 2, but the size of a continuous
        range [2.0,3.0] is 1.0 + Double.MIN_VALUE.
        <P>
        Issues of accuracy have not been addressed.
    */
    public abstract Value.Scalar getInRange();
    
    /** Used by the getInRange() methods of Discrete and Continuous. */
    public static double mod(double x, double y)
    {
      int n = (int)java.lang.Math.floor(x/y);
      return x - (n * y);
    }
  }


  /** Value.Discrete is for integers, subranges and symbolic Values.  Note that there is no
      Value.Symbolic class because Type.Symbolic and Value.Discrete are already sufficient.
      @see Type.Discrete
      @see Type.Symbolic
  */
  public static class Discrete extends Scalar                       
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 474386504412997356L;
	
	protected int i;  // The int Value, or the code for symbolic.

    /** This constructor sets the type to the default type Type.DISCRETE, the status to S_PROPER,
        and the value to i.
    */
    public Discrete(int i) 
    { 
      this(Type.DISCRETE, S_PROPER, i); 
    }

    /** This constructor sets the type to t, the status to S_PROPER, and the value to i. */
    public Discrete(Type.Discrete t, int i) 
    { 
      this(t, S_PROPER, i); 
    }

    /** This constructor sets the type to the default type Type.DISCRETE, the status to status, 
        and the value to i.
    */
    public Discrete(ValueStatus status, int i) 
    { 
      this(Type.DISCRETE, status, i); 
    }

    /** This constructor sets the type to t, the status to status, and the value to i. */
    public Discrete(Type.Discrete t, ValueStatus status, int i) 
    { 
      super(t);

      // Check for valid ValueStatus.
      if (status == S_NA)
      {
        throw new RuntimeException("Invalid status specified.");
      }
      else
      {
        this.status = status; 
      }

      // Verify that Value is acceptable for Type.
      if (status == S_PROPER)
      {
        if (i < t.LWB) this.status = S_INVALID; 
        if (i > t.UPB) this.status = S_INVALID; 
      }

      this.i = i; 
    }

    public ValueStatus status()
    { 
      return status;
    }

    public int getDiscrete()
    {
      return i;
    }

    /** Casts the numeric (int) value as a double. */
    public double getContinuous()
    {
      return (double) i;
    }

    /** For discrete values, this method returns the String representation of the numeric value
        using Integer.toString(int).  For symbolic values, this method returns the relevant String
        defined in the value's type information.
    */
      public String toString()
      {
	  // flag values which are not of status S_PROPER
	  // ??? remove flag.
	  String flag = "";
	  if ( status == Value.S_INTERVENTION ) { flag = "*I*"; }
	  else if (status == Value.S_UNOBSERVED) { flag = "*M*"; }
	  else if (status != Value.S_PROPER) { flag = "*" + status + "*"; }
	  if (t instanceof Type.Symbolic)
	      return ((Type.Symbolic) t).int2string(i) + flag;
	  return Integer.toString(i) + flag; 
    }
    
    public Value.Scalar getInRange()
    {
      return new Value.Discrete((Type.Discrete)t, _getInRange());
    }
    
    /** A convenience method - does the same thing as getInRange(), but returns
        an int instead of a heavy-weight value.  This method is actually called
        by getInRange(), which wraps up the result in a Value.Discrete.
    */
    public int _getInRange()
    {
      int upb = (int)((Type.Discrete)t).UPB;
      int lwb = (int)((Type.Discrete)t).LWB;
      int inc = 1;
      int result = getDiscrete();
      int x = result;
      if(x > upb) result = (int)Scalar.mod(x-upb,upb-lwb+inc) + lwb - inc;
      if(x < lwb) result = upb - (int)Scalar.mod(lwb-x,upb-lwb+inc) + inc;
      if(result < lwb) result = upb;               // in case mod returns 0.       6 with [3,4] should map to 4, not 2.
      if(result > upb) result = lwb;               // in case mod returns 0.       1 with [3,4] should map to 3, not 5.
      return result;
    }
    
    public boolean equals(Object o)
    {
      if(o instanceof Scalar)
      {
        return (i == ((Scalar)o).getDiscrete());
      }
      return false;
    }
    
    public int hashCode() { 
    	return i;
    }
  }

  /** A special class for optimisation.  Used in the case where a function must be
      repetitively called with different discrete values.  Rather than create a new
      discrete value each time we use this class.  This is a dangerous class to use
      and should only be used when you know or can guarantee that the funtion being
      called does not return a lazy value.  A good way of guaranteeing that the
      function is not lazy is if you use the applyInt method - since this returns a
      Java primitive.
      @see Value.VariableContinuous
  */
  public static class VariableDiscrete extends Discrete
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5851162563812107463L;

	public VariableDiscrete(int i)
    {
      super(i); 
    }

    public VariableDiscrete(Type.Discrete t, int i)
    {
      super(t, i);
    }

    public Object clone()
    {
      return new Discrete((Type.Discrete) t, i);
    }

    public void setDiscrete(int i)
    {
      this.i = i;
    }
  }

  /** Value.Continuous represents continuous valued variables measured to an absolute 
      accuracy (+-delta).  The accuracy can be obtained with the getAccuracy() method, 
      which returns possible range of values this variable lies in (in the form of a 
      Value.Continuous.Range class).  Other kinds of accuracy (relative etc.) are 
      supported by child classes of Value.Continuous which over-ride the getAccuracy() 
      method, and may use delta differently.
      @see Value.Continuous.Range
      @see Type.Continuous
  */
  public static class Continuous extends Scalar                   
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4508273692007357991L;

	/** This field holds accuracy information.  It should only be used inside the class,
        or by child classes.  Accuracy information can be accessed from other classes using
        this getAccuracy() method.
    */
    protected double delta;  

    /** The numeric value of the Continuous value.  From outside this class this should
        be accessed using the getContinuous() method defined in Value.Scalar. */
    protected double x; 

    /** The default accuracy (for values created with no accuracy information specified) is
        Double.MIN_VALUE.  This is the smallest positive number representable using java doubles.
        The range of such a value is then [x-double.MIN_VALUE, x+double.MIN_VALUE].
    */
    public static final double DEFAULT_DELTA = Double.MIN_VALUE;

    /** The quick constructor creates a continuous value with Type.CONTINUOUS, status S_PROPER,
        measurement accuracy of +/- DEFAULT_DELTA, and value x.
    */
    public Continuous(double x) 
    { 
      this(Type.CONTINUOUS, S_PROPER, x, DEFAULT_DELTA); 
    }

    /** This constructor allows specification of the value x, and measurement accuracy (+/-) delta.
        The type of the value is Type.CONTINUOUS, and it has S_PROPER status.
    */
    public Continuous(double x, double delta) 
    { 
      this(Type.CONTINUOUS, S_PROPER, x, delta); 
    }

    /** This constructor allows specification of the value x, measurement accuracy (+/-) delta, and
        type t.  The resultant value has S_PROPER status, providing x is within the bounds
        specified in the continuous type t.  If x is outside these bounds it will be given status
        S_INVALID.
    */
    public Continuous(Type.Continuous t, double x, double delta) 
    { 
      this(t, S_PROPER, x, delta); 
    }

    /** This constructor creates a continuous value with status S_PROPER, default measurement
        accuracy, numeric value x, and (continuous) type t.
    */
    public Continuous(Type.Continuous t, double x) 
    { 
      this(t, S_PROPER, x, DEFAULT_DELTA); 
    }

    /** This constructor creates a continuous value with status 'status', default measurement
        accuracy, numeric value x, and type Type.CONTINUOUS.
    */
    public Continuous(ValueStatus status, double x)
    {
      this(Type.CONTINUOUS, status, x, DEFAULT_DELTA);
    }

    /** This constructor creates a continuous value with status 'status', default measurement
        accuracy, numeric value x, and (continuous) type t.
    */
    public Continuous(Type.Continuous t, ValueStatus status, double x) 
    {
      this(t, status, x, DEFAULT_DELTA);
    }

    /** This constructor creates a continuous value with status 'status', measurement accuracy
        delta, numeric value x, and (continuous) type t.
    */
    public Continuous(Type.Continuous t, ValueStatus status, 
                      double x, double delta) 
    { 
      super(t);

     // Check for valid ValueStatus.
      if (status == S_NA)
      {
          throw new RuntimeException("Invalid status specified.");
      }
      else
      {
        this.status = status; 
      }

      // Verify that Value is acceptable for Type.
      if (status == S_PROPER)
      {
        if (x < t.LWB) this.status = S_INVALID; 
        if (x > t.UPB) this.status = S_INVALID; 
      }
      this.delta = delta;
      this.x = x; 
    }

    public ValueStatus status()
    { 
      return status;
    }

    /** This class is used to represent continuous intervals for the measurement accuracy of
        continuous values.
    */
    public static final class Range 
    { 
      /** The lower bound of the continuous interval. */
      public double LWB;

      /** The upper bound of the continuous interval. */
      public double UPB;

      public Range(double LWB, double UPB) 
      { 
        this.LWB = LWB; 
        this.UPB = UPB; 
      }
    }

    /** Because any continuous value is measured or stated only to a finite accuracy, the true value
        could actually lie anywhere in a range (a Continuous interval).  This method returns this
        range as a Value.Continuous.Range object.  The Range returned is guaranteed to be within the
        bounds of this continuous type.  The field x and the getContinuous() method represent the
        point estimate within this range.
    */
    public Range getAccuracy() // accuracy  interval of v
    { 
      double low, hi;

      low = x - delta; 
      hi = x + delta; 

      if (low < ((Type.Continuous)this.t).LWB) low = ((Type.Continuous)this.t).LWB;
      if (hi  > ((Type.Continuous)this.t).UPB) hi = ((Type.Continuous)this.t).UPB;

      return new Range(low, hi);
    }

    public String toString()
    {
      return Double.toString(x); 
    }

    public double getContinuous()
    {
      return x;
    }

    public int getDiscrete()
    {
      return (int)x;
    }
    
    public Value.Scalar getInRange()
    {
      return new Value.Continuous((Type.Continuous)t, _getInRange());
    }
    
    
    /** A convenience method - does the same thing as getInRange(), but returns
        an double instead of a heavy-weight value.  This method is actually called
        by getInRange(), which wraps up the result in a Value.Continuous.
    */
    public double _getInRange()
    {
      double upb = (int)((Type.Continuous)t).UPB;
      double lwb = (int)((Type.Continuous)t).LWB;
      double inc = Double.MIN_VALUE;
      double result = getContinuous();
      double x = result;
      if(x > upb) result = Scalar.mod(x-upb,upb-lwb+inc) + lwb - inc;
      if(x < lwb) result = upb - Scalar.mod(lwb-x,upb-lwb+inc) + inc;
      if(result < lwb) result = upb;   // in case mod returns 0.
      if(result > upb) result = lwb;   // in case mod returns 0.
      return result;
    }
    
    public boolean equals(Object o)
    {
      if(o instanceof Scalar)
      {
        return (x == ((Scalar)o).getContinuous());
      }
      return false;
    }
    
    public int hashCode() { 
    	return new Double(x).hashCode();
    }

  }

  /** A special class for optimisation.  Used in the case where a function must be
      repetitively called with different continuous values.  Rather than create a new
      continuous value each time we use this class.  This is a dangerous class to use
      and should only be used when you know or can guarantee that the funtion being
      called does not return a lazy value.  A good way of guaranteeing that the
      function is not lazy is if you use the applyDouble method - since this returns a
      Java primitive.
      @see Value.VariableDiscrete
  */
  public static class VariableContinuous extends Continuous
  {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 94049261153213710L;

	public VariableContinuous(double x)
    {
      super(x);
    }

    public VariableContinuous(Type.Continuous t, double x, double delta)
    {
      super(t, x, delta);
    }

    public VariableContinuous(Type.Continuous t, double x)
    {
      super(t, x);
    }

    public Object clone()
    {
      return new Continuous((Type.Continuous) t, x);
    }

    public void setContinuous(double x)
    {
      this.x = x;
    }

    public void setDelta(double delta)
    {
      this.delta = delta;
    }
  }

  /** This extension of Value.Continuous implements relative accuracy.  The variable delta here is
      used to represent the fractional error.  i.e.  real value = x +- (delta * x).
  */
  public static class RelativeContinuous extends Continuous
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7424432065371968173L;

	public RelativeContinuous(Type.Continuous t, ValueStatus status, 
                              double x, double delta) 
    {
      super(t, status, x, delta);
    }
   
    /** getAccuracy() returns the measurement accuracy Range about this value.  The Range returned
        is guaranteed to be within the bounds of v.  
    */
    public Range getAccuracy()
    { 
      double low, hi;

      low = x - java.lang.Math.abs(x * delta); 
      hi = x + java.lang.Math.abs(x * delta); 

      if (low < ((Type.Continuous)this.t).LWB) low = ((Type.Continuous)this.t).LWB;
      if (hi  > ((Type.Continuous)this.t).UPB) hi  = ((Type.Continuous)this.t).UPB;

      return new Range(low, hi);
    }
  }

  /** This extension of Value.Continuous implements asymetric accuracy.  The variable 'delta' here
      is used to represent the lower bound on x, and the extra variable 'upperDelta' is used to
      represent the upper bound on x.  i.e.  real value is in the range [delta, upperDelta]. 
  */
  public static class AsymetricContinuous extends Continuous
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2108406166089960399L;
	protected double upperDelta;

    public AsymetricContinuous(Type.Continuous t, ValueStatus status, 
                               double x, double low, double hi) 
    {
      super(t, status, x, low);
      upperDelta = hi;
    }
   
    /** getAccuracy() returns the measurement accuracy Range about this value.  The Range returned
        is guaranteed to be within the bounds of v.
    */
    public Range getAccuracy()
    { 
      if (delta < ((Type.Continuous)this.t).LWB) delta = ((Type.Continuous)this.t).LWB;
      if (upperDelta  > ((Type.Continuous)this.t).UPB) upperDelta  = ((Type.Continuous)this.t).UPB;

      return new Range(delta, upperDelta);
    }
  }


  /** Multivariate data, a collection of (possibly) heterogeneous components.  status is not defined
      ( = S_NA) for Structured values.<p>
      <strong>Future possibilities:</strong> status could be used to indicate whether all components
      of the structured value are proper(to save checking each one).<P>
      This class is abstract, and does not define how the components should be stored or retrieved.
      The class DefStructred is a default implementation, suitable for most structured values.
      @see Value.DefStructured
      @see Type.Structured
  */
  public static abstract class Structured extends Value            
  { 
	  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	  private static final long serialVersionUID = -4691570441862060366L;

    public Structured(Type.Structured t)
    { 
      super(t);
    }

    /** A method returning the component at index i. */
    public abstract Value cmpnt(int i);

    /** A method returning the number of components. */
    public abstract int length();

    /** A shortcut to return the component at index i as a double.  If the component at index i is
        not a Scalar value this will produce a class cast exception.
    */
    public double doubleCmpnt(int i)
    {
      return ((Scalar) cmpnt(i)).getContinuous();
    }

    /** A shortcut to return the component at index i as an int.  If the component at index i is
        not a Scalar value this will produce a class cast exception.
    */
    public int intCmpnt(int i)
    {
      return ((Scalar) cmpnt(i)).getDiscrete();
    }

    public String toString()
    {
      Type.Structured st = (Type.Structured) t;
      StringBuffer sb = new StringBuffer();

      sb.append("(");
      for (int i = 0; i < length(); i++)
      {
        if (i != 0) sb.append(",");
        if (st.labels != null && st.labels[i] != null && st.labels[i].compareTo("") != 0)
        {
          sb.append(st.labels[i]);
          sb.append(" = ");
        }
        sb.append(cmpnt(i).toString());
      }
      sb.append(")");

      return sb.toString();
    }
    
    public boolean equals(Object o)
    {
      if(o instanceof Structured)
      {
        Structured s = (Structured)o;
        if(length() != s.length()) return false;
        int count;
        for(count = 0; count < length(); count++)
        {
          if(!cmpnt(count).equals(s.cmpnt(count))) return false;
        }
        return true;
      }
      return false;
    }

    
    public int hashCode() { 
    	int code[] = new int[length()];
    	for (int i = 0; i < code.length; i++) {
    		code[i] = cmpnt(i).hashCode();
    	}
    	
    	// Use hashcode of "array of hashcodes"
    	return Arrays.hashCode(code);
    }

  }

  /** The default extension of Value.Structured.  This class should suffice for most structured
      values.  The components of the structured value are stored as an array of Values.
  */
  public static class DefStructured extends Structured
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -941481956758342696L;

	/** The components of the value stored as an array. */
    protected Value[] cmpnts; 

    /** The status of this structured value. */
    protected ValueStatus status;

    public DefStructured(Type.Structured t, Value[] cmpnts)
    { 
      this(t, cmpnts, S_PROPER);
    }

    /** This constructor allows specification of a value status, as well as a structured type and
        the components.
    */
    public DefStructured(Type.Structured t, Value[] cmpnts, ValueStatus status)
    {
      super(t);
      this.cmpnts = cmpnts;
      this.status = status;
    }

    public static boolean[] booleanArray(boolean v, int n)
    {
      boolean array[] = new boolean[n];
      int count;
      for(count = 0; count < n; count++)
      {
        array[count] = v;
      }
      return array;
    }

    public DefStructured(Value[] cmpnts)
    {
      this(new Type.Structured(cmpnts, null, 
                               booleanArray(false, cmpnts.length)), 
                               cmpnts);
    }

    /** This constructor creates a structured type for the resultant value, with component types
        matching the components supplied, and with component labels given by the names parameter.
    */
    public DefStructured(Value[] cmpnts, String[] names)
    {
      this(new Type.Structured(cmpnts, names, 
                               booleanArray(true, cmpnts.length)), 
                               cmpnts);
    }

    public Value cmpnt(int i)
    {
      return cmpnts[i];
    }

    public int length()
    {
      return ((Type.Structured) t).cmpnts.length;
    }

  }

  /** A Vector is a homogeneous collection of Values, of arbitrary length.  Vector is abstract, it
      has no constructor, length() and elt(i) must be instantiated and, in addition, one of
      intAt(i), doubleAt(i) and cmpnt(i) must generally be overridden for specific subclasses of
      Vector.  Implementors of subclasses of Vector can and should consider carefully matters of
      storage and access efficiency; this is particularly important for single-column Vectors.
      @see Type.Vector
  */
  public static abstract class Vector extends Value                  
  {
	  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	  private static final long serialVersionUID = -1392610946463761061L;

    public Vector(Type.Vector t)
    {
      super(t);
    }

    /** This method returns the number of elements in the vector. */
    public abstract int length();

    /** This method returns the value at index i.  The first element of the vector has index 0. */
    public abstract Value elt(int i); // NB. must instantiate       // elt(i)

    /** This method returns an element of the vector indexed by the Value v.  The type of v should
        match the index type of the vector.  The default implementation of this method casts v as a
        Scalar value, retrieves an integer i from it using the v.getDiscrete() method, and returns
        element i of the vector.  A cast error will occur if v is not a Scalar value.  This method
        should be over-ridden if a child-vector class knows a better way of accessing an element by
        a value.
    */
    public Value elt(Value v)
    {
      return elt(((Value.Scalar)v).getDiscrete());
    }

    /** Unless it is a Vector of Vector, a Vector should normally implement, i.e. override, one of
        intAt(i), doubleAt(i), cmpnt(i).  The "inappropriate" methods will fail with cast errors if
        used.
    */

    /** Override intAt(i) to make a fast shortcut to the int "code" for a Discrete elt(i). */
    public int intAt(int i) 
    {
// 	if (true) {  // ??? rempve this.
// 	    System.out.println("Inefficient intAr() : " + this.getClass());
// 	    RuntimeException e = new RuntimeException();
// 	    e.printStackTrace();
// 	    System.exit(0);
// 	}
      return ( (Scalar) elt(i)).getDiscrete(); 
    }

    /** Override doubleAt(i) to make a fast shortcut to the double for a Continuous elt(i). */
    public double doubleAt(int i)
    { 
      return ( (Scalar) elt(i)).getContinuous(); 
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer();

      sb.append("["); 
      for (int i = 0; i < length(); i++)
      {
        if (i != 0) sb.append(",");
        try
        {
//          sb.append(weight(i) + ":");   // Temporary `feature' to print weights of each element.
          sb.append(elt(i).toString());
        }
        catch (Exception e)
        {
          sb.append(e);
        }
      }
      sb.append("]"); 
      
      return sb.toString();
    }

    /** The notion of weight for a vector element allows "partial values" to be stored in vectors. 
        Override weight(i) to implement fractions of data for partial assignment etc. 
        @see VectorFN.FatWeightVector 
    */
    public double weight(int i)
    {
      return 1.0;
    }

    /** Override cmpnt(i) to make a fast shortcut to column/attribute i of a MultiCol Vector,  e.g.
        see zip() in FN. 
    */
    public Vector cmpnt(int col)
    { 
      return new Cmpnt(this,col); 
    } 

    /** A class used for the default implementation of the cmpnt(int) method of Value.Vector. */
    private static class Cmpnt extends Vector // NB. private
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 2091781131939457477L;

	/** The multicolumn vector that this vector is a column of. */
      private Value.Vector v;

      /** The number of the column (the first column is 0) */
      private int col;

      public Cmpnt(Value.Vector v, int col) 
      { 
        super( new Type.Vector(Type.DISCRETE, 
                    ((Type.Structured) ((Type.Vector) v.t).elt).cmpnts[col], 
                    false, false, false, false) );
        this.v = v;
        this.col = col; 
      }

      public int length() 
      { 
        return v.length(); 
      }

      public int intAt(int i) 
      {
	  return ((Structured) v.elt(i)).intCmpnt(col); 
      }

      public double doubleAt(int i)
      { 
        return ((Structured) v.elt(i)).doubleCmpnt(col); 
      }

      public Value elt(int i)
      { 
        return ((Structured) v.elt(i)).cmpnt(col); 
      }

      public double weight(int i)
      {
        return v.weight(i);
      }
    } 

    
    /** This returns a new vector, comprising elements i through to j (inclusive) of this vector.
        Override sub(i) to make a fast shortcut to range of values.
    */
    public Vector sub(int i, int j)
    { 
      return new SubVector(this,i, j); 
    } 

    /** A vector class for the default implementation of the sub(int,int) method. */
    private static class SubVector extends Vector // NB. private
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 672845768997410437L;

	/** The vector of which this vector is a sub-range. */
      private Value.Vector v;

      /** The start and end elements. */
      private int s, e;

      public SubVector(Value.Vector v, int s, int e) 
      { 
        super((Type.Vector) v.t);
        this.v = v;
        this.s = s;
        this.e = e;
      }

      public int length() 
      { 
        return e - s + 1; 
      }

      public Value elt(int i)
      { 
        return v.elt(s + i); 
      }
      
      public double weight(int i)
      {
        return v.weight(s + i);
      }
    } 
    
    public boolean equals(Object o)
    {
      if(o instanceof Vector)
      {
        Vector v = (Vector)o;
        if(length() != v.length()) return false;
        int count;
        for(count = 0; count < length(); count++)
        {
          if(!elt(count).equals(v.elt(count))) return false;
        }
        return true;
      }
      return false;
    }

    
    public int hashCode() { 
    	int code[] = new int[length()];
    	for (int i = 0; i < code.length; i++) {
    		code[i] = elt(i).hashCode();
    	}

    	// Use hashcode of "array of hashcodes"
    	return Arrays.hashCode(code);
    }

  }

  /** The value class corresponding to Type.Model.  This class is abstract, and defines the 
      behaviour required of a model. 
      @see Type.Model
  */
  public static abstract class Model extends Value
  { 
    public Model(Type.Model t) 
    { 
      super(t); 
    }

    // logP(X|Y,Z)
    /** This gives the log-probability of value x, given the parameter y, and "input" value z. 
    */
    public abstract double logP(Value x, Value y, Value z);

    // logP(X|Y,Z) where v = (X,Y,Z)
    /** This gives the log-probability of value x (or the log-probability-density if x is from a 
        continuous data space), given the parameter y, and "input" value z, where the structured
        value v = (x,y,z).  The default implementation of this method calls logP(Value x, Value y,
        Value z).
    */
    public double logP(Value.Structured v)
    {
      return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }

    // Returns a vector of elements from the data-space conditional on Y,Z.
    /** This method generates a vector of n values, using the parameters y, and the input value z.
    */
    public abstract Value.Vector generate(Random rand, int n, Value y, Value z);

      /** NOTE: This function crashes when z.length() == 0 */
    public Value.Vector generate(Random rand, Value y, Value.Vector z)
    {
      Value[] res = new Value[z.length()];
      for(int count = 0; count < res.length; count++)
      {
        res[count] = generate(rand, 1, y, z.elt(count)).elt(0);
      }
      
      return new VectorFN.FatVector(res);
    }

    /** This method predicts a value, using the parameters y and input value z.  */
    public abstract Value predict(Value y, Value z);

    /** This method predicts a vector of values.  The resultant predicted vector is the same length
        as the input vector z, and each element i is predicted using the parameters y, and the
        input value z.elt(i).
    */
    public abstract Value.Vector predict(Value y, Value.Vector z);

    // logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2)...
    /** This gives the log-probability (or the log-probability-density for continuous data spaces)
        of the vector x, given parameters y, and input vector z.  z and x must be of the same
        length, and it is assumed each element i of x has been generated using the parameters y and
        the corresponding element i of z as an input value. 
    */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
      double s = 0;
      for (int i = 0; i < x.length(); i++)
        s += logP(x.elt(i),y,z.elt(i));
      return s;
    }

    /** Returns sufficient statistics for this model of the data. */
    public abstract Value getSufficient(Value.Vector x, Value.Vector z);

    // logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2)... where s is a sufficient statistic of X&Z for Y.
    /** Returns the log-probability (or the log-probability-density for continuous data spaces)
        of the vector x, given parameters y, and input vector z (where x and z are represented
        by the sufficient statistics s.
    */
    public abstract double logPSufficient(Value s, Value y);

    /** A method to display a set of model parameters.  By default this returns a CDMS browser with
        the default representation of the parameters.  It can be over-ridden, however, to provide a
        more user-friendly graphical representation (e.g. a line-plot of a normal distribution with 
        specified mu and sigma).
    */
    public java.awt.Component displayParams(Value y)
    {
      cdms.plugin.desktop.Browser browser = new cdms.plugin.desktop.Browser();
      browser.setSubject(y);
      return browser;
    }

    public String toString()
    {
      return "Model";
    }
  }

  /** Instances of Function are first class Value under this sytem.  The only method required to
      be implemented is the apply method.
      @see Type.Function
      @see FN
  */
  public static abstract class Function extends Value                   
  {
    public Function(Type.Function t)
    {
      super(t);
    }

    /** apply computes the result Value of the Function, i.e. it is the body of the Function. */
    public abstract Value apply(Value param);

    /** A shortcut method to get the result of the function as a double.  If the result of the
        function is not a Scalar value this method will throw a run time exception.
    */
    public double applyDouble(Value param)
    {
      Value res = apply(param);
      if (res instanceof Scalar)
        return ((Scalar) res).getContinuous();
      else throw new RuntimeException("Function.applyDouble call is invalid.");
    }

    /** A shortcut method to get the result of the function as an int.  If the result of the
        function is not a Scalar value this method will throw a run time exception.
    */
    public int applyInt(Value param)
    {
      Value res = apply(param);
      if (res instanceof Scalar)
        return ((Scalar) res).getDiscrete();
      else throw new RuntimeException("Function.applyInt call is invalid.");
    }

    public String toString()
    {
      return ((Type.Function) t).param.toString() + " -> " + 
             ((Type.Function) t).result.toString();
    }
  }


  /** Objs are values representing Java Objects with state.  The Java Object must be serializable.
      The object may be passes explicitly to the constructor, or created lazily when it is first
      accessed and then cached.  To create an object lazily a Type.Obj must be passed to the 
      constructor, containing the object's class name.  This will only work if there is a 
      constructor for that class that takes no arguments.
  */
  public static class Obj extends Value     
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1649622505812814260L;
	
	/** A pointer to the (serializable) java object this value represents. */
    protected Object o = null;

    public Obj(Type.Obj t)
    {
      super(t);
    }

    public Obj(Object o)
    {
      super(new Type.Obj(o.getClass().getName()));
      this.o = o;
      checkValid();
    }

    public Obj(Type.Obj t, Object o)
    {
      super(t);
      this.o = o;
      checkValid();
    }

    /** A method to check that the object is serializable. */
    protected void checkValid()
    {
      if (!(o instanceof java.io.Serializable)) 
        throw new RuntimeException("Class " + ((Type.Obj) t).className + 
                                   " is not Serializable and is therefore not a valid Obj.");
    }

    /** This method retrives the java object that this Obj value represents. */
    public synchronized Object getObj()
    {
      // Lazy Obj;
      try
      {
        if (o == null)
        {
          o = Class.forName( ((Type.Obj)t).className ).newInstance();
          checkValid();
        }
        return o;
      }
      catch (Exception e)
      {
        throw new RuntimeException("Call to Obj.getObj() failed: " + ((Type.Obj)t).className);
      }
    }  

    public Type getMethodType(String methName)
    {
      return Type.FUNCTION; 
    }

    /** This method is used to call methods of the Java Object represented by this value,
        and return their result.  Only methods with either void or Value return types and
        parameter types may be called in this manner.  If the parameter type of the desired
        method is void, the param arguement should be Value.TRIV.  If the result type
        of the desired method is null, Value.TRIV is returned.
    */
    public Value invoke(String methName, Value param)
    {
      try
      {
        if (param instanceof Value.Triv)   // If triv then try without params ().
        {
          try
          {
            Method method = getObj().getClass().getMethod(methName,new Class[] { } );
            Object res = method.invoke(getObj(),new Object[] { });
            if (res instanceof Value) return (Value) res;
              else return Value.TRIV;
          }
          catch (NoSuchMethodException e)
          {
            ;
          }
        }

        /** The getMethod() method does not match descendants, so
            we have to manually try the hierarchy.  LF 
        */
        for (Class cls = param.getClass(); ; cls = cls.getSuperclass())
        {
          try
          {
            Method method = getObj().getClass().getMethod(methName,new Class[] { cls } );
            Object res = method.invoke(getObj(),new Object[] { param });
            if (res instanceof Value) return (Value) res;
              else return Value.TRIV;
          }
          catch (NoSuchMethodException e)
          {
            continue; // try super class
          }
        }

      /* We can allow method calls to Java objects using Java datatypes using
         Value.toNative().  The getMethod method distinguishes between primitive types
         and their Object representations as does the Java language.  The invoke method
         does not.  The problem is that we do not know which Class to pass in the
         getMethod method since all data is returned from toNative() as Objects because
         Java does not have a primitive super-type.

         So code like the following won't be able to call methods expecting primitive types:
         Method method =
           getObj().getClass().getMethod(methName,new Class[] { param.toNative().getClass() } );
         return (Value) method.invoke(o,new Object[] { param.toNative() });

         Maybe there is some code that does the conversion somewhere.  What do they
         do in JPython?

         In the meantime we can only call methods that expect a Value class.
      */

      }
      catch (Exception e)
      {
        throw new RuntimeException("Invocation problem with " + ((Type.Obj)t).className + " method "
          + methName + " : " + e.toString());
      }

    }

    public String toString()
    {
      return "Class(" + ((Type.Obj) t).className + ")";
    }
  }


  // Some particularly useful special Values.

  /** Value.TRIV is the sole Value in Type.TRIV.  
    @see Type#TRIV
  */
  public static final Value TRIV = new Triv(Type.TRIV);

  public static final Discrete FALSE = Type.BOOLEAN.string2value("false");

  public static final Discrete TRUE  = Type.BOOLEAN.string2value("true");
  
  public static final Continuous PI = new Value.Continuous(Math.PI);

  /** Returns the name for the value v in the environment.  A value is given a name when it is added
      to the environment.  This method returns null if the value v does not exist in the
      environment.
  */
  public static String byValue(Value v)  
  {
    return Environment.env.getName(v);
  }

    public static final Value.Str newline = new Value.Str("\n");
  static 
  {
    Environment.env.add("()","Standard",TRIV,"");
    Environment.env.add("true","Standard",TRUE,"");
    Environment.env.add("false","Standard",FALSE,"");
    Environment.env.add("PI","Standard",PI,"3.141592654");
    Environment.env.add("newline","Standard",newline,"linefeed char");

    // Report the random number generator in use.
    String rngMess = (new java.util.Random()).toString();
    if (rngMess.indexOf('@') != -1)
      System.out.println("Using Sun's standard random number generator.");
    else System.out.println(rngMess); 
  }

}

