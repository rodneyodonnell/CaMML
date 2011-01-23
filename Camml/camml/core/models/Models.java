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
// Module containing camml Models and Functions
//

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
