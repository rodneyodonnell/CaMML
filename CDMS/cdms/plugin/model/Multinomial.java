//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Multinomial.java
// Author: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.model;

import java.awt.*;
import java.util.Random;

import javax.swing.*;
import cdms.core.*;

/** Multinomial model. */
public class Multinomial extends Value.Model
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5550094524155119504L;
public int lwb, upb;

/** Ideally this would be polymorphic - the Type of the resulting Model is dependent on the 
    parameters to the function.  At compile time we fill in as many of the `blanks' as possible, 
    and flesh it out at run time, inside the "apply" method.
*/
  public static class MultinomialCreator extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2714000151829647667L;

	private static Type.Structured paramType = new Type.Structured(new Type[]{Type.DISCRETE, Type.DISCRETE}, 
                                                                   new String[]{"Lower Bound","Upper Bound"}, 
                                                                   new boolean[]{false, false});

    private static Type.Model resultType = new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED);

    public MultinomialCreator()
    {
      super(new Type.Function(paramType, resultType, false, false));      
    }


      final static int numPrecomputedModels = 50;
      /** precomputedModel is a set of models with lower bound and upper bound n, this saves creating a new model every time this function is called. */
      protected static Multinomial precomputedModel[] = new Multinomial[numPrecomputedModels];

    public Value apply(Value v)
    {
	Value.Structured s = (Value.Structured)v;
	int lwb = ((Value.Scalar)s.cmpnt(0)).getDiscrete();
	int upb = ((Value.Scalar)s.cmpnt(1)).getDiscrete();


	Multinomial mn;

	if (lwb == 0 && upb < numPrecomputedModels ) {
	    if (precomputedModel[upb] == null) {
		precomputedModel[upb] = new Multinomial(lwb, upb);
	    }
	    mn = precomputedModel[upb];
	}
	else {
	    mn = new Multinomial(lwb, upb);
	}
	return mn;
    }
  }

  public Multinomial(int lwb, int upb)
  {
    super(new Type.Model(makeDataSpace(lwb, upb), makeParamSpace(lwb, upb), Type.TRIV, makeSufficientSpace(lwb, upb)));
    this.lwb = lwb;
    this.upb = upb;
  }

  public Multinomial(Type.Discrete dataSpace)
  {
    super(new Type.Model(dataSpace, makeParamSpace((int)dataSpace.LWB, (int)dataSpace.UPB), Type.TRIV, makeSufficientSpace((int)dataSpace.LWB, (int)dataSpace.UPB)));
    lwb = (int)dataSpace.LWB;
    upb = (int)dataSpace.UPB;
  }

  public static Type.Discrete makeDataSpace(int lwb, int upb)
  {
    return new Type.Discrete(lwb, upb, false, false, false, false);
  }

  public static Type.Structured makeParamSpace(int lwb, int upb)
  {
    Type[] cmpnts = new Type[upb - lwb + 1];
    String[] labels = new String[upb - lwb + 1];
    boolean[] falseArray = new boolean[upb - lwb + 1];
    int count;
    for(count = 0; count < upb - lwb + 1; count++)
    {
      cmpnts[count] = Type.PROBABILITY;
      labels[count] = "Pr["+(count + lwb)+"]";
      falseArray[count] = false;
    }
    return new Type.Structured(cmpnts, labels, falseArray);
  }

  public static Type.Structured makeSufficientSpace(int lwb, int upb)
  {
    Type[] cmpnts = new Type[upb - lwb + 1];
    String[] labels = new String[upb - lwb + 1];
    boolean[] falseArray = new boolean[upb - lwb + 1];
    int count;
    for(count = 0; count < upb - lwb + 1; count++)
    {
      cmpnts[count] = Type.SCALAR;
      labels[count] = "x["+(count + lwb)+"]";
      falseArray[count] = false;
    }
    return new Type.Structured(cmpnts, labels, falseArray);
  }

  // logP(X|Y,Z)
  public double logP(Value x, Value y, Value z)
  {
    return java.lang.Math.log((((Value.Structured)y).doubleCmpnt(((Value.Scalar)x).getDiscrete() - lwb)));
  }

  // logP(X|Y,Z) where v = (X,Y,Z)
  public double logP(Value.Structured v)
  {
    return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
  }

  // Returns a vector of elements from the data-space, where the ith element is taken from p(.|Y,Z_i). 
  public Value.Vector generate(Random rand, Value y, Value.Vector z)
  {
    return generate(rand, z.length(), y, Value.TRIV);
  }

  // Returns a vector of elements from the data-space conditional on Y,Z.
  public Value.Vector generate(Random rand, int n, Value y, Value z)
  {
    double[] probs = new double[upb - lwb + 1];
    int count, count2;
    for(count = 0; count < probs.length; count++)
    {
      probs[count] = ((Value.Scalar)((Value.Structured)y).cmpnt(count)).getContinuous();
    }
    int[] array = new int[n];
    double x, sum;
    for(count = 0; count < n; count++)
    {
      sum = probs[0];
      x = rand.nextDouble();
      for(count2 = 1; (count2 < probs.length) && (x > sum); count2++)
      {
        sum += probs[count2];
      }
      array[count] = count2 - 1 + lwb;
    }
    return new VectorFN.DiscreteVector(new Type.Vector(Type.TYPE, ((Type.Model)t).dataSpace, false, false, false, false), array);
  }

  public Value predict(Value y, Value z)
  {
    double tmp;
    double bestProb = 0;
    int bestVal = 0;
    int count;
    for(count = 0; count < ((Value.Structured)y).length(); count++)
    {
      tmp = ((Value.Scalar)((Value.Structured)y).cmpnt(count)).getContinuous();
      if(tmp > bestProb)
      {
        bestProb = tmp;
        bestVal = count;
      }
    }
    return new Value.Discrete((Type.Discrete)((Type.Model)t).dataSpace, bestVal + lwb);
  }

  public Value.Vector predict(Value y, Value.Vector z)
  {
    return new VectorFN.UniformVector(z.length(), predict(y, Value.TRIV));
  }

  // Returns sufficient statistics for this model of the data.
  public Value getSufficient(Value.Vector x, Value.Vector z)
  {
// //       if ( x instanceof VectorFN.FatVector ) {  // ??? rempve this.
// // 	  System.out.println("Inefficient getSufficient() : " + this.getClass());
// // 	  RuntimeException e = new RuntimeException("FatVector used");
// // 	  e.printStackTrace();
// // 	  System.exit(0);
// //       }
//     double[] tallies = new double[upb - lwb + 1];
//     Value.Continuous[] vals= new Value.Continuous[upb - lwb + 1];
//     int count;
//     for(count = 0; count < x.length(); count++)
//     {
//       tallies[x.intAt(count) - lwb] += x.weight(count);
//     }
//     for(count = 0; count < tallies.length; count++)
//     {
//       vals[count] = new Value.Continuous(tallies[count]);
//     }
// //     timesCalled ++;
// //     if ( x.length() < stats.length ) {
// // 	stats[x.length()]++;
// //     }
// //     else {
// // 	stats[stats.length-1] ++;
// //     }
// //     if ( timesCalled == 1 ) {
// // 	for ( int i = 0; i < stats.length-1; i++ ) {
// // 	    System.out.print("" + i + "\t");
// // 	}
// // 	System.out.println(" > " + (stats.length - 1) + "\t");
// //     }
// //     if ( timesCalled % 10000 == 0 ) {
// // 	for ( int i = 0; i < stats.length; i++ ) {
// // 	    System.out.print("" + stats[i] + "\t");
// // 	}
// // 	System.out.println();
// //     }
//     return new Value.DefStructured(vals);

     double[] tallies = new double[upb - lwb + 1];
     int len = x.length();
     for(int i = 0; i < len; i++) {
	 int index = x.intAt(i) - lwb;
	 double weight = x.weight(i);
	 tallies[index] += weight;
     }
     return new FastContinuousStructure( tallies );
  }

