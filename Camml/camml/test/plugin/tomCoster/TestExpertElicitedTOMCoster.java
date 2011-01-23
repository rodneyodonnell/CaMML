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
// Test functions for ExpertElicitedTOMCoster2.
//

// File: TestExpertElixitedTOMCoster.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.tomCoster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Random;

import camml.core.library.BlockingSearch;
import camml.core.search.MetropolisSearch;
import camml.core.search.SECResultsVector;
import camml.core.search.SearchDataCreator;
import camml.core.search.SearchPackage;
import camml.core.search.TOM;
import camml.core.search.MMLEC.MMLECStructure;
import camml.core.search.SECResultsVector.TOMStructure;
import camml.plugin.tomCoster.DirectRelationPrior;
import camml.plugin.tomCoster.ExpertElicitedTOMCoster;
import camml.plugin.tomCoster.PriorProb;
import camml.plugin.tomCoster.RelationPrior;
import junit.framework.*;
import cdms.core.*;

/**
 * Test functions for ExpertElicitedTOMCoster2.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.21 $ $Date: 2007/05/15 10:21:35 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/tomCoster/TestExpertElicitedTOMCoster.java,v $
 */

public class TestExpertElicitedTOMCoster extends TestCase {
    public TestExpertElicitedTOMCoster(String name) {    super(name); }
    
    public static Test suite() 
    {
        return new TestSuite(TestExpertElicitedTOMCoster.class);
    }

    Random rand;
    protected void setUp() throws Exception    {
        rand = new Random(123);
    }

    final double searchFactor = 0.1;
    boolean trueCase = false;
    
    
    private Value.Vector data8 = SearchDataCreator.generateWallaceKorbStyleDataset(new Random(123),1000,8,1,1,true);
    private Value.Vector data3 = SearchDataCreator.generateWallaceKorbStyleDataset(new Random(123),1000,3,1,1,true);
    
    /** Ensure data generated in setup is valid. */
    public void testSetup() { }
    
    

    /** Test kTau returns bubbleSort distance */
    public void testKTau()
    {
        // Regular cases
        assertEquals(0, ExpertElicitedTOMCoster.kTau(new int[] {0,1,2}, 3 ) );
        assertEquals(12, ExpertElicitedTOMCoster.kTau(new int[] {6,7,0,1,2,3,4,5},8 ) );
        assertEquals(6, ExpertElicitedTOMCoster.kTau(new int[] {10,0,20,1,30,2,40},7 ) );
        assertEquals(3, ExpertElicitedTOMCoster.kTau(new int[] {1,4,1,1,1},5 ) );
        
        // n != length
        assertEquals(3, ExpertElicitedTOMCoster.kTau(new int[] {10,0,20,1,30,2,40},5 ) );
        assertEquals(1, ExpertElicitedTOMCoster.kTau(new int[] {10,0,20,1,30,2,40},2 ) );
    }
    
    /** Test kendallTau gives same results as kTau */
    public void testKendallTau()
    {
        // Regular cases
        assertEquals(6, ExpertElicitedTOMCoster.kendallTau(
                                                           new int[] {0,1,2,3,4,5,6},
                                                           new int[] {3,0,4,1,5,2,6}
                                                           ) );
    }

    
    /** Ensure elicitMatrix and convertMatrix are working. */
    public void testElicit() throws IOException {
        //StringReader sr = new StringReader("4 3 1 3 2 1 0");
        StringReader sr = new StringReader("set {n=4;} ed {3<- 1; 3 <- 2; 1 <- 0;}");
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.6,sr);
        tc.readPriors(sr);
        boolean[][] arcMatrix = tc.edPriorDAG; 
        
        assertTrue(arcMatrix[3][1]);
        assertTrue(arcMatrix[3][2]);
        assertTrue(arcMatrix[1][0]);
        assertFalse(arcMatrix[0][3]);
        
