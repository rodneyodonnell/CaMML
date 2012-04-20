/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lucas Hope, Lloyd Allison, Kevin Korb
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
// CPT model class
//

// File: CPT.java
// Author: {rodo,lhope}@csse.monash.edu.au

package camml.core.models.cpt;

import camml.core.library.SelectedVector;
import camml.core.models.ModelLearner.GetNumParams;
import camml.core.models.multinomial.MultinomialLearner.Multinomial2;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;

import java.util.Random;

/**
 * CPT model.
 * A CPT is a lookup table for P(X|Pa(X)), X may be any form of value (often discrete) and all
 * parent variables are bounded discrete.  All parent combinations are modeled as being independent
 * and the CPT implementation could be described as a "splitter".  Data is split according to it's
 * parent combination and the "real modeling" is done by a different model (often Multinomial).
 * <p/>
 * x is any Type, This is the dependant variable.
 * y is a Vector of structures. [(model,params)] where model is the subModel (oftem Multinomial),
 * and params are the parameters associated with the subModel.
 * z is a Type.Structured containing Type.Discrete values, These are the parent variables.
 * s is simply a structure containing x and z.  ie. (x,z)
 */
public class CPT extends Value.Model
        implements GetNumParams {
    /**
     * Serial ID required to evolve class while maintaining serialisation compatibility.
     */
    private static final long serialVersionUID = -3908621250487297651L;

    /**
     * Maximum number of cells any CPT can use
     */
    // 64000 is default in Wallace CaMML
    protected static final int defaultMaxCells = 64000;

    /**
     * Array of Lower bounds of z[i]
     */
    protected final int[] parentlwbs;

    /**
     * Array of Upper bounds of z[i]
     */
    protected final int[] parentupbs;

    /**
     * this stores the multipliers used in decodeParents
     */
    private final int[] multipliers;

    /**
     * number of combinations of parent variables possible.
     */
    protected final int numCombinations;

    /**
     * What sort of model the child node is.  Usually a Multinomial
     */
    protected final Value.Model childModel;

    /**
     * Accesor for numCombinations
     */
    public int getNumCombinations() {
        return numCombinations;
    }

    /**
     * return the number of parents this variable has
     */
    public int getNumParents() {
        return multipliers.length;
    }

    /**
     * Constructor
     */
    public CPT(Value.Model childModel, int[] parentlwbs, int[] parentupbs) {
        this(childModel, parentlwbs, parentupbs, defaultMaxCells);
    }

    /**
     * Initialise the CPT. This checks that parentlwbs and parentupbs have the same length,
     * and that logProbs is exactly the right size. Throws an exception if the above is not
     * the case.
     */
    public CPT(Value.Model childModel, int[] parentlwbs, int[] parentupbs, int maxCells) {

        super(new Type.Model(((Type.Model) childModel.t).dataSpace,
                new Type.Vector(Type.STRUCTURED),
                makeSharedSpace(parentlwbs, parentupbs),
                makeSufficientSpace(new Type.Variable(),
                        makeSharedSpace(parentlwbs, parentupbs))));

        if (parentlwbs.length != parentupbs.length) {
            throw new IllegalArgumentException("Parent bounds do not match!");
        }

        // check that parent bounds make sense.
        for (int i = 0; i < parentlwbs.length; i++) {
            if (parentlwbs[i] > parentupbs[i]) {
                throw new IllegalArgumentException("Parent variable " + i +
                        " must have at least one value!");
            }
        }

        // Use a 64 bit long to calculate numCombinations.
        // We use a long to ensure we don't accidentilly wrap over the 32 bit boundry.
        long numCombinations = 1;
        for (int i = 0; i < parentlwbs.length; i++) {
            numCombinations *= parentupbs[i] - parentlwbs[i] + 1;
        }

        // Ensure maxCells is not exceeded.
        Type.Discrete xType = (Type.Discrete) ((Type.Model) childModel.t).dataSpace;
        int arity = (int) xType.UPB - (int) xType.LWB + 1;
        if (maxCells != -1 && numCombinations * arity > maxCells) {
            throw new ExcessiveCombinationsException("Too many combinations in CPT : " +
                    numCombinations * arity);
        }

        this.numCombinations = (int) numCombinations;

        // copy the parent bounds
        this.parentlwbs = (int[]) parentlwbs.clone();
        this.parentupbs = (int[]) parentupbs.clone();
        this.childModel = childModel;

        // Create array of multipliers
        this.multipliers = makeMultipliers();
    }

    /**
     * The parameterspace is a structure of the different ranges of the parent variables
     */
    public static Type.Structured makeSharedSpace(int[] lwbs, int[] upbs) {
        int len = lwbs.length;                   // assuming upbs.length = lwbs.length
        Type[] cmpnts = new Type[len];
        String[] labels = new String[len];
        boolean[] falseArray = new boolean[len]; // note java sets all booleans to false initially

        for (int i = 0; i < len; i++) {
            cmpnts[i] = new Type.Discrete(lwbs[i], upbs[i], false, false, false, false);
            labels[i] = "Parent " + i;
        }
        return new Type.Structured(cmpnts, labels, falseArray);
    }

    /**
     * The sufficient stats summarise a vector of x and a vector of z.
     * This is simply done as the structure (x,y)
     */
    public static Type.Structured makeSufficientSpace(Type dataSpace,
                                                      Type.Structured sharedSpace) {
        Type.Vector[] cmpnt = new Type.Vector[2];
        String[] label = new String[2];

        cmpnt[0] = new Type.Vector(dataSpace);
        cmpnt[1] = new Type.Vector(sharedSpace);
        label[0] = "dataSpace";
        label[1] = "sharedSpace";

        return new Type.Structured(cmpnt, label);
    }

    /**
     * decodeParentArray does the same job as decodeParents, except it operates on a full vector
     * instead of a single element.  Doing it this way may have large efficiency gains.
     */
    public int[] decodeParentVector(Value.Vector z) {
        int[] decodedArray = new int[z.length()];

        // turn the multi columned vector into an array of single column vectors.
        int structLength = ((Type.Structured) ((Type.Vector) z.t).elt).cmpnts.length;
        Value.Vector[] vectorArray = new Value.Vector[structLength];
        for (int i = 0; i < vectorArray.length; i++) {
            vectorArray[i] = z.cmpnt(i);
        }

        // Loop through vectors decoding values.
        for (int i = 0; i < decodedArray.length; i++) {
            int retVal = 0;
            for (int j = 0; j < multipliers.length; j++) {
                retVal += multipliers[j] * (vectorArray[j].intAt(i) - parentlwbs[j]);
            }
            decodedArray[i] = retVal;
        }
        return decodedArray;
    }


    /**
     * decodeParents converts a structure of parent values into a single index into the CPT
     * y = ({0,1},{0,1},{0,1}) <br>
     * y = (0,0,0), return 0 <br>
     * y = (1,0,0), return 1 <br>
     * y = (0,1,0), return 2 <br>
     * y = (1,1,0), return 3 <br>
     * y = (0,0,1), return 4 <br>
     * y = (1,0,1), return 5 <br>
     * y = (0,1,1), return 6 <br>
     * y = (1,1,1), return 7 <br>
     * <p/>
     * <p>If there are no parents, decodeParents returns 0.
     */
    public int decodeParents(Value.Structured z) {
        // Check if lengths match.
        if (z.length() != parentlwbs.length)
            throw new java.lang.IllegalArgumentException("Invalid parent length in decodeParents " +
                    z.length() + " != " + parentlwbs.length);

        // calculate the index, using the multipliers.
        int retval = 0;

        for (int i = 0; i < parentlwbs.length; i++) {
            int zVal = z.intCmpnt(i);
            if ((zVal >= parentlwbs[i]) && (zVal <= parentupbs[i]))            // bounds checking.
            {
                retval += multipliers[i] * (zVal - parentlwbs[i]);
            } else {
                throw new IllegalArgumentException("Value " + zVal +
                        " not in range (" + parentlwbs[i] + "," +
                        parentupbs[i] + ").");
            }
        }

        return retval;
    }

    /**
     * Create multiplier array for decoding parents.
     */
    protected int[] makeMultipliers() {
        int[] multipliers = new int[parentlwbs.length];
        for (int i = 0; i < multipliers.length; i++) {
            multipliers[i] = 1;
            // add one because the upbs are inclusive.
            for (int j = 0; j < i; j++) {
                multipliers[i] *= parentupbs[j] - parentlwbs[j] + 1;
            }
        }
        return multipliers;
    }

    /**
     * Does the reverse of the decodeParants procedure.  Takes the decoded value and turns it into
     * a list of parent states.
     */
    public int[] encodeParents(int x) {
        if (multipliers == null) {
            makeMultipliers();
        }
        int[] result = new int[parentlwbs.length];

        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = x / multipliers[i] + parentlwbs[i];
            x %= multipliers[i];
        }
        return result;
    }

    /**
     * return logP(X|Y,Z)
     */
    public double logP(Value x, Value y, Value z) {
        Value.Structured parentVals = (Value.Structured) z;
        int parentIndex = decodeParents(parentVals);

        Value.Vector yVector = (Value.Vector) y;
        Value.Structured yElt = (Value.Structured) yVector.elt(parentIndex);
        Value.Model subModel = (Value.Model) yElt.cmpnt(0);
        Value subParams = yElt.cmpnt(1);

        return subModel.logP(x, subParams, z);
    }

    /**
     * return logP(X|Y,Z) where v = (X,Y,Z)
     */
    public double logP(Value.Structured v) {
        return logP(v.cmpnt(0), v.cmpnt(1), v.cmpnt(2));
    }

    /**
     * Returns a stochastic vector of elements from the data-space conditional on Y,Z.
     * This normalises the CPT.
     */
    public Value.Vector generate(Random rand, int n, Value y, Value z) {
        Value.Structured parentVals = (Value.Structured) z;
        int parentIndex = decodeParents(parentVals);

        Value.Vector yVector = (Value.Vector) y;
        Value.Structured yElt = (Value.Structured) yVector.elt(parentIndex);
        Value.Model subModel = (Value.Model) yElt.cmpnt(0);
        Value subParams = yElt.cmpnt(1);

        Value.Vector result = subModel.generate(rand, n, subParams, Value.TRIV);

        return result;
    }

    /**
     * Generate data.
     */
    public Value.Vector generate(Random rand, Value y, Value.Vector z) {
        Value.Vector yVector = (Value.Vector) y;

        // Special case for generating no data.
        // This is required to ensure the correct type is returned.
        if (z.length() == 0) {
            Value.Structured yElt = (Value.Structured) yVector.elt(0);
            Value.Model subModel = (Value.Model) yElt.cmpnt(0);
            Value subParams = yElt.cmpnt(1);
            return subModel.generate(rand, 0, subParams, Value.TRIV);
        }


        // Tally up how many times each parent combinaiton occurs.
        int[] parentState = decodeParentVector(z);
        int[] tally = new int[numCombinations];
        for (int i = 0; i < parentState.length; i++) {
            tally[parentState[i]]++;
        }

        // Generate data for each parent combination.
        Value.Vector subResult[] = new Value.Vector[tally.length];
        for (int i = 0; i < subResult.length; i++) {
            if (tally[i] > 0) {
                Value.Structured yElt = (Value.Structured) yVector.elt(i);
                Value.Model subModel = (Value.Model) yElt.cmpnt(0);
                Value subParams = yElt.cmpnt(1);
                subResult[i] = subModel.generate(rand, tally[i], subParams, Value.TRIV);
            }
        }

        // Reorder generated data so parents match children properly.
        int index[] = new int[tally.length];
        int data[] = new int[parentState.length];
        for (int i = 0; i < parentState.length; i++) {
            int parent = parentState[i];
            data[i] = subResult[parent].intAt(index[parent]);
            index[parent]++;
        }

        // Return data with the correct type.
        Type.Discrete eltType = (Type.Discrete) ((Type.Vector) subResult[parentState[0]].t).elt;
        return new VectorFN.FastDiscreteVector(data, eltType);
    }


    /**
     * predict returns the value x which has the maximum probability given y (hence the least
     * logprob) if two values have the same logprob, it will return the first such.
     */
    public Value predict(Value y, Value z) {
        Value.Structured parentVals = (Value.Structured) z;
        int parentIndex = decodeParents(parentVals);

        Value.Vector yVector = (Value.Vector) y;
        Value.Structured yElt = (Value.Structured) yVector.elt(parentIndex);
        Value.Model subModel = (Value.Model) yElt.cmpnt(0);
        Value subParams = yElt.cmpnt(1);

        return subModel.predict(subParams, Value.TRIV);

    }

    public Value.Vector predict(Value y, Value.Vector z) {
        return new VectorFN.UniformVector(z.length(), predict(y, z));
    }


    //////////////////////////////////////////////////////////////////////////////////
    // suffA, suffB and suffC are three alternate ways of calculating sufficient    //
    // statistics for a CPT.  In CaMML this must be done many times and can take    //
    // up over 90% of computation time.  These three methods are under construction //
    // and being tested/optimized to speed CaMML up.                                //
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Original method for calculating sufficient statistics
     */
    private Value.Vector suffA(Value.Vector x, Value.Vector z) {
        // Original implementation of getSufficient
        int[][] tally = getSufficientArray(x, z);

        Value.Vector[] vecArray = new Value.Vector[tally.length];
        for (int i = 0; i < vecArray.length; i++) {
            vecArray[i] = new VectorFN.FastDiscreteVector(tally[i]);
        }
        Value.Vector v = new VectorFN.MultiCol(new Value.DefStructured(vecArray));
        return v;

    }

    /**
     * Test method for calculating stats, split vectors are saved for later reuse.
     */
    private Value.Vector suffB(Value.Vector x, Value.Vector z) {
        // Attempt splits. 
        int[] all = new int[parentupbs.length];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        SelectedVector[] zSplits = ((SelectedVector) z).split(all);
        SelectedVector[] xSplits = new SelectedVector[zSplits.length];
        for (int i = 0; i < xSplits.length; i++) {
            xSplits[i] = zSplits[i].copyRowSplit(x);
        }
        Value subStats[] = new Value[zSplits.length];
        for (int i = 0; i < subStats.length; i++) {
            subStats[i] = childModel.getSufficient(xSplits[i], zSplits[i]);
        }
        Value.Vector v2 = new VectorFN.FatVector(subStats);

        return v2;
    }

    /**
     * Similar to suffB with recursive splits.
     */
    private Value.Vector suffC(Value.Vector x, Value.Vector z) {
        // Attempt splits. 
        int[] all = new int[parentupbs.length];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        SelectedVector[] zSplits = ((SelectedVector) z).runRecursiveSplit(all);
        SelectedVector[] xSplits = new SelectedVector[zSplits.length];
        for (int i = 0; i < xSplits.length; i++) {
            xSplits[i] = zSplits[i].copyRowSplit(x);
        }
        Value subStats[] = new Value[zSplits.length];
        for (int i = 0; i < subStats.length; i++) {
            subStats[i] = childModel.getSufficient(xSplits[i], zSplits[i]);
        }
        Value.Vector v2 = new VectorFN.FatVector(subStats);

        return v2;
    }

    /** temp variable for perfomaance testing */
    //private static int suffCalls = 0;

    /** temp variable for perfomaance testing */
    //private static int[] suffCallArray = new int[10];

    /**
     * The sufficient statistics of a CPT is a list of sufficient stats of its elements.
     * Each paremt combination maps to a given entry in the sufficient vector.  This mapping is
     * defined by CPT.decodeParents()
     */
    //static int count = 0;
    public Value getSufficient(Value.Vector x, Value.Vector z) {
        //return suffC(x,z);

        /*
         *         suffCalls++;
         suffCallArray[parentlwbs.length]++;
         if ( (suffCalls & 0xFF) == 0) { 
         System.out.println( new StructureFN.FastDiscreteStructure(suffCallArray) + "\t" + suffCalls );
         }
        */


        // suffC is broken when intervention data is present. use suffA instead.
        //        Value.Vector v3;
        //        if ( parentupbs.length < 4 && z instanceof SelectedVector) { v3 = suffC(x,z); }
        //        else { v3 = suffA(x,z); }
        //        return v3;
        return suffA(x, z);

        /*
          Value.Vector v1 = suffA(x,z);
          //Value.Vector v2 = suffB(x,z);
          Value.Vector v3 = suffC(x,z);        
          if ( !v1.equals(v3) ) {    System.out.println( v1 + " != " + v3 );    }        
          return v1;
        */
    }

    /**
     * Returns sufficient statistics in an array.  This array may seem backwards, but it is
     * done this way for east compatibility with getSufficient.
     */
    public int[][] getSufficientArray(Value.Vector x, Value.Vector z) {
        // sanity check on vector lengths.
        if (x.length() != z.length()) {
            throw new RuntimeException("Vector lengths do not match in CPT.getSufficient.");
        }

        // Split up all data points relative to their parents.
        // decodedParant[i] == decodeParent( z.elt(i) )
        int[] decodedParent = decodeParentVector(z);

        Type.Discrete xType = (Type.Discrete) ((Type.Model) t).dataSpace;
        int upb = (int) xType.UPB;
        int lwb = (int) xType.LWB;
        int arity = upb - lwb + 1;

        // Tally up each occurance.
        int[][] tally = new int[arity][numCombinations];
        for (int i = 0; i < decodedParent.length; i++) {
            tally[lwb + x.intAt(i)][decodedParent[i]]++;
        }

        return tally;
    }


    /**
     * logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
     * In this case, s is simply the vector x
     */
    public double logPSufficient(Value s, Value y) {
        Value.Vector stats = (Value.Vector) s;
        Value.Vector params = (Value.Vector) y;

        double total = 0;
        for (int i = 0; i < stats.length(); i++) {
            Value.Structured elt = (Value.Structured) params.elt(i);
            Value.Model subModel = (Value.Model) elt.cmpnt(0);
            Value subParams = elt.cmpnt(1);
            total += subModel.logPSufficient(stats.elt(i), subParams);
        }
        return total;
    }

    // logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2)...

    /**
     * This gives the log-probability (or the log-probability-density for continuous data spaces)
     * of the vector x, given parameters y, and input vector z.  z and x must be of the same
     * length, and it is assumed each element i of x has been generated using the parameters y and
     * the corresponding element i of z as an input value.
     */
    public double logP(Value.Vector x, Value y, Value.Vector z) {
        Value s = getSufficient(x, z);
        return logPSufficient(s, y);
    }


    /**
     * returns a representation of the CPT
     */
    public String toString() {
        String s = "CPT : ";
        for (int i = 0; i < parentlwbs.length; i++)
            s = s + "(" + parentlwbs[i] + "," + parentupbs[i] + ")";
        return s;
    }

    /**
     * Shortcut method to make (sometimes cumbersome) CPT parameters.
     */
    public Value.Vector makeCPTParams(double[][] params) {
        // Take (double)params[][] and make an 2D array of Value.Continuous
        Value.Continuous[][] continuousParams =
                new Value.Continuous[params.length][params[0].length];

        for (int i = 0; i < continuousParams.length; i++) {
            if (continuousParams[i].length == params[i].length) { // copy all params
                for (int j = 0; j < params[i].length; j++) {
                    continuousParams[i][j] = new Value.Continuous(params[i][j]);
                }
            } else {
                throw new RuntimeException("Invalid Parameter Length");
            }
        }

        // Turn the 2D array of Value.Continuous into a 1D array of Value.Structured
        Value.Structured[] structuredParams = new Value.Structured[continuousParams.length];
        for (int i = 0; i < structuredParams.length; i++) {
            Value.Structured continuousStruct = new Value.DefStructured(continuousParams[i]);
            structuredParams[i] = new Value.DefStructured(new Value[]{childModel,
                    continuousStruct});

        }

        return new VectorFN.FatVector(structuredParams);
    }

    /**
     * Exception thrown when MaxCells exceeded
     */
    public static class ExcessiveCombinationsException extends RuntimeException {
        /**
         * Serial ID required to evolve class while maintaining serialisation compatibility.
         */
        private static final long serialVersionUID = -6198598888853126685L;

        public ExcessiveCombinationsException(String s) {
            super(s);
        }

        public ExcessiveCombinationsException(Exception e) {
            super(e);
        }

    }

    /**
     * used by set/getNumParams(), -1 indicated numParams has not been initialised.
     */
    protected int numParams = -1;

    /**
     * Hardcode the number of parameters to be returned by numParams
     * This is useful for using a CPT to represent another distribution (eg. DTree or logit)
     * while still returning the correct value in getNumparams.
     */
    public void setNumParams(int n) {
        this.numParams = n;
    }

    /**
     * Return number of parameters present in CPT
     */
    public int getNumParams(Value params) {
        if (numParams != -1) {
            return numParams;
        }

        int total = 0;
        Value.Vector paramVec = (Value.Vector) params;
        for (int i = 0; i < paramVec.length(); i++) {
            Value.Structured elt = (Value.Structured) paramVec.elt(i);
            Value.Model subModel = (Value.Model) elt.cmpnt(0);
            Value subParams = elt.cmpnt(1);

            if (subModel instanceof GetNumParams) {
                total += ((GetNumParams) subModel).getNumParams(subParams);
            } else {
                throw new RuntimeException("model : " + subModel +
                        " has not implemented ModelGlue.GetNumParams\t" + subModel.getClass());
            }
        }
        return total;
    }

    /**
     * Make a (cptModel, cptParams) struct from a list of doubles and input/output types
     */
    public static Value.Structured makeCPTStruct(double[][] p, Type.Discrete x, Type.Structured z) {
        Type[] array = z.cmpnts;
        int[] lwb = new int[array.length];
        int[] upb = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            Type.Discrete dType = (Type.Discrete) array[i];
            if (Double.isInfinite(dType.LWB) || Double.isInfinite(dType.UPB)) {
                throw new RuntimeException("CPT Dimentions must not be infinite : " + z);
            }
            lwb[i] = (int) dType.LWB;
            upb[i] = (int) dType.UPB;
        }

        // do NOT restrict CPT size to defaultMaxCells
        // Multinomial2 used as it it implements ModelGlue.getNumParams()
        CPT cpt = new CPT(new Multinomial2(x), lwb, upb, -1);
        Value.Vector params = cpt.makeCPTParams(p);

        return new Value.DefStructured(new Value[]{cpt, params});
    }

}
