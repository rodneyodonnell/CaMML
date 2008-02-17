//
// JUnit test routine for CDMS Types
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestType.java
// Modified : rodo@dgs.monash.edu.au

package test.cdms.core;

import junit.framework.*;

import cdms.core.Type;
import java.io.*;

/**
 * Test routine for Type.java
 * Initially only tests serialization
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.3 $ $Date: 2006/08/22 22:40:05 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/CDMS/test/cdms/core/TestType.java,v $
 */
public class TestType extends TestCase
{
	public TestType(String name) 
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
	
	/** Serialise an object o then read it back in. Used for testing serialisation. */
	public static Object rewrite(Object o) throws IOException, ClassNotFoundException {
		// Open output stream
		ObjectOutputStream oStream = new ObjectOutputStream(new FileOutputStream("temp.out"));
		
		// Write object
		oStream.writeObject(o);
		oStream.close();

		// reread object
		ObjectInputStream iStream = new ObjectInputStream( new FileInputStream("temp.out"));
		return iStream.readObject();	
	}

	/**
	 * Call rewrite on several types. Standard sytem types should "==" eachother,
	 * non-standard types should not.
	 */
	public void testSerialize()  throws IOException, ClassNotFoundException
	{
		// Create array of "Standard" and "Non-Standard" types
		Type[] standard = new Type[] {Type.STRUCTURED, Type.VECTOR, Type.TRIV, Type.TYPE};

		// All Standard types should return true to "==" after serialization
		Type[] sCopy = new Type[standard.length];
		for ( int i = 0 ; i < standard.length; i++ ) {
			sCopy[i] = (Type)rewrite(standard[i]);
			assertEquals( standard[i], sCopy[i] );
		}

		// Non-Standard types may or may not return true to "==".
		// Subtypes (eg. vector elements) should return true to  "=="
		Type[] nonStandard = new Type[standard.length];
		Type[] nsCopy = new Type[nonStandard.length];
		for ( int i = 0 ; i < nonStandard.length; i++ ) {
			nonStandard[i] = new Type.Vector(standard[i]);
			nsCopy[i] = (Type)rewrite(nonStandard[i]);
			assertEquals( ((Type.Vector)nonStandard[i]).elt, ((Type.Vector)nsCopy[i]).elt );
		}
			
	}
	
}
