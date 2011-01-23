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

import camml.core.search.TOM;

/** Basic data structure used in extension counting, can contain up to 64 nodes */
public class UnlabelledGraph64 extends UnlabelledGraph {
    static { initHash(64); }
    
    // Accessors Galore!
    public int getNumLeafNodes() { return ExtensionCounterLib.countBits(leafNodes); }
    public int getNumRootNodes() { return ExtensionCounterLib.countBits(rootNodes); }
    public int getNumDisconnectedNodes() { return ExtensionCounterLib.countBits(leafNodes&rootNodes); }
    
    public int[] getLeafNodeArray() { return ExtensionCounterLib.getNodeArray(leafNodes); }
    public int[] getRootNodeArray() { return ExtensionCounterLib.getNodeArray(rootNodes); }
    public int[] getDisconnectedNodeArray() { return ExtensionCounterLib.getNodeArray(leafNodes&rootNodes); }

    public int[] getParents(int node) {return ExtensionCounterLib.getNodeArray(parentList[node]); }
    public int[] getChildren(int node) {return ExtensionCounterLib.getNodeArray(childList[node]); }

    
    
    /** Mask for each element in arcMatrix. nodeMask[i] = 1<<(i-1) <br>
     *  This code is essentially useless but may clarify some areas of the code. */
    final static long[] nodeMask = ExtensionCounterLib.nodeMask;
    
    /** List of parents for each node */
    final long[] parentList;

    /** List of children for each node */
    private final long[] childList;

    /** List of leaf nodes */
    private long leafNodes;
    
    /** List of root nodes */
    private long rootNodes;
        
    /** Initialise UnlabelledGraph from a TOM */
    public UnlabelledGraph64( TOM tom ) {
        this(tom.getNumNodes());
        initFromTOM(tom);        
    }
    
    /** Initialise empty graph with 'n' nodes*/
    public UnlabelledGraph64( int n ) {            
        super(n);
        
        // Initialise empty parent lists.
        parentList = new long[n];
        childList = new long[n];
                
        // Initialise connected list, all entries start as 0
        connected = new int[n];
        
        // Initialise bitfield of rootNodes & leafNodes.
        // In an empty network all nodes are rootNodes and leafNodes so the bottom
        // 'n' bits are set.  (the >>> operator shifts without sign extension)
        rootNodes = -1l  >>> (64-n);
        leafNodes = -1l  >>> (64-n);
    }

    /** return a copy of this graph with a single node removed. 
     *  If overwrite == true, this node is overwritten with the node removed.
     *  NOTE: this.immutable == false takes precedence over overwrite parameter. 
     */
    public UnlabelledGraph64 removeNode(int node, boolean overwrite) {
        if ( node >= numNodes || node < 0 || numNodes == 0 ) {
            throw new RuntimeException("Cannot remove node. ("+node+","+numNodes+")");
        }
        
        
        UnlabelledGraph64 g;
        if (immutable) {overwrite = false;}
        if (overwrite) {g = this;}
        else {g = new UnlabelledGraph64(numNodes-1);};
        
        
        /////////////////////////////////////
        // Update connected[] in new graph //
        /////////////////////////////////////
        int gIndex = 0;
        for ( int i = 0; i < numNodes; i++ ) {
            if (i==node) {continue;}
                            
            g.connected[gIndex] = connected[i]; // Initialise connected list.
            if ((parentList[node] & nodeMask[i]) != 0) { g.connected[gIndex]--; }
            if ((childList[node] & nodeMask[i]) != 0) { g.connected[gIndex]--; }
            
            gIndex++;
        }

        ///////////////////////////////////////////////////
        // Create parentList and childList for new graph //
        ///////////////////////////////////////////////////
            
        gIndex = 0;
        for ( int i = 0; i < numNodes; i++) {
            if (i == node) {continue;}
            
            g.parentList[gIndex] = ExtensionCounterLib.removeBit(parentList[i], node);
            g.childList[gIndex] = ExtensionCounterLib.removeBit(childList[i], node);

            gIndex++;
        }
        
        if (overwrite) {
            g.parentList[numNodes-1] = 0;
            g.childList[numNodes-1] = 0;            
            g.numNodes --;
        }

        ////////////////////////////////////////
        // Update root/leafNodes in new graph //
        ////////////////////////////////////////
        g.rootNodes = 0l;
        g.leafNodes = 0l;
        for ( int i = 0; i < g.numNodes; i++ ) {
            if (g.parentList[i] == 0l) g.rootNodes |= nodeMask[i];
            if (g.childList[i] == 0l) g.leafNodes |= nodeMask[i];
        }

        return g;
    }

