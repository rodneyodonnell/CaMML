//
// JUnit test routine.  This is simply a collection of all Camml tests
//
// Copyright (C) 2004 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestCases.java
// Author: rodo@dgs.monash.edu.au

package camml.test.core.search;

import junit.framework.*;

/**
 * All Camml Tests
 */
public class TestCases extends TestCase
{
	
	public TestCases(String name) 
	{
		super(name);
	}
	
	protected void setUp() 
	{
	}
	
	/** Add all subtests to the TestCases */
	public static Test suite() 
	{
		TestSuite tSuite = new TestSuite(TestCases.class);
		
		tSuite.addTest( TestUltraSimpleSearch.suite() );
		tSuite.addTest( TestMetropolisSearch.suite() );
		tSuite.addTest( TestQuickSearch.suite() );
		
		tSuite.addTest( TestGreedyLookaheadSearch.suite() );
		tSuite.addTest( TestAnnealSearch.suite() );
		
		
		tSuite.addTest( TestTOM.suite() );
		
		return tSuite;
	}
	
	public static long mem() {
		Runtime r = Runtime.getRuntime();
		System.gc();
		return r.totalMemory()-r.freeMemory();
		
	}
	
	/** Empty test in place as we require at least one test in a TestCase. */
	public void testNothing() { }
	
}
