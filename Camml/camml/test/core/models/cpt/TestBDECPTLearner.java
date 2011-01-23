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
// Test cases for BDE CPT Learner
//

// File: TestBDECPTLearner.java
// Author: rodo@dgs.monash.edu.au

package camml.test.core.models.cpt;

import camml.core.models.cpt.BDECPTLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test cases for BDE CPT Learner
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.3 $ $Date: 2006/08/22 03:13:39 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/core/models/cpt/TestBDECPTLearner.java,v $
 */

public class TestBDECPTLearner extends TestCase {

    /** */
    public TestBDECPTLearner() { super(); }

    /**     */
    public TestBDECPTLearner(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite( TestBDECPTLearner.class );        
    }

    /** Binary type. */
    private final static Type.Discrete binary = 
        new Type.Discrete(0,1,false,false,false,false); 
    
    /** Ternary (3 state) type.*/
    private final static Type.Discrete ternary = 
        new Type.Discrete(0,2,false,false,false,false); 

    /**  */
    protected void setUp() throws Exception {        
    }
    
    /** Convenience function to extract the nth set of parameters from CPT param vec. */
    public static Value.Structured getParams(Value.Vector y, int n) {
        return (Value.Structured)((Value.Structured)y.elt(n)).cmpnt(1);
    }
    
