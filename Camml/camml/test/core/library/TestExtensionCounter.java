//
// Test Cases for camml.core.library.ExtensionCounter
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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

import camml.core.library.ExtensionCounter;

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
		
		ExtensionCounter.UnlabelledGraph g = new ExtensionCounter.UnlabelledGraph(5);
		// Add 0->3, 3->2 and implied arc 0->2
		g.addArc(0,3,true);
		g.addArc(3,2,true);
	
		// Try to add the arc 0->2 manually, an exception should be thrown.
		try { g.addArc(0,2,false); assertTrue("No exception thrown when duplicate arc added.",false); }
		catch (RuntimeException e) {/* Ignore as this is correct behaviour */ }
				
		// Check leaf nodes are correct
		assertEquals("Leaf Node mismatch",g.getLeafNodes(),0x0000000000000016l);
		assertEquals("Root Node mismatcH",g.getRootNodes(),0x0000000000000013l);
		
		// Check connectedness is reported correctly
		
		
		// Check parentList and childList are consistant.
		long[] parentList = g.getParentList();
		long[] childList = g.getChildList();
		long[] mask = ExtensionCounter.UnlabelledGraph.nodeMask;
						
		for ( int i = 0; i < parentList.length; i++ ) {
			for ( int j = 0; j < parentList.length; j++ ) {
				assertEquals( "Mismatch in parentList/childList",
						(parentList[i] & mask[j]) == 0, (childList[j]&mask[i]) == 0 );
			}
		}	
		
		ExtensionCounter.UnlabelledGraph g2 = g.removeNode(0, false);
		assertEquals(g2.getRootNodes(),0x0dl);
		assertEquals(g2.getLeafNodes(),0x0bl);

	}

	/** Test Wallace Extension counter. */
	public final void testWallaceExtensionCounter() {
		
		ExtensionCounter.UnlabelledGraph g = new ExtensionCounter.UnlabelledGraph(6);
			
		ExtensionCounter.WallaceCounter count = new ExtensionCounter.WallaceCounter();
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
		
		ExtensionCounter.UnlabelledGraph g = new ExtensionCounter.UnlabelledGraph(6);
			
		ExtensionCounter.DynamicCounter count = new ExtensionCounter.DynamicCounter();
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
				
		ExtensionCounter.DynamicCounter dCount = new ExtensionCounter.DynamicCounter();
		ExtensionCounter.WallaceCounter wCount = new ExtensionCounter.WallaceCounter();

		final int numNodes = 10;
		final double arcProb = (1.0*numNodes)/(numNodes*(numNodes-1)/2);
		
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
					totalCalls += ExtensionCounter.DynamicCounter.dCounterCalls[ii];
					System.out.print( ExtensionCounter.DynamicCounter.dCounterCalls[ii] + " ");
				}
				System.out.println("\t"+totalCalls);
			}
			
			ExtensionCounter.UnlabelledGraph g = new ExtensionCounter.UnlabelledGraph(numNodes);
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
			for (int i = 0; i < ExtensionCounter.DynamicCounter.dCounterCalls.length; i++ ) {
				totalCalls += ExtensionCounter.DynamicCounter.dCounterCalls[i];
				System.out.print( ExtensionCounter.DynamicCounter.dCounterCalls[i] + " ");
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
				
		ExtensionCounter.DynamicCounter dCount = new ExtensionCounter.DynamicCounter();
		ExtensionCounter.WallaceCounter wCount = new ExtensionCounter.WallaceCounter();

		ExtensionCounter.UnlabelledGraph g = new ExtensionCounter.UnlabelledGraph(5);

		g.addArc(0,3,true);
		g.addArc(1,3,true);
		g.addArc(2,4,true);
		
		assertEquals((double)wCount.lperms(g),dCount.countPerms(g));
	}

	
	/** Test the interleave function in DynamicCounter. */
	public final void testInterleave() {
		assertEquals( ExtensionCounter.DynamicCounter.interleave(1,1), 2);
		assertEquals( ExtensionCounter.DynamicCounter.interleave(10,10), 184756);
		assertEquals( ExtensionCounter.DynamicCounter.interleave(99,1), 100);
		assertEquals( ExtensionCounter.DynamicCounter.interleave(30,30), 118264581564861424l);
		assertEquals( ExtensionCounter.DynamicCounter.interleave(43,26), 7023301266595310928L);
		try{
			ExtensionCounter.DynamicCounter.interleave(43,27);
			assertTrue("Exception not properly thrown",false);
		} catch (Exception e) {/* Correct behaviour. >64 bits needed to pass this test.*/}
	}	
}
