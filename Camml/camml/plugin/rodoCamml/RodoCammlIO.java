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
// Functions for dealing with rodo-format camml files.
//

// File: RodoCammlIO.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.rodoCamml;

import cdms.core.*;
import cdms.core.VectorFN.FastDiscreteVector;

import java.io.*;

// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
public class RodoCammlIO {


    /**
     * TODO: Multi line description of RodoCammlIO.java
     *
     * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
     * @version $Revision: 1.11 $ $Date: 2007/03/27 02:59:32 $
     * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/rodoCamml/RodoCammlIO.java,v $
     */

    public static class FastStatusDiscreteVector extends FastDiscreteVector {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -8248239003042791564L;
        
        final ValueStatus[] statusArray;
        final Type.Discrete eltType;
        
        public FastStatusDiscreteVector( int[] x, ValueStatus[] status, Type.Discrete tt) {
            super(x,tt);
            this.eltType = tt;
            this.statusArray = status;
        }
        
        
        public Value elt(int i) {
            return new Value.Discrete(eltType, statusArray[i], super.intAt(i));
        }
    }

    /** Static instance of Load function. */
    public static Value.Function load = new Load();

    /** Static instance of store function. */
    public static Value.Function store = new Store(false);
    
    /** Static instance of store using old camml format. */
    public static Value.Function storeInOldFormat = new Store(true);
    
    /** Constructor for RodoCammlLoader. */
    public RodoCammlIO() {
        super();
    }

    
    /**
     *  Load is a Value.Function of type STR -> [(...)] <br>
     *  It takes as a parameter a filename string, and returns a vector of structured values. <br>
     *  It loads files from the oldCamml format.
     */
    protected static class Load extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 5451462463176740961L;
        public static final Type.Function tt = 
            new Type.Function( Type.STRING, new Type.Vector(Type.STRUCTURED) );
        public Load()
        {
            // Type = STR -> [()]
            super ( tt );
        }

