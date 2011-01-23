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
// Bayesian Network model class
//

// File: BNetStochastic.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.bNet;

import java.util.Random;

import camml.core.library.WallaceRandom;
import cdms.core.*;

/**
 *  Bayesian Network model. <br>
 *
 *  A BNetStochastic model is a CDMS implementation of a Bayesian Network. <br>
 *  
 *  Unlike many other models, the input and output of a Bayesian Net are not strictly defined and
 *  a variable which was once viewed as an input may later be viewed as an output.  To cope with
 *  this all variables are included as both inputs and outputs of the network. <br>
 *
 *  To "query" a variable it's value should be set to missing. <br>
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
 *  Inference is performed stochasticaly.  This is due to ease of implementation, and it also 
 *  removes concerns about the combination of differing variable types. <br>
 *
 */


public class BNetStochastic extends BNet
{        
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7175787224241505759L;
    /** Number of samples used when stochastically finding a probability */
    public final int numSamples;
    
    /**
     *  Simple constructor.  Model type = { 
     *  X = Type.VECTOR,
     *  Z = Type.VECTOR, 
     *  Y = [ ( name, [parents], (model,params) ) ] 
     *  S = (Type.VECTOR, Type.VECTOR) }
     */
    public BNetStochastic( Type.Structured dataType, int numSamples )
    {
        super( dataType );
        this.numSamples = numSamples;
    }
    
    /**
     *  Simple constructor.  Model type = { 
     *  X = Type.VECTOR,
     *  Z = Type.VECTOR, 
     *  Y = [ ( name, [parents], (model,params) ) ] 
     *  S = (Type.VECTOR, Type.VECTOR) }
     *
     *  numSamples automatically set to 10000
     */
    public BNetStochastic( Type.Structured dataType )
    {
        super( dataType );
        this.numSamples = 10000;
    }
    
    
    /** logP(X|Y,Z) where v = (X,Y,Z) */
    public double logP(Value.Structured v)
    {
        return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }
    
    /**
     *  Returns a stochastic vector of elements from the data-space conditional on Y,Z.
     *  This normalises the CPT. <br>
     *  Y = [ name, [parents], (model.params) ]       
     */
    public Value.Vector generate(Random rand, int n, Value y, Value z)
    {
        // Ensure we are using a 'good' random number generator.
        if (rand.getClass() == Random.class) {
            rand = new WallaceRandom(new int[]{rand.nextInt(),rand.nextInt()} );
        }
        // Extract information from parameter list.
        String[] name = makeNameList( (Value.Vector)y );
        int[][] parentList = makeParentList( (Value.Vector)y );
        int[] order = makeConsistantOrdering( parentList );
        Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
        Value[] subModelParamList = makeSubParamList( (Value.Vector)y );
        Type.Model mType = (Type.Model)t;
        
        Type.Structured inputType = (Type.Structured)mType.sharedSpace;
        
        
        // If triv is passed as input, this means that all input values are missing.
        //  so we have to create values in their place.
        Value.Structured input;
        if (z == Value.TRIV ) {            
            input =  makeUnobservedInputStruct();
        }
        else {
            input = (Value.Structured)z;
        }
        
        
        // OutputType is a vector of inputType.
        Type.Structured outputType;
        Type subOutputType[] = new Type[name.length];
        for ( int i = 0; i < subOutputType.length; i++) 
            subOutputType[i] = new Type.Vector( inputType.cmpnts[i] );
        outputType = new Type.Structured( subOutputType, name );
        
        
        // data[i] is the i'th subvector of data.  ie. the i'th variable
        Value.Vector[] data = new Value.Vector[ parentList.length ];
        for ( int i = 0; i < order.length; i++ ) {
            
            // We need to sample the variable from a topologically sorted list so parent data
            //  is always evaluated before its childrens data
            int current = order[i];
            ValueStatus status = input.cmpnt(current).status();
            
            
            if ( status == Value.S_UNOBSERVED ) { // if value unobserved
                // Copy all parent data vectors into parentArray
                Value.Vector[] parentArray = new Value.Vector[ parentList[current].length ];
                Type[] parentTypeArray = new Type[parentArray.length];
                String[] parentNameArray = new String[parentArray.length];
                
                
                
                for ( int j = 0; j < parentArray.length; j++ ) {
                    parentArray[j] = data[ parentList[current][j] ];
                    parentTypeArray[j] = subOutputType[ parentList[current][j] ];
                    parentNameArray[j] = name[ parentList[current][j] ];
                }
                Type.Structured parentType = new Type.Structured( parentTypeArray, 
                                                                  parentNameArray );
                
                Value.Vector parentData;
                if ( parentArray.length != 0 ) { // make a vector from parentArray
                    parentData = new VectorFN.MultiCol( new Value.DefStructured(parentType,
                                                                                parentArray) );
                }
                else { // Have to manually create an empty vector, otherwise length == 0
                    parentData = 
                        new VectorFN.UniformVector(n,new Value.DefStructured(new Value[] {}));
                }
                
                
                // using parent data and parameters, generate data for current variable.
                data[current] = ((Value.Model)subModelList[current]).generate( rand,
                                                                               subModelParamList[current], 
                                                                               parentData );
                
            }
            else if ( status == Value.S_PROPER ) { // normal observed data
                if ( parentList[current].length != 0 ) {
                    throw new RuntimeException("Cannot generate data with non root node evidence.");
                }
                
                data[current] = new VectorFN.UniformVector(n,input.cmpnt(current));
            }
            else if ( status == Value.S_INTERVENTION ) { // Data with an intervention present.
                data[current] = new VectorFN.UniformVector(n,input.cmpnt(current));
            }
            else {
                throw new RuntimeException("Invalid Value Status. : " + status );
            }
        }        
        
        
        
        Value.Structured tempStruct = new Value.DefStructured( outputType, data);
        Value.Vector initialVector = new VectorFN.MultiCol( tempStruct );
        
        
        // Sadly the above is not enough to generate a proper sample.  This will work if all input
        // variables are in root nodes (or only have parents which are also inputs).  We now need
        // to sift through and remove all invalid cases.
        
        return initialVector;
    }
    
