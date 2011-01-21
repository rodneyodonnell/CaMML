//
// JUnit test routine.  This is simply a collection of all Camml tests
//
// Copyright (C) 2004 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestCases.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.tetrad4;

import junit.framework.*;

/**
 * All Camml Tests
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.5 $ $Date: 2006/08/22 03:13:41 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/tetrad4/TestCases.java,v $
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

        tSuite.addTest( TestData.suite() );
        tSuite.addTest( TestTetrad4FN.suite() );
        
        return tSuite;
    }
    
    
    /** Empty test in place as we require at least one test in a TestCase. */
    public void testNothing() { }
    
}
