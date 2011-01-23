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
// Test Simple Search for CaMML
//

// File: TestAnnealSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import camml.core.search.*;
import camml.core.models.bNet.*;
import camml.core.library.BlockingSearch;

import camml.core.models.ModelLearner;


import camml.plugin.rodoCamml.RodoCammlIO;
import cdms.plugin.search.*;


import javax.swing.*;



public class TestAnnealSearch extends TestCase
{
    public TestAnnealSearch(String name) 
    {
        super(name);
    }
    
    // Create some data sets to test with.
    protected void setUp() 
    {
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestAnnealSearch.class);
    }
    
    /** 
     * Test Anneal search.  This must be done in a blocking way, as a non-blocking search
     * will spawn a new thread and JUnit will think everything is fine (even if it is not).
     */
    public void testBlockingSearch()
    {
        System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

        java.util.Random rand = new java.util.Random(123);
        
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        AnnealSearch quickSearchObject = new AnnealSearch( rand, dataset,
                                                           SearchPackage.mlCPTLearner,
                                                           SearchPackage.mmlCPTLearner );
        Search blockingSearch = new BlockingSearch( quickSearchObject );
        
        blockingSearch.start();
    }
    
    /*
      public void testSearchInterface()
      {
      JFrame f = new JFrame("Anneal Search Object");
      Value.Vector dataset = 
      SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
      Search s = new Search( new AnnealSearch( new java.util.Random(12345), dataset,
      SearchPackage.mlCPTLearner,
      SearchPackage.mmlCPTLearner ));
      f.getContentPane().add(new SearchControl(s,new JLabel("AnnealSearch")));
      f.pack();
      f.setVisible(true);
        
      s.start();
      }
    */
    
    
    
    
    /** Test main function. */
    public static void main( String args[] )
        throws ModelLearner.LearnerException, java.io.IOException
    {
        Value.Vector data;    
        if ( args.length == 1 ) {  // load a old format camml file
            data = (Value.Vector)RodoCammlIO.load( args[0] );
        }
        else if (args.length == 2) {
            int seed = Integer.parseInt(args[0]);
            int samples = Integer.parseInt(args[1]);
            data = SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(seed),samples,2,2,2);
        }
        else {
            throw new IllegalArgumentException("Correct Syntax java <prog> seed samples lookahead");
        }
        
        
        java.util.Random rand = new java.util.Random( 123 );
        
        ModelLearner mmlModelLearner = SearchPackage.mmlCPTLearner;
        ModelLearner mlModelLearner = SearchPackage.mlCPTLearner;
        
        Search s = new Search( new AnnealSearch( rand, data,
                                                 mlModelLearner, mmlModelLearner ) );    
        final AnnealSearch anneal = (AnnealSearch)s.getSearchObject();       
        
        
        
        
        final BNetInferenceVisualiser vis = new BNetInferenceVisualiser( anneal.getBNet() );
        vis.updateParams( anneal.getCurrentParams( anneal.getMMLModelLearner() ),
                          anneal.getBNet().makeMissingStruct() );
        
        
        JFrame f = new JFrame("Anneal Search Object");
        f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE  );
        // f.getContentPane().add(new SearchControl(s,new JLabel("Hello")));
        f.getContentPane().add(new SearchControl(s,vis));
        f.pack();
        f.setVisible(true);
        
        
        
        /** Add a SearchListener updating BNet visualisation after each step. */
        s.addSearchListener( new Search.SearchListener() {
                public void reset( Search sender ) { }
                public void beforeEpoch( Search sender ) { }
                public void afterEpoch( Search sender ) {
                    try {
                        vis.updateParams( anneal.getCurrentParams( anneal.getMMLModelLearner() ),
                                          anneal.getBNet().makeMissingStruct() );
                    } catch ( ModelLearner.LearnerException e ) { throw new RuntimeException(e); }
                }
                public void onCompletion( Search sender ) {}
            } );
    }
    
}
