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
// Module containing various Library functions.
//

// File: Library.java
// Author: rodo@csse.monash.edu.au

package camml.core.library;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import camml.core.library.StructureFN.FastDiscreteStructure;
import camml.core.models.ModelLearner;
import cdms.core.*;

/**
 * Library contains a group of useful functions.  Some of these could be incorporated into 
 * CDMS.core at some stage. <br>
 *
 * Current members are :    <p>
 * 
 * FUNCTIONS:               <br>      
 * Sum                      <br>
 * MakeWeightedVector       <br>
 *                          <p>
 * VECTOR IMPLEMENTATIONS   <br>
 * WeightedVector2          <br>
 */


public class Library extends Module
{
    /** Static instance of class */
    //   public static Library library = new Library();

    public static java.net.URL helpURL = Module.createStandardURL(Library.class);
    public String getModuleName() { return "Library"; }    
    public java.net.URL getHelp() { return helpURL; }


    public void install (Value v)
    {
        add( "sum", Sum.sum, "Sum a vector.  [Discrete]->Discrete or [Continuous]->Continuous or " +
             "[(a,b,c)] -> (a,b,c)");
        add( "print", Print.print, "Print v, return ()" );
        add( "println", Print.println, "Print v, return ()" );

        add( "vec2struct", StructureFN.vectorToStruct, "Convert a vector to a structure.");
        add( "struct2vec", StructureFN.structToVector, "Convert a structure to a vector.");

        add( "enumerateDAGs", EnumerateDAGs.enumerateDAGs, "Enumerate all DAG structures.");

        add( "emptyStruct", new Value.DefStructured(new Value[0]), "Struct with no elements.");
        add( "serialise", serialise, "Serialise value to a file");
        add( "unserialise", unserialise, "Unserialise value from a file");
    
        add( "getNumParams", new GetNumParamsFN(), "Return number of parameters used in y");
        add( "emap", EMap.emap, "Eagerly map FN->Vec");
    
        // ??? for some reason adding this means a NullPointerException is thrown every time
        // apply is used (via clicking on the values in the left bar of CDMS GUI)
        //     add( "makeWeightedVector", MakeWeightedVector.makeWeightVector, 
        //            "Create a weighted vector from a vector and a set of weights ([t],[continuous]->[t]");
        //      add("OldCammlIOStore", OldCammlIO.store, 
        //          "Save a file in oldCamml format. String -> Data -> String" +
        //           " ---- filename -> Data -> String" );
        //      add("OldCammlIOLoad", OldCammlIO.load, 
        //         "Load a file in the oldCamml format.  String -> [(...)]" );     
        //     add("selectColumns", SelectedVector.selectCols,
        //         "MulticolVec -> [int] -> MuiltocolVec.  Select specified columns from a multicol vec" );
        //     add("selectRows", SelectedVector.selectRows,
        //         "Vec -> [int] -> Vec.  Select specified rows from a vector" );
        //     add("splitOn", SelectedVector.splitOn,
        //         "MulticolVec -> int -> [MulticolVec].  Split vec into multiple vectors based on "+
        //         "value of column i.  Similar to what a decision tree does." );
    }


    /** Print to stdout, return TRIV */
    public static class Print extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 7305429218251394370L;

        /** System.out.println(v); return triv;  */
        public static Print println = new Print( true );

        /** System.out.print(v); return triv;  */
        public static Print print = new Print( false );

        /** Print a new line? */
        protected final boolean newLine;
    
        Print( boolean newLine ) 
        { 
            super( new Type.Function( Type.STRING, Type.TRIV) ); 
            this.newLine = newLine;
        }


