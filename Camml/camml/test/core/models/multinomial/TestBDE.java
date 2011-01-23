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
// Test functions for multinomial BDE
//

// File: TestBDE.java
// Author: rodo@dgs.monash.edu.au

package camml.test.core.models.multinomial;

import camml.core.models.multinomial.BDELearner;
import cdms.core.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test functions for multinomial BDE.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.2 $ $Date: 2006/08/22 03:13:40 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/core/models/multinomial/TestBDE.java,v $
 */

public class TestBDE extends TestCase {

    /** */
    public TestBDE() { super(); }

    /**     */
    public TestBDE(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite( TestBDE.class );        
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
    
    /** Test correct parameters are generated. */
    public final void testParams() throws Exception {
        // Create datasets
        Value.Vector data[] = new Value.Vector[] {
            new VectorFN.FastDiscreteVector(new int[] {0}, binary),
            new VectorFN.FastDiscreteVector(new int[] {1,1}, binary),
            new VectorFN.FastDiscreteVector(new int[] {0,0,1}, binary),
            new VectorFN.FastDiscreteVector(new int[] {0}, ternary),
            new VectorFN.FastDiscreteVector(new int[] {2}, ternary),
            new VectorFN.FastDiscreteVector(new int[] {2,2,1,0}, ternary)
        };

        // Create several BDE parameterizers with different ESS values.
        double ess[] = new double[] {1, 2, 2.5, 5, 9.6};
        BDELearner bl[] = new BDELearner[ess.length];
        for (int i = 0; i < bl.length; i++) {
            bl[i] = new BDELearner(ess[i]);
        }
        
        // Run bde parameterizer on each dataset/learner pair.
        Value.Structured msy[][] = new Value.Structured[data.length][bl.length];        
        Value.Structured y[][] = new Value.Structured[msy.length][msy[0].length];
        double cost[][] = new double[msy.length][msy[0].length];
        
        Value triv = Value.TRIV;
        for (int i = 0; i < data.length; i++) {            
            for (int j = 0; j < bl.length; j++) {                
                Value.Vector x = data[i];
                Value.Vector z = new VectorFN.UniformVector(x.length(),triv);
                // (model,stats,params) structure
                msy[i][j] = bl[j].parameterize(triv,x,z);
                // extract params from (m,s,y)
                y[i][j] = (Value.Structured)msy[i][j].cmpnt(2);
                
                // Calculate cost.
                cost[i][j] = bl[j].parameterizeAndCost(triv,x,z);
                assertEquals(cost[i][j], bl[j].msyCost(msy[i][j]));
            }
        }        
        
        for (int i = 0; i < msy.length; i++) {
            for (int j = 0; j < msy[i].length; j++) {
                // Assert probabilities sum to 1.0
                double sum = 0;
                for (int k = 0; k < y[i][j].length(); k++) {
                    sum += y[i][j].doubleCmpnt(k);
                }
                assertEquals( sum, 1.0, 0.0000000001);
            }
        }
                
        // Check parameters are correct.
        for (int i = 0; i < ess.length; i++) {
            // binary cases
            assertEquals((ess[i]/2+1)/(ess[i]+1), y[0][i].doubleCmpnt(0),  0.00000001);            
            assertEquals((ess[i]/2+0)/(ess[i]+2), y[1][i].doubleCmpnt(0), 0.00000001);
            assertEquals((ess[i]/2+2)/(ess[i]+3), y[2][i].doubleCmpnt(0), 0.00000001);
            
            // ternary cases
            assertEquals((ess[i]/3+1)/(ess[i]+1), y[3][i].doubleCmpnt(0), 0.00000001);
            assertEquals((ess[i]/3+0)/(ess[i]+1), y[3][i].doubleCmpnt(1), 0.00000001);
            assertEquals((ess[i]/3+0)/(ess[i]+1), y[4][i].doubleCmpnt(0), 0.00000001);
            assertEquals((ess[i]/3+0)/(ess[i]+1), y[4][i].doubleCmpnt(1), 0.00000001);
            assertEquals((ess[i]/3+1)/(ess[i]+4), y[5][i].doubleCmpnt(0), 0.00000001);
            assertEquals((ess[i]/3+1)/(ess[i]+4), y[5][i].doubleCmpnt(1), 0.00000001);
        }

        // Check costs are correct.
        for (int i = 0; i < ess.length; i++) {
            // binary cases
            assertEquals(-Math.log(0.5), cost[0][i],  0.00000001);            
            assertEquals(-Math.log(0.5*(ess[i]/2+1)/(ess[i]+1)), cost[1][i],  0.00000001);            
            assertEquals(-Math.log(0.5*(ess[i]/2+1)/(ess[i]+1)*(ess[i]/2+0)/(ess[i]+2)), 
                         cost[2][i],  0.00000001);            
            
            // ternary cases
            assertEquals(-Math.log(1.0/3), cost[3][i],  0.00000001);
            assertEquals(-Math.log(1.0/3), cost[4][i],  0.00000001);
            assertEquals(-Math.log(1.0/3*(ess[i]/3+1)/(ess[i]+1)*
                                   (ess[i]/3+0)/(ess[i]+2)*(ess[i]/3+0)/(ess[i]+3)), 
                         cost[5][i],  0.00000001);
            
            assertEquals((ess[i]/3+1)/(ess[i]+4), y[5][i].doubleCmpnt(0), 0.00000001);
            assertEquals((ess[i]/3+1)/(ess[i]+4), y[5][i].doubleCmpnt(1), 0.00000001);
        }
    }

}
