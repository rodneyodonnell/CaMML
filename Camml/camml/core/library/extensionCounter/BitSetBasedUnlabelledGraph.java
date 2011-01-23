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

import java.util.BitSet;
import camml.core.search.TOM;

public class BitSetBasedUnlabelledGraph extends UnlabelledGraph {
    
    /** List of parents for each node */
    final BitSet[] parentList;

    /** List of children for each node */
    private final BitSet[] childList;

    /** List of leaf nodes */
    private final BitSet leafNodes = new BitSet();
    
    /** List of root nodes */
    private final BitSet rootNodes = new BitSet();
    
    private BitSet doAnd(BitSet bitSet1, BitSet bitSet2) {
        BitSet bs = (BitSet)bitSet1.clone();
        bs.and(bitSet2);
        return bs;
    }
    
    // Accessors Galore!
    public int getNumLeafNodes() { return leafNodes.cardinality(); }
    public int getNumRootNodes() { return rootNodes.cardinality(); }
    public int getNumDisconnectedNodes() { return doAnd(leafNodes,rootNodes).cardinality(); }
    
    public int[] getLeafNodeArray() { return ExtensionCounterLib.getNodeArray(leafNodes); }
    public int[] getRootNodeArray() { return ExtensionCounterLib.getNodeArray(rootNodes); }
    public int[] getDisconnectedNodeArray() { return ExtensionCounterLib.getNodeArray(doAnd(leafNodes,rootNodes)); }

    public int[] getParents(int node) {return ExtensionCounterLib.getNodeArray(parentList[node]); }
    public int[] getChildren(int node) {return ExtensionCounterLib.getNodeArray(childList[node]); }

    
    
        
    /** Initialise UnlabelledGraph from a TOM */
    public BitSetBasedUnlabelledGraph( TOM tom ) {
        this(tom.getNumNodes());
        initFromTOM(tom);        
    }
    
    /** Initialise empty graph with 'n' nodes*/
    public BitSetBasedUnlabelledGraph( int n ) {            
        super(n);
        
        // Initialise empty parent lists.
        parentList = new BitSet[n];
        childList = new BitSet[n];
        
        for (int i = 0; i < parentList.length; i++) {
            parentList[i] = new BitSet(n);
            childList[i] = new BitSet(n);
        }
        
        // Initialise connected list, all entries start as 0
        connected = new int[n];
        
        // Initialise BitSet of rootNodes & leafNodes.
        rootNodes.set(0, n, true);
        leafNodes.set(0, n, true);
    }

    /** return a copy of this graph with a single node removed. 
     *  If overwrite == true, this node is overwritten with the node removed.
     *  NOTE: this.immutable == false takes precedence over overwrite parameter. 
     */
    public BitSetBasedUnlabelledGraph removeNode(int node, boolean overwrite) {
        if ( node >= numNodes || node < 0 || numNodes == 0 ) {
            throw new RuntimeException("Cannot remove node. ("+node+","+numNodes+")");
        }
        
        
        BitSetBasedUnlabelledGraph g;
        if (immutable) {overwrite = false;}
        if (overwrite) {g = this;}
        else {g = new BitSetBasedUnlabelledGraph(numNodes-1);};
        
        
        /////////////////////////////////////
        // Update connected[] in new graph //
        /////////////////////////////////////
        int gIndex = 0;
        for ( int i = 0; i < numNodes; i++ ) {
            if (i==node) {continue;}
                            
            g.connected[gIndex] = connected[i]; // Initialise connected list.
            if (parentList[node].get(i)) { g.connected[gIndex]--; }
            if (childList[node].get(i) ) { g.connected[gIndex]--; }
            
            gIndex++;
        }

        ///////////////////////////////////////////////////
        // Create parentList and childList for new graph //
        ///////////////////////////////////////////////////
        
        // TODO: Remove Longs!
        // Create masks used for removing/shifting out of parent/child list.
        BitSet mask1 = ExtensionCounterLib.toBitSet(-1l << (node+1));
        BitSet mask2 = ExtensionCounterLib.toBitSet(-1l >>> 64-(node));
        
        // We need a special check for boundary conditions.
        if (node == 0) {mask2.clear();}
        if (node == 63) {mask1.clear();}
        
        
        gIndex = 0;
        for ( int i = 0; i < numNodes; i++) {
            if (i == node) {continue;}
            
            g.parentList[gIndex] = ExtensionCounterLib.removeBit(parentList[i], node, false);
            g.childList[gIndex] = ExtensionCounterLib.removeBit(childList[i], node, false);
            gIndex++;
        }
        
        if (overwrite) {
            g.parentList[numNodes-1].clear();
            g.childList[numNodes-1].clear();            
            g.numNodes --;
        }

        ////////////////////////////////////////
        // Update root/leafNodes in new graph //
        ////////////////////////////////////////
        g.rootNodes.clear();
        g.leafNodes.clear();
        for ( int i = 0; i < g.numNodes; i++ ) {
            if (g.parentList[i].isEmpty()) g.rootNodes.set(i);
            if (g.childList[i].isEmpty()) g.leafNodes.set(i);
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
        if (childList[a].get(b)) { return reverse; }
        if (childList[b].get(a)) { return !reverse; }
        
        // Third sorting key: number of children A and B have.
        int bitsA = childList[a].cardinality();
        int bitsB = childList[b].cardinality();
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

        long hash = 0;            
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++ ) {
                if (parentList[nodeOrder[i]].get(nodeOrder[j])) {
                    hash += nodeHash[i][j];
                }
            }
        }
        
