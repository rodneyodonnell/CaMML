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
// DTree model generator
//

// File: DTreeGenerator.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.dTree;

import cdms.core.*;
import java.util.ArrayList;
import java.util.Random;

import camml.core.models.multinomial.*;

/**
   DTreeGenerator is a model which generates fully parameterized decision trees. <br>
   This should theoretically allow us to use standard model functions (generate,logP,predict,etc)
   to deal with the creation and costing of Parameterized Decision Tree models.
 
   Parameter Space = (numParams,subModel,subParams)                         <br>
   where subParams = (splitArrtibute, [params]) for a split node  <br>
   and subParams = (params) for a leaf node.                      <br>
 
   input = ()                           <br>
   output = (Model,[params])            <br>
   sufficient = output                  <br>
   parameter = ([parentArity],[(leafModel,leafParams)]))             <br>
*/
public class DTreeGenerator extends Value.Model
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -4510483415171647277L;

    /** Static instance of DTreeGenerator */
    public static final DTreeGenerator generator = new DTreeGenerator();
    
    /** Output type of DTreeGeneratoe */
    public static Type xType = new Type.Structured(new Type[] {Type.MODEL,Type.STRUCTURED});
    
    /** Initialise the DTreeGenerator.   */
    public DTreeGenerator( )
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
    
    
    /** Generate a vector of parameterized DTrees */
    public Value.Vector generate(Random rand, int n, Value y, Value z) {    
        return generate( rand, y, new VectorFN.UniformVector( n, z ) );
    }
    
    /** Structure used to keep track of tree as it is being built. */
    protected static class TreeNode
    {
        public TreeNode( int[] availableSplits, int[] parentArity, TreeNode parent )
        {
            this.parent = parent;        
            this.parentArity = parentArity;
            if ( availableSplits == null ) {
                this.availableSplits = new int[parentArity.length];
                for ( int i = 0; i < this.availableSplits.length; i++ ) {
                    this.availableSplits[i] = i;
                }
            }
            else {
                this.availableSplits = availableSplits;
            }
        }
        public final int[] parentArity;
        public final int[] availableSplits;
        public final TreeNode parent;
        public final ArrayList<TreeNode> children = new ArrayList<TreeNode>();
        
        public int splitAttribute = -1;
        public Value.Model model;
        public Value params;
        
        /** Return the number of leaf descendants this node has (leaf nodes return 1) */
        public int numDescendentLeaves()
        {
            if ( splitAttribute == -1 ) return 1;
            int numLeaves = 0;
            for ( int i = 0; i < children.size(); i++ ) {
                numLeaves += children.get(i).numDescendentLeaves();
            }
            return numLeaves;
        }
        
        public void splitOn( int x ) {
            if ( splitAttribute != -1 ) { throw new RuntimeException("Error."); }
            this.splitAttribute = x;
            int[] childSplitAttribute = new int[availableSplits.length-1];
            
            for ( int i = 0; i < childSplitAttribute.length; i++ ) {
                if ( availableSplits[i] < splitAttribute ) 
                    childSplitAttribute[i] = availableSplits[i];
                else  
                    childSplitAttribute[i] = availableSplits[i+1];
            }
            
            for ( int i = 0; i < parentArity[splitAttribute]; i++ ) {
                children.add( new TreeNode(childSplitAttribute, parentArity, this ) );
            }
        }
        
        /** Return the parameters of this DTree */
        Value.Structured makeParams()        
        {
            Value subParams;
            Value.Discrete splitVal = null;
            if ( splitAttribute == -1 ) {
                subParams = new Value.DefStructured( new Value[] {model,       
                                                                  params} );
                splitVal = new Value.Discrete( -1 );
            }
            else {
                Value[] subParamArray = new Value[children.size()];
                for ( int i = 0; i < children.size(); i++ ) {
                    subParamArray[i] = children.get(i).makeParams();
                }
                subParams = new VectorFN.FatVector( subParamArray );
                
                for ( int i = 0; i < availableSplits.length; i++ ) {
                    if ( availableSplits[i] == splitAttribute ) {
                        splitVal = new Value.Discrete(i);
                    }
                }
            }
            
            if ( splitVal == null ) { throw new RuntimeException("Unreachable state"); }
            
            Value.Structured full = 
                new Value.DefStructured( new Value[] { splitVal,
                                                       new Value.Continuous(0),
                                                       subParams } );
            if ( parent != null ) {
                return full;
            }
            else {
                return new Value.DefStructured( new Value[] {DTree.dTree, full} );
            }
        }
        
        public String toString()
        {
            StringBuffer s = new StringBuffer();
            s.append( "splitAttribute = " + splitAttribute + "\t" + "availableSplits = " );
            for ( int i = 0; i < availableSplits.length; i++ ) {
                s.append( "" + availableSplits[i] + " " );
            }
            s.append("\tnumDescendent = "+numDescendentLeaves()+"\n");
            for ( int i = 0; i < children.size(); i++ ) {
                s.append( children.get(i).toString() );
            }
            return s.toString();
        }
        
    }
    
    /**
     * We use a (probably bad) algorithm which keeps track of leaves and splits in 2 ArrayLists
     * Each leaf and node is stored as an array of possible children.
     * Each step of the algorithm selects a leaf at random, removes this leaf from the leafList
     *  and adds it to the splitList.  The leaf is replaces in leafList by it's new decendents
     *  which are N (where n is the arity of the variable being split upon) new leaves.
     * This splitting is stops when the next split would make the number of distributions
     *  required more than numLeaves (although it may terminate with less).
     * If a leaf is chosen that is unable to be further split upon, a new leaf is chosen instead
     * If all leaves are unable to be split upon (tree is a CPT) then the algorithm terminates. <br>
     *
     * if splitOnAll == true we make sure each variable is split on at least once. <br>
     *
     * childArity[] is the arity of the leaf node     <br>
     * arity[] is a list of the arity of each parent. <br>
     * leafProb defines the number of leaves.  numLeaves = maxParentCombinations * leafProb <br>
     */
    public static Value.Structured generate( java.util.Random rand, int childArity,
                                             int[] arity, double leafProb, boolean splitOnAll )
    {
        // We start with an empty split list, and a leafList with a single leaf able to be split
        // on any parent.
        java.util.ArrayList<TreeNode> leafList = new java.util.ArrayList<TreeNode>();
        java.util.ArrayList<TreeNode> splitList = new java.util.ArrayList<TreeNode>();
        
        if ( leafProb < 0 || leafProb > 1 ) {
            throw new RuntimeException( "leafProb out of range : " + leafProb );
        }
        
        // We can only generate up to maxCombinations leaves.
        int maxCombinations = 1;
        for ( int i = 0; i < arity.length; i++ ) maxCombinations *= arity[i];
        int numLeaves = (int)(leafProb * maxCombinations);
        if ( numLeaves == 0 ) numLeaves = 1; // make sure we have at least one leaf
        
        
        TreeNode treeRoot = new TreeNode(null,arity,null);
        leafList.add( treeRoot );
        
        // Keep track of all variables which have not been split on.
        boolean[] varHasBeenSplit = new boolean[arity.length];
        int unsplitVars = varHasBeenSplit.length;
        // if splitOnAll is NOT enabled, we don'e care about unsplit vars.  set this to 0.
        if ( splitOnAll == false ) { 
            for ( int i = 0; i < varHasBeenSplit.length; i++ ) {
                varHasBeenSplit[i] = true;
            }
            unsplitVars = 0; 
        }
        
        while ( true ) {
            if ( maxCombinations == 1 ) break;
            
            // Randomly select a leaf from leafList
            int nextLeaf = rand.nextInt( leafList.size() );
            TreeNode currentLeaf = leafList.remove( nextLeaf );
            int[] availableSplits = currentLeaf.availableSplits;     
            // If there are no possible splits, put the leaf back and try again.
            if ( availableSplits.length == 0 ) { leafList.add(currentLeaf); continue; }
            int splitVar = availableSplits[ rand.nextInt(availableSplits.length) ];
            
            // If this split would give us too many leaves, tree is split far enough already.
            if ( arity[splitVar] + leafList.size() > numLeaves ) { 
                // If all vars have been split upon, we are done.
                if ( unsplitVars == 0 ) {
                    leafList.add(currentLeaf); break;
                }        
                // If a different variable needs to be split upon, put leaf back and try again.
                else if ( unsplitVars != 0 && varHasBeenSplit[splitVar] == true ) {
                    leafList.add(currentLeaf); continue;
                }
                // else continue splitting this variable even though it will give us
                // excess leaves.
            }
            
            if ( varHasBeenSplit[ splitVar] == false ) {
                unsplitVars --;
                varHasBeenSplit[ splitVar ] = true;
            }
            currentLeaf.splitOn( splitVar );
            leafList.addAll( currentLeaf.children );
            splitList.add( currentLeaf );
            
            if ( unsplitVars == 0 && leafList.size() >= numLeaves ) { break; }
        }
        
        Value[] tempArray = new Value[leafList.size()];
        for ( int i = 0; i < tempArray.length; i++ ) {
            tempArray[i] = MultinomialGenerator.generate(rand,childArity);
        }
        Value.Vector leafParamVec = new VectorFN.FatVector(tempArray);
        
        // Now we need to populate each leaf with a distribution and set of parameters
        for ( int j = 0; j < leafList.size(); j++ ) {
            TreeNode currentLeaf = leafList.get(j);
            Value.Structured currentStruct = (Value.Structured)leafParamVec.elt(j);
            currentLeaf.model = (Value.Model)currentStruct.cmpnt(0);
            currentLeaf.params = currentStruct.cmpnt(1);
        }
        return treeRoot.makeParams();
        
    }
    
    /**
     * Does the real work of the generate function.  We use a java.util.Random as a seed instead
     * of an int.  
     * y = ( [parentArity], leafProb, childArity, splitOnAll )
     */
    public Value.Vector generate( java.util.Random rand, Value y, Value.Vector z ) {
        
        
        Value.Vector parentArityVec = (Value.Vector)((Value.Structured)y).cmpnt(0);
        int childArity = ((Value.Scalar)((Value.Structured)y).cmpnt(2)).getDiscrete();
        // int numLeaves = ((Value.Scalar)((Value.Structured)y).cmpnt(1)).getDiscrete();
        double leafProb = ((Value.Continuous)((Value.Structured)y).cmpnt(1)).getContinuous();
        Value.Discrete split = (Value.Discrete)((Value.Structured)y).cmpnt(3);
        
        final boolean splitOnAll;
        if ( split == Value.TRUE ) splitOnAll = true;
        else if ( split == Value.FALSE ) splitOnAll = false;
        else { throw new RuntimeException("Boolean value expected, found : " + split); }
        
        int[] arity = new int[parentArityVec.length()];
        for ( int i = 0; i < arity.length; i++ ) {
            arity[i] = parentArityVec.intAt(i);
        }
        
        Value[] treeArray = new Value[z.length()];
        for ( int i = 0; i < treeArray.length; i++ ) {
            treeArray[i] = generate( rand, childArity, arity, leafProb, splitOnAll );
        }
        
        return new VectorFN.FatVector( treeArray );
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
    
    /** returns a representation of the DTreeGenerator */
    public String toString()
    {
        return "DTreeGenerator";
    } 
}
