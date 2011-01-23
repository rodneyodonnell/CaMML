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
// Friedman wrapper plugin
//

// File: RodoCamml.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.friedman;

import cdms.core.*;

import camml.core.models.bNet.*;
import camml.core.models.cpt.CPT;
import camml.core.models.FunctionStruct;
import camml.core.models.ModelLearner;
import camml.core.library.StructureFN;
import camml.plugin.netica.NeticaFn;
import camml.plugin.netica.NeticaFn.LoadNet;
import camml.plugin.rodoCamml.RodoCammlIO;

import java.io.*;
import java.util.ArrayList;

/**
 * Module to interface with the friedman's BNet learner.  <br>
 *  Note: This plugin requires a compiled version of Frieman to be present. <br>
 *  This can be obtained from: http://www.cs.huji.ac.il/labs/compbio/LibB/  <br>
 *   
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.16 $ $Date: 2006/08/22 03:13:33 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/friedman/FriedmanWrapper.java,v $
 */
public class FriedmanWrapper extends Module {
    public static java.net.URL helpURL = Module
        .createStandardURL(FriedmanWrapper.class);

    public String getModuleName() {
        return "FriedmanWrapper";
    }

    public java.net.URL getHelp() {
        return helpURL;
    }

    public void install(Value params) throws Exception {
        add("parameterize_BDE", new FunctionStruct(
                                                   FriedmanLearner.modelLearner_BDE).doParameterize, "");
        add("parameterize_MDL", new FunctionStruct(
                                                   FriedmanLearner.modelLearner_MDL).doParameterize, "");

        add("parameterize_MDL_Tree", new FunctionStruct(
                                                        FriedmanLearner.modelLearner_MDL_Tree).doParameterize, "");

        // Removed as file reading doesn't work
        //add("parameterize_Default", new FunctionStruct(
        //        FriedmanLearner.modelLearner_Default).doParameterize, "");
        add("parameterize_Tree", new FunctionStruct(
                                                    FriedmanLearner.modelLearner_Tree).doParameterize, "");
        // Removed as file reading doesn't work
        //add("parameterize_BinaryTree", new FunctionStruct(
        //        FriedmanLearner.modelLearner_BinaryTree).doParameterize, "");
        add("parameterize_Fixed", new FunctionStruct(
                                                     FriedmanLearner.modelLearner_Fixed).doParameterize, "");
        add("parameterize_NaiveBayes", new FunctionStruct(
                                                          FriedmanLearner.modelLearner_NaiveBayes).doParameterize, "");
        add("parameterize_TAN", new FunctionStruct(
                                                   FriedmanLearner.modelLearner_TAN).doParameterize, "");
        add("parameterize_CLTree", new FunctionStruct(
                                                      FriedmanLearner.modelLearner_CLTree).doParameterize, "");

        //     public static FriedmanLearner modelLearner_BDE = new FriedmanLearner( "" );
        //     public static FriedmanLearner modelLearner_MDL = new FriedmanLearner( "-t I" );

        //     public static FriedmanLearner modelLearner_Default = new FriedmanLearner( "-D" );
        //     public static FriedmanLearner modelLearner_Tree = new FriedmanLearner( "-T" );
        //     public static FriedmanLearner modelLearner_BinaryTree = new FriedmanLearner( "-Tb" );
        //     public static FriedmanLearner modelLearner_Fixed = new FriedmanLearner( "-F" );
        //     public static FriedmanLearner modelLearner_NaiveBayes = new FriedmanLearner( "-t B <class>" );
        //     public static FriedmanLearner modelLearner_TAN = new FriedmanLearner( "-t F <class>" );
        //     public static FriedmanLearner modelLearner_CLTree = new FriedmanLearner( "-t T" );

    }

    /*
     * Write out the names of the parane states as shown below. <br>
     * This is required when writing .net and .names files
     * (var '<var1 name> '(<var1.state1> <var1.state2>))                                        <br>
     * (var '<var2 name> '(<var2.state1> <var2.state2> <var2.state3>))                          <br>
     * (var '<var3 name> '(<var3.state1> <var3.state2> <var3.state3> <var3.state4>))            <br>
     */
    public static Writer writeNodeNames(String[] varName, String[][] stateName,
                                        Writer writer) throws IOException {

        // Number of variables and the states each has.
        for (int i = 0; i < varName.length; i++) {
            writer.write("(var " + varName[i] + " ( ");
            for (int j = 0; j < stateName[i].length; j++) {
                writer.write(stateName[i][j] + " ");
            }
            writer.write("))\n");
        }
        writer.write("\n");

        writer.flush();
        return writer;
    }

