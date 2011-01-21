//
// TODO: 1 line description of TestCases.java
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestCases.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.tomCoster;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TODO: Multi line description of TestCases.java
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.2 $ $Date: 2006/08/22 03:13:42 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/tomCoster/TestCases.java,v $
 */

public class TestCases extends TestCase {
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
        tSuite.addTest( TestExpertElicitedTOMCoster.suite() );
        return tSuite;
    }
    
    
    /** Empty test in place as we require at least one test in a TestCase. */
    public void testNothing() { }
}
