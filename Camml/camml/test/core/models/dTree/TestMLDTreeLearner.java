//
//JUnit test routines for DTreeGenerator model
//
//Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
//Source formatted to 100 columns.
//4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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
