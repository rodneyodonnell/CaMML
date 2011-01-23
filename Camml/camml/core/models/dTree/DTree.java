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
// DTree model class
//

// File: DTree.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.dTree;

import java.util.Random;

import cdms.core.*;
import camml.core.library.DTreeSelectedVector;
import camml.core.library.MergedVector;
import camml.core.library.SelectedStructure;

import camml.core.models.ModelLearner.GetNumParams;

/**
   DTree model. <br>
   A DTree model represents a decision tree.  This is an experimental version of a DTree 
   specifically for use withing the Camml program, but should be fine for elsewhere <br>
 
   Parameter Space = (subModel,subParams)                         <br>
   where subParams = (splitArrtibute, [params]) for a split node  <br>
   and subParams = (params) for a leaf node.                      <br>
*/
public class DTree extends Value.Model
    implements GetNumParams
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -3094720043530783582L;
    
    public static final DTree dTree = new DTree();
    
    /**
       Initialise the DTree. This checks that parentlwbs and parentupbs have the same length,
       and that logProbs is exactly the right size. Throws an exception if the above is not
       the case.
    */
    public DTree( )
    {
        super(new Type.Model(new Type.Variable(), // input space (x)
                             Type.STRUCTURED,     // parameter space (y)  
                             Type.STRUCTURED,     // shared space (z)
                             Type.STRUCTURED) );   // sufficient space
        
        
    }
    
    /**
     *  we require sufficient statistics to get number of parents ... this should be part of
     *  initial information (which does not exist yet)
     */
    public int getNumParents( Value s ) {
        Value.Structured stats = (Value.Structured)s;
        Value.Vector z = (Value.Vector)stats.cmpnt(1);
        Type.Vector vectorType = (Type.Vector)z.t;
        Type.Structured eltType = (Type.Structured)vectorType.elt;
        return eltType.cmpnts.length;
    }
    
    /** logP(X|Y,Z) where v = (X,Y,Z) */
    public double logP(Value.Structured v)
    {
        return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }
    
    
    /**
       Returns a stochastic vector of elements from the data-space conditional on Y,Z.
       This normalises the DTree.
    */
    public Value.Vector generate(Random rand, int n, Value y, Value z)
    {
        return generate( rand, y, new VectorFN.UniformVector( n, z ) );
    }
    
    /**
     * Does the real work of the generate function.  We use a java.util.Random as a seed instead
     * of an int.
     */
    public Value.Vector generate( java.util.Random rand, Value y, Value.Vector z )
    {
        //    System.out.println( z );
        Value.Structured params = (Value.Structured)y;
        int splitAttribute = ((Value.Structured)y).intCmpnt(0);
        
        // If we are in a leaf node, return logP of all values into the leaf.
        if ( splitAttribute == -1 ) {
            Value.Structured paramStruct = (Value.Structured)params.cmpnt(2);
            Value.Model subModel = (Value.Model)paramStruct.cmpnt(0);
            Value subParams = paramStruct.cmpnt(1);
            //int samples = z.length();
            return subModel.generate( rand, subParams, z
                                      /*new VectorFN.UniformVector( samples, Value.TRIV)*/ );
        }
        
        
        // Split input based on the value of splitAttribute.  remove splitAttribute from dataset.
        DTreeSelectedVector[] splitZ;
        try {
            splitZ = DTreeSelectedVector.d_splitVector( z, splitAttribute, true );
        }
        catch (java.lang.ArrayIndexOutOfBoundsException e) {
            System.out.println("y = " + y);
            System.out.println(z.t);
            System.out.println("splitAttribute = " + splitAttribute);
            throw e;
        }
        
        // extract parameters for each submodel
        Value.Vector subParamVector = (Value.Vector)params.cmpnt(2);
        
        
        // split on parent attributes to recursively create data.
        Value.Vector[] splitX = new Value.Vector[splitZ.length];
        for ( int i = 0; i < splitZ.length; i++ ) {
            splitX[i] = generate( rand, subParamVector.elt(i), splitZ[i] );
        }
        
        // now me must merge the data again into the correct order.       
        int[][] order = new int[splitX.length][];
        for ( int i = 0; i < order.length; i++ ) {
            order[i] = splitZ[i].d_getRows();
        }
        
        return new MergedVector( splitX,  order );
    }
    
    
    /** predict returns the value x which has the maximum probability given y (hence the least 
        logprob) if two values have the same logprob, it will return the first such.
    */
    public Value predict(Value y, Value z)
    {
        throw new RuntimeException("Not implemented");
        //     Value.Structured parentVals = (Value.Structured)z;
        //     int parentIndex = decodeParents(parentVals);
        
        //     Value.Vector yVector = (Value.Vector)y;
        //     Value.Structured yElt = (Value.Structured)yVector.elt(parentIndex);
        //     Value.Model subModel = (Value.Model)yElt.cmpnt(0);
        //     Value subParams = yElt.cmpnt(1);
        
        //     return subModel.predict( subParams, Value.TRIV ); 
        
    } 
    
    public Value.Vector predict(Value y, Value.Vector z) {
        return new VectorFN.UniformVector(z.length(), predict(y, z));
    }
    
    /**
     *  The sufficient statistics of a DTree is a list of sufficient stats of its elements.
     *  Each paremt combination maps to a given entry in the sufficient vector.  This mapping is
     *  defined by DTree.decodeParents()
     */
    public Value getSufficient(Value.Vector x, Value.Vector z)
    {
        return new Value.DefStructured( new Value[] {x,z},
                                        new String[] {"x", "z"});
    }
    
    /** 
     *  return logP of output vector given input vector and params.
     */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
        Value.Structured params = (Value.Structured)y;
        int splitAttribute = ((Value.Structured)y).intCmpnt(0);
        
        // If we are in a leaf node, return logP of all values into the leaf.
        if ( splitAttribute == -1 ) {
            Value.Structured paramStruct = (Value.Structured)params.cmpnt(2);
            Value.Model subModel = (Value.Model)paramStruct.cmpnt(0);
            Value subParams = paramStruct.cmpnt(1);
            return subModel.logP( x, subParams, z /*new VectorFN.UniformVector(x.length(),Value.TRIV)*/ );
        }
        
        
        // Split input based on the value of splitAttribute.  remove splitAttribute from dataset.
        DTreeSelectedVector[] splitZ = DTreeSelectedVector.d_splitVector( z, splitAttribute, true );
        
        // extract parameters for each submodel
        Value.Vector subParamVector = (Value.Vector)params.cmpnt(2);
        
        double sum = 0;
        for ( int i = 0; i < splitZ.length; i++ ) {
            Vector splitX = splitZ[i].d_copyRowSplit( x );
            sum += logP( splitX, subParamVector.elt(i), splitZ[i] );
        }
        
        return sum;
        
    }
    
    /** logP(X|Y,Z) */
    public double logP(Value x, Value y, Value z)
    {
        
        Value.Structured params = (Value.Structured)y;
        int splitAttribute = ((Value.Structured)y).intCmpnt(0);
        
        // If we are in a leaf node, return logP of all values into the leaf.
        if ( splitAttribute == -1 ) {
            Value.Structured paramStruct = (Value.Structured)params.cmpnt(2);
            Value.Model subModel = (Value.Model)paramStruct.cmpnt(0);
            Value subParams = paramStruct.cmpnt(1);
            return subModel.logP( x, subParams, z);
        }
        

        
        
        // Create copy of 'z' with splitAttribute missing.
        Value.Structured zStruct = (Value.Structured)z;
        int[] col = new int[zStruct.length()-1];
        for ( int i = 0; i < splitAttribute; i++ ) { col[i] = i; }
        for ( int i = splitAttribute; i < col.length; i++ ) { col[i] = i+1; }        
        SelectedStructure subZ = new SelectedStructure(zStruct,col);
                
        Value.Vector subParamVector2 = (Value.Vector)params.cmpnt(2);
        double sum2 = logP( x, subParamVector2.elt(zStruct.intCmpnt(splitAttribute)), subZ );

        return sum2;
    }

    
    /** logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
        In this case, s is simply the vector x */
    public double logPSufficient(Value s, Value y)
    {
        Value.Vector x = (Value.Vector)((Value.Structured)s).cmpnt(0);;
        Value.Vector z = (Value.Vector)((Value.Structured)s).cmpnt(1);;
        
        return logP( x, y, z );
    }
    
    /** returns a representation of the DTree */
    public String toString()
    {
        return "DTree";
    } 
    
    /** return the number of free parameters. */
    public int getNumParams( Value y ) {
        Value.Structured params = (Value.Structured)y;
        int splitAttribute = ((Value.Structured)y).intCmpnt(0);
        
        // If we are in a leaf node, return logP of all values into the leaf.
        if ( splitAttribute == -1 ) {
            Value.Structured paramStruct = (Value.Structured)params.cmpnt(2);
            Value.Model subModel = (Value.Model)paramStruct.cmpnt(0);
            Value subParams = paramStruct.cmpnt(1);
            return ((GetNumParams)subModel).getNumParams( subParams );
        }
        
        
        
        // extract parameters for each submodel
        Value.Vector subParamVector = (Value.Vector)params.cmpnt(2);
        
        int sum = 0;
        for ( int i = 0; i < subParamVector.length(); i++ ) {
            sum += getNumParams( subParamVector.elt(i) );
        }
        
        return sum;
        
    }
    
}