    /**
     * Take a BNet and it's parameters and return a string compadible with the friedman file 
     * format <br>
     *
     * The format appears to be as follows                                                      <br>
     * (network '<network name> ':probability)                                                  <br>
     *                                                                                          <br>
     * (var '<var1 name> '(<var1.state1> <var1.state2>))                                        <br>
     * (var '<var2 name> '(<var2.state1> <var2.state2> <var2.state3>))                          <br>
     * (var '<var3 name> '(<var3.state1> <var3.state2> <var3.state3> <var3.state4>))            <br>
     *                                                                                          <br>
     * (parents '<var1.name> '() '( <p1> <p2> ))                                                <br>
     * (parents '<var2.name> '(<parant1> <parent2>)                                             <br>
     *   (                                                                                      <br>
     *     (( 0  0)      0.1 0.9 )                                                              <br>
     *     (( 0  1)        0 1 )                                                                <br>
     *     (( 0  2)        0 1 )                                                                <br>
     *     (( 1  0)       0.2 0.8 )                                                             <br>
     *     (( 1  1)        0 1 )                                                                <br>
     *     (( 1  2)        0 1 )                                                                <br>
     *     (( 2  0)       0.5 0.5 )                                                             <br>
     *     (( 2  1)        0 1 )                                                                <br>
     *     (( 2  2)        0 1 )                                                                <br>
     *     (( 3  0)       0.4 0.6 )                                                             <br>
     *     (( 3  1)        0 1 )                                                                <br>
     *     (( 3  2)        0 1 )                                                                <br>
     *   )                                                                                      <br>
     * )                                                                                        <br>
     * (parents '<var3.name> '() '(0.483333 0.216667 0.216667 0.0833333))                       <br>
     *                                                                                          <br>
     * NOTE: Angular brackets should be omitted from the file, and ' chars seem to be optional.
     */
    public static Writer writeNetwork(BNet bNet, Value.Vector params,
                                      String netName, Writer writer) throws IOException {

        String[] varName = bNet.makeNameList(params);
        String[][] stateName = bNet.makeStateNameList();
        int[][] parentList = bNet.makeParentList(params);
        int[] arity = bNet.getArity();

        // Write out network header
        writer.write("(network " + netName + " :probability)\n\n");

        // Write node names and states
        writeNodeNames(varName, stateName, writer);

        // write parents of each variable and CPTs
        for (int i = 0; i < varName.length; i++) {

            Value.Structured elt = (Value.Structured) params.elt(i);
            Value.Model subModel = (Value.Model) ((Value.Structured) elt
                                                  .cmpnt(2)).cmpnt(0);
            Value subParams = ((Value.Structured) elt.cmpnt(2)).cmpnt(1);

            writer.write("(parents " + varName[i] + " ( ");
            // print list of parents
            for (int j = 0; j < parentList[i].length; j++) {
                writer.write(varName[parentList[i][j]] + " ");
            }
            writer.write(")");

            // Find the arity for each parent node
            int[] parentArity = new int[parentList[i].length];
            int numCombinations = 1;
            for (int j = 0; j < parentArity.length; j++) {
                parentArity[j] = arity[parentList[i][j]];
                numCombinations *= parentArity[j];
            }
            int[] index = new int[parentArity.length];

            // for no parents, write (0.013432799999999995    0.986567 )
            if (parentArity.length == 0) {
                writer.write("(");

                Value.Structured z = new Value.DefStructured(new Value[] {});
                for (int j = 0; j < arity[i]; j++) {
                    Value.Discrete x = new Value.Discrete(j);
                    writer.write(Math.exp(subModel.logP(x, subParams, z))
                                 + "\t");
                }

                writer.write(")");
            }
            // for parents write:
            // (
            //   (( 0 ) 0.827103    0.172897    )
            //   (( 1 ) 0.485812    0.514188    )
            // )
            else {
                writer.write("\n(\n");
                for (int combination = 0; combination < numCombinations; combination++) {
                    writer.write("  (( ");
                    // state names go here
                    for (int j = 0; j < index.length; j++) {
                        writer.write(stateName[parentList[i][j]][index[j]]
                                     + " ");
                    }
                    writer.write(")");

                    // parameters go here            
                    Value.Structured z = new StructureFN.FastDiscreteStructure(
                                                                               index);
                    for (int j = 0; j < arity[i]; j++) {
                        Value.Discrete x = new Value.Discrete(j);
                        writer.write(Math.exp(subModel.logP(x, subParams, z))
                                     + "\t");
                    }
                    writer.write(")");
                    BNet.incrementBitfield(index, parentArity);
                }
                writer.write("\n)");
            }

            writer.write(")\n\n");
        }
        writer.flush();
        return writer;
    }

