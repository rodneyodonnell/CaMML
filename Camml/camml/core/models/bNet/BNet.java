//
// Bayesian Network model class
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: BNet.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.bNet;

import java.util.ArrayList;
import java.util.Arrays;

import cdms.core.*;
import camml.core.library.ArrayIndexedHashTable;
import camml.core.library.SelectedStructure;
import camml.core.library.StructureFN;
import camml.core.models.ModelLearner.GetNumParams;
import camml.plugin.augment.AugmentFN;
import camml.plugin.augment.AugmentFN2;
import camml.plugin.augment.AugmentFN3;
import camml.plugin.netica.NeticaFn;
import camml.plugin.tomCoster.ExpertElicitedTOMCoster;

/**
 *  Bayesian Network model. <br>
 *
 *  A BNet model is a CDMS implementation of a Bayesian Network. <br>
 *  
 *  Unlike many other models, the input and output of a Bayesian Net are not strictly defined and
 *  a variable which was once viewed as an input may later be viewed as an output.  To cope with
 *  this all variables are included as both inputs and outputs of the network. <br>
 *
 *  To "query" a variable it's value should be set to missing in input, and specified during 
 *  output. <br>
 *
 *  Specifying Value.TRIV as input is a shortcut method to specify all values missing. <br>
 *
 *  Both discrete and continuous variables (or any other sort) may be used so long as the models
 *  and parameters which encode the local structure will accept these data types. <br>
 *
 *  The sufficient statistics of a model is simply taken as (X,Z).  X and Z will often be similar
 *  but for function such as logP() we need two seperate values (one with some values missing, one
 *  perhaps with those values included.)
 * 
 *  The parameterisation of the network is done as a vector of local structures.  Each local 
 *  structure consists of a vector of parents and a (model,params). <br>
 *
 *  No inference is performed in this abstract class.  This is left to subclasses.
 *
 */
