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

import camml.core.models.dTree.CatlanTable;
import camml.core.search.CoreTOM;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.Value.Function;
import cdms.core.VectorFN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author rodo
 *         <p/>
 *         EnumerateDAGs enumerates all DAG structures as a 2d vector [[]].
 *         As the number of DAGs for n variables is super-exponential, only small values of n (ie <= 5)
 *         should be used.
 *         <p/>
 *         Discrete -> [[Boolean]]
 */
public class EnumerateDAGs extends Function {

    /**
     * Serial ID required to evolve class while maintaining serialisation compatibility.
     */
    private static final long serialVersionUID = 7790361056481007328L;

    /**
     * Vector implementation which mimics a 2d array
     */
    public static class CoreTOMArcVec extends Vector {
        /**
         * Serial ID required to evolve class while maintaining serialisation compatibility.
         */
        private static final long serialVersionUID = -8024553631912299577L;
        /**
         * Matrix holding the data.
         */
        protected final int[][] arcMatrix;

        /**
         * Create a 2d vector [[]] based on the arcs present in tom
         */
        public CoreTOMArcVec(CoreTOM tom) {
            super(new Type.Vector(new Type.Vector(Type.BOOLEAN)));
            int n = tom.getNumNodes();
            arcMatrix = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (tom.isArc(i, j) && (tom.getNodePos(i) < tom.getNodePos(j))) {
                        arcMatrix[i][j] = 1;
                    }
                }
            }
        }

        /**
         * return length of vector
         */
        public int length() {
            return arcMatrix.length;
        }

        /**
         * return vector of length numNodes containing boolean vector of all parents of node(i)
         */
        public Value elt(int i) {
            return new VectorFN.FastDiscreteVector(arcMatrix[i]);
        }
    }


    /**
     * enumerateGraphs will not run for n >= maxGraphSize (default = 7)
     */
    public final static int maxGraphSize = 6;

    /**
     * Function type.
     */
    protected static final Type.Function tt = new Type.Function(Type.DISCRETE, Type.VECTOR);

    /**
     * Static instance of class.
     */
    // NOTE: This must be declared below tt or else tt will still be null when 
    // enumerateDAGs declared.
    public static final EnumerateDAGs enumerateDAGs = new EnumerateDAGs();

    /**
     * Constructor
     */
    public EnumerateDAGs() {
        super(tt);
    }

    /**
     * Return a vector of all possible DAGs with given number of variables.
     */
    public Value apply(Value v) {
        return _apply(((Value.Discrete) v).getDiscrete());
    }

    /**
     * Shortcut to apply()
     */
    public Value.Vector _apply(int numVars) {
        CoreTOM[] dagArray = enumerateDAGs(numVars);

        Value.Vector[] vecArray = new Value.Vector[dagArray.length];
        for (int i = 0; i < vecArray.length; i++) {
            vecArray[i] = new CoreTOMArcVec(dagArray[i]);
        }
        return new VectorFN.FatVector(vecArray);
    }


    /**
     * IncrementOrdering increments a varable ordering [2,3,1,0] -> [2,3,0,1],
     * n! calls return array to original value passing through all orderings on the way.
     */
    public static int[] incrementOrder(int[] order) {
        // algorithm: Search from back uptil digit[n] > digit[n-1]
        //               swap digit[n] with next highest value from remaining list.
        //            sort remaining list.
        for (int n = order.length - 2; n >= 0; n--) {
            if (order[n] < order[n + 1]) {
                // choose nextHighest as index of value with minumum possible increase
                int nextHighest = n + 1;
                for (int i = n + 1; i < order.length; i++) {
                    if ((order[i] > order[n]) && (order[i] < order[nextHighest])) {
                        nextHighest = i;
                    }
                }
                // Swap(order[n], order[nextHighest])
                int temp = order[n];
                order[n] = order[nextHighest];
                order[nextHighest] = temp;

                // sort remainder of array.
                Arrays.sort(order, n + 1, order.length);
                break;
            }
            if (n == 0) {
                Arrays.sort(order);
            }
        }

        return order;
    }

    /**
     * Define n!
     */
    protected int factorial(int n) {
        if (n == 0) {
            return 1;
        } else return n * factorial(n - 1);
    }

    /**
     * Define number of DAGs for N variables
     * \[ a_n = \sum_{k=1}^n (-1)^{k-1} {n \choose k} 2^{k(n-k)} a_{n-k} \]
     */
    public static int numDAGs(int n) {
        if (n < 0) return 0;
        if (n < 2) return 1;
        long sum = 0;
        for (int k = 1; k <= n; k++) {
            long nCr = (long) CatlanTable.nCr(n, k);
            sum = sum + ((long) Math.pow(-1, k - 1)) * nCr * (long) Math.pow(2, k * (n - k)) * numDAGs(n - k);
        }
        return (int) sum;
    }


    /**
     * Enumerate and return all DAGs with given number of variables
     * This should not be called with n > 5 as number of dags is super-exponential.
     */
    public CoreTOM[] enumerateDAGs(int numVars) {
        // Begin with an enumerated array of all undirected graphs
        CoreTOM[] graphArray = enumerateGraphs(numVars);
        int nFact = factorial(numVars);

        // Create an array to store DAGs in.
        CoreTOM[] dagArray = new CoreTOM[numDAGs(numVars)];
        // Index to next free element of dagArray.
        int dagArrayIndex = 0;

        // For each TOM in graphArray enumerate all n! orderings
        for (CoreTOM undirectedGraph : graphArray) {
            // Initialise total ordering.
            int[] order = new int[numVars];
            for (int j = 0; j < numVars; j++) {
                order[j] = j;
            }

            // Create hashSet to hold all permutations.
            // Using a hashSet automatically removes duplicates and being a hashtable
            // is is hopefully reasonably efficient.
            HashSet<CoreTOM> perms = new HashSet<CoreTOM>();  // n! possible permutations.

            // Attempt to add each of n! orderings to HashSet
            for (int j = 0; j < nFact; j++) {
                // Create a list of N! TOMs.
                CoreTOM tom = new CoreTOM(undirectedGraph);
                // set order of TOM
                for (int k = 0; k < order.length; k++) {
                    tom.swapOrder(tom.nodeAt(k), order[k]);
                }
                perms.add(tom);
                incrementOrder(order);
            }

            // perms should contain all unique DAGs for a given undirected graph.
            CoreTOM[] permsArray = perms.toArray(new CoreTOM[perms.size()]);
            for (int j = 0; j < permsArray.length; j++) {
                dagArray[dagArrayIndex] = permsArray[j];
                dagArrayIndex++;
            }
        }
        return dagArray;
    }

    /**
     * Enumerate and return a list of all undirected graphs with numVars variables.
     */
    public CoreTOM[] enumerateGraphs(int numVars) {
        if (numVars == 0) {
            return new CoreTOM[0];
        }
        if (numVars >= maxGraphSize) {
            throw new RuntimeException("Too many vars, limit is " + maxGraphSize);
        }

        // Calculate number of undirected graphs
        final int numEdges = (numVars * (numVars - 1) / 2);
        final int numGraphs = 1 << numEdges;

        // Initialise graphList to be a single TOM.
        // We must create a fake Dataset for TOM (which is unused).
        ArrayList<CoreTOM> graphList = new ArrayList<CoreTOM>(numGraphs);
        graphList.add(new CoreTOM(numVars, numVars));


        // x and y are used as an index into the list of all edges.
        int x = 0;
        int y = 0;
        for (int i = 0; i < numEdges; i++) {
            int size = graphList.size();

            // figure out which edge to add.  This is a bit tricky as edge matrix is triangular.
            do {
                y++;
                if (y == numVars) {
                    y = 0;
                    x++;
                }
            } while (x >= y);

            for (int j = 0; j < size; j++) {
                CoreTOM t = new CoreTOM(graphList.get(j));
                t.addArc(x, y);
                graphList.add(t);
            }
        }

        // Return array of graphs.
        return graphList.toArray(new CoreTOM[numGraphs]);
    }
}
