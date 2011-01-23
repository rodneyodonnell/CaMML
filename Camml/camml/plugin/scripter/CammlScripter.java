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
// Last Modifies : 30-5-02
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: CammlScripter.java
// This is a command line based method for executiong Camml Scripts

// package cdms.plugin.cammlPlugin;
package camml.plugin.scripter;

import java.io.*;
import cdms.core.*;

/** CammlScripter contains the basic user environment for the Camml scriptiong language.*/
public class CammlScripter {

    /** quitValue used for flow control. */
    public static Value.Triv quitValue = new Value.Triv( Type.TRIV );

    /** Storage space to save and restore all variables used in language.*/
    public static CammlEnvironment cammlEnv = new CammlEnvironment( );

    /** Main function for Scripting Language.
        args[0] should be a library containing functions implemented.*/
    public static void main( String args[] ) {

        // Check correct number of arguments are passed.
        if (args.length != 1) {
            System.out.println("useage : java <prog> LambdaScript.fp");
            return;
        }    

        // Load in library of functions.
        // This defines functions and variables.  Variables are set to their default values.     
        CammlPlugin.runLambdaScript.apply( new Value.Str( args[0] ) );

        // number of command lines entered.
        int line = 0;
       
        // Make a local copy of environment variables relevant to the user interface.
        // Verbosity is used to determine the amount of data printed to the screen.
        // logFile is a log of activity written to a file.
        int verbosity = ((Value.Discrete)cammlEnv.getObject("var_verbosity")).getDiscrete();;    

        // Redirect stdin, stdout and stderr to a logfile.
        try {
            SaveOutput.start("camml.log");
        } 
        catch (IOException e) {
            // Print exception.  Detail level depends on verbosity.
            printException( e, verbosity, "Could not open logfile.  Proceeding anyway.\n" );
        } 
    

        // Open a StreamReader with stdin (to read from command line)
        // note: This must be done after stdin has been redirected.
        BufferedReader inStream = new BufferedReader( new InputStreamReader( System.in ) );
    

        // Keep looping and looking for new commands;  Exit when quitValue is returned.
        while (true) {
            
            Value v;         // temporary value.
            String cmd;      // command string read in.

            // Print command prompt.
            if (verbosity > 2) System.out.print("\nCamml > ");
            line++;
        
            // Read a command from stdin
            try {    
                cmd = inStream.readLine();
            }
            catch (IOException e) {
                printException( e, verbosity, "Error reading command.\n" );
                continue;
            }        

        
            // Ignore blank lines.
            if (cmd.length() == 0) continue;
        
            try {
                // Run this command as a single line lambda script.
                v = CammlPlugin.runCammlCommand.apply( new Value.Str(cmd) );
            }
            catch (CammlRuntimeException e) {
                printException( e, verbosity, "Error running command : " + cmd + "\n");
                continue;
            }
            catch (RuntimeException e) {
                printException( e, verbosity, "Error running command : " + cmd + "\n");
                continue;
            }
        

            v.toString();  // force evaluation of v (otherwise program is lazily evaluated)
            if ( v == quitValue )
                break;

            System.out.println( v );
        
            // Houskeeping.  Check if verbosity has changed.
            verbosity = ((Value.Discrete)cammlEnv.getObject("var_verbosity")).getDiscrete();;
        }    
    

        try {
            SaveOutput.stop();
        } 
        catch (IOException e) {
            printException( e, verbosity, "Could not stop logfile.\n" );
        } 

    }    

    /** Print information about an exception being thrown.  Detail level varies with verbosity.
     *  Low Verbosity < 4 gives general errors in a simple format.  Verbosity 5 gives basic 
     *  "java.IOException" type level warnings, and Verbosity > 5 gives full stack traces.
     */
    public static void printException( Exception e, int verbosity, String s ) {
        if (verbosity > 1) System.err.println(s);
        if (verbosity > 4) System.err.println(e); 
        if (verbosity > 5) e.printStackTrace();
    }

}
