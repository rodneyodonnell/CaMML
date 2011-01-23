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

// File: TestData.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.tetrad4;

import junit.framework.*;

import camml.plugin.tetrad4.*;

import cdms.core.*;
import camml.core.search.SearchDataCreator;
import camml.plugin.rodoCamml.RodoCammlIO;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.*;

import camml.core.search.*;
import camml.core.library.BlockingSearch;


/**
 * Some simple tests.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.11 $ $Date: 2006/08/22 03:13:41 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/tetrad4/TestData.java,v $
 */
public class TestData extends TestCase
{
    private static final double significance = 0.05;
    private static final int depth = -1; // -1 = unlimited

    /** result of RunAll, contains [ges_graph,fci_graph,pc_graph,anneal_TOM,met_TOM].
     * runAllResult = null if testRunAll has not bee run yet.
     */
    static Object[] runAllResult = null;

    Value.Vector cdmsData;
    public TestData(String name) 
    {
        super(name);
        System.out.println( camml.test.plugin.tetrad4.TestData.class );
    }
    
    protected void setUp() 
    {
        cdmsData = SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(123),
                                                                      10, 1,2,3 );
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestData.class);
    }
    

    /** Run GES,PC,FCL,MML_Anneal,MML_Metropolis on a generated dataset */
    public void testRunAll() {
        runAllResult = runAll( cdmsData );
    }

    /** Test dag enumaration from a graph */
    public void testEnumerate() {
        for ( int i = 0; i < 3; i++ ) { 
            Tetrad4.enumerateDAGs( cdmsData, (Graph)runAllResult[i] );  
        }
    }


    /** Run GES,PC,FCI,MML_Anneal,MML_Metropolis on cdmsData, return array of results. */
    public static Object[] runAll( Value.Vector cdmsData ) {
        // Load Data
        RectangularDataSet data = Tetrad4.cdms2TetradDiscrete( cdmsData );

        // Setup
        Knowledge knowledge = new Knowledge();
        IndependenceTest independence = new IndTestGSquare((RectangularDataSet) data, significance);

        // Run GES Search.
        System.out.println("===================== GES ======================");
        GesSearch gesSearch = new GesSearch(data);
        Graph gesGraph = gesSearch.search();
        System.out.println(gesGraph);

        // Run PC Search
        System.out.println("===================== PC =======================");
        PcSearch pcSearch = new PcSearch(independence, knowledge);
        pcSearch.setDepth(depth);
        Graph pcGraph = pcSearch.search();
        System.out.println(pcGraph);

        // Run FCI Search
        System.out.println("===================== FCI ======================");
        FciSearch fciSearch = new FciSearch(independence, knowledge);
        Graph fciGraph = fciSearch.search();
        System.out.println(fciGraph);


        // Run MML Anneal Search
        System.out.println("=================== ANNEAL =====================");
        java.util.Random rand = new java.util.Random(123);                
        AnnealSearch annealSearchObject = new AnnealSearch( rand, cdmsData,
                                                            SearchPackage.mlCPTLearner,
                                                            SearchPackage.mmlCPTLearner );
        BlockingSearch blockingSearch = new BlockingSearch( annealSearchObject );
        blockingSearch.start();
        TOM annealTOM  = annealSearchObject.getBestTOM();
        System.out.println( annealTOM );
        // free mem as soon as possible
        blockingSearch = null; annealSearchObject = null; 

        // Run MML Metropolis Search
        System.out.println("================= METROPOLIS ===================");
        rand = new java.util.Random(123);                
        MetropolisSearch metropolisSearchObject = new MetropolisSearch( rand, cdmsData,
                                                                        SearchPackage.mlCPTLearner,    SearchPackage.mmlCPTLearner );
        BlockingSearch metBlockingSearch = new BlockingSearch( metropolisSearchObject );
        
        Value.Vector metParams;
        metBlockingSearch.start();
        try {
            metParams = metropolisSearchObject.getBestParams(SearchPackage.mmlCPTLearner);            
        } catch( Exception e ) { throw new RuntimeException(e); }
        TOM metTOM = new TOM( cdmsData );
        metTOM.setStructure( metParams );
        //String metParamString = metTOM.toString();


        System.out.println("===================== ALL ======================");
        System.out.println( "ges:\n" + gesGraph );
        System.out.println( "pc:\n"  + pcGraph );
        System.out.println( "fci:\n" + fciGraph );
        System.out.println( "anneal:\n" + annealTOM );
        System.out.println( "metropolis:\n" + metTOM );
        
        return new Object[] { gesGraph, pcGraph, fciGraph, annealTOM, metTOM };

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

        runAll( cdmsData );
    }

}