    /** Test correct parameters are generated. */
    public final void testParams() throws Exception {
        // Create datasets
        Value.Vector xData[] = new Value.Vector[] {
            new VectorFN.FastDiscreteVector(new int[] {0,1}, binary),
            new VectorFN.FastDiscreteVector(new int[] {1,1}, binary),
            new VectorFN.FastDiscreteVector(new int[] {0,0,2,2}, ternary),
            new VectorFN.FastDiscreteVector(new int[] {0,1,2,0}, ternary)
        };

        // Temporary vectors to make specifying zData easier
        Value.Vector temp[] = new  Value.Vector[] {
            new VectorFN.FastDiscreteVector(new int[] {0,0}, binary),
            new VectorFN.FastDiscreteVector(new int[] {0,1}, binary),
            new VectorFN.FastDiscreteVector(new int[] {0,0,1,1}, binary),
            new VectorFN.FastDiscreteVector(new int[] {1,0,0,2}, ternary)
        };
        
        Value.Vector zData[][] = new Value.Vector[][] {
            new Value.Vector[] {temp[0]},
            new Value.Vector[] {temp[0],temp[1]},
            new Value.Vector[] {temp[2]},
            new Value.Vector[] {temp[2],temp[3]}
        };
        
        
        // Create several BDE parameterizers with different ESS values.
        double ess[] = new double[] {1, 2, 2.5, 5, 9.6};
        BDECPTLearner bl[] = new BDECPTLearner[ess.length];
        for (int i = 0; i < bl.length; i++) {
            bl[i] = new BDECPTLearner(ess[i]);
        }
        
        // Run bde parameterizer on each dataset/learner pair.
        Value.Structured msy[][] = new Value.Structured[xData.length][bl.length];        
        Value.Vector y[][] = new Value.Vector[msy.length][msy[0].length];
        double cost[][] = new double[msy.length][msy[0].length];
        
        Value triv = Value.TRIV;
        for (int i = 0; i < xData.length; i++) {            
            for (int j = 0; j < bl.length; j++) {                
                Value.Vector x = xData[i];
                Value.Vector z = new VectorFN.MultiCol(new Value.DefStructured(zData[i]));
                // (model,stats,params) structure
                msy[i][j] = bl[j].parameterize(triv,x,z);
                // extract params from (m,s,y)
                y[i][j] = (Value.Vector)msy[i][j].cmpnt(2);
                
                // Calculate cost.
                cost[i][j] = bl[j].parameterizeAndCost(triv,x,z);
                assertEquals(cost[i][j], bl[j].msyCost(msy[i][j]));
            }
        }        
        
        double p[][][][] = new double[msy.length][msy[0].length][][]; 
        
        for (int i = 0; i < msy.length; i++) {
            for (int j = 0; j < msy[i].length; j++) {
                p[i][j] = new double[y[i][j].length()][];
                for (int k = 0; k < y[i][j].length(); k++) {
                    Value.Structured subParams = getParams(y[i][j],k);
                    p[i][j][k] = new double[subParams.length()];
                    // Assert probabilities sum to 1.0
                    double sum = 0;
                    for (int l = 0; l < subParams.length(); l++) {
                        p[i][j][k][l] = subParams.doubleCmpnt(l); 
                        sum += p[i][j][k][l];
                    }
                    assertEquals( sum, 1.0, 0.0000000001);
                }
            }
        }
                                        
        // Check parameters are correct.
        for (int i = 0; i < ess.length; i++) {
            // binary cases 
            // data = [(0|0),(1|0)]
            int n = 0; 
            int len = p[n][i].length;
            assertEquals((ess[i]/len/2+1)/(ess[i]/len+2), p[n][i][0][0],  0.00000001);    // x = 0 | z = 0
            assertEquals((ess[i]/len/2+0)/(ess[i]/len+0), p[n][i][1][0],  0.00000001);    // x = 0 | z = 1            
            assertEquals(-Math.log(0.5*(ess[i]/len/2+0)/(ess[i]/len+1)), cost[n][i],  0.00000001);
            
            n = 1; 
            // data = [(0|0,0),(0|0,1)]
            len = p[n][i].length;
            assertEquals((ess[i]/len/2+0)/(ess[i]/len+1), p[n][i][0][0],  0.00000001);    // x = 0 | z = 0 0
            assertEquals((ess[i]/len/2+0)/(ess[i]/len+0), p[n][i][1][0],  0.00000001);    // x = 0 | z = 1 0
            assertEquals((ess[i]/len/2+0)/(ess[i]/len+1), p[n][i][2][0],  0.00000001);    // x = 0 | z = 0 1
            assertEquals((ess[i]/len/2+0)/(ess[i]/len+0), p[n][i][3][0],  0.00000001);    // x = 0 | z = 1 1
            assertEquals(-Math.log(0.5*0.5), cost[n][i],  0.00000001);
            
            // ternary cases
            // data = [(0|0),(0|0),(2|1),(2|1)]
            n = 2;
            len = p[n][i].length;
            assertEquals((ess[i]/len/3+2)/(ess[i]/len+2), p[n][i][0][0],  0.00000001);    // x = 0 | z = 0
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+2), p[n][i][0][1],  0.00000001);    // x = 1 | z = 0
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+2), p[n][i][1][0],  0.00000001);    // x = 0 | z = 1
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+2), p[n][i][1][1],  0.00000001);    // x = 1 | z = 1
            assertEquals(-Math.log(1.0/3*(ess[i]/len/3+1)/(ess[i]/len+1))*2, cost[n][i],  0.00000001);
            
            n = 3;
            // data = [(0|0,1),(1|0,0),(2|1,0),(0|1,2)]
            len = p[n][i].length;
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+1), p[n][i][0][0],  0.00000001);    // x = 0 | z = 0 0 0 
            assertEquals((ess[i]/len/3+1)/(ess[i]/len+1), p[n][i][0][1],  0.00000001);    // x = 1 | z = 0 0 0 
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+1), p[n][i][1][0],  0.00000001);    // x = 0 | z = 1 0 0
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+1), p[n][i][1][1],  0.00000001);    // x = 1 | z = 1 0 0
            assertEquals((ess[i]/len/3+1)/(ess[i]/len+1), p[n][i][2][0],  0.00000001);    // x = 0 | z = 0 1 1
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+1), p[n][i][2][1],  0.00000001);    // x = 1 | z = 0 1 1
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+0), p[n][i][3][0],  0.00000001);    // x = 0 | z = 1 1 1
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+0), p[n][i][3][1],  0.00000001);    // x = 1 | z = 1 1 1
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+0), p[n][i][4][0],  0.00000001);    // x = 0 | z = 0 2 2
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+0), p[n][i][4][1],  0.00000001);    // x = 1 | z = 0 2 2
            assertEquals((ess[i]/len/3+1)/(ess[i]/len+1), p[n][i][5][0],  0.00000001);    // x = 0 | z = 1 2 2
            assertEquals((ess[i]/len/3+0)/(ess[i]/len+1), p[n][i][5][1],  0.00000001);    // x = 1 | z = 1 2 2
            assertEquals(-Math.log(1.0/3)*4, cost[n][i],  0.00000001);
        }
    }
}
