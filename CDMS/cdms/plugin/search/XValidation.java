package cdms.plugin.search;

import cdms.core.*;


/** XValidation is a "curried" function - it takes firstly a seed, returning
    a function (XValidation1) which expects the number of folds,
    and returns a function (XValidation2) that expects a vector of data.  The
    XValidation2 function returns a vector with "folds" elements.  Each element
    is itself a structure of two vectors of data.  The first component is the 
    training set, and the second is its complement - the corresponding testing
    set.  All testing sets represent mutually exclusive subsets of the original 
    data vector. <br>
    The XValidation function randomly selects elements from the original data
    vector to make up each component vector.  It does NOT simply take a sub-range
    for each component vector, as the original data may not be randomly ordered.
    We would like each component vector to be a decent representation of the
    original vector.
*/
public class XValidation extends Value.Function
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3028158031272479240L;
public static XValidation xValidation = new XValidation();

  public XValidation()
  {
    super(new Type.Function(Type.DISCRETE, XValidation1.TT));
  }

  public Value apply(Value v)
  {
    return new XValidation1(((Value.Discrete)v).getDiscrete());
  }

  public static class XValidation1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7803154511539543250L;
	public static Type.Function TT = new Type.Function(Type.DISCRETE, XValidation2.TT);
    private int seed;

    public XValidation1(int seed)
    {
      super(TT);
      this.seed = seed;
    }

    public Value apply(Value folds)
    {
      return new XValidation2(seed, ((Value.Scalar)folds).getDiscrete());
    }
  }

  public static class XValidation2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4379019112052936951L;
	private static Type.Vector tv1 = new Type.Vector(new Type.Variable());
    public static Type.Function TT = new Type.Function(tv1, 
                                                       new Type.Structured(new Type[]{new Type.Vector(tv1), new Type.Vector(tv1)},
                                                       new String[]{"training","testing"},
                                                       new boolean[]{false, false}));
    private int seed;
    private int folds;

    public XValidation2(int seed, int folds)
    {
      super(TT);
      this.seed = seed;
      this.folds = folds;
    }

    public Value apply(Value v)
    {
      int[] limit = new int[folds];
      int[] counts = new int[folds];
      java.util.Random r = new java.util.Random(seed);
      Value.Vector allData = (Value.Vector)v;
      java.util.Vector[] newVector = new java.util.Vector[folds];
      java.util.Vector[] newTrainVector = new java.util.Vector[folds];
      Value.Vector train;// = new Value.Vector[folds];
      Value.Vector test;// = new Value.Vector[folds];
      Value.Structured[] vs = new Value.Structured[folds];
      int count, membership, count2, sum = 0;
      int base_limit = allData.length()/folds;      // Assign an equal number of data to each group, with some remainder data left over.
      for(count = 0; count < folds; count++)        //   0 <= remainder <= (data.length - 1)
      {
        newVector[count] = new java.util.Vector();
        newTrainVector[count] = new java.util.Vector();
        counts[count] = 0;
        limit[count] = base_limit;
        sum += base_limit;
      }
      int remainder = allData.length() - sum;       // Spread out remaining data evenly over the first "remainder" number of groups.
      for(count = 0; count < remainder; count++)
      {
        limit[count]++;
      }
      
//      newVector[count] = new java.util.Vector();
//      newTrainVector[count] = new java.util.Vector();
//      counts[count] = 0;
//      limit[folds-1] = allData.length() - sum;


System.out.println("#data: " + allData.length());
for(count = 0; count < folds; count++)
{
System.out.println("Limit[" + count + "]: " + limit[count]);
}


      for(count = 0; count < allData.length(); count++)
      {

//System.out.print("Assigning datum #" + count + "...");
        membership = r.nextInt(folds);
        while(counts[membership] == limit[membership]) membership = r.nextInt(folds);
        newVector[membership].add(allData.elt(count));
        counts[membership]++;
        for(count2 = 0; count2 < folds; count2++)
        {
          if(count2 != membership)
          { 
            newTrainVector[count2].add(allData.elt(count));
          } 
        }
//System.out.println(" to " + membership);
      }
System.out.println("Building vectors...");
      Value[] tmp1;
      Value[] tmp2;
      for(count = 0; count < folds; count++)
      {
        tmp1 = new Value[newTrainVector[count].size()];
        for(count2 = 0; count2 < newTrainVector[count].size(); count2++)
        {
          tmp1[count2] = (Value)newTrainVector[count].elementAt(count2);
        }
        tmp2 = new Value[newVector[count].size()];
        for(count2 = 0; count2 < newVector[count].size(); count2++)
        {
          tmp2[count2] = (Value)newVector[count].elementAt(count2);
        }
        train = new VectorFN.FatVector(tmp1);
        test = new VectorFN.FatVector(tmp2);
        vs[count] = new Value.DefStructured(new Value[]{train, test});
      }
System.out.println("Done.");
      return new VectorFN.FatVector(vs);
    }
  }
}
