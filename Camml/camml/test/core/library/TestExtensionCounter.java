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
// Test Cases for camml.core.library.ExtensionCounter
//

// File: TestExtensionCounter.java
// Author: rodo@dgs.monash.edu.au


/**
 * Class containtins test cases for ExtensiouCounter <br>
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.8 $ $Date: 2007/02/21 07:12:38 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/core/library/TestExtensionCounter.java,v $
 */

package camml.test.core.library;

import camml.core.library.extensionCounter.BruteForceExtensionCounter;
import camml.core.library.extensionCounter.DynamicCounter;
import camml.core.library.extensionCounter.ExtensionCounterLib;
import camml.core.library.extensionCounter.UnlabelledGraph;
import camml.core.library.extensionCounter.UnlabelledGraph64;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/** Test functions associated with EnumerateDAGs.java */
public class TestExtensionCounter extends TestCase {
    
    public static Test suite() 
    {
        return new TestSuite(TestExtensionCounter.class);
    }
    
    
    /** Test Adding arcs. */
    public final void testUnlabeledGraph() {
        
        UnlabelledGraph64 g = new UnlabelledGraph64(5);
        // Add 0->3, 3->2 and implied arc 0->2
        g.addArc(0,3,true);
        g.addArc(3,2,true);
    
        // Try to add the arc 0->2 manually, an exception should be thrown.
        try { g.addArc(0,2,false); assertTrue("No exception thrown when duplicate arc added.",false); }
        catch (RuntimeException e) {/* Ignore as this is correct behaviour */ }
                
        // Check leaf nodes are correct
        assertEquals("Leaf Node mismatch",g.testOnly_getLeafNodes(),0x0000000000000016l);
        assertEquals("Root Node mismatcH",g.testOnly_getRootNodes(),0x0000000000000013l);
        
        // Check connectedness is reported correctly
        
        
        // Check parentList and childList are consistant.
        long[] parentList = g.getParentList();
        long[] childList = g.getChildList();
        long[] mask = ExtensionCounterLib.nodeMask;
                        
        for ( int i = 0; i < parentList.length; i++ ) {
            for ( int j = 0; j < parentList.length; j++ ) {
                assertEquals( "Mismatch in parentList/childList",
                              (parentList[i] & mask[j]) == 0, (childList[j]&mask[i]) == 0 );
            }
        }    
        
        UnlabelledGraph64 g2 = g.removeNode(0, false);
        assertEquals(g2.testOnly_getRootNodes(),0x0dl);
        assertEquals(g2.testOnly_getLeafNodes(),0x0bl);

    }

    /** Test Wallace Extension counter. */
    public final void testWallaceExtensionCounter() {
        
        UnlabelledGraph64 g = new UnlabelledGraph64(6);
            
        BruteForceExtensionCounter count = new BruteForceExtensionCounter();
        assertEquals(720,count.lperms(g));
        
        g.addArc(0,1,true);
        assertEquals(360,count.lperms(g));

        g.addArc(1,2,true);
        assertEquals(120,count.lperms(g));

        g.addArc(5,4,true);
        assertEquals(60,count.lperms(g));

        g.addArc(5,0,true);
        assertEquals(24,count.lperms(g));

        g.addArc(5,3,true);
        assertEquals(20,count.lperms(g));
        
        g.addArc(0,3,true);
        assertEquals(15,count.lperms(g));

        g.addArc(1,3,true);
        assertEquals(10,count.lperms(g));

        g.addArc(3,4,true);
        assertEquals(3,count.lperms(g));

        g.addArc(2,3,true);
        assertEquals(1,count.lperms(g));
    }

    public final void testDynamicExtensionCounter() {
        
        UnlabelledGraph g = new UnlabelledGraph64(6);
            
        DynamicCounter count = new DynamicCounter();
        assertEquals(720.0,count.countPerms(g));
        
        g.addArc(0,1,true);
        assertEquals(360.0,count.countPerms(g));

        g.addArc(1,2,true);
        assertEquals(120.0,count.countPerms(g));

        g.addArc(5,4,true);
        assertEquals(60.0,count.countPerms(g));
        
        g.addArc(5,0,true);
        assertEquals(24.0,count.countPerms(g));

        g.addArc(5,3,true);
        assertEquals(20.0,count.countPerms(g));
        
        g.addArc(0,3,true);
        assertEquals(15.0,count.countPerms(g));

        g.addArc(1,3,true);
        assertEquals(10.0,count.countPerms(g));

        g.addArc(3,4,true);
        assertEquals(3.0,count.countPerms(g));

        g.addArc(2,3,true);
        assertEquals(1.0,count.countPerms(g));
        
    }

