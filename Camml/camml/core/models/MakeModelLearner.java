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
// Default way to create a model learner and handle options.
//

// File: MakeModelLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models;

import cdms.core.*;

/**
 * MakeModelLearner defines a standard way to create a a ModelLearner while passing in options.
 */

public abstract class MakeModelLearner extends Value.Function implements java.io.Serializable
{
    
    /** Constructor initialising types. */
    public MakeModelLearner( ) {
        super( new Type.Function( new Type.Vector( new Type.Structured(new Type[] { 
                            Type.STRING, Type.TYPE }, new String[] {"name","value"} ))
                ,Type.STRUCTURED )) ;
    }
    
    /** Shortcut apply method */
    public abstract ModelLearner _apply( String[] option, Value[] optionVal );
    
    /**
     * return a MultinomialLearner using specified options <br>
     * [ ("option", optionValue) ] <br>
     */
    public Value apply( Value v ) { 
        
        Value.Vector vec = (Value.Vector)v;
        String[] option = new String[vec.length()];
        Value[] optionVal = new Value[option.length];
        
        for ( int i = 0; i < option.length; i++ ) {
            Value.Structured elt = (Value.Structured)vec.elt(i);
            if (elt.length() != 2) { 
                throw new RuntimeException( "Invalid Option in makeLearnerStruct : " + elt );
            }
            option[i] = ((Value.Str)elt.cmpnt(0)).getString();
            optionVal[i] = elt.cmpnt(1);
        }
        
        return _apply( option, optionVal ).getFunctionStruct();
    }
    
    /**
     * Return an array of strings containing all options;
     * Strings may take the form "option name - description of option"
     */
    public abstract String[] getOptions();
    
    /** Remove a single String (index x) from s and return a new array. */
    public static String[] remove( String[] s, int x ) {
        String[] s2 = new String[s.length-1];
        for ( int i = 0; i < x; i++ ) { s2[i] = s[i]; }
        for ( int i = x; i < s2.length; i++ ) { s2[i] = s[i+1]; }
        return s2;
    }
    
    /** Remove a single Value (index x) from v and return a new array. */
    public static Value[] remove( Value[] v, int x ) {
        Value[] v2 = new Value[v.length-1];
        for ( int i = 0; i < x; i++ ) { v2[i] = v[i]; }
        for ( int i = x; i < v2.length; i++ ) { v2[i] = v[i+1]; }
        return v2;
    }
}



