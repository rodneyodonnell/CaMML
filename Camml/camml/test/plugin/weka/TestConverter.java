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

package camml.test.plugin.weka;

import weka.classifiers.functions.Logistic;
import weka.core.Instances;
import junit.framework.*;
import camml.core.models.logit.JulesLogit;
import camml.plugin.weka.Converter;
import cdms.core.*;


/**
 * Some simple tests.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.9 $ $Date: 2006/08/22 03:13:42 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/weka/TestConverter.java,v $
 */
public class TestConverter extends TestCase
{
    public TestConverter(String name) {    super(name); }
    
    Value.Vector cdmsData;
    Value.Vector xData;
    Value.Vector zData;
    int numVars;
    JulesLogit.Node node;
    Type.Structured sType;
    
    protected void setUp() throws Exception
    {
        node = new JulesLogit.Node();

        node.nd = 4;
        node.dads = new int[]{0,1,2,3}; //0,1,2,3};
        node.ndad = node.dads.length;

        
        
        //cdmsData = SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(1235), 
        //                                                              1000, 2,3,3 );
        //cdmsData = Converter.load("camml/test/iris.arff",true,true);
        cdmsData = Converter.load("camml/test/mydat.cas",true,true);
        
        int xVar = node.nd;
        int[] zVars = node.dads;
        
        // Work out how many components in cdmsData
        sType = (Type.Structured)((Type.Vector)cdmsData.t).elt;
        numVars = sType.cmpnts.length;
        
        // Exctact x and z from cdmsData
        xData = cdmsData.cmpnt(xVar);
        Value.Vector zArray[] = new Value.Vector[zVars.length];
        for (int i = 0; i < zArray.length; i++) { zArray[i] = cdmsData.cmpnt(zVars[i]); }
        
        
        // Copy labels from labels[] to labels2[] to be used in zArryay 
        String labels[] = sType.labels;
        String labels2[] = new String[labels.length-1];
        for (int i = 0; i < labels2.length; i++) { labels2[i] = labels[i]; }
        
        zData = new VectorFN.MultiCol( new Value.DefStructured(zArray,labels2));
        

    }

    /** Ensure data generated in setup is valid. */
    public void testSetup() {
        // Labels required in z
        String labels[] = ((Type.Structured) ((Type.Vector)zData.t).elt).labels; 
        assertTrue(labels != null);
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestConverter.class);
    }
    

    /** Convert a (discrete) CDMS vector to a weka Instances dataset */
    public void testConvertToWeka() throws Exception
    {
        Instances instances = Converter.vectorToInstances(cdmsData);
        instances.setClassIndex(numVars-1);
        Logistic logistic = new Logistic();
        
        logistic.buildClassifier(instances);
    }
}
