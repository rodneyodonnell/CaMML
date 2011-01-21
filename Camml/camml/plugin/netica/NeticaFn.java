//
// Netica Plugin
//
// Various functions to allow interaction between CDMS and Netica
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: NeticaFN.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.netica;

import java.io.FileWriter;
import java.io.IOException;

import cdms.core.*;

import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Net;
import norsys.netica.Streamer;
import norsys.netica.Util;

import camml.core.models.bNet.BNet;
import camml.core.models.cpt.CPT;
import camml.core.models.multinomial.MultinomialLearner;

public class NeticaFn
{

    /**
     *  if s is a valid netica name it is returned. Otherwise the name is mangled as appropriate<br>
     *  a "v_" is added to names not beginning with non-alphabetic characters. <br>
     *  All non alphanumeric characters are replaced with an underscore.
     *
     *  There may be more operations needed I am currently unaware of.  This should be further 
     *  investigated.
     */
    /*public*/ private static String makeValidNeticaName( String s ) 
    {
        // append "v_" to start it variable does not start with a letter
        if ( !Character.isLetter( s.charAt(0) ) ) {
            s = "v_" + s;
        }

        // If a non [a..z|A..Z|0..9|_] char is found, replace it with an underscore.
        for ( int i = 0; i < s.length(); i++ ) {
            char x = s.charAt(i);
            if ( !(Character.isDigit(x) || Character.isLetter(x) || x == '_') ) {
                s = s.replace( x, '_' );
            }
        }

        // Strings must have less than 30 chars.
        if ( s.length() > 30 ) { s = s.substring(0,30); }

        return s;
    }

    /**
     * Runs makeVelidNeticaName on each element of s, but adds the constraint that each resulting
     *  string must be different.  This is useful to ensure all variable or state names are unique.
     * If overwrite == false a new array is returned, if overwrite == true, the original array is
     *  returned (with modified names.)
     */
    public static String[] makeValidNeticaNames( String[] s, boolean overwrite ) 
    {
        // If overwriting s is not required, make a copy of s.
        if ( overwrite == false ) {
            s = (String[])s.clone();
        }

        for ( int i = 0; i < s.length; i++ ) {
            s[i] = makeValidNeticaName( s[i] );
        }

        // Try adding an _x to the end where x is the position in the original order.
        int numChanges = 0;
        for ( int i = 0; i < s.length; i++ ) {
            if ( s[i] == null ) { s[i] = "var_"+i;}
            for ( int j = i+1; j < s.length; j++ ) {
                if ( s[i].equals(s[j]) ) {
                    s[i] = s[i] + "_" + i;
                    s[j] = s[j] + "_" + j;
                    numChanges ++;
                }
            }
        }        
    
        // Do a quick check to make sure all elements are now different.
        boolean allDifferent = true;
        if ( numChanges > 0 ) {
            for ( int i = 0; i < s.length; i++ ) {
                for ( int j = i+1; j < s.length; j++ ) {
                    if ( s[i].equals(s[j]) ) {
                        allDifferent = false;
                    }
                }
            }                
        }

        // If all elements still not all different, give up and rename everything var_x
        if ( !allDifferent ) {
            for ( int i = 0; i < s.length; i++ ) {
                s[i] = "var_" + i;
            }
        }
    

        return s;
    }



    /** static instance of loadNet. */
    public static LoadNet loadNet = new LoadNet();

    /** LoadNet loads in a netica network file.  "fileName.dnet" -> (BNetNetica,params) */
    public static class LoadNet extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 1900918945676015406L;
        static final Type.Function tt = new Type.Function( Type.STRING, Type.STRUCTURED );
        public LoadNet() { super(tt); }

        /** Load in Bayes Net. Str -> (Model,params) */
        public Value apply( Value v )
        {
            return _apply( ((Value.Str)v).getString() );
        }

