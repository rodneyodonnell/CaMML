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
// Class to create AdaptiveCodeLearners with given options.
//

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