    /** 
     * Write out data and names files to the Writers provided in the friedman format. <br>
     * 
     */
    public static void writeCases(Value.Vector vec, java.io.Writer data,
                                  java.io.Writer names) throws java.io.IOException {

        // extract state name information from vector type.
        Type.Structured type = (Type.Structured) ((Type.Vector) vec.t).elt;
        String[] varName = NeticaFn.makeValidNeticaNames(type.labels,false);
        String[][] stateName = new String[varName.length][];
        for (int i = 0; i < stateName.length; i++) {
            if (Type.SYMBOLIC.hasMember(type.cmpnts[i])) {
                //         String[] temp = ((Type.Symbolic)type.cmpnts[i]).ids;
                //         stateName[i] = new String[temp.length];
                //         for ( int j = 0; j < stateName[i].length; j++ ) {
                //             stateName[i][j] = camml.plugin.netica.NeticaFn.makeValidNeticaName( temp[j] );
                //         }
                stateName[i] = NeticaFn.makeValidNeticaNames(((Type.Symbolic) type.cmpnts[i]).ids, false);
            } else if (Type.DISCRETE.hasMember(type.cmpnts[i])) {
                Type.Discrete t = (Type.Discrete) type.cmpnts[i];
                int arity = (int) t.UPB - (int) t.LWB + 1;
                stateName[i] = new String[arity];
                for (int j = 0; j < stateName[i].length; j++) {
                    // replace any non-standard chars with an underscore
                    String name = String.valueOf(j + (int) t.LWB);
                    //             stateName[i][j] = camml.plugin.netica.NeticaFn.makeValidNeticaName( name );
                    stateName[i][j] = name;
                }
                stateName[i] = camml.plugin.netica.NeticaFn
                    .makeValidNeticaNames(stateName[i], true);
            } else {
                throw new RuntimeException(
                                           "writeCases only handles symbolic data.");
            }

        }

        // Write out names file
        writeNodeNames(varName, stateName, names);
        names.flush();

        // write out data file.
        for (int i = 0; i < vec.length(); i++) {
            data.write("(");
            Value.Structured elt = (Value.Structured) vec.elt(i);
            for (int j = 0; j < elt.length(); j++) {
                data.write(stateName[j][elt.intCmpnt(j)] + " ");
            }
            data.write(")\n");
        }
        data.write("\n");

        data.flush();

    }

    /**
     * Read in a network from the input stream in the friedman format described 
     * in @see writeNetwork
     */
    public static Value.Structured readNetwork(Reader reader)
        throws IOException {

        // create a stream tokenizer for easy text manipulation
        RodTokenizer t = new RodTokenizer(reader);

        // read in header
        t.readChar('(');
        t.readString("network");
        t.readAny(); // name of network
        t.readString("probability");
        t.readChar(')');

        // Read in variable and state names from header.
        String[][] varNameRef = new String[1][];
        String[][][] stateNameRef = new String[1][][];
        readNodeNames(t, varNameRef, stateNameRef);
        String[] varName = varNameRef[0];
        String[][] stateName = stateNameRef[0];

        // Make shortcuts to numNodes and arity of variables.
        final int numNodes = varName.length;
        int[] arity = new int[numNodes];
        for (int i = 0; i < arity.length; i++) {
            arity[i] = stateName[i].length;
        }

        // cptArray is an array of all the CPTs in the network.
        double[][][] cptArray = new double[numNodes][][];
        int paramsRead[][] = new int[numNodes][1];
        
        // Store the parents of each state.
        int[][] parentArray = new int[numNodes][];

        // -----------------------------------------------
        // --- Now we read in the connections and CPTs ---
        // -----------------------------------------------
        boolean firstVar = true;
        while (t.peekType() != StreamTokenizer.TT_EOF) {
            // if this is the first variable, or the first token is '(' everything is fine.
            // This has to be done as readNodeNames has a habit of eating the next '(' char after
            // it has finished.
            if (t.peekType() == '(' || firstVar != true) {
                t.readChar('(');
            }
            firstVar = false;

            t.readString("parents");
            int node = t.tokenID(varName);

            t.readChar('(');

            // Read in list of parents.
            int[] parent = new int[numNodes];
            int parentsRead = 0;
            int numCombinations = 1;
            while (t.peekType() != ')') {
                parent[parentsRead] = t.tokenID(varName);
                numCombinations *= stateName[parent[parentsRead]].length;
                parentsRead++;
            }
            t.readChar(')');

            // Crop the parent array to an appropriate length
            int[] tmp = new int[parentsRead];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = parent[i];
            }
            parent = tmp;
            parentArray[node] = parent;

            cptArray[node] = readLocalStructure(numCombinations, parent, arity,
                                                node, stateName, varName, t, paramsRead[node]);

            //         for ( int  i = 0; i < cptArray[node].length; i++ ) {
            //         System.out.println( cptArray[node][i] );
            //         }
        }

