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
// Class to create CPT Learners with a given set of options.
//

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
