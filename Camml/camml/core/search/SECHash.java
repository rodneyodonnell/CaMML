//
// SEC hashing class for CaMML
//
// Copyright (C) 2002 Lucas Hope, Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: SECHash.java
// Author: {lhope,rodo}@csse.monash.edu.au

package camml.core.search;


/**
   Notes on "learning linear causal models" signature making:
 
   1) Clean TOM of low-effect arcs (easy in linear, hard otherwise)... arc weights ala Jitnah?
 
   2) Form hash of skeleton and log-likelihood to index 2^16 array of counts.
 
   3) Hash value:
   a) KxK symmetric matrix of 32 bit constant random numbers representing the arcs.
   Only need upper-triangular portion, really.
   b) 32-bit skeleton signature is the sum of those numbers matching the arcs mod 2^32.
   c) Hash value is upper 16 bits + lower 16 bits of skeleton + 100*log-likelihood mod 2 ^ 16.
 
   Full reference:
   @InCollection{wallace1999,
   author =      {Wallace, C. S. and Korb, K. B.},
   title =      {Learning Linear Casual Models by MML Sampling},
   booktitle = {Causal Models and Intelligent Data Management},
   publisher = {Springer-Verlag},
   year =     1999,
   editor =     {Gammerman, A.}
 
   <br><br>
   Instead of producing the 16 bit hash described above, 64 bit numbers are used.  The end result
   is also a 64 bit number.
   }
 
*/
public class SECHash extends ModelHash {
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -5235268983525975491L;

    /** Random number generator */
    private java.util.Random rand;
    
    /** Random upper triangular matrix used in hashing function */
    private long[][] matrix;
    
    /** Constructor */
    public SECHash(java.util.Random rand, int numNodes) {
        this.rand = rand;
        init(numNodes);
    }
    
    /** Initialise triangular matrix with random 64bit numbers. */
    protected void init(int numNodes) {
        matrix = new long[numNodes - 1][];
        for(int i = 0; i < matrix.length; i ++) {
            matrix[i] = new long[i + 1];
            for(int j = 0; j < matrix[i].length; j ++)
                { matrix[i][j] = rand.nextLong() /*>> 32*/; }
        }
    }
    
    
    /** provide a standard index to the upper-triangular matrix. */
    public long getRandom(int x, int y)
    {
        int first, second;
        if(x > y) {
            first = x - 1;
            second = y;
        }
        else {
            first = y - 1;
            second = x;
        }
        
        return matrix[first][second];
    }
    
    /** hash = (int)(logL*128) + sum_{undirectedArcs}( matrix[i][j] ) */
    public long hash(TOM tom, double logL) {
        long skelHash = 0L;
        int numNodes = tom.getNumNodes();
        
        // The calculateion of tomHash is now performes locally in nodes (the sum is the tomHash)
        for (int i = 0; i < numNodes; i++) {
            int[] parent = tom.node[i].parent;
            for ( int j = 0; j < parent.length; j++) {
                skelHash += getRandom(parent[j],i);
            }
            //skelHash += caseInfo.nodeCache.getUndirectedHash( tom.node[i], tom );
        }

        return skelHash + (long)(logL * 128);
    }
    
}
