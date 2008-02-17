//
// Test Simple Search for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestGreedyLookaheadSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import cdms.plugin.search.*;
import camml.core.search.*;
import camml.core.models.bNet.*;
import camml.core.models.ModelLearner;
import camml.core.library.BlockingSearch;


//import camml.models.*;
//import cdms.plugin.model.*;
//import camml.SearchPackage.*;


import javax.swing.*;



public class TestGreedyLookaheadSearch extends TestCase
{
	protected static Value.Model binaryMultistateModel;
	
	public TestGreedyLookaheadSearch(String name) 
	{
		super(name);
	}
	
	// Create some data sets to test with.
	protected void setUp() 
	{
		binaryMultistateModel = new cdms.plugin.model.Multinomial(0,1);
	}
	
	public static Test suite() 
	{
		return new TestSuite(TestGreedyLookaheadSearch.class);
	}
	
	
	public void testBlockingSearch()
	{
		System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

		Value.Vector dataset = 
			SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),1000,1,2,2);
		
		GreedyLookaheadSearch search = new GreedyLookaheadSearch( new java.util.Random(12345),
				dataset, 1 );
		Search blockingSearch = new BlockingSearch( search );
		
		blockingSearch.start();
	}
	
	
	public void _testSearchInterface()
	{
		Value.Vector dataset = 
			SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
		
		JFrame f = new JFrame("Greedy Search Object");
		Search s = new Search( new GreedyLookaheadSearch( new java.util.Random(12345), 
				dataset, 1 ));
		f.getContentPane().add(new SearchControl(s,new JLabel("GreedyLookahead")));
		f.pack();
		f.setVisible(true);
		
		s.start();
	}
	
	
	
	/** Test main function. */
	public static void main( String args[] )
	throws ModelLearner.LearnerException 
	{
		if (args.length != 3)
			throw new IllegalArgumentException("Correct Syntax java <prog> seed samples lookahead");
		
		int seed = Integer.parseInt(args[0]);
		int samples = Integer.parseInt(args[1]);
		int lookahead = Integer.parseInt(args[2]);
		
		java.util.Random rand = new java.util.Random( seed );
		Value.Vector dataset = 
			SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),samples,2,2,2);
		
		
		JFrame f = new JFrame("Greedy Search Object");
		f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE  );
		final GreedyLookaheadSearch greedy = new GreedyLookaheadSearch( rand, dataset, lookahead );
		Search s = new Search( greedy );
		
		
		final BNetInferenceVisualiser vis = new BNetInferenceVisualiser( greedy.getBNet() );
		vis.updateParams( greedy.getCurrentParams( greedy.getMMLModelLearner() ),
				greedy.getBNet().makeMissingStruct() );
		
		
		
		// f.getContentPane().add(new SearchControl(s,new JLabel("Hello")));
		f.getContentPane().add(new SearchControl(s,vis));
		f.pack();
		f.setVisible(true);
		
		
		
		/** Add a SearchListener updating BNet visualisation after each step. */
		s.addSearchListener( new Search.SearchListener() {
			public void reset( Search sender ) { }
			public void beforeEpoch( Search sender ) { }
			public void afterEpoch( Search sender ) {
				try {
					vis.updateParams( greedy.getCurrentParams( greedy.getMMLModelLearner() ),
							greedy.getBNet().makeMissingStruct() );
				} catch ( ModelLearner.LearnerException e ) { throw new RuntimeException(e); }
			}
			public void onCompletion( Search sender ) {}
		} );
	}
	
	
}
