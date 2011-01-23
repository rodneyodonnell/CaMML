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
// RodoCamml wrapper plugin
//

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