//     static int timesCalled = 0;
//     static int[] stats = new int[21];

  // logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2)... where s is a sufficient statistic of X for Y.
  public double logPSufficient(Value s, Value y)
  {
	  Value.Structured stats = (Value.Structured)s;
	  Value.Structured params = (Value.Structured)y;
    double sum = 0;
    int count;
    for(count = 0; count < upb - lwb + 1; count++)
    {
	double tally = stats.doubleCmpnt(count);
	if (tally != 0) {
	    double prob = params.doubleCmpnt(count);
	    sum += tally * java.lang.Math.log(prob);
	}
    }
    return sum;
  }

    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
	return logPSufficient( getSufficient(x,z), y);
    }  
    
  public Component displayParams(Value y)
  {
    java.text.DecimalFormat df = new java.text.DecimalFormat();
    df.setMaximumFractionDigits(3);
    Value.Structured ys = (Value.Structured)y;
    double[] probs = new double[ys.length()];
    String[] probStrings = new String[probs.length];
    String[] labels = new String[probs.length];
    double sum = 0;
    int count;
    for(count = 0; count < probs.length; count++)
    {
      probs[count] = ys.doubleCmpnt(count);
      sum += probs[count];
    }
    for(count = 0; count < probs.length; count++)
    {
      probs[count] /= sum;
      probStrings[count] = df.format(probs[count]);
      labels[count] = "Pr("+count+")";
    }
    if(((Type.Model)t).dataSpace instanceof Type.Symbolic)
    {
      Type.Symbolic ts = (Type.Symbolic)((Type.Model)t).dataSpace;
      if(ts.ids != null)
      {
        for(count = 0; count < probs.length; count++)
        {
          if(ts.ids[count] != null)
          {
            labels[count] = "Pr(" + ts.ids[count] + ")";
          }
        }
      }
    }
    return new ParamComponent(labels, probStrings, probs);
  }
  
  private static class ParamComponent extends JComponent
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6196223320213411343L;
	private String[] labels;
    private String[] probStrings;
    private double[] probs;
    private int maxLabelLength = 0;
    private int maxBar = 0;
    
    public ParamComponent(String[] labels, String[] probStrings, double[] probs)
    {
      this.labels = labels;
      this.probStrings = probStrings;
      this.probs = probs;
    }
    
    public Dimension getPreferredSize()
    {
      int count;
      for(count = 0; count < labels.length; count++)
      {
        maxLabelLength = java.lang.Math.max(maxLabelLength, getGraphics().getFontMetrics().stringWidth(labels[count]));
      }
      maxBar = getGraphics().getFontMetrics().stringWidth("0.000");
      return new Dimension(20 + maxLabelLength + 2 * maxBar, 10 + getGraphics().getFontMetrics().getHeight() * labels.length);
    }
    
    public void paint(Graphics g)
    {
      getPreferredSize(); // - makes sure all the fields are set up properly.
      Graphics2D gr = (Graphics2D)g;
      gr.setColor(Color.white);
      gr.fillRect(0,0,getWidth(),getHeight());
      int count;
      for(count = 0; count < labels.length; count++)
      {
        gr.setColor(Color.gray);
        gr.fillRect(10 + maxLabelLength, 5 + count * gr.getFontMetrics().getHeight(), maxBar, gr.getFontMetrics().getHeight() -1);
        gr.setColor(Color.green);
        gr.fillRect(10 + maxLabelLength, 5 + count * gr.getFontMetrics().getHeight(), (int)(probs[count] * maxBar), gr.getFontMetrics().getHeight() -1);
      }
      gr.setColor(Color.black);
      for(count = 0; count < labels.length; count++)
      {
        gr.drawString(labels[count], 5, 5 + (int)((count+0.75) * gr.getFontMetrics().getHeight()));
        gr.drawString(probStrings[count], getWidth() - 5 - maxBar, 5 + (int)((count+0.75) * gr.getFontMetrics().getHeight()));
        gr.drawRect(10 + maxLabelLength, 5 + count * gr.getFontMetrics().getHeight(), maxBar/*(int)(probs[count] * maxBar)*/, gr.getFontMetrics().getHeight() -1);
      }
      gr.drawRect(0,0,getWidth()-1,getHeight()-1);
    }
  }

  public String toString()
  {
    return "Multinomial Model (" + lwb + "," + upb + ")";
  }



    /////////////////////////////////////////////////////////////////////////////////////////////
    //////////  Structure Implementation taken from camml.CDMSUtils.library.StructureFN /////////
    /////////////////////////////////////////////////////////////////////////////////////////////


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
		private static final long serialVersionUID = -733817335191434376L;
	protected double data[];
	
	/** Make the type of the structure.  Type is a structure of continuous of a given length */
	protected static Type.Structured makeType( int length )
	{
	    if ( (length < 20) && (precomputedType[length] != null) ) {
		return precomputedType[length];
	    }

	    Type.Continuous typeArray[] = new Type.Continuous[ length ];
	    for (int i = 0; i < length; i++)
		typeArray[i] = Type.CONTINUOUS;
	    Type.Structured retVal = new Type.Structured(typeArray);
	    if ( length < 20 ) {
		precomputedType[length] = retVal;
	    }
	    return retVal;
	}
	protected static Type.Structured[] precomputedType = new Type.Structured[20];

	public FastContinuousStructure( double data[] )
	{
	    super( makeType(data.length) );
	    this.data = data;
	}
		
	public Value cmpnt(int i)
	{
	    return new Value.Continuous( data[i] );
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

}
