//
// Test Cases for EnumerateDAGs
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestEnumerateDAGs.java
// Author: rodo@dgs.monash.edu.au
// Created on 9/02/2005

package camml.test.core.library;

import camml.core.library.Library;
import camml.plugin.rodoCamml.RodoCammlIO;
import cdms.core.Value;
import junit.framework.*;



/** Test functions associated with EnumerateDAGs.java */
public class TestLibrary extends TestCase {
    
    public static Test suite() 
    {
        return new TestSuite(TestLibrary.class);
    }
    
    
    /** Test Library.weightedSummaryVec */
    public final void testSummaryVec() throws Exception {
        Value.Vector data = RodoCammlIO.load("camml/test/AsiaCases.1000.cas");
        Value.Vector summary = Library.makeWeightedSummaryVec(data);
        
        assertEquals( 37, summary.length() );
    }
    

    /** Test Library.weightedSummaryVec */
    public final void testJoinVec() throws Exception {
        Value.Vector data = RodoCammlIO.load("camml/test/AsiaCases.1000.cas");
        Value.Vector join = Library.joinVectors(data, data.cmpnt(7), "extra" );
            
        assertEquals( join.cmpnt(7), join.cmpnt(8) );
        System.out.println("join.t = " + join.t);
    }

}