    /** Return a hash of the (semi) canonical version of this DAG. 
     *  We attempt to create a canonical version by sorting the nodes by 
     *  number of connecteions and by ordering. This will not always produce ideal results.*/
    public long getCanonicalHash() {
        
        return getCanonicalHash4();

    }

    /** Returns true if a is before b in the sorter order used in "sort" */
    private boolean before(int a, int b, boolean reverse) {            
        
        if (a==b) {return false;}
        
        // Primary sorting key: number of connections.
        if (connected[a] != connected[b]) {return connected[a] < connected[b];}

        // Secondary sorting key: if a and b are linked, which is the parent?
        if ((childList[a] & nodeMask[b]) != 0) { return reverse; }
        if ((childList[b] & nodeMask[a]) != 0) { return !reverse; }
        
        // Third sorting key: number of children A and B have.
        int bitsA = ExtensionCounterLib.countBits(childList[a]);
        int bitsB = ExtensionCounterLib.countBits(childList[b]);
        if (bitsA != bitsB) { return reverse ^ (bitsA < bitsB); }            
        
        return false;        
    }
    
    /** Sort variables in index[] based on:
     *  - Number of connections
     *  - Position in total order
     *  - Number of parents 
     *  if (reverse==true), position and parent tests are reversed. */
    private void sort( int[] index, int start, int end, boolean reverse ) {

        // Index of split variable.
        // We choose the middle variable as quicksort has worst case
        // performance of O(n^2) when run on a fully ordered list and splitting on
        // index[start].  Sorting a fully ordered list with split point in the middle
        // of the list should (hopefully) run faster.        
        int split = index[(start+end)/2];        
        
    
        int left = start;
        int right = end;
        
        if (right <= left) {return;}
        
        // Quicksort.
        while (left < right) {
            // First sorting criteria is connectivity
            //while ( connected[index[left]] < connected[split] ) { left++; }
            //while ( connected[index[right]] > connected[split] ) { right--; }

            while ( /*(left > right) &&*/ before(index[left],split,false) ) {left++; }
            while ( /*(left > right) &&*/ before(split,index[right],false) ) { right--; }

            if (left > right) {break;}
            
            //if (split == left) { split = right;}
            int temp = index[left]; index[left] = index[right]; index[right] = temp;
            left++; right--;
        }

        sort(index,start,right,false);
        sort(index,left,end,false);
    }
    
