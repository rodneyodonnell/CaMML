//
// Class to create CPT Learners with a given set of options.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MakeCPTLearner.java
// Author: rodo@dgs.monash.edu.au
// Created on 20/02/2005

package camml.core.models.cpt;

import camml.core.models.FunctionStruct;
import camml.core.models.MakeModelLearner;
import camml.core.models.ModelLearner;
import camml.core.models.multinomial.AdaptiveCodeLearner;
import cdms.core.Value;


/** MakeCPTLearner returns a CPTLearner given a "leafLearner" in its options. */
public class MakeCPTLearner extends MakeModelLearner
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -5513660850923106236L;
    /** Default implementation of makeCPTLearner */
    public static final MakeCPTLearner makeCPTLearner = new MakeCPTLearner();

    public MakeCPTLearner( ) { }
    
    /** Shortcut apply method */
    public ModelLearner _apply( String[] option, Value[] optionVal ) {  
        
        // Set default values.
        ModelLearner leafLearner = AdaptiveCodeLearner.adaptiveCodeLearner2;
        
        // Search options for overrides.
        for ( int i = 0; i < option.length; i++ ) {
            if ( option[i].equals("leafLearner") ) {
                leafLearner = ((FunctionStruct)optionVal[i]).getLearner();
            }
            else { throw new RuntimeException("Unknown option : " + option[i] );}
        }
        
        return new CPTLearner( leafLearner );
    }
    
    public String[] getOptions() { return new String[] {
            "leafLearner -- Learner used for each subModel.  Default is AdaptiveCode"
        }; }
}
