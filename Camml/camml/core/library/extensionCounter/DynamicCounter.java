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

import java.util.Hashtable;

/** Dynamic programming approach to Extension Counting. 
 *  Various operations are used in an attempt to reduce the branching factor of the
 *  algorithm and a cache of partial results is kept in an attempt to speed up the calculation*/
public class DynamicCounter {

    /** Static instance of DynamicCounter */
    public static final DynamicCounter dCounter = new DynamicCounter();

    public double countPerms( UnlabelledGraph g ) {
        boolean temp = g.immutable;
        g.immutable = true;
        double ret = perms(g, opCheckUnconnected | 
                           opCheckFullyConnected |
                           opCheckSingleRootOrLeafNode |
                           opCheckDisconnectedSubgraph |
                           /*opCheckDAGHash |*/
                           opCheckCanonicalDAGHash);
        g.immutable = temp;
        return ret;
    }

    /** Hash table mapping from Long(hash) -> Long(numExtensions)*/
    /*protected*/ public Hashtable<Long,Double> dagHash = new Hashtable<Long,Double>();
    
            
    /** perms is recursively called to calculate permutations.
     *  operations is a bitfield used to specify which operations should be used at the
     *  next level of recursion. */
    protected double perms( UnlabelledGraph g, int operations ) {
        final int numNodes = g.numNodes;
        
        // Base case
        if (numNodes <= 1) { return 1;}
        
        // Check if there are any disconnected nodes.
        // If so return n*perms() with the node removed.
        if ( (operations & opCheckUnconnected) != 0 ) {
            // If a node is a leaf node and a root node, it is disconnected.                                
            int[] disconnectedNodes = g.getDisconnectedNodeArray();
            if (disconnectedNodes.length > 0) {
                return numNodes * perms(g.removeNode(disconnectedNodes[0], true),operations);                    
            }                
        }

        // Check if there is a single root node or single leaf node and remove it.
        // If a single root/leaf node exists, we can remove it without effecting
        // the number of permutations in the network.
        if ( (operations & opCheckSingleRootOrLeafNode) != 0) {                
            
            int[] rootNodes = g.getRootNodeArray();
            int numRootNodes = rootNodes.length;

            int[] leafNodes = g.getLeafNodeArray();
            int numLeafNodes = leafNodes.length;
            
            
            if (numRootNodes == 1 && numLeafNodes > 1) {
                return perms(g.removeNode(rootNodes[0], true),operations);
            }
            else if (numLeafNodes == 1 && numRootNodes > 1) {
                return perms(g.removeNode(leafNodes[0], true),operations);
            }
            else if (numRootNodes == 1 && numLeafNodes == 1) {
                // Optimization? If single root node & single leaf node exist
                // then we save having to calculate numRootNodes twice.
                if (rootNodes[0] > leafNodes[0]) { int temp = rootNodes[0]; rootNodes[0] = leafNodes[0]; leafNodes[0] = temp;}
                return perms(g.removeNode(leafNodes[0], true).removeNode(rootNodes[0], true),operations);                    
            }
            // else no single root/leaf nodes exist. Continue to next step.
        }
        
        // Check if there are any fully connected nodes.
        if ( (operations & opCheckFullyConnected) != 0) {
            for ( int splitNode = 0; splitNode < numNodes; splitNode++ ) {
                if ( g.connected[splitNode] == numNodes-1 ) { // If Connected to all other nodes.
                    
                    // If we find a fully connected node in the DAG, split into two DAGs.
                    // The first containing all nodes which are parents of node[i] and
                    // the second containing all children of node[i].  As out DAG is acyclic
                    // there should be no overlap.                        
                    
                    int[] parents = g.getParents(splitNode);
                    int[] children = g.getChildren(splitNode);
                    
                    // Create parent/child DAG
                    UnlabelledGraph parentDAG = g.removeNode(splitNode, false);
                    UnlabelledGraph childDAG = g.removeNode(splitNode, true);
                    
                    for (int i = 0; i < parents.length; i++) {
                        int p = parents[parents.length-i-1];
                        if (p >= splitNode) {p--;}
                        childDAG = childDAG.removeNode(p, true);
                    }
                    
                    for (int i = 0; i < children.length; i++) {
                        int c = children[children.length-i-1];
                        if (c >= splitNode) {c--;}
                        parentDAG = parentDAG.removeNode(c, true);
                    }
                    
                    // We are now left with parentDAG and childDAG.
                    // As each element in parentDAG must come before each element in childDAG
                    // we can simply multiply the combinations.
                    return perms(parentDAG,operations) * perms(childDAG,operations);
                    
                }
            }
        }

        // Check if there are any disconnected subgraphs in the network.
        // This is a more thorough check than opCheckUnconnected
        //
        // If a subgraph is found we create two graphs g1 and g2.
        // g1 is the subgraph containing node[0], g2 is the remaining nodes.
        // We calculate permutations as perms(g1)*perms(g2)*interleve(g1.numNodes,g2.numNodes)
        // where interleave is the number of ways two lists of length 'a' and 'b' may be combined
        // while maintaining the relative ordering of each individual list.
        if ( (operations & opCheckDisconnectedSubgraph) != 0) {                
            int[] subGraphNodeArray = g.getSubgraphNodeArray(0);
            
            if (subGraphNodeArray.length != numNodes) {
                UnlabelledGraph g1 = g;
                UnlabelledGraph g2 = g;
                
                int g1Index = 0;
                int g2Index = 0;
                int subGraphIndex = 0;
                for ( int i = 0; i < numNodes; i++) {

                    boolean overwrite = !(g1 == g2);
                    if ( subGraphIndex < subGraphNodeArray.length &&
                         i == subGraphNodeArray[subGraphIndex]) {
                        g2 = g2.removeNode(g2Index, overwrite);
                        g1Index++;
                        subGraphIndex++;
                    }
                    else {
                        g1 = g1.removeNode(g1Index, overwrite);
                        g2Index++;                            
                    }
                }
                
                return interleave(g1Index,g2Index) * perms(g1,operations) * perms(g2,operations);
                
            }    // end if subGraph != fullGraphMask
        }

        // Count every entry that has to split (or has it's split in the cache)
        dCounterCalls[numNodes]++;

        // Lookup dag in hashtable.
        Long hash = null;
        if (numNodes < maxHashableSize) {
            if ((operations & opCheckDAGHash) != 0) { hash = new Long(g.getHash());    }
            else if ((operations & opCheckCanonicalDAGHash) != 0) {    hash = new Long(g.getCanonicalHash()); }
        }
        
        if (hash != null) {
            Double ext = dagHash.get(hash);
            if (ext != null) { return ext.doubleValue(); }
        }
        

        
        // There are no polynomial time operations left to reduce the complexity of the problem.
        // We still have a few options open to us:
        // 1. Add an arc to the model.  perms(g) = perms(g + arc(a->b)) + perms(g + arc(b->a))
        // 2. Sum over all possible leaf nodes or root nodes.
        //    perms(g) = sum( perms(g with node[i] first in total ordering) ) 
        //
        // We attempt to choose the model with the lowest branching factor required to reduce
        // the problem to n-1 variables.
        // Splitting on root/leaf nodes has a branching factor of k where k is the number of root
        //  nodes or leaf nodes in the current model.
        // Adding an arc has a branching factor of approx 2^k where k is the number of nodes
        // the most connected node is not connected too.
        
        // If less root nodes than leaf nodes, split on root nodes. Else leaf nodes.
        int numRootNodes = g.getNumRootNodes();
        int numLeafNodes = g.getNumLeafNodes();
        

        int[] remNodes = null;
        if ( numRootNodes > numLeafNodes ) { 
            remNodes = g.getLeafNodeArray(); 
        }
        else {
            remNodes = g.getRootNodeArray();
        }
        
        double total = 0;
        for ( int i = 0; i < remNodes.length; i++ ) {
            int node = remNodes[i];
            UnlabelledGraph g2 = g.removeNode(node, (node == numNodes-1));
            total += perms(g2,operations);
        }
        
        
            
        // put value in hashtable for later use.
        if ((operations & (opCheckDAGHash|opCheckCanonicalDAGHash)) != 0 &&
            (numNodes < maxHashableSize) ) {
            dagHash.put(hash,new Double(total));            
            
            dHashEntries[0]++;
            dHashEntries[numNodes]++;
        }
        return total;
    }

