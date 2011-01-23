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

package camml.core.library.extensionCounter;

import java.util.Random;

import camml.core.search.TOM;

public abstract class UnlabelledGraph {

    /** Number of nodes represented by graph. */
    public int numNodes;

    
    /** Accessor for connected */
    public int[] getConnected() { return connected; }

    /** Connectivity of each node. (#of arcs into or out of node) */
    protected int[] connected;

    /** If immutable == true && overwrite parameter == true we are free to overwrite
     *  this graph.  This flag is required so ancestors in recursive calls can disallow
     *  overwriting. */
    public boolean immutable = false;

    public UnlabelledGraph( int n ) {            
        this.numNodes = n;
        initHash(n);
    }
        
    
    protected static long[][] nodeHash;
    protected static Random rand = new Random(123);
    /** Initialise random n*n array of longs used to hash DAGs */
    protected static void initHash( int n )
    {
        if (nodeHash == null) { nodeHash = new long[0][0]; }
        if (n <= nodeHash.length) { return; }
        long newHash[][] = new long[n][n]; 
        for ( int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++ ) {
                
                if (i < nodeHash.length && j < nodeHash.length) { 
                    newHash[i][j] = nodeHash[i][j]; 
                }
                else { 
                    newHash[i][j] = rand.nextLong(); 
                }
            }
        }    
        nodeHash = newHash;
    }

    /** Initialise UnlabelledGraph based on the given TOM */
    public void initFromTOM( TOM tom ) {
        for ( int i = 0; i < numNodes; i++) {
            for ( int j = 0; j < numNodes; j++) {
                if (i!=j && tom.isDirectedArc(tom.nodeAt(i),tom.nodeAt(j))) {
                    this.addArc(i,j,true);
                }
            }
        }
    }
    
    /** Return a hash of this DAG. */
    public long getHash() {
        long hash = 0;
        for (int child = 0; child < numNodes; child++) {
            int[] parentArray = getParents(child);
            for (int parent : parentArray ) {
                hash += nodeHash[child][parent];
            }
        }
        return hash;            
    }

    public abstract int getNumLeafNodes();
    public abstract int getNumRootNodes();
    public abstract int getNumDisconnectedNodes();
    
    public abstract int[] getLeafNodeArray();
    public abstract int[] getRootNodeArray();
    public abstract int[] getDisconnectedNodeArray();
    
    public abstract int[] getParents(int node);
    public abstract int[] getChildren(int node);

    public abstract long getCanonicalHash();

    public abstract void addArc(int parent, int child, boolean addImpliedLinks);
    public abstract UnlabelledGraph removeNode(int node, boolean overwrite);

    public abstract int[] getSubgraphNodeArray(int node);
    public abstract boolean isDirectedArc(int parent, int child);
}
