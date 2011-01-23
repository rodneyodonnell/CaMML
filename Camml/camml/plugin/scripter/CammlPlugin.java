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
// Last Modifies : 26-5-02
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: CammlPlugin.java
// Contains the main Module class for the scripting language.


// package cdms.plugin.cammlPlugin;
//package camml.cdmsPlugin.scripter;
package camml.plugin.scripter;

import java.io.*;

import camml.plugin.rodoCamml.RodoCammlIO;
import cdms.core.*;
import cdms.plugin.fpli.*;

/** The Camml scriptiong function Module.  */
public class CammlPlugin extends Module {

    /** Help URL required part of Module */
    public static java.net.URL helpURL = Module.createStandardURL(CammlPlugin.class);
    public java.net.URL getHelp() { return helpURL; }

    /** Name of Module = CammlPlugin */
    public String getModuleName() { return "CammlPlugin"; } 

    /** Used to install various model components into CDMS.  Modules must also be
        added to CDMS Bootstrap file.                                             */
    public void install(Value params) throws Exception {
    
        add("printString", CammlScripterFN.printString, "Prints a string to stdout.");
    
        add("quitValue", CammlScripter.quitValue, "Flag used to Quit Camml scripter" );
        add("nullValue", CammlScripterFN.nullValue, "Flag used to signal failure in cammlGet" );

        add("cammlAdd", CammlScripterFN.cammlAdd, "Add a value to the environment." );
        add("cammlSet", CammlScripterFN.cammlSet, "Set a value in the environment." );
        add("cammlGet", CammlScripterFN.cammlGet, "Get a value from the environment." );
        //add("loadCammlFile", CammlLoader.loadCammlFile, "Load a camml file");
        add("loadCammlFile", RodoCammlIO.load, "Load a camml file");
    
        add("runCammlScript", CammlPlugin.runCammlScript, "Run a Camml Script");
        add("runLambdaScript", CammlPlugin.runLambdaScript, "Run a Lambda Script");
        add("runCammlCommand", CammlPlugin.runCammlCommand, "Run a single Camml command");

        add("startLog", CammlScripterFN.startLog, "Turn logfile on");
        add("stopLog", CammlScripterFN.stopLog, "Turn logfile off");
    
    }


    /** <code>String -> t</code> Runs a Lambda script from a file, and returns result. */
    public static Value.Function runLambdaScript = new RunLambdaScript();

    /** Open a Lambda script file and run it. */
    public static class RunLambdaScript extends Value.Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 8808593066669068672L;

        /** Type is polymorphic, so Type = Type.Variable */
        public static final Type.Variable T = new Type.Variable();

        /** Takes a string, and returns a Value of any Type. */
        public static final Type.Function TT = new Type.Function(Type.STRING,T);

        public RunLambdaScript() { super(TT); }
        public String toString() { return "CammlPlugin function: RunLambdaScript, String -> t"; }

       
        /** Run script and return result */
        public Value apply(Value v) {
            // System.out.println("Running Lambda Script : " + v);
            try    {
                String fileName = ((Value.Str)v).getString();                 // get filename from v
                FileInputStream inStream = new FileInputStream( fileName );   // open stream
                Syntax syn = new Syntax( new Lexical( inStream ) );
                Expression e = syn.exp();                          // parse an Expression
                Value v2 = e.eval( CammlScripter.cammlEnv );       // evaluate Expression
                //        System.out.println( v );
                return v2;
            }
            catch ( IOException e ) {
                throw new CammlRuntimeException("IOException reading from " + v);
            }
        }
    }

    


    /** <code>String -> t</code> Runs Camml script from a file, and returns result. */
    public static Value.Function runCammlScript = new RunCammlScript();

    /** Open a Camml script file and run it. */
    public static class RunCammlScript extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 8251736494388169232L;

        /** Type is polymorphic, so Type = Type.Variable */
        public static final Type.Variable T = new Type.Variable();

        /** Takes a string, and returns a Value of any Type. */
        public static final Type.Function TT = new Type.Function(Type.STRING,T);

        public RunCammlScript() { super(TT); }
        public String toString() { return "CammlPlugin function: RunCammlScript, String -> t"; }

       
        /** Print to stdout and return v */
        public Value apply(Value v) {
            //   System.out.println("Running CaMML Script " + v);
            try    {        
                // Open input file.  Buffered Stream needed for readLine()
                String fileName = ((Value.Str)v).getString();
                InputStream inStream = new FileInputStream( fileName );
                BufferedReader input = new BufferedReader(new InputStreamReader( inStream ));
        
                int i = 0;
                while (true){
                    i++;
                
                    if ( !input.ready() ) break;   // Break at end of file.
                
                    String str = input.readLine();
                    if (str.length() == 0) continue;  // Ignore blank lines.
            
                    Syntax syn = new Syntax(new Lexical(new ByteArrayInputStream(str.getBytes())) );
            
                    Expression e = syn.exp();                          // parse an Expression
                    e.eval( CammlScripter.cammlEnv );    
                }
                return v;
            }
            catch ( IOException e ) {
                throw new CammlRuntimeException("IOException reading from " + v + e);
            }
        }
    }


    /** <code>String -> t</code> Runs single line command, and returns result. */
    public static Value.Function runCammlCommand = new RunCammlCommand();

    /** Open a Camml script file and run it. */
    public static class RunCammlCommand extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 3625278069595507574L;

        /** Type is polymorphic, so Type = Type.Variable */
        public static final Type.Variable T = new Type.Variable();
    
        /** Takes a string, and returns a Value of any Type. */
        public static final Type.Function TT = new Type.Function(Type.STRING,T);
    
        public RunCammlCommand() { super(TT); }
        public String toString() { return "CammlPlugin function: RunCammlCommand, String -> t"; }

       
        /** Run Camml command passed in string v */
        public Value apply(Value v) {        
            String command = ((Value.Str)v).getString();
            return _apply(command);
        }
    
        /** Run Camml command passed in string v */
        public Value _apply( String command ) {
            cdms.plugin.fpli.Environment env = CammlScripter.cammlEnv;

            // StringBufferInputStream deprecated so ByteArrayInputStream used.
            //Syntax syn = new Syntax(new Lexical(new StringBufferInputStream(command.getString())));
        
            Syntax syn = new Syntax( new Lexical( new ByteArrayInputStream(command.getBytes()) ) );
            Expression e = syn.exp();                          // parse an Expression
            Value v2 = e.eval( env );                          // Evaluate expression.
                     
            return v2;
        }
    }
}