        // ------------------------------------------------
        // --- All file IO done.  Now convert to Values ---
        // ------------------------------------------------

        // Create the BNet model
        Type.Symbolic[] typeArray = new Type.Symbolic[numNodes];
        for (int i = 0; i < typeArray.length; i++) {
            typeArray[i] = new Type.Symbolic(false, false, false, false,
                                             stateName[i]);
        }
        Type.Structured dataType = new Type.Structured(typeArray, varName);
        BNet bNet = new BNetStochastic(dataType);

        // Create the parameters to go with the BNet model
        Value.Structured[] valueArray = new Value.Structured[numNodes];
        for (int i = 0; i < valueArray.length; i++) {

            Type.Symbolic xType = typeArray[i];
            Type.Symbolic[] zTypeArray = new Type.Symbolic[parentArray[i].length];
            for (int j = 0; j < zTypeArray.length; j++) {
                zTypeArray[j] = typeArray[parentArray[i][j]];
            }
            Value.Structured cptStruct = CPT.makeCPTStruct(cptArray[i], xType,
                                                           new Type.Structured(zTypeArray));
            
            // Call setNumParams to ensure cpt.getNumParams() returns the correct value
            // even when CPT is represnting a DTree. (ie. should return the number of
            // parameters the decision tree used, not the normal full CPT number)
            ((CPT)cptStruct.cmpnt(0)).setNumParams(paramsRead[i][0]);
            
            valueArray[i] = new Value.DefStructured(
                                                    new Value[] { new Value.Str(varName[i]),
                                                                  new VectorFN.FastDiscreteVector(parentArray[i]),
                                                                  cptStruct });
        }