    /** Generate random graphs to ensure Dynamic and Wallace produce the same answers. */
    public final void testDynamicVsWallace() {
                
        DynamicCounter dCount = new DynamicCounter();
        BruteForceExtensionCounter wCount = new BruteForceExtensionCounter();

        final int numNodes = 13;
        final double arcProb = (2.0*numNodes)/(numNodes*(numNodes-1)/2);
        
        java.util.Random rand = new java.util.Random(123);
        int[] totalOrder = new int[numNodes];
        for (int i = 0; i < numNodes; i++) {totalOrder[i] = i;}
        
        long wTime = 0;
        long dTime = 0;
        boolean verbose = false;
        for ( int i = 0; i < 200; i++ ) {
            if (verbose && i%1000 == 0) {
                System.out.print("i="+i+"\t");
                int totalCalls = 0;
                for (int ii = 0; ii < numNodes+1; ii++ ) {
                    totalCalls += DynamicCounter.dCounterCalls[ii];
                    System.out.print( DynamicCounter.dCounterCalls[ii] + " ");
                }
                System.out.println("\t"+totalCalls);
            }
            
            UnlabelledGraph64 g = new UnlabelledGraph64(numNodes);
            for ( int ii = 0; ii < numNodes; ii++) {
                int r = ii+rand.nextInt(numNodes-ii);
                int temp = totalOrder[ii]; totalOrder[ii] = totalOrder[r]; totalOrder[r] = temp;
            }
            
            for (int j = 0; j < g.numNodes; j++ ) {
                for ( int k = j+1; k < g.numNodes; k++ ) {
                    if ( rand.nextDouble() < arcProb ) {
                        g.addArc(totalOrder[j],totalOrder[k],true);
                    }
                }
            }
            
            dTime -= System.currentTimeMillis(); 
            double d = dCount.countPerms(g);
            dTime += System.currentTimeMillis(); 

            wTime -= System.currentTimeMillis();
            double w = wCount.lperms(g);
            wTime += System.currentTimeMillis();
            
            
            assertEquals(w,d);
        }
        
        if(verbose) {
            System.out.println();
            System.out.println("wTime = " + wTime);
            System.out.println("dTime = " + dTime);
            System.out.println("dCounterCalls:");
        
            int totalCalls = 0;
            for (int i = 0; i < DynamicCounter.dCounterCalls.length; i++ ) {
                totalCalls += DynamicCounter.dCounterCalls[i];
                System.out.print( DynamicCounter.dCounterCalls[i] + " ");
            }
            System.out.println("\n"+totalCalls);
        }
        
        System.out.println("wTime = " + wTime);
        System.out.println("dTime = " + dTime);

        // Assert dynamic algorihm should be at least 8 times faster than
        // wallace algorithm on given examples.
        assertTrue(dTime < wTime/5);

    }
 
    /** Test case used to find a bug in UnlabelledGraph.getSubgraphMask(). */
    public final void testBrokenCase() {
                
        DynamicCounter dCount = new DynamicCounter();
        BruteForceExtensionCounter wCount = new BruteForceExtensionCounter();

        UnlabelledGraph64 g = new UnlabelledGraph64(5);

        g.addArc(0,3,true);
        g.addArc(1,3,true);
        g.addArc(2,4,true);
        
        assertEquals((double)wCount.lperms(g),dCount.countPerms(g));
    }

    
    /** Test the interleave function in DynamicCounter. */
    public final void testInterleave() {
        assertEquals( DynamicCounter.interleave(1,1), 2);
        assertEquals( DynamicCounter.interleave(10,10), 184756);
        assertEquals( DynamicCounter.interleave(99,1), 100);
        assertEquals( DynamicCounter.interleave(30,30), 118264581564861424l);
        assertEquals( DynamicCounter.interleave(43,26), 7023301266595310928L);
        try{
            DynamicCounter.interleave(43,27);
            assertTrue("Exception not properly thrown",false);
        } catch (Exception e) {/* Correct behaviour. >64 bits needed to pass this test.*/}
    }    
}
