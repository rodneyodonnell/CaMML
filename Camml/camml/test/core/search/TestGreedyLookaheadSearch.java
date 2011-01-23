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

// File: TestGreedyLookaheadSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import cdms.plugin.search.*;
import camml.core.search.*;
import camml.core.models.bNet.*;
import camml.core.models.ModelLearner;
import camml.core.library.BlockingSearch;


//import camml.models.*;
//import cdms.plugin.model.*;
//import camml.SearchPackage.*;


import javax.swing.*;



public class TestGreedyLookaheadSearch extends TestCase
{
    protected static Value.Model binaryMultistateModel;
    
    public TestGreedyLookaheadSearch(String name) 
    {
        super(name);
    }
    
    // Create some data sets to test with.
    protected void setUp() 
    {
        binaryMultistateModel = new cdms.plugin.model.Multinomial(0,1);
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestGreedyLookaheadSearch.class);
    }
    
    
    public void testBlockingSearch()
    {
        System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),1000,1,2,2);
        
        GreedyLookaheadSearch search = new GreedyLookaheadSearch( new java.util.Random(12345),
                                                                  dataset, 1 );
        Search blockingSearch = new BlockingSearch( search );
        
        blockingSearch.start();
    }
    
    
    public void _testSearchInterface()
    {
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        
        JFrame f = new JFrame("Greedy Search Object");
        Search s = new Search( new GreedyLookaheadSearch( new java.util.Random(12345), 
                                                          dataset, 1 ));
        f.getContentPane().add(new SearchControl(s,new JLabel("GreedyLookahead")));
        f.pack();
        f.setVisible(true);
        
        s.start();
    }
    
    
    
    /** Test main function. */
    public static void main( String args[] )
        throws ModelLearner.LearnerException 
    {
        if (args.length != 3)
            throw new IllegalArgumentException("Correct Syntax java <prog> seed samples lookahead");
        
        int seed = Integer.parseInt(args[0]);
        int samples = Integer.parseInt(args[1]);
        int lookahead = Integer.parseInt(args[2]);
        
        java.util.Random rand = new java.util.Random( seed );
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),samples,2,2,2);
        
        
        JFrame f = new JFrame("Greedy Search Object");
        f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE  );
        final GreedyLookaheadSearch greedy = new GreedyLookaheadSearch( rand, dataset, lookahead );
        Search s = new Search( greedy );
        
        
        final BNetInferenceVisualiser vis = new BNetInferenceVisualiser( greedy.getBNet() );
        vis.updateParams( greedy.getCurrentParams( greedy.getMMLModelLearner() ),
                          greedy.getBNet().makeMissingStruct() );
        
        
        
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
                        vis.updateParams( greedy.getCurrentParams( greedy.getMMLModelLearner() ),
                                          greedy.getBNet().makeMissingStruct() );
                    } catch ( ModelLearner.LearnerException e ) { throw new RuntimeException(e); }
                }
                public void onCompletion( Search sender ) {}
            } );
    }
    
    
}
