//
// Test Simple Search for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestUltraSimpleSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import cdms.plugin.search.*;
import camml.core.library.BlockingSearch;
import camml.core.search.*;

import javax.swing.*;



public class TestUltraSimpleSearch extends TestCase
{
	protected Value.Vector commonCauseDataset;
	
	public TestUltraSimpleSearch(String name) 
	{
		super(name);
	}
	
	// Create some data sets to test with.
	protected void setUp() 
	{
		commonCauseDataset = 
			SearchDataCreator.generateCommonCauseDataset( new java.util.Random(123), 200 );
	}
	
	public static Test suite() 
	{
		return new TestSuite(TestUltraSimpleSearch.class);
	}
	
	public void testSearchInterface()
	{
		System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

		JFrame f = new JFrame("Simple Search Object");
		Value.Vector dataset = 
			SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
		Search s = new BlockingSearch( new UltraSimpleSearch( new java.util.Random(12345), dataset ));
		f.getContentPane().add(new SearchControl(s,new JLabel("UltraSimpleSearch")));
		f.pack();
		//f.setVisible(true);
		
		s.start();
	}
	
	
	
	/** Test main function. */
	public static void main( String args[] )
	{
		if (args.length != 2)
			throw new IllegalArgumentException("Correct Syntax java <prog> seed samples");
		
		java.util.Random rand = new java.util.Random( (Integer.parseInt(args[0])+1)*777 );
		Value.Vector data = SearchDataCreator.generateCommonCauseDataset(
				new java.util.Random(Integer.parseInt(args[0])),
				Integer.parseInt(args[1]));
		
		
		JFrame f = new JFrame("Sample Search Object");
		Search s = new Search(new UltraSimpleSearch( rand, data ));
		f.getContentPane().add(new SearchControl(s,new JLabel("Hello")));
		f.pack();
		f.setVisible(true);
	}
	
	
}
