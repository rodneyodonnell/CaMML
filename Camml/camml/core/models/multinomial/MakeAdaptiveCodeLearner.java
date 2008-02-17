//
// Class to create AdaptiveCodeLearners with given options.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MakeAdaptiveCodeLearner.java
// Author: rodo@dgs.monash.edu.au
// Created on 20/02/2005

package camml.core.models.multinomial;

import camml.core.models.MakeModelLearner;
import camml.core.models.ModelLearner;
import cdms.core.Value;


/** MakeAdaptiveCodeLearner returns a AdaptiveCodeLearner given a options. */
public class MakeAdaptiveCodeLearner extends MakeModelLearner
{
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2278791859853347308L;
	/** Default implementation of makeAdaptiveCodeLearner */
	public static final MakeAdaptiveCodeLearner makeAdaptiveCodeLearner = 
		new MakeAdaptiveCodeLearner();
	
	
	public MakeAdaptiveCodeLearner( ) { }
	
	/** Shortcut apply method */
	public ModelLearner _apply( String[] option, Value[] optionVal ) {  
		
		// Set default values.
		double bias = 1.0;
		boolean useMML = false;
		
		
		// Search options for overrides.
		for ( int i = 0; i < option.length; i++ ) {
			if ( option[i].equals("bias") ) {
				bias = ((Value.Scalar)optionVal[i]).getContinuous();
			}
			else if ( option[i].equals("useMML") ) {
				useMML = (((Value.Discrete)optionVal[i]).getDiscrete() == 0);
			}
			else { throw new RuntimeException("Unknown option : " + option[i] );}
		}
		
		return new AdaptiveCodeLearner( bias, useMML );
	}
	
	public String[] getOptions() { return new String[] {
			"bias - p(i) = (n(i) + bias) / (sum(n(i)+bias))",
			"useMML - add (|x|-1)!*log(Pi*e/6)/2 as an approximation to MML coding"
	}; }
	
}