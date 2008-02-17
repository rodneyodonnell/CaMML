//
// Netica Plugin
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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
	// 	add("cdmsPlot", cdms.plugin.twodplot.TestPlot.rodPlot, "Plot with a nice axis.");
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

