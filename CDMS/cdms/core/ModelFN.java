//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: ModelFN.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.util.Random;

/** ModelFN contains the standard library of Functions for operations on Models.
*/
public class ModelFN extends Module.StaticFunctionModule
{
  public ModelFN()
  {
    super("Standard",Module.createStandardURL(ModelFN.class),ModelFN.class);
  }


  /** <code>Model ds ps sp suff -> [ds] -> [sp] -> suff</code> Returns the sufficient 
      statistics for the model parameters (sp) from the input and output data (sp and ds).
  */
  public static GetSufficient getSufficient = new GetSufficient();

  /** <code>Model ds ps sp suff -> [ds] -> [sp] -> suff</code>
      <p>
      Returns the sufficient statistics (suff) for the model parameters (sp) from the 
      input and output data (sp and ds).  For example 
      <code>getSufficient normal [1,2,3] [] evaluates to (sum=6,sumsqr=l4,n=3)</code>.
  */
  public static class GetSufficient extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3257650894584433225L;
	public static final Type.Variable dataSpace = new Type.Variable();
    public static final Type.Variable paramSpace = new Type.Variable();
    public static final Type.Variable sharedSpace = new Type.Variable();
    public static final Type.Variable sufficientSpace = new Type.Variable();

    public static final Type.Function TT = 
      new Type.Function(new Type.Model(dataSpace,paramSpace,sharedSpace,sufficientSpace),
                        GetSufficient2.TT);

    public GetSufficient()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new GetSufficient2((Value.Model) v);
    }
  }


  /** <code>[ds] -> [sp] -> suff</code>
      <p>
      @see ModelFN.GetSufficient
  */
  public static class GetSufficient2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1431842244083581369L;

	public static final Type.Function TT = 
      new Type.Function(new Type.Vector(GetSufficient.dataSpace),GetSufficient3.TT);

    protected Value.Model m;

    public GetSufficient2(Value.Model m)
    {
      super(TT);
      this.m = m;
    }

    public Value apply(Value v)
    {
      return new GetSufficient3(m,(Value.Vector) v);
    }
  }

  /** <code>[sp] -> suff</code>
      <p>
      @see ModelFN.GetSufficient
  */
  public static class GetSufficient3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8525637812865061944L;

	public static final Type.Function TT = 
      new Type.Function(new Type.Vector(GetSufficient.sharedSpace),GetSufficient.sufficientSpace);

    protected Value.Model m;
    protected Value.Vector ys;

    public GetSufficient3(Value.Model m, Value.Vector ys)
    {
      super(TT);
      this.m = m;
      this.ys = ys;
    }

    public Value apply(Value v)
    {
      return m.getSufficient(ys,(Value.Vector) v);
    }
  }


  /** <code>Model ds ps sp suff -> seed -> sp -> ps -> n -> [ds]</code> Generates data 
      from a model given the model, a random number generator seed, an element from the 
      shared space, an element from the parameter space and the number of elements required.
  */
  public static Generate generate = new Generate();

  /** <code>Model ds ps sp suff -> seed -> sp -> ps -> n -> [ds]</code>
      <p>
      Generates data from a model given the model, a random number generator seed, 
      an element from the shared space, an element from the parameter space and the 
      number of elements required.  For example 
      <code>generate Model.uniform 0 () (-1.0,1.0) 100</code> 
      will return a vector of 100 uniform random elements in the interval [-1,1].
  */
  public static class Generate extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3569222546316634884L;

	public static final Type.Variable dataSpace = new Type.Variable();
    public static final Type.Variable paramSpace = new Type.Variable();
    public static final Type.Variable sharedSpace = new Type.Variable();

    public static final Type.Function thisType = 
      new Type.Function(new Type.Model(dataSpace,paramSpace,sharedSpace,Type.TYPE),
                        Generate1.thisType);

    public Generate()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