        return new Value.DefStructured(new Value[] { bNet,
                                                     new VectorFN.FatVector(valueArray) });
    }

    /**
     * Read in local structure (CPT,Tree,etc) from tokenizer after parents found, examples : <br>
     * '(0.201058 0.201058 0.597884)                                                         <br>
     * (((A) 0.0606061 0.150364 0.789029) ((B) 0.208333 0.22807 0.563596))                   <br>
     *                                                                                       <br>
     * '(Tree (Leaf 0.201058 0.201058 0.597884))                                             <br>
     * '(Tree (Test class (A (Leaf 0.0606061 0.150364 0.789029))                             <br>
     *                    (B (Leaf 0.208333 0.22807 0.563596)) ))                            <br>
     *                                                                                       <br>
     *  '(Default ((() 0.333333 0.333333 0.333333)))                                         <br>
     *  '(Default (((v____inf_0_6__ Iris_setosa) 0.991758 0.00274725 0.00274725 0.00274725)  <br>
     *             ((v___0_6_1_7__ Iris_versicolor) 0.00280269 0.890695 0.1037 0.00280269)   <br>
     *             ((v___1_7_inf__ Iris_virginica) 0.00304878 0.00304878 0.288415 0.705488)  <br>
     *             (() 0.089286 0.196429 0.410714 0.303571)))                                <br>
     *                                                                                       <br>
     *  '(BinaryTree (Leaf 0.290427 0.709573))                                               <br>
     *  '(BinaryTree (Test class tested_negative                                             <br>
     *                     (Test skin v____inf_7__ (Leaf 0.379111 0.620889)                  <br>
     *                     (Test skin v___7_23__ (Leaf 0.790406 0.209594)                    <br>
     *               (Leaf 0.596663 0.403337)))                                        <br>
     *              (Leaf 0.267098 0.732902))))                                            <br>
     */
    public static double[][] readLocalStructure(int numCombinations,
                                                int[] parent, int[] arity, int node, String[][] stateName,
                                                String[] varName, RodTokenizer t, int[] paramsRead) throws IOException {
        double[][] cpt = new double[numCombinations][];

        t.readChar('(');

        String nextToken = t.readAny();
        t.pushBack();

        final int CPT = 0, TREE = 1, DEFAULT = 2, BINARY_TREE = 3;
        final int structureType; // What type of local structure are we dealing with?
        if (nextToken.equals("(")) {
            structureType = CPT;
        } else if (nextToken.equals("Tree")) {
            structureType = TREE;
        } else if (nextToken.equals("Default")) {
            structureType = DEFAULT;
        } else if (nextToken.equals("BinaryTree")) {
            structureType = BINARY_TREE;
        } else {
            structureType = CPT;
        } // a number will be returned by a CPT with no parents. 

        // for a single parent expext "0.0238095 0.97619"
        // or "Tree (Leaf 0.333333 0.333333 0.333333)" for a Tree
        if (parent.length == 0) {
            if (structureType == CPT) {                
                cpt[0] = readRow(arity[node], t, paramsRead);
            } else {
                t.readAny(); // Read in "Tree","Default" or "BinaryTree"
                t.readChar('(');

                // Read in "Leaf" from a tree, "()" is used to signal default values 
                if (structureType == TREE || structureType == BINARY_TREE) {
                    t.readString("Leaf");
                } else if (structureType == DEFAULT) {
                    t.readChar('(');
                    t.readChar('(');
                    t.readChar(')');
                }
                cpt[0] = readRow(arity[node], t, paramsRead); // Read in actual values.
                t.readChar(')');
                if (structureType == DEFAULT) {
                    t.readChar(')');
                }

            }
        }

        // for multiple parents expect "((Visit )0.5 0.5 )((No_Visit )0.0121951 0.987805 )"
        // or 
        else { // if multiple parents
            if (structureType == CPT) {
                for (int i = 0; i < cpt.length; i++) {
                    t.readChar('(');
                    t.readChar('(');

                    // Read in parent state.
                    int rowNum = 0;
                    int index[] = new int[parent.length];
                    for (int j = 0; j < parent.length; j++) {
                        index[j] = t.tokenID(stateName[parent[j]]);
                    }
                    for (int j = parent.length - 1; j >= 0; j--) {
                        rowNum *= arity[parent[j]];
                        rowNum += index[j];
                    }
                    t.readChar(')');

                    // read in row of CPT
                    double[] row = new double[arity[node]];
                    for (int j = 0; j < row.length; j++) {
                        row[j] = t.readNum();
                    }

                    t.readChar(')');
                    cpt[rowNum] = row;
                    paramsRead[0] += row.length-1;
                }
            } else if (structureType == TREE) {
                t.readString("Tree");
                int[] set = new int[varName.length];
                // represent parent[i] is not set.
                for (int i = 0; i < set.length; i++) {
                    set[i] = -1;
                }

                // recursively read the tree.                
                cpt = readTree(parent, arity, node, stateName, varName, cpt,
                               set, t, paramsRead);
                
                
                //             try {
                //             t.eolIsSignificant(true);
                //             while ( t.peekType() != t.TT_EOF ) { System.out.print(t.readAny() + " " ); }
                //             } catch ( Exception e ) { System.out.println(e);}           
            }
        }
        //         if ( treeLeaf ) { t.readChar(')'); }

        //         System.out.println("\n--\n");
        t.readChar(')');
        t.readChar(')');

        return cpt;

    }

    /** Recursively read dtree and return a CPT containing results */
    protected static double[][] readTree(int[] parent, int[] arity, int node,
                                         String[][] stateName, String[] varName, double[][] cpt, int[] set,
                                         RodTokenizer t, int[] paramsRead) throws IOException {

        t.readChar('(');
        // figure out if this is a leaf or test node
        boolean leaf = (t.tokenID(new String[] { "Leaf", "Test" }) == 0);
        if (leaf) {
            //         System.out.print( "Set ; " );
            //         for ( int i = 0; i < parent.length; i++ ) { System.out.print( set[parent[i]] + "\t" ); }
            //         System.out.print("\t\t");

            double[] row = readRow(arity[node], t, paramsRead);            
            
            // Now we have to sift through the CPT using this row in appropriate places.

            // parentArity is set to the flexibility of each parent.  If the parent is fixed
            // then this is 1, if it is not fixed, the parents arity is used.
            int[] parentArity = new int[parent.length];
            int availableParentCombinations = 1;
            for (int i = 0; i < parentArity.length; i++) {
                if (set[parent[i]] == -1) {
                    parentArity[i] = arity[parent[i]];
                } else
                    parentArity[i] = 1;
                availableParentCombinations *= parentArity[i];
            }
            //         System.out.println("availableCombinations = " + availableParentCombinations );
            int[] index = new int[parent.length];
            int[] offset = new int[parent.length];

            // use offset to add values kept constant.
            for (int i = 0; i < offset.length; i++) {
                offset[i] = set[parent[i]];
                if (offset[i] == -1) {
                    offset[i] = 0;
                }
            }

            // multipliers show how much each row is worth
            int[] multipliers = new int[parent.length];
            multipliers[0] = 1;
            for (int i = 1; i < multipliers.length; i++) {
                multipliers[i] = multipliers[i - 1] * arity[parent[i - 1]];
            }

            // Now loop through updating cpt.
            for (int i = 0; i < availableParentCombinations; i++) {
                int combinationNum = 0;
                for (int j = 0; j < index.length; j++) {
                    combinationNum += multipliers[j] * (index[j] + offset[j]);
                }
                cpt[combinationNum] = row;

                //         System.out.print( "Combination " );
                //         for ( int j = 0; j < index.length; j++ ) {
                //             System.out.print( (index[j] + offset[j])  + " " );
                //         }
                //         System.out.print( " = " + combinationNum + " = ");
                //         for ( int j = 0; j < row.length; j++ ) {
                //             System.out.print( row[j] + "\t" );
                //         }
                //         System.out.println();

                BNet.incrementBitfield(index, parentArity);

            }

        } else {
            // Which varaible am I splitting on?
            int splitID = t.tokenID(varName);
            for (int i = 0; i < arity[splitID]; i++) {
                t.readChar('(');
                //int parentVal = t.tokenID(stateName[splitID]);
                
                // Read variable to be split on.
                // Added 12/5/06, though this code previously worked without it??
                t.readString( stateName[splitID][i]);
                
                set[splitID] = i;
                readTree(parent, arity, node, stateName, varName, cpt, set, t, paramsRead);
                t.readChar(')');
            }
            set[splitID] = -1;

        }
        t.readChar(')');
        return cpt;

    }

    /** Read numTokens numbers from t and return the result. */
    protected static double[] readRow(int numTokens, RodTokenizer t, int[] paramsRead)
        throws IOException {
        double[] row = new double[numTokens];
        for (int i = 0; i < row.length; i++) {
            row[i] = t.readNum();
            //         System.out.print( row[i] + "\t" );
        }
        paramsRead[0] += row.length-1;
        //     System.out.println();
        return row;
    }

    public static Value.Vector readCases(Reader nameReader, Reader dataReader)
        throws java.io.IOException {
        // create a stream tokenizer for easy text manipulation
        RodTokenizer d = new RodTokenizer(dataReader);
        RodTokenizer n = new RodTokenizer(nameReader);

        // Read in variable and state names from name file.
        String[][] varNameRef = new String[1][];
        String[][][] stateNameRef = new String[1][][];
        readNodeNames(n, varNameRef, stateNameRef);
        String[] varName = varNameRef[0];
        String[][] stateName = stateNameRef[0];

        final int numNodes = varName.length;

        // Create the Data type
        Type.Symbolic[] typeArray = new Type.Symbolic[numNodes];
        for (int i = 0; i < typeArray.length; i++) {
            typeArray[i] = new Type.Symbolic(false, false, false, false,
                                             stateName[i]);
        }

        // Now load in the data.
        ArrayList<int[]> list = new ArrayList<int[]>();
        while (d.peekType() == '(') {
            d.readChar('(');
            int[] row = new int[varName.length];
            for (int i = 0; i < row.length; i++) {
                row[i] = d.tokenID(stateName[i]);
            }
            list.add(row);
            d.readChar(')');
        }
        int[][] preTransformData = list.toArray(new int[list.size()][]);

        // We now have the date in [row][column] form, however many operations are optimised to
        // to deal with MultiCol data in [column][row] form.  So we do a quick conversion.
        int[][] data = new int[numNodes][preTransformData.length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = preTransformData[j][i];
            }
        }

        // Now we convert the data into a CDMS readable format.
        VectorFN.DiscreteVector[] vectorArray = new VectorFN.DiscreteVector[numNodes];
        Type.Vector[] vecType = new Type.Vector[vectorArray.length];
        for (int i = 0; i < numNodes; i++) {
            vecType[i] = new Type.Vector(typeArray[i]);
            vectorArray[i] = new VectorFN.DiscreteVector(vecType[i], data[i]);
        }
        // Se need structType so labels are preserved in multiVec
        Type.Structured structType = new Type.Structured(vecType, varName);
        VectorFN.MultiCol multiVec = new VectorFN.MultiCol(
                                                           new Value.DefStructured(structType, vectorArray));

        // return the finished vector.
        return multiVec;
    }

    /*
     * Read in a network of the form @see writeNodeNames <br>
     * NOTE: This function actually reads an extra "(" char after node names and will crash if the
     *       next set of tokens is not of the form "( <word>"
     */
    public static void readNodeNames(RodTokenizer t, String[][] varNameRef,
                                     String[][][] stateNameRef) throws IOException {
        ArrayList<String> nodes = new ArrayList<String>();
        ArrayList<String[]> nodeStates = new ArrayList<String[]>();

        // read in var names
        while (t.peekType() == '(') {
            t.readChar('(');

            // At this point we can have 2 valid strings "var" or "parents"
            // if "parente" is enocuntered we have gone too far.  Put this back and continue.
            if (t.tokenID(new String[] { "var", "parents" }) != 0) {
                t.pushBack();
                break;
            }

            // Read in the name of a variable and add it to nodes
            nodes.add(t.readAny());

            t.readChar('(');

            // Read in all state names.
            ArrayList<String> states = new ArrayList<String>();
            while (t.peekType() != ')') {
                states.add(t.readAny());
            }

            if (states.size() == 0) {
                throw new RuntimeException("No states read in?");
            }

            t.readChar(')');
            t.readChar(')');

            // Add an array of string to nodeStates
            nodeStates.add(states.toArray(new String[states.size()]));
        }

        // Package up the names into varNameRef and stateNameRef
        varNameRef[0] = new String[nodes.size()];
        stateNameRef[0] = new String[nodes.size()][];

        for (int i = 0; i < varNameRef[0].length; i++) {
            varNameRef[0][i] = (String) nodes.get(i);
            stateNameRef[0][i] = (String[]) nodeStates.get(i);
        }
    }

    /** Read in a network from file.  Determing format by file extension.  */
    public static Value.Structured loadNet(String source)
        throws java.io.IOException {
        Value.Structured struct;
        if (source.endsWith(".dnet") || source.endsWith(".dne")) {
            struct = LoadNet._apply(source);
        } else if (source.endsWith(".net")) {
            struct = readNetwork(new FileReader(source));
        } else {
            throw new RuntimeException("Unrecognised extension : " + source);
        }

        return struct;
    }

    /** Write a network to file.  Determing format by file extension.  struct = (model,params) */
    public static void saveNet(String dest, Value.Structured struct)
        throws java.io.IOException {
        BNet bNet = (BNet) struct.cmpnt(0);
        Value.Vector params = (Value.Vector) struct.cmpnt(1);

        if (dest.endsWith(".dnet") || dest.endsWith(".dne")) {
            throw new RuntimeException("Netica writing not implemented.");
        } else if (dest.endsWith(".net")) {
            writeNetwork(bNet, params, dest, new FileWriter(dest));
        } else {
            throw new RuntimeException("Unrecognised extension : " + dest);
        }
    }

    /** Load a data file. Determing format by file extension. */
    public static Value.Vector loadData(String source)
        throws java.io.IOException {
        Value.Vector data;
        if (source.endsWith(".cas")) {
            data = (Value.Vector) RodoCammlIO.load(source);
        } else if (source.endsWith(".data")) {
            String stub = source.substring(0, source.length() - 5);
            //         System.out.println("stub = " + stub);
            data = readCases(new FileReader(stub + ".names"), new FileReader(
                                                                             source));
        } else if (source.endsWith(".arff")) {
            try {
                data = camml.plugin.weka.Weka.load(source, true, true);
            } catch (Exception e) {
                throw new RuntimeException("Error Loading arff format", e);
            }

        } else {
            throw new RuntimeException("Unrecognised Extension " + source
                                       + " require [.cas|.data]");
        }
        return data;
    }

    /** Load a data file. Determing format by file extension. */
    public static void saveData(String dest, Value.Vector data)
        throws java.io.IOException {
        if (dest.endsWith(".cas")) {
            RodoCammlIO.store(dest, data, false);
        } else if (dest.endsWith(".data")) {
            String stub = dest.substring(0, dest.length() - 5);
            writeCases(data, new FileWriter(dest), new FileWriter(stub
                                                                  + ".names"));
        } else {
            throw new RuntimeException("Unrecognised Extension " + dest
                                       + " require [.cas|.data]");
        }
    }

    /** Test main() function converts a netica file to a friedman file*/
    public static void main(String[] args) throws java.io.IOException {

        if (args.length == 2 && args[0].equals("read")) {
            loadData(args[1]);
        } else {
            runCommandParams(args);
        }
    }

    /*   *//**
            * Probe list applying o.equals() to each member.  Return the position it is found at.
            * -1 is returned if not in list.
            */
    /*
      public static int find( Object[] list, Object o ) {
      for ( int i = 0; i < list.length; i++ ) { if ( o.equals(list[i]) ) { return i; } }
      return -1;
      }
    */

    public static void runCommandParams(String[] args)
        throws java.io.IOException {
        if (args.length != 0) {
            String cmd = args[0];

            // Convert a network -> network or datafile -> datafile based on file extensions.
            if (cmd.equals("convert")) {
                if (args.length != 3) {
                    System.out.println("try convert infile outfile");
                    return;
                }

                String source = args[1];
                String dest = args[2];

                // If a network
                if (source.endsWith(".net") || source.endsWith(".dnet")
                    || source.endsWith(".dne")) {
                    saveNet(dest, loadNet(source));
                }
                // else if data
                else {
                    saveData(dest, loadData(source));
                }
            } else if (cmd.equals("run")) {
                if (args.length != 4) {
                    String error = "try run [Friedman|Rodo|Camml2] <datafile> <output net>";
                    throw new IllegalArgumentException(error);
                }
                String metric = args[1];
                String dataFile = args[2];
                String outFile = args[3];

                ModelLearner learner;
                if (metric.equals("Friedman")) {
                    learner = FriedmanLearner.modelLearner_BDE;
                } else if (metric.equals("Rodo")) {
                    learner = camml.plugin.rodoCamml.RodoCammlLearner.modelLearner;
                } else if (metric.equals("Camml2")) {
                    learner = camml.core.models.bNet.BNetLearner.metropolis;
                } else {
                    throw new IllegalArgumentException(metric
                                                       + " !=  [Friedman|Rodo|Camml2]");
                }

                try {
                    Value.Vector data = loadData(dataFile);
                    Value.Structured msy = learner.parameterize(Value.TRIV,
                                                                data, data);
                    saveNet(outFile, new Value.DefStructured(new Value[] {
                                msy.cmpnt(0), msy.cmpnt(2) }));
                } catch (ModelLearner.LearnerException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.out.println("try [convert|run] for help");
        }

    }

    /**
     * RodTokenizer extends tokenizer adding additional functionality 
     * nextToken() is not recommended when using RodTokenizer, the appropriate type should be
     * requested instead.  The next token type is accesable via peekType(); <br>
     *
     * By default all numbers are set as word chars (and NOT number chars).  Assorted chars have
     * been added to whitespace to allow for easier parsing of Friedman files (this can be fixed.)
     */
    public static class RodTokenizer extends StreamTokenizer {
        public RodTokenizer(Reader r) {
            super(r);
            initialize();
        }

        /** Read in the appropriate char or throw an exception. */
        public void readChar(char c) throws java.io.IOException {
            if (nextToken() != c) {
                throw new TokenizerException(String.valueOf(c));
            }
        }

        /** Read in the appropriate string or throw an exception. */
        public void readString(String s) throws java.io.IOException {
            if (nextToken() != TT_WORD || !sval.equals(s)) {
                throw new TokenizerException(s);
            }
        }

        /** Exception throws when misreading a class */
        public class TokenizerException extends IOException {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 3578401580725962338L;

            public TokenizerException(String expected) {
                super("expecting <" + expected + "> in "
                      + RodTokenizer.this.toString());
            }
        }

        public void initialize() {
            resetSyntax();

            wordChars('a', 'z');
            wordChars('A', 'Z');
            wordChars('0', '9');
            wordChars('.', '.');
            wordChars('-', '-');
            wordChars('_', '_');
            wordChars('?', '?');

            eolIsSignificant(false);
            whitespaceChars('\'', '\''); // ' should be treated as whitespace
            whitespaceChars(':', ':'); // : should be treated as whitespace
            whitespaceChars('\t', '\t');
            whitespaceChars(' ', ' ');
            whitespaceChars(TT_EOL, TT_EOL); // Linefeed
            whitespaceChars(13, 13); // Carriage return

        }

        /** return next token (of whatever type) as a string */
        public String readAny() throws java.io.IOException {

            String s;

            switch (nextToken()) {
            case StreamTokenizer.TT_NUMBER:
                throw new RuntimeException("Numbers not handled??");
            case StreamTokenizer.TT_WORD:
                s = sval;
                break;
            case StreamTokenizer.TT_EOF:
                s = "EOF";
                break;
            case StreamTokenizer.TT_EOL:
                s = "EOL";
                break;
            default:
                s = String.valueOf((char) ttype);
                break;
            }

            return s;

        }

        /** Peek at the type of the next token*/
        public int peekType() throws java.io.IOException {
            int type = nextToken();
            pushBack();
            return type;
        }

        /** read in and return an number (can also handle e^3 notation in future) */
        public double readNum() throws java.io.IOException {
            return Double.parseDouble(readAny());
        }

        /** Read in the next token and search for it in array.  If not present, throw exception */
        public int tokenID(String[] array) throws java.io.IOException {
            String s = this.readAny();
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(s)) {
                    return i;
                }
            }

            // Throw Exception showing possible options
            StringBuffer options = new StringBuffer();
            options.append("[");
            for (int i = 0; i < array.length; i++) {
                options.append(array[i]);
                if (i != array.length - 1) {
                    options.append(",");
                }
            }
            options.append("]");
            throw new TokenizerException(options.toString());
        }

    }

}
