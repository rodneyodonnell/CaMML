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
// Netica Plugin
//

// File: Models.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.netica;

import cdms.core.*;
import norsys.netica.*;

/**
   Module to load Netica functions
*/
public class Netica extends Module 
{
    public static java.net.URL helpURL = Module.createStandardURL(Netica.class);
    
    public String getModuleName() { return "Netica"; }
    public java.net.URL getHelp() { return helpURL; }
    
    public void install(cdms.core.Value params) throws Exception
    {
        add("loadNet", NeticaFn.loadNet, "load a netica network from a file" );
        add("reorderNet", NeticaFn.reorderNet, "reorder a network" );
        //     add("cdmsPlot", cdms.plugin.twodplot.TestPlot.rodPlot, "Plot with a nice axis.");
        add("cdmsPlot", camml.core.library.CammlPlot.fancyPlot, "Plot with nice axis");
        add("convertToNeticaNet", NeticaFn.convertToNeticaNet, 
            "Convert from BNetStochastic to BNetNetica" );
        add("saveNet", NeticaFn.saveNet, "Save a netica network to a file.");
        add( "bNetClassify", NeticaFn.bNetClassify,
             "Calculate log probability of each BNet element given all other elements.");
        add( "classify", NeticaFn.classify, "Classify values for a single node of a BN.");
        add( "classifyProb", NeticaFn.classifyProb, 
             "Return classification probabilities for a single node of a BN.");

    }
    
    /** Default netica environment. */
    protected static Environ env;
    
    // set up Netica if necessary
    static {
        try {
            env = Environ.getDefaultEnviron();
            if(env == null)
                { env = new Environ(System.getProperty("netica.reg")); }
        } catch(NeticaException ne)
            { throw new RuntimeException(ne.toString()); }
    }
    /** accessor for Netica.env */
    public static Environ getEnv() { return env; }

}

