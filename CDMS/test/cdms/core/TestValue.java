//
// JUnit test routine for CDMS Values
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestValue.java
// Modified : rodo@dgs.monash.edu.au

package test.cdms.core;

import junit.framework.*;

import cdms.core.Value;
import java.io.*;

/**
 * Test routine for Value.java
 * Initially only tests serialization
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.3 $ $Date: 2006/08/22 22:40:05 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/CDMS/test/cdms/core/TestValue.java,v $
 */
public class TestValue extends TestCase
{
	public TestValue(String name) 
	{
		super(name);
	}
	
	protected void setUp() 
	{
	}
	
	public static Test suite() 
	{
		return new TestSuite(TestValue.class);
	}
	
	/**
	 * Test if Value.TRIV is serialised/unserialised properly.
	 */
	public void testSerialize()  throws IOException, ClassNotFoundException
	{
		assertEquals( Value.TRIV, TestType.rewrite(Value.TRIV) );			
	}
	
}
