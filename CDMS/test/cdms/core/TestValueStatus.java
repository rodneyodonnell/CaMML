//
//JUnit test routine for CDMS ValueStatus
//
//Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
//Source formatted to 100 columns.
//4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

//File: TestType.java
//Modified : rodo@dgs.monash.edu.au

package test.cdms.core;

import junit.framework.*;

import cdms.core.Value;

import java.io.*;

/**
* Test routine for ValueStatus.java
* Initially only tests serialization
*
* @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
* @version $Revision: 1.4 $ $Date: 2006/08/22 22:40:05 $
* $Source: /u/csse/public/bai/bepi/cvs/CAMML/CDMS/test/cdms/core/TestValueStatus.java,v $
*/
public class TestValueStatus extends TestCase
{
	public TestValueStatus(String name) 
	{
		super(name);
	}
	
	protected void setUp() 
	{
	}
	
	public static Test suite() 
	{
		return new TestSuite(TestType.class);
	}
	
	/**
	 * Call rewrite on several status values. Standard status values types should "==" eachother,
	 */
	public void testSerialize()  throws IOException, ClassNotFoundException
	{
		assertEquals(Value.S_INTERVENTION, TestType.rewrite(Value.S_INTERVENTION));
		assertEquals(Value.S_INVALID, TestType.rewrite(Value.S_INVALID));
		assertEquals(Value.S_IRRELEVANT, TestType.rewrite(Value.S_IRRELEVANT));
		assertEquals(Value.S_NA, TestType.rewrite(Value.S_NA));
		assertEquals(Value.S_PROPER, TestType.rewrite(Value.S_PROPER));
		assertEquals(Value.S_UNOBSERVED, TestType.rewrite(Value.S_UNOBSERVED));		
	}
	
}
