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
// Class Contains tests for MetropolisSearch and a main() function for convenience.
//

// File: TestMetropolisSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import java.io.FileNotFoundException;

import junit.framework.*;

import cdms.core.*;
import camml.core.search.*;

import cdms.plugin.search.*;
import camml.core.library.BlockingSearch;
import camml.core.library.Library;
import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner;

import camml.plugin.rodoCamml.*;
import camml.core.models.bNet.*;
import camml.core.models.cpt.CPTLearner;

import javax.swing.*;



public class TestMetropolisSearch extends TestCase
{
    public TestMetropolisSearch(String name) 
    {
        super(name);
    }
    
    // Create some data sets to test with.
    protected void setUp() 
    {
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestMetropolisSearch.class);
    }
    
    /** 
     * Test Metropolis search.  This must be done in a blocking way, as a non-blocking search
     * will spawn a new thread and JUnit will think everything is fine (even if it is not).
     */
    public void _testBlockingSearch()
    {
        java.util.Random rand = new java.util.Random(123);
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        
        MetropolisSearch metropolisSearchObject = 
            new MetropolisSearch( rand, dataset, 
                                  SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner);
        Search blockingSearch = new BlockingSearch( metropolisSearchObject );
        
        blockingSearch.start();
    }
    
    public void testSearchInterface()
    {
        System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

        //JFrame f = new JFrame("Metropolis Search Object");
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        Search s = new BlockingSearch( new MetropolisSearch( new java.util.Random(12345), dataset,
                                                             SearchPackage.mlCPTLearner, 
                                                             SearchPackage.mmlCPTLearner ));
        //f.getContentPane().add(new SearchControl(s,new JLabel("MetropolisSearch")));
        //f.pack();
        //f.setVisible(true);
        
        s.start();
    }
    
    
    /** 
     * Test main function. <br>
     * Useage : java camml.test.SearchPackage.TestMetropolisSearch datafile <br>
     *          or java camml.test.SearchPackage.TestMetropolisSearch seed numCases n0 n1 n2
     *             to generate a n0*n1*n2 matrix ov correlated variables.
     */
    public static void main( String args[] ) 
        throws java.io.IOException, ModelLearner.LearnerException,  
               FileNotFoundException, Exception
    {
        // load/generate data
        Value.Vector data;
        if ( args.length == 1 ) {  // load file in .cas or .arff format
            if (args[0].endsWith(".cas")) {
                data = (Value.Vector)RodoCammlIO.load( args[0] );
            }
            else if ( args[0].endsWith(".arff") ) {
                // load .arff file, convert continuous to discrete and fix missing as required.
                data = camml.plugin.weka.Converter.load(args[0],true,true);
            }                
            else throw new RuntimeException("Unknown file type, must be .arff or .cas");
        }
        // generate a n0*n1*n2 array of correlated variables.  Each node has node[i-1][j][k],
        // node[i][j-1][k] anm node[i][j][k-1] as it's parents.
        else if (args.length == 5) {
            int seed = Integer.parseInt(args[0]);
            int samples = Integer.parseInt(args[1]);
            int n0 = Integer.parseInt(args[2]);
            int n1 = Integer.parseInt(args[3]);
            int n2 = Integer.parseInt(args[4]);
            data = SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(seed),
                                                                     samples,n0,n1,n2);
        }
        else {
            throw new IllegalArgumentException("Correct Syntax java <prog> file.[arff|cas]");
        }
        
        WallaceRandom rand = new WallaceRandom(new int[] {123,456} );
        
        //////////////////////////////////////////////
        // Which model learners should be used?        //
        //////////////////////////////////////////////
        
        // CPT learner using costing function from oldCamml
        ModelLearner mmlModelLearner = CPTLearner.mmlAdaptiveCPTLearner;
        //ModelLearner mmlModelLearner = ForcedSplitDTreeLearner.multinomialDTreeLearner;
        //ModelLearner mmlModelLearner = SearchPackage.dualLearner;
        
        // mlCPTLearner is used to calculate ML costs of networks.
        ModelLearner mlModelLearner = CPTLearner.mlMultinomialCPTLearner;
        
        // Should the graphical interface be used? 
        boolean gui = false;
        
        // Create a (Metroplois) SearchObject and a Search to run in.
        final MetropolisSearch met = 
            new MetropolisSearch( rand, data, mlModelLearner, mmlModelLearner );
        final Search s = new Search( met );
        final BNetInferenceVisualiser vis;

        // Create visualiser if required.
        if ( gui ) {
            vis = new BNetInferenceVisualiser( met.getBNet() );
            vis.updateParams( met.getCurrentParams( met.getMMLModelLearner() ),
                              met.getBNet().makeMissingStruct() );
        }
        else {
            vis = null;
        }
        
        final String args0 = args[0].substring(args[0].lastIndexOf("/")+1);
        
        /** Add a SearchListener updating BNet visualisation after completion. */
        s.addSearchListener( new Search.SearchListener() {
                /** ignore message */
                public void reset( Search sender ) { }
                
                /** ignore message */
                public void beforeEpoch( Search sender ) { }

                /** ignore message */
                public void afterEpoch( Search sender ) { }

                /** Print resulta and update visualiser after sampling finishes. */
                public void onCompletion( Search sender ) {
                    
                    // get final list of MMLECs formed during sampling
                    Value.Vector fullResults = met.getResults();
                    
                    // Extract group of SECs from best MMLECs
                    Value.Vector bestGroupOfSEC = 
                        (Value.Vector)((Value.Structured)fullResults.elt(0)).cmpnt(0);
                    
                    // Extract group of DAGs from best representative SEC
                    Value.Vector bestSEC = 
                        (Value.Vector)((Value.Structured)bestGroupOfSEC.elt(0)).cmpnt(0);
                    
                    // Extract representative DAG from representative SEC
                    Value.Structured bestDAG = (Value.Structured)bestSEC.elt(0);
                    
                    // Extrace parameters from DAG
                    Value.Vector bestParams = (Value.Vector)bestDAG.cmpnt(1);
                    
                    // Update parameters for visualisation.
                    if ( vis != null ) {
                        vis.updateParams( bestParams, met.getBNet().makeMissingStruct() );
                    }
                    
                    // Print reaults.
                    System.out.println( "fullResults ");
                    System.out.println( fullResults + "\n");
                    
                    System.out.println("SECs");
                    System.out.println( bestGroupOfSEC + "\n" );
                    
                    System.out.println("DAGs");
                    System.out.println( bestSEC + "\n" );
                
                    //System.out.println("Netica");
                    //System.out.println( bNet.exportNetica(args[0],bestParams) );
                    BNet bNet = met.getBNet();
                    try {
                        java.io.Writer out = new java.io.FileWriter(args0+".dne");
                        //System.out.println(bNet.exportNetica(args0,bestParams));
                        out.write(bNet.exportNetica(args0,bestParams));
                        out.flush();
                    }
                    catch ( java.io.IOException e ) { System.out.println("Could not export netica file."); }
                
                    MetropolisSearch met = (MetropolisSearch)sender.getSearchObject();
                    met.caseInfo.nodeCache.printStats(2);
                
                    Library.serialise._apply(args0+".serialise",fullResults);

                }
            } );

        // Create and display JFrame containing search buttons and visualiser.
        // User must press "Start" for search to begin.
        if ( gui ) {        
            JFrame f = new JFrame("Metropolis Search Object");
            f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE  );
            f.getContentPane().add(new SearchControl(s,vis));
            f.pack();
            f.setVisible(true);
        }
        else { // if gui == false, start the search.
            s.start();
        }
    }
}
