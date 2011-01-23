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
// Test Cases for EnumerateDAGs
//

// File: TestEnumerateDAGs.java
// Author: rodo@dgs.monash.edu.au
// Created on 9/02/2005

package camml.test.core.library;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import camml.core.library.BlockingSearch;
import camml.core.library.EnumerateDAGs;
import camml.core.models.bNet.FixedStructureSearch;
import camml.core.models.cpt.CPTLearner;
import camml.core.search.SearchDataCreator;
import camml.core.search.TOM;
import cdms.core.Value;


/** Test functions associated with EnumerateDAGs.java */
public class TestEnumerateDAGs extends TestCase {
    
    public static Test suite() 
    {
        return new TestSuite(TestEnumerateDAGs.class);
    }
    
    protected EnumerateDAGs enumDag;
    
    public final void testApply() {    }
    
    /** Test enumerateDAGs returns for various values of n */
    public final void testEnumerateGraphs() {
        enumDag = new EnumerateDAGs();
        
        final int max = EnumerateDAGs.maxGraphSize;
        for ( int i = 0; i < max; i++ ) {
            enumDag.enumerateGraphs(i);
        }    
        // Check exception is thrown when calling with n >= max
        try { 
            enumDag.enumerateGraphs(max); 
            fail("Attempted to calculate with too many vars in enumerateGraphs.");
        } catch ( RuntimeException e ) { /* ignore exception */}
        // Check exception is thrown when calling with n < 0
        try { 
            enumDag.enumerateGraphs(-1); 
            fail("Attempted to calculate with negative n in enumerateGraphs.");
        } catch ( RuntimeException e ) { /* ignore exception */}        
    }
    
    /** Ensure that correct numDags is calculated for n=0..5 */
    public final void testNumDAGs() {        
        assertEquals( EnumerateDAGs.numDAGs(0), 1 );
        assertEquals( EnumerateDAGs.numDAGs(1), 1 );
        assertEquals( EnumerateDAGs.numDAGs(2), 3 );
        assertEquals( EnumerateDAGs.numDAGs(3), 25 );
        assertEquals( EnumerateDAGs.numDAGs(4), 543 );
        assertEquals( EnumerateDAGs.numDAGs(5), 29281 );
        assertEquals( EnumerateDAGs.numDAGs(-1), 0 );
    }
    
    /** Test enumerateDAGs produces unique TOMs */
    public final void testEnumerateDAGs() {
        enumDag = new EnumerateDAGs();
        TOM[] dag = enumDag.enumerateDAGs(4);

        for ( int i = 0; i < dag.length; i++ ) {
            for ( int j = 0; j < dag.length; j++ ) {
                if ( (i != j) && dag[i].equals(dag[j]) ) {
                    System.out.println("\n\n----" + i + " = " + j );
                    System.out.println( dag[i] + "\n" + dag[j]);
                }
            }
        }
    }
    
    /** Test enumerateDAGs and FixedStructureSearch combined produces unique TOMs */
    public final void testFixedStructureSearch() {
        enumDag = new EnumerateDAGs();
        int numVars = 4;
        Value.Vector dagVec = enumDag._apply( numVars);
            
        Random rand = new Random(123);
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(rand,10,1,1,numVars);
        
        // Running all tests is a bit excessive, 20 should find any abnormalities. 
        int numTests = 10;
        if ( dagVec.length() < 10 ) { numTests = dagVec.length(); }
        
        TOM[] tom = new TOM[dagVec.length()];    
        for ( int i = 0; i < numTests; i++ ) {        
            Value elt = dagVec.elt(i);
            FixedStructureSearch fsSearch = new FixedStructureSearch(rand,data,
                                                                     CPTLearner.adaptiveCPTLearner, CPTLearner.mlMultinomialCPTLearner);
            fsSearch.setOption("currentTOM",elt);
            new BlockingSearch(fsSearch).start();
            tom[i] = fsSearch.getBestTOM();
            
            // tom[i] contains a reference to caseInfo for each search.
            // This created n instances of NodeCache which is an bib memory hog.
            // By copying the structure we allow this to be garbage collected.
            TOM tempTOM = new TOM(data);
            tempTOM.setStructure(tom[i]);
            tom[i] = tempTOM;
            
            //System.out.println(tom[i]);
            
            for ( int j = 0; j < i; j++ ) {
                assertFalse( tom[i].equals(tom[j]));
            }
        }
    }

    /** Test incrementing of n! orderings works. */
    public final void testIncrementOrder() {
        int order[] = new int[] {0,1,2};
        for ( int i = 0; i < 15; i++ ) {
            java.util.ArrayList<Integer> a = new java.util.ArrayList<Integer>();
            for ( int j = 0; j < order.length; j++ ) { a.add( new Integer(order[j]) ); }
            EnumerateDAGs.incrementOrder( order );
        }
        
    }
    
    public final void test_apply() {
        enumDag = new EnumerateDAGs();
        assertEquals( enumDag._apply(3).length(), 25 );
    }
    
}