        /** Load in Bayes Net. Str -> (Model,params) <br>
         *  Any zero-arity nodes are removed as they are usually title nodes. */
        public static Value.Structured _apply( String fileName )
        {
            synchronized (Netica.env) { try {
                    // initialise environment
                    //Environ env = Netica.env;

                    // read in network from input file.
                    System.out.println("filename = " + fileName);
                    Net net = new Net( new Streamer( fileName ) );

                    // Remove all nodes containing zero states.
                    // These are commonly used as "TITLE" nodes in netica examples.
                    NodeList nodeList = net.getNodes();
                    for (int i = 0; i < nodeList.size(); i++) {
                        Node node = nodeList.getNode(i);            
                        // If node contains zero states, remove it from the network.
                        if (node.getNumStates() == 0) {
                            node.delete();
                        }
                    }
        
                    nodeList = net.getNodes();
                    int numVars = nodeList.size();

                    // store type of each node.
                    Type.Symbolic[] typeArray = new Type.Symbolic[numVars];

                    // store parameters of each node
                    Value.Structured[] bNetParamArray = 
                        new Value.Structured[numVars];

                    // store list of all names.
                    String[] nameArray = new String[numVars];

                    // for each node, create CPT & Type
                    for ( int i = 0; i < numVars; i++ ) {
                        Node node = nodeList.getNode(i);
                        NodeList parents = node.getParents();
                        int arity = node.getNumStates();
                        Value.Str name = new Value.Str( node.getName() );
                        nameArray[i] = node.getName();

                        // create the appropriate type for this variable.
                        String[] stateName = new String[arity];
                        for ( int j = 0; j < stateName.length; j++ ) {
                            stateName[j] = node.state(j).getName();
                        }
                        typeArray[i] = new Type.Symbolic(false,false,false,false,stateName);


                        // set up to find CPT
                        int[] parentArity = new int[parents.size()];
                        int[] parentLWB = new int[parents.size()];
                        int[] parentUPB = new int[parents.size()];
                        int[] parentState = new int[parents.size()];
                        // store index into nodeList of parents
                        int[] parentIndex = new int[parents.size()]; 
                        int parentCombinations = 1;

                        for ( int j = 0; j < parentState.length; j++ ) {            
                            // number of states variable may take.
                            parentArity[j] = parents.getNode(j).getNumStates();

                            // set lower and upper bounds to {0,arity-1}
                            parentLWB[j] = 0; 
                            parentUPB[j] = parentArity[j] - 1;
                            parentCombinations *= parentArity[j];

                            // initialise the current parent state to {0,0,...0}
                            parentState[j] = 0;

                            Node parentJ = parents.getNode(j);
                            for ( int k = 0; k < numVars; k++ ) {
                                if ( nodeList.getNode(k) == parentJ ) {
                                    parentIndex[j] = k;
                                    break;
                                }
                                // this should never happen.
                                if ( k == numVars - 1 ) {
                                    throw new RuntimeException("Parent not found : " + parentJ);
                                }
                            }
                        }


                        // create CPT model
                        //Multinomial multinomialModel = new Multinomial(0,arity-1);
                        Value.Model multinomialModel = 
                            MultinomialLearner.getMultinomialModel(0,arity-1);
                        CPT cpt = new CPT( multinomialModel, parentLWB, parentUPB );
                        Value.Vector parentVec =
                            new VectorFN.FastDiscreteVector( parentIndex );

            

                        // create CPT parameters.
                        double[][] paramArray = new double[parentCombinations][];
                        for ( int j = 0; j < parentCombinations; j++ ) {
                            paramArray[j] = Util.toDoubles( node.getCPTable( parentState, null ) );
                            // BNet.incrementBitfield( parentState, parentArity );
            
                            // ordering in CPTs is backwards...
                            BNet.reverseIncrementBitfield( parentState, parentArity );
                        }
            
                        // create params for CPT
                        Value.Vector cptParams = cpt.makeCPTParams( paramArray );

                        Value.Structured modelParamStruct = 
                            new Value.DefStructured( new Value[] {cpt,cptParams} );

                        bNetParamArray[i] =
                            new Value.DefStructured(new Value[]
                                { name,
                                  parentVec, 
                                  modelParamStruct } );
                    }
        
                    Value.Vector paramVec = new VectorFN.FatVector( bNetParamArray );
                    BNet bNet = new BNetNetica( new Type.Structured(typeArray,nameArray) );



                    //        env.finalize();
        
                    return new Value.DefStructured( new Value[] {bNet,paramVec} );

                } catch( NeticaException e ) {
                    throw new RuntimeException(e);
                } }

        }

    }


    /** static instance of reorderNet. */
    public static ReorderNet reorderNet = new ReorderNet();

    /**
     * Netica has a bad habit of putting variables in a different order than you would like.
     * ReorderNet takes a list of names and a (model,params) struct and returns a (model,params)
     * struct with variables in the correct order.
     */
    public static class ReorderNet extends Value.Function
    {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 2238869347640096380L;

        static final Type.Structured myType = 
            new Type.Structured( new Type[] {Type.MODEL, Type.VECTOR});
    
        static final Type.Function tt = 
            new Type.Function(new Type.Structured(new Type[] 
                {new Type.Vector(Type.STRING),myType}), myType );
    
        public ReorderNet() { super(tt); }

        /** Reorder nodes in network to order in newOrderNames.    myStruct = (model,params) */
        public static Value.Structured
            _apply( String[] newOrderNames, Value.Structured myStruct )
        {
            synchronized ( Netica.env ) {
                // deconstruct v into relevent components.
                Value.Model model = (Value.Model)myStruct.cmpnt(0);
                Value.Vector params = (Value.Vector)myStruct.cmpnt(1);
                Type.Structured dataType = (Type.Structured)((Type.Model)model.t).dataSpace;


                // extract names from old and new ordering.
                String[] oldOrderNames = new String[ params.length() ];
                for ( int i = 0; i < newOrderNames.length; i++ ) {
                    oldOrderNames[i] =
                        ((Value.Str)((Value.Structured)params.elt(i)).cmpnt(0)).getString();
                }
        

                // Find the mapping from old to new names.
                int[] newOrder = new int[ params.length() ];
                int[] oldOrder = new int[ params.length() ];
                boolean[] used = new boolean[ newOrder.length ];
                for ( int i = 0; i < newOrderNames.length; i++ ) {
                    boolean found = false;
                    for ( int j = 0; j < oldOrderNames.length; j++ ) {            
                        if ( newOrderNames[i].equals( oldOrderNames[j] ) ) {
                            if ( used[i] == true ) {
                                throw new RuntimeException("Attemtpting to use name twice.");
                            }
                            used[i] = true;
                            found = true;
                            newOrder[i] = j;
                            oldOrder[j] = i;
                            //System.out.println("NewOrder["+i+"] -> OldOrder["+j+"]");
                        }
                    }
                    if ( found == false ) {
                        throw new RuntimeException("Can't find match for " + newOrderNames[i]);
                    }
                }



                // Allocate space to store new parameter and type info
                Value.Structured[] paramArray = 
                    new Value.Structured[ params.length() ];
                Type[] typeArray = new Type[ params.length() ];


                // loop through and rearrange type and param arrays.
                for ( int i = 0; i < paramArray.length; i++ ) {
                    typeArray[i] = dataType.cmpnts[ newOrder[i] ];
                    paramArray[i] = (Value.Structured)params.elt(newOrder[i]);

                    // we also hava to reparent paramArray to reflect changes.
                    Value.Vector parents = (Value.Vector)paramArray[i].cmpnt(1);
                    int[] newParent = new int[parents.length()];
                    for ( int j = 0; j < parents.length(); j++ ) {
                        newParent[j] = oldOrder[ parents.intAt(j) ];
                    }

                    Value.Vector newParents = new VectorFN.FastDiscreteVector(newParent);
                    paramArray[i] = new Value.DefStructured( new Value[]
                        { paramArray[i].cmpnt(0),
                          newParents,
                          paramArray[i].cmpnt(2) } );
        
                }

                Value.Vector newParamVec = new VectorFN.FatVector( paramArray );
                BNet bNet = new BNetNetica( new Type.Structured(typeArray,newOrderNames) );

        
                return new Value.DefStructured(new Value[] 
                    { bNet, newParamVec } );
            }
        }
    
        /** Reorder in Bayes Net. ([Str],(Model,params)) -> (Model,params) */
        public Value apply( Value v )
        {
            // deconstruct v into relevent components.
            Value.Structured vStruct = (Value.Structured)v;
            Value.Vector stringVec = (Value.Vector)vStruct.cmpnt(0);
            // (model,param) struct
            Value.Structured myStruct = (Value.Structured)vStruct.cmpnt(1);
            Value.Vector params = (Value.Vector)myStruct.cmpnt(1);

            String[] name = new String[ params.length() ];
            for ( int i = 0; i < name.length; i++ ) {
                name[i] = ((Value.Str)stringVec.elt(i)).getString();
            }

            return _apply( name, myStruct );
        }
    }






    /** static instance of convertToNet. */
    public static ConvertToNeticaNet convertToNeticaNet = new ConvertToNeticaNet();

    /**
     * ConvertToNeticaNet takes a BNet and returns a BNetNetica
     * Useful for converting from BNetStochastic to BNetNetica.
     */
    public static class ConvertToNeticaNet extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -6203242109043154964L;
        
        static final Type.Function tt = new Type.Function( Type.MODEL, Type.MODEL );
        public ConvertToNeticaNet() { super(tt); }

        /** ConvertToNetica in Bayes Net. ([Str],(Model,params)) -> (Model,params) */
        public Value apply( Value v )
        {
            BNet oldNet = (BNet)v;
            Type.Structured dataType = (Type.Structured)((Type.Model)oldNet.t).dataSpace;
            return new BNetNetica( dataType );
        }
    }

    
    public static SaveNet saveNet = new SaveNet();
    /** 
     * Save network to a file.  Returns a string representation of the network.
     * (filename, model, params) -> String <br>*/
    public static class SaveNet extends Value.Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7030134785705863399L;
        
        /** Static type (model,[]) -> String */
        static Type.Function tt =             
            new Type.Function(
                              new Type.Structured(new Type[] {Type.STRING,Type.MODEL,Type.VECTOR}),
                              Type.STRING
                              );
        
        /**     */
        public SaveNet() { super(tt); }

        /** Write netica net fo file and return a
         * Value.String representation of (bNet,params)*/
        public Value apply(Value v) {
            
            Value.Structured struct = (Value.Structured)v;
            String fName = ((Value.Str)struct.cmpnt(0)).getString();
            BNet bNet = (BNet)struct.cmpnt(1);
            Value.Vector params = (Value.Vector)struct.cmpnt(2);
            
            return _apply(fName, bNet, params);
        }

        /** Write netica net fo file and return a
         * Value.String representation of (bNet,params)*/
        public static Value
            _apply(String fName, BNet bNet, Value.Vector params) {
                        
            String netString = bNet.exportNetica("cammlNet",params);
            
            try {
                FileWriter out = new FileWriter(fName);
                out.write(netString);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return new Value.Str( netString );
        }

    }
    
    
    /** BNetClassify return the classification probabilities for a
     *  given dataset where the BNet is used as a classifier. */
    public static class BNetClassify extends Value.Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -940885203937446080L;
        
        final static Type.Function tt =
            new Type.Function(new Type.Structured( new Type[] 
                {Type.MODEL,Type.VECTOR, Type.VECTOR} ), Type.VECTOR);
        
        public BNetClassify() {
            super(tt);
        }

        /** Take a parameterized BNet and a dataset. For each element in the
         *  dataset return the logP of each value given all other values. */
        Value.Vector
            _apply( BNet bNet, Value.Vector params, Value.Vector data) {
            // Ensure bn is a BNetNetica so efficient inference can be used.
            final BNetNetica bn;
            if ( bNet instanceof BNetNetica) { bn = (BNetNetica)bNet;}
            else { bn = new BNetNetica( bNet.getDataType() ); }
            
            int numVars = params.length();
            Value.Vector vecArray[] = new Value.Vector[numVars];
            for ( int i = 0; i < numVars; i++) {
                double[] logPArray = bn.probClassify( params, data, i);
                vecArray[i] = new VectorFN.FastContinuousVector(logPArray);
            }
            String[] names = bn.makeNameList(params);
            
            return new VectorFN.MultiCol( new Value.DefStructured(vecArray,names) );
        }
        
        public Value apply(Value v) {
            Value.Structured vStruct = (Value.Structured)v;
            BNet bNet = (BNet)vStruct.cmpnt(0);
            Value.Vector params = (Value.Vector)vStruct.cmpnt(1);
            Value.Vector data = (Value.Vector)vStruct.cmpnt(2);
            return _apply(bNet,params,data);
        }
        
    }

    public static BNetClassify bNetClassify = new BNetClassify();

    
    /** Classify returns the highest probability value for a given row. */
    public static class Classify extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -1229040283526770891L;
        
        final static Type.Function tt =
            new Type.Function(new Type.Structured( new Type[] 
                {Type.MODEL,Type.VECTOR, Type.VECTOR, Type.DISCRETE} ), Type.VECTOR);
        
        public Classify() {
            super(tt);
        }

        /** Classify values for a single BN variable. */
        public Value.Vector _apply( BNet bNet, Value.Vector params, Value.Vector data, int var) {
            // Ensure bn is a BNetNetica so efficient inference can be used.
            final BNetNetica bn;
            if ( bNet instanceof BNetNetica) { bn = (BNetNetica)bNet;}
            else { bn = new BNetNetica( bNet.getDataType() ); }
            
            return bn.classify(params, data, var);
        }
        
        public Value apply(Value v) {
            Value.Structured vStruct = (Value.Structured)v;
            BNet bNet = (BNet)vStruct.cmpnt(0);
            Value.Vector params = (Value.Vector)vStruct.cmpnt(1);
            Value.Vector data = (Value.Vector)vStruct.cmpnt(2);
            int var = ((Value.Discrete)vStruct.cmpnt(3)).getDiscrete();
            return _apply(bNet,params,data,var);
        }
        
    }

    public static Classify classify = new Classify();

    /** Returns probabilities for specified variable given other variables.. */
    public static class ClassifyProb extends Value.Function {
    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7641558140712305036L;
        
        final static Type.Function tt =
            new Type.Function(new Type.Structured( new Type[] 
                {Type.MODEL,Type.VECTOR, Type.VECTOR, Type.DISCRETE} ), Type.VECTOR);
        
        public ClassifyProb() {
            super(tt);
        }

        /** Classify values for a single BN variable. */
        public Value.Vector _apply( BNet bNet, Value.Vector params, Value.Vector data, int var) {

            // Ensure bn is a BNetNetica so efficient inference can be used.
            final BNetNetica bn;
            if ( bNet instanceof BNetNetica) { bn = (BNetNetica)bNet;}
            else { bn = new BNetNetica( bNet.getDataType() ); }

            double classProbs[][] = bn.getClassProbs( params, data, var);
            Value.Vector vecs[] = new Value.Vector[classProbs.length];
            for (int i = 0; i < vecs.length; i++) {
                vecs[i] = new VectorFN.FastContinuousVector( classProbs[i] );
            }
            
            return new VectorFN.MultiCol( new Value.DefStructured(vecs) );
        }
        
        public Value apply(Value v) {
            Value.Structured vStruct = (Value.Structured)v;
            BNet bNet = (BNet)vStruct.cmpnt(0);
            Value.Vector params = (Value.Vector)vStruct.cmpnt(1);
            Value.Vector data = (Value.Vector)vStruct.cmpnt(2);
            int var = ((Value.Discrete)vStruct.cmpnt(3)).getDiscrete();
            return _apply(bNet,params,data,var);
        }
        
    }

    public static ClassifyProb classifyProb = new ClassifyProb();

}
