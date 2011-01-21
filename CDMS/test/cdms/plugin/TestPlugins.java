//
// JUnit test routine for modelGlue plugin
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: TestPlugins.java
// Author: rodo@dgs.monash.edu.au

package test.cdms.plugin;

import junit.framework.*;



/**
 * Some simple tests.
 *
 */
public class TestPlugins extends TestCase
{
    public TestPlugins(String name) 
    {
	super(name);
    }

    protected void setUp() 
    {
    }

    public static Test suite() 
    {
	TestSuite tSuite = new TestSuite(TestPlugins.class);
	//	tSuite.addTest( TestModelGlue.suite() );
	//	tSuite.addTest( TestLibrary.suite() );

	return tSuite;
    }

    public void testPluginsStuff() 
    {
	    // throw new UnsupportedOperationException("tests not yet implemented.");
    }
}
