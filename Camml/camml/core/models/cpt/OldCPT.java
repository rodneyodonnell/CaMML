/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Lucas Hope, Rodney O'Donnell
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
// CPT model for CDMS
//

// File: OldCPT.java
// Author: lhope@csse.monash.edu.au
// Modified : rodo@dgs.monash.edu.au

package camml.core.models.cpt;

import java.util.Random;

import cdms.core.*;

/**
   OldCPT model.
   A OldCPT is a lookup table for P(X|Pa(X)), where X and Pa(X) are discrete (multinomial),and Pa(X)
   are the variables which X is directly dependant on.)
 
   x is a Type.Discrete, This is the dependant variable.
   y is of Type.Triv as the OldCPT is completely parameterized by it's constructors
   z is a Type.Structured containing Type.Discrete values, These are the independant variables. 
   s is simply a structure containing x and y.  ie. (x,y)
 
*/
public class OldCPT extends Value.Model
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 2380925701194381958L;

    /** a static type that could be used to fill OldCPTs */
    public static Type.Continuous OldCPTLOGP = 
        new Type.Continuous(0.0, Double.POSITIVE_INFINITY, true, false);
    
    protected int lwb;              // Lower bound of x
    protected int upb;              // Upper bound of x
    protected int[] parentlwbs;     // Array of Lower bounds of z[i]
    protected int[] parentupbs;     // Array of Upper bounds of z[i]
    protected double[][] logProbs;  // logProbs[i][j] = -log(x[j] | z[i]) 
    
    //this stores the multipliers used to index the logProbs matrix.
    private int[] multipliers;
    
    /**
       OldCPTCreator creates OldCPTs :).
     
       Parameters:<br>
       a lwb and upb for the dataspace. Note that symbolic types are lowerbounded at 0<br>
       a vector of discrete, representing the lowerbound of each of Pa(X) (the parents X)<br>
       a vector of discrete, representing the upperbound of each of Pa(X) (the parents X)<br>
       (the order is important, and must be kept constant. It defines how to index the table.)<br>
       a vector of vectors of continuous, representing the logP of each value of X, indexed by
       Pa(X).
       <p>
       all wrapped inside a structured type.
    */
    public static OldCPTCreator cptCreator = new OldCPTCreator();
    public static class OldCPTCreator extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7966099516844439666L;

        private static Type.Structured paramType =
            new Type.Structured(new Type[]{Type.DISCRETE, Type.DISCRETE,
                                           new Type.Vector(Type.DISCRETE),
                                           new Type.Vector(Type.DISCRETE),
                                           new Type.Vector(new Type.Vector(Type.CONTINUOUS))},
                new String[]{"Data lowerbound", "Data upperbound",
                             "Parent lowerbounds", "Parent upperbounds",
                             "logProbs"},
                new boolean[]{false, false, false, false, false});
        
        private static Type.Model resultType =
            new Type.Model(Type.DISCRETE, 
                           Type.STRUCTURED,
                           Type.TRIV,
                           new Type.Vector(Type.DISCRETE));
        
        public OldCPTCreator()
        {
            super(new Type.Function(paramType, resultType, false, false));
        }
        
        public Value apply(Value v)
        {
            // decode v
            Value.Structured s = (Value.Structured)v;
            int lwb = ((Value.Scalar)s.cmpnt(0)).getDiscrete();
            int upb = ((Value.Scalar)s.cmpnt(1)).getDiscrete();
            Value.Vector lwbVector = (Value.Vector)s.cmpnt(2);
            Value.Vector upbVector = (Value.Vector)s.cmpnt(3);
            Value.Vector probMatrix = (Value.Vector)s.cmpnt(4);
            
            // translate lwbVector and upbVector
            int[] lwbs = new int[lwbVector.length()];
            int[] upbs = new int[upbVector.length()];
            for(int i = 0; i < lwbs.length; i ++) {
                lwbs[i] = lwbVector.intAt(i);
                upbs[i] = upbVector.intAt(i);
            }
            
            // translate probMatrix
            double[][] logProbs = new double[probMatrix.length()][];
            for(int i = 0; i < logProbs.length; i ++) {
                Value.Vector probVector = (Value.Vector)probMatrix.elt(i);
                logProbs[i] = new double[probVector.length()];
                for(int j = 0; j < logProbs[i].length; j ++)
                    { logProbs[i][j] = probVector.doubleAt(j); }
            }
            
            return new OldCPT(lwb, upb, lwbs, upbs, logProbs);
        }
    }
    
    /**
       Initialise the OldCPT. This checks that parentlwbs and parentupbs have the same length,
       and that logProbs is exactly the right size. Throws an exception if the above is not
       the case.
    */
    public OldCPT(int lwb, int upb, int[] parentlwbs, int[] parentupbs, double[][] logProbs)
    {
        super(new Type.Model(makeDataSpace(lwb, upb),
                             Type.TRIV,
                             makeSharedSpace(parentlwbs, parentupbs),
                             makeSufficientSpace(makeDataSpace(lwb, upb),
                                                 makeSharedSpace(parentlwbs, parentupbs))));
        
        // check for input validity.
        if(lwb > upb) {
            throw new IllegalArgumentException("Dependent variable must have at least one value!");
        }
        
        if(parentlwbs.length != parentupbs.length)
            { throw new IllegalArgumentException("Parent bounds do not match!"); }
        
        for(int i = 0; i < parentlwbs.length; i ++) {
            if(parentlwbs[i] > parentupbs[i]) {
                throw new IllegalArgumentException("Parent variable " + i + 
                                                   " must have at least one value!");
            }
        }
        
        int logProbLen = 1;
        for(int i = 0; i < parentlwbs.length; i ++) 
            { logProbLen *= parentupbs[i] - parentlwbs[i] + 1; }
        if(logProbs.length != logProbLen) {
            throw new IllegalArgumentException("logProbs has length " + logProbs.length +
                                               ", should be " + logProbLen + ".");
        }
        
        for(int i = 0; i < logProbs.length; i ++) {
            if(logProbs[i].length != upb - lwb + 1) {
                throw new IllegalArgumentException("logProbs[" + i + "] has length " +
                                                   logProbs[i].length + ", should be " +
                                                   (upb-lwb+1) + ".");
            }                             
        }
        
        // copy the dependent variable bounds
        this.lwb = lwb;
        this.upb = upb;
        
        // copy the parent bounds
        this.parentlwbs = (int[])parentlwbs.clone();
        this.parentupbs = (int[])parentupbs.clone();
        
        // copy the logProbs (deep copy)
        this.logProbs = new double[logProbs.length][];
        for(int i = 0; i < logProbs.length; i ++)
            { this.logProbs[i] = (double[])logProbs[i].clone(); }
    }
    
    /** The dataspace is the bounds of the discrete variable. */
    public static Type.Discrete makeDataSpace(int lwb, int upb)
    {
        return new Type.Discrete(lwb, upb, false, false, false, false);
    }
    
    /** 
        The parameterspace is a structure of the different ranges of the parent variables
        Not sure if this will work for parentless OldCPTs. Can Type.Structured have no components?
    */
    public static Type.Structured makeSharedSpace(int[] lwbs, int[] upbs)
    {
        int len = lwbs.length;                   // assuming upbs.length = lwbs.length
        Type[] cmpnts = new Type[len];
        String[] labels = new String[len];
        boolean[] falseArray = new boolean[len]; // note java sets all booleans to false initially
        
        for(int i = 0; i < len; i ++) {
            cmpnts[i] = new Type.Discrete(lwbs[i], upbs[i], false, false, false, false);
            labels[i] = "Parent " + i;
        }
        return new Type.Structured(cmpnts, labels, falseArray);
    }
    
    /**
       The sufficient summarises a vector of x and a vector of z.
       This is simply done as the structure (x,y)
    */
    public static Type.Structured makeSufficientSpace(Type.Discrete dataSpace, 
                                                      Type.Structured sharedSpace )
    {
        Type.Vector[] cmpnt = new Type.Vector[2];
        String[] label = new String[2];
        
        cmpnt[0] = new Type.Vector(dataSpace);
        cmpnt[1] = new Type.Vector(sharedSpace);
        label[0] = "dataSpace";
        label[1] = "sharedSpace";
        
        return new Type.Structured( cmpnt, label );
    }
    
    /**
       decodeParents converts a structure of parent values into a single index into
       the OldCPT class's table. The way the index increments is best explained with an example:<br>
       y = ({0,1},{0,1},{0,1}) <br>
       y = (0,0,0), return 0 <br>
       y = (1,0,0), return 1 <br>
       y = (0,1,0), return 2 <br>
       y = (1,1,0), return 3 <br>
       y = (0,0,1), return 4 <br>
       y = (1,0,1), return 5 <br>
       y = (0,1,1), return 6 <br>
       y = (1,1,1), return 7 <br>
     
       <p>If there are no parents, decodeParents returns 0.
     
       This method does not check whether y contains items with the correct ranges.
    */
    public int decodeParents(Value.Structured z)
    {
        // no parents is a special case.
        if (parentlwbs.length == 0)
            { return 0; }
        
        // array to calculate the multiplier for each parent. Only needs to be done once.
        if(multipliers == null) {
            multipliers = new int[parentlwbs.length];
            for(int i = 0; i < multipliers.length; i ++) {
                multipliers[i] = 1;
                // add one because the upbs are inclusive.
                for(int j = 0; j < i; j ++)
                    { multipliers[i] *= parentupbs[j] - parentlwbs[j] + 1; }
            }
        }
        
        // calculate the index, using the multipliers.
        int retval = 0;
        
        for(int i = 0; i < parentlwbs.length; i ++) { 
            int zVal = z.intCmpnt(i);
            if ((zVal >= parentlwbs[i]) && (zVal <= parentupbs[i]))            // bounds checking. 
                { retval += multipliers[i] * (z.intCmpnt(i) - parentlwbs[i]); }
            else
                { throw new IllegalArgumentException("Value " + zVal + " not in range (" + 
                                                     parentlwbs[i]+ ","+ parentupbs[i] + ")." ); }
        }
        
        
        return retval;
    } 
    
    // logP(X|Y,Z)
    public double logP(Value x, Value y, Value z)
    {
        Value.Structured parentVals = (Value.Structured)z;
        return logProbs[decodeParents(parentVals)][((Value.Discrete)x).getDiscrete() - lwb];
    }
    
    // logP(X|Y,Z) where v = (X,Y,Z)
    public double logP(Value.Structured v)
    {
        return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }
    
    /*
    // Returns a vector of elements from the data-space, where the ith element is taken from
    // p(.|Y,Z_i). 
    public Value.Vector generate(long seed, Value y, Value.Vector z)
    {
    return generate(seed, z.length(), y, Value.TRIV);
    }
    */
    
    
    /**
       Returns a stochastic vector of elements from the data-space conditional on Y,Z.
       This normalises the OldCPT.
    */
    public Value.Vector generate(Random rand, int n, Value y, Value z)
    {
        rand.nextDouble();  // use random generator...
        
        int parentIndex = decodeParents((Value.Structured)z);
        double[] probs = new double[upb - lwb + 1];
        
        double tot = 0.0;
        // convert from logprobs
        for(int i = 0; i < probs.length; i ++) {
            probs[i] =  Math.exp(- logProbs[parentIndex][i]);
            tot += probs[i];
        }
        
        // normalise
        if(tot == 0.0) {
            for(int i = 0; i < probs.length; i ++) 
                { probs[i] = 1.0 / probs.length; }
        }
        else {
            for(int i = 0; i < probs.length; i ++) 
                { probs[i] /= tot; }
        }
        
        int[] xValues = new int[n];
        
        for(int i = 0; i < n; i ++) {
            // pick the value stochastically
            double prob = rand.nextDouble();
            
            int probIndex = 0;
            double probSum = probs[0];
            
            // cycle through probs until we find the matching probability.
            while(probSum < prob && probIndex < probs.length - 1) {
                probIndex ++;
                probSum += probs[probIndex];
            }
            
            xValues[i] = probIndex + lwb;
        }
        return new VectorFN.DiscreteVector(new Type.Vector(Type.TYPE,
                                                           ((Type.Model)t).dataSpace,
                                                           false, false, false, false), xValues);
    }
    
    /** predict returns the value x which has the maximum probability given y (hence the least 
        logprob) if two values have the same logprob, it will return the first such.
    */
    public Value predict(Value y, Value z)
    {
        int parentIndex = decodeParents((Value.Structured)z);
        
        // get the smallest logprob
        int bestVal = 0;
        double bestLogProb = logProbs[parentIndex][0];
        for(int i = 1; i < logProbs[parentIndex].length; i ++) {
            double tmpLogProb = logProbs[parentIndex][i];
            
            if(tmpLogProb < bestLogProb) {
                bestLogProb = tmpLogProb;
                bestVal = i;
            }
        }
        return new Value.Discrete((Type.Discrete)((Type.Model)t).dataSpace, bestVal + lwb);
    }
    
    public Value.Vector predict(Value y, Value.Vector z) {
        Value[] predictedVec = new Value[ z.length() ];
        for (int i = 0; i < predictedVec.length; i++ )
            predictedVec[i] = predict( y, z.elt(i) );
        
        return new VectorFN.UniformVector(z.length(), predict(y, Value.TRIV));
    }
    
    /**
       The sufficient statistic is simply the vector x.
       Because CDMS vectors are constant, it is safe to just return x.
       (will check)
    */
    public Value getSufficient(Value.Vector x, Value.Vector z)
    {
        if ( x.length() != z.length() )
            throw new IllegalArgumentException("Parent bounds do not match in getSufficient");    
        Value.Vector[] cmpnt = new Value.Vector[2];
        
        cmpnt[0] = x;
        cmpnt[1] = z;
        
        Type.Structured tt = (Type.Structured)((Type.Model)t).sufficientSpace;;
        return new Value.DefStructured( tt, cmpnt);
    }
    
    /** logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
        In this case, s is simply the vector x */
    public double logPSufficient(Value s, Value y)
    {
        Value.Vector x = (Value.Vector)((Value.Structured)s).cmpnt(0);;
        Value.Vector z = (Value.Vector)((Value.Structured)s).cmpnt(0);;
        double sum = 0.0;
        
        for(int i = 0; i < x.length(); i ++)
            { sum += logP(x.elt(i), y, z.elt(i)); }
        return sum;
    }
    
    /** returns a representation of the OldCPT */
    public String toString()
    {
        String retval = "";
        for(int i = 0; i < logProbs.length; i ++) {
            retval += "| ";
            for(int j = 0; j < logProbs[i].length; j ++)
                { retval += logProbs[i][j] + " | "; }
            
            retval += "\n";
        }
        return retval;
    }
    
    
}