        return hash;            
    }
    
    /** Add arc to arcMatrix. Updates rootNodes, leafNodes, connected, parentList
     *  and (if requested) adds implied links too. */
    public void addArc(int parent, int child, boolean addImpliedLinks) {
        if (immutable) { throw new RuntimeException("Cannot overwrite immutable graph."); }
        
        // Check if arc already exists
        if ( parentList[child].get(parent) ) {
            throw new RuntimeException("arc already exists");
        }
        
        connected[parent]++;
        connected[child]++;
        
        rootNodes.clear(child);  // Mask away child from rootNodes
        leafNodes.clear(parent);  // Mask away parent from leafNodes
    
        parentList[child].set(parent);  // Mask parent into parentList
        childList[parent].set(child);;   // Mask child into childlist.
        
        // Add implied links if required.
        if ( addImpliedLinks ) {
            for (int i = 0; i < numNodes; i++) {

                // If a node is a parent of "parent" but not a parent of "child", we must add it as a
                // parent of "child".  Naturally this rule does not apply for the node "parent" which cannot
                // be a parent of itself.
                if ( (parentList[parent].get(i)) &&   // If 'i' is parent of parent 
                     (!parentList[child].get(i)) ) {   // but not parent of child                        
                    addArc(i,child,true);                           // add 'i' as parent of child
                }
                
                // As above, accept adding implied children.
                if ( childList[child].get(i)  &&      // If 'i' is child of child 
                     !childList[parent].get(i) ) {    // but not child of parent                        
                    addArc(parent,i,true);       // add 'i' as child of parent
                }
                                
            }
        }
    }        
    
    /** Return a bitfield containing all nodes connected to node specified in 'node' */
    public BitSet getSubgraphMask(int node) {
        if (numNodes == 0) { return new BitSet(); }
        
        // Mask of variables connected to 'node'
        BitSet connected = new BitSet();
        connected.set(node); 
        
        // Mask of variables connected to 'node' who's parents and children have been added to 'connected'
        BitSet checked = new BitSet();
        
        
        while ( !connected.equals(checked) ) {  // While not all nodes in connected have been checked                
            for (int i = 0; i < numNodes; i++) {
                // If node[i] has not been checked, add add its parents and children to connected and flag it as ckecked.
                if ( checked.get(i) != connected.get(i) ) {
                    checked.set(i);
                    connected.or(childList[i]);
                    connected.or(parentList[i]);
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
                if ( parentList[i].get(i)) {
                    s.append( " <- " + j );
                }
            }
            s.append("\n");
        }
        
        for ( int i = 0; i < numNodes; i++ ) { s.append("["+i+"] => "+connected[i]+"\t"); }
        s.append("\n");
        
        //s.append("Root: " + BitSet.toHexString(rootNodes) + "\n");
        //s.append("Leaf: " + BitSet.toHexString(leafNodes) + "\n");
        //s.append("subGraph(0): " + BitSet.toHexString(getSubgraphMask(0)) + "\n");
        
        return s.toString();
    }

    /** Return true if the arc parent -> child exists */
    public boolean isDirectedArc(int parent, int child) {
        return childList[parent].get(child);
    }

    /** Accessor for parentList */
    public BitSet[] getParentList() { return parentList; }
    /** Accessor for childList */
    public BitSet[] getChildList() { return childList; }

    /** Accessor for leafNodes */
    public BitSet testOnly_getLeafNodes() { return leafNodes; }
    /** Accessor for rootNodes */
    public BitSet testOnly_getRootNodes() { return rootNodes; }

}
