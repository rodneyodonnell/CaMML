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
// Functions to learn DTrees's from raw data.
//

// File: DTreeLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.dTree;

import cdms.core.*;
import camml.core.models.ModelLearner;
import camml.core.models.multinomial.MultinomialLearner;

import camml.core.library.DTreeSelectedVector;

/**
 * DTreeLearner is a standard module for parameterizing and costing DTrees. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br> 
 * <br>
 * A DTree is learned by splitting the data based on it's parents, then using a "leafLearner" 
 * to parameterize the date given each parent combination (This is the same as each leaf of a fully
 * split decision tree).  The ModelLearner passed in the constructor is used to parameterize each 
 * leaf and must have a "shared space" (aka. parentSpace or z) of Value.TRIV
 */
public class DTreeLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -9097992381856737052L;
    /** Static instance of DTree using Multistates in the leaves */
    public static DTreeLearner multinomialDTreeLearner = 
        new DTreeLearner( MultinomialLearner.multinomialLearner );
    
    public String getName() { return "DTreeLearner"; }    
    
    /** ModelLearner used to cost leaves of the tree. */
    ModelLearner leafModelLearner;
    
    /** Default constructor same as DTreeLearner( MultinomialLearner.multinomialLearner )*/
    public DTreeLearner()
    {
        super( makeModelType(), Type.TRIV );
        this.leafModelLearner = MultinomialLearner.multinomialLearner;
    }
    
    public DTreeLearner( ModelLearner leafModelLearner )
    {
        super( makeModelType(), Type.TRIV );
        this.leafModelLearner = leafModelLearner;
    }
    
    protected static Type.Model makeModelType( )
    {
        //Type.Model subModelType = Type.MODEL;
        Type dataSpace = Type.DISCRETE;
        Type paramSpace = Type.VECTOR;
        Type sharedSpace = Type.STRUCTURED;
        Type sufficientSpace = new Type.Structured( new Type[] {dataSpace, sharedSpace} );
        
        return new Type.Model(dataSpace, paramSpace, sharedSpace, sufficientSpace);
        
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        if ( z.length() != z.length() ) {
            throw new RuntimeException("Length mismatch in DTreeLearner.parameterize");
        }
        
        double oneBit = Math.log(2);
        
        // Best parameters and best cost initialised to that of a leaf node.
        Value.Structured bestMSY = leafModelLearner.parameterize( initialInfo, x, z );
        
        // add 1 as structure cost to state leaf exists.
        double bestCost = leafModelLearner.msyCost( bestMSY ) + oneBit;
        int bestSplit = -1;
        
        // Find the number of splits which must be tested from this node.
        Type.Structured inputType = (Type.Structured)((Type.Vector)z.t).elt;
        int numVars = inputType.cmpnts.length;
        
        // if there are no variables to split on stating we don't split is redundant and inefficient
        // we can remove the bit we used to state this before.
        if ( numVars == 0 ) {
            bestCost -= oneBit;
        }
        
        //     System.out.println("-----------------------------------------------");
        //     System.out.println("Finding best split on inputType = " + inputType );
        //     for ( int i = 0; i < 16; i++ ) {
        //         System.out.println("Sample ["+i+"]: " + z.elt(i) + " ----> " + x.elt(i));
        //     }
        // For each variable it is possible to split upon test the cost of a split.
        // (lookahead = 0)
        for ( int i = 0; i < numVars; i++ ) {
            
            //         System.out.println("Splitting on : " + i);
            
            // Split the input & output vector into splitX[] and splitZ[] respectively.
            DTreeSelectedVector[] splitZ = DTreeSelectedVector.d_splitVector( z, i, true );
            DTreeSelectedVector[] splitX = new DTreeSelectedVector[splitZ.length];
            for ( int j = 0; j < splitZ.length; j++ ) {
                splitX[j] = splitZ[j].d_copyRowSplit( x );
            }
            
            // number of splits = arity of data
            //int arity = splitX.length;
            
            // structure cost = log(numVars) + 1 see WallacePatrick.
            // 1 bit to state this is a split node, log(numVars) to state what is being split upon.
            double structureCost =  Math.log(numVars) + oneBit;
            
            // Find total cost (& parameters) of stating all subtrees.
            double subTreeCost = 0;
            //Value.Structured[] msy = new Value.Structured[ arity ];
            for ( int j = 0; j < splitX.length; j++ ) {
                // add cost for each leaf +1 for each node to state structure.
                subTreeCost += 
                    leafModelLearner.msyCost( leafModelLearner.parameterize(initialInfo, 
                                                                            splitX[j], 
                                                                            splitZ[j]) )  + oneBit;
                //         System.out.println("subX["+i+"]["+j+"] = " + splitX[j]);
                //         System.out.println("subZ["+i+"]["+j+"] = " + splitZ[j]);
            }
            
            double totalCost = structureCost + subTreeCost;
            //         System.out.println("cost["+i+"] = " + totalCost);
            
            if ( totalCost < bestCost ) {
                bestCost = totalCost;
                bestSplit = i;
            }
        }        
        
        //     System.out.println("Best split found at : " + bestSplit);
        
        // if a good split was found.
        if ( bestSplit != -1) {
            DTreeSelectedVector[] splitZ = DTreeSelectedVector.d_splitVector( z, bestSplit, true );
            DTreeSelectedVector[] splitX = new DTreeSelectedVector[splitZ.length];
            for ( int j = 0; j < splitZ.length; j++ ) {
                splitX[j] = splitZ[j].d_copyRowSplit( x );
            }
            
            // number of splits = arity of data
            int arity = splitX.length;
            
            // Make a split's name human readable.
            // NOTE: There is problem using "i" as the name.  If variables are split upon
            // and as such removed from the list of valid variables, each variables numbering
            // will change.
            final String splitName; 
            if ( inputType.labels == null ) {
                splitName = "var("+bestSplit+")";  
            }
            else {
                splitName  = inputType.labels[bestSplit];
            }
            Value.Discrete splitAttribute = new Value.Discrete( bestSplit ) {
                    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
                    private static final long serialVersionUID = -5706670701072353793L;

                    public String toString() {return splitName;}
                };
            
            // Now we recurse deeper to find thr true best cost/parameters.
            // structure cost = log(numVars) + log(arity) see WallacePatrick.
            double structureCost =  Math.log(numVars) + oneBit;
            
            // Find total cost (& parameters) of stating all subtrees.
            double subTreeCost = 0;
            Value.Structured[] msy = new Value.Structured[ arity ];
            for ( int j = 0; j < splitX.length; j++ ) {
                msy[j] = parameterize(initialInfo, splitX[j], splitZ[j]);
                subTreeCost += msyCost( msy[j] );
            }
            
            double totalCost = structureCost + subTreeCost;
            
            
            
            
            Value.Continuous cost = new Value.Continuous( totalCost );
            
            Value paramArray[] = new Value[arity];
            for ( int j = 0; j < paramArray.length; j++ ) { 
                paramArray[j] = msy[j].cmpnt(2);
            }
            Value.Vector paramVector = new VectorFN.FatVector( paramArray );
            
            
            // The parameters of the tree.  MML cost is included here to simplify matters
            // elsewhere.  This is a bit of a fudge.
            Value.Structured treeParams = 
                new Value.DefStructured( new Value[] {splitAttribute, cost, paramVector},
                                         new String[] {"splitAttribute", "cost", 
                                                       "paramVector"} );
            
            
            bestMSY = new Value.DefStructured( new Value[] { DTree.dTree, 
                                                             DTree.dTree.getSufficient(x,z), 
                                                             treeParams },
                new String[] { "DTree", "Stats", "Params"} );
        }
        // if the best model found is a leaf, make a DTree containing just a leaf.
        else {
            Value.Discrete splitAttribute = new Value.Discrete( -1 );
            Value.Continuous cost = new Value.Continuous( bestCost );
            Value.Structured leafParams = new Value.DefStructured( new Value[] {
                    bestMSY.cmpnt(0), bestMSY.cmpnt(2) });
            Value.Structured treeParams = 
                new Value.DefStructured( new Value[] { splitAttribute, cost, leafParams},
                                         new String[] {"splitAttribute", "cost", "params"});
            bestMSY = new Value.DefStructured( new Value[] { DTree.dTree, 
                                                             DTree.dTree.getSufficient(x,z),
                                                             treeParams},
                new String[] {"DTree", "Stats", "Params"} );
        }
        
        
        
        return bestMSY;
    }
    
    public boolean rootNodeFlag = false;
    
    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        if ( z.length() != z.length() ) {
            throw new RuntimeException("Length mismatch in DTreeLearner.parameterize");
        }
        
        boolean rootNode = false;
        if ( rootNodeFlag == false ) {
            rootNode = true;
            rootNodeFlag = true;
        }
        
        
        double oneBit = Math.log(2);
        
        double bestCost = leafModelLearner.parameterizeAndCost(initialInfo,x,z) + oneBit;
        int bestSplit = -1;
        
        // Find the number of splits which must be tested from this node.
        Type.Structured inputType = (Type.Structured)((Type.Vector)z.t).elt;
        int numVars = inputType.cmpnts.length;
        
        // if there are no variables to split on stating we don't split is redundant and inefficient
        // we can remove the bit we used to state this before.
        if ( numVars == 0 ) {
            bestCost -= oneBit;
        }
        
        if ( rootNode && numVars != 0 ) {
            bestCost = Double.POSITIVE_INFINITY;
        }
        
        for ( int i = 0; i < numVars; i++ ) {
            // Split the input & output vector into splitZ[] and splitZ[] respectively.
            DTreeSelectedVector[] splitZ = DTreeSelectedVector.d_splitVector( z, i, true );
            DTreeSelectedVector[] splitX = new DTreeSelectedVector[splitZ.length];
            for ( int j = 0; j < splitZ.length; j++ ) {
                splitX[j] = splitZ[j].d_copyRowSplit( x );
            }
            
            // number of splits = arity of data
            //int arity = splitX.length;
            
            // structure cost = log(numVars) + 1 see WallacePatrick.
            // 1 bit to state this is a split node, log(numVars) to state what is being split upon.
            double structureCost =  Math.log(numVars) + oneBit;
            
            // Find total cost (& parameters) of stating all subtrees.
            double subTreeCost = 0;
            for ( int j = 0; j < splitX.length; j++ ) {
                // add cost for each leaf +1 for each node to state structure.
                subTreeCost += 
                    leafModelLearner.parameterizeAndCost( initialInfo, splitX[j], splitZ[j] ) + oneBit;
            }
            
            double totalCost = structureCost + subTreeCost;
            
            if ( totalCost < bestCost ) {
                bestCost = totalCost;
                bestSplit = i;
            }
        }        
        
        // if a good split was found.
        if ( bestSplit != -1) {
            DTreeSelectedVector[] splitZ = DTreeSelectedVector.d_splitVector( z, bestSplit, true );
            DTreeSelectedVector[] splitX = new DTreeSelectedVector[splitZ.length];
            for ( int j = 0; j < splitZ.length; j++ ) {
                splitX[j] = splitZ[j].d_copyRowSplit( x );
            }
            
            // number of splits = arity of data
            //int arity = splitX.length;
            
            // Now we recurse deeper to find thr true best cost/parameters.
            // structure cost = log(numVars) + log(arity) see WallacePatrick.
            double structureCost =  Math.log(numVars) + oneBit;
            
            // Find total cost (& parameters) of stating all subtrees.
            double subTreeCost = 0;
            //Value.Structured[] msy = new Value.Structured[ arity ];
            for ( int j = 0; j < splitX.length; j++ ) {
                // ??? do I need to add oneBit to this?
                subTreeCost += parameterizeAndCost(initialInfo, splitX[j], splitZ[j]);
            }
            
            bestCost = structureCost + subTreeCost;        
        }
        
        if ( rootNode == true ) {
            rootNodeFlag = false;
            if (bestSplit == -1 ) {
                //             System.out.println("bestSplit = " + bestSplit + "\tbestCost = " + bestCost 
                //                         + "\tnumParents = " + numVars );
                emptyTrees[numVars]++;
                totalEmptyTrees ++;
                
                if ( totalEmptyTrees % 1000 == 0 ) { 
                    System.out.println("empty Trees : " + totalEmptyTrees );
                    for ( int i = 0; i < emptyTrees.length; i++ ) {
                        System.out.print("" + emptyTrees[i] + "\t");
                    }
                    System.out.println();
                }
            }
        }
        //     else {
        //         System.out.println("nonRoot bestSplit = " + bestSplit + "\tnumVars = " + numVars);
        //     }
        
        return bestCost;
    }
    
    
    int[] emptyTrees = new int[11];
    int totalEmptyTrees = 0;
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
        throws LearnerException
    {    
        return parameterize( Value.TRIV, 
                             (Value.Vector)((Value.Structured)s).cmpnt(0), 
                             (Value.Vector)((Value.Structured)s).cmpnt(1) );
    }
    
    
    
    /**
     * return cost.  This is read directly out of parameters.  Ideally it should be calculated
     * using parameters and data as currently it entirely ignores data.
     */
    public double cost(Value.Model m, Value initialInfo, Value.Vector x, Value.Vector z, Value y)
        throws LearnerException
    {    
        if ( m instanceof DTree ) {
            Value.Structured params = (Value.Structured)y;
            double cost = params.doubleCmpnt(1);
            return cost;
        }
        else {
            return leafModelLearner.cost( m, initialInfo, x, z, y );
        }
    } 
    
    /**
     * This function ignores the parameters given and costs using the costing method from
     * Camml Classic discrete. <br>
     *
     *
     */
    public double sCost( Value.Model m, Value s, Value y )
        throws LearnerException
    {
        if ( m instanceof DTree ) {
            Value.Structured params = (Value.Structured)y;
            double cost = params.doubleCmpnt(1);
            return cost;
        }
        else {
            return leafModelLearner.sCost( m, s, y );
        }
    }
    
    public String toString() { return "DTreeLearner("+leafModelLearner+")"; }    
}

