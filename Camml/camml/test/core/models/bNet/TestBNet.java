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
// JUnit test routines for BNet model
//

// File: TestBNet.java
// Author: rodo@dgs.monash.edu.au
// Created on 28/02/2005

package camml.test.core.models.bNet;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Random;

import camml.core.models.ModelLearner.LearnerException;
import camml.core.models.bNet.BNet;
import camml.core.models.bNet.BNetGenerator;
import camml.core.models.bNet.BNetLearner;
import camml.core.models.bNet.BNetStochastic;
import camml.core.models.normal.NormalLearner;
import camml.core.search.SearchDataCreator;
import camml.core.search.SearchPackage;
import camml.core.search.TOM;
import camml.plugin.augment.AugmentFN;
import camml.plugin.augment.AugmentFN2;
import camml.plugin.augment.AugmentFN3;
import camml.plugin.netica.BNetNetica;
import camml.plugin.netica.NeticaFn;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author rodo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestBNet extends TestCase {

    protected final static Random rand = new Random(123);

    /** BNet model used in testing*/
    protected static BNet model;
    
    /** Parameters corresponding to model */
    protected static Value.Vector params1;
    protected static Value.Vector params2;
    
    
    /** */
    public TestBNet() { super(); }

    /**     */
    public TestBNet(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite( TestBNet.class );        
    }

    protected void initParams() throws LearnerException {
        // Initialise search object.
        Value.Vector data1 = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,1,3,3);
        Value.Vector data2 = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,1,3,3);

        BNetLearner learner = new BNetLearner( SearchPackage.mlCPTLearner,
                                               SearchPackage.mmlCPTLearner, false, false );
        learner.setSearchType( "Anneal" );
        
        // Run search
        Value.Structured msy1 = learner.parameterize(Value.TRIV, data1, data1 );
        Value.Structured msy2 = learner.parameterize(Value.TRIV, data2, data2 );
        
        // Extract model and params from result.
        model = (BNet)msy1.cmpnt(0);
        params1 = (Value.Vector)msy1.cmpnt(2);            
        
        params2 = (Value.Vector)msy2.cmpnt(2);            

    }
        
    /** Initialise model and params with sample data (uses AnnealSearch). */
    protected void setUp() throws Exception { 
        if ( model == null || params1 == null ) {
            initParams();
        }
    }
    
    /** Test exporting of netica files. */
    public final void testExportNetica() throws Exception {
        // Create netica String
        String neticaString = model.exportNetica("tempNet",params1);

        // Save as test.dnet
        File f = new File("test.dnet");        
        FileWriter out = new FileWriter(f);
        out.write( neticaString );
        out.flush(); out.close();
        
        // Read network back in
        NeticaFn.LoadNet._apply( "test.dnet" );
        f.delete();
    }
    
    /** Test exporting of augmented netica files (which use DTrees). */
    public final void testAugmentExportNetica() throws Exception {
        Value.Structured augMY = AugmentFN2.augment2.apply(model,params1,params1);                
        
        BNet augModel = (BNet)augMY.cmpnt(0);
        Value.Vector augParams = (Value.Vector)augMY.cmpnt(1);        
        
        // Create netica String
        String neticaString = augModel.exportNetica("tempNet",augParams);
        
        // Save as test.dnet
        File f = new File("test.dnet");        
        FileWriter out = new FileWriter(f);
        out.write( neticaString );
        out.flush(); out.close();
        
        // Read network back in
        NeticaFn.LoadNet._apply( "test.dnet" );
        f.delete();
    }

    /** Bugfix test: Ensure ordering from params & model are the same. */
    public final void testAugment3() throws Exception {

        // Original model with two sets of parameters
        BNetNetica m = new BNetNetica( model.getDataType() );
        
        // Augment models.
        Value.Structured augmy1 = AugmentFN3.augment3.apply(m,params1,params1);
        Value.Vector augY1 = (Value.Vector)augmy1.cmpnt(1);
        BNetNetica augM = (BNetNetica)augmy1.cmpnt(0);
        
        // Assert that model-type & net labels are the same.
        Type.Structured sType1 = augM.getDataType();
        Value.Vector labels = augY1.cmpnt(0);
        for ( int i = 0; i < sType1.labels.length; i++) {
            assertEquals( sType1.labels[i], ((Value.Str)labels.elt(i)).getString() );
        }
    }
    
    /** Test exporting of augmented netica files (which use DTrees). */
    public final void testAugmentDatatype() throws Exception {
        
        Value.Structured[][] myArray = new Value.Structured[4][2];
        
        myArray[0][0] = new Value.DefStructured( new Value[] {model,params1} ); // KL
        myArray[0][1] = new Value.DefStructured( new Value[] {model,params2} ); // KL

        myArray[1][0] = AugmentFN.augment.apply(model,params1);                 // CKL1
        myArray[1][1] = AugmentFN.augment.apply(model,params2);                 // CKL1
        
        myArray[2][0] = AugmentFN2.augment2.apply(model,params1,params1);        // CKL2
        myArray[2][1] = AugmentFN2.augment2.apply(model,params2,params1);        // CKL2
        
        myArray[3][0] = AugmentFN3.augment3.apply(model,params1,params1);        // CKL3
        myArray[3][1] = AugmentFN3.augment3.apply(model,params2,params1);        // CKL3
        
        
        
        for ( int i = 0; i < myArray.length; i++) {
            BNet bNet = (BNet)myArray[i][0].cmpnt(0);
            Value.Vector p1 = (Value.Vector)myArray[i][0].cmpnt(1);
            Value.Vector p2 = (Value.Vector)myArray[i][1].cmpnt(1);
            //double kl = bNet.klExact(p1,p2);
            //System.out.println( "CKL"+i+" = " + kl );
        }
        
        System.out.println("myArray[0][0].cmpnt(1) = " + ((Value.Vector)myArray[0][0].cmpnt(1)).cmpnt(1) );
        System.out.println("myArray[0][1].cmpnt(1) = " + ((Value.Vector)myArray[0][1].cmpnt(1)).cmpnt(1) );
        
        Value.Structured augMY = AugmentFN3.augment3.apply(model,params1,params1);                
        


        
        
        //        BNet augModel = (BNet)augMY.cmpnt(0);
        //        Value.Vector augParams = (Value.Vector)augMY.cmpnt(1);        
        //
        //        for (int i = 0; i < augParams.length(); i++) {
        //            System.out.println("augMY.elt(i) = " + augParams.elt(i));
        //        }
        //
        //        Value.Vector data = augModel.generate( rand, 10, augParams, Value.TRIV );
        //        for ( int i = 0; i < data.length(); i++ ) {
        //            System.out.println("data = " + data.elt(i));
        //        }
    }


    public final void testExactKL() throws Exception {
        // Initialise search object.
        Value.Vector data1 = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,2,2,2);
        Value.Vector data2 = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,2,2,2);
    
        // Generate parameters from identical TOMs. 
        TOM tom1 = new TOM(data1);
        TOM tom2 = new TOM(data2);            
                
        BNet bNet = new BNetStochastic((Type.Structured)((Type.Vector)data1.t).elt);
        BNet bNetNetica = new BNetNetica(bNet.getDataType());
        
        for (int i = 0; i < 10; i++) {
            tom1.randomOrder(rand);
            tom1.randomArcs(rand,2.0/tom1.getNumNodes());
            
            tom2.randomOrder(rand);
            tom2.randomArcs(rand,2.0/tom1.getNumNodes());                    
            
            //System.out.println(tom1);
            //System.out.println(tom2);

            Value.Vector params1 = tom1.makeParameters(SearchPackage.mmlCPTLearner);
            Value.Vector params2 = tom2.makeParameters(SearchPackage.mmlCPTLearner);
        
            double kl1 = bNet.klExactSlow(params1,params2);
            double kl2 = bNet.klExact(params1,params2);
            double kl3 = bNetNetica.klExact(params1,params2);
            assertEquals(kl1,kl2,0.00000000001);
            assertEquals(kl2,kl3,0.000001);
        }
    }

    public final void testExactKLNetica() throws Exception {

        for ( int size = 4; size <= 4; size += 1) {
        
            TOM tom[] = new TOM[10];
            Value.Vector data[] = new Value.Vector[tom.length];
            Value.Vector y[] = new Value.Vector[tom.length];
            for ( int i = 0; i < tom.length; i++) {
                data[i] = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,1,1,size);
                tom[i] = new TOM(data[i]);            
                tom[i].randomOrder(rand);
                tom[i].randomArcs(rand,2.0/tom[0].getNumNodes());
                y[i] = tom[i].makeParameters(SearchPackage.mmlCPTLearner);
            }

            BNet bNet = new BNetStochastic((Type.Structured)((Type.Vector)data[0].t).elt);
            BNetNetica bNetNetica = new BNetNetica( bNet.getDataType() );

            long tSlow = 0, tExact = 0, tNetica = 0, tNeticaVec = 0;
        
            double klExactSlow[][] = new double[tom.length][tom.length];
            double klExact[][]  = new double[tom.length][tom.length];
            double klNetica[][] = new double[tom.length][tom.length];
            double klNeticaVec[][] = new double[tom.length][];
            for (int i = 0; i < klExactSlow.length; i++) {
                tNeticaVec -= System.currentTimeMillis();
                klNeticaVec[i] = BNetNetica.exactKLNetica( bNet, y, i );
                tNeticaVec += System.currentTimeMillis();
                for (int j = 0; j < klExactSlow[i].length; j++) {
                    tSlow -= System.currentTimeMillis();
                    klExactSlow[i][j] = bNet.klExactSlow(y[i],y[j]);
                    tSlow += System.currentTimeMillis();
                    tExact -= System.currentTimeMillis();
                    klExact[i][j] = bNet.klExact(y[i],y[j]);
                    tExact += System.currentTimeMillis();
                    tNetica -= System.currentTimeMillis();
                    klNetica[i][j] = bNetNetica.klExact(y[i],y[j]);
                    tNetica += System.currentTimeMillis();
                
                    assertEquals( klExactSlow[i][j], klExact[i][j], 0.00001 );
                    assertEquals( klExactSlow[i][j], klNetica[i][j], 0.00001 );
                    assertEquals( klExactSlow[i][j], klNeticaVec[i][j], 0.00001 );
                }            
            }
        
            System.out.print("size = " + size);
            System.out.print("\ttSlow = " + tSlow);
            System.out.print("\ttExact = " + tExact);
            System.out.print("\ttNetica = " + tNetica);
            System.out.println("\ttNeticaVec = " + tNeticaVec);
        }
    }

    
    public final void _testStochasticAccuracy() throws Exception {
        int folds = 20;
        
        int kArray[] = new int[] {5,10,15,20,30,40,60,80,120,160};
        
        for ( int i = 0; i < kArray.length; i++) {
            //System.out.print("k = " + k + "\t");
            int k = kArray[i];
            
            double[] diff = new double[folds];
            for (int fold = 0; fold < folds; fold++) {

                Value.Structured z = new Value.DefStructured( new Value[] {
                        new VectorFN.UniformVector(k,new Value.Discrete(2)), // arity
                        new Value.Continuous(2.0/k), new Value.Continuous(1.0)
                    });
                Value.Vector myVec = BNetGenerator.generator.generate(rand,2,Value.TRIV,z);
                Value.Structured my1 = (Value.Structured)myVec.elt(0);
                Value.Structured my2 = (Value.Structured)myVec.elt(1);
                
                Value.Vector params1 = (Value.Vector)my1.cmpnt(1);
                Value.Vector params2 = (Value.Vector)my2.cmpnt(1);

                BNet bNet = (BNet)my1.cmpnt(0);
                BNet bNetNetica = new BNetNetica(bNet.getDataType());

                double kl2 = bNetNetica.klExact(params1,params2);
                double kl3 = bNet.klStochastic(params1,params2);
                
                diff[fold] = kl2 - kl3;
            }
            double[] muSd = NormalLearner.getStats(diff);
            System.out.println( "delta[" + k + "] = \t" + Arrays.toString(muSd));
        }
    }

}
