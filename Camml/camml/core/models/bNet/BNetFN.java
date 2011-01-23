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
// Module for BNet functions.
//

// File: BNetFN.java
// Author: rodo@dgs.monash.edu.au
// Created on 28/02/2005

package camml.core.models.bNet;

import java.io.FileWriter;
import java.io.IOException;

import cdms.core.Module;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.Value.Function;

/**
 * @author rodo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BNetFN extends Module {
    
    /**
     * @author rodo
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    public static class ExportNetica extends Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3560562088301857177L;
        final static protected Type.Function tt = new Type.Function( 
                                                                    new Type.Structured( new Type[]{ Type.STRING, Type.MODEL, Type.VECTOR}), 
                                                                    Type.STRING );
        
        /**    Constructor */
        public ExportNetica() { super(tt);    }

        /* (non-Javadoc)
         * @see cdms.core.Value.Function#apply(cdms.core.Value)
         */
        public Value apply(Value v) { 
            Value.Structured struct = (Value.Structured)v;
            Value.Str name = (Value.Str)struct.cmpnt(0);
            BNet bNet = (BNet)struct.cmpnt(1);
            Value.Vector params = (Value.Vector)struct.cmpnt(2);
            return new Value.Str( bNet.exportNetica( name.getString(), params ) );    
        }
    }
    
    /** Function saves a string to a given file. */
    public static class ToFile extends Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 5229444636748632579L;
        final static protected Type.Function tt = new Type.Function( 
                                                                    new Type.Structured( new Type[]{ Type.STRING, Type.STRING }), 
                                                                    Type.STRING );
        
        /**    Constructor */
        public ToFile() { super(tt);    }

        /** (name,string) -> string */
        public Value apply(Value v) { 
            Value.Structured struct = (Value.Structured)v;
            Value.Str name = (Value.Str)struct.cmpnt(0);
            Value.Str string = (Value.Str)struct.cmpnt(1);
            try {
                FileWriter fw = new FileWriter( name.getString() );
                fw.write( string.getString() );
                fw.flush();
            } catch ( IOException e ) {    throw new RuntimeException(e); }
            return string;
        }
    }

    public static java.net.URL helpURL = Module.createStandardURL( BNetFN.class );    
    public String getModuleName() { return "BNetFN"; }
    public java.net.URL getHelp() { return helpURL; }
    
    /* (non-Javadoc)
     * @see cdms.core.Module#install(cdms.core.Value)
     */
    public void install(Value params) throws Exception {
        add("neticaString", exportNetica, "turn into a netica String" );
        add("toFile", toFile, "Write string to a file." );
    }
    
    public final ExportNetica exportNetica = new ExportNetica();
    public final ToFile toFile = new ToFile();
}
