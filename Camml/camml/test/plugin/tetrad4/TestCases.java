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
// JUnit test routine.  This is simply a collection of all Camml tests
//

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
