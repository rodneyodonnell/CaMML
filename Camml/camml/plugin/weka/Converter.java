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
// CDMS distribution interface to Weka
//

// File: Converter.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.weka;

import java.io.StringReader;

import weka.core.*;
import cdms.core.*;
import camml.plugin.netica.NeticaFn;
import camml.plugin.rodoCamml.RodoCammlIO;

import weka.filters.supervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;


/**
 * Functions to convert betweek weka and CDMS data typed.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.9 $ $Date: 2006/08/22 03:13:35 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/weka/Converter.java,v $
 *
 */
public class Converter {

    /** Create an Instances containing a multicol vector z and a single col vector x.
     *  'x' is set as the class variable. */
    public static Instances vectorToInstances(Value.Vector x, Value.Vector z) {
        Type.Structured zType = (Type.Structured)((Type.Vector)z.t).elt;
        
        Value.Vector subVecs[] = new Value.Vector[zType.cmpnts.length+1];
        for (int i = 0; i < subVecs.length-1; i++) {
            subVecs[i] = z.cmpnt(i);
        }
        subVecs[subVecs.length-1] = x;
        
        Value.Vector multiVec = new VectorFN.MultiCol(new Value.DefStructured(subVecs)); 
        Instances instances = vectorToInstances(multiVec);
        instances.setClassIndex(subVecs.length-1);
        return instances;
    }
    
    /**
     *  convert from CDMS Vector to weka instances.
     *  Vector is reordered so target attribute appears last. <br>
     *  
     *   weka attribute | cdms type        <br>
     *   ---------------|----------        <br>
     *   string         | Type.String      <br>
     *   numeric        | Type.Continuous  <br>
     *   nominal        | Type.Symbolic    <br>
     */
    public static Instances vectorToInstances(Value.Vector vec) {

        StringBuffer sb = new StringBuffer();
        // Print file header (unnecesarry...)
        sb.append("% weka datafile generated from CDMS\n");

        // Extract variable names fron data.
        Type.Structured sType = ((Type.Structured) ((Type.Vector) vec.t).elt);
        
        // TODO: Creating labels here is a nasty hack and should be fixed elsewhere. 
        if (sType.labels == null) {
            sType.labels = new String[sType.cmpnts.length];
        }
        for (int i = 0; i < sType.labels.length; i++) {
            sType.labels[i] = "v_"+i;
        }
        
        String[] labels = NeticaFn.makeValidNeticaNames(sType.labels, false);

        // TODO: Fix
        if (labels.length == 0) {
            throw new RuntimeException("Cannot create instance with no variables (fix this).");
        }
        
        // Print out attributes & values.
        sb.append("@relation temp\n");
        for (int i = 0; i < labels.length; i++) {
            sb.append("@attribute '" + labels[i] + "' ");

            if (sType.cmpnts[i] instanceof Type.Symbolic) {
                Type.Symbolic dType = (Type.Symbolic) sType.cmpnts[i];
                sb.append("{ ");
                for (int j = (int) dType.LWB; j < (int) dType.UPB + 1; j++) {
                    sb.append(dType.ids[j]);
                    if (j != (int) dType.UPB) {
                        sb.append(", ");
                    }
                }
                sb.append(" }\n");
            }
            else if (sType.cmpnts[i] instanceof Type.Discrete) {
                Type.Discrete dType = (Type.Discrete) sType.cmpnts[i];
                sb.append("{ ");
                for (int j = (int) dType.LWB; j < (int) dType.UPB + 1; j++) {
                    sb.append(j);
                    if (j != (int) dType.UPB) {
                        sb.append(", ");
                    }
                }
                sb.append(" }\n");
            }

            else {throw new RuntimeException("Type not handled: " + sType.cmpnts[i]); }
        }

        // Print out data
        sb.append("@data\n");
        for (int i = 0; i < vec.length(); i++) {
            Value.Structured elt = (Value.Structured) vec.elt(i);
            for (int j = 0; j < labels.length - 1; j++) {
                sb.append(elt.cmpnt(j) + ",");
            }
            sb.append(elt.cmpnt(labels.length - 1) + "\n");
        }

        //System.out.println(sb);

        StringReader sr = new StringReader(sb.toString());

        Instances instances;
        try {
            instances = new Instances(sr);
            //System.out.println(instances);
        } catch (java.io.IOException e) {
            System.out.println("sb = " + sb);
            System.out.println("vec = " + vec);
            Value.Structured elt = (Value.Structured) vec.elt(0);
            System.out.println("elt = " + elt);
            for (int j = 0; j < labels.length - 1; j++) {
                System.out.println("elt.cmpnt(j) = " + elt.cmpnt(j));                
            }
            System.out.println("vec.t = " + vec.t);
            System.out.println("vec.cmpnt(0).t = " + vec.cmpnt(0).t);
            System.out.println("elt.t = " + elt.t);
            System.out.println("elt.cmpnt(0).t = " + elt.cmpnt(0).t);
            System.out.println();
            
            System.out.println("vec.getClass() = " + vec.getClass());
            System.out.println("vec.cmpnt(0).getClass() = " + vec.cmpnt(0).getClass());
            System.out.println("elt.getClass() = " + elt.getClass());
            System.out.println("elt.cmpnt(0).getClass() = " + elt.cmpnt(0).getClass());
            throw new RuntimeException(e);
        }
        

        return instances;
    }

