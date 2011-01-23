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
// Test wekaLearner plugin.
//

// File: TestWekaLearner.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.weka;

import java.io.FileReader;
import java.util.Random;

import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner;
import camml.core.models.dual.DualLearner;
import camml.core.search.SearchDataCreator;
import camml.plugin.weka.Converter;
import camml.plugin.weka.WekaLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;

import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test wekaLearner plugin.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.6 $ $Date: 2006/08/22 03:13:42 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/weka/TestWekaLearner.java,v $
 */

public class TestWekaLearner extends TestCase {

    public TestWekaLearner(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite(TestWekaLearner.class);
    }

    Value.Vector cdmsData;
    Value.Vector xData;
    Value.Vector zData;
    int numVars;
    
    protected void setUp() throws Exception
    {
        //cdmsData = SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(125), 1000, 3,3,3 );
        cdmsData = Converter.load("camml/test/iris.arff",true,true);
        
        
        // Work out how many components in cdmsData
        Type.Structured sType = (Type.Structured)((Type.Vector)cdmsData.t).elt;
        numVars = sType.cmpnts.length;
        
        // Exctact x and z from cdmsData
        xData = cdmsData.cmpnt(numVars-1);
        Value.Vector zArray[] = new Value.Vector[numVars-1];
        for (int i = 0; i < zArray.length; i++) { zArray[i] = cdmsData.cmpnt(i); }
        
        
        // Copy labels from labels[] to labels2[] to be used in zArryay 
        String labels[] = sType.labels;
        String labels2[] = new String[labels.length-1];
        for (int i = 0; i < labels2.length; i++) { labels2[i] = labels[i]; }
        
        zData = new VectorFN.MultiCol( new Value.DefStructured(zArray,labels2));
    }

    /** Load dataset and test Weka Logistic classifier*/
    public void _testRunWeka() throws Exception
    {    
        Instances instances = new Instances(new FileReader("camml/test/iris.arff"));
        instances.setClassIndex(4);
        Logistic logistic = new Logistic();
        logistic.buildClassifier(instances);
    }

    /** Run weka learner on a dataset. */
    public void testRunWekaLearner() throws Exception
    {
        ModelLearner ml = new WekaLearner(new Logistic());
        ml.parameterize(Value.TRIV,cdmsData.cmpnt(0),cdmsData);
    }

    /** Run weka learner on a dataset. */
    public void testRunWekaCoster() throws Exception
    {
        // Create WekaLearner
        //ModelLearner ml = new WekaLearner(new Logistic());
        ModelLearner ml = new WekaLearner(new J48());

        // Create empty parent vector
        Value.Vector emptyZ = new VectorFN.ConstantVector(xData.length(),new Value.DefStructured(new Value[] {}));
        
        // Run wekaLearner
        Value.Structured msy = ml.parameterize(Value.TRIV,xData,zData);
        //double cost1 = ml.msyCost(msy);
        double cost1 = ml.parameterizeAndCost(Value.TRIV,xData,zData);
        double logP1 = ((Value.Model)msy.cmpnt(0)).logPSufficient(msy.cmpnt(1),msy.cmpnt(2));
        //System.out.println("cost1 = " + cost1);
        //System.out.println("logP = " + logP1 );

        // Run wekaLearner
        Value.Structured msy2 = ml.parameterize(Value.TRIV,xData,emptyZ);
        //double cost2 = ml.msyCost(msy2);
        double cost2 = ml.parameterizeAndCost(Value.TRIV,xData,emptyZ);
        double logP2 = ((Value.Model)msy2.cmpnt(0)).logPSufficient(msy2.cmpnt(1),msy2.cmpnt(2));
        //System.out.println("cost = " + cost2);        
        //System.out.println("logP = " + logP2);


         
        //Value.Structured msy3 = CPTLearner.adaptiveCPTLearner.parameterize(Value.TRIV,xData,zData);
        ml = DualLearner.dualLearner;
        //double cost3 = ml.msyCost(msy3);
        Value.Structured msy3 = ml.parameterize(Value.TRIV,xData,zData);                
        double cost3 = ml.parameterizeAndCost(Value.TRIV,xData,zData);
        double logP3 = ((Value.Model)msy3.cmpnt(0)).logPSufficient(msy3.cmpnt(1),msy3.cmpnt(2));
        
        //System.out.println("cost = " + cost3);        
        //System.out.println("logP = " +  logP3 );

        
        assertTrue("Unexpedted result: Learning with empty parent set "+
                   "outperforms learning with non-empty parent set.",cost1 <= cost2);
        assertTrue(cost1 > 0); assertTrue(logP1 < 0);
        assertTrue(cost2 > 0); assertTrue(logP2 < 0);
        assertTrue(cost3 > 0); assertTrue(logP3 < 0);
    }

    /** Run weka learner on a dataset. */
    public void testWekaRelearn() throws Exception
    {
        // Create WekaLearner
        ModelLearner ml1 = WekaLearner.wekaLogitLearner; //new WekaLearner(new Logistic());
        ModelLearner ml2 = ml1; //new WekaLearner(new Logistic());
        //ModelLearner ml = new WekaLearner(new J48());

        // Run wekaLearner to learn a model
        Value.Structured msy1 = ml1.parameterize(Value.TRIV,xData,zData);
        Value.Model model1 = (Value.Model)msy1.cmpnt(0);
        Value stats1 = msy1.cmpnt(1);
        Value params1 = msy1.cmpnt(2);
        
        //System.out.println("params1 = " + ((Value.Obj)params1).getObj());
        
        // Renerate data from the model learned
        //Value.Vector xData2 = model1.generate(new Random(123),xData.length(),params1,zData);
        Value.Vector xData2 = model1.generate(new Random(1236),params1,zData);

        // Relearn parameters based on the newly generated data.
        Value.Structured msy2 = ml2.parameterize(Value.TRIV,xData2,zData);
        Value.Model model2 = (Value.Model)msy2.cmpnt(0);
        Value stats2 = msy2.cmpnt(1);
        Value params2 = msy2.cmpnt(2);

        double logP1 = model1.logPSufficient(stats1,params1);
        double logP2 = model2.logPSufficient(stats1,params2);
        //System.out.println("logP | data1 = " + logP1 + "\t" + logP2);

        double logP3 = model1.logPSufficient(stats2,params1);
        double logP4 = model2.logPSufficient(stats2,params2);
        //System.out.println("logP | data2 = " + logP3 + "\t" + logP4);
        
        //System.out.println("params1 = " + ((Value.Obj)params1).getObj());
        //System.out.println("params2 = " + ((Value.Obj)params2).getObj());

        System.out.println("xData.t = " + xData.t);
        System.out.println("zData.t = " + zData.t);
        System.out.println("ml1.sCost(model1,stats1,params1) = " + ml1.sCost(model1,stats1,params1));
        System.out.println("ml1.sCost(model2,stats2,params2) = " + ml1.sCost(model2,stats2,params2));
        
        
        // Assert logP on training set will be lower than on 'regenerated' training set.
        assertTrue(-logP1 < -logP2);
        assertTrue(-logP4 < -logP3);
    }

    /** Run CaMML using logit model as MMLModelLearner */
    public void testRunWekaAsCammlLearner() throws Exception {
        Random r = new WallaceRandom(new int[] {123,456});
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset( r, 1000, 1,2,3 );
        WekaLearner.wekaBNetLogitLearner.parameterize(Value.TRIV,data,data);
    }
}
