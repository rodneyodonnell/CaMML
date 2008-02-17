//
// Module containing camml Models and Functions
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: Models.java
// Author: rodo,@csse.monash.edu.au

package camml.core.models;

import cdms.core.*;
import camml.core.models.bNet.*;
import camml.core.models.cpt.*;
import camml.core.models.dTree.*;
import camml.core.models.dual.*;
import camml.core.models.logit.LogitLearner;
import camml.core.models.multinomial.*;
import camml.core.models.normal.*;
import camml.plugin.intervention.InterventionLearner;

/**
 Module containing various models to be used by camml.
 */
public class Models extends Module
{
	public static java.net.URL helpURL = Module.createStandardURL(Models.class);
	
	public String getModuleName() { return "CammlModels"; }
	public java.net.URL getHelp() { return helpURL; }
	
	public void install(Value params) throws Exception
	{
		add("cptCreator", CPTCreator.cptCreator, "Creates a CPT model");
		add("visualiseBNet", BNetVisualiser.visualiseBNet, "Visualise a Bayesian Network");
		add("visualiseInferenceBNet", 
				BNetInferenceVisualiser.visualiseInferenceBNet, "Visualise a Bayesian Network");
		add("visualiseDTree", DTreeVisualiser.visualiseDTree, "Visualise a Decision Tree");
		//      add("bNet",BNet.bNet,"Bayesian Network Model");
		add("superMultinomial", camml.core.models.multinomial.MultinomialGenerator.generator,"");
		add("superDTree", camml.core.models.dTree.DTreeGenerator.generator,"");
		add("superBNet", camml.core.models.bNet.BNetGenerator.generator,"");
		
		add("maskMissing", camml.core.models.bNet.BNet.maskMissingFN,
		"Mask off missing values ((..)or[(...)], (bool)) -> (...)or[(...)]");
		
		NormalLearner.normalLearner.getFunctionStruct().install(null);
		MLMultinomialLearner.mlMultinomialLearner.getFunctionStruct().install(null);
		MultinomialLearner.multinomialLearner.getFunctionStruct().install(null);
		UnimodalMultinomialLearner.unimodalMultinomialLearner.getFunctionStruct().install(null);
		
		DualLearner.dualLearner.getFunctionStruct().install(null);
		DTreeLearner. multinomialDTreeLearner.getFunctionStruct().install(null);
		ForcedSplitDTreeLearner.multinomialDTreeLearner.getFunctionStruct().install(null);
		MLDTreeLearner.mlDTreeLearner.getFunctionStruct().install(null);
		//PenalisedDTreeLearner.multinomialDTreeLearner.getFunctionStruct().install(null);				
		LogitLearner.logitLearner.getFunctionStruct().install(null);
		
		CPTLearner.mmlAdaptiveCPTLearner.getFunctionStruct().install(null);
		InterventionLearner.mmlInterventionCPTLearner.getFunctionStruct().install("mmlIntCPT");
		InterventionLearner.mlInterventionCPTLearner.getFunctionStruct().install("mlIntCPT");
		
		// ModelLearners for various dual models.
		DualLearner.dualCPTDTreeLogitLearner.getFunctionStruct().install("dualCTL");
		DualLearner.dualCPTDTreeLearner.getFunctionStruct().install("dualCT");
		DualLearner.dualCPTLogitLearner.getFunctionStruct().install("dualCL");
		DualLearner.dualDTreeLogitLearner.getFunctionStruct().install("dualTL");
		
		
		//       CPTLearner.multinomialCPTLearner.getFunctionStruct().install("MMLCPT");
		//       CPTLearner.mlMultinomialCPTLearner.getFunctionStruct().install("MLCPT");
		
		//       CPTLearner.adaptiveCPTLearner.getFunctionStruct().install("AdaptiveCPT");
		//       CPTLearner.mmlAdaptiveCPTLearner.getFunctionStruct().install("MMLAdaptiveCPT");
		//       CPTLearner.adaptiveCPTLearner2.getFunctionStruct().install("AdaptiveCPT2");
		//       CPTLearner.mmlAdaptiveCPTLearner2.getFunctionStruct().install("MMLAdaptiveCPT2");
		
		add("makeForcedSplitDTreeLearner", 
				camml.core.models.dTree.ForcedSplitDTreeLearner.makeForcedSplitDTreeLearnerStruct,
		"return a Forced Split DTree Learner Struct given options [(\"option\", optionVal)]");
		
		add("makeCPTLearner", camml.core.models.cpt.MakeCPTLearner.makeCPTLearner,
		"return a CPT Learner Struct given options [(\"option\", optionVal)]");
		
		
		add("makeMultinomialLearner", 
				camml.core.models.multinomial.MultinomialLearner.makeMultinomialLearner,
		"return a Multinomial Learner Learner Struct given options [(\"option\", optionVal)]");
		
		add("makeMultiLearner", camml.core.models.dual.MultiLearner.makeMultiLearner,
		"return a CPT Learner Struct given options [(\"option\", optionVal)]");
		
		add("makeBNetLearner", camml.core.models.bNet.BNetLearner.makeBNetLearner,
		"return a bNet Learner Struct given options [(\"option\", optionVal)]");
		
		//       WallaceCPTLearner.multinomialCPTLearner.getFunctionStruct().install("WallaceCPT");
		//       WallaceCPTLearner.mekldLearner.getFunctionStruct().install("MEKLD_CPT");
		
	}
	
}
