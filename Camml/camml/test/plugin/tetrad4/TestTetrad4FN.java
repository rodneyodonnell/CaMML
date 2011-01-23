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
// JUnit test routine for Tetrad4 interface to CDMS
//

// File: TestTetrad4FN.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.tetrad4;

import junit.framework.*;

import camml.plugin.tetrad4.*;
import camml.core.models.mixture.Mixture;


import cdms.core.*;
import camml.core.search.SearchDataCreator;
import camml.core.search.TOM;
import camml.plugin.rodoCamml.RodoCammlIO;
import camml.core.models.ModelLearner;
import camml.core.models.bNet.*;
import edu.cmu.tetrad.graph.Graph;



/**
 * Some simple tests.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision $ $Date: 2006/08/22 03:13:42 $
 * $Source $
 */
public class TestTetrad4FN extends TestCase
{
    //private static final double significance = 0.05;
    //private static final int depth = -1; // -1 = unlimited

    /** result of RunAll, contains [ges_graph,fci_graph,pc_graph,anneal_TOM,met_TOM].
     * runAllResult = null if testRunAll has not bee run yet.
     */
    static Object[] runAllResult = null;

    Value.Vector trainData;
    Value.Vector testData;

    public TestTetrad4FN(String name) 
    {
        super(name);
    }
    
    protected void setUp() 
    {
        int trainSize = 50;
        int testSize = 1000;


        java.util.Random rand = new java.util.Random( 123 );
        Value.Vector fullData =  SearchDataCreator.generateWallaceKorbStyleDataset( rand, trainSize+testSize, 1,2,3 );
        
        trainData = fullData.sub( 0, trainSize-1 );
        testData = fullData.sub( trainSize, fullData.length()-1 );
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestTetrad4FN.class);
    }
    
    public void testArcMatrixToGraph() {
        boolean[][] arcMatrix = new boolean[][] {
            new boolean[] {false, true, true},
            new boolean[] {true, false, false},
            new boolean[] {false, false, false}
        };
        
        Value.Vector data =  SearchDataCreator.getUncorrelatedDataset();
        Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
        Graph g = Tetrad4FN.arcMatrixToGraph( arcMatrix, dataType.labels );
        
        TOM[] toms = Tetrad4.enumerateDAGs(data, g); 
        assertEquals( toms.length, 1 );
        
        arcMatrix[0][2] = false;
        Graph g2 = Tetrad4FN.arcMatrixToGraph( arcMatrix, dataType.labels );
        assertEquals( 2, Tetrad4.enumerateDAGs(data, g2).length );
        
    }

    public void _testMixTetrad() throws ModelLearner.LearnerException {
        // Run searches.
        Value.Structured[] results = run( trainData );

        Value.Vector testX = testData;
        Value.Vector testZ = new VectorFN.UniformVector( testX.length(), Value.TRIV );
        for ( int i = 0; i < results.length; i++ ) {
            System.out.println( "\n--\n");
            // System.out.println( results[i] );
            Value.Model m = (Value.Model)results[i].cmpnt(0);
            Value.Vector y = (Value.Vector)results[i].cmpnt(1);

            if ( m instanceof Mixture ) {
                // Calculate logP on each mixture element.                
                for ( int j = 0; j < y.length(); j++ ) {
                    Value.Structured elt = (Value.Structured)y.elt(j);
                    Value.Model m2 = (Value.Model)elt.cmpnt(1);
                    Value.Vector y2 = (Value.Vector)elt.cmpnt(2);
                    System.out.println( "logP["+j+"] = "+m2.logP( testX, y2, testZ ) );
                }
                
                double logP = m.logP( testX, y, testZ );
                System.out.println( "logP[mix] = " + logP );            
            }
            else {

                double logP = m.logP( testX, y, testZ );
                System.out.println( "logP[no-mix] = " + logP );            
            }

        }
    }

    /** Run GES,PC,FCI,MML_Anneal,MML_Metropolis on cdmsData, return array of results. */
    public static Value.Structured[] run( Value.Vector cdmsData ) 
        throws ModelLearner.LearnerException {

        // Training data.
        Value.Vector trainX = cdmsData;
        Value.Vector trainZ = new VectorFN.UniformVector( trainX.length(), Value.TRIV );

        // Array of all learners used.
        ModelLearner learner[] = new ModelLearner[] {
            //            TetradLearner.ges,
            //             TetradLearner.gesMix,
            //             TetradLearner.fci,
            //             TetradLearner.fciMix,
            //             TetradLearner.pc,
            //             TetradLearner.pcMix,
            //             BNetLearner.metropolis,
            //             BNetLearner.mixMetropolis,
            //             BNetLearner.anneal

            TetradLearner.pcRepair,
            TetradLearner.pcMixRepair,
            TetradLearner.pcRerun,
            TetradLearner.pcMixRerun,

            TetradLearner.ges,
            TetradLearner.gesMix,
            TetradLearner.fci,
            TetradLearner.fciMix,

            BNetLearner.metropolis,
            BNetLearner.mixMetropolis,
            BNetLearner.anneal

        };

        // Run model learners.
        Value.Structured result[] = new Value.Structured[ learner.length ];
        for ( int i = 0; i < learner.length; i++ ) {
            // Calculate (model,stats,params) struct for each learner.
            result[i] = learner[i].parameterize( Value.TRIV, trainX, trainZ );
            
            // Convert to (model,params) structure.
            result[i] = new Value.DefStructured( new Value[] {result[i].cmpnt(0),result[i].cmpnt(2)} );
        }

        return result;

    }

    /** Run tetrad using GES, PC and FCI searches. */
    public static void main( String[] args ) throws Exception
    {
        // load data
        Value.Vector cdmsData;
        if ( args.length == 1 ) {  // load file in .cas or .arff format
            if (args[0].endsWith(".cas")) { 
                cdmsData = (Value.Vector)RodoCammlIO.load( args[0] ); 
            }
            else if ( args[0].endsWith(".arff") ) {
                // load .arff file, convert continuous to discrete and fix missing as required.
                cdmsData = camml.plugin.weka.Converter.load(args[0],true,true);
            }                
            else throw new RuntimeException("Unknown file type, must be .arff or .cas");
        }
        else {
            throw new IllegalArgumentException("Correct Syntax java <prog> file.[arff|cas]");
        }

        // run( cdmsData );
        
        TestTetrad4FN test = new TestTetrad4FN("TestTetrad4FN");
        test.trainData = cdmsData.sub( 0, cdmsData.length()/2 );
        test.testData = cdmsData.sub( (cdmsData.length()/2)+1, cdmsData.length()-1 );
        test._testMixTetrad();
        System.out.println("Train Size: " + test.trainData.length() );
        System.out.println("Test Size: " + test.testData.length() );
    }

}
