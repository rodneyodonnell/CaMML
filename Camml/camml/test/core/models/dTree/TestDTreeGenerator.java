//
// JUnit test routines for DTreeGenerator model
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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

	/**	 */
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
