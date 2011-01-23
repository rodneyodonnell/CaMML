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

// File: TestQuickSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import cdms.plugin.search.*;

import camml.core.search.*;
import camml.core.library.BlockingSearch;
import camml.core.models.bNet.*;
import camml.core.models.ModelLearner;

import javax.swing.*;


public class TestQuickSearch extends TestCase
{
    public TestQuickSearch(String name) 
    {
        super(name);
    }
    
    // Create some data sets to test with.
    protected void setUp() 
    {
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestQuickSearch.class);
    }
    
    /** 
     * Test Quick search.  This must be done in a blocking way, as a non-blocking search
     * will spawn a new thread and JUnit will think everything is fine (even if it is not).
     */
    public void _testBlockingSearch()
    {
        java.util.Random rand = new java.util.Random(123);
        //    Value.Vector dataset = SearchDataCreator.getCommonCauseDataset();
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        QuickSearch quickSearchObject = new QuickSearch( rand, dataset,
                                                         SearchPackage.mlCPTLearner,
                                                         SearchPackage.mmlCPTLearner );
        Search blockingSearch = new BlockingSearch( quickSearchObject );
        
        blockingSearch.start();
    }
    
    public void testSearchInterface()
    {
        System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

        JFrame f = new JFrame("QuickSearch Search Object");
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        Search s = new BlockingSearch( new QuickSearch( new java.util.Random(12345), dataset,
                                                        SearchPackage.mlCPTLearner,
                                                        SearchPackage.mmlCPTLearner ));
        f.getContentPane().add(new SearchControl(s,new JLabel("QuickSearch")));
        f.pack();
        //f.setVisible(true);
        
        s.start();
    }
    
    
    /** 
     * Test main function. <br>
     * Useage : java camml.test.SearchPackage.TestQuickSearch datafile <br>
     */
    public static void main( String args[] ) 
        throws java.io.IOException, ModelLearner.LearnerException
    {
        Value.Vector data;
        if (args.length == 0) { // create a random dataset.
            data = SearchDataCreator.getWallaceKorbStyleDataset();
        }
        else if ( args.length == 1 ) {  // load a old format camml file
            data = (Value.Vector)camml.plugin.rodoCamml.RodoCammlIO.load( args[0] );
        }
        else {
            throw new IllegalArgumentException();
        }
        
        java.util.Random rand = new java.util.Random( 123 );
        
        
        
        
        Search s = new BlockingSearch( new QuickSearch( rand, data,
                                                        SearchPackage.mlCPTLearner,
                                                        SearchPackage.mmlCPTLearner ) );    
        QuickSearch qSearch = (QuickSearch)s.getSearchObject();       
        
        s.start();
        
        System.out.println();
        System.out.println( "bestMML = " + qSearch.getBestCost() );
        System.out.println( "bestArcProb = " + qSearch.getBestArcProb() );
        System.out.println( "bestTOM = \n" + qSearch.getBestTOM() );
        
        BNet bNet = new BNetStochastic( (Type.Structured)((Type.Vector)data.t).elt);
        
        boolean showGraph = true;
        if ( showGraph == true ) {
            
            Value params = qSearch.getBestTOM().makeParameters( SearchPackage.mmlCPTLearner );
            
            BNetVisualiser panel = new BNetVisualiser( bNet );
            panel.updateParams( (Value.Vector)params );
            
            JFrame f = new JFrame( "Sample Search Object" );
            f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE  );
            f.getContentPane().add( panel );
            
            f.pack();
            f.setSize( 1000, 1000 );
            f.setVisible(true);
            //         cdms.plugin.desktop.Desktop.show.apply( new Value.DefStructured( new Value[] 
            //        { new Value.Str("Results"), met.getResults() } ) );
        }
        
    }
    
    
}
