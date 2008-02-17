//
// Module for Tetrad4 interface.
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: RodoCamml.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.tetrad4;

import cdms.core.*;

/**
* Module to interface with Tetrad IV functions.  <br>
*  Note: This plugin requires a copy of Tetrad4.jar in the classpath.
*   
* @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
* @version $Revision$ $Date$
* $Source$
*/
public class Tetrad4Module extends Module {
	public static java.net.URL helpURL = Module
			.createStandardURL(Tetrad4Module.class);

	public String getModuleName() {
		return "Tetrad4Module";
	}

	public java.net.URL getHelp() {
		return helpURL;
	}

	public void install(Value params) throws Exception {
		add("GES", TetradLearner.ges.getFunctionStruct(), "Tetrad GES functions. Member of SEC chosen at random.");
		add("FCI", TetradLearner.fci.getFunctionStruct(), "Tetrad SEC functions. Member of SEC chosen at random.");
		add("PC", TetradLearner.pcRepair.getFunctionStruct(), 
				"Tetrad PC functions. Member of SEC chosen at random. Broken SECs repaired.");
		if ( params instanceof Value.Vector) {
			Value.Vector pVec = (Value.Vector)params;
			for ( int i = 0; i < pVec.length(); i++) {
				Value.Structured elt = (Value.Structured)pVec.elt(i);
				String s = ((Value.Str)elt.cmpnt(0)).getString();
				if (s.equals("stringParams") ) { 
					TetradLearner.stringParams = (elt.cmpnt(1) == Value.TRUE);
					System.out.println("stringParams = " + TetradLearner.stringParams);
				}
				else if (s.equals("useVariableNames")) {
					TetradLearner.useVariableNames = (elt.cmpnt(1) == Value.TRUE);
					System.out.println("stringParams = " + TetradLearner.useVariableNames);
					
				}
			}
		}
	}


}