    /**
     *  convert from weka instances to a CDMS Vector  <br>
     *   weka attribute | cdms type        <br>
     *   ---------------|----------        <br>
     *   string         | Type.String      <br>
     *   numeric        | Type.Continuous  <br>
     *   nominal        | Type.Symbolic    <br>
     */
    public static Value.Vector instancesToVector(Instances instances) {

        int numVars = instances.numAttributes();

        // allocate space for array of CDMS vectors 
        Value.Vector[] vecArray = new Value.Vector[numVars];
        String[] nameArray = new String[numVars];

        // convert from weka to CDMS one attribute at a time.
        for (int i = 0; i < instances.numAttributes(); i++) {
            Attribute current = instances.attribute(i);
            nameArray[i] = current.name();

            if (current.isString()) { // weka string -> cdms Type.String
                Value.Str[] stringArray = new Value.Str[instances
                                                        .numInstances()];
                for (int j = 0; j < stringArray.length; j++) {
                    stringArray[j] = new Value.Str(instances.instance(j)
                                                   .toString(current));
                }
                vecArray[i] = new VectorFN.FatVector(stringArray);

            } else if (current.isNumeric()) { // weka numeric -> cdms Type.Continuous
                double[] doubleArray = instances.attributeToDoubleArray(i);
                vecArray[i] = new VectorFN.FastContinuousVector(doubleArray);
            } else if (current.isNominal()) { // weka nominal -> cdms Type.Symbolic

                // find the names of each symbolic state and create a type from them.
                java.util.Enumeration states = current.enumerateValues();
                int numStates = current.numValues();
                String[] stateArray = new String[numStates];

                for (int j = 0; j < numStates; j++) {
                    stateArray[j] = (String) states.nextElement();
                }

                stateArray = camml.plugin.netica.NeticaFn.makeValidNeticaNames(
                                                                               stateArray, true);
                // create cdms Type.Symbolic
                Type.Symbolic type = new Type.Symbolic(false, false, false,
                                                       false, stateArray);

                // Create the array of ints required to specify data.
                double[] doubleArray = instances.attributeToDoubleArray(i);
                int[] intArray = new int[doubleArray.length];
                for (int j = 0; j < intArray.length; j++) {
                    intArray[j] = (int) doubleArray[j];
                    if (Double.isNaN(doubleArray[j])) {
                        throw new RuntimeException(
                                                   "Missing values not handled properly");
                    }
                }

                // combine type and value together to form a vector of symbolic values.
                vecArray[i] = new VectorFN.FastDiscreteVector(intArray, type );

            } else { // This should never occur.
                throw new RuntimeException("Unknown type.");
            }
        }

        nameArray = camml.plugin.netica.NeticaFn.makeValidNeticaNames(
                                                                      nameArray, true);
        Value.Structured vecStruct = new Value.DefStructured(vecArray,
                                                             nameArray);
        Value.Vector vec = new VectorFN.MultiCol(vecStruct);

        return vec;
    }