        int[][] tierList = ExpertElicitedTOMCoster.convertMatrix(arcMatrix);
        assertTrue( Arrays.equals(tierList[0],new int[]{0,2}) );
        assertTrue( Arrays.equals(tierList[1],new int[]{1}) );
        assertTrue( Arrays.equals(tierList[2],new int[]{3}) );
        
        
        int[] order = ExpertElicitedTOMCoster.generate(tierList,new int[]{3,2,0,1});
        assertTrue( Arrays.equals(order,new int[]{2,0,1,3}) );
    }

    /** Check if exception is thrown when 'n' not specified. */
    public void testBrokenElicit() throws IOException {        
        try {
            StringReader sr = new StringReader("ed {3<- 1; 3 <- 2; 1 <- 0;}");
            ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.6,sr);
            tc.readPriors(sr);
            fail("\"n not found\"Exception should be thrown.");
        } catch (RuntimeException e) { /*ignore. Correct behaviour.*/ }
    }
    
    /** Ensure elicitMatrix and convertMatrix are working on larger nets. */
    public void testLargeElicit() throws IOException {
        StringReader sr = new StringReader("set{n=21;} ed {" + "\n" + 
                                           "12->13 9;  9 -> 18; 18 -> 19; 19 -> 11; "+ "\n" +
                                           "18 -> 8 ; 8  -> 11;  6 ->  8;  6 ->  4;  4 -> 10; "+ "\n" +
                                           "8  -> 10; 8  -> 17; 17 -> 16; 16 ->  0; 16 ->  1; "+ "\n" +
                                           "15 <- 3 7 14; 3  -> 7 ; 3  -> 14;  7 -> 14; }");

        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.6,sr);
        tc.readPriors(sr);
        boolean[][] arcMatrix = tc.edPriorDAG;
        
        assertTrue(arcMatrix[15][3]);
        assertTrue(arcMatrix[11][8]);
        assertTrue(arcMatrix[16][17]);
        assertFalse(arcMatrix[17][11]);
        
        int[][] tierList = ExpertElicitedTOMCoster.convertMatrix(arcMatrix);
        assertTrue( Arrays.equals(tierList[0],new int[]{2,3,5,6,12,20}) );
        assertTrue( Arrays.equals(tierList[1],new int[]{4,7,9,13}) );
        assertTrue( Arrays.equals(tierList[2],new int[]{14,18}) );
        assertTrue( Arrays.equals(tierList[3],new int[]{8,15,19}) );
        assertTrue( Arrays.equals(tierList[4],new int[]{10,11,17}) );
        assertTrue( Arrays.equals(tierList[5],new int[]{16}) );
        assertTrue( Arrays.equals(tierList[6],new int[]{0,1}) );
        
        int ord[] = new int[] {20,18,16,14,12,10,8,6,4,2,0,19,17,15,13,11,9,7,5,3,1};
        int[] order = ExpertElicitedTOMCoster.generate(tierList,ord);
        
        // Hand verified.
        assertTrue( Arrays.equals(order,new int[]{20,12,6,2,5,3,4,13,9,7,18,14,8,19,15,10,17,11,16,0,1}) );    
        assertEquals(91, ExpertElicitedTOMCoster.kendallTau(ord,order));
    }
    
    /** Ensure elicitMatrix and convertMatrix are working. */
    public void testReadPrior() throws IOException {
        StringReader sr = new StringReader(
                                           "set {n = 4;}" +
                                           "ed {3 <- 1 2; 0 -> 1;}" );
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.6,sr);
        tc.readPriors(sr);
        boolean[][] arcMatrix = tc.edPriorDAG;
        
        assertTrue(arcMatrix[3][1]);
        assertTrue(arcMatrix[3][2]);
        assertTrue(arcMatrix[1][0]);
        assertFalse(arcMatrix[0][3]);
        
        int[][] tierList = ExpertElicitedTOMCoster.convertMatrix(arcMatrix);
        assertTrue( Arrays.equals(tierList[0],new int[]{0,2}) );
        assertTrue( Arrays.equals(tierList[1],new int[]{1}) );
        assertTrue( Arrays.equals(tierList[2],new int[]{3}) );
        
        
        int[] order = ExpertElicitedTOMCoster.generate(tierList,new int[]{3,2,0,1});
        assertTrue( Arrays.equals(order,new int[]{2,0,1,3}) );
    }

    /** Test cost using edit distance. */
    public void testEDCost() throws IOException {
        
        // Test network has topology of Asia network.
        // Asia = 0, Smoke = 1, 2 = TB, 3 = Cancer, 4 = Bronch, 5 = TB|CA, 6 = XRay, 7 = Dysp 
        StringReader edString = new StringReader(
                                                 "set {n = 8;edPrior = 0.75;}" +
                                                 "ed {0 -> 2; 1 -> 3 4; 2 -> 5; 3 -> 5; 4 -> 7; 5 -> 6 7;}" );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.6,edString);        
        TOM tom = new TOM(data8);
        
        double arcCost = Math.log(0.75)-Math.log(0.25); 
        assertEquals(8*arcCost,tc.cost(tom),0.001);
        
        tom.addArc(5,6);    
        assertEquals(7*arcCost,tc.cost(tom),0.001);
        
        tom.swapOrder(5,6,true);        
        assertEquals(9*arcCost,tc.cost(tom),0.001);

        tom.addArc(0,1);
        assertEquals(10*arcCost,tc.cost(tom),0.001);
    }

    /** Test cost using kt distance. */
    public void testKTCost() throws IOException {
        
        // Test network:
        //  0 <- 1 2
        //  1 <- 
        //  2 <- 
        //  3 <-
        //  4 <- 
        //  5 <- 3 4
        //  6 <- 7
        //  7 <- 2
        StringReader edString = new StringReader(
                                                 "set {n = 8; ktPrior = 0.75;}" +
                                                 "kt {0 <- 1 2; 5 <- 3 4; 6 <- 7; 7 <- 2;}" );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.6,edString);        
        TOM tom = new TOM(data8);
        
        // Calculations done as (undirectedED + kt)*arcCost where kt
        // is found using Kevin's flawed algorithm.
        double arcCost = Math.log(0.75)-Math.log(0.25); 
        assertEquals((6+5)*arcCost,tc.cost(tom),0.001);
        
        // Adding a valid arc (even in the wrong direction) reduced KT.
        tom.addArc(0,2);        
        assertEquals((5+5)*arcCost,tc.cost(tom),0.001);
        
        tom.swapOrder(0,2,false);
        assertEquals((5+3)*arcCost,tc.cost(tom),0.001);        

        tom.addArc(0,7);
        assertEquals((6+3)*arcCost,tc.cost(tom),0.001);
        
    }

    /** Test cost using directed arc costs. */
    public void testDirectedArcCost() throws IOException {
        
        // Test network:
        //  0 <- 1 (0.99) 2 (0.75)
        //  1 <- 
        //  2 <- 
        //  3 <-
        //  4 <- 
        //  5 <- 3 (0.60) 4 (0.10)
        //  6 <- 7 (0.50)
        //  7 <- 2 (1.00)
        StringReader edString = new StringReader(
                                                 "set {n = 8;}" +
                                                 "arcs {0 <- 1 0.99; 2 -> 0 0.75; 5 <- 3 0.60; 5 <- 4 0.10; 6 <- 7 0.50; 7 <- 2 1.00;}" );
        
        double arcP = 0.6;
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(arcP,edString,data8);        
        TOM tom = new TOM(data8);        
        
        assertEquals(0.99, tc.directPrior[1][0].pArcIJ(), 0.001);
        assertEquals(0.75, tc.directPrior[2][0].pArcIJ(), 0.001);
        assertEquals(0.50, tc.directPrior[7][6].pArcIJ(), 0.001);

        assertEquals(1.00, tc.directPrior[7][2].pArcJI(), 0.001);
        assertEquals(0.10, tc.directPrior[5][4].pArcJI(), 0.001);
        assertEquals(0.60, tc.directPrior[5][3].pArcJI(), 0.001);
    
        assertEquals(arcP * 0.5, tc.directPrior[6][5].pArcJI(), 0);
        assertEquals(arcP * 0.5, tc.directPrior[3][1].pArcJI(), 0);
        assertEquals(arcP * 0.5, tc.directPrior[4][2].pArcJI(), 0);

        assertEquals(tc.cost(tom),Double.POSITIVE_INFINITY,0.0);
        
    }

    /** Test examples used in AI06 Paper. */
    public void testPaperExample() throws IOException {
                
        // Test network:
        //  0 <- 1 (0.7)
        //  1 <- 0 (0.2)
        //  2 -- 0 (0.6)
        StringReader edString = new StringReader(
                                                 "set {n = 3;}" +
                                                 "arcs {0 <- 1 0.7; 1 <- 0 0.2; 2 -- 0 0.6;}" );
        
        // Default arc probability.
        double pArc = 0.5; 
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(pArc,edString);        
        TOM tom = new TOM(data3);
        
        tom.swapOrder(1,2,true);
        tom.addArc(0,2);
        tom.addArc(1,2);
        
        double c2 = -Math.log(0.05) - Math.log(0.30) - Math.log(.25);
        assertEquals( c2, tc.cost(tom), 0.00000000001);
    }

    /** Test examples used in AI06 Paper. */
    public void testAncestorExample() throws IOException {
                
        // Default prior.
        StringReader defaultString = new StringReader(    "set {n = 3;}"  );

        // Test prior:
        //  0 <= 1 (0.7)
        //  1 <= 0 (0.2)
        //  2 == 0 (0.6)
        StringReader arcString = new StringReader( "set {n = 3;}" +
                                                   "arcs {0 => 1 0.7; 1 => 0 0.2; 2 == 0 0.6;}" );
    
        // Default arc probability.
        double pArc = 0.5; 
        //System.out.println( "Prior = \n" + PriorProb.calcPrior(pArc,3));
        
        // Create TOM costers.
        ExpertElicitedTOMCoster tc1 = new ExpertElicitedTOMCoster(pArc,defaultString);
        ExpertElicitedTOMCoster tc2 = new ExpertElicitedTOMCoster(pArc,arcString);        

        // Cost the network 0 -> 2 -> 1
        TOM tom = new TOM(3);        
        tom.swapOrder(1,2,true);
        tom.addArc(0,2);
        tom.addArc(1,2);
        
        // Difference in cost should be the results of the ancestor prior.
        // P(0=>1) = 0.7, P(1=>0) = .2, P(0<=>1) = .1*2/22, P(0=/=1) = .1*20/22
        // P(0=>2) = 0.6*13/28, P(2=>0) = .6*13/28, P(0<=>2) = .6*2/28, P(0=/=2) = .4
        // P(1=>2) = 13/48, P(2=>1) = 13/48, P(1<=>2) = 2/48, P(1=/=2) = 20/48
        // note: P(1 .. 2) not used as no prior is given for it.
        double diff = -Math.log(0.70) - Math.log(0.60 * 13/28);
        // default cost
        double cost1 = tc1.cost(tom);
        assertEquals( -Math.log(.25*.25*.25), cost1 );
        // cost with ancestor prior.
        double cost2 = tc2.cost(tom);
        
        // Allow .01 nits of error due to sampling.
        assertEquals( diff, cost2 - cost1, 0.01);
    }

    /** Test cost using directed arc costs. */
    public void testUndirectedArcCost() throws IOException {
        
        // Test network:
        //  0 -- 1 (0.99) 2 (0.75)
        //  1 -- 
        //  2 -- 
        //  3 --
        //  4 -- 
        //  5 -- 3 (0.60) 4 (0.10)
        //  6 -- 7 (0.50)
        //  7 -- 2 (0.00)
        StringReader edString = new StringReader(
                                                 "set {n = 8;}" +
                                                 "arcs {0 -- 1 0.99; 2 -- 0 0.75; 5 -- 3 0.60; 5 -- 4 0.10; 6 -- 7 0.50; 7 -- 2 0.00;}" );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.9,edString);        
        TOM tom = new TOM(data8);

        assertEquals(.99, tc.directPrior[1][0].pArc(), 0.001);
        assertEquals(.75, tc.directPrior[2][0].pArc(), 0.001);
        assertEquals(.60, tc.directPrior[5][3].pArc(), 0.001);
        assertEquals(.10, tc.directPrior[5][4].pArc(), 0.001);
        assertEquals(.50, tc.directPrior[7][6].pArc(), 0.001);
        assertEquals(.00, tc.directPrior[7][2].pArc(), 0.001);
        
        // Matrix should be symetric.
        for (int i = 0; i < tom.getNumNodes(); i++) {
            for (int j = 0; j < i; j ++) {
                assertEquals(tc.directPrior[i][j].pArc(),
                             tc.directPrior[i][j].pArcIJ()+tc.directPrior[i][j].pArcJI(),0.0001);
            }
        }
        
        // Cost to state an empty TOM
        double oldCost = tc.cost(tom);                        

        // Add impossible link, cost should be infinite.
        tom.addArc(7,2);
        double newCost = tc.cost(tom);
        assertEquals(Double.POSITIVE_INFINITY,newCost,0.001);

        // Remove it again, cost should be 'oldCost' again.
        tom.removeArc(7,2);
        newCost = tc.cost(tom);
        assertEquals(oldCost,newCost,0.001);
    
        // Swapping ordering should not change cost
        tom.swapOrder(7,2,true);
        tom.swapOrder(1,3,true);
        tom.swapOrder(2,4,true);
        assertEquals(oldCost, tc.cost(tom), 0.001);
    }

    /** Test cost using directed arc costs. */
    public void testTemporalCost() throws IOException {
        
        // Test network:
        //  0 <- 1 (0.99) 2 (0.75)
        //  1 <- 
        //  2 <- 
        //  3 <-
        //  4 <- 
        //  5 <- 3 (0.60) 4 (0.10)
        //  6 <- 7 (0.50)
        //  7 <- 2 (1.00)
        StringReader edString = new StringReader( "set {n = 8;}" +
                                                  "arcs {0 >> 1 0.99; 2 << 0 0.75; 5 >> 3 0.60; 5 >> 4 0.10; 6 >> 7 0.50; 7 >> 2 0.99;}" );

        double arcP = 0.7;
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(arcP,edString);        
        TOM tom = new TOM(data8);
        
        assertEquals(.99, tc.directPrior[1][0].pBefore(), 0.001);
        assertEquals(.75, tc.directPrior[2][0].pBefore(), 0.001);
        assertEquals(.50, tc.directPrior[7][6].pBefore(), 0.001);

        assertEquals(.60, 1-tc.directPrior[5][3].pBefore(), 0.001);
        assertEquals(.10, 1-tc.directPrior[5][4].pBefore(), 0.001);
        assertEquals(.99, 1-tc.directPrior[7][2].pBefore(), 0.001);
    
        assertEquals(.50, tc.directPrior[6][5].pBefore(), 0.001);
        assertEquals(.50, tc.directPrior[3][1].pBefore(), 0.001);
        assertEquals(.50, tc.directPrior[4][2].pBefore(), 0.001);
        
        double oldCost = tc.cost(tom);
                
        // Adding arc does not change cost.
        tom.addArc(0,1);
        assertEquals(oldCost+(-Math.log(tc.getArcProb()) - 
                              -Math.log(1-tc.getArcProb())),tc.cost(tom),0.001);        
    }
    
    /** Test combining TOMCosters gives the same result as the sum of its parts. 
     *  This test should hold when default priors are disabled. 
     *  */
    public void testCombinationCost() throws IOException {
        
        String setString = "set {edPrior = 0.75;ktPrior = 0.75;}";
        String ktString = "kt {0 <- _0_0_1_ 2; 5 <- 3 4; 6 <- 7; 7 <- 2;}";
        String dArcString = "arcs {0 <- 1 0.99; _0_0_2_ -> 0 0.75; 5 <- 3 0.60; 5 <- 4 0.10; 6 <- 7 0.50; 7 <- 2 1.00;}";
        String uArcString = "arcs {0 -- 1 0.99; 2 -- _0_0_0_ 0.75; 5 -- 3 0.60; 5 -- 4 0.10; 6 -- 7 0.50; 7 -- 2 1.00;}";
        String tArcString = "arcs {0 >> 1 0.99; 2 << 0 0.75; 5 >> 3 0.60; 5 >> 4 0.10; 6 >> 7 0.50; 7 >> 2 1.00;}";
        
        // All costers done deperately
        ExpertElicitedTOMCoster[] coster = new ExpertElicitedTOMCoster[] {
            new ExpertElicitedTOMCoster(0.5,new StringReader(setString+ktString), data8),
            new ExpertElicitedTOMCoster(0.5,new StringReader(setString+dArcString), data8),
            new ExpertElicitedTOMCoster(0.5,new StringReader(setString+uArcString), data8),
            new ExpertElicitedTOMCoster(0.5,new StringReader(setString+tArcString), data8)
        };

        
        
        // Combined arc coster.
        ExpertElicitedTOMCoster arcCoster = 
            new ExpertElicitedTOMCoster(0.5, new StringReader(setString+dArcString+uArcString+tArcString), data8);
        // Combined ed & kt coster
        ExpertElicitedTOMCoster ktCoster = 
            new ExpertElicitedTOMCoster(0.5, new StringReader(setString+ktString), data8);
        // All costers combined.
        ExpertElicitedTOMCoster fullCoster = 
            new ExpertElicitedTOMCoster(0.5, new StringReader(setString+ktString+dArcString+uArcString+tArcString), data8);        

        TOM tom = new TOM(data8);
        
        double cost = 0.0;
        for (int i = 0; i < coster.length; i++) { cost += coster[i].cost(tom);}
        assertEquals(cost,ktCoster.cost(tom)+arcCoster.cost(tom),0.0001);
        assertEquals(cost,fullCoster.cost(tom),0.0001);
            
        Random r = new Random(123);
        for (int i = 0; i < 100; i++) {
            tom.clearArcs();
            tom.randomOrder(r);
            tom.randomArcs(r);

            cost = 0.0;
            for (int j = 0; j < coster.length; j++) { 
                cost += coster[j].cost(tom);
            }
        }
    }

    /** Test cost using tiers. */
    public void testTiers() throws IOException {

        StringReader edString = new StringReader( "set {n = 8; tierPrior = 0.9;}" +
                                                  "tier {_0_0_0_ 1 2 < 3 4 < 7; 5 6 < _0_0_4_;}" );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.9,edString, data8);        
        TOM tom = new TOM(data8);
        
        double tierPriorPenalty = -Math.log( tc.getTierProb() );
        double notierPriorPenalty = -Math.log( 1-tc.getTierProb() );
        assertEquals(tierPriorPenalty, -Math.log(tc.directPrior[5][4].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(tc.directPrior[6][4].pBefore()), 0.001);

        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[3][0].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[3][1].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[4][2].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[7][1].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[7][4].pBefore()), 0.001);


        assertEquals(0.5, tc.directPrior[2][0].pBefore(), 0.001);
        assertEquals(0.5, tc.directPrior[6][3].pBefore(), 0.001);
        assertEquals(0.5, tc.directPrior[7][5].pBefore(), 0.001);
        
        int nArcs = (8*7)/2;  // n(n-1)/2 arcs, 0 specified in priors. 28 use default prior.
        double baseCost = nArcs * -Math.log(1-tc.getArcProb());

        
        // 13 priors, 11 met 
        // 15 relationsips not specified.
        // => 11 * tierPrior + 2 * notierPrior + 15 * -log(.5)
        assertEquals(baseCost + 11*tierPriorPenalty + 2*notierPriorPenalty + -15*Math.log(.5), tc.cost(tom), 0.001);
        
        // 13 priors satisfied.
        tom.swapOrder(4,5,true);
        tom.swapOrder(4,6,true);
        assertEquals( baseCost + 13 * tierPriorPenalty -15*Math.log(.5), tc.cost(tom), 0.001);
    }

    /** Test using invalid labels. */
    public void testIgnoreBadLabels() throws IOException {

        // try bad labels tiers using default ignoreBadArcs
        try {
            StringReader edString2 = new StringReader( "set {n = 8; tierPrior = 0.9; }" +
                                                       "tier {_0_0_0_ dog 1 2 < 3 4 < 7; 5 6 < _0_0_4_;}" );    
            new ExpertElicitedTOMCoster(0.9, edString2, data8);
            fail("No exception thrown when bad arcs given.");
        } catch (RuntimeException e) {
            // do nothing
        }

        
        // Try bad arcs with ignoreBadArcs = true
        StringReader edString = new StringReader( "set {n = 8; tierPrior = 0.9; ignoreBadLabels = true;}" +
                                                  "tier {aa _0_0_0_ dog 1 2 < 3 4 < 7; 5 6 < _0_0_4_ hhjk;}" );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.9, edString, data8);                
        TOM tom = new TOM(data8);
        
        double tierPriorPenalty = -Math.log( tc.getTierProb() );
        double notierPriorPenalty = -Math.log( 1-tc.getTierProb() );
        assertEquals(tierPriorPenalty, -Math.log(tc.directPrior[5][4].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(tc.directPrior[6][4].pBefore()), 0.001);

        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[3][0].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[3][1].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[4][2].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[7][1].pBefore()), 0.001);
        assertEquals(tierPriorPenalty, -Math.log(1-tc.directPrior[7][4].pBefore()), 0.001);


        assertEquals(0.5, tc.directPrior[2][0].pBefore(), 0.001);
        assertEquals(0.5, tc.directPrior[6][3].pBefore(), 0.001);
        assertEquals(0.5, tc.directPrior[7][5].pBefore(), 0.001);
        
        int nArcs = (8*7)/2;  // n(n-1)/2 arcs, 0 specified in priors. 28 use default prior.
        double baseCost = nArcs * -Math.log(1-tc.getArcProb());

        
        // 13 priors, 11 met 
        // 15 relationsips not specified.
        // => 11 * tierPrior + 2 * notierPrior + 15 * -log(.5)
        assertEquals(baseCost + 11*tierPriorPenalty + 2*notierPriorPenalty + -15*Math.log(.5), tc.cost(tom), 0.001);
        
        // 13 priors satisfied.
        tom.swapOrder(4,5,true);
        tom.swapOrder(4,6,true);
        assertEquals( baseCost + 13 * tierPriorPenalty -15*Math.log(.5), tc.cost(tom), 0.001);
    }
    
    
    /** Test repair function when constraints are specified. */
    public void testRepair() throws IOException {

        StringReader edString = new StringReader( "set {n = 8; tierPrior = 0.9;}" +
                                                  "tier {0 1 2 < 3 4 < 7; 5 6 < 4;}" +
                                                  "arcs {5 -> 6 0.0; 1 -> 2 1.0; 1 -- 5 1.0;}" 
                                                  );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.5,edString);        
        TOM tom = new TOM(data8);
        
        int preexist[] = new int[2];
        int postexist[] = new int[2];
        
        for (int i = 0; i < 1000; i++) {
            tom.randomOrder(rand);
            tom.randomArcs(rand);

            // Repair TOM. Keep track of how often arc 5--6 exists.
            if ( tom.isDirectedArc(6,5) ) { preexist[0] ++; } else { preexist[1] ++; }        
            tc.repairTOM(tom);
            if ( tom.isDirectedArc(6,5) ) { postexist[0] ++; } else { postexist[1] ++; }

            assertTrue(tom.isDirectedArc(1,2));
            assertTrue(!tom.isDirectedArc(5,6));
            assertTrue(tom.isArc(1,5));
            assertTrue(tom.before(0,3));
            assertTrue(tom.before(6,7));
            assertTrue(tom.before(0,7));
            
            
            // Calculate the cost to state edges expert hasn't specified.
            // 28 pairwise relationships
            
            // 12 * No relationship, all four (A->B,B->A,A..B,B..A) states equilikely.
            double c1 = 12 * -Math.log(0.25);
            // 13 with order constraint specified (.9 strength), = log(.5*.9)
            double c2 = 13 * -Math.log(0.5*0.9);
            // 1 arc existence specified, only need to state order.
            double c3 = 1 * -Math.log(0.5);
            // 1 (I->J = 0) relationship, normalise over remaining 3 states.
            double c4 = 1 * -Math.log(1.0/3);
            // One (5->6 1.0) fully specified, nothing to state.
            double c5 = 1 * -Math.log(1.0);
            
            double tomCost = tc.cost(tom);
            //System.out.println("tomCost = " + tomCost + "\t in testRepair");
            assertEquals(c1 + c2 + c3 + c4 + c5, tomCost, 0.001);
        }
        
        
        // Ensure networks (pre & post repair) are being generated randomly.
        // This check is for a bug which always added the arc[5][6] during a repair.
        System.out.println("preExist = " + Arrays.toString(preexist));
        System.out.println("postExist = " + Arrays.toString(postexist));
        assertEquals( 250, preexist[0], 30 );
        assertEquals( 750, preexist[1], 30 );
        assertEquals( 250, postexist[0], 30 );
        assertEquals( 750, postexist[1], 30 );        
    }

    /** Test repair function when constraints are specified. */ 
    public void testRepair2() throws IOException {
        Value.Vector data = SearchDataCreator.getUncorrelatedDataset();
        StringReader edString = new StringReader( 
                                                 "set {n = 3; edPrior = 1.0;}" +
                                                 "ed {1 -> 0;}"
                                                  );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.85,edString);        
        TOM tom = new TOM(data);
        
        for (int i = 0; i < 100; i++) {
            tom.randomOrder(rand);
            tom.randomArcs(rand);

            tc.repairTOM(tom);
            assertEquals("\n"+tom.toString(),0.0, tc.cost(tom), 0.001);

            assertTrue(tom.isDirectedArc(1,0));
            assertTrue(!tom.isArc(0,2));
            assertTrue(!tom.isArc(1,2));

        }
        
    }

    /** Test repair never increases TOM cost.. */ 
    public void testRepairTiers() throws IOException {
        Random r = new Random(123);
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(r,1000,1,8,1);
        
        StringReader edString = new StringReader( 
                                                 "set {n = 8; tierPrior = 0.75;}" +
                                                 "tier {0 < 1 2 < 3; 4 > 5 > 6 7;}"
                                                  );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.85,edString);        
        TOM tom = new TOM(data);                
        
        for (int i = 0; i < 100; i++) {
            tom.randomOrder(rand);
            double randCost = tc.cost(tom);
            
            tc.repairTOM(tom);
            double repairCost = tc.cost(tom);
            assertTrue( repairCost <= randCost );
        }
        
    }

    public void _testMetropolis() {
        Random r = new Random(123);
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(r,1000,2,2,2);
        MetropolisSearch met = new MetropolisSearch(r,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        
        //StringReader sr = new StringReader("set {n=8;} tier{0 > 1 2 3 > 4 5 6 > 7;}");
        StringReader sr = new StringReader("set {n=8;} ");
        met.setTOMCoster( new ExpertElicitedTOMCoster(0.5,sr));
        new BlockingSearch(met).start(); // run search
        met.caseInfo.nodeCache.printStats(2);
        System.out.println(met.getBestTOM());
    }

    public void _testMetropolis2() {
        Random r = new Random(123);
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(r,100,3,2,2);
        MetropolisSearch met = new MetropolisSearch(r,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        
        StringReader sr = new StringReader("set {n=12;} "+
                                           "kt {0 -> 1; 1 -> 2; 3 -> 4; 4 -> 5; 6 -> 7; 7 -> 8; 9 -> 10; 10 -> 11;"+
                                           "0 -> 3; 1 -> 4; 2 -> 5; 6 -> 9; 7 -> 10; 8 -> 11;" + 
                                           "0 -> 6; 1 -> 7; 2 -> 8; 3 -> 9; 4 -> 10; 5 -> 11;}"
                                           );
        met.setTOMCoster( new ExpertElicitedTOMCoster(0.5,sr));
        new BlockingSearch(met).start(); // run search
        met.caseInfo.nodeCache.printStats(2);
        System.out.println(met.getBestTOM());
    }

    /** Added unit test for bug found in specification of tiers using arcs tag. */
    public void testMetropoli3() {
        Random r1 = new Random(123);
        Value.Vector data = SearchDataCreator.getCommonCauseDataset();
        MetropolisSearch met1 = new MetropolisSearch(r1,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        met1.setOption("searchFactor", new Value.Continuous(searchFactor));
        
        StringReader sr1 = new StringReader( "set {n=3;} arcs {0 -- 1 1.0; 0 << 1 1.0;}" );
        met1.setTOMCoster( new ExpertElicitedTOMCoster(0.5,sr1));
        new BlockingSearch(met1).start(); // run search
        Value.Vector results1 = met1.getResults();
        for ( int i = 0; i < results1.length(); i++) {
            System.out.println("results1.elt(i) = " + results1.elt(i) + "\t in testMetropoli3");
        }
        
        
        Random r2 = new Random(123);
        MetropolisSearch met2 = new MetropolisSearch(r2,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        met2.setOption("searchFactor", new Value.Continuous(searchFactor));
        StringReader sr2 = new StringReader( "set {n=3;} arcs {0 -- 1 1.0;} tier {0 < 1;}" );
        met2.setTOMCoster( new ExpertElicitedTOMCoster(0.5,sr2));
        new BlockingSearch(met2).start(); // run search
        Value.Vector results2 = met2.getResults();

        //        for ( int i = 0; i < results2.length(); i++) {
        //            System.out.println("results2.elt(i) = " + results2.elt(i) + "\t in testMetropoli3");
        //        }        
        //        
        //        System.out.println("\nmet1.getBestTOM() = \n" + met1.getBestTOM() + "\t in testMetropoli3");
        //        System.out.println("\nmet2.getBestTOM() = \n" + met2.getBestTOM() + "\t in testMetropoli3");
        assertEquals( results1.toString(), results2.toString() );

    }

    /** Added unit test for bug found where tiers cause "No SECs found" exception. */
    public void testMetropoli4() throws Exception {
        Random r1 = new Random(123);
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(r1,100,1,3,1);
        MetropolisSearch met1 = new MetropolisSearch(r1,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        met1.setOption("searchFactor", new Value.Continuous(searchFactor));
        
        StringReader sr1 = new StringReader( "set {n=3;} tier {1 > 0;}" );
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.5,sr1); 
        met1.setTOMCoster( tc );
        
        new BlockingSearch(met1).start(); // run search
        Value.Vector results = met1.getResults();
        System.out.println("results = " + results + "\t in testMetropoli4");
    }

    /** Added unit test for bug found where tiers cause arc weights to be incorrectly displayed */
    public void testMetropoli5() throws Exception {
        Random r1 = new Random(123);
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(r1,10000, 13, 2, 1);
        MetropolisSearch met1 = new MetropolisSearch(r1,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        met1.setOption("searchFactor", new Value.Continuous(searchFactor));
        met1.setOption("printArcWeights", new Value.Discrete(1));
        
        String s = "set { n = 26; tierPrior = 0.99; } tier { 21 19 15 2 4 11 5 < 9 10 6 7 8; 21 19 15 2 4 11 5 < 3 16 1 12 0 22 23 13 14 20 18 17; 9 10 6 7 8 < 3 16 1 12 0 22 23 13 14 20 18 17; }";
        StringReader sr1 = new StringReader( s );
        
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.5,sr1); 
        met1.setTOMCoster( tc );
        
        new BlockingSearch(met1).start(); // run search
        Value.Vector results = met1.getResults();
        System.out.println("results = " + results + "\t in testMetropoli4");
        
        double arcPortion[][] = met1.getArcPortions();
        for (int i = 0; i < arcPortion.length; i++) {
            for (int j = 0; j < arcPortion.length; j++) {
                double p = arcPortion[i][j];
                assertTrue(p >= 0);
                assertTrue(p <= 1);
                assertTrue(arcPortion[i][j] + arcPortion[j][i] <= 1);
            }
        }
    }

    //    /** Added unit test for bug found where tiers cause arc weights to be incorrectly displayed */
    //    public void testMetropoli6() throws Exception {
    //        Random r1 = new Random(123);
    //        Value.Vector data = camml.plugin.weka.Converter.load("/home/rodo/data/arff/DIABETES-BMI-SH.arff", true, true);
    //        //Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(r1,10000, 13, 2, 1);
    //        MetropolisSearch met1 = new MetropolisSearch(r1,data,SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
    //
    //        met1.setOption("searchFactor", new Value.Continuous(searchFactor*0.001));
    //        met1.setOption("printArcWeights", new Value.Discrete(1));
    //
    //        String s = "set { n = 26; tierPrior = 0.99; } tier { 21 19 15 2 4 11 5 < 9 10 6 7 8; 21 19 15 2 4 11 5 < 3 16 1 12 0 22 23 13 14 20 18 17; 9 10 6 7 8 < 3 16 1 12 0 22 23 13 14 20 18 17; }";
    //        StringReader sr1 = new StringReader( s );
    //
    //        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.5,sr1);
    //        met1.setTOMCoster( tc );
    //
    //        new BlockingSearch(met1).start(); // run search
    //        Value.Vector results = met1.getResults();
    //        //System.out.println("results = " + results + "\t in testMetropoli4");
    //
    //        double arcPortion[][] = met1.getArcPortions();
    //        for (int i = 0; i < arcPortion.length; i++) {
    //            for (int j = 0; j < arcPortion.length; j++) {
    //                double p = arcPortion[i][j];
    //                assertTrue(p >= 0);
    //                assertTrue(p <= 1);
    //                assertTrue(arcPortion[i][j] + arcPortion[j][i] <= 1);
    //            }
    //        }
    //    }
    
    /** test calcIndirectPrior() */
    public void testCalcIndirectPrior() {
        PriorProb p = PriorProb.calcPrior( 0.5, 10);
        
        System.out.println(p);
    }

    /** test DirectRelationPrior */
    public void testRelationPrior() {
        int numNodes = 5;
        double pArc = .5;
        PriorProb p = PriorProb.calcPrior( pArc, numNodes);        
        System.out.println(p);
        
        ///////////////////////////////////////////////////////////////
        // Ensure exception is thrown when duplicate prior specified //
        ///////////////////////////////////////////////////////////////
        RelationPrior rp = new DirectRelationPrior(0,1,numNodes,pArc);
        rp.setP( "->", 0.7 );
        try {
            rp.setP( "->", 0.8 );
            fail("No exception thrown for duplicate prior.");
        } catch (RuntimeException e) {
            // Correct behaviour, ignore.
        }

        ///////////////////////////////////////////////////////////////////
        // Ensure exception is thrown when contradictory prior specified //
        ///////////////////////////////////////////////////////////////////
        rp = new DirectRelationPrior(0,1,numNodes,pArc);
        rp.setP( "->", 0.7 );
        try {
            rp.setP( "<-", 0.4 );
            fail("No exception thrown for contradictory prior.");
        } catch (RuntimeException e) {
            // Correct behaviour, ignore.
        }
        
        rp = new DirectRelationPrior(0,1,numNodes,pArc);
        rp.setP( "->", 0.7 );
        try {
            rp.setP( "--", 0.6 );
            fail("No exception thrown for contradictory prior.");
        } catch (RuntimeException e) {
            // Correct behaviour, ignore.
        }
        
        rp = new DirectRelationPrior(0,1,numNodes,pArc);
        rp.setP( "->", 0.7 );
        try {
            rp.setP( "<<", 0.2 );
            fail("No exception thrown for contradictory prior.");
        } catch (RuntimeException e) {
            // Correct behaviour, ignore.
        }

        /////////////////////////////////////////////////////////////        
        // Assert rp gives local structure correct probabilities.  //
        // and ignores non-local changes.                          //
        /////////////////////////////////////////////////////////////
        rp = new DirectRelationPrior(0,1,numNodes,pArc);
        rp.setP( "->", 0.60 );        
        rp.setP( "--", 0.80 );        
        rp.setP( "<<", 0.75 );

        TOM tom = new TOM(numNodes);    
        assertEquals( .15, rp.relationProb(tom), 0.00001 );
        tom.addArc(0,1);
        assertEquals( .60, rp.relationProb(tom), 0.00001 );
        tom.swapOrder(0,1,true);
        assertEquals( .20, rp.relationProb(tom), 0.00001 );
        tom.removeArc(0,1);
        assertEquals( .05, rp.relationProb(tom), 0.00001 );
        // make non-local mutations, cost should not change.
        tom.addArc(1,2); tom.swapOrder(0,4,true);
        assertEquals( .05, rp.relationProb(tom), 0.00001 );
    }

    /** Test Ancestor prior constraint. All DAGs returned should match constraints. */
    public void testAncestorConstraint() throws Exception {
        Random rand = new Random(123);

        Value.Vector data = 
            SearchDataCreator.generateWallaceKorbStyleDataset(rand,10,1,1,10);
            
        MetropolisSearch met1 = new MetropolisSearch(rand,data,
                                                     SearchPackage.mlCPTLearner,SearchPackage.mmlCPTLearner);
        TOM tom = new TOM(data);
            
        // Learn network with ancestor constraint, and other soft priors.
        String s = "arcs {3 => 1 1.0; 1 >> 2 0.6; 5 -> 4 0.0; 2 <- 3 0.5; 2 -- 3 0.6;}";
        StringReader sr1 = new StringReader( s );
            
        try {
            new ExpertElicitedTOMCoster(0.5,new StringReader(s));
            fail("Exception should be thrown as 'n' not set");
        }
        catch (Exception e) {
            // Correct behaviour.
        }
            
        ExpertElicitedTOMCoster tc = new ExpertElicitedTOMCoster(0.5,sr1, data); 
        met1.setTOMCoster( tc );
        met1.setOption("joinSECs", Value.FALSE);
        met1.setOption("searchFactor", new Value.Continuous(searchFactor));
        met1.setOption("arcProb",new Value.Continuous(0.5));
        met1.setOption("maxNumSECs",new Value.Discrete(100));
            
            
        new BlockingSearch(met1).start(); // run search
        Value.Vector results = met1.getResults();               
            
        for ( int i = 0; i < results.length(); i++) {
            MMLECStructure mmlec = (MMLECStructure)results.elt(i);
            SECResultsVector secVec = (SECResultsVector)mmlec.cmpnt(0);
            Value.Structured sec = (Value.Structured)secVec.elt(0);
            Value.Vector tomVec = (Value.Vector)sec.cmpnt(0);
            SECResultsVector.TOMStructure tomStruct = (TOMStructure)tomVec.elt(0);                  
            Value.Vector params = (Value.Vector)tomStruct.cmpnt(1); 
                    
            tom.setStructure( params );
            assertTrue( "Relationship 3 => 1 required", tom.isAncestor(3,1) );
            assertFalse( "Relationhsip 5 -> 4 disallowed", tom.isDirectedArc(5,4) );
        }
            
        String fName = "camml/test/TestAncestorPrior.txt";

        // Print MMLECs
        StringBuffer resultBuf = new StringBuffer();
        resultBuf.append( results );
            
        if (trueCase) {
            // Output data to file
            PrintWriter out = new PrintWriter( new FileWriter(fName) );
            out.print( resultBuf.toString() );
            out.flush();
            out.close();
        }
        
        //            // Compare the contents of the loaded file and generated string for equality.
        //            BufferedReader r1 = new BufferedReader(new FileReader(fName));
        //            BufferedReader r2 = new BufferedReader(new StringReader(resultBuf.toString()));
        //
        //            assertEquals( r1.readLine(), r2.readLine() );
    }

}