public abstract class BNet extends Value.Model
    implements GetNumParams
{        
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -5463896129693625993L;

    /** Type of data network is based on. */
    protected final Type.Structured dataType;
    
    public Type.Structured getDataType() {return dataType;}
    
    /**
     *  Simple constructor.  Model type = { 
     *  X = Type.VECTOR,
     *  Z = Type.VECTOR, 
     *  Y = [ ( name, [parents], (model,params) ) ] 
     *  S = (Type.VECTOR, Type.VECTOR) }
     */
    public BNet( Type.Structured dataType )
    {
        super( new Type.Model( dataType,   // x (data space)
                               new Type.Vector(new Type.Structured(new Type[] { // y (parameters)
                                           new Type.Str(),                              // variable name
                                           new Type.Vector(Type.DISCRETE),              // global structure (parents)
                                           new Type.Structured( new Type[] {Type.MODEL, Type.TYPE} )  // (m,y) pair
                                       } ) ),
                               dataType,   // z (shared space/input)
                               // s (sufficient stats)
                               new Type.Structured( new Type[] {dataType, dataType} )
                               ) );
        this.dataType = dataType;
    }
    
    
    /** logP(X|Y,Z) where v = (X,Y,Z) */
    public double logP(Value.Structured v)
    {
        return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }
    
    /**
     * logP which accepts a vector instead of individual elements. <br>
     * If z is a vector of TRIV then noInputLogP is used
     * else logP(Value,Value,Value) is called repeatedly.
     */
    public double logP( Value.Vector x, Value y, Value.Vector z ) {
        
        // Extract information from parameter list.
        //String[] name = makeNameList( (Value.Vector)y );
        int[][] parentList = makeParentList( (Value.Vector)y );
        int[] order = makeConsistantOrdering( parentList );
        Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
        Value[] subModelParamList = makeSubParamList( (Value.Vector)y );
        
        // If all input values are Value.TRIV (ie. inputs missing) then we can run
        // noInputLogPVec which is much faster.
        boolean allMissing = true;
        for ( int i = 0; i < z.length(); i++ ) {
            if ( z.elt(i) != Value.TRIV ) {
                allMissing = false;
            }
        }
        
        double totalLogP = 0;
        
        if ( allMissing == false ) {
            for ( int i = 0; i < x.length(); i++ ) {
                if ( z.elt(i) == Value.TRIV ) {
                    totalLogP += noInputLogP( (Value.Structured)x.elt(i), 
                                              subModelList, subModelParamList, order, parentList );
                }
                else {
                    totalLogP += logP(x.elt(i), y, z.elt(i) );
                }
            }
        }
        else {
            totalLogP = noInputLogPVec( x, subModelList, subModelParamList, order, parentList );
        }
        
        return totalLogP;
        
    }
    
    
    /**
     *  Predict returns a vector of predictions given various initial networks.  The highest
     *   likelyhood instantiation of the network is difficult to find so a sub-optimal instantiation
     *   is returned.  This is generated by starting at the root nodes and working through the
     *   network to the leaf nodes generating the best value for each state.
     */
    public Value.Vector predict(Value y, Value.Vector z) 
    {
        Value.Structured[] data = new Value.Structured[z.length()];
        for ( int i = 0; i < data.length; i++ ) {
            data[i] = (Value.Structured)predict( y, z.elt(i) );
        }
        return new VectorFN.FatVector( data );
    }
        
    /** create a single empty input value with appropriate type information. */
    public Value.Structured makeUnobservedInputStruct()
    {
        Type.Model modelType = (Type.Model)t;
        Type.Structured inputType = (Type.Structured)modelType.dataSpace;
        String[] name = inputType.labels;
        
        Value[] unobservedValArray = new Value[ name.length ];
        for ( int i = 0; i < unobservedValArray.length; i++ ) {        
            if ( inputType.cmpnts[i] instanceof Type.Discrete ) {
                unobservedValArray[i] = new Value.Discrete((Type.Discrete)inputType.cmpnts[i],
                                                           Value.S_UNOBSERVED,0 );
            }
            else if ( inputType.cmpnts[i] instanceof Type.Continuous ) {
                unobservedValArray[i] = 
                    new Value.Continuous((Type.Continuous)inputType.cmpnts[i],
                                         Value.S_UNOBSERVED,0 );
            }
            else if ( inputType.cmpnts[i] instanceof Type.Structured ) {
                unobservedValArray[i] = 
                    new Value.DefStructured((Type.Structured)inputType.cmpnts[i],new Value[0]) {
                        private static final long serialVersionUID = 5222888080799639973L;
                        public ValueStatus status() {
                            return Value.S_UNOBSERVED;
                        }
                    };
            }
            else { throw new RuntimeException("Unobserved input not created : "+
                                              inputType.cmpnts[i]); }
        }
        
        return new Value.DefStructured( inputType, unobservedValArray );        
    }
    
    
    /**
     *  The sufficient statistics of a CPT is a list of sufficient stats of its elements.
     *  Each paremt combination maps to a given entry in the sufficient vector.  This mapping is
     *  defined by CPT.decodeParents()
     */
    public Value getSufficient(Value.Vector x, Value.Vector z)
    {
        return new Value.DefStructured( new Value[] {x,z},
                                        new String[] {"x", "z"} );
    }
    
    
    /** Take BNet parameters and return a list of names. */
    public String[] makeNameList( Value.Vector params )
    {
        int numVars = params.length();
        String name[] = new String[ numVars ];
        
        for ( int i = 0; i < name.length; i++ ) {
            Value.Structured subStructure = (Value.Structured)params.elt(i);
            name[i] = ((Value.Str)subStructure.cmpnt(0)).getString();
        }
        
        return name;
    }
    
    /**
     * Return a list of all state names for this BNet.
     * State names for non-scalar entries are set to null
     */
    public String[][] makeStateNameList( )
    {
        Type[] type = ((Type.Structured)((Type.Model)t).dataSpace).cmpnts;
        String name[][] = new String[ type.length ][];
        
        for ( int i = 0; i < name.length; i++ ) {   
            
            // if type[i] is a Type.Symbolic then we use proper names.
            if ( Type.SYMBOLIC.hasMember( type[i] ) ) {
                name[i] = (String[])((Type.Symbolic)type[i]).ids.clone();
            } 
            else if ( Type.DISCRETE.hasMember( type[i] ) ) {
                Type.Discrete dType = (Type.Discrete)type[i];
                if ( !Double.isInfinite(dType.LWB) && !Double.isInfinite(dType.LWB) ) {
                    String[] nameArray = new String[(int)(dType.UPB - dType.LWB) + 1];
                    for ( int j = 0; j < nameArray.length; j++ ) {
                        nameArray[j] = String.valueOf( (int)(dType.LWB + j) );
                    }
                    name[i] = nameArray;
                }
                else {
                    System.err.println("Discrete variables with infinite domains cannot be named.");
                    name[i] = null;
                }
            }
            else { 
                name[i] = null;
                System.err.println("State name does not exist for non-discrete variable");
            }
        }
        
        return name;
    }
    
    /**
     * returns an array containing the arity of each variable in the network.
     * Arity of non-symbolic variables are returned as -1;
     */
    public int[] getArity() 
    {
        Type[] type = ((Type.Structured)((Type.Model)t).dataSpace).cmpnts;
        int[] arity = new int[ type.length ];
        
        for ( int i = 0; i < arity.length; i++ ) {   
            
            // if type[i] is a Type.Symbolic then we use proper names.
            if ( Type.SYMBOLIC.hasMember( type[i] ) ) {
                arity[i] = ((String[])((Type.Symbolic)type[i]).ids).length;
            } 
            else if ( Type.DISCRETE.hasMember( type[i] ) ) {
                Type.Discrete dType = (Type.Discrete)type[i];
                if ( !Double.isInfinite(dType.LWB) && !Double.isInfinite(dType.LWB) ) {
                    arity[i] = (int)(dType.UPB - dType.LWB) + 1;
                }
                else {
                    System.err.println("Discrete variables with infinite domains have no arity");
                    arity[i] = -1;
                }
                
            }
            else {
                System.err.println("Warning: Arity cannot be found for non-discrete variable");
                arity[i] = -1;
            }
        }
        
        return arity;
    }
    
    /** Take BNet parameters and return an array of the parents of each variable. */
    public int[][] makeParentList( Value.Vector params )
    {
        int numVars = params.length();
        
        int[][] parent = new int[numVars][];
        for ( int i = 0; i < parent.length; i++ ) {
            Value.Structured subStructure = (Value.Structured)params.elt(i);
            Value.Vector subParent = (Value.Vector)subStructure.cmpnt( 1 );
            parent[i] = new int[ subParent.length() ];
            for (int j = 0; j < parent[i].length; j++) {
                parent[i][j] = subParent.intAt( j );
            }
        }
        
        return parent;
    }
    
    /** 
     * Perform a topological sort to find an ordering consistant with the parent relationships 
     * given
     */
    public int[] makeConsistantOrdering( int[][] parents )
    {
        int[] order = new int[ parents.length ];
        boolean[] inserted = new boolean[ parents.length ];
        //int current = 0;
        
        // Inisially no variables have been inserted.
        for ( int i = 0; i < parents.length; i++ ) {
            inserted[i] = false;
        }
        
        // i = position to be inserted into
        // j = current variable being checked for insertability
        // k = current parent of j beging checked 
        for ( int i = 0; i < parents.length; i++ ) {
            for ( int j = 0; j < parents.length; j++ ) {
                // is it possible to insert the current variable.        
                boolean possible = !inserted[j];  
                
                for ( int k = 0; k < parents[j].length; k++ ) {
                    if ( inserted[parents[j][k]] != true ) {
                        possible = false;
                        break;
                    }
                }
                if ( possible == true ) { // we are able to insert this variable.
                    inserted[j] = true;
                    order[i] = j;
                    break;
                }
                if ( j == parents.length - 1 ) {
                    throw new RuntimeException("Graph has cycles.");
                } 
            }
            
        }
        return order;
    } 
    
    /** Make a list of submodels from params. */
    protected Value.Model[] makeSubModelList( Value.Vector params )
    {
        Value.Model[] model = new Value.Model[ params.length() ];
        
        for ( int i = 0 ; i < model.length; i++ ) {
            Value.Structured temp = (Value.Structured)params.elt(i);
            model[i] = (Value.Model)((Value.Structured)(temp.cmpnt(2))).cmpnt(0);
        }
        return model;
    }
    
    /** Make a list of submodel parameters from original parameters */
    protected Value[] makeSubParamList( Value.Vector params )
    {
        Value[] value = new Value[ params.length() ];
        
        for ( int i = 0 ; i < value.length; i++ ) {
            Value.Structured temp = (Value.Structured)params.elt(i);
            value[i] = ((Value.Structured)(temp.cmpnt(2))).cmpnt(1);
        }
        return value;
    }
    
    
    
    
    /** Fast computation of logP on a vector of outputs given that all inputs are missing. */
    public double noInputLogPVec( Value.Vector x, Value.Model[] subModel, Value[] subParam,
                                  int[] order, int[][] parentList ) {
        double total = 0;
        
        for ( int i = 0; i < order.length; i++ ) {
            int current = order[i];
            
            // create parent data.
            Value.Vector parentData = 
                new camml.core.library.SelectedVector(x,null,parentList[current]);
            
            
            Value.Vector input = parentData;
            Value.Vector output = x.cmpnt(current); 
            
            total += subModel[current].logP( output, subParam[current], input );
        }
        
        return total;
    }
    
    
    /** return logP for a case where input is entirely unobserved */
    public double noInputLogP( Value.Structured x, 
                               Value.Model[] subModel, 
                               Value[] subParam,
                               int[] order,
                               int[][] parentList )
    {
        double totalLogP = 0;
        
        // Loop through each variable in topological order.
        for ( int i = 0; i < order.length; i++ ) {
            int current = order[i];
            
            // create parent data.
            Value[] parentArray = new Value[ parentList[current].length ];
            for ( int j = 0; j < parentArray.length; j++ ) {
                parentArray[j] = x.cmpnt( parentList[current][j] );
            }
            Value.Structured parentData = new Value.DefStructured(parentArray);
            
            
            
            Value input = parentData;
            Value output = x.cmpnt(current); 
            
            if ( output.status().equals( Value.S_PROPER ) ){
                totalLogP += subModel[current].logP( output, subParam[current], input );
            }
            else {
                throw new RuntimeException("cannot deal with status: " + output.status() );
            }
            
        }        
        
        return totalLogP;
    }
    
    
    /**
     * IncrementBitfield treats bitfield as a multiple base number and each increment 
     * increases the rightmost (ie length-1) digit. If this digit reaches max the increment is
     * passed on to the next digit and the original bit is set to 0.  If max were set to {1,1,1..,1}
     * this function would emulate a binary counter. <br>
     *
     * bitfield is modified by this function <br>
     * max is not modified by this function <br>
     */
    public static void incrementBitfield( int[] bitfield, final int[] max )
    {
        for ( int digit = max.length - 1; digit >= 0; digit-- ) {
            if (max[digit] != 0) {
                bitfield[digit] ++;
            
                if ( bitfield[digit] != max[digit] ) { // do we have to roll on to the next digit?
                    break;
                }
                else {
                    bitfield[digit] = 0;
                }
            }
        }
    }
    
    
    /**
     * reverseIncrementBitfield is the same as incrementBitfield but reversing the order of 
     * significance.
     */
    public static void reverseIncrementBitfield( int[] bitfield, final int[] max )
    {
        for ( int digit = 0; digit < max.length; digit ++ ) {
            if (max[digit] != 0) {
                bitfield[digit] ++;
            
                if ( bitfield[digit] != max[digit] ) { // do we have to roll on to the next digit?
                    break;
                }
                else {
                    bitfield[digit] = 0;
                }
            }
        }
    }
    
    
    public String toString() {
        return "BNet  Model";
    }
    
    public int getNumParams( Value params ) {
        int total = 0;
        Value.Vector paramVec = (Value.Vector)params;
        for ( int i = 0; i < paramVec.length(); i++ ) {
            Value.Structured elt = (Value.Structured)((Value.Structured)paramVec.elt(i)).cmpnt(2);
            try {
                Value.Model subModel = (Value.Model)elt.cmpnt(0);
                Value subParams = elt.cmpnt(1);
                
                if ( subModel instanceof GetNumParams ) {
                    total += ((GetNumParams)subModel).getNumParams( subParams );
                }
                else { throw new RuntimeException("model : " + subModel + 
                                                  " has not implemented ModelGlue.GetNumParams"); 
                }
            } catch ( ClassCastException e ) {
                System.out.println("elt = " + elt);
                throw(e);    
            }
        }
        return total;
    }
    
    
    /** return a human readable string showing the links in the network */
    public String makeString( Value.Vector params )
    {
        // set the length of each name to 15
        String[] fullName = ((Type.Structured)((Type.Model)t).dataSpace).labels;
        
        if ( fullName == null ) {
            System.out.println( "BNet = " + this );
            System.out.println( "BNet.t = " + this.t );
            
            throw new NullPointerException();
        }
        
        int max = 0;
        for ( int i = 0; i < fullName.length; i++ ) {
            if ( max < fullName[i].length() ) {
                max = fullName[i].length();
            }
        }
        String[] name = new String[fullName.length];
        for ( int i = 0; i < name.length; i++ ) {
            name[i] = setLength( fullName[i], max );
        }
        
        StringBuffer s = new StringBuffer();
        for ( int i = 0; i < params.length(); i++ ) {
            Value.Structured elt = (Value.Structured)params.elt(i);
            s.append( name[i] + " <-" );
            Value.Vector parents = (Value.Vector)elt.cmpnt(1);
            for ( int j = 0; j < parents.length(); j++ ) {
                s.append( "  " + name[parents.intAt(j)] );
            }
            s.append("\n");
            
        }
        return s.toString();
    }
    
    /** Convenience function for maskMissing */
    public static Value.Structured maskMissing( Value.Structured in, boolean missing[] ) {
        return maskStatus(in,missing, Value.S_UNOBSERVED);
    }
        
    /** Return a Value.Structured identical to in but with specified values missing. */
    public static Value.Structured maskStatus( Value.Structured in, boolean missing[], ValueStatus status ) {
        if ( in.length() != missing.length ) { 
            throw new RuntimeException("Length mismatch in maskMissing");
        }
        Value[] array = new Value[missing.length];
        for ( int i = 0; i < array.length; i++ ) {
            if ( missing[i] ) {
                if ( in.cmpnt(i) instanceof Value.Discrete ) {
                    array[i] = new Value.Discrete((Type.Discrete)in.cmpnt(i).t, status, in.intCmpnt(i));
                }
                else if ( in.cmpnt(i) instanceof Value.Continuous ) {
                    array[i] = new Value.Continuous( (Type.Continuous)in.cmpnt(i).t, status, in.doubleCmpnt(i) );
                }
                else {
                    throw new RuntimeException( "Type not handled in maskMissing : " + 
                                                in.cmpnt(i).getClass() );
                }
                
            }
            else {
                array[i] = in.cmpnt(i);
            }
        }
        return new Value.DefStructured( (Type.Structured)in.t, array );
    }
    
    /** Convenience function for maskMissing */
    public static Value.Vector maskMissing( Value.Vector in, boolean missing[] ) {
        return maskStatus( in, missing, Value.S_UNOBSERVED);
    }
    
    /** Return a Value.Vector identical to in but with specified values missing. */
    public static Value.Vector maskStatus( Value.Vector in, boolean missing[], ValueStatus status ) {
        Type.Structured inType = (Type.Structured)((Type.Vector)in.t).elt;
        
        if ( inType.cmpnts.length != missing.length ) { 
            throw new RuntimeException("Length mismatch in maskMissing");
        }
        Value.Vector[] array = new Value.Vector[missing.length];
        
        for ( int i = 0; i < array.length; i++ ) {
            if ( missing[i] ) {
                if ( inType.cmpnts[i] instanceof Type.Discrete ) {            
                    array[i] = new VectorFN.UniformVector( in.length(),    new Value.Discrete((Type.Discrete)inType.cmpnts[i], status, 0) );
                }
                else if ( inType.cmpnts[i] instanceof Type.Continuous ) {
                    array[i] = new VectorFN.UniformVector( in.length(), new Value.Continuous( (Type.Continuous)inType.cmpnts[i], status, 0 ) );
                }
                else {
                    throw new RuntimeException( "Type not handled in maskMissing : " + 
                                                in.cmpnt(i).getClass() );
                }
                
            }
            else {
                array[i] = in.cmpnt(i);
            }
        }
        
        Type.Vector[] vecTypeArray = new Type.Vector[array.length];
        for ( int i = 0; i < vecTypeArray.length; i++ ) {
            vecTypeArray[i] = new Type.Vector( inType.cmpnts[i] );
        }
        
        return new VectorFN.MultiCol( new Value.DefStructured( new Type.Structured( vecTypeArray ), array ) );
    }
    
    /** Return a Value.Structured containing all missing values and of the correct Type */
    public Value.Structured makeMissingStruct( ) {
        Type.Structured xType = (Type.Structured)((Type.Model)t).dataSpace;
        int numVars = xType.cmpnts.length;
        
        Value[] array = new Value[numVars];
        for ( int i = 0; i < array.length; i++ ) {
            if ( xType.cmpnts[i] instanceof Type.Discrete ) {
                array[i] = new Value.Discrete((Type.Discrete)xType.cmpnts[i], 
                                              Value.S_UNOBSERVED, 0);
            }
            else if ( xType.cmpnts[i] instanceof Type.Continuous ) {
                array[i] = new Value.Continuous( (Type.Continuous)xType.cmpnts[i], 
                                                 Value.S_UNOBSERVED, 0 );
            }
            else { throw new RuntimeException("Type not handled in makeMissing"); }
        }
        return new Value.DefStructured( xType, array );
    }
    
    /** clip or pad out a string to the required length. */
    protected String setLength( String s, int len ) {    
        if ( s.length() > len ) {
            return s.substring(0,len);
        }
        else {
            StringBuffer buf = new StringBuffer( s );
            while ( buf.length() < len ) {
                buf.append(" ");
            }
            return buf.toString();
        }
    }
    
    
    /** Static instance of maskMissing */
    public static final MaskMissing maskMissingFN = new MaskMissing();
    
    /**
     * Pass a structure or vector and a structure of boolean to maskMissing to return an
     * identical value with appropriate values masked as being missing.
     */
    public static class MaskMissing extends Value.Function {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3010834328010575554L;

        MaskMissing(){ super(new Type.Function(Type.STRUCTURED, Type.STRUCTURED) ); }
        
        public Value apply( Value v ) {
            Value.Structured vStruct = (Value.Structured)v;
            
            
            Type.Structured sType;
            if ( vStruct.cmpnt(0).t instanceof Type.Vector ) {
                sType = (Type.Structured)((Type.Vector)vStruct.cmpnt(0).t).elt;
            }
            else if ( vStruct.cmpnt(0).t instanceof Type.Structured ) {
                sType = (Type.Structured)vStruct.cmpnt(0).t;
            }
            else { throw new RuntimeException("Unknown type in maskMissing");}
            
            
            boolean[] flag = new boolean[ sType.cmpnts.length ];
            
            if ( vStruct.cmpnt(1) instanceof Value.Vector ) {
                Value.Vector boolVec = (Value.Vector)vStruct.cmpnt(1);
                for ( int i = 0; i < boolVec.length(); i++ ) {
                    flag[ boolVec.intAt(i) ] = true;
                }
            }
            else if ( vStruct.cmpnt(1) instanceof Value.Structured ) {
                Value.Structured boolStruct = (Value.Structured)vStruct.cmpnt(1);
                for ( int i = 0; i < boolStruct.length(); i++ ) {
                    flag[ i ] = (boolStruct.intCmpnt(i)==0);
                }
            }
            else { throw new RuntimeException("Unknown type in maskMissing");}
            
            
            
            
            if ( vStruct.cmpnt(0) instanceof Value.Vector ) {
                return maskMissing( (Value.Vector)vStruct.cmpnt(0), flag );
            }
            else if ( vStruct.cmpnt(0) instanceof Value.Structured ) {
                return maskMissing( (Value.Structured)vStruct.cmpnt(0), flag );
            }
            else { throw new RuntimeException("Unknown type in maskMissing"); }
        }
        
    }
    
    /** Export parameterised network in specified format 
     *  currently "netica" is only valid format. */
    public String export( String netName, Value.Vector params, String format ) {
        final String netString;
        if ( format.equals("netica") ) {
            netString = exportNetica( netName, params );
        }
        else { throw new RuntimeException("Unsupported export format : " + format ); }
        return netString;
    }
    
    /** Return a parameterised network in netica format. */
    public String exportNetica( String netName, Value.Vector params ) {
        Type.Model mType = (Type.Model)this.t;
        Type.Structured dataSpace = (Type.Structured)mType.dataSpace;

        StringBuffer s = new StringBuffer();
        
        // TODO fix VERSION_NUMBER
        double VERSION_NUMBER = 1.0;

        // TODO add numCases info;
        
        // print header information.
        s.append( "// ~->[DNET-1]->~\n" );
        s.append( "// Network : " + netName + "\n" );
        s.append( "// File created by CaMML2, Version : " + VERSION_NUMBER +"\n\n" );
        
        s.append( "bnet Reconstructed_Learned_Network {\n" );
        s.append( "autoupdate = TRUE;\n" );
        
        int[] arity = getArity();
        String[] name = makeNameList(params); // Create name[] full of valid netica names.
        name = NeticaFn.makeValidNeticaNames( name, false );
        int[][] parent = makeParentList(params);
        Value[] subParam = makeSubParamList(params);
        Value.Model[] subModel = makeSubModelList(params);
        
        // print the individual nodes
        for ( int i=0; i < params.length(); i++ ) {
            s.append( "node " + name[i] + " {\n" );
            s.append( "\tkind = NATURE;\n" );
            s.append( "\tdiscrete = TRUE;\n" );
            s.append( "\tnumstates = " + arity[i] + ";\n" );
            
            if (dataType.cmpnts[i] instanceof Type.Symbolic) {
                Type.Symbolic sType = (Type.Symbolic)dataType.cmpnts[i];                
                String str = Arrays.toString(NeticaFn.makeValidNeticaNames(sType.ids,true));                
                s.append( "\tstates = (" + str.subSequence(1,str.length()-1) + ");\n" );
            }
            
            // print list of parents[i]
            s.append( "\tparents = ( " );  
            for ( int j = 0; j < parent[i].length; j++ ) {
                s.append( name[parent[i][j]] );
                if ( j != parent[i].length-1) { s.append( ", "); }
            } 
            s.append( " );\n" );
            
            // Create Type.Structured for parent type.
            Type[] parentTypeArray = new Type[parent[i].length];
            for ( int j = 0; j < parentTypeArray.length; j ++) {
                parentTypeArray[j] = dataSpace.cmpnts[parent[i][j]];
            }
            Type.Structured parentType = new Type.Structured( parentTypeArray );

            
            // Now many parent combinations exist?
            int parentComs = 1;
            for ( int j = 0; j < parent[i].length; j++ ) { parentComs *= arity[parent[i][j]]; }            
            
            // Initialise bitfield used to loop through all parent combinations.
            int max[] = new int[parent[i].length];
            int index[] = new int[ max.length ];
            int prevIndex[] = new int[ max.length ];    // Previous state of index             
            for ( int j = 0; j < max.length; j++ ) { 
                max[j] = arity[parent[i][j]];
                prevIndex[j] = max[j] - 1;
            }
                        
            // print CPT[i]
            s.append( "\tprobs = \n" );
            for ( int j = 0; j < parentComs; j++ ) { // for each parent combination                
                s.append( "\t\t(");
                for ( int k = parent[i].length-1; k >= 0; k-- ) {
                    if ( (index[k] == 0) && (prevIndex[k] != index[k])) { s.append("("); }
                    else break;
                }
                
                // TODO: set values of prob
                double total = 0;
                double prob[] = new double[arity[i]];
                
                for ( int k = 0; k < prob.length; k++ ) {
                    prob[k] = subModel[i].logP( new Value.Discrete((Type.Discrete)dataSpace.cmpnts[i] ,k), 
                                                subParam[i], new StructureFN.FastDiscreteStructure(parentType,index) );
                    prob[k] = Math.exp( prob[k] );
                    total += prob[k];
                }                
                
                for ( int k = 0; k < arity[i]; k++ ) {
                    s.append( prob[k] );
                    if ( k != arity[i]-1) { s.append(", "); }
                }
                                
                // increment bitfield to next parent combination
                prevIndex = (int[])index.clone();
                incrementBitfield( index, max );
                
                for ( int k = index.length-1; k >= 0; k-- ) {
                    if ( (index[k] == 0) && (index[k] != prevIndex[k])) { s.append( ")" ); }
                    else break;
                }
                s.append( ")");
                if ( j != parentComs-1 ) { s.append(",\n"); }
                else { s.append(";\n"); }
            }
            s.append("\t};\n");
        }
        
        s.append( "};\n" );
        return s.toString();
    }    

    
    private Value.Vector klParams = null;
    private Value.Vector testData = null;
    private Value.Vector trivVec = null;
    private double logP1 = 0.0;
    /** Calculate KL distance stochastically using 10000 samples */
    public double klStochastic( Value.Vector params1, Value.Vector params2 ) {
        if ( klParams != params1 ) {
            //if (klParams != null) {System.out.println("Regenerating test data, etc.");}
            klParams = params1;
            testData = generate(new java.util.Random(123),10000,params1,Value.TRIV);
            trivVec = new VectorFN.UniformVector(testData.length(),Value.TRIV);
            logP1 = logP(testData,params1,trivVec);            
        }
        
        //     Calculate KL distance stochastically
        double logP2 = logP(testData,params2,trivVec);
        double kl = (logP1-logP2)/testData.length();
        return kl;        
    }
    
    /** Calculate exact KL distance by enumerating all states in joint distribution */
    public double klExactSlow( Value.Vector params1, Value.Vector params2 ) {
        int[] arity = getArity();
        int combinations = 1;
        for ( int i = 0; i < arity.length; i++ ) { combinations *= arity[i]; }
                
        int index[] = new int[arity.length];
        double kl = 0;
        // NOTE: this datum "cheats".  CDMS Values are supposed to be immutable
        //       but as datum is based on index when index is updated so is datum.
        Value.Structured datum = new StructureFN.FastDiscreteStructure(dataType,index);        
        for ( int i = 0; i < combinations; i++ ) {
            double logP1 = logP( datum, params1, Value.TRIV );

            // NOTE: We check if there are any probabilities of 0.0 in the logPArray1
            //       but not logPArray2.  A probability of 0.0 in the original table
            //       can be ignored (as it is impossible), but a 0.0 in the second table
            //       gives an infinite KL distance as the model gives no way to encode
            //       an event which is possible.
            //         Moving the logP2=... statement here almost doubles performence when
            //         dealing with augmented networks.
            if ( logP1 != Double.NEGATIVE_INFINITY ) {
                double logP2 = logP( datum, params2, Value.TRIV );
                kl += Math.exp(logP1) * ( logP1 - logP2 );
            }

            incrementBitfield(index,arity);            
        }                
        
        return kl;        
    }
    
    /** Calculate (discrete) probabily distribution over variable n given y and z */
    private double[] subLogP( Value.Vector y, Value.Structured z, int n) {
        Value.Structured subParams = (Value.Structured)y.elt(n);
        Value.Vector parentVec = (Value.Vector)subParams.cmpnt(1);
        Value.Structured subSubParams = (Value.Structured)subParams.cmpnt(2);
        Value.Model subModel = (Value.Model)subSubParams.cmpnt(0);
        Value subModelParams = subSubParams.cmpnt(1);
        int[] parents = new int[parentVec.length()];
        for (int i = 0; i < parents.length; i++) {parents[i] = parentVec.intAt(i);}
        Value.Structured subZ = new SelectedStructure(z,parents);
        Type.Discrete xType = (Type.Discrete)((Type.Structured)z.t).cmpnts[n];
        int lwb = (int)xType.LWB;
        int upb = (int)xType.UPB;
        double logP[] = new double[upb-lwb+1];
        for (int i = lwb; i < upb+1; i++) {
            Value.Discrete x = new Value.Discrete(xType,i);
            logP[i-lwb] = subModel.logP(x,subModelParams,subZ);
        }
        return logP;
    }
    
    /**
     * klSub recursively calculates KL distance between params1 and params2. <br>
     * 
     *  Algorithm: <br>
     *  state[] consists of the current value for each node, or -1 if no value has been set (referred to as 'x' below). <br>
     *  We loop through the total ordering and find the first 'x' value (which may be a single node or a clique). <br>
     *  
     *  KL(A=x,B=x,..Z=x) = sum(A=a, P(A=a)*log(P(A=a)/P`(A=a)) + P(A=a)*KL(A=a,B=x,...Z=x)) <br>
     *  
     *  A cache of partially computed KL values is also kept which greatly improves calculation speed.
     */
    private double klSub(int[][] arcs1, int[][] arcs2, int[][] order, 
                         Value.Vector params1, Value.Vector params2,
                         int state[], int[] arity, ArrayIndexedHashTable hash ) {
        
        // KeyState contains all 'x' elements from state and all parents
        // of an 'x' element.  All other values are replaced by ignore.
        int keyState[] = new int[state.length];
        Arrays.fill(keyState,-2);
        for (int i = 0; i < keyState.length; i++) {
            if (state[i] == -1) {
                keyState[i] = state[i];
                for (int j = 0; j < arcs1[i].length; j++) {
                    keyState[arcs1[i][j]] = state[arcs1[i][j]];
                }
                for (int j = 0; j < arcs2[i].length; j++) {
                    keyState[arcs2[i][j]] = state[arcs2[i][j]];
                }
            }
        }
        
        Double klVal = (Double)hash.get2(keyState);
        if (klVal != null) { 
            return klVal.doubleValue(); 
        }
        
        // Record the position of the first 'x' element, and if more than one 'x' exists.
        int firstX[] = null;
        boolean multipleX = false;
        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < order[i].length; j++) {
                if (firstX == null && state[order[i][j]] == -1) { firstX = order[i]; }
                else if (firstX != null && state[order[i][j]] == -1) { multipleX = true; break;}
            }
        }
        
        if (firstX == null) {firstX = new int[0];}

        // Calculate arity and #combinationf for firstX.
        int[] firstXArity = new int[firstX.length];
        int[] firstXindex = new int[firstX.length];
        int firstXCombinations = 1;
        for (int i = 0; i < firstX.length; i++) { 
            firstXArity[i] = arity[firstX[i]];
            firstXCombinations *= firstXArity[i];
        }
        
        double kl = 0;
        
        Value.Structured datum = new StructureFN.FastDiscreteStructure(dataType,state);
            
        for (int i = 0; i < firstXCombinations; i++) {
            // Copy states from firstXIndex[] to state[]            
            for (int j = 0; j < firstX.length; j++) {
                state[firstX[j]] = firstXindex[j];
            }
            double logPX1 = 0;
            for (int j = 0; j < firstX.length; j++) {
                logPX1 += subLogP(params1,datum,firstX[j])[firstXindex[j]];
                if (logPX1 == Double.NEGATIVE_INFINITY) { break; }
            }
            if (logPX1 != Double.NEGATIVE_INFINITY) {
                double logPX2 = 0;
                for (int j = 0; j < firstX.length; j++) {
                    logPX2 += subLogP(params2,datum,firstX[j])[firstXindex[j]];
                    if (logPX2 == Double.NEGATIVE_INFINITY) { break; }
                }

                double p1 = Math.exp(logPX1);
                kl += p1 * (logPX1 - logPX2);
                if ( multipleX ) {
                    kl += p1 * klSub(arcs1,arcs2,order,params1,params2,state,arity,hash);
                }
            }
            BNet.incrementBitfield(firstXindex,firstXArity);
        }
        for (int j = 0; j < firstX.length; j++) {
            state[firstX[j]] = -1;
        }
            
        hash.put2(keyState,new Double(kl));
        
        return kl;
    }
    
    /** Calculate kl distance recursively between two network structures. */
    public double klExact( Value.Vector params1, Value.Vector params2 ) {
        int[] arity = getArity();
        
        int n = arity.length;
        
        // Extract DAG structures from params1 and params2 to form
        // a new directed graph which may contain cycles.
        int[][] arcs1 = new int[n][];
        int[][] arcs2 = new int[n][];
        for (int i = 0; i < n; i++) {
            Value.Vector parents1 = (Value.Vector)((Value.Structured)params1.elt(i)).cmpnt(1);
            Value.Vector parents2 = (Value.Vector)((Value.Structured)params2.elt(i)).cmpnt(1);
            
            arcs1[i] = new int[parents1.length()];
            for (int j = 0; j < arcs1[i].length; j++) {
                arcs1[i][j] = parents1.intAt(j);
            }

            arcs2[i] = new int[parents2.length()];
            for (int j = 0; j < arcs2[i].length; j++) {
                arcs2[i][j] = parents2.intAt(j);
            }

        }
        
        // Create an arcMatrix containing all links from both graphs,
        // then add all implied links.  This makes the detection of
        // cycles easier.
        boolean[][] arcMatrix = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < arcs1[i].length; j++) {
                arcMatrix[i][arcs1[i][j]] = true;
            }
            for (int j = 0; j < arcs2[i].length; j++) {
                arcMatrix[i][arcs2[i][j]] = true;
            }
        }
        ExpertElicitedTOMCoster.addImpliedConstraints(arcMatrix);

        // Count how many implied parents each node has.
        int numParents[] = new int[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && arcMatrix[i][j]) { numParents[i] ++; }
            }
        }
        
        // Extract a valid total ordering from arc matrix.
        // Instead of checking if all a node's parents are already used, we can
        // sort by the number of parents each node has.
        // This works as if (A) is a parent of (B) in a DAG, then B will have all of
        // A's parents as well as A, and as such more arcs.
        // If we are not dealing with a DAG (no consistent total ordering) then each node
        // in a cycle will have the same set of parents, and as such the same number
        // of parents so our method still works.
        //
        // When a valid total ordering is not possible, we form cliques.
        // Each clique consists of one or more nodes.  A clique contains multiple
        // nodes when nodes are part of a cycle.

        // Create an array of Lists, list[i] contains all elements with i parents.
        ArrayList<Integer>[] orderList = new ArrayList[n+1];
        for (int i = 0; i < orderList.length; i++) { orderList[i] = new ArrayList<Integer>(); }
        for (int i = 0; i < numParents.length; i++) {
            orderList[numParents[i]].add(i);
        }

        // Each list in orderList contains a group of variables with the same 
        // number of parents.  We need to further split this grouping into cliques.
        // A clique is a group of nodes in which every node is both a parent and child
        // of every other node in the clique.  This occurs because by joining the structure
        // of two DAGs (and adding implied links) we end up with a directed graph which may
        // contain cycles.
        ArrayList<ArrayList<Integer>> orderList2 = new ArrayList<ArrayList<Integer>>();
        for (ArrayList<Integer> list : orderList) {
            // Group cliques into seperate items.
            if (list.size() == 0) { continue; }
            if (list.size() == 1) { orderList2.add(list); continue;}
            
            while ( list.size() > 0 ) {
                // Create a clique using the first element of the list
                ArrayList<Integer> clique = new ArrayList<Integer>();
                Integer cliqueNode = list.remove(0);
                clique.add(cliqueNode);                                
                // Loop through the list and add all elements which are members of the new clique
                for (Integer x: list) {
                    if ( arcMatrix[cliqueNode][x] && arcMatrix[x][cliqueNode] ) {                        
                        clique.add( x );
                    }
                }
                for ( Integer x : clique) { list.remove(x); }
                orderList2.add(clique);
            }
        }
        
        //System.out.println("order : " + orderList2);
        
        // Convert list of cliques into partial ordering required by klSub        
        int order[][] = new int[orderList2.size()][];
        for (int i = 0; i < order.length; i++) {
            order[i] = new int[orderList2.get(i).size()];
            for (int j = 0; j < order[i].length; j++) {
                order[i][j] = orderList2.get(i).get(j);
            }
        }
        
        // State is [-1,-1,....,-1] as this means we need the KL distance over the whole network.
        int[] state = new int[n];
        Arrays.fill(state,-1);

        // Recursively calculate kl distance.
        return klSub(arcs1,arcs2,order,params1,params2,state,arity, new ArrayIndexedHashTable());
    }
    
    /** Flag is warning has been printed in kl() */
    private static boolean klWarningPrinted = false;

    /** Calculate KL distance exactly if k <= 30, or stochastically if k > 30. */
    public double kl( Value.Vector params1, Value.Vector params2 ) {        
        if ( params1.length() <= 30) {
            return klExact(params1,params2);
        }
        else {
            if ( !klWarningPrinted ) {
                System.out.println("WARNING: Using stochastic approximation to KL.");
                klWarningPrinted = true;
            }
            return klStochastic(params1, params2);
        }
    }
    
    /** Calculate causal KL distance using uniform prior over interventions.  <br>
     *  prior == 0 : Normal KL <br>
     *  prior == 1 : CKL1, Uniform prior over intervention set and space <br>
     *  prior == 2 : CKL2, Prior P over intervention space, uniform over intervention set. <br>
     *  prior == 3 : CKL3, Prior P over intervention space, all-but-ont intervention set prior. <br> 
     *  
     *  Result is multiplied by k so ckl results are directly comparable to KL.
     *  For KL k=1, for CKL1&CKL2 k=2, for CKL3 k=numVars. <br>
     *  */        
    public double ckl( Value.Vector paramsP, Value.Vector paramsQ, int prior ) {
        Value.Structured my1, my2;
        double multiplier;
        switch (prior) {
        case 0:    
            my1 = new Value.DefStructured( new Value[] {this,paramsP} );
            my2 = new Value.DefStructured( new Value[] {this,paramsQ} );
            multiplier = 1;
            break;
        case 1:
            my1 = AugmentFN.augment.apply(this,paramsP);
            my2 = AugmentFN.augment.apply(this,paramsQ);
            multiplier = 2;
            break;
        case 2:
            my1 = AugmentFN2.augment2.apply(this,paramsP,paramsP);
            my2 = AugmentFN2.augment2.apply(this,paramsQ,paramsP);
            multiplier = 2;
            break;
        case 3:
            my1 = AugmentFN3.augment3.apply(this,paramsP,paramsP);
            my2 = AugmentFN3.augment3.apply(this,paramsQ,paramsP);
            multiplier = 1;
            break;
        default:
            throw new RuntimeException("Unknown CKL type.");
        }

        BNet bNet = (BNet)my1.cmpnt(0);
        Value.Vector augParams1 = (Value.Vector)my1.cmpnt(1);
        Value.Vector augParams2 = (Value.Vector)my2.cmpnt(1);
        return bNet.kl(augParams1,augParams2) * multiplier;
    }
}    