    /**
     *  convert a single weka instance to a CDMS Structure  <br>
     *   weka attribute | cdms type        <br>
     *   ---------------|----------        <br>
     *   string         | Type.String      <br>
     *   numeric        | Type.Continuous  <br>
     *   nominal        | Type.Symbolic    <br>
     */
    public static Value.Structured instanceToStruct(Instance instance) {

        int numVars = instance.numAttributes();

        // allocate space for array of CDMS vectors 
        Value[] valArray = new Value[numVars];
        String[] nameArray = new String[numVars];

        // convert from weka to CDMS one attribute at a time.
        for (int i = 0; i < instance.numAttributes(); i++) {
            Attribute current = instance.attribute(i);
            nameArray[i] = current.name();

            if (current.isString()) { // weka string -> cdms Type.String
                valArray[i] = new Value.Str(instance.toString(i));

            } else if (current.isNumeric()) { // weka numeric -> cdms Type.Continuous
                valArray[i] = new Value.Continuous(
                                                   (instance.toDoubleArray())[i]);
            } else if (current.isNominal()) { // weka nominal -> cdms Type.Symbolic

                // find the names of each symbolic state and create a type from them.
                java.util.Enumeration states = current.enumerateValues();
                int numStates = current.numValues();
                String[] stateArray = new String[numStates];

                for (int j = 0; j < numStates; j++) {
                    stateArray[j] = (String) states.nextElement();
                }

                // create cdms Type.Symbolic
                Type.Symbolic type = new Type.Symbolic(false, false, false,
                                                       false, stateArray);

                // Create the array of ints required to specify data.
                int val = (int) instance.toDoubleArray()[i];
                valArray[i] = new Value.Discrete(type, val);
            } else { // This should never occur.
                throw new RuntimeException("Unknown type.");
            }
        }

        Value.Structured valStruct = new Value.DefStructured(valArray,
                                                             nameArray);

        return valStruct;
    }

    /**
     * load works out from the file extansion which type of file to try to load. 
     * Files loaded in the .arff format can automatically be discretized and have missing values
     *  replaced.  This is done through weka and something similar should be implemented in CDMS.
     * <br>
     * Currently .arff -> weka, .cas -> RodoCamml <br>     
     * Friedman format not implemented.
     */
    public static Value.Vector load(String fileName, boolean discretize,
                                    boolean fixMissing) throws java.io.FileNotFoundException,
                                                               java.io.IOException, Exception {
        // Load data from appropriately file type.
        final Value.Vector data;
        if (fileName.endsWith(".arff")) {
            Instances instances = new Instances(
                                                new java.io.FileReader(fileName));

            instances.setClassIndex(instances.numAttributes() - 1);
            // filter instances if required.
            if (discretize) {
                //DiscretizeFilter df = new DiscretizeFilter();
                Discretize df = new Discretize();
                df.setUseBetterEncoding(true);
                df.setInputFormat(instances);
                instances = weka.filters.Filter.useFilter(instances, df);
            }
            if (fixMissing) {
                ReplaceMissingValues mf = new ReplaceMissingValues();
                //ReplaceMissingValuesFilter mf = new ReplaceMissingValuesFilter();
                mf.setInputFormat(instances);
                instances = weka.filters.Filter.useFilter(instances, mf);
            }

            data = Converter.instancesToVector(instances);
        } else if (fileName.endsWith(".cas")) {
            data = RodoCammlIO.load(fileName);
        } else {
            throw new RuntimeException("Unknown file format : " + fileName);
        }
        return data;
    }

    /**
     * Works out file type by extension and saves data.
     * Currently .cas (RodoCamml) and .arff format not implemented.
     */
    public static void save(String fileName, Value.Vector data)
        throws java.io.IOException {
        // Now write the data out
        if (fileName.endsWith(".arff")) {
            throw new RuntimeException("Conversion from Value.Vector to Instances not implemented");
        } else if (fileName.endsWith(".cas")) {
            RodoCammlIO.store(fileName, data, false);
        } else {
            System.err.println("Unknown file format : " + fileName);
        }
    }

}