        public Value apply( Value v )
        {
            Value.Vector vec;
            try {
                vec = load( ((Value.Str)v).getString() );
            }
            catch ( IOException e ) {
                e.printStackTrace();
                vec =  new VectorFN.EmptyVector();
            }
        
            return vec;
        }
    }



    /** return the vector specified by filename */
    public static Value.Vector load( String filename ) throws java.io.IOException
    {
        // open up a file.
        StreamTokenizer tokenizer = new StreamTokenizer( new BufferedReader(new FileReader(filename)));
        tokenizer.wordChars('_','_');  // add underscore to list of word characters.c
        // Tell the tokenizer to view the '%' char as the start of a comment.
        tokenizer.commentChar('%');

        // Tell the tokenizer that end of line characters are NOT significant.
        tokenizer.eolIsSignificant( false );


        // Read in numVariables.
        int numVariables;
        if ( tokenizer.nextToken() != StreamTokenizer.TT_NUMBER ) {
            throw new RuntimeException("Parse error on line" + tokenizer.lineno() 
                                       + " : int expected" );
        }
        numVariables = (int)tokenizer.nval;


        // Read in numSamples
        int numSamples;
        if ( tokenizer.nextToken() != StreamTokenizer.TT_NUMBER ) {
            throw new RuntimeException("Parse error on line" + tokenizer.lineno()
                                       + " : int expected" );
        }
        numSamples = (int)tokenizer.nval;
    
        /////////////////////////////////////////////////////////////////
        // Read in names for each variable.                            //
        // If a non-word is the next token read, then variable names   //
        // are not present in the file so we must create them instead. //
        /////////////////////////////////////////////////////////////////
        String[] variableName = new String[ numVariables ];    
        // Peek at the next token's type
        int nextTokenType = tokenizer.nextToken();
        tokenizer.pushBack();
        // if token is a word, read in variable names
        if ( nextTokenType == StreamTokenizer.TT_WORD) {
            for (int i = 0; i < variableName.length; i++) {
                if ( tokenizer.nextToken() != StreamTokenizer.TT_WORD ) {
                    throw new RuntimeException("Parse error on line" + tokenizer.lineno()
                                               + " : String expected");
                }
                variableName[i] = tokenizer.sval;
            }
        } else { // else create or own token names.
            for (int i = 0; i < variableName.length; i++) {
                variableName[i] = "v"+i;
            }
        }
        

        // Read in the arity for each variable
        int[] arity = new int[ numVariables ];
        for (int i = 0; i < arity.length; i++) {
            if ( tokenizer.nextToken() != StreamTokenizer.TT_NUMBER ) {
                throw new RuntimeException("Parse error on line" + tokenizer.lineno()
                                           + " : int expected");
            }
            arity[i] = (int)tokenizer.nval;
        }


        // Read in the data.  [j][i] seems reversed.  This allows us to make a multiCol vector.
        int[][] data = new int[ numVariables ][ numSamples ];
        ValueStatus[][] status = new ValueStatus[numVariables][];
        for ( int i = 0; i < numSamples; i++ ) {
            for ( int j = 0; j < numVariables; j++ ) {
                tokenizer.nextToken();
                boolean intervene = false;
                if (tokenizer.ttype == '*') {
                    intervene = true;
                    tokenizer.nextToken();
                }
                else if (tokenizer.ttype != StreamTokenizer.TT_NUMBER ) {
                    throw new RuntimeException("Parse error on line" + tokenizer.lineno()
                                               + " : int expected");
                }
        
                data[j][i] = (int)tokenizer.nval;
                if ( data[j][i] >= arity[j] || data[j][i] < 0 ) {
                    throw new RuntimeException( "data["+i+"]["+j+"] = "+data[j][i]
                                                + " out of range (0,"+arity[j]+").  On line "
                                                + tokenizer.lineno() );
                }

                // Mark intervention data 
                if (intervene) {
                    if (status[j] == null) {
                        status[j] = new ValueStatus[numSamples];
                        for (int k = 0; k < status[j].length; k++) { status[j][k] = Value.S_PROPER; }
                    }
                    status[j][i] = Value.S_INTERVENTION;
                }
            }
        }
    
        // Now we convert the data into a CDMS readable format.
        Value.Vector[] vectorArray = new Value.Vector[ numVariables ];
        Type.Vector vecTypeArray[] = new Type.Vector[ numVariables ];
    
    
        for (int i = 0; i < numVariables; i++ ) {
            //vectorArray[i] = new VectorFN.FastDiscreteVector( typeArray[i], data[i] );        
            Type.Discrete discType = new Type.Discrete(0,arity[i]-1,false,false,false,false);
            vecTypeArray[i] = new Type.Vector( discType );
            // if no intervention values, use fast data structure
            if ( status[i] == null ) {
                vectorArray[i] = new VectorFN.FastDiscreteVector( data[i], discType );
            }
            else {
                // Fast discrete vector which also stores status info.
                vectorArray[i] = new FastStatusDiscreteVector(data[i], status[i], discType);
            }
        }

        Type.Structured structType = new Type.Structured( vecTypeArray, variableName );
        Value.Structured tempStruct = new Value.DefStructured( structType, vectorArray );
    
        VectorFN.MultiCol multiVec = new VectorFN.MultiCol( tempStruct );

        // return the finished vector.
        return multiVec;
    
    }
    




    /**
     *  Store if a Value.Function of type String -> [(...)] -> String <br>
     *  It takes a string and a vector of structured and saves them in the oldCamml format. <br>
     *  The string returned will be "Data Saved" (if everything works). <br>
     *  Warning : This will overwrite any old file with the same name.
     */
    protected static class Store extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3379268484225409845L;

        public static final Type.Function tt = new Type.Function( Type.STRING, Store2.tt );

        protected boolean oldFormat;
    
        public Store(boolean oldFormat) { super(tt); this.oldFormat = oldFormat; }
        
        public Value apply( Value v )
        {
            return new Store2( ((Value.Str)v).getString(), oldFormat );
        }
    }
    
    /**
     * The second half of the store function.  Type [(...)] -> String
     * @see Store
     */
    protected static class Store2 extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2689074262026588318L;

        public static final Type.Function tt 
            = new Type.Function( new Type.Vector(Type.STRUCTURED), Type.TRIV );

        protected String filename;
    
        /** Should old camml format be used? */
        protected boolean oldFormat;
    
        public Store2( String filename, boolean oldFormat )
        {
            super(tt);
            this.filename = filename;
            this.oldFormat = oldFormat;
        }

        public Value apply( Value v )
        {
            String messageString = "Data Saved";            
            Value.Vector data = (Value.Vector)v;

            try {  // Test for an IOException
                store( filename, data, oldFormat );
            }
            // If there is an IOException, return it as an error to the user.
            catch ( java.io.IOException e) {  
                messageString = e.toString();
            }
            return new cdms.core.Value.Str( messageString );
        }
    }

    /** Store the vector data in the file specified by filename. 
     *  if oldFormat == true, the output file will use the old camml format
     *  (which does not contain variable names or comments.) */
    public static void store( String filename, Value.Vector data, boolean oldFormat ) 
        throws java.io.IOException
    {
        Type.Vector vectorType = (Type.Vector)data.t;
        Type.Structured structType = (Type.Structured)vectorType.elt;

        int numSamples = data.length();
        int numVariables = structType.cmpnts.length;


        // open up a file.
        PrintWriter outFile = new PrintWriter( new BufferedWriter(new FileWriter( filename )));
    
        if (!oldFormat) {
            outFile.println("% File automatically generated by CDMS");
            outFile.println("% Camml-Discrete-rodo Format");
            outFile.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        
            outFile.println( numVariables + "\t % Number of variables");
            outFile.println( numSamples + "\t % Number of cases");
    
            outFile.println("% Variable names");
            String[] label = structType.labels;
            if ( label == null ) {
                label = new String[numVariables];
            }
            for ( int i = 0; i < label.length; i++ ) {
                if ( label[i] == null ) {
                    label[i] = "Var"+i;
                }
            }
            label = camml.plugin.netica.NeticaFn.makeValidNeticaNames( label, false );
            for ( int i = 0; i < label.length; i++ ) {
                outFile.print( label[i] + '\t' );
            }
            outFile.println();

        } else {
            // Old file format does not allow comments or variable names.
            outFile.print(numVariables + " " + numSamples + "\n");
        }

        // Print arity of variables.
        if (!oldFormat) { outFile.println("% Arity of variables"); }
        for (int i = 0; i < numVariables; i++) {
            Type.Discrete currentType = (Type.Discrete)structType.cmpnts[i];
            int arity = (int)currentType.UPB - (int)currentType.LWB + 1;
            outFile.print( "" + arity + '\t' );
        }
        outFile.println();
    
        // Print the data.
        if (!oldFormat) {
            outFile.println ("% ------------------------ Data --------------------");
        }
    
        for (int i = 0; i < numSamples; i++) {
            Value.Structured currentElt = (Value.Structured)data.elt(i);
            for (int j = 0; j < numVariables; j++) {
                // Print a "*" if an intervention is present.
                String intStr = "";
                ValueStatus status = currentElt.cmpnt(j).status();
                if ( status == Value.S_INTERVENTION) { intStr = "*";}
                else if ( status != Value.S_PROPER) { throw new RuntimeException("Status not supported:" + status); } 
                outFile.print( "" + intStr + currentElt.intCmpnt(j) + '\t');
            }
            outFile.println();
        }
        outFile.println();
    
        outFile.close();
    }
}


