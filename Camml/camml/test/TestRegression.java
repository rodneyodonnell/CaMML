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
// Tests to ensure results do not change between CaMML updates.
//

// File: TestRegression.java
// Author: rodo@dgs.monash.edu.au
// Created on 31/08/2005

package camml.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import cdms.core.Value;
import cdms.plugin.search.Search;
import camml.core.library.BlockingSearch;
import camml.core.library.WallaceRandom;
import camml.core.models.bNet.BNet;
import camml.core.models.dual.DualLearner;
import camml.core.search.AnnealSearch;
import camml.core.search.MetropolisSearch;
import camml.core.search.SearchDataCreator;
import camml.core.search.SearchPackage;
import camml.core.search.TOMCoster;
import camml.plugin.netica.NeticaFn;

import java.io.*;

/**
 * Tests to ensure CaMMLs results do not change between undates.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.11 $ $Date: 2006/09/14 04:56:30 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/TestRegression.java,v $
 */
public class TestRegression extends TestCase {

    /**
     * 
     */
    public TestRegression(String name) {
        super(name);
    }
    
    
    /** if trueCase == true, TestRegression sets the current case as the "true"
     *  case for other runs (when trueCase==false) to regress too. <br>
     *  trueCase defaults to false.
     */
    protected boolean trueCase;
    
    protected double searchFactor = 0.1;
    
    BNet model;
    Value.Vector params;
    Value.Vector data;
    java.util.Random rand = new WallaceRandom(new int[] {123,456});
    
    /** Create data, etc.*/
    protected void setUp() 
    {
        Value.Structured my = NeticaFn.LoadNet._apply("camml/test/TestRegression.dne");
        model = (BNet)my.cmpnt(0);
        params = (Value.Vector)my.cmpnt(1);
        data = model.generate( rand, 1000, params, Value.TRIV );
        trueCase = false;
    }
    
    /** Add all subtests to the TestCases */
    public static Test suite() 
    {
        return new TestSuite(TestRegression.class);
    }
    
    /** Assert the contents of two (string based) buffers are equal. */
    public void assertEquals( Reader r1, Reader r2 ) throws Exception {
        BufferedReader br1 = new BufferedReader(r1);
        BufferedReader br2 = new BufferedReader(r2);
        
        while (true) {
            String s1 = br1.readLine();
            String s2 = br2.readLine();
            assertEquals( s1, s2 );
            if (s1 == null) { break; }
        }        
    }
    
    /** make sure the same dataset is generated given given the same initial conditions. */
    public void testData() throws Exception {
        String dataString = data.toString();
        String fName = "camml/test/TestRegression.data";
        
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.println( dataString );
            out.flush();
            out.close();
        }
        
