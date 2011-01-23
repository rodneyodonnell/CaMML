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
// Camml plugin for CDMS
//
// Author        : Rodney O'Donnell
// Last Modifies : 21-5-02
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: CammlScripterFN.java
// Contains variaous functions used in the Camml Scripting function.

// package cdms.plugin.cammlPlugin;
//package camml.cdmsPlugin.scripter;
package camml.plugin.scripter;

import java.io.*;
import cdms.core.*;

/** My CammlPlugin module */
public class CammlScripterFN
{
    /** nullValue used to signal failure from cammlGet. */
    public static Value.Triv nullValue = new Value.Triv( Type.TRIV );

    /** <code> (t -> STRING) -> t </code> Adds v to CammlEnv and returns it.*/
    public static Value.Function cammlAdd = new CammlAdd( false );

    /** <code> (t -> STRING) -> t </code> Set v in CammlEnv (if it already exists) and returns it.*/
    public static Value.Function cammlSet = new CammlAdd( true );

    /** <code> (t -> STRING) -> t </code> Adds t to CammlEnv and returns it. */
    public static class CammlAdd extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -4855161272559461010L;
        /** Type is polymorphic, so Type = Type.Variable */
        public static final Type.Variable T = new Type.Variable();
        public static final Type.Function TT = new Type.Function(T,
                                                                 new Type.Function( Type.STRING, new Type.Function( T, T) ));

        /** Value of setOnly distinguises between set and add.  Add may create new variables while
            set may only modify variables already in existance. */
        public CammlAdd( boolean setOnly ) { super(TT);   this.setOnly = setOnly; }
        public String toString() { 
            if (setOnly) return "CammlPlugin function: cammlAdd, (STRING -> t) -> t";
            else return "CammlPlugin function: cammlSet, (STRING -> t) -> t" ;
        }

        /** setOnly flag detemines if variables may be created, or just set to new values. */
        boolean setOnly;  

        /** Apply : Requires multiple parameters so function is returned. */
        public Value apply(Value v) {
            return new CammlAdd2( ((Value.Str)v).getString(), setOnly );      
        }
    }

    /** <code> t -> t <code> */
    public static class CammlAdd2 extends Value.Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 6032058671071712261L;
        // Type is polymorphic, so Type = Type.Variable
        public static final Type.Variable T = new Type.Variable(); //cammlAdd.T;
        public static final Type.Function TT = new Type.Function( T, T);
        String name;

        boolean setOnly;
        public CammlAdd2( String name, boolean setOnly ) { 
            super(TT); 
            this.name = name;
            this.setOnly = setOnly;
        }

        public String toString() { 
            if (setOnly) return "CammlPlugin function: cammlAdd2, (t -> t)";
            else return "CammlPlugin function: cammlSet2, (t -> t)" ;
        }

        /** Apply */
        public Value apply(Value v) {
            if (setOnly) {
                if ( CammlScripter.cammlEnv.getObject( name ) != null )
                    CammlScripter.cammlEnv.add( name, v );
                else
                    System.out.println("Could not set " + name + " to " + v + 
                                       "variable " + name + "not declared." );
            }
            else
                CammlScripter.cammlEnv.add( name, v );
            return v; 
        }
    
    }

    
    /** <code> (t -> STRING) -> t </code> Gets v from CammlEnv */
    public static Value.Function cammlGet = new CammlGet( );

    /** <code> (t -> STRING) -> t </code> Gets v from CammlEnv */
    public static class CammlGet extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7925795699958178191L;
        /** Type is polymorphic, so Type = Type.Variable */
        public static final Type.Variable T = new Type.Variable();
        public static final Type.Function TT = new Type.Function(Type.STRING, T);

        public CammlGet( ) { super(TT); }
        public String toString() { return "CammlPlugin function: cammlGet, (STRING -> t) -> t"; }

        /** Apply : if v is a Value.str, return the value with that name from cammlEnv
            if not, return nullValue.  If value is not found in cammlEnv, return nullValue.
        */ 
        public Value apply(Value v) {
            Value v2 = nullValue;
            if (v instanceof Value.Str)        {
                v2 =  (Value)CammlScripter.cammlEnv.getObject( ((Value.Str)v).getString());
                if (v2 == null) 
                    v2 = nullValue; 
            }
            return v2;
        }
    
    }

    /** <code>t -> t</code> Prints Value V to stdout and returns V (polymorphic). */
    public static Value.Function printString = new PrintString();

    /** Function prints a value to stdout and returns the value it was passed     */
    public static class PrintString extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 6695784374821864523L;
        /** Type is polymorphic, so Type = Type.Variable */
        public static final Type T = Type.STRING;
        public static final Type.Function TT = new Type.Function(T,T);

        public PrintString() { super(TT); }
        public String toString() { return "CammlPlugin function: printString, t -> t"; }

        /** Print to stdout and return v */
        public Value apply(Value v) {
            Value.Str vStr = (Value.Str)v;
            System.out.println( vStr.getString() );
            return v;
        }
    }

    /** Value.Function to turn on logging of stdin, stdout and stderr streams. <br>
     *  The stream to be opened is passed to apply() */
    public static Value.Function startLog = new StartLog();

    /** Value.Function to turn on logging of stdin, stdout and stderr streams. <br>
     *  The stream to be opened is passed to apply() */
    public static class StartLog extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -1778074164539313086L;
        public static final Type T = Type.STRING;
        public static final Type.Function TT = new Type.Function(T,T);

        public StartLog() { super(TT); }
        public String toString() { return "CammlPlugin function: startLog, String -> String"; }

        /** Redirect sdtin, stdout and stderr.  Filename passed as parameter*/
        public Value apply(Value v) {
            String fileName = ((Value.Str)v).getString();
        
            // Redirect stdin, stdout and stderr to a logfile.
            try {
                SaveOutput.start(fileName);
            }
            catch (IOException e) {
                throw new CammlRuntimeException("IOException starting logfile\n" + e );
            }
        
            return v;
        }
    }


    /** Value.Function to stop a Logfile from logging stdin, stdout and stderr streams */
    public static Value.Function stopLog = new StopLog();

    /** Value.Function to stop a Logfile from logging stdin, stdout and stderr streams */
    public static class StopLog extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -5859702064710093005L;
        public static final Type T = new Type.Variable();
        public static final Type.Function TT = new Type.Function(T,T);

        public StopLog() { super(TT); }
        public String toString() { return "CammlPlugin function: stopLog, t -> t"; }

        /** Stop logging to current logfile.  Value V is ignored.*/
        public Value apply(Value v) {
            try {
                SaveOutput.stop();
                return v;
            }
            catch (IOException e) {
                throw new CammlRuntimeException("IOException stopping logfile\n" + e );
            }
        }
    }
   
}













