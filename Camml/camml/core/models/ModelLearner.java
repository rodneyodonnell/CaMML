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
// Standard interface for Model Learners to implement.
//

// File: ModelLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models;

import cdms.core.*;

/**
 * Interface defining a standard way to access Model Learner functions.
 * 
 * The functions : <br>
 * abstract public Value parameterize( Value i, Value.Vector x, Value.Vector z );         <br>
 * abstract public Value sParameterize( Value.Model model, Value stats );                 <br>
 * abstract public double cost( Value i, Value.Vector x, Value.Vector z, Value params );  <br>
 * abstract public double sCost( cdms.core.Value stats, cdms.core.Value params );         <br>
 * must be overloaded. <br>
 *
 * The function : <br>
 * double msyCost( Value.Structured modelStatsParams );         <br>
 * has a default implementation but may be overloaded.          <br>
 * <br>
 * 
 * Note: all the above functions take the form of curried functions.  This means that instead of
 *       passing several parameters to a function, a function requireing multiple parameters returns
 *       is simulated by a function which takes one value, and returns a function requireing extra
 *       parameters.  "Functions are first class values".
 */
public interface ModelLearner extends java.io.Serializable
{    
    /** Return the type of model being learned. */
    public Type.Model getModelType();
    
    /** Return type of initial information*/
    public Type getIType();
    
    /** Function should be overloaded to accept i,x,z and return (m,s,y) */
    public Value.Structured parameterize( Value i, Value.Vector x, Value.Vector z )
        throws LearnerException;
    
    /** 
     * Function should be overloaded to accept m,s and return (m,s,y) <br>
     * NOTE : m is required as stats are defined in terms of a model
     */
    public Value.Structured sParameterize( Value.Model m, Value stats )
        throws LearnerException;
    
    /** 
     * Function should be overloaded to accept i,x,z,y and return a double  <br>
     * NOTE: m is required as params are defined in terms of a model
     */
    public double cost( Value.Model m, Value i, Value.Vector x, Value.Vector z, Value params )
        throws LearnerException;
    
    /** 
     * Function should be overloaded to accept s,y and return a double <br>
     * NOTE: m is required as params and stats are defined in terms of a model
     */
    public double sCost( Value.Model m, Value stats, Value params )
        throws LearnerException;
    
    /** Function splits msy into it's components and calls sCost(s,y) */
    public double msyCost( Value.Structured msy )
        throws LearnerException;
    
    /** Parameterize and cost data all in one hit.   */
    public double parameterizeAndCost( Value i, Value.Vector x, Value.Vector z )
        throws LearnerException;
    
    /** Parameterize and cost data all in one hit.   */
    public double sParameterizeAndCost( Value.Model m, Value s )
        throws LearnerException;
    
    /** Return the name of the modelLearner */
    public String getName();
    
    /** Return a structure containing Value.Function instances of all ModelLearner functions. */    
    public FunctionStruct getFunctionStruct();
    
    /** 
     * This convenience class implements ModelLearner while filling in a few common values. 
     */
    public abstract class DefaultImplementation implements ModelLearner
    {
        /** 
         * Create new parameterizers and costers based on modelType.  If modelType is not fully 
         * known,  Type.MODEL or other "watered down" versions of Type.Model may be used. These 
         * are primarily for type checking and a Value.Model with more fully specified types 
         * should be returned. <br>
         *
         * iType is where extra information is passed.  The form of iType is still not finalised 
         * in CDMS. <br>
         */
        public DefaultImplementation( Type.Model modelType, Type iType )
        {
            this.iType = iType;
            this.modelType = modelType;
        }
        
        /** return a FunctioNStruct based on the current ModelLearner */
        public FunctionStruct getFunctionStruct() {
            return new FunctionStruct( this );
        }
        
        /**********************************************************************************
         *                                                                                *
         *            DEFAULT IMPLEMENTAIONS OF ABSTRACT FUNCTIONS                        *
         *                                                                                *
         **********************************************************************************/
        
        /** Model Type */
        protected Type.Model modelType;
        /** Initial information Type */
        protected Type iType;
        
        /** Return the type of model being learned. */
        public Type.Model getModelType()
        {
            return modelType;
        }
        
        /** Return the type of model initial information. */
        public Type getIType()
        {
            return iType;
        }
        
        /** 
         * Function should be overloaded to accept i,x,z,y and return a double  <br>
         * NOTE: m is required as params are defined in terms of a model
         */
        public double cost( Value.Model m, Value i, Value.Vector x, Value.Vector z, Value params )
            throws LearnerException
        {
            return sCost( m, m.getSufficient(x,z), params );
        }
        
        /** Function splits msy into it's components and calls sCost(s,y) */
        public double msyCost( Value.Structured msy )
            throws LearnerException
        { 
            return sCost( (Value.Model)msy.cmpnt(0), msy.cmpnt(1), msy.cmpnt(2));
        } 
        
        /** Parameterize and cost data all in one hit.   */
        public double parameterizeAndCost( Value i, Value.Vector x, Value.Vector z )
            throws LearnerException
        {
            return msyCost( parameterize(i,x,z) );
        }
        
        /** Parameterize and cost data all in one hit.   */
        public double sParameterizeAndCost( Value.Model m, Value s ) 
            throws LearnerException
        {
            return msyCost( sParameterize(m,s) );
        }
        
        /** toString() returns "ModelLearner" by default. */
        public String toString() { return getName(); }        
    }
    
    
    /** This exception should be thrown whenever Parameterizing of Costing fails. */
    public static class LearnerException extends Exception {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2120191820491551274L;
        public LearnerException( String s ) { super(s); } 
        public LearnerException( Exception e ) { super(e); } 
        public LearnerException( String s, Exception e ) { super(s,e); }
    }
    
    
    /**     
     * A Value.Model implementing GetNumParams can return the number of continuous parameters
     * required to state a model.  This may be useful for implementing non MML metrics, (and is
     * useful when you hava a numParams column in a paper...)
     */
    public interface GetNumParams {
        public int getNumParams( Value params );
    }
    
}

