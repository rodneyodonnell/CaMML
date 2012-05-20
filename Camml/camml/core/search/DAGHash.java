/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Lucas Hope, Rodney O'Donnell
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
// TOM hashing class for CaMML
//

// File: DAGHash.java
// Author: {rodo,lhope}@csse.monash.edu.au

package camml.core.search;


/**
 * A unique DAG hash is formed by
 * 1) Creating a N*N array of random numbers
 * 2) For each arc present, sum the values of these numbers
 * <p/>
 * This differs from a SEC hashing in that the direction of the arcs should distinguish
 * between two TOMs.  This method does not require a ML scores.
 */
public class DAGHash extends ModelHash {
    /**
     * Serial ID required to evolve class while maintaining serialisation compatibility.
     */
    private static final long serialVersionUID = 4220042542582625181L;

    /**
     * Random number generator
     */
    private java.util.Random rand;

    /**
     * Random n*n matrix used to create hash
     */
    private long[][] matrix;

    /**
     * Constructor
     */
    public DAGHash(java.util.Random rand, int numNodes) {
        this.rand = rand;
        init(numNodes);
    }

    /**
     * Initialise 64bit random matrix.
     */
    protected void init(int numNodes) {
        matrix = new long[numNodes][numNodes];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = rand.nextLong();
            }
        }
    }

    /**
     * Accessor function used so hash can be calculated incrementally in NodeCache
     */
    public long getRandom(int x, int y) {
        return matrix[x][y];
    }

    /**
     * hash = sum_{directedArcs}( matrix[i][j] )
     */
    // TODO: hash(TOM) is being replaced with hash(CoreTom) so this can go away soon?
    public long hash(TOM tom, double logL) {
        long skelHash = 0L;
        int numNodes = tom.getNumNodes();

        // The calculation of tomHash is now performed locally in nodes (the sum is the tomHash)
        for (int i = 0; i < numNodes; i++) {
            for (int parent : tom.getParents(i)) {
                skelHash += getRandom(parent, i);
            }
        }

        return skelHash;
    }

    /**
     * hash = sum_{directedArcs}( matrix[i][j] )
     */
    public long hash(CoreTOM tom, double logL) {
        long skelHash = 0L;
        int numNodes = tom.getNumNodes();

        // The calculation of tomHash is now performed locally in nodes (the sum is the tomHash)
        for (int i = 0; i < numNodes; i++) {
            for (int parent : tom.getParents(i)) {
                skelHash += getRandom(parent, i);
            }
        }

        return skelHash;
    }

}
