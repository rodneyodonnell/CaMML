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
// JUnit test routines for DTreeGenerator model
//

// File: TestBNet.java
// Author: rodo@dgs.monash.edu.au
// Created on 28/02/2005

package camml.test.core.models.dTree;

import java.util.Random;

import junit.framework.*;

import camml.core.models.dTree.*;
import cdms.core.*;

/**
 * @author rodo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestDTreeGenerator extends TestCase {

    /** BNet model used in testing*/
    protected static DTree model;
    
    /** Parameters corresponding to model */
    protected static Value.Vector params;

    /** */
    public TestDTreeGenerator() { super(); }

    /**     */
    public TestDTreeGenerator(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite( TestDTreeGenerator.class );        
    }

    
    /** Initialise. */
    protected void setUp() throws Exception { 
    }

    /** Test KL function on Augmented networks */
    public final void testHighArityGenerate() throws Exception {
        Random rand = new Random(123);
        int arity = 2;
        double leafP = 0.25; 
        int numParents = 18;
        int parents[] = new int[numParents];
        for (int i = 0; i < parents.length; i++) { parents[i] = arity; }
        DTreeGenerator.generate(rand,arity,parents,leafP,true);
        //System.out.println(my);
    }

}
