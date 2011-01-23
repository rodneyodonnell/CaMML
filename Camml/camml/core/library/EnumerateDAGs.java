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
// Perform functions associated with DAG enumeration
//

// File: EnumerateDAGs.java
// Author: rodo@dgs.monash.edu.au
// Created on 8/02/2005

package camml.core.library;

import java.util.ArrayList;

import cdms.core.Value;
import cdms.core.VectorFN;
import cdms.core.Value.Function;
import cdms.core.Type;
import camml.core.models.dTree.CatlanTable;
import camml.core.search.NodeCache;
import camml.core.search.SearchDataCreator;
import camml.core.search.TOM;
import camml.core.search.TOMHash;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 * @author rodo
 *
 * EnumerateDAGs enumerates all DAG structures as a 2d vector [[]].
 * As the number of DAGs for n variables is super-exponential, only small values of n (ie <= 5)
 * should be used.
 * 
 * Discrete -> [[Boolean]]
 * */
public class EnumerateDAGs extends Function {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7790361056481007328L;

    /** Vector implementation which mimics a 2d array */
    public static class TOMArcVec extends Vector {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -8024553631912299577L;
        /** Matrix holding the data.*/
        protected final int[][] arcMatrix;;
        
        /** Create a 2d vector [[]] based on the arcs present in tom */
        public TOMArcVec( TOM tom ) {
            super( new Type.Vector( new Type.Vector( Type.BOOLEAN ) ) );
            int n = tom.getNumNodes();
            arcMatrix = new int[n][n];
            for ( int i = 0; i < n; i++ ) {
                for ( int j = 0; j < n; j++ ) {
                    if ( tom.isArc(i,j) && (tom.getNodePos(i) < tom.getNodePos(j)) ) { 
                        arcMatrix[i][j] = 1;
                    }
                }
            }
        }
        
        /** return length of vector */
        public int length() { return arcMatrix.length; }
        
        /** return vector of length numNodes containing boolean vector of all parents of node(i) */
        public Value elt(int i) {
            return new VectorFN.FastDiscreteVector( arcMatrix[i] );
        }
    }
    
    
    /** enumerateGraphs will not run for n >= maxGraphSize (default = 7) */
    public final static int maxGraphSize = 6;
    
    /** Function type. */
    protected static final Type.Function tt = new Type.Function( Type.DISCRETE, Type.VECTOR );
    
    /** Static instance of class. */
    // NOTE: This must be declared below tt or else tt will still be null when 
    // enumerateDAGs declared.
    public static final EnumerateDAGs enumerateDAGs = new EnumerateDAGs();
    
    /** Constructor */
    public EnumerateDAGs() { super(tt); }
    
    /**    Return a vector of all possible DAGs with given number of variables. */
    public Value apply(Value v) {
        return _apply( ((Value.Discrete)v).getDiscrete() );
    }
    
    /** Shortcut to apply() */
    public Value.Vector _apply( int numVars ) {
        TOM[] dagArray = enumerateDAGs(numVars);
        
        Value.Vector[] vecArray = new Value.Vector[ dagArray.length ];
        for ( int i = 0; i < vecArray.length ; i++ ) {
            vecArray[i] = new TOMArcVec( dagArray[i] );
        } 
        return new VectorFN.FatVector( vecArray );
    }
    
    
    
    /** IncrementOrdering increments a varable ordering [2,3,1,0] -> [2,3,0,1], 
     *  n! calls return array to original value passing through all orderings on the way. */
    public static int[] incrementOrder( int[] order ) {
        // algorithm: Search from back uptil digit[n] > digit[n-1]
        //               swap digit[n] with next highest value from remaining list.
        //            sort remaining list.
        for ( int n = order.length-2; n >=0; n-- ) {
            if ( order[n] < order[n+1] ) {
                // choose nextHighest as index of value with minumum possible increase
                int nextHighest = n+1;
                for ( int i = n+1; i < order.length; i++ ) {
                    if ( (order[i] > order[n]) && (order[i] < order[nextHighest]) ) {
                        nextHighest = i;
                    }
                }
                // Swap(order[n], order[nextHighest])
                int temp = order[n]; order[n] = order[nextHighest]; order[nextHighest] = temp;
                
                // sort remainder of array.
                Arrays.sort( order, n+1, order.length);
                break;
            }
            if ( n == 0 ) { Arrays.sort( order ); }
        }
        
        return order;
    }
    
    /** Define n! */
    protected int factorial( int n ) { if (n == 0) {return 1;} else return n * factorial(n-1); }
    