    /**
     * Predict returns the most probably state of the network.  As the optimal solution to this
     *  would (possibly) require searching through all states instead a sub-optimal solution is
     *  found by traversing the network from root to leaf choosing the most likely state at each
     *  point.
     */
    public Value predict(Value y, Value z)
    {
        // Extract information from parameter list.
        //String[] name = makeNameList( (Value.Vector)y );
        int[][] parentList = makeParentList( (Value.Vector)y );
        int[] order = makeConsistantOrdering( parentList );
        Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
        Value[] subModelParamList = makeSubParamList( (Value.Vector)y );
        
        Value.Structured input;
        if ( z == Value.TRIV ) {
            input = makeUnobservedInputStruct();
        }
        else {
            input = (Value.Structured)z;
        }
        
        // data[i] is the i'th subvector of data.  ie. the i'th variable
        Value[] data = new Value[ parentList.length ];
        for ( int i = 0; i < order.length; i++ ) {
            
            // We need to sample the variable from a topologically sorted list so parent data
            //  is always evaluated before its childrens data
            int current = order[i];
            
            if ( input.cmpnt(current).status() == Value.S_UNOBSERVED ) { // if value unobserved
                // Copy all parent data vectors into parentArray
                Value[] parentArray = new Value[ parentList[current].length ];
                for ( int j = 0; j < parentArray.length; j++ ) {
                    parentArray[j] = data[parentList[current][j]];
                }
                
                // make a structure from parentArray
                Value.Structured parentData = new Value.DefStructured(parentArray);
                
                // using parent data and parameters, generate data for current variable.
                data[current] = subModelList[current].predict( subModelParamList[current],
                                                               parentData );
            }
            else {
                data[current] = input.cmpnt(current);
            }
        }        
        
        // Return a structure containing the predicted output values.
        return new Value.DefStructured( data );
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
    
    
    /** logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
        In this case, s is simply the vector x */
    public double logPSufficient(Value s, Value y)
    {
        throw new RuntimeException("Function not implemented");
    }   
    
    
    /** Take BNetStochastic parameters and return a list of names. */
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
    
    /** Take BNetStochastic parameters and return an array of the parents of each variable. */
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
    
    protected Value.Vector lastY;
    protected Value.Structured lastZ;
    protected Value.Vector lastData;
    /**
     *  Generates numSample cases using the given input amd parameters.  <br>
     *  No weighting of samples occurs so the output generated should differ from a (correctly
     *   implemented) version of the standard Model.generate() function.
     */
    protected Value.Vector generateUnweightedSample( java.util.Random rand, 
                                                     Value.Vector y, Value.Structured z)
    {
        // Shortcut.  Simply return the last set of generated data.
        if ( lastY == y && lastZ == z ) { return lastData; }
        
        // Extract information from parameter list.
        int[][] parentList = makeParentList( (Value.Vector)y );
        int[] order = makeConsistantOrdering( parentList );
        Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
        Value[] subModelParamList = makeSubParamList( (Value.Vector)y );
        
        
        // data[i] is the i'th subvector of data.  ie. the i'th variable
        Value.Vector[] data = new Value.Vector[ parentList.length ];
        for ( int i = 0; i < order.length; i++ ) {
            
            // We need to sample the variable from a topologically sorted list so parent data
            //  is always evaluated before its childrens data
            int current = order[i];
            
            
            // Next we must collect all parent data of the current variable into a single
            //  Value.Vector.  This is done by making an array of Value.Vectors then creating a 
            //  MultiCol Vector from this.  If the current variable has no parents than we need
            //  to create an empty vector of the appropriate length instead.
            Value.Vector[] parentArray = new Value.Vector[ parentList[current].length ];
            Value.Vector parentData;
            if ( parentArray.length != 0 ) {
                for ( int j = 0; j < parentArray.length; j++ ) {
                    parentArray[j] = data[parentList[current][j]];
                }
                parentData = new VectorFN.MultiCol( new Value.DefStructured(parentArray) );
            }
            else { // Have to manually create an empty vector, otherwise length == 0
                parentData = 
                    new VectorFN.UniformVector(numSamples ,
                                               new Value.DefStructured(new Value[] {}) );
            }
            
            
            
            Value cmpnt = z.cmpnt(current);
            ValueStatus status = cmpnt.status();
            
            // No data has been onserved, so randomly generate some samples.
            if ( status == Value.S_UNOBSERVED || cmpnt == Value.TRIV ) {
                
                // using parent data and parameters, generate data for current variable.
                data[current] = ((Value.Model)subModelList[current]).generate(rand, 
                                                                              subModelParamList[current], 
                                                                              parentData );        
            }
            // We know what the data should be, so make a vector of it.
            else if ( status == Value.S_PROPER || 
                      status == S_INTERVENTION ) {
                data[current] = new VectorFN.UniformVector( numSamples, cmpnt );
            }
            else {
                throw new RuntimeException("Invalid Status : " + status );
            }
            
        }
        
        Value.Vector vec = new VectorFN.MultiCol( new Value.DefStructured( data ) );
        
        lastY = y;
        lastZ = z;
        lastData = vec;
        
        return vec;
    }
    
    /** logP(X|Y,Z) */
    public double logP(Value x, Value y, Value z)
    {    
        // Extract information from parameter list.
        //String[] name = makeNameList( (Value.Vector)y );
        int[][] parentList = makeParentList( (Value.Vector)y );
        int[] order = makeConsistantOrdering( parentList );
        Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
        Value[] subModelParamList = makeSubParamList( (Value.Vector)y );
        
        
        // if input is Value.TRIV (ie. all parents missing) then we can take a shortcut.
        if ( z == Value.TRIV ) {
            return noInputLogP( (Value.Structured)x, 
                                subModelList, subModelParamList, order, parentList );
            
        }
        
        Value.Structured input = (Value.Structured)z;    
        
        Value.Structured output = (Value.Structured)x;
        
        
        // Initialise all weights to 1.0
        double[] weight = new double[numSamples];
        boolean[] match = new boolean[numSamples];
        for ( int i = 0; i < weight.length; i++ ) {
            weight[i] = 1.0;
            match[i] = true;
        } 
        
        
        Value.Vector sample = generateUnweightedSample( new java.util.Random(123),
                                                        (Value.Vector)y, input );
        
        
        // data[i] is the i'th subvector of data.  ie. the i'th variable
        for ( int i = 0; i < order.length; i++ ) {
            
            // We need to sample the variable from a topologically sorted list so parent data
            //  is always evaluated before its childrens data
            int current = order[i];
            
            
            // Next we must collect all parent data of the current variable into a single
            //  Value.Vector.  This is done by making an array of Value.Vectors then creating a 
            //  MultiCol Vector from this.  If the current variable has no parents than we need
            //  to create an empty vector of the appropriate length instead.
            Value.Vector[] parentArray = new Value.Vector[ parentList[current].length ];
            Value.Vector parentData;
            if ( parentArray.length != 0 ) {
                for ( int j = 0; j < parentArray.length; j++ ) {
                    parentArray[j] = sample.cmpnt( parentList[current][j] );
                }
                parentData = new VectorFN.MultiCol( new Value.DefStructured(parentArray) );
            }
            else { // Have to manually create an empty vector, otherwise length == 0
                parentData = 
                    new VectorFN.UniformVector(numSamples ,
                                               new Value.DefStructured(new Value[] {}) );
            }
            
            Value in = input.cmpnt(current);
            Value out = output.cmpnt(current);

            // Status of current input/output.  
            // Value.TRIV is used as a convenient way to specify "Unobserved" 
            ValueStatus inStatus = in.status();
            ValueStatus outStatus = out.status();
            if (in == Value.TRIV) { inStatus = Value.S_UNOBSERVED; }
            if (out == Value.TRIV) { outStatus = Value.S_UNOBSERVED; }
            
            //
            // If the current value is not known at input or output, simply generate a vector
            // of values based on parent data.
            //
            if ( inStatus == Value.S_UNOBSERVED && outStatus == Value.S_UNOBSERVED  ) { 
                
                
            }
            //
            //  If the current attribute was unobserved as an input, but obvserved as an output we
            //  must make all outputs equal to the observed value.  In addition to this we must
            //  scale all weight using likelyhood weighting.
            //
            else if (inStatus == Value.S_UNOBSERVED && outStatus == Value.S_PROPER ){
                
                Value.Vector vec = sample.cmpnt( current );
                for ( int j = 0; j < match.length; j++ ) {
                    if ( !vec.elt(j).equals( output.cmpnt(current) ) ) {
                        match[j] = false;
                    }            
                }
                
            }
            // 
            // If the current value is known at input and output, do a check to make sure they
            // are the same.  If so, simply set the output sample to the appropriate value.
            // If the two values are different, throw an exception.
            // In addition to this all values must be weighted for likelyhood weighting.
            //
            else if (inStatus == Value.S_PROPER && outStatus == Value.S_PROPER ){
                
                // This should only occur when querying the probability of a state when a value
                // for that state has already been given.  Prob should equal zero.
                if ( !input.cmpnt(current).equals(output.cmpnt(current)) ) {
                    for ( int j = 0; j < match.length; j++ ) {
                        weight[j] = 0.0;
                    }
                }
                else {            
                    for ( int j = 0; j < weight.length; j++ ) {
                        double likelyhood = subModelList[current].logP( output.cmpnt(current), 
                                                                        subModelParamList[current], 
                                                                        parentData.elt(j) );
                        weight[j] *= Math.exp( likelyhood );
                    }
                    
                }
                
            }
            //
            // If there is an intervention on the input
            //
            else if ( inStatus == S_INTERVENTION && outStatus == S_INTERVENTION ) {
                
                // Make sure the value has not been modified
                if ( !input.cmpnt(current).equals(output.cmpnt(current)) ) {
                    throw new RuntimeException("Intervened upon variable differs from in->out");
                }
                else {            
                }
                
            }
            //
            // If we have a value at input, but not on output throw an exception.
            //
            else {
                throw new RuntimeException( "Invalid Status flag combination <" + 
                                            input.cmpnt(current).status() + "," + 
                                            output.cmpnt(current).status() + ">" );
            }
        }        
        
        double totalWeight = 0;
        double totalMatches = 0;
        for ( int i = 0; i < weight.length; i++) {
            if ( match[i] == true ) {
                totalMatches += weight[i];
            }
            totalWeight += weight[i];
        }
        
        if ( totalWeight == 0 ) {
            return Math.log(0);
        }
        else {
            return Math.log(totalMatches / totalWeight);
        }
    }
    
    
    public String toString() {
        return "BNet Stochastic Model";
    }
    
}