        /** a -> () */
        public Value apply( Value v )
        {
            if ( newLine ) System.out.println( v );
            else System.out.print(v);
            return Value.TRIV;
        }        
    }

    /** Static instance os Serialise*/
    public static final Serialise serialise = new Serialise();
    
    /** ("filemame",val) -> triv
     *  Serialise value and write it to a file.*/
    public static class Serialise extends Value.Function {        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 7524907767078096436L;

        Serialise() {
            super( new Type.Function(new Type.Structured(new Type[] {Type.STRING,Type.TYPE}), Type.TRIV) );            
        }
        
        public void _apply(String fName, Value val) {
            try {
                ObjectOutputStream s = new ObjectOutputStream( new FileOutputStream(fName) );
                s.writeObject(val);
                s.flush();               
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            
        }
        
        /** Save v to a file */
        public Value apply(Value v) {
            Value.Structured struct = (Value.Structured)v;
            String fName = ((Value.Str)struct.cmpnt(0)).getString();
            Value val = struct.cmpnt(1);
                        
            _apply(fName,val);
            
            return Value.TRIV;
        }
    }

    public static final Unserialise unserialise = new Unserialise();
    
    /** "filemame" -> val
     *  Unserialise file and return Value. */
    public static class Unserialise extends Value.Function {        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -835480243591179223L;

        Unserialise() {    super( new Type.Function(Type.STRING, Type.TYPE) );     }
        
        public Value _apply(String fName) {
            try {
                ObjectInputStream s = new ObjectInputStream( new FileInputStream(fName) );                            
                Value val = (Value)s.readObject();
                return val;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            
        }
        
        /** Save v to a file */
        public Value apply(Value v) {
            String fName = ((Value.Str)v).getString();                                    
            return _apply(fName);
        }
    }

    /** Value.Function (m,y) -> Discrete, returns number of free parameters
     *  are contained in (m,y). */
    public static class GetNumParamsFN extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -1021361942763463361L;
        final static Type.Function tt = 
            new Type.Function( new Type.Structured(new Type[]{Type.MODEL,Type.TYPE}), 
                               Type.DISCRETE);
        public GetNumParamsFN(){super(tt);}
        
        public Value apply(Value v) {
            Value.Structured struct = (Value.Structured)v;
            ModelLearner.GetNumParams m = (ModelLearner.GetNumParams)struct.cmpnt(0);
            return new Value.Discrete(m.getNumParams(struct.cmpnt(1)));
        }
    }
    
    /** Takes a multi-column vector as a parameter and returns a weighted multicolumn
     *  vector in which each element appears at most once.  Weights indicate the 
     *  total weight of the vector in expanded vector. 
     *  e.g. [(0,1),(0,1),(0,0),(1,0),(0,1),(0,0)] -> [(0,0):2.0,(0,1):3.0,(1,0):1.0] 
     *  <br>
     *  
     *  Only works for discrete columns */
    public static Value.Vector makeWeightedSummaryVec( Value.Vector vec ) {
        
        // Copy columns into a workable format.
        Type.Structured sType = ((Type.Structured)((Type.Vector)vec.t).elt);
        final int size = sType.cmpnts.length;

        final Value.Vector zColArray[] = new Value.Vector[size];
        for (int i = 0; i < zColArray.length; i++) { zColArray[i] = vec.cmpnt(i); }
        
        int tempData[] = new int[zColArray.length];
        StructureFN.FastDiscreteStructure tempStruct = 
            new StructureFN.FastDiscreteStructure(tempData);

        HashMap<Value.Structured, double[]> hash = new HashMap<Value.Structured, double[]>();        
        for (int i = 0; i < vec.length(); i++) {
            for (int j = 0; j < size; j++) { tempData[j] = zColArray[j].intAt(i); }
                        
            double[] xx = hash.get(tempStruct);
            if (xx != null) {
                xx[0] += vec.weight(i);
            }
            else {
                // use tempStruct as a key.
                hash.put(tempStruct, new double[]{vec.weight(i)});
                // Create a net tempStruct object as old one should not be overwritten.
                tempData = new int[zColArray.length];
                tempStruct = new StructureFN.FastDiscreteStructure(tempData);
            }
        }
        
        Entry<Value.Structured,double[]> hashEntries[] = 
            hash.entrySet().toArray(new Entry[hash.size()]);
        
        
        
        // Extract weights from hashtable
        double weightArray[] = new double[hashEntries.length];
        for (int i = 0; i < hashEntries.length; i++) {
            weightArray[i] = hashEntries[i].getValue()[0];
        }
        
        // Extract FastDiscreteVectors from hashtable
        Value.Vector colArray[] = new Value.Vector[size];
        for (int col = 0; col < colArray.length; col++) {
            int valueArray[] = new int[hashEntries.length];
            for (int row = 0; row < hashEntries.length; row++) {
                valueArray[row] = hashEntries[row].getKey().intCmpnt(col);                
            }
            colArray[col] = new VectorFN.FastDiscreteVector(valueArray,
                                                            (Type.Discrete)sType.cmpnts[col]);
        }
        
        // Make MultiCol vec
        Value.Vector multiCol = new VectorFN.MultiCol( new Value.DefStructured(colArray,sType.labels) );
        
        // Attach weights to MultiCol
        Value.Vector weightedVec = new WeightedVector2( multiCol, weightArray );
        
        return weightedVec;
    }
    
    
    /** Attach a single column vector the the end of a Multicol style vector */
    public static Value.Vector joinVectors( Value.Vector multiVec, Value.Vector colVec, String name ) {
        Type.Structured sType = (Type.Structured)((Type.Vector)multiVec.t).elt;
        Value.Vector colArray[] = new Value.Vector[ sType.cmpnts.length + 1 ];
        String names[] = new String[colArray.length];
        for (int i = 0; i < colArray.length-1; i++) {
            colArray[i] = multiVec.cmpnt(i);
            names[i] = sType.labels[i];
        }
        colArray[colArray.length-1] = colVec;
        names[colArray.length-1] = name;

        return new VectorFN.MultiCol( new Value.DefStructured(colArray,names));
    }
}
