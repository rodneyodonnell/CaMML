//
// Augment Model plugin
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: Augment.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.augment;

import cdms.core.*;

/**
   Module to load Augment functions
 */
public class Augment extends Module 
{
    public static java.net.URL helpURL = Module.createStandardURL(Augment.class);
    
    public String getModuleName() { return "Augment"; }
    public java.net.URL getHelp() { return helpURL; }
    
    public void install(Value params) throws Exception
    {
    	add("augment", AugmentFN.augment, "augment a network" );
    	add("augment2", AugmentFN2.augment2, "augment a network" );
    	add("augment3", AugmentFN3.augment3, "augment a network" );
    }
}

