//
// Module containing camml.SearchPackage Functions
//
// Copyright (C) 2002 Rodney O'Donnell  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: SearchPackage.java
// Author: rodo@csse.monash.edu.au

package camml.core.search;

import cdms.core.*;
import camml.core.models.ModelLearner;

import camml.core.models.bNet.BNetLearner;
import camml.core.models.cpt.*;
import camml.core.models.dTree.ForcedSplitDTreeLearner;
import camml.core.models.dual.DualLearner;

/**
 Module containing various models to be used by camml.
 */
public class SearchPackage extends Module
{
	public static java.net.URL helpURL = Module.createStandardURL(SearchPackage.class);
	
	public String getModuleName() { return "CammlSearchPackage"; }
	public java.net.URL getHelp() { return helpURL; }
	
	public void install(Value params) throws Exception
	{
		
		add("commonCauseDataset", SearchDataCreator.getCommonCauseDataset(), 
		"a <- (), b <- (a,c), c <- ()  --- All variables binary " );
		add("commonCauseDataset2", SearchDataCreator.getFourVariableCommonCauseDataset(), 
		"a <- (), b <- (a,c), c <- (), d <- () --- All variables binary" );
		add("uncorrelatedDataset", SearchDataCreator.getUncorrelatedDataset(),
		"a <- (), b <- (), c <- () --- a = ternary, (b.c) = binary");
		
		add("bivariateDataset", 
				SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(123), 
						1000,1,1,2),
		"a <- (), b <- (a)");
		
		add("makeWallaceKorbDataset", makeWallaceKorbDataset, "makeWallaceKorbDataset");
		add("getRepresentativeDAG", MMLEC.getRepresentative, "Get DAG representing SEC or MMLEC.");
	}
	
	/** 
	 * Create a WallaceKorb99 style dataset. This essentially creates a 3d cube with variables
	 * spaced on all corners as if the cube were filled with 1*1*1 subcubes.  Each variable
	 * has a causal link to the variable above, behing and to it's right (naturally depending on
	 * your orientation).  So most variables will have three inputs, variables on some faces will
	 * have one or two causes, and the variable in the top front left corned will have no causes.
	 * <br>
	 * (samples,length,width,height) -> [(...)]
	 */
	public static final MakeWallaceKorbDataset makeWallaceKorbDataset =new MakeWallaceKorbDataset();
	
	/** 
	 * Create a WallaceKorb99 style dataset. This essentially creates a 3d cube with variables
	 * spaced on all corners as if the cube were filled with 1*1*1 subcubes.  Each variable
	 * has a causal link to the variable above, behing and to it's right (naturally depending on
	 * your orientation).  So most variables will have three inputs, variables on some faces will
	 * have one or two causes, and the variable in the top front left corned will have no causes.
	 * <br>
	 * (samples,length,width,height) -> [(...)]
	 */
	public static class MakeWallaceKorbDataset extends Value.Function
	{
		/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 2322911878153470717L;

		public MakeWallaceKorbDataset( ) {
			super( new Type.Function( new Type.Structured (new Type[] 
																	{ Type.DISCRETE, Type.DISCRETE, Type.DISCRETE, Type.DISCRETE},
																	new String[] 
																			   {"numSamples","length","width","height"}) , Type.VECTOR  ));
		}
		
		public Value apply( Value v ) {
			Value.Structured struct = (Value.Structured)v;
			
			
			return SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(123),
					struct.intCmpnt(0),
					struct.intCmpnt(1),
					struct.intCmpnt(2), 
					struct.intCmpnt(3));
		}
	}
	
	/** CPT learner with ML costing function */
	public final static ModelLearner mlCPTLearner = CPTLearner.mlMultinomialCPTLearner;
	
	/** CPT learner with MML style costing function (as found in oldCamml) */
	public final static ModelLearner mmlCPTLearner = CPTLearner.mmlAdaptiveCPTLearner;
	
	/** DTree Learner */
	public final static ModelLearner dTreeLearner = 
		ForcedSplitDTreeLearner.multinomialDTreeLearner;
	
	/** Dual Learner */
	public final static ModelLearner dualLearner = DualLearner.dualLearner;
	
	/** BNet learner based on DTreeLearner */
	public final static ModelLearner bNetDTreeLearner = 
		new BNetLearner( mlCPTLearner, dTreeLearner, false, false );

	/** BNet learner based on DualLearner (CPT & DTree) */
	public final static ModelLearner bNetDualLearner1 = 
		new BNetLearner( mlCPTLearner, DualLearner.dualCPTDTreeLearner, false, false );

	/** BNet learner based on DualLearner (CPT & Logit) */
	public final static ModelLearner bNetDualLearner2 = 
		new BNetLearner( mlCPTLearner, DualLearner.dualCPTLogitLearner, false, false );

	/** BNet learner based on DualLearner (Logit & DTree) */
	public final static ModelLearner bNetDualLearner3 = 
		new BNetLearner( mlCPTLearner, DualLearner.dualDTreeLogitLearner, false, false );

	/** BNet learner based on DualLearner (CPT, Logit & DTree) */
	public final static ModelLearner bNetTriLearner = 
		new BNetLearner( mlCPTLearner, DualLearner.dualCPTDTreeLogitLearner, false, false );

	/** BNet learner based on CPTLearner */
	public final static ModelLearner bNetCPTLearner = 
		new BNetLearner( mlCPTLearner, mmlCPTLearner, false, false );

}
