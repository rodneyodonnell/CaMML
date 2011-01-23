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

package camml.core.models.logit;

import java.util.Random;

import camml.core.library.StructureFN;
import camml.core.models.ModelLearner.GetNumParams;
import camml.core.models.multinomial.MultinomialLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.Value.Model;

public class Logit extends Model implements GetNumParams {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 5471907342493946942L;
    /** Static instance of model. */
    public static Logit logit = new Logit();
    
    public Logit() { super(Type.MODEL); }

    /** Calculate array of logP's given x,y,z */
    public double[] logPArray(Value y, Value z) {
        // Extract parameters
        Value.Structured yStruct = (Value.Structured)y;
        double[] c = (double[])LogitFN.makeArray((Value.Vector)yStruct.cmpnt(0));
        double[][][] d = (double[][][])LogitFN.makeArray((Value.Vector)yStruct.cmpnt(1));
        
        // Extract parent values.
        Value.Structured zStruct = (Value.Structured) z;
        int[] zArray = new int[zStruct.length()];
        for (int i = 0; i < zArray.length; i++) { zArray[i] = zStruct.intCmpnt(i); }
        
        return LogitFN.logP(c,d,zArray);
    }
    
    public double logP(Value x, Value y, Value z) {
        // Return logP
        Value.Discrete xVal = (Value.Discrete)x;
        return logPArray(y,z)[xVal.getDiscrete()];
    }

    /** Original implementation of logP, under java1.5 this is very slow
     *  due to StrictMath.log and StrictMath.exp being called whenever
     *  Math.log or Math.exp are used.  This may be fixed in a later
     *  version of java?  
     *  
     *  Discussion at:
     *  http://forums.java.net/jive/thread.jspa?threadID=2151&start=30&tstart=0
     *  */
    public double slowLogP(Value.Vector x, Value y, Value.Vector z)         
    {
        // Extract parameters
        Value.Structured yStruct = (Value.Structured)y;
        double[] c = (double[])LogitFN.makeArray((Value.Vector)yStruct.cmpnt(0));
        double[][][] d = (double[][][])LogitFN.makeArray((Value.Vector)yStruct.cmpnt(1));
        
        double total = 0;
        
        // Hopefully extracting all vectors first will speed things up.
        Type.Structured zType = (Type.Structured)((Type.Vector)z.t).elt;
        Value.Vector zVecArray[] = new Value.Vector[zType.cmpnts.length]; 
        for (int i = 0; i < zVecArray.length; i++) {
            zVecArray[i] = z.cmpnt(i);
        }
        
        // Extract parent values.
        int[] zArray = new int[zVecArray.length];
        // Extracting zLen here gives a surprising performance boost.
        int zLen = z.length(); 
        for (int i = 0; i < zLen; i++) {
            for (int j = 0; j < zArray.length; j++) { 
                zArray[j] = zVecArray[j].intAt(i); 
            }                        
            total += LogitFN.logP(c,d,zArray)[x.intAt(i)];
        }
        return total;
    }

    /** Return log probability of data given parameters. */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
        // Extract parameters
        Value.Structured yStruct = (Value.Structured)y;
        double[] c = (double[])LogitFN.makeArray((Value.Vector)yStruct.cmpnt(0));
        double[][][] d = (double[][][])LogitFN.makeArray((Value.Vector)yStruct.cmpnt(1));
        
        // It is significantly faster to transform 'c' and 'd' here and call
        // logPTransformed than the more obvious implementation found above
        // in slowLogP, so we do it this way.
        // Hopefully we don't run into any overflow/underflow issues.
        for (int i = 0; i < c.length; i++) { c[i] = Math.exp(c[i]); }
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                for (int k = 0; k < d[i][j].length; k++) {
                    d[i][j][k] = Math.exp(d[i][j][k]);
                }
            }
        }
        
        double total = 0;
        
        // Extracting all vectors first speeds things up.
        Type.Structured zType = (Type.Structured)((Type.Vector)z.t).elt;
        Value.Vector zVecArray[] = new Value.Vector[zType.cmpnts.length]; 
        for (int i = 0; i < zVecArray.length; i++) {
            zVecArray[i] = z.cmpnt(i);
        }
        
        // Extract parent values.
        int[] zArray = new int[zVecArray.length];
        // Extracting zLen here gives a surprising performance boost.
        int zLen = z.length(); 
        for (int i = 0; i < zLen; i++) {
            for (int j = 0; j < zArray.length; j++) { 
                zArray[j] = zVecArray[j].intAt(i); 
            }                        
            double[] array = LogitFN.logPTransformed(c,d,zArray);
            double p = array[x.intAt(i)];
            total += Math.log(p);
        }
        return total;
    }

    
    public Vector generate(Random rand, int n, Value y, Value z) {
        // Create paramets for given parent set.
        double array[] = logPArray(y,z);
        for (int i = 0; i < array.length; i++) {array[i] = Math.exp(array[i]); }
        
        // Use a multinomial model to generate data.
        Value.Model m = MultinomialLearner.getMultinomialModel(0,array.length-1);
        return m.generate(rand,n,new StructureFN.FastContinuousStructure(array),Value.TRIV);        
    }

    public Value predict(Value y, Value z) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public Vector predict(Value y, Vector z) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public Value getSufficient(Vector x, Vector z) {
        return new Value.DefStructured(new Value[]{x,z});
    }

    public double logPSufficient(Value s, Value y) {
        Value.Structured struct = (Value.Structured) s;
        return logP((Value.Vector)struct.cmpnt(0),y,(Value.Vector)struct.cmpnt(1));
    }

    /* (non-Javadoc)
     * @see camml.core.models.ModelLearner.GetNumParams#getNumParams(cdms.core.Value)
     */
    public int getNumParams(Value params) {
        Value.Structured y = (Value.Structured)params;
        double c[] = (double[])LogitFN.makeArray((Value.Vector)y.cmpnt(0));
        double d[][][] = (double[][][])LogitFN.makeArray((Value.Vector)y.cmpnt(1));
        
        int numParams = c.length-1;

        for (int i=0; i< d.length-1; i++) {
            for(int j = 0; j < d[i].length; j++) {
                numParams += d[i][j].length-1;
            }
        }
        return numParams;
    }

    public String toString() {return "logit";}
}
