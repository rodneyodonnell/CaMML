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
// Test cases for BNetNetica
//

// File: TestBNetNetica.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.netica;

import java.util.Random;

import norsys.netica.Net;

import camml.plugin.netica.BNetNetica;
import camml.plugin.netica.Netica;
import camml.plugin.netica.NeticaFn;
import cdms.core.Value;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test cased for BNetNetica
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.6 $ $Date: 2006/09/05 01:57:35 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/netica/TestBNetNetica.java,v $
 */

public class TestBNetNetica extends TestCase {
    public static Test suite() 
    {
        return new TestSuite(TestBNetNetica.class);
    }

    protected void setUp() throws Exception    {
    }


    /** Attempt to load a netica file. */
    public void testLoadNet() throws Exception {
        Value.Structured v = NeticaFn.LoadNet._apply("camml/test/TestRegression.dne");
        assertEquals(v.length(), 2);
        assertTrue(v.cmpnt(0) instanceof Value.Model);
        assertTrue(v.cmpnt(1) instanceof Value.Vector);
    }

    public void testSaveNet() throws Exception {
        Value.Structured v = NeticaFn.LoadNet._apply("camml/test/TestRegression.dne");
        BNetNetica m = (BNetNetica)v.cmpnt(0);
        Value.Vector params = (Value.Vector) v.cmpnt(1);

        Value.Structured saveStruct = new Value.DefStructured( new Value[] {
                new Value.Str("temp.dne"),m,params
            });
        
        String s1 = m.exportNetica("cammlNet",params);
        String s2 = ((Value.Str)NeticaFn.saveNet.apply(saveStruct)).getString();

        assertEquals(s1,s2);
        
    }
    
    /** Test logPClassify function. */
    public void testLogPClassify() {
        // Load "Asia" network (nenamed as TestRegression)
        Value.Structured my = NeticaFn.LoadNet._apply("camml/test/TestRegression.dne");
        BNetNetica m = (BNetNetica)my.cmpnt(0);
        Value.Vector y = (Value.Vector)my.cmpnt(1);
        
        // Generate some test data.
        Random r = new Random(123);
        Value.Vector data = m.generate(r,10,y,Value.TRIV);
        
        // Classify each node in turn.
        double [][] logPArray = new double[y.length()][];
        for ( int i = 0; i < logPArray.length; i++) {
            logPArray[i] = m.probClassify(y,data,i); 
        }
    
        // Correct answers for 10 datapoints with seed=123
        double [][] trueResult = {
            {-0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994, -0.009646180038733994},
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            {-0.41836848198062093, -0.4382549607334781, -1.0732945402884664, -0.4382549607334781, -0.4382549607334781, -1.0360919614890984, -1.0732945402884664, -0.4382549607334781, -1.0732945402884664, -1.0732945402884664},
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            {-0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688, -0.05129330693589688},
            {-1.4880770852321559, -0.08004270270648271, -0.2876820724517809, -0.08004270270648271, -0.08004270270648271, -0.25593340393952346, -0.2876820724517809, -0.08004270270648271, -0.2876820724517809, -2.5649494170661833},
            {-2.3025850780928847, -0.22314353641304868, -0.10536054214877988, -0.22314353641304868, -0.22314353641304868, -0.22314353641304868, -0.10536054214877988, -0.22314353641304868, -0.10536054214877988, -2.3025850780928847}
        };
        
        // Chec results have not changed.
        for ( int i = 0; i < trueResult.length; i++) {
            for (int j = 0; j < trueResult[i].length; j++) {
                assertEquals( Math.exp(trueResult[i][j]), logPArray[i][j], 0.000001 );
            }
        }
    }    
    
    /** Version 2.21 (and before) of NeticaJ cannot handle threaded code properly.
     *  As such, CaMML may randomly crash when using netica. 
     *  This test isolates (a manifestation of) that bug. */
    public void testNeticaThreadingBroken() throws Exception {
        // Ensure that Environ.getDefaultEnviron() has been initalised.
        System.out.println("Netica.env = " + Netica.getEnv() + "\t in testRaceCondition2");
        
        // Start several threads simultaneously consructing and
        // destructing new Net objects simultaneously.
        // This may cause netica to throw an exception.
        Thread thread[] = new Thread[200];
        for (int i = 0; i < thread.length; i++) {
            thread[i] = new Thread() {
                    public void run() {
                        try {
                            for ( int j = 0; j < 100; j++) {                        
                                Net net = new Net();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    };
                };
        }
        
        // Start all other threads.
        for ( int i = 0; i < thread.length; i++) { thread[i].start(); }
        
        // Wait for all other threads to finish.
        for (Thread t : thread) {
            while (t.isAlive()) Thread.yield();
        }
    }    
}