//System.out.println("Model: " + ((Value.Model)v).toString());
      return new Generate1((Value.Model) v);
    }
  }

  /** <code>seed -> sp -> ps -> n -> [ds]</code>
      <p>
      @see ModelFN.Generate
  */
  public static class Generate1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1572337814413383080L;

	public static final Type.Function thisType = 
      new Type.Function(Type.DISCRETE,Generate2.thisType);

    private Value.Model m;

    public Generate1(Value.Model m)
    {
      super(thisType);
      this.m = m;
    }

    public Value apply(Value v)
    {
//System.out.println("Seed: " + ((Value.Discrete) v).getDiscrete());
      return new Generate2(m,((Value.Discrete) v).getDiscrete());
    }
  }


  /** <code>sp -> ps -> n -> [ds]</code>
      <p>
      @see ModelFN.Generate
  */
  public static class Generate2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3485944387438594988L;

	public static final Type.Function thisType = 
      new Type.Function(Generate.sharedSpace,Generate3.thisType);

    private Value.Model m;
    private int seed;

    public Generate2(Value.Model m, int seed)
    {
      super(thisType);
      this.m = m;
      this.seed = seed;
    }

    public Value apply(Value v)
    {
//System.out.println("Shared: " + v);
      return new Generate3(m,seed,v);
    }
  }

  /** <code>ps -> n -> [ds]</code>
      <p>
      @see ModelFN.Generate
  */
  public static class Generate3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8030596484733284207L;

	public static final Type.Function thisType = 
      new Type.Function(Generate.paramSpace,Generate4.thisType);

    private Value.Model m;
    private int seed;
    private Value z;

    public Generate3(Value.Model m, int seed, Value z)
    {
      super(thisType);
      this.m = m;
      this.seed = seed;
      this.z = z;
    }

    public Value apply(Value v)
    {
//System.out.println("Params: " + v);
      return new Generate4(m,seed,z,v);
    }
  }

  /** <code>n -> [ds]</code>
      <p>
      @see ModelFN.Generate
  */
  public static class Generate4 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 337948873285650976L;

	public static final Type.Vector thisResultType = new Type.Vector(Generate.dataSpace);
    public static final Type.Function thisType = new Type.Function(Type.DISCRETE,thisResultType);

    private Value.Model m;
    private int seed;
    private Value z;
    private Value y;

    public Generate4(Value.Model m, int seed, Value z, Value y)
    {
      super(thisType);
      this.m = m;
      this.seed = seed;
      this.z = z;
      this.y = y;
    }

    public Value apply(Value v)
    {
//System.out.println("N: " + ((Value.Discrete)v).getDiscrete());
      return m.generate(new Random(seed),((Value.Discrete) v).getDiscrete(), y,z);
    }
  }

  /** <code>Model ds ps sp suff -> seed -> [sp] -> ps -> [ds]</code> Generates data 
      from a model given the model, a random number generator seed, an a vector with 
      elements from the shared space, and an element from the parameter space.
  */
  public static GenerateV generatev = new GenerateV();

  /** <code>Model ds ps sp suff -> seed -> [sp] -> ps -> [ds]</code>
      <p>
      Generates data from a model given the model, a random number generator seed, 
      an a vector with elements from the shared space, and an element from the parameter space.
      The length of the resulting vector matches the length of the vector of shared space elements.
  */
  public static class GenerateV extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8477118729533240735L;
	public static final Type.Variable dataSpace = new Type.Variable();
    public static final Type.Variable paramSpace = new Type.Variable();
    public static final Type.Variable sharedSpace = new Type.Variable();

    public static final Type.Function thisType = 
      new Type.Function(new Type.Model(dataSpace,paramSpace,sharedSpace,Type.TYPE),
                        Generate1V.thisType);

    public GenerateV()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      return new Generate1V((Value.Model) v);
    }
  }

  /** <code>seed -> [sp] -> ps -> [ds]</code>
      <p>
      @see ModelFN.GenerateV
  */
  public static class Generate1V extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 340161138148432330L;

	public static final Type.Function thisType = 
      new Type.Function(Type.DISCRETE,Generate2V.thisType);

    private Value.Model m;

    public Generate1V(Value.Model m)
    {
      super(thisType);
      this.m = m;
    }

    public Value apply(Value v)
    {
      return new Generate2V(m,((Value.Discrete) v).getDiscrete());
    }
  }


  /** <code>[sp] -> ps -> [ds]</code>
      <p>
      @see ModelFN.GenerateV
  */
  public static class Generate2V extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8445888760882827297L;

	public static final Type.Function thisType = 
      new Type.Function(new Type.Vector(new Type.Variable(), 
      GenerateV.sharedSpace, false, false, false, false),Generate3V.thisType);

    private Value.Model m;
    private int seed;

    public Generate2V(Value.Model m, int seed)
    {
      super(thisType);
      this.m = m;
      this.seed = seed;
    }

    public Value apply(Value v)
    {
      return new Generate3V(m,seed,v);
    }
  }

  /** <code>ps -> [ds]</code>
      <p>
      @see ModelFN.GenerateV
  */
  public static class Generate3V extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7184493826064324635L;

	public static final Type.Function thisType = 
      new Type.Function(GenerateV.paramSpace,Type.VECTOR);

    private Value.Model m;
    private int seed;
    private Value z;

    public Generate3V(Value.Model m, int seed, Value z)
    {
      super(thisType);
      this.m = m;
      this.seed = seed;
      this.z = z;
    }

    public Value apply(Value v)
    {
      return m.generate(new Random(seed), v, (Value.Vector)z);
    }
  }

  public static final Predict predict = new Predict();

  /** <code>Model ds ps sp suf -> [sp] -> ps -> [ds]</code>  This function takes a model, followed
      by a vector "v1" of input values (shared space of the model), followed by parameters "p" for 
      the model, and returns a vector "v2" of data space values, where v2[i] is predicted using the
      parameters p, and input value v1[i].
  */
  public static class Predict extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6723355767205916169L;
	public static Type.Function TT = new Type.Function(Type.MODEL, Predict2.TT, false, false);

    public Predict()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Predict2((Value.Model)v);
    }
  }

  public static class Predict2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -105333980651042876L;

	public static Type.Function TT = new Type.Function(Type.VECTOR, Predict3.TT, false, false);

    private Value.Model m;
 
    public Predict2(Value.Model m)
    {
      super(TT);
      this.m = m;
    }

    public Value apply(Value v)
    {
      return new Predict3(m, (Value.Vector)v);
    }
  }

  public static class Predict3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7515445036006607005L;

	public static Type.Function TT = 
      new Type.Function(new Type.Variable(), Type.VECTOR, false, false);

    private Value.Model m;
    private Value.Vector input;

    public Predict3(Value.Model m, Value.Vector input)
    {
      super(TT);
      this.m = m;
      this.input = input;
    }

    public Value apply(Value v)
    {
      return m.predict(v, input);
    }
  }
  

    /**
     * Model -> params -> z -> x -> continuous
     */
  public static final Value.Function logP = new LogP();

    /**
     * Model -> params -> z -> x -> continuous
     */  
  public static class LogP extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 3849790776290594111L;
	public static Type.Function TT = new Type.Function(Type.MODEL, LogP2.TT, false, false);
    
    public LogP()
    {
      super(TT);
    }
    
    public Value apply(Value v)
    {
      return new LogP2((Value.Model)v);
    }
  }
  
  public static class LogP2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5032020181267210086L;

	public static Type.Function TT = 
      new Type.Function(new Type.Variable(), LogP3.TT, false, false);
    
    private Value.Model model;
    
    public LogP2(Value.Model model)
    {
      super(TT);    
      this.model = model;
    }
    
    public Value apply(Value v)
    {
      return new LogP3(model, v);
    }
  }
  
  public static class LogP3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 803036615167722125L;

	public static Type.Function TT = new Type.Function(Type.VECTOR, LogP4.TT, false, false);

    private Value.Model model;
    private Value params;
        
    public LogP3(Value.Model model, Value params)
    {
      super(TT);    
      this.model = model;
      this.params = params;
    }
    
    public Value apply(Value v)
    {
      return new LogP4(model,params,(Value.Vector)v);
    }
  }

  public static class LogP4 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2209101827356974188L;

	public static Type.Function TT = new Type.Function(Type.VECTOR, Type.CONTINUOUS, false, false);

    private Value.Model model;
    private Value params;
    private Value.Vector z;
        
    public LogP4(Value.Model model, Value params, Value.Vector z)
    {
      super(TT);    
      this.model = model;
      this.params = params;
      this.z = z;
    }
    
    public Value apply(Value v)
    {
      return new Value.Continuous(model.logP((Value.Vector)v,params,z));
    }
  }
}
