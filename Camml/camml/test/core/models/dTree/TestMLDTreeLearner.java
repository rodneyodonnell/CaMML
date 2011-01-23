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
//JUnit test routines for DTreeGenerator model
//

package camml.test.core.models.dTree;

import java.util.Random;

import junit.framework.*;

import camml.core.library.SelectedVector;
import camml.core.models.cpt.CPTLearner;
import camml.core.models.dTree.*;
import camml.core.search.SearchDataCreator;
import camml.plugin.weka.Weka;
import cdms.core.*;

/**
 * @author rodo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestMLDTreeLearner extends TestCase {

    /** BNet model used in testing*/
    protected static DTree model;
    
    /** Parameters corresponding to model */
    protected static Value.Vector params;

    /** */
    public TestMLDTreeLearner() { super(); }

    /**     */
    public TestMLDTreeLearner(String name) { super(name); }
    
    public static Test suite() 
    {
        return new TestSuite( TestMLDTreeLearner.class );        
    }

    
    /** Initialise. */
    protected void setUp() throws Exception { 
    }

    /** Ensure that Max LH CPT and DTree models return the same answer. */
    public final void testMLTreeVsCPT2() throws Exception {
        Random r = new Random(123);
        Value.Vector data30 = SearchDataCreator.generateWallaceKorbStyleDataset(r,1000,20,1,1);
        
        for (int ii = 0; ii < 15; ii++) {        
            Value.Vector x = data30.cmpnt(0);
            int[] cmpnts = new int[ii];
            for (int i = 0; i < cmpnts.length; i++) { cmpnts[i] = i+1;}
            Value.Vector z = new SelectedVector(data30,null,cmpnts);
        
            double cost1 = MLDTreeLearner.mlDTreeLearner.parameterizeAndCost(Value.TRIV, x, z);
            double cost2 = CPTLearner.mlMultinomialCPTLearner.parameterizeAndCost(Value.TRIV, x, z);
            
            assertEquals( cost1, cost2, 0.0001 );
        }
    }


    /** Ensure that Max LH CPT and DTree models return the same answer. */
    public final void testMLTreeVsCPT() throws Exception {
        Value.Vector data = 
            Weka.load("camml/test/letter.symbolic.arff", false, false);
        
        for (int ii = 0; ii < 3; ii++) {        
            Value.Vector x = data.cmpnt(0);
            int[] cmpnts = new int[ii];
            for (int i = 0; i < cmpnts.length; i++) { cmpnts[i] = i+1;}
            Value.Vector z = new SelectedVector(data,null,cmpnts);
        
            double cost1 = MLDTreeLearner.mlDTreeLearner.parameterizeAndCost(Value.TRIV, x, z);
            double cost2 = CPTLearner.mlMultinomialCPTLearner.parameterizeAndCost(Value.TRIV, x, z);
            
            //System.out.println(ii + "\t" + cost1 + "\t" + cost2);
            assertEquals( cost1, cost2, 0.0001 );
        }
    }

}