        // Compare the contents of the loaded file and generated string for equality.
        BufferedReader r1 = new BufferedReader(new FileReader(fName));
        BufferedReader r2 = new BufferedReader(new StringReader(dataString));
        assertEquals( r1, r2 );
    }
            
    /** Empty test in place as we require at least one test in a TestCase. */
    public void testRun() throws Exception { 
        String fName = "camml/test/TestRegression.dagList";
        
        // Run metropolis search
        MetropolisSearch met =
            new MetropolisSearch( rand, data, 
                                  SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner);
        met.setOption("regression",Value.TRUE);
        met.setOption("searchFactor", new Value.Continuous(searchFactor));
        Search blockingSearch = new BlockingSearch( met );            
        blockingSearch.start();
        
        String resultString = getMetropolisResultString(met);
        
        
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.print( resultString );
            out.flush();
            out.close();
        }
        
        // Compare the contents of the loaded file and generated string for equality.
        BufferedReader r1 = new BufferedReader(new FileReader(fName));
        BufferedReader r2 = new BufferedReader(new StringReader(resultString));
        assertEquals( r1, r2 );
        
        
        System.out.println("DONE!");
    }

    public String getMetropolisResultString(MetropolisSearch met) {
        Value.Vector results = met.getResults(); 
        // Print MMLECs
        StringBuffer resultBuf = new StringBuffer();
        for ( int i = 0; i < results.length(); i++ ) {
            Value.Structured mmlec = (Value.Structured)results.elt(i);
            resultBuf.append("\n\n"+mmlec+"\n");
            //System.out.println(mmlec);
                
            // Print SECs
            Value.Vector mmlecVec = (Value.Vector)mmlec.cmpnt(0);
            for ( int j = 0; j < mmlecVec.length(); j++ ) {
                Value.Structured sec = (Value.Structured)mmlecVec.elt(j);
                resultBuf.append("\n"+sec+"\n");
                //System.out.println("MMLEC : " + i + "\tSEC : " + j + "\t" + sec);
                    
                // Print DAGs
                Value.Vector secVec = (Value.Vector)sec.cmpnt(0);
                for ( int k = 0; k < secVec.length(); k++ ) {
                    Value.Structured dag = (Value.Structured)secVec.elt(k);
                    resultBuf.append(dag+"\n");
                    //System.out.println(dag);
                }
                    
            }
        }
        
        return resultBuf.toString();
    }
    
    /** Empty test in place as we require at least one test in a TestCase. */
    public void testDTreeRun() throws Exception { 
        String fName = "camml/test/TestDTreeRegression.dagList";
        
        // Run metropolis search
        MetropolisSearch met =
            new MetropolisSearch( rand, data, 
                                  SearchPackage.mlCPTLearner, SearchPackage.dTreeLearner);
        met.setOption("regression",Value.TRUE);
        met.setOption("searchFactor", new Value.Continuous(searchFactor));
        Search blockingSearch = new BlockingSearch( met );            
        blockingSearch.start();
        
        
        String resultString = getMetropolisResultString(met); 
        
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.print( resultString );
            out.flush();
            out.close();
        }
        
        
        // Compare the contents of the loaded file and generated string for equality.
        BufferedReader r1 = new BufferedReader(new FileReader(fName));
        BufferedReader r2 = new BufferedReader(new StringReader(resultString));
        assertEquals( r1, r2 );
    }

    /** Empty test in place as we require at least one test in a TestCase. */
    public void testDualCPTDtreeLogitRun() throws Exception {
        String fName = "camml/test/TestDualRegression.dagList";
        
        // Run metropolis search
        MetropolisSearch met =
            new MetropolisSearch( rand, data, 
                                  SearchPackage.mlCPTLearner, DualLearner.dualCPTDTreeLogitLearner );
        met.setOption("regression",Value.TRUE);
        met.setOption("searchFactor", new Value.Continuous(searchFactor));
        Search blockingSearch = new BlockingSearch( met );            
        blockingSearch.start();
        
        
        String resultString = getMetropolisResultString(met); 
        
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.print( resultString );
            out.flush();
            out.close();
        }
        
        
        // Compare the contents of the loaded file and generated string for equality.
        BufferedReader r1 = new BufferedReader(new FileReader(fName));
        BufferedReader r2 = new BufferedReader(new StringReader(resultString));
        assertEquals( r1, r2 );
    }

    /** Run Anneal search on a larger network. 
     *  This should test for a bug causing too many arcs to be present
     * */
    public void testRun2() throws Exception { 
        String fName = "camml/test/TestRegression.anneal.txt";
        
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,3,3,3);
        
        // Run metropolis search
        AnnealSearch ann = new AnnealSearch(rand, data, 
                                            SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner);        
        ann.setOption("regression",Value.TRUE);
        ann.setOption("searchFactor", new Value.Continuous(searchFactor));
        Search blockingSearch = new BlockingSearch( ann );            
        blockingSearch.start();
        Value.Vector results = ann.getBestParams(SearchPackage.mmlCPTLearner);
        
        // Print MMLECs
        StringBuffer resultBuf = new StringBuffer();
        resultBuf.append( results );
                
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.print( resultBuf );
            out.flush();
            out.close();
        }
        
        // Compare the contents of the loaded file and generated string for equality.
        BufferedReader r1 = new BufferedReader(new FileReader(fName));
        BufferedReader r2 = new BufferedReader(new StringReader(resultBuf.toString()));
        assertEquals( r1, r2 );
        
        
        System.out.println("DONE!");
    }

    /** Run Anneal search using TOM prior and DAG prior. */
    public void testDAGPrior() throws Exception {
        //trueCase = true;
        String fName = "camml/test/TestRegression.DAGvsTOM.txt";
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(rand,1000,2,2,3);
        
        // Run metropolis search
        AnnealSearch tomSearch = new AnnealSearch(rand, data, 
                                                  SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner);        

        AnnealSearch dagSearch = new AnnealSearch(rand, data, 
                                                  SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner);        

        AnnealSearch[] ann = new AnnealSearch[] {tomSearch,dagSearch};
        StringBuffer resultBuf = new StringBuffer();
        
        ann[0].setOption("TOMCoster",new Value.Obj(new TOMCoster.UniformTOMCoster(0.5)));
        ann[1].setOption("TOMCoster",new Value.Obj(new TOMCoster.DAGCoster(0.5)));

        ann[0].setOption("searchFactor", new Value.Continuous(searchFactor));
        ann[1].setOption("searchFactor", new Value.Continuous(searchFactor));
        
        long time[] = new long[ann.length];
        
        
        for ( int i = 0; i < ann.length; i++) {
            ann[i].setOption("regression",Value.TRUE);

            time[i] -= System.currentTimeMillis(); 
            Search blockingSearch = new BlockingSearch( ann[i] );            
            blockingSearch.start();
            Value.Vector results = ann[i].getBestParams(SearchPackage.mmlCPTLearner);
            time[i] += System.currentTimeMillis();            
            
            // Print MMLECs
            resultBuf.append( results );
            resultBuf.append( "\nbest Cost: " + ann[i].getBestCost() + "\n");
            resultBuf.append( "extensions: " + TOMCoster.DAGCoster.countExtensions(ann[i].getBestTOM()) + "\n\n");

            System.out.println("time["+i+"] = "+time[i]);
        }    
        
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.print( resultBuf );
            out.flush();
            out.close();
        }
        
        // Compare the contents of the loaded file and generated string for equality.
        BufferedReader r1 = new BufferedReader(new FileReader(fName));
        BufferedReader r2 = new BufferedReader(new StringReader(resultBuf.toString()));
        assertEquals( r1, r2 );
        
    
        
        System.out.println("DONE!");
    }
    
    public static void main(String args[]) throws Exception {
        TestRegression tr = new TestRegression("TestRegression");
        tr.setUp();
        tr.testDAGPrior();
    }
}
