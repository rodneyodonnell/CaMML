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
// BNet model generator
//

// File: BNetGenerator.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.bNet;

import java.util.Random;

import cdms.core.*;
import camml.core.models.dTree.DTreeGenerator;

/**
   BNetGenerator is a model which generates fully parameterized decision trees. <br>
   This should theoretically allow us to use standard model functions (generate,logP,predict,etc)
   to deal with the creation and costing of Parameterized Decision Tree models.
 
   Parameter Space = (numParams,subModel,subParams)                         <br>
   where subParams = (splitArrtibute, [params]) for a split node  <br>
   and subParams = (params) for a leaf node.                      <br>
 
   input = ([parentArity],arcProb,leafProb) <br>
   output = (Model,[params])            <br>
   sufficient = output                  <br>
   parameter = ()            <br>
*/
public class BNetGenerator extends Value.Model
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7312964969631464022L;

    /** Static instance of BNetGenerator */
    public static final BNetGenerator generator = new BNetGenerator();
    
    /** Output type of BNetGeneratoe */
    public static Type xType = new Type.Structured(new Type[] {Type.MODEL,Type.STRUCTURED});
    
    /** Initialise the BNetGenerator.   */
    public BNetGenerator( )
    {
        super(new Type.Model( Type.TRIV,      // input space (x)
                              Type.DISCRETE,  // parameter space (y)
                              xType,          // shared space (z)
                              xType ));       // sufficient space(s)
    }
    
    
    
    /** logP(X|Y,Z) -- not implemented */
    
    public double logP(Value x, Value y, Value z)
    {
        throw new RuntimeException("Not Implemented");
    }
    
    /** logP(X|Y,Z) where v = (X,Y,Z) -- not implemented */
    public double logP(Value.Structured v)
    {
        throw new RuntimeException("Not Implemented");
    }
    
    
    /** Generate a vector of parameterized BNets */
    public Value.Vector generate(Random rand, int n, Value y, Value z) {    
        return generate( rand, y, new VectorFN.UniformVector( n, z ) );
    }
    
    public Value.Structured generate( java.util.Random rand, int[] arity,
                                      double pArc, double leafProb ) {
        boolean splitOnAll = true;
        
        int numNodes = arity.length;
        int[] order = new int[numNodes];
        for ( int i = 0; i < order.length; i++ ) {
            order[i] = i;
        }
        // Randomize the order of nodes.
        for ( int i = 0; i < order.length; i++ ) {
            int j = rand.nextInt(order.length-i)+i;  // generate a number [i..length)
            int swap = order[i];
            order[i] = order[j];
            order[j] = swap;
        }
        
        // Create a random matrix of links.
        // if i is before j in order, link[i][j] = true, else link[j][i] = true;
        boolean[][] link = new boolean[numNodes][numNodes];
        for ( int i = 0; i < numNodes; i++ ) {
            for ( int j = 0; j < i; j++ ) {
                if ( rand.nextDouble() < pArc ) {
                    for ( int k = 0; k < order.length; k++ ) {
                        if ( order[k] == i ) { link[i][j] = true; break; }
                        else if ( order[k] == j ) { link[j][i] = true; break; }
                        else if ( k == order.length-1 ) { 
                            throw new RuntimeException("Unreachable state."); 
                        }
                    }
                }
            }
        }
        
        // Create a name for each variable.
        String[] name = new String[numNodes];
        for ( int i = 0; i < name.length; i++ ) {
            name[i] = "var"+i;
        }
        
        // Create a list of parents for each variable.
        int[][] parent = new int[numNodes][];
        for ( int i = 0; i < link.length; i++ ) {
            int numParent = 0;
            for ( int j = 0; j < link[i].length; j++ ) {
                if ( link[i][j] == true ) numParent ++;
            }
            parent[i] = new int[numParent];
            
            numParent = 0;
            for ( int j = 0; j < link[i].length; j++ ) {
                if ( link[i][j] == true ) { parent[i][numParent] = j; numParent ++; }
            }
        }
        
        // Work out the parent arity for each node.
        int[][] parentArity = new int[numNodes][];
        for ( int i = 0; i < parentArity.length; i++ ) {
            parentArity[i] = new int[parent[i].length];
            for ( int j = 0; j < parentArity[i].length; j++ ) {
                parentArity[i][j] = arity[parent[i][j]];
            }
        }
        
        // Create a parameterized model for each node.
        Value[] subModel = new Value[numNodes];
        for ( int i = 0; i < subModel.length; i++ ) {
            subModel[i] = DTreeGenerator.generate(rand,arity[i],parentArity[i],leafProb,splitOnAll);
        }
        
        
        Value[] nodeVal = new Value[numNodes];
        for ( int i = 0; i < nodeVal.length; i++ ) {
            nodeVal[i] = new Value.DefStructured( new Value[] { new Value.Str(name[i]),
                                                                new VectorFN.FastDiscreteVector(parent[i]),
                                                                subModel[i] } );
        }
        
        
        Type.Discrete[] typeArray = new Type.Discrete[ numNodes ];
        for ( int i = 0; i < typeArray.length; i++ ) {
            typeArray[i] = new Type.Discrete( 0, arity[i]-1, false, false, false, false );
        }
        Type.Structured dataType = new Type.Structured( typeArray, name );
        //Value.Model model = new camml.plugin.netica.BNetNetica( dataType );
        Value.Model model = new camml.core.models.bNet.BNetStochastic( dataType );
        Value.Vector params = new VectorFN.FatVector( nodeVal );
        Value.Structured modelParamStruct = new Value.DefStructured( new Value[] {model,params} );
        
        return modelParamStruct;
    }
    
    /**
     * Does the real work of the generate function.  We use a java.util.Random as a seed instead
     * of an int.  z = [([parentArity],arcProb,leafProb)]
     */
    public Value.Vector generate( java.util.Random rand, Value y, Value.Vector z ) {
        
        Value.Structured[] bNetVec = new Value.Structured[z.length()];
        
        for ( int i = 0; i < bNetVec.length; i++ ) {
            Value.Structured zStruct = (Value.Structured)z.elt(i);
            Value.Vector arityVec = (Value.Vector)zStruct.cmpnt(0);
            int[] arity = new int[arityVec.length()];
            for ( int j = 0; j < arity.length; j++ ) {
                arity[j] = arityVec.intAt(j);
            }
            double arcProb = zStruct.doubleCmpnt(1);
            double leafProb = zStruct.doubleCmpnt(2);
            
            bNetVec[i] = generate( rand, arity, arcProb, leafProb );
        }
        
        return new VectorFN.FatVector( bNetVec );
    }
    
    
    
    /** not implemented */
    public Value predict(Value y, Value z)
    {
        throw new RuntimeException("Not implemented");
    } 
    
    public Value.Vector predict(Value y, Value.Vector z) {
        return new VectorFN.UniformVector(z.length(), predict(y, z));
    }
    
    /** returns x */
    public Value getSufficient(Value.Vector x, Value.Vector z)
    {
        return x;
    }
    
    /** not implemented */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
        throw new RuntimeException("Not implemented");
    }
    
    
    
    /** logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
        In this case, s is simply the vector x */
    public double logPSufficient(Value s, Value y)
    {
        Value.Vector x = (Value.Vector)((Value.Structured)s).cmpnt(0);;
        Value.Vector z = (Value.Vector)((Value.Structured)s).cmpnt(1);;
        
        return logP( x, y, z );
    }
    
    /** returns a representation of the BNetGenerator */
    public String toString()
    {
        return "BNetGenerator";
    } 
}
