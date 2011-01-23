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
//Test cases for BDE CPT Learner
//

//File: TestBDECPTLearner.java
//Author: rodo@dgs.monash.edu.au

package camml.test.core.models.cpt;

import java.util.Random;

import camml.core.library.StructureFN;
import camml.core.library.WallaceRandom;
import camml.core.models.cpt.CPT;
import camml.core.models.multinomial.MultinomialLearner;
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
 * @version $Revision: 1.2 $ $Date: 2006/08/22 03:13:39 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/core/models/cpt/TestCPT.java,v $
 */

public class TestCPT extends TestCase {

    /** */
    public TestCPT() { super(); }

    /**     */
    public TestCPT(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite( TestCPT.class );        
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
    public final void testGenerate() throws Exception {
        Value.Model model2 = MultinomialLearner.getMultinomialModel(binary);
        Value.Model model3 = MultinomialLearner.getMultinomialModel(ternary);
        
        // Create CPT with 2 parents (1 binary, 1 ternary)
        CPT cpt = new CPT(model2, new int[]{0,0}, new int[]{1,2});
        
        double y1[] = new double[]{0.4,0.6};
        double y2[] = new double[]{0.1,0.2,0.7};
        Value.Structured parent1Params = new StructureFN.FastContinuousStructure(y1);
        Value.Structured parent2Params = new StructureFN.FastContinuousStructure(y2);

        double cptY[][] = new double[][]{
            new double[] {0.9, 0.1},    // 0 0            
            new double[] {0.8, 0.2},    // 1 0
            new double[] {0.7, 0.3},    // 0 1
            new double[] {0.6, 0.4},    // 1 1
            new double[] {0.5, 0.5},    // 0 2
            new double[] {0.4, 0.6}        // 1 2
        } ;
        
        // Generate data for CPT's inputs
        Random rand = new WallaceRandom(new int[]{123,456});
        int n = 100000;        
        Value.Vector trivVec = new VectorFN.UniformVector(n,Value.TRIV);

        for (int ii = 0; ii < 10; ii++) {

            Value.Vector parentData1 = model2.generate(rand, parent1Params, trivVec);
            Value.Vector parentData2 = model3.generate(rand, parent2Params, trivVec);
            
            Value.Structured stats1 = (Value.Structured)model2.getSufficient(parentData1,trivVec);
            Value.Structured stats2 = (Value.Structured)model3.getSufficient(parentData2,trivVec);

            for (int i = 0; i < y1.length; i++) {
                assertEquals( y1[i]*n, stats1.doubleCmpnt(i), 0.1*n );    
            }

            for (int i = 0; i < y2.length; i++) {
                assertEquals( y2[i]*n, stats2.doubleCmpnt(i), 0.1*n );    
            }
            
            Value.Vector cptParams = cpt.makeCPTParams(cptY);
            Value.Vector z = new VectorFN.MultiCol(new Value.DefStructured(new Value.Vector[]{parentData1,parentData2}));
            Value.Vector data = cpt.generate(rand, cptParams, z);
            Value.Vector stats = (Value.Vector)cpt.getSufficient(data,z);
            //System.out.println("stats = " + stats);
            for (int i = 0; i < stats.length(); i++) {
                Value.Structured elt = (Value.Structured)stats.elt(i);
                double prob = (elt.doubleCmpnt(0)/(elt.doubleCmpnt(0)+elt.doubleCmpnt(1)));
                //System.out.print(""+ prob+"\t");
                assertEquals( 1.0-(i+1)*.1, prob, 0.1);
            }
            //System.out.println();

        }
        
    }
}
