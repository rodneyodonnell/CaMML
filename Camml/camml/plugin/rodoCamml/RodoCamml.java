//
// RodoCamml wrapper plugin
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: RodoCamml.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.rodoCamml;

import cdms.core.*;

/**
 * Module to interface with the "rodo" version of camml. (this mainly exists for regression testing
 *  but may also be used if faster execution times are required.)
 *  Note: This plugin requires a compiled version of Camml to be present.
 */
public class RodoCamml extends Module 
{
    public static java.net.URL helpURL = Module.createStandardURL(RodoCamml.class);
    
    public String getModuleName() { return "RodoCamml"; }
    public java.net.URL getHelp() { return helpURL; }
    
    public void install(Value params) throws Exception
    {
        add("load", RodoCammlIO.load, "load a .cas file" );
        add("store", RodoCammlIO.store, "store a .cas file" );
        add("storeOld", RodoCammlIO.storeInOldFormat, "store a .cas file, compatible with oldCamml" );
        add("makeRodoCammlLearner", RodoCammlLearner.makeRodoCammlLearner,
            "return a bNet Learner Struct given options [(\"option\", optionVal)]");
        add("rodoParameterize", RodoCammlLearner.modelLearner.getFunctionStruct(), "RodoCammlLearner");
        add("julesCPT", RodoCammlLearner.julesCPTLearner.getFunctionStruct(), "Julian Neil CPT Learner");
        add("julesLogit", RodoCammlLearner.julesLogitLearner.getFunctionStruct(), "Julian Neil Logit learner");
        add("julesDual", RodoCammlLearner.julesDualLearner.getFunctionStruct(), "Julian Neil Dual Learner");
        RodoCammlLearner.modelLearner.getFunctionStruct().install(null);

    }
}