    public static int[] dCounterCalls = new int[100];
    public static int[] dHashEntries = new int[100];
    
    /** Return the number of ways a sequence of length 'a' and length 'b' can be interleaved. 
     *  (equivalent to N choose R)*/
    public static long interleave(int a, int b) {
        if (a == 1) { return b+1; }
        if (b == 1) { return a+1; }
        if ( a > interleaveTable.length || b > interleaveTable.length) {
            throw new RuntimeException("interleave Table length exceeded.");
        }
        if (interleaveTable[a][b] == 0) {
            long sum = 0;
            for (int i = 0; i < a+1; i++) {
                sum += interleave(i,b-1);
                if (sum < 0) {throw new RuntimeException("64 bits exceeded in interleave("+a+","+b+")");}
            }
            interleaveTable[a][b] = sum;
        }
        return interleaveTable[a][b];
    }
    private static long[][] interleaveTable = new long[100][100];
    
    /** Flag in operations, check for disconnected nodes. */
    public final static int opCheckUnconnected = 0x01;
    
    /** Flag in operations, check for any fully connected models. */
    public final static int opCheckFullyConnected = 0x02;
    
    /** Check if there is a single leaf or single root node in the network.
     *  This check is also done as part of opCheckFullyConnected but this
     *  check is optimised for root/leaf nodes. */
    public final static int opCheckSingleRootOrLeafNode = 0x04;

    /** Check if all variables are connected by an undirected path.
     *  If not, we can split the graph into it's subgraphs.
     *  This is a more thorough version of opCheckSingleRootOrLeafNode
     *  which only finds single disconnected nodes. */
    public final static int opCheckDisconnectedSubgraph = 0x08;

    /** Should cache lookups be attempted? (Hash of Labelled DAGs)*/
    public final static int opCheckDAGHash = 0x10;

    /** Should cache lookups be attempted? (Using hash of canonical unlabelled DAGs) */
    public final static int opCheckCanonicalDAGHash = 0x20;
    
    /** DAGs with >= maxHashableSize are not inserted in hashtable. */
    public final int maxHashableSize = 60;
}
