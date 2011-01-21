//
// Default way to create a model learner and handle options.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456780

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



