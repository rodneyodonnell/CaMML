//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Type.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.util.Hashtable;
import java.io.ObjectStreamException;

/** The Types of data and Values that can be manipulated.  Classes Type and Value are crucial
    because they define how data are represented and passed around the System.  Type and Value must
    be kept small, clean and orthogonal in design; they can only be changed or extended after
    considerable thought.
    <p>
    The Type class is the super-type of all CDMS Types.  No Value should ever actually be of Type
    "Type" - every Value should be of a specific sub-type of Type.<BR>Types can be broken into 2
    groups -<BR>
    <OL>
    <LI>Simple Types - these types have no nested types in their type information (they only have
    non-Type fields).  e.g. Discrete, Str.
    <LI>Complex Types - the type information for these Types involves other Types (they have Type
    fields).  e.g. Function (has parameter and result type information), Structured (has a Type for
    each of its components), Vector (has an index Type and element Type).
    </OL>
    <P>
    The toString method of each type return a possible type constructor representation of the type. 
    <p>
    See also the parallel Value classes.
    @see Value
*/

public class Type implements java.io.Serializable
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5364771627802991420L;

public Type()
  {
    super();
  }

  /** The hasMember function returns true if the Type "t" is a sub-type of this Type, or if "t" is
      equivalent to this Type.  Every type is a subtype of "Type".
  */
  public boolean hasMember(Type t)
  {
    return hasMember(t, new Hashtable());
  }

  /** This method is used to check if type "t" is a subtype of this type, in a specific type
      variable environment represented by the Hashtable "h".
  */
  public boolean hasMember(Type t, Hashtable h)
  {
    if (t instanceof Variable)
    {
      return ((Variable)t).unify(this, h);
    }
    return true;
  }

  public String toString()
  {
    return "TYPE";
  }

  /** Returns the "nice" name for this type class. */
  public String getTypeName()
  {
    return getTypeName(getClass());
  }

  /* Since we cannot override static methods we handle nice names for all base types by storing them
     in a hash table. 
  */
  private static java.util.Hashtable typeNameHash = new java.util.Hashtable(20);

  /** Returns the "nice" name for the (type) class "c".  If the class "c" is not a Type class, the
      method returns "Unknown".  These are the names used for the type hierarchy in the environment.
  */
  public static String getTypeName(Class c)
  {
    java.lang.Object name = typeNameHash.get(c);
    if (name == null) return "Unknown";
      else return (String) name;
  }

  static 
  {
    typeNameHash.put(Type.class,"Type");
    typeNameHash.put(Type.Str.class,"String");
    typeNameHash.put(Type.Triv.class,"Triv");
    typeNameHash.put(Type.Scalar.class,"Scalar");
    typeNameHash.put(Type.Discrete.class,"Discrete");
    typeNameHash.put(Type.Continuous.class,"Continuous");
    typeNameHash.put(Type.Structured.class,"Structured");
    typeNameHash.put(Type.Union.class,"Union");
    typeNameHash.put(Type.Vector.class,"Vector");
    typeNameHash.put(Type.Function.class,"Function");
    typeNameHash.put(Obj.class,"Object");
    typeNameHash.put(Type.Model.class,"Model");
    typeNameHash.put(Type.Symbolic.class,"Symbolic");
    typeNameHash.put(Type.Variable.class,"Variable");
  }

  /** Triv is the null type for CDMS. */
  public static class Triv extends Type
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1831962839075320620L;

	public boolean hasMember(Type t, Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        return Triv.class.isInstance(t);
      }
    }

    public String toString()
    {
      return "()";
    }
  }
 
  /** Type.Variable is not a (monomorphic) Type as such; it is a Variable over Type for polymorphic
      Functions, e.g. id: t -> t.
      @see Type.Function
  */
  public static final class Variable extends Type
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1442261087640698801L;
	private static int counter = 0;
    public final int n;

    public Variable() 
    { 
      n = (counter++); 
    }

    /** The type "t" is a sub-type of this Variable type if:<BR>"t" is a sub-type of the type this
        Variable type is bound to (in the type variable environment represented by "h").  An unbound
        Variable type can be thought of as being bound to Type "Type".  Hence if this Variable type
        is unbound, the method will always return true.<P>
        If "t" is also a Variable type the method returns true if "t" can be unified with the type
        this Variable type is bound to.
    */
    public boolean hasMember(Type t, Hashtable h)
    {
      Type currentBinding = (Type) h.get(this);
      if (currentBinding != null)
      {
        if (t instanceof Variable)
          return ((Variable) t).unify(currentBinding,h);
        return currentBinding.hasMember(t,h);
      }
      else
      {
        return true;
      }
    }

    /** Returns true if this Variable type can be (or is already) bound to the type "t", or a
        sub-type of "t".
    */
    public boolean unify(Type t, Hashtable h)
    {
      Type currentBinding = (Type)h.get(this);
      if (currentBinding == null)
      {
        h.put(this, t);
        return true;
      }
      else
      {
        if (t.hasMember(currentBinding,h))
        {
          return true;
        }
        else
        {
          if (currentBinding.hasMember(t,h))
          {
            h.put(this, t);
            return true;
          }
          else
          {
            return false;
          }
        }
      }
    }

    public String toString()
    {
      return "t(" + hashCode() + ")";
    }
  }


  /** The String Type for CDMS.  @see Value.Str */
  public static class Str extends Type
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6127964478841012907L;

	public boolean hasMember(Type t, Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        return Str.class.isInstance(t);
      }
    }

    public String toString()
    {
      return "String";
    }
  }

  /** The Scalar type is the parent-type of the Discrete and Continuous types.
      No values actually have type "Scalar".  It exists only for convenience, and is a simple
      way for java code or CDMS Functions etc. to refer to a Union of Discrete and Continuous.
      Scalar is in fact equivalent to a Union of Discrete and Continuous.
      <P>
      The Scalar type requires bounds to be set.  An unbounded Continuous type can be expressed
      using Double.MAX_VALUE and -Double.MAX_VALUE for the bounds since the value will
      be represented as a double.  The same applies to a Discrete using Integer.MAX_VALUE.  
      Setting the bounds to any value less than these extremes implies knowledge of the bounds.  
      An interesting example is setting the upper bound on a Discrete type to 
      Integer.MAX_VALUE - 1 and the lower bound to Integer.MIN_VALUE + 1, this represents
      all bounded Scalar types.  Another example is using Double.MAX_VALUE and -Double.MAX_VALUE
      as the bounds, this represents all scalar types since Double.MAX_VALUE > Integer.MAX_VALUE
      and -Double.MAX_VALUE < Integer.MAX_VALUE.
      <p>
      The bounds are inclusive (ie [lwb,upb]).  An exclusive bound can be represented for
      a Continuous type as plus or minus Double.MIN_VALUE.
  */
  public static class Scalar extends Type
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5720349404439638177L;
	public static final int NO  = 0;
    public static final int YES = 1;
    public static final int MAYBE = 2;

    /** True if this type is cyclic (i.e. "wraps around") e.g. day_of_the_week, angle etc. */
    public boolean isCyclic = false;

    /** True if we care or know whether this type is cyclic.  If this is false
        the value of isCyclic is meaningless. 
    */
    public boolean ckIsCyclic = false;

    /** The value of the upper bound.  A double is used as this needs to be able to represent
        upper bounds for Continuous types, however it should only have integer values for Discrete
        types. 
    */
    public double UPB;

    /** The value of the lower bound.  A double is used as this needs to be able to represent
        upper bounds for Continuous types, however it should only have integer values for Discrete
        types. 
    */
    public double LWB;

    /** The constructor for the default Scalar type.  This creates an instance of the most general 
        Scalar type where we do not specify whether or not it is bounded, or cyclic.  
        This should not need to be called as an instance of this general Scalar type 
        already exists as Type.SCALAR. 
    */
    public Scalar()
    {
      this(-Double.MAX_VALUE,Double.MAX_VALUE,false,false);
    }

    /** The constructor allowing full specification of a Scalar type.  
        See member variables for definitions of parameters. 
    */
    public Scalar(double LWB, double UPB, boolean ckIsCyclic, boolean isCyclic)
    {
      this.LWB = LWB;
      this.UPB = UPB;
      this.ckIsCyclic = ckIsCyclic;
      this.isCyclic = isCyclic;
    }

    public int hasUPB()
    {
      return MAYBE;
    }

    public int hasLWB()
    {
      return MAYBE;
    }

    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Scalar.class.isInstance(t))
          return false;

        Scalar tmp = (Scalar)t;

        if (tmp.UPB > UPB) return false;
        if (tmp.LWB < LWB) return false;

        if (ckIsCyclic && isCyclic != tmp.isCyclic) return false;

        return true;  // Passed.
      }
    } 

    public String toString()
    {
      StringBuffer sb = new StringBuffer("Scalar(");
      boolean c = false;

      if (LWB != -Double.MAX_VALUE)
      {
        sb.append("LWB=" + LWB); 
        c = true; 
      }

      if (UPB != Double.MAX_VALUE)
      {
        if (c) sb.append(",");
        sb.append("UPB=" + UPB); 
        c = true; 
      }

      if (ckIsCyclic)
      {
        if (c) sb.append(",");
        sb.append("cyclic=" + isCyclic); 
        c = true; 
      }

      sb.append(")");

      if (c) return sb.toString();
        else return "Scalar";
    }
  }

  /** Type.Discrete includes ints, subranges thereof and symbolic Types.
      @see Type.Scalar
      @see Value.Discrete
  */
  public static class Discrete extends Scalar
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6367726304425877670L;

	/** A value of true indicates discrete values of this type are ordered.  i.e. The notions of
        "greater than" and "less than" make sense in some way.  e.g. integers, a symbolic "low",
        "medium", "high" type.<BR>A value of false indicates the values are not ordered.  e.g. a
         symbolic "animal", "vegetable", "mineral" type.
    */
    public boolean isOrdered = false;

    /** A value of true indicates we know or care whether the type is ordered or not.  False
        indicates we do not know or do not care whether the type is ordered or not.
    */
    public boolean ckIsOrdered = false;

    /** The default constructor that creates a discrete type with no information regarding bounds,
        cyclicity, or order.  This does not to be called as a discrete type object like this already
        exists as "Type.DISCRETE". 
    */
    public Discrete()
    {
      this(Integer.MIN_VALUE,Integer.MAX_VALUE,false,false,false,false);
    }

    /** The constructor allowing full specification of discrete type parameters. */
    public Discrete(double LWB, double UPB, 
                    boolean ckIsCyclic, boolean isCyclic, 
                    boolean ckIsOrdered, boolean isOrdered)
    { 
      super(LWB, UPB, ckIsCyclic, isCyclic);
      this.ckIsOrdered = ckIsOrdered;
      this.isOrdered = isOrdered;
    }

    /** If this discrete type is ordered, member types must also be ordered.
        If this discrete type is not ordered, member types must not be ordered.
        If this discrete type does not specify ordered or not ordered (i.e. this.ckOrdered = false)
        member types can be ordered or unordered.
        Other than these rules, member types must satisfy the hasMember method of the Scalar type. 
    */
    public boolean hasMember(Type t, Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Discrete.class.isInstance(t)) return false;

        if (!super.hasMember(t,h)) return false;

        if (ckIsOrdered)
        {
          if(!((Discrete)t).ckIsOrdered) return false;
          if(isOrdered != ((Discrete)t).isOrdered) return false;
        }
        return true;
      }
    }

    public int hasUPB()
    {
      if (UPB < Integer.MAX_VALUE - 1) return YES;
        else if (UPB == Integer.MAX_VALUE - 1) return MAYBE;
        else return NO;
    }

    public int hasLWB()
    {
      if (LWB > Integer.MIN_VALUE + 1) return YES;
        else if (LWB == Integer.MIN_VALUE + 1) return MAYBE;
        else return NO;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer("Discrete(");
      boolean c = false;

      if (LWB != Integer.MIN_VALUE)
      {
        sb.append("LWB=" + (int) LWB); 
        c = true; 
      }

      if (UPB != Integer.MAX_VALUE)
      {
        if (c) sb.append(",");
        sb.append("UPB=" + (int) UPB); 
        c = true; 
      }

      if (ckIsCyclic)
      {
        if (c) sb.append(",");
        sb.append("cyclic=" + isCyclic); 
        c = true; 
      }

      if (ckIsOrdered)
      {
        if (c) sb.append(",");
        sb.append("ordered=" + isOrdered); 
        c = true; 
      }

      sb.append(")");
      if (c) return sb.toString();
        else return "Discrete";
    }
  }

  /** A Symbolic Type has Values represented by Strings; the Values can be ordered (e.g. {bad,
      average, good} or unordered DNA={A,C,G,T}).<p>
      <strong>Note that we do not need a Value.Symbolic class;  Value.Discrete is
      sufficient.</strong>  Symbolic types are always bounded, with a lower bound of 0.
      @see Type.Scalar
      @see Type.Discrete
      @see Value.Discrete
  */
  public static class Symbolic extends Discrete
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8237073574719467121L;

	/** This array holds the string representations of symbolic values.  ids[x] is
        the string for the discrete value x.  If the string representation for a 
        particular value y is not known or defined, ids[y] can be null.  If no string
        representations are known or defined, ids itself can be null, and the field
        ckValues should be false.
    */
    public String[] ids = null; // id = null if we do not know or care about it.

    /** True indicates that the number of values is known (i.e. the upper bound
        is known), and perhaps some of the string representations are defined.
        Note that it is possible to define the number of values without defining any
        of the string representations.<BR>False indicates that neither the number of
        values nor any of the string representations are known or defined.  This represents
        the most general symbolic type. 
    */
    public boolean ckValues;

    /** To construct a symbolic type without defining the 
        number of symbols, or the symbols themselves.  Type.SYMBOLIC
        is created with this.
    */
    public Symbolic(boolean ckIsCyclic, boolean isCyclic, boolean ckIsOrdered, boolean isOrdered)
    {
      super(Integer.MIN_VALUE, Integer.MAX_VALUE, ckIsCyclic, isCyclic, ckIsOrdered, isOrdered);
      ckValues = false;
    }

    /** To construct a symbolic type where the number of values, and possibly
        some string representations are defined.  The size of the String array
        parameter "ids" defines the number of values (i.e. upper bound) for this type.
    */
    public Symbolic(boolean ckIsCyclic, boolean isCyclic, 
                    boolean ckIsOrdered, boolean isOrdered, String[] ids)
    { 
      super(0, (double)ids.length-1, ckIsCyclic, isCyclic, ckIsOrdered, isOrdered);
      ckValues = true;
      this.ids = ids;
    }

    /** A method that returns the String representation of a value.  */ 
    public String int2string(int i)
    { 
      return ids[i];
    }

    /** A method that returns the value of a Symbolic representation.  */ 
    public int string2int(String s)
    { 
      for(int i = 0; i < ids.length; i++)
      {
        if (ids[i] != null)
        {
          if (ids[i].equals(s)) 
          {
            return i;
          }
        }
      }
      throw new RuntimeException("Illegal value: " + s);
    }

    /** A method that returns the value of a Symbolic 
        representation as a Value.Discrete.
        @see Value.Discrete
    */ 
    public Value.Discrete string2value(String s)
    { 
      for(int i = 0; i < ids.length; i++)
      {
        if (ids[i] != null)
        {
          if (ids[i].equals(s)) 
          {
            return new Value.Discrete(this, i);
          }
        }
      }
      throw new RuntimeException("Illegal value.");
    }

    /** As well as a sub-type satisfying the rules for Discrete types, it must have the same string
        representation for every defined string representation of this type.  If this type does not
        define a string representation for a value, the sub-type may also leave this undefined, or
        define any string for this value.
    */
    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Symbolic.class.isInstance(t)) return false;

        if (!super.hasMember(t,h)) return false;

        if (ckValues)
        {
          Symbolic tmp = (Symbolic)t;

          if (!tmp.ckValues) return false;
 
          if (ids.length != tmp.ids.length) return false;

          for (int count = 0; count < ids.length; count++)
          {
            if (ids[count] != null)
            {
              if (!ids[count].equals(tmp.ids[count])) return false;
            }
          }
        }
        return true;
      }
    }

    public String toString()
    {
      if (ids == null) 
      {
        return "Symbolic";
      }
      else
      {
        StringBuffer sb = new StringBuffer("Symbolic(");
        for (int i = 0; i < ids.length; i++)
        {
          if (i != 0) sb.append(",");
          sb.append(ids[i]);
        }
        sb.append(")");
        return sb.toString();
      }
    }
  }

  /** A Continuous Scalar value with upper and lower bounds.  The bounds are inclusive.  Exclusive
      bounds can be represented by adding or subtracting Double.MIN_VALUE. Continuous types are
      inherently ordered.  
      @see Type.Scalar
      @see Value.Continuous
  */
  public static class Continuous extends Scalar
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7827646194841523330L;

	/** The default most general continuous type (Type.CONTINUOUS) which does not specify any bounds
        information, or cyclic information.
    */
    public Continuous()
    {
      this(-Double.MAX_VALUE,Double.MAX_VALUE,false,false);
    }

    public Continuous(double LWB, double UPB, boolean ckIsCyclic, boolean isCyclic)
    {
      super(LWB,UPB,ckIsCyclic,isCyclic);
    }

    public boolean hasMember(Type t, Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Continuous.class.isInstance(t)) return false;
        if (!super.hasMember(t,h)) return false;
        return true;
      }
    }

    public int hasUPB()
    {
      if (UPB < Double.MAX_VALUE - Double.MIN_VALUE) return YES;
        else if (UPB == Double.MAX_VALUE - Double.MIN_VALUE) return MAYBE;
        else return NO;
    }

    public int hasLWB()
    {
      if (LWB > -Double.MAX_VALUE + Double.MIN_VALUE) return YES;
        else if (LWB == -Double.MAX_VALUE + Double.MIN_VALUE) return MAYBE;
        else return NO;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer("Continuous(");
      boolean c = false;

      if (LWB != -Double.MAX_VALUE)
      {
        sb.append("LWB=" + LWB); 
        c = true; 
      }

      if (UPB != Double.MAX_VALUE)
      {
        if (c) sb.append(",");
        sb.append("UPB=" + UPB); 
        c = true; 
      }

      if (ckIsCyclic)
      {
        if (c) sb.append(",");
        sb.append("cyclic=" + isCyclic); 
        c = true; 
      }

      sb.append(")");
      if (c) return sb.toString();
        return "Continuous";
    }
  }

  /** Type.Structured is the Type of Structured, ie multivariate, data Values.  Each of its
      components has a Type (given by the cmpnts array), and can have a label (given by the labels
      array).  The labels are typically used to give each component more meaning when displayed by
      the system.  e.g. a color type could be a structured type with three continuous components
      labelled "Red", "Green", and "Blue".  The desktop could then display a value of this type as
      (say) (Red = 0.3, Green = 0.6, Blue = 0.0).  For type checking, we can specify either name
      equivalence or structural equivalence for each component seperately.
      @see Value.Structured
      @see Value.DefStructured
  */
  public static class Structured extends Type                    
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5031766494169947870L;

	/** This array defines the name of each component.  The array itself can be null (indicating
        that no component names are known or defined.  If the array is not null, it should be the
        same length as the Type array "cmpnts".  In this case, any of the elements of the array can
        be null (indicating there is no label for the corresponding component). 
    */
    public String[] labels;  // Label for component.  labels and labels[i] can be null.

    /** This array holds the type information for each of this type's components.  If ckCmpnts is
        false, this array (and the labels array) are meaningless.
    */
    public Type[] cmpnts;

    /** If an element of this array is true, it forces the type-checking to use name equivalence
        for the corresponding component.  False indicates that structural equivalence is sufficient
        (as will probably be the case more often than not). 
    */
    public boolean[] checkCmpntsNames;  // False = structural equivalence, true = 'name equivalence'.

    /** A flag to say whether the number of components is known.  For the most general 
        structured type Type.STRUCTURED (made with the default constructor) this value 
        is false.  For any more specific structured type it should be true. 
    */
    public boolean ckCmpnts;

    /** Creates a structured type without giving labels to the components. */
    public Structured(Type[] cmpnts, boolean[] checkCmpntsNames)
    { 
      this.cmpnts = cmpnts; 
      this.checkCmpntsNames = checkCmpntsNames;
      this.ckCmpnts = true;
    }

    /** Creates a structured type, fully specifying component types, labels, and type checking
        information.
    */
    public Structured(Type[] cmpnts, String[] labels, boolean[] checkCmpntsNames)
    { 
      this.cmpnts = cmpnts;
      this.labels = labels;
      this.checkCmpntsNames = checkCmpntsNames;
      this.ckCmpnts = true;
    }

    /** To define a structured type supplying component types and labels, and using
        structural equivalence (as opposed to name-equivalence) for the type checking of
        each component. 
    */
    public Structured(Type[] cmpnts, String[] labels)
    { 
      this.cmpnts = cmpnts;
      this.labels = labels;
      this.checkCmpntsNames = Value.DefStructured.booleanArray(false,cmpnts.length);
      this.ckCmpnts = true;
    }

    /** To define a structured type by supplying only the component types.  Structural
        equivalence (as opposed to name-equivalence) is used for the type checking of each
        copmonent.  No component labels are defined. 
    */
    public Structured(Type[] cmpnts)
    { 
      this.cmpnts = cmpnts;
      this.checkCmpntsNames = Value.DefStructured.booleanArray(false,cmpnts.length);
      this.ckCmpnts = true;
    }

    /** The default constructor.  This creates the most general structured type, which 
        already exists statically as Type.STRUCTURED.  This should not need to be called,
        as one can simply refer to Type.STRUCTURED instead.  Type.STRUCTURED does not
        define the number of components, and hence does not define their type information,
        labels, or method of type checking. 
    */
    public Structured()
    {
      ckCmpnts = false;
    }

    /** A convenience constructor that extracts the type information from each element
        of the array "val_cmpnts", and creates the corresponding structured type.  It
        essentially performs the same task as the Structured(Type[] cmpnts, String[] labels,
        boolean[] checkCmpntsNames) constructor, but uses the type information for each
        element of "val_cmpnts" instead of the Type array "cmpnts".  This is handy when
        creating a structured type to match an array of Values.
    */
    public Structured(Value[] val_cmpnts, String[] labels, boolean[] checkCmpntsNames)
    { 
      if (val_cmpnts != null)
      {
        ckCmpnts = true;
        this.cmpnts = new Type[val_cmpnts.length];
        for(int i=0; i < val_cmpnts.length; i++)
          this.cmpnts[i] = val_cmpnts[i].t;
      }
      else
      {
        this.ckCmpnts = false;
        this.cmpnts = null;
      }
      this.labels = labels;
      this.checkCmpntsNames = checkCmpntsNames;
    }
   
    /** If the number of components is not known, any structured type is a valid
        sub-type.<BR>
        If the number of components is known, it must be the same as the number of
        components for the candidate type, and each component of the candidate type
        must be a sub-type of the corresponding component of this type.
        If checkCmpntsNames[i] is true for some component i, then component i of the
        candidate type must actually be the same (pointer comparison) as component i
        for this type (name equivalence).  Otherwise the normal hasMember method is 
        used for that component (structural equivalence).
    */
    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Structured.class.isInstance(t)) return false;

        Structured tmp = (Structured)t;

        if (!ckCmpnts) return true;

        if (!tmp.ckCmpnts) return false;

        if (cmpnts.length != tmp.cmpnts.length) return false;

        for(int count = 0; count < cmpnts.length; count++)
        {
          if (!checkCmpntI(count, tmp.cmpnts[count], h)) return false;
        }

        return true;
      }
    }

    /** This method checks to see whether candidate Type t 
        is allowable for component i. 
    */
    public boolean checkCmpntI(int i, Type t,Hashtable h)
    {
      if (checkCmpntsNames[i])
      {
        // Since Type names are unique, we can just compare 
        // Type pointers rather than perform a string comparison 
        // of Type names.
        if (cmpnts[i] != t) return false;
      }

      return cmpnts[i].hasMember(t,h);
    }

    /** Convenience method to get the label for component idx.  Remember the labels array could be
        null so it is a good idea to use this method. The method returns null if the component
        does not have a label. */
    public String getLabel(int idx)
    {
      if (labels != null)
      {
        return labels[idx];
      }
      else return null;
    }

    public String toString()
    {
      if (ckCmpnts)
      {
        StringBuffer sb = new StringBuffer("(");
        for (int i = 0; i < cmpnts.length; i++)
        {
          if (i != 0) sb.append(",");
          if (labels != null && labels[i] != null) sb.append(labels[i] + "=");
          sb.append(cmpnts[i]);
        }
        sb.append(")");
        return sb.toString();
      }
      else return "(...)";
    }
  }

  /** Type.Union is for Values that can be from two or more Types as in
      Algol-68 or C unions, Pascal variant records, etc.
      Used with Structured Values and Types, and with Value.TRIV, Union allows
      linked lists, binary trees, and similar data-structures to be programmed.
  */
  public static class Union extends Type
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2970897012896685986L;

	/** An array of allowable types.  i.e. this type is types[0] U types[1] U ... U types[n].
       This array should not be null.
    */
    public Type types[];

    /** A value of true for element i forces a name-equivalence check to be done for types[i].
        That is, a type t is only a successful candidate for types[i] if t is EQUAL to types[i].
        A value of false for element i indicates structural equivalence should be used, so t would
        be a successful candidate for types[i] if it is (a sub-type of) types[i].
    */
    public boolean ckName[];

    public Union(Type types[], boolean ckName[])
    {
      this.types = types;
      this.ckName = ckName;
    }

    public Union(Type types[])
    {
      ckName = new boolean[types.length];
      for (int i = 0; i < ckName.length; i++)
        ckName[i] = false;
      this.types = types; 
    }

    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (t == this) return true;

        // Union A in Union B
        // Make sure all A cmpts are members of at least one of union B's cmpts.
        if (t instanceof Type.Union)
        {
          if ( ((Type.Union)t).types != null )  // INCORRECT SHOULD CHECK IF CARE.
          {
            boolean found = false;
            for (int i = 0; i < ((Type.Union)t).types.length; i++)
            {
              for (int j = 0; j < types.length; j++)
              {
                if (ckName[j])
                {
                  if(types[j] == ((Type.Union)t).types[i])       // Name equivalence.
                  {
                    found = true;
                    break;
                  }
                }
                else
                {
                  if (types[j].hasMember( ((Type.Union)t).types[i],h )) // Structural equivalence.
                  {
                    found = true; 
                    break;
                  }
                }
              } 
              if (!found) return false;  // If a single cmpnt of t is not a sub-type of 
                                         // at least one of this type's cmpnts, t is not 
                                         // a sub-type of this type.
            }
          } 
        }

        if (types != null)
        {
          for (int i = 0; i < types.length; i++)
          {
            if(ckName[i])
            {
              if (types[i] == t) return true;  // Name equivalence.
            }
            else
            {
              if (types[i].hasMember(t,h)) return true;  // Structural equivalence.
            } 
          }
        }

        return false;
      }
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < types.length; i++)
      {
        if (i != 0) sb.append(" | ");
        sb.append(types[i]);
      }
      return sb.toString();
    }
  }

  /** Type.Vector is the Type of Value.Vector.  Note that the elements
      of Vectors are homogeneous.
      @see Value.Vector
  */
  public static class Vector extends Type
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4566374440268476170L;

	/** The element Type of the Vector. */
    public Type elt;

    /** The index Type of the Vector. */
    public Type index;

    /** Determines whether the Vector is a sequence.  (Only meaningful if ckIsSequence is true.)*/
    public boolean isSequence;

    /** A value of true indicates we care or know whether the vector is a sequence. */
    public boolean ckIsSequence;

    /** True indicates that name-equivalence should be used when type-checking the element type of a
        candidate vector type t against the element type of this vector type.
    */
    public boolean checkEltName;

    /** True indicates that name-equivalence should be used when type-checking the index type of a 
        candidate vector type t against the index type of this vector type.
    */
    public boolean checkIndexName;

    /** Allows full specification of all type information. */
    public Vector(Type index, Type elt, boolean ckIsSequence, 
                  boolean isSequence, boolean checkEltName, 
                  boolean checkIndexName)
    { 
      this.index = index; 
      this.elt = elt;
      this.ckIsSequence = ckIsSequence;
      this.isSequence = isSequence;
      this.checkEltName = checkEltName;
      this.checkIndexName = checkIndexName;
      
      if ( elt == null ) { throw new RuntimeException("Type.Vector.elt not initialised??"); }
    }

    /** A constructor which creates a vector type with element type elt, index type Type.TYPE, no
        information regarding whether or not the vector is a sequence, and using structural
        equivalence to check the element and index types of candidate vector sub-types.
    */
    public Vector(Type elt)
    {
      this(Type.TYPE,elt,false,false,false,false);

      if ( elt == null ) { throw new RuntimeException("Type.Vector.elt not initialised??"); }
    } 

    /** A candidate sub-type t is only a sub-type if it is (or can be bound to) a vector type with
        element type being (a sub-type of) this element type, and index type being (a sub-type of)
        this index type.
    */ 
    public boolean hasMember(Type t,Hashtable h)
    {  
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Vector.class.isInstance(t)) return false;

        if (ckIsSequence && isSequence != ((Vector)t).isSequence) return false;

	try {
	    if (!elt.hasMember(((Vector)t).elt,h)) return false;
	}
	catch ( NullPointerException e ) {
	    System.out.println("this = " + this );
	    System.out.println("t = " + t);
	    System.out.println("elt = " + elt);
	    throw e;
	} 
        if (!index.hasMember(((Vector)t).index,h)) return false;

        return true;
      }
    }

    /** This method checks whether candidate type t is an allowable element type.  This is different
        to simply calling this.elt.hasMember(t) because if this.checkEltName is true then name
        equivalence should be used and no type other than this.elt is an allowable element type.
        Calling this.elt.hasMember(t) only does a structural equivalence check.
    */
    public boolean checkElt(Type t)
    {
      if (checkEltName)
      {
        if (elt != t) return false;
      }
      return elt.hasMember(t);
    }

    /** This method checks whether candidate type t is an allowable index type.  This is different
        to simply calling this.index.hasMember(t) because if this.checkIndexName is true then name
        equivalence should be used and no type other than this.index is an allowable index type.
        Calling this.index.hasMember(t) only does a structural equivalence check.
    */
    public boolean checkIndex(Type t)
    {
      if (checkIndexName)
      {
        if (index != t) return false;
      }
      return index.hasMember(t);
    }

    public String toString()
    {
      return "[" + elt + "]";
    }
  }

  /** The Type of a Function, i.e. parameter Type -> result Type.
      @see Value.Function
  */
  public static class Function extends Type                        
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2329915548447289634L;

	/** The parameter Type of the Function. */
    public Type param;

    /** The result Type of the Function. */
    public Type result;

    /** A flag to indicate whether or not name equivalence should be used when checking if a type is
        an allowable param type.  (True indicates this function ONLY accepts values of type param,
        false indicates this function accepts values of type param or any sub-type of param.
    */
    public boolean checkParamName;

    /** A flag to indicate whether or not name equivalence should be used when checking if a type is
        an allowable result type.  If this is true, only function types with result type EQUAL to
        this.result can be members of this type.  If this is false, function sub-types can have
        result types either equal to this.result, or a sub-type of this.result.
    */
    public boolean checkResultName;

    public Function(Type param, Type result, 
                    boolean checkParamName, boolean checkResultName)
    { 
      this.param = param;
      this.result = result; 
      this.checkParamName = checkParamName;
      this.checkResultName = checkResultName;
    }

    public Function(Type param, Type result)
    { 
      this(param,result,false,false);
    }

    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!(t instanceof Function)) return false;
        if (!checkParamType(((Type.Function)t).param)) return false;
        if (!checkResultType(((Type.Function)t).result)) return false;
        return true;
      }
    }

    /** This method checks whether type t is an allowable param Type.  This is different to simply
        calling this.param.hasMember(t) because if this.checkParamName is true then name equivalence
        should be used and no type other than this.param is an allowable param type. Calling
        this.param.hasMember(t) only does a structural equivalence check.
    */
    public boolean checkParamType(Type t)
    {
      if(checkParamName)
      {
        return (param == t);
      }
      else
      {
        return param.hasMember(t);
      }
    }

    /** This method checks whether type t is an allowable result Type.  This is different to simply
        calling this.result.hasMember(t) because if this.checkResultName is true then name
        equivalence should be used and no type other than this.result is an allowable result type.
        Calling this.result.hasMember(t) only does a structural equivalence check.
    */
    public boolean checkResultType(Type t)
    {
      if(checkResultName)
      {
        return (result == t);
      }
      else
      {
        return result.hasMember(t);
      }
    }

    public String toString()
    {
      return param + " -> " + result;
    }
  }

  /** The type of Value.Obj values.  These values represent Java Objects.  The type information for
      such values includes the name of the object's class, and whether Type.Obj types representing
      descendants of this class are sub-types of this type.  If className is null then hasMember 
      returns true for all Obj types.
  */
  public static class Obj extends Type
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6509990384611351443L;

	/** The Java Class name of the Java Object represented by values of this type. */
    public String className;

    /** A flag to indicate whether only Type.Obj types with exactly the same className can be
        sub-types of this type.  If this is false, any Type.Obj with a className which is a 
        descendent of this.className in the Java Class heirachy is a sub-type of this type.
    */
    public boolean ckClassName = false;

    public Obj(String className, boolean ckClassName)
    {
      this.className = className;
      this.ckClassName = ckClassName;
    }

    public Obj()
    {
      this.className = null;
    }

    public Obj(String className)
    {
      this.className = className;
    }

    /** If ckClassName is true, only Type.Obj types with the same className can be member types.
        If ckClassName is false, a member Type.Obj type can have className equal to this.className,
        or the name of a descendent class of this.className.  e.g. if this.className is
        "javax.swing.JComponent" and this.ckClassName is true, only Type.Obj types with this
        className can be members of this type.  If this.ckClassName is false however, Type.Obj types
        with a descendent className can also be members - for instance a className of
        "javax.swing.JPanel" is acceptable.
    */
    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Obj.class.isInstance(t)) return false;

        if (className != null)
        {
          if (((Obj)t).className == null) return false;

          try
          {
            Class x = Class.forName(((Obj)t).className);
            Class y = Class.forName(className);
            if (!y.isAssignableFrom(x)) return false;
          }
          catch (Exception e)
          {
            System.out.println("Cannot access class: " + className + ". " + e);
            return false;
          }
        }
        return true;
      }
    }

    public String toString()
    {
      if (className == null) return "Obj";
        else return "Obj(" + className + ")";
    }
  }


  /** The type of CDMS models.  The type information for a model includes:<BR>
      <UL>
      <LI>its data space - the type of the data x that the model is capable of giving probabilities
          for,
      <LI>its parameter space - the type of the model's parameters y,
      <LI>its shared space - the type of its 'input' values z, and
      <LI>its sufficient space - the type of the sufficient statistics for a vector of 'x' and 'z'.
      </UL>
      This concept of model encompasses "conditional" models, i.e. models which model the data x
      as a function of both the model parameters y and an additional "input" value z.  Such models
      include decision trees and neural networks.
      @see Value.Model
  */
  public static class Model extends Type
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5675143780988591029L;

	/** The data space of the model - the type of the data x that the model is capable of giving
        probabilities for.
    */
    public Type dataSpace;          // type of X.  Structural equivalence only.

    /** The parameter space of the model - the type of the model's parameters y. */
    public Type paramSpace;         // type of Y.

    /** The shared space of the model - the type of its 'input' values z.  This should be Type.TRIV
        if the model has no input values (e.g. Gaussian models, multinomial models etc).
    */
    public Type sharedSpace;

    /** The sufficient space of the model - the type of the sufficient statistics for a vector of
        'x' and 'z'.  Sufficient statistics are used for efficient calculation of the
         log-probability of a vector of data from the data space, and other such tasks.
    */
    public Type sufficientSpace;    // as above.

    public Model(Type dataSpace, Type paramSpace, Type sharedSpace, Type sufficientSpace)
    {
      this.dataSpace = dataSpace;
      this.paramSpace = paramSpace;
      this.sharedSpace = sharedSpace;
      this.sufficientSpace = sufficientSpace;
    }
 
    /** A sub-type must be a Type.Model type with dataSpace equal to (a sub-type of) this.dataSpace,
        paramSpace equal to (a sub-type of) this.paramSpace,sharedSpace equal to (a sub-type of)
        this.sharedSpace, and sufficientSpace equal to (a sub-type of) this.sufficientSpace.
    */
    public boolean hasMember(Type t,Hashtable h)
    {
      if (t instanceof Variable)
      {
        return ((Variable)t).unify(this, h);
      }
      else 
      {
        if (!Model.class.isInstance(t)) return false;

        if (!dataSpace.hasMember(((Model)t).dataSpace,h)) return false;
        if (!paramSpace.hasMember(((Model)t).paramSpace,h)) return false;
        if (!sharedSpace.hasMember(((Model)t).sharedSpace,h)) return false;
        if (!sufficientSpace.hasMember(((Model)t).sufficientSpace,h)) return false;

        return true;
      }
    }

    public String toString()
    {
      return "Model(" + dataSpace + "|" + paramSpace + "," + sharedSpace + ")";
    }
  }

  /** Create the major types. */
  public static final Type TYPE = new Type();
  public static final Type.Triv TRIV = new Type.Triv();
  public static final Type.Str STRING = new Type.Str();
  public static final Type.Symbolic BOOLEAN = 
    new Symbolic(true, false, true, false, new String[]{"true", "false"});
  public static final Type.Scalar SCALAR = new Scalar();
  public static final Type.Discrete DISCRETE = new Discrete();
  public static final Type.Continuous CONTINUOUS = new Continuous();
  public static final Type.Structured STRUCTURED = new Structured(); 
  public static final Type.Vector VECTOR = 
    new Vector(TYPE,TYPE,false,false,false,false);
  public static final Type.Function FUNCTION = 
    new Function(TYPE,TYPE,false,false);
  public static final Type.Obj OBJECT = new Obj();
  public static final Type.Model MODEL = new Model(TYPE,TYPE,TYPE,TYPE);
  public static final Type SYMBOLIC = new Symbolic(false, false, false, false);

  /** Derived types. */
  public static final Type.Continuous PROBABILITY = 
    new Type.Continuous(0, 1 + Double.MIN_VALUE, true, false);

  public static final Type.Symbolic DNA = 
    new Symbolic(true,false,true,false,new String[] { "a","c","g","t" });

  public static String byType(Type t)
  {
    return Environment.env.getName(t);
  }

  static 
  {
    Environment.env.add(getTypeName(TYPE.getClass()),"Standard",TYPE,"Built in TYPE type.");
    Environment.env.add(getTypeName(TRIV.getClass()),"Standard",TRIV,"Built in TRIV type.");
    Environment.env.add(getTypeName(STRING.getClass()),"Standard",STRING,"Built in STRING type.");
    Environment.env.add("Boolean","Standard",BOOLEAN,"Built in BOOLEAN type.");
    Environment.env.add(getTypeName(SCALAR.getClass()),"Standard",SCALAR,"Built in SCALAR type.");
    Environment.env.add(getTypeName(DISCRETE.getClass()),"Standard",DISCRETE,
                        "Built in DISCRETE type.");
    Environment.env.add(getTypeName(CONTINUOUS.getClass()),"Standard",CONTINUOUS,
                        "Built in CONTINUOUS type.");
    Environment.env.add(getTypeName(STRUCTURED.getClass()),"Standard",STRUCTURED,
                        "Built in STRUCTURED type.");
    Environment.env.add(getTypeName(VECTOR.getClass()),"Standard",VECTOR,"Built in VECTOR type.");
    Environment.env.add(getTypeName(FUNCTION.getClass()),"Standard",FUNCTION,
                        "Built in FUNCTION type.");
    Environment.env.add(getTypeName(OBJECT.getClass()),"Standard",OBJECT,"Built in OBJECT type.");
    Environment.env.add(getTypeName(MODEL.getClass()),"Standard",MODEL,"Built in MODEL type.");
    Environment.env.add(getTypeName(SYMBOLIC.getClass()),"Standard",SYMBOLIC,
                        "Built in SYMBOLIC type.");
    Environment.env.add("Probability","Standard",PROBABILITY,"Built in PROBABILITY type.");
    Environment.env.add("DNA","Standard",DNA,"Built in DNA type.");
   }



	/** 
	 * Wrapper class used to serialize the standard types. 
	 * Added by Rodney O'Donnell, 10/8/05
	 */
	private static class SerialType extends Type {
		/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 7116697165669397431L;
		final String typeName;
		public SerialType(String typeName) { this.typeName = typeName; }
	}

	/**
	 * The standard CDMS types (in Environment.env) must be serialised properly
	 *  to allow for the "==" method to be used after serialisation or RMI operations
	 *  have been performed. <br>
	 * Added by Rodney O'Donnell, 10/8/05
	 * see: http://java.sun.com/j2se/1.4.2/docs/api/java/io/Serializable.html <br>
	 */
	public Object writeReplace() throws ObjectStreamException
	{
		// If this type is a standard CDMS type wrap it up and send it
		// Wrapping it simply flags it as a "Standard" class for readResolve
		Object o = Environment.env.getObject(getTypeName(this.getClass()), "Standard");
		if (o == this) { return new SerialType(getTypeName(this.getClass())); }
		// If not, send it normally
		else { return this; }
	}

	/**
	 * The standard CDMS types (in Environment.env) must be serialised properly
	 *  to allow for the "==" method to be used after serialisation or RMI operations
	 *  have been performed. <br>
	 * Added by Rodney O'Donnell, 10/8/05
	 * see: http://java.sun.com/j2se/1.4.2/docs/api/java/io/Serializable.html <br>
	 */
	public Object readResolve() throws ObjectStreamException {
		if (this instanceof SerialType) {
			Object o = Environment.env.getObject(((SerialType)this).typeName, "Standard");
			if (o != null) return o;
			else { throw new RuntimeException("Unrecognised standard type: "+this); }
		}
		else return this;
	}
	
	
}

// End of file.