    /** Define number of DAGs for N variables 
     * \[ a_n = \sum_{k=1}^n (-1)^{k-1} {n \choose k} 2^{k(n-k)} a_{n-k} \] */
    public static int numDAGs( int n ) {
        if ( n < 0 ) return 0;
        if ( n < 2 ) return 1;
        long sum = 0;
        for ( int k = 1; k <= n; k++ ) {
            long nCr = (long)CatlanTable.nCr(n,k);
            sum = sum + ((long)Math.pow(-1,k-1)) * nCr * (long)Math.pow(2,k*(n-k)) * numDAGs(n-k);
        }
        return (int)sum;
    }
    
    
    /** Enumerate and return all DAGs with given number of variables 
     *  This should not be called with n > 5 as number of dags is super-exponential. */
    public TOM[] enumerateDAGs( int numVars ) {
        // Begin with an enumerated array of all undirected graphs
        TOM[] graphArray = enumerateGraphs( numVars );
        int nFact = factorial(numVars);
        
        // Create an array to store DAGs in.
        TOM[] dagArray = new TOM[numDAGs(numVars)];
        // Index to next free element of dagArray.
        int dagArrayIndex = 0;
        
        // For each TOM in graphArray enumerate all n! orderings
        for ( int i = 0; i < graphArray.length; i++ ) {
            // Initialise total ordering.
            int[] order = new int[numVars];
            for ( int j = 0; j < numVars; j++ ) {
                order[j] = j;    
            }
            
            // Create hashSet to hold all permutations.
            // Using a hashSet automatically removes duplicates and being a hashtable
            // is is hopefully reasonably efficient.
            HashSet<TOM> perms = new HashSet<TOM>( );  // n! possible permutations.
            
            // Attempt to add each of n! orderings to HashSet
            for ( int j = 0; j < nFact; j++ ) {
                // Create a list of N! TOMs.
                TOM tom = (TOM)graphArray[i].clone();
                // set order of TOM
                for ( int k = 0; k < order.length; k++ ) {
                    tom.swapOrder( tom.nodeAt(k), order[k], true );
                }
                perms.add( tom );
                incrementOrder(order);
            }
            
            // perms should contain all unique DAGs for a given undirected graph.
            TOM[] permsArray = (TOM[])perms.toArray(new TOM[perms.size()]);
            for ( int j = 0; j < permsArray.length; j++ ) {
                dagArray[dagArrayIndex] = permsArray[j];
                dagArrayIndex ++;
            }
        }        
        return dagArray;
    }
    
    /** Enumerate and return a list of all undirected graphs with numVars variables. */
    public TOM[] enumerateGraphs( int numVars ) {
        if ( numVars == 0 ) { return new TOM[0]; }
        if ( numVars >= maxGraphSize ) { 
            throw new RuntimeException("Too many vars, limit is "+maxGraphSize); 
        }
        
        // Calculate number of undirected graphs
        final int numEdges = (numVars * (numVars-1) / 2);
        final int numGraphs = 1 << numEdges;
        
        // Initialise graphList to be a single TOM.
        // We must create a fake Dataset for TOM (which is unused).
        ArrayList<TOM> graphList = new ArrayList<TOM>(numGraphs);
        Value.Vector data = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new Random(123), 1, numVars, 1, 1 );
        TOM tempTOM = new TOM(data);        
        tempTOM.caseInfo.tomHash = new TOMHash(new Random(123), numVars);
        tempTOM.caseInfo.tomHash.caseInfo = tempTOM.caseInfo;
        tempTOM.caseInfo.nodeCache = new NodeCache(data, null, null);
        tempTOM.caseInfo.nodeCache.caseInfo = tempTOM.caseInfo;
        graphList.add( tempTOM );
        
        
        // x and y are used as an index into the list of all edges.
        int x = 0;
        int y = 0;
        for ( int i = 0; i < numEdges; i++ ) {
            int size = graphList.size();
            
            // figure out which edge to add.  This is a bit tricky as edge matrix is triangular.
            do { 
                y++;
                if ( y == numVars ) {y = 0; x++; }
            } while (x >= y);
            
            for ( int j = 0; j < size; j++ ) {
                TOM t = (TOM)graphList.get(j).clone();
                t.addArc(x,y);
                graphList.add(t);
            }
        }
        
        // Return list of graphs.
        TOM[] graph = (TOM[])graphList.toArray( new TOM[numGraphs] );        
        return graph;
    }


    /** Randomly generate N dags each containing M variables.
     *  This is useful when a full enumeration is undesirable (or impossible). 
     *  DAGs are generated as TOMs with uniform priors over ordering, and given prior over arcs. 
     *  */
    public static TOM[] randomDAGs( java.util.Random rand, int numVars, int numDAGs, double arcProb ) {

        // We need a dummy dataset to create a TOM with.  Tells TOM how many variables it has.
        Value.Vector data = SearchDataCreator.generateWallaceKorbStyleDataset(new Random(123), 1, numVars, 1, 1 );
        
        TOM[] tomArray = new TOM[numDAGs];

        for ( int i = 0; i < tomArray.length; i++ ) {
            TOM t = new TOM(data);          // Create a new TOM
            t.randomOrder(rand);        // With a random variable ordering.
            t.randomArcs(rand,arcProb); // And random arcs.
            tomArray[i] = t;
        }

        return tomArray;
    }

    /** Convert an array of TOMs to a Vector of arcs. */
    public static Value.Vector tomArray2Vec( TOM[] tomArray ) {
        Value.Vector[] vecArray = new Value.Vector[ tomArray.length ];
        for ( int i = 0; i < vecArray.length ; i++ ) {
            vecArray[i] = new TOMArcVec( tomArray[i] );
        } 
        return new VectorFN.FatVector( vecArray );
    }

}
