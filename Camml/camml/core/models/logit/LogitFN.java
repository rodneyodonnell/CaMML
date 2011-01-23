/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
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
// Class contains useful functions for dealing with logit models.
//

// File: LogitFN.java
// Author: rodo@dgs.monash.edu.au

package camml.core.models.logit;

import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;

/**
 * Class contining useful functions for dealing with logit models.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.4 $ $Date: 2006/08/22 03:13:29 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/models/logit/LogitFN.java,v $
 */

public class LogitFN {
    
    /** Return x (child) vector given a node and a complete dataset*/
    public static Value.Vector getX(JulesLogit.Node node, Value.Vector data) {
        return data.cmpnt(node.nd);
    }

    /** Return z (parent) vector given a node and a complete dataset*/
    public static Value.Vector getZ(JulesLogit.Node node, Value.Vector data) {
        Value.Vector[] zArray = new Value.Vector[node.ndad];
        for (int i = 0; i < node.ndad; i++) {
            zArray[i] = data.cmpnt(node.dads[i]);
        }
        return new VectorFN.MultiCol(new Value.DefStructured(zArray));
    }

    /** Return x and z as a multicol vector [z0,z1,....zn,x] */
    public static Value.Vector combineXZ(Value.Vector x, Value.Vector z) {
        Type.Structured zType = (Type.Structured)((Type.Vector)z.t).elt;
        Value.Vector array[] = new Value.Vector[zType.cmpnts.length+1];
        for (int i = 0; i < array.length-1; i++) {
            array[i] = z.cmpnt(i);
        }
        array[array.length-1] = x;
        return new VectorFN.MultiCol(new Value.DefStructured(array));
    }
    
    /** Take an array of type double[], double[][], double[].....[] and return a cmds Vector*/
    public static Value.Vector makeVec(Object o) {
        if ( o instanceof double[]) {return new VectorFN.FastContinuousVector((double[])o); }
        if ( o instanceof Object[] ) {
            Object[] array = (Object[])o;
            Value.Vector[] vec = new Value.Vector[array.length];
            for (int i = 0; i < array.length; i++) {
                vec[i] = makeVec(array[i]);
            }
            Type.Structured vType = new Type.Structured();
            return new VectorFN.FatVector(vec,new Type.Vector(vType));
        }
        throw new RuntimeException("double[] or Object[] required, found " + o.getClass());        
    }
    
    /** Reverse process of makeVec, takes cdms vector and return an array. */
    public static Object makeArray(Value.Vector vec) {
        if (vec instanceof VectorFN.FastContinuousVector) {
            double array[] = new double[vec.length()];
            for (int i = 0; i < array.length; i++) {array[i] = vec.doubleAt(i);}
            return array;
        }
        Object[] o = new Object[vec.length()];
        for (int i = 0; i < o.length; i++) {
            o[i] = makeArray((Value.Vector)vec.elt(i));
        }
        
        // Typecase to get the correct types.
        // This could probably be done with reflection, but this is as good as we need.

        // nasty hack. Required when running inside Camml, probably due to zero parent nodes.
        if (o.length == 0) { return new double[0][0];} 
        else if (o[0] instanceof double[]) { 
            double [][] o2 = new double[o.length][];
            for (int i = 0; i < o2.length; i++) { o2[i] = (double[])o[i]; }
            return o2;
        }
        else if (o[0] instanceof double[][]) { 
            double [][][] o2 = new double[o.length][][];
            for (int i = 0; i < o2.length; i++) { o2[i] = (double[][])o[i]; }
            return o2;
        }
        else if (o[0] instanceof double[][][]) { 
            double [][][][] o2 = new double[o.length][][][];
            for (int i = 0; i < o2.length; i++) { o2[i] = (double[][][])o[i]; }
            return o2;
        }
        
        
        return o;
    }

    /** logP returns the probability of data given parent data. */
    public static double[] logP(double[] c, double[][][] d, int[] parentVal) {

        //int ry = arity[node.nd] - 1;
        int ry = c.length-1;
        double prob[] = new double[ry + 1];
        double total = 0.0;

        /* calc conditional probs for this parent combo */
        for (int i = 0; i <= ry; i++) {
            prob[i] = c[i];
            for (int j = 0; j < d[i].length; j++) {
                prob[i] += d[i][j][parentVal[j]];
            }
            //prob[i] = Math.exp(prob[i]);
            total += Math.exp(prob[i]);
        }

        // Normalise probabilities 
        total = -Math.log(total);        
        for (int k = 0; k <= ry; k++) {
            prob[k] = prob[k] + total;
        }
        return prob;
    }

    /** Same as logP except c and d are expected to be log(c[]) and log(d[][][]) 
     *  This provides a dramatic speed increase. */
    public static double[] logPTransformed(double[] c, double[][][] d, int[] parentVal) {
    
        
        //int ry = arity[node.nd] - 1;
        int ry = c.length-1;
        double prob[] = new double[ry + 1];
        double total = 0.0;

        /* calc conditional probs for this parent combo */
        for (int i = 0; i <= ry; i++) {
            prob[i] = c[i];
            for (int j = 0; j < d[i].length; j++) {
                prob[i] *= d[i][j][parentVal[j]];
            }
            //prob[i] = Math.exp(prob[i]);
            total += prob[i];
        }
        
        
        // Normalise probabilities             
        for (int k = 0; k <= ry; k++) {
            prob[k] = prob[k] / total;
        }
        return prob;
    }

}
