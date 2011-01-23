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
// JUnit test routine for weka interface to CDMS
//

// File: TestConverter.java
// Author: rodo@dgs.monash.edu.au

package camml.test.core.models.logit;

import java.util.Random;

import junit.framework.*;
import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner.LearnerException;
import camml.core.models.logit.JulesLogit;
import camml.core.models.logit.Logit;
import camml.core.models.logit.LogitFN;
import camml.core.models.logit.LogitLearner;
import camml.core.search.SearchDataCreator;
import camml.plugin.weka.Converter;
import cdms.core.*;


/**
 * Some simple tests.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.6 $ $Date: 2006/08/22 03:13:40 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/core/models/logit/TestLogit.java,v $
 */
public class TestLogit extends TestCase
{
    public TestLogit(String name) {    super(name); }
    
    Value.Vector cdmsData;
    JulesLogit.Node node4;

    protected void setUp() //throws Exception
    {
        node4 = new JulesLogit.Node();
        node4.nd = 4;
        node4.dads = new int[]{0,1,2,3}; //0,1,2,3};
        node4.ndad = node4.dads.length;
        
        try {
            //cdmsData = SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(1235), 
            //                                                              1000, 1,1,2 );
            //cdmsData = Converter.load("camml/test/iris.arff",true,true);
            cdmsData = Converter.load("camml/test/mydat.cas",true,true);
        } 
        catch (Exception e) {
            throw new RuntimeException("setUp() failed", e);
        }
        
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestLogit.class);
    }
    

    /** Test each node individually in a disconnected network.
     *  Assert MML costs have not changed since original code written. */
    public void testDisconnectedLogit() throws LearnerException {        
        JulesLogit logit = new JulesLogit();
        
        JulesLogit.Node n2 = new JulesLogit.Node();        
        n2.dads = new int[]{};
        n2.ndad = n2.dads.length;
        
        // Create empty parent set.
        Value.Structured emptyStruct = new Value.DefStructured(new Value[0]);
        Value.Vector emptyVec = new VectorFN.UniformVector(cdmsData.length(), emptyStruct);
        
        // MML costs of stating each variable with no parents.
        double result[] = new double[] { 1016.4340593886684, 726.9762083696897,
                                         674.3941786637096, 953.8733456512995, 605.0533179874037 }; 
        
        for (int i = 0; i < result.length; i++) {
            logit.nodeCost( cdmsData.cmpnt(i), emptyVec );
            assertEquals( result[i], logit.getMMLCost(), 0.00000000001 );            
        }
    }
    
    public void testConnectedLogit() throws LearnerException {

        JulesLogit logit = new JulesLogit();
        logit.nodeCost(LogitFN.getX(node4,cdmsData),LogitFN.getZ(node4,cdmsData));
        assertEquals(75.51879427495705, logit.getMMLCost(), 0.00000000001);

        
        // Calculate logP for various parent combinations.        
        double[][] prob = new double[][] {
            logit.logP(node4,new int[] {0,0,0,0}),
            logit.logP(node4,new int[] {0,0,0,1}),
            logit.logP(node4,new int[] {0,0,1,0}),
            logit.logP(node4,new int[] {0,1,0,0}),
            logit.logP(node4,new int[] {1,0,0,0})
        };
        // Assert values have not changed since original implementation.
        assertEquals( 0.9782399553233281,  prob[0][0], 0.00000000001);
        assertEquals( 0.9856438959920014,  prob[1][0], 0.00000000001);
        assertEquals( 0.054737281665359336,prob[2][0], 0.00000000001);
        assertEquals( 0.9832851680280267,  prob[3][0], 0.00000000001);
        assertEquals( 0.9999262247685651,  prob[4][0], 0.00000000001);

        // Assert both implementations give equivelant results.
        double[] c = logit.c;
        double[][][] d = logit.d;
        double[][] prob2 = new double[][] {
            LogitFN.logP(c,d, new int[] {0,0,0,0}),
            LogitFN.logP(c,d, new int[] {0,0,0,1}),
            LogitFN.logP(c,d, new int[] {0,0,1,0}),
            LogitFN.logP(c,d, new int[] {0,1,0,0}),
            LogitFN.logP(c,d, new int[] {1,0,0,0})                
        };
        
        for (int i = 0; i < prob2.length; i++) {
            for (int j = 0; j < prob2[i].length; j++) {                
                assertEquals(prob[i][j],Math.exp(prob2[i][j]),0.0000000001);    
            }            
        }
    }

    /** Assert d == makeArray(maveVec(d)) */
    public void testParamConverter() throws LearnerException {
        // Run julesLearner
        JulesLogit logit = new JulesLogit();
        logit.nodeCost(LogitFN.getX(node4,cdmsData),LogitFN.getZ(node4,cdmsData));

        Value.Structured params = logit.getParams();
        
        Value.Vector dVec = (Value.Vector)params.cmpnt(1);        
        Object dArray = LogitFN.makeArray(dVec);

        assertEquals(dVec,LogitFN.makeVec(dArray));
        double[][][] d = (double[][][])dArray;
        assertEquals(dVec,LogitFN.makeVec(d));
    }
    
    /** Compare logP from logit model with that calculated by JulesLogit */
    public void testLogitModelLogP() throws LearnerException {
        // Run JulesLogit
        JulesLogit logit = new JulesLogit();
        logit.nodeCost(LogitFN.getX(node4,cdmsData),LogitFN.getZ(node4,cdmsData));
        
        Value.Structured y = logit.getParams();
        Value.Vector x = LogitFN.getX(node4,cdmsData);
        Value.Vector z = LogitFN.getZ(node4,cdmsData);
        
        assertEquals(Logit.logit.logP(x,y,z), -logit.emlcost,0.0000000001);
    }

    /**  */
    public void testLogitModelGenerate() throws LearnerException {
        Random r = new Random(123);

        // Run JulesLogit
        JulesLogit logit = new JulesLogit();
        logit.nodeCost(LogitFN.getX(node4,cdmsData),LogitFN.getZ(node4,cdmsData));
        
        // Extract parameters
        Value.Structured y1 = logit.getParams();
        Value.Vector x1 = LogitFN.getX(node4,cdmsData);
        Value.Vector z = LogitFN.getZ(node4,cdmsData);
        
        // Generate new training set
        Value.Vector x2 = Logit.logit.generate(r,y1,z);
        
        // Run JulesLogit on new dataset
        JulesLogit logit2 = new JulesLogit();         
        logit2.nodeCost(x2,z);        
        
        Value.Structured y2 = logit2.getParams();
        
        // Both models should state the data they were learned from more
        // efficiently than the other model.
        Logit m = Logit.logit;
        assertTrue( -m.logP(x1,y1,z) < -m.logP(x1,y2,z) );
        assertTrue( -m.logP(x2,y2,z) < -m.logP(x2,y1,z) );
                
        System.out.println("y1 = " + y1);
        System.out.println("y2 = " + y2);        
        
    }

    
    public void testLogitLearner() throws LearnerException, Exception {
        LogitLearner ll = LogitLearner.logitLearner;
        
        Value.Vector x = LogitFN.getX(node4,cdmsData);
        Value.Vector z = LogitFN.getZ(node4,cdmsData);

        Value.Structured msy = ll.parameterize(Value.TRIV,x,z);
        assertTrue(msy != null);
        //System.out.println(msy);
    }
    
    /** Test logitLearner inside MetropolisSearch*/
    /** Run CaMML using logit model as MMLModelLearner */
    public void testRunLogitAsCammlLearner() throws Exception {
        Random r = new WallaceRandom(new int[] {123,456});
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset( r, 1000, 1,2,3 );
        //Value.Vector data = Converter.load("/home/rodo/Repository/data/UCI/small/diabetes.arff",true,true);
        //Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(1235), 10000, 2,2,2 );
        //Value.Vector data = Converter.load("/home/rodo/Repository/data/UCI/small/breast-cancer.arff",true,true);
        //Value.Vector data = Converter.load("/home/rodo/Repository/data/UCI/small/breast-w.arff",true,true);
        //Value.Vector data = Converter.load("/home/rodo/Repository/data/UCI/medium/letter.symbolic.arff",true,true);
        LogitLearner.logitBNetLearner.parameterize(Value.TRIV,data,data);
    }
}
