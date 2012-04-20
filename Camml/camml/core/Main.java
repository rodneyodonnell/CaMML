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
// Main entry point for CaMML.
//

// File: Main.java
// Author: rodo@dgs.monash.edu.au

package camml.core;

import cdms.core.Value;
import cdms.plugin.fpli.Fpli;

import java.io.FileInputStream;
import java.io.InputStream;

/** */
public class Main {
    public static void main(String args[]) throws Exception {
        try {
            // Load interpreted
            Value.Function interpreter = new Fpli.Interpreter();

            String script = "/script/cammlBootstrap.fp";
            if (args.length > 0) {
                script = args[0];
            }

            // Attempt to open script in jarfile.
            InputStream is = Main.class.getResourceAsStream(script);

            // If not in jarfile, look in the regular file system.
            if (is == null) {
                is = new FileInputStream(script);
            }

            // Read stream into sb.
            StringBuffer sb = new StringBuffer();
            int chr;


            while ((chr = is.read()) != -1) {
                sb.append((char) chr);
            }
            is.close();

            // Interpret the script.
            Value res = interpreter.apply(new Value.Str(sb.toString()));
            System.out.println("\n" + res.toString() + "\n");

        } catch (Exception e) {
            if (true) throw new RuntimeException(e);
            System.out.println("An error occured during startup.\n");
            e.printStackTrace();
            System.exit(1);
        }

    }

}