    private long getCanonicalHash4() {

        // Create initial node ordering.
        int[] nodeOrder = new int[numNodes];
        for (int i = 0; i < numNodes; i++) { nodeOrder[i] = i; }
        
        // Sort nodes by connectivity, number of parents and total order.
        sort(nodeOrder,0,nodeOrder.length-1,false);

        
        // Nodes which cannot be split by the sorting criteria are placed into cliques.
        // If a node is uniquely identifiable, it is placed in a clique by itself.
        int clique[] = new int[nodeOrder.length];        
        for (int i = 1; i < clique.length; i++) {
            if ((!before(nodeOrder[i-1],nodeOrder[i],false)) && 
                (!before(nodeOrder[i],nodeOrder[i-1],false))) 
                { clique[i] = clique[i-1]; }
            else { clique[i] = clique[i-1]+1; }
        }
        
        // Set all single entry clique elements to -1
        for (int i = 1; i < clique.length-1; i++) {                
            // If we are not in a clique
            if ( (clique[i] != clique[i-1]) && (clique[i] != clique[i+1]) ) {
                clique[i] = -1;
            }
        }
        if (clique[0] != clique[1]) {clique[0] = -1;}
        if (clique[clique.length-1] != clique[clique.length-2]) {clique[clique.length-1] = -1;}
        
        // Move all elements in single cliques to the front of the list.
        int min = 0;
        int max = clique.length;
        boolean changes = true;
        while(changes) {
            changes = false;
            for (int i = min; i < max-1; i++) {
                if (clique[i] > clique[i+1]) {
                    int temp = clique[i]; clique[i] = clique[i+1]; clique[i+1] = temp;
                    temp = nodeOrder[i]; nodeOrder[i] = nodeOrder[i+1]; nodeOrder[i+1] = temp;
                    changes = true;
                    if (i == min) min++;
                    if (i == max-2) max--;
                }
            }
        }
        min = 0;
        while (min < clique.length && clique[min] == -1) {min++;}

        // Sort the remainder of the list using extra sorting criteria based
        // on which nodes they are connected to in the total ordering.
        /*while (min < clique.length) {
          for (int i = min; i < clique.length; i++) {
                
          }
          }*/
        
        
        long hash = 0;            
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++ ) {
                if ((parentList[nodeOrder[i]] & nodeMask[nodeOrder[j]]) != 0) {
                    hash += nodeHash[i][j];
                }
            }
        }
        
        // Repeat hash with arcs reversed 
        // (A graph with all arcs reversed should have the same number of extensions
        //  as the original graph so we hash them to the same value.)
        /*
          sort(nodeOrder,0,nodeOrder.length-1,true);
        
          long hash2 = 0;            
          for (int i = 0; i < numNodes; i++) {
          for (int j = 0; j < numNodes; j++ ) {
          if ((parentList[nodeOrder[i]] & nodeMask[nodeOrder[j]]) != 0) {
          hash2 += nodeHash[i][j];
          }
          }
          }
        
          if (hash2 < hash) {return hash2;}
        */            
        return hash;            
    }
    
    /** Add arc to arcMatrix. Updates rootNodes, leafNodes, connected, parentList
     *  and (if requested) adds implied links too. */
    public void addArc(int parent, int child, boolean addImpliedLinks) {
        if (immutable) { throw new RuntimeException("Cannot overwrite immutable graph."); }
        
        // Check if arc already exists
        if ( (parentList[child] & nodeMask[parent]) != 0) {
            throw new RuntimeException("arc already exists");
        }
        
        connected[parent]++;
        connected[child]++;
        
        rootNodes &= ~nodeMask[child];  // Mask away child from rootNodes
        leafNodes &= ~nodeMask[parent];  // Mask away parent from leafNodes
    
        parentList[child] |= nodeMask[parent];  // Mask parent into parentList
        childList[parent] |= nodeMask[child];   // Mask child into childlist.
        
        // Add implied links if required.
        if ( addImpliedLinks ) {
            for (int i = 0; i < numNodes; i++) {

                // If a node is a parent of "parent" but not a parent of "child", we must add it as a
                // parent of "child".  Naturally this rule does not apply for the node "parent" which cannot
                // be a parent of itself.
                if ( ((parentList[parent] & nodeMask[i]) != 0) &&   // If 'i' is parent of parent 
                     ((parentList[child] & nodeMask[i]) == 0) ) {   // but not parent of child                        
                    addArc(i,child,true);                           // add 'i' as parent of child
                }
                
                // As above, accept adding implied children.
                if ( ((childList[child] & nodeMask[i]) != 0) &&     // If 'i' is child of child 
                     ((childList[parent] & nodeMask[i]) == 0) ) {   // but not child of parent                        
                    addArc(parent,i,true);                       // add 'i' as child of parent
                }
                                
            }
        }
    }        
    
    /** Return a bitfield containing all nodes connected to node specified in 'node' */
    public long getSubgraphMask(int node) {
        if (numNodes == 0) { return 0; }
        
        long connected = nodeMask[node]; // Mask of variables connected to 'node'
        long checked = 0;                // Mask of variables connected to 'node' who's 
                                         // parents and children have been added to 'connected'
        
        while ( connected != checked ) {  // While not all nodes in connected have been checked                
            for (int i = 0; i < numNodes; i++) {
                // If node[i] has not been checked, add add its parents and children to connected and flag it as ckecked.
                if ( (checked & nodeMask[i]) != (connected & nodeMask[i]) ) {
                    checked |= nodeMask[i];
                    connected |= childList[i];
                    connected |= parentList[i];
                }
            }
        }
        
        return connected;
    }
    
    public int[] getSubgraphNodeArray(int node) {
        return ExtensionCounterLib.getNodeArray(getSubgraphMask(node));
    }
    
    /** Return an ASCII art representation of UnlabelledGraph */
    public String toString() {
        StringBuffer s = new StringBuffer();
        
        for ( int i = 0; i < numNodes; i++) {
            s.append(""+i+" : ");
            for (int j = 0; j < numNodes; j++) {
                if ( (parentList[i] & nodeMask[j]) != 0) {
                    s.append( " <- " + j );
                }
            }
            s.append("\n");
        }
        
        for ( int i = 0; i < numNodes; i++ ) { s.append("["+i+"] => "+connected[i]+"\t"); }
        s.append("\n");
        
        s.append("Root: " + Long.toHexString(rootNodes) + "\n");
        s.append("Leaf: " + Long.toHexString(leafNodes) + "\n");
        s.append("subGraph(0): " + Long.toHexString(getSubgraphMask(0)) + "\n");
        
        return s.toString();
    }

    /** Return true if the arc parent -> child exists */
    public boolean isDirectedArc(int parent, int child) {
        return (childList[parent] & nodeMask[child]) != 0;
    }

    /** Accessor for parentList */
    public long[] getParentList() { return parentList; }
    /** Accessor for childList */
    public long[] getChildList() { return childList; }

    /** Accessor for leafNodes */
    public long testOnly_getLeafNodes() { return leafNodes; }
    /** Accessor for rootNodes */
    public long testOnly_getRootNodes() { return rootNodes; }

}
