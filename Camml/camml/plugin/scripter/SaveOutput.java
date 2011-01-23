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
// Last Modifies : 30-6-02
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: This class redirects stdout and stderr to a logfile (still prints to screen)
//  based on : http://developer.java.sun.com/developer/TechTips/txtarchive/1999/Oct99_PatrickC.txt
// Original version did not log stdin and several other changes made.

// package cdms.plugin.cammlPlugin;
package camml.plugin.scripter;
        
import java.io.*;

class SaveOutput extends PrintStream {
    
    /** Logfile to be writen to. */
    static OutputStream logfile;

    // Save original values of System streams
    static final PrintStream oldStdout = System.out;  /** Original value of System.out */
    static final PrintStream oldStderr = System.err;  /** Original value of System.err */
    static final InputStream oldStdin  = System.in;   /** Original value of System.in  */
    
    /** Flag stating if logfile is currently open or closed. */
    private static boolean isOpen = false;  

    SaveOutput(PrintStream ps) {
        super(ps);
    }

    /** Starts copying stdout, stderr and stdin to the file f. */
    public static void start(String f) throws IOException {

        // If logfile currently open, close it and create a new logfile.
        if (isOpen == true)
            stop();

        // Create/Open logfile.
        logfile = new PrintStream( new FileOutputStream(f) );

        // Start redirecting the output.
        System.setOut( new SaveOutput(System.out) );
        System.setErr( new SaveOutput(System.err) );
        System.setIn( new SaveInput() );
    
        // Flag that the logfile is open.
        isOpen = true;
    }

    /** Close logfile and restores the original settings of std streams */
    public static void stop() throws IOException{
        if (isOpen == true) {
            System.setOut(oldStdout);  // restore old streams
            System.setErr(oldStderr);
            System.setIn (oldStdin);

            logfile.close();           // Close logfile.
        }
        isOpen = false;
    }

    /** PrintStream override. Forks input to logfile and super()*/
    public void write(int b) {
        try {
            logfile.write(b);
        
        } catch (IOException e) {            
            setError();
            throw new CammlRuntimeException("Error writing to logfile\n" + e.toString());
        } 
      
        super.write(b);
    }

    /** PrintStream override. Forks input to logfile and super()*/
    public void write(byte buf[], int off, int len) {
        try {
            logfile.write(buf, off, len);
        } catch (IOException e) {            
            setError();
            throw new CammlRuntimeException("Error writing to logfile\n" + e.toString());
        }
        super.write(buf, off, len);
    }


    /** This class reads from stdin and outputs to a logfile. */
    static class SaveInput extends InputStream {
    
        /** Remember the old value of stdin */
        private static InputStream oldStdin;
        SaveInput() { oldStdin = System.in; }

        /** Read stdin normally, then write it to logfile.*/
        public int read(byte[] b, int off, int len) throws IOException {
        
            int x = oldStdin.read(b,off,len);
            logfile.write(b, off, x);

            return x;
        }

        /** Read stdin normally, then write it to logfile.*/
        public int read(byte[] b) throws IOException {
        
            int x = oldStdin.read(b);
            logfile.write(b);

            return x;
        }

        /** Read stdin normally, then write it to logfile.*/
        public int read() throws IOException {
            return oldStdin.read();
        }


    }
    
}
