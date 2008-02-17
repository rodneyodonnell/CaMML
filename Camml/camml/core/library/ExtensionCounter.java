//
// Functions Calculating the number of linear extensions of a TOM
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: CountExtensions.java
// Author: rodo@dgs.monash.edu.au


/**
 * Class containting functions to calculate linear extensions of a TOM quickly. <br>
 * Calculating extensions (ie. total orderings which comply to the partial ordering
 * implied by a DAG) is a NP complete problem /ref{?} <br>
 *
 * However, by storing partial results we may be able to amortise this NP cost when
 * counting extensions on many TOMs.  Naturally we are still faced with a NP problem,
 * but this approach will hopefully move the "knee" a bit further along.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.12 $ $Date: 2007/02/21 07:12:37 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/library/ExtensionCounter.java,v $
 */

package camml.core.library;

import java.util.Arrays;
import java.util.Hashtable;

import camml.core.search.TOM;
import camml.plugin.netica.NeticaFn;
import cdms.core.Value;


public class ExtensionCounter {

	/** Test function, load in netica network and return number of extensions 
	 *  Performs comparison in prior probability using DAG and TOM based priors. */
	public static void main( String args[]) {		
		if (args.length != 1) { 
			System.out.println("Usage: java camml.core.library.ExtensionCounter net.dne"); 
			return;
		}
		Value.Structured my = NeticaFn.LoadNet._apply(args[0]);
		//System.out.println("my = " + my);
		
		Value.Vector params = (Value.Vector)my.cmpnt(1);
		int n = params.length();
		TOM tom = new TOM(n);
		tom.setStructure(params);
		//System.out.println(tom);
		UnlabelledGraph dag = new UnlabelledGraph(tom);
		double numPerms = dCounter.countPerms(dag);
		//System.out.println("numPerms = " + numPerms);

		double totalTOMs = Math.pow(2, n*(n-1)/2) * factorial(n);
		double tomPrior = (numPerms / totalTOMs);
		System.out.println("TOM prior = " + tomPrior + "\t=\t" + 
				numPerms + " / " + totalTOMs);
		
		double totalDAGs = countDAGs(n);
		double dagPrior = (1 / totalDAGs);
		System.out.println("DAG prior = " + dagPrior + "\t=\t" + 
				1 + " / " + totalDAGs);

		System.out.println("Ratio = " + (tomPrior/dagPrior) + " : 1");
	}
	
	public static double factorial(int n) {
		if (n == 0) { return 1.0; }
		else { return n * factorial(n-1); }
	}
	
	static Hashtable<Integer, Double> dagCountHash = new Hashtable<Integer, Double>();
	
	public static double countDAGs(int n) {		
		if (dagCountHash.containsKey(n)) { return dagCountHash.get(n); }
		if (n <= 1) return 1;
		double sum = 0;
		for (int k = 1; k <= n; k++) {
			sum += -Math.pow(-1,k) * choose(n,k) * Math.pow(2,k*(n-k)) * countDAGs(n-k);
		}
		dagCountHash.put(n, sum);
		return sum;
	}
	public static double choose( int n, int r) {
		return factorial(n)/(factorial(r)*factorial(n-r));
	}
	
	/** Basic data structure used in extension counting, can contain up to 64 nodes */
	public static class UnlabelledGraph {
		
		/** Mask for each element in arcMatrix. nodeMask[i] = 1<<(i-1) <br>
		 *  This code is essentially useless but may clarify some areas of the code. */
		public final static long[] nodeMask = new long[64];
		static { for (int i = 0; i < 64; i++) {nodeMask[i] = 1l << (i); } }
		
		/** Random array used for hashing DAGs */
		private final static long[][] nodeHash = new long[64][64];
		static {
			java.util.Random rand = new java.util.Random(123);
			for ( int i = 0; i < nodeHash.length; i++) {
				for (int j = 0; j < nodeHash.length; j++ ) {
					nodeHash[i][j] = rand.nextLong();
				}
			}
		}
		
		/** Number of nodes represented by graph. */
		public int numNodes;
		
		/** List of parents for each node (~= transpose of childList)*/
		final long[] parentList;
	
		/** List of children for each node (~= transpose of parentList) */
		final long[] childList;

		/** List of leaf nodes */
		long leafNodes;
		
		/** List of root nodes */
		long rootNodes;
		
		/** Connectivity of each node. (#of arcs into or out of node) */
		final int[] connected;			
		
		/** If immutable == true && overwrite parameter == true we are free to overwrite
		 *  this graph.  This flag is required so ancestors in recursive calls can disallow
		 *  overwriting. */
		public boolean immutable = false;
		
		/** Initialise UnlabelledGraph from a TOM */
		public UnlabelledGraph( TOM tom ) {
			this(tom.getNumNodes());
		
			for ( int i = 0; i < numNodes; i++) {
				for ( int j = 0; j < numNodes; j++) {
					if (i!=j && tom.isDirectedArc(tom.nodeAt(i),tom.nodeAt(j))) {
						this.addArc(i,j,true);
					}
				}
			}
		}
		
		/** Initialise empty graph with 'n' nodes*/
		public UnlabelledGraph( int n ) {			
			this.numNodes = n;
			
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
		public UnlabelledGraph removeNode(int node, boolean overwrite) {
			if ( node >= numNodes || node < 0 || numNodes == 0 ) {
				throw new RuntimeException("Cannot remove node.");
			}
			
			
			UnlabelledGraph g;
			if (immutable) {overwrite = false;}
			if (overwrite) {g = this;}
			else {g = new UnlabelledGraph(numNodes-1);};
			
			
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
			
			// Create masks used for removing/shifting out of parent/child list.
			long mask1 = -1l << (node+1);
			long mask2 = -1l >>> 64-(node);
			
			// We need a special check for boundary conditions.
			if (node == 0) {mask2 = 0;}
			if (node == 63) {mask1 = 0;}
			
			gIndex = 0;
			for ( int i = 0; i < numNodes; i++) {
				if (i == node) {continue;}
				
				// Mask and shift to remove node from parent and child lists. 
				g.parentList[gIndex] = ((parentList[i] & mask1) >> 1) | (parentList[i] & mask2);
				g.childList[gIndex] = ((childList[i] & mask1) >> 1) | (childList[i] & mask2);				
				
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

		/** Return a hash of this DAG. */
		public long getHash() {
			long hash = 0;
			for (int i = 0; i < numNodes; i++) {
				for (int j = 0; j < numNodes; j++ ) {
					if ((parentList[i] & nodeMask[j]) != 0) {
						hash += nodeHash[i][j];
					}
				}
			}
			return hash;			
		}

		
		/** Return a hash of the (semi) canonical version of this DAG. 
		 *  We attempt to create a canonical version by sorting the nodes by 
		 *  number of connecteions and by ordering. This will not always produce ideal results.*/
		public long getCanonicalHash() {
			
			return getCanonicalHash4();

		}

		/** count the number of bits set in x */
		public int countBits( long x ) {
			// This algorithm uses the nice little trick that
			// x & (x-1) == x with its lowest bit set to false.
			// It makes sense if you think about it for a while.
			int count = 0;
			while (x != 0) { x = x & (x-1); count++;}

			return count;
		}
		
		/** Returns true if a is before b in the sorter order used in "sort" */
		public boolean before(int a, int b, boolean reverse) {			
			
			if (a==b) {return false;}
			
			// Primary sorting key: number of connections.
			if (connected[a] != connected[b]) {return connected[a] < connected[b];}

			// Secondary sorting key: if a and b are linked, which is the parent?
			if ((childList[a] & nodeMask[b]) != 0) { return reverse; }
			if ((childList[b] & nodeMask[a]) != 0) { return !reverse; }
			
			// Third sorting key: number of children A and B have.
			int bitsA = countBits(childList[a]);
			int bitsB = countBits(childList[b]);
			if (bitsA != bitsB) { return reverse ^ (bitsA < bitsB); }			
			
			return false;		
		}

		public static int sortCalled = 0;
		public static void breakpointFn() {
			System.out.println("Breakpoint:");
		}
		
		/** Sort variables in index[] based on:
		 *  - Number of connections
		 *  - Position in total order
		 *  - Number of parents 
		 *  if (reverse==true), position and parent tests are reversed. */
		public void sort( int[] index, int start, int end, boolean reverse ) {

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
		
		/** Return a hash of the (semi) canonical version of this DAG. 
		 *  We attempt to create a canonical version by sorting the nodes by 
		 *  number of connecteions and by ordering. This will not always produce ideal results.*/
		static int hashCount22 = 0;
		public long getCanonicalHash3() {

			// Create initial node ordering.
			int[] nodeOrder = new int[numNodes];
			for (int i = 0; i < numNodes; i++) { nodeOrder[i] = i; }
			
			sort(nodeOrder,0,nodeOrder.length-1,false);
			
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
			return hash;			
		}
		
		public long getCanonicalHash4() {

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
		
		/** Return an ascii art representation of UnlabelledGraph */
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
		public long getLeafNodes() { return leafNodes; }
		/** Accessor for rootNodes */
		public long getRootNodes() { return rootNodes; }

		/** Accessor for connected */
		public int[] getConnected() { return connected; }

	}
	
	/** Static instance of WallaceCounter */
	public static final WallaceCounter wCounter = new WallaceCounter();
	
	/** Algorithm implemented in original CaMML (should probably be called something else.) */
	public static class WallaceCounter {
	
		/** Extension Counter taken from Chris Wallece's original CaMML code. (with modifications) */
		long perms( UnlabelledGraph g, int lev ) {
			/*	Routine lperms should return LogFactorialOfNumVariables - log (num of linear extensions
			 of current model), provided NumVariables < 32	*/
			//	int doms [32];   /* A set of bit patterns with a 1 to show that
			//		if the node given by the bit must precede this node * /
			//	int perm [32];   /* current partial permutation  * /
			//	int dset;	/* Has a one for every node in partial perm   * /
			//	int vbit [32];	/* Set of 1-bit patterns, one per variable * /


			/*	This assumes all nodes in perm[0...lev] are in place
			 and enterd in dset. For each node in perm[lev+1....(NumVariables-1)]
			 it tries to see if the node can be placed in perm[lev+1]
			 and enterd in dset. This can be done only if dset contains
			 all nodes dominating the node to be placed
			 */
		
			long[] doms = g.getParentList();
			long[] vbit = UnlabelledGraph.nodeMask;
		
			lev = lev + 1;
			if (lev >= g.numNodes-2) {  // If 2 or less vars remaining, set np=2 or np=1 if they are joined
				long np = 2;
				// if perm[lev] and perm[lev+1] are conncected, np = 1. else np = 2.
				if (((doms [perm[lev]] | doms [perm[lev+1]]) & ~dset) != 0) np = 1;
				return np; 
			}

			long np = 0;
			/*	Save node number at lev	*/
			int k = perm [lev];
			for (int i = lev; i < g.numNodes; i++)	{
				/*	Can we fix the node in perm [i] ?   */
				int j = perm [i];
				if ((doms [j] & (~ dset)) == 0) { 
				
				/*		Yes it can be done	*/
				perm [i] = k;
				perm [lev] = j;
				dset |= vbit [j];
				/*	Have inserted node j in dset	*/
				np += perms (g,lev);
				/*	Restore perms, dset	*/
				dset &= ~ vbit[j];
				perm [i] = j;
				}			
			}
			perm [lev] = k;

			return (np);
		}

		/** Used by lperms & perms */
		private int[] perm = null;
		/** Used by lperms & perms */
		private int dset = 0;

		// doms[i] = g.getParentList()[i]
		// vbit[i] = g.nodeMask[i]
	
		/*	The routine lperms sets up doms, perm from current model then
		 uses perms recursively	*/
		public long lperms(UnlabelledGraph g)
		{
  
			if (g.numNodes >= 32) {
				throw new RuntimeException("numNodes too large to count extensions.");
			}
	  
			perm = new int[g.numNodes];
			for (int i = 0; i < g.numNodes; i++) {		   
				perm [i] = i;
			}
	  
			dset = 0;
			return perms(g,-1);
	  
		}
	}

	/** Static instance of DynamicCounter */
	public static final DynamicCounter dCounter = new DynamicCounter();

	/** Dynamic programming approach to Extension Counting. 
	 *  Various operations are used in an attempt to reduce the branching factor of the
	 *  algorithm and a cache of partial results is kept in an attempt to speed up the calculation*/
	public static class DynamicCounter {
	
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
			//System.out.println( g );
			
			// Base case
			if (numNodes <= 1) { return 1;}
			
			// Check if there are any disconnected nodes.
			// If so return n*perms() with the node removed.
			if ( (operations & opCheckUnconnected) != 0 ) {
				// If a node is a leaf node and a root node, it is disconnected.
				if ( (g.rootNodes & g.leafNodes) != 0 ) {
					long disconnectedNodes = g.rootNodes & g.leafNodes;
					for ( int i = 0; i < numNodes; i++ ) {
						if ((disconnectedNodes & UnlabelledGraph.nodeMask[i]) != 0) {
							return numNodes * perms(g.removeNode(i, true),operations);
						}
					}
				}
			}

			// Check if there is a single root node or single leaf node and remove it.
			// If a single root/leaf node exists, we can remove it without effecting
			// the number of permutations in the network.
			if ( (operations & opCheckSingleRootOrLeafNode) != 0) {				
				int numRootNodes = 0;
				int numLeafNodes = 0;
				int rootNode = -1;
				int leafNode = -1;
				
				for ( int i = 0; i < numNodes; i++) {
					if ((g.rootNodes & UnlabelledGraph.nodeMask[i]) != 0) {
						rootNode = i;
						numRootNodes++;
					}
					else if ((g.leafNodes & UnlabelledGraph.nodeMask[i]) != 0) {
						leafNode = i;
						numLeafNodes++;
					} 
				}
				if (numRootNodes == 1 && numLeafNodes > 1) {
					return perms(g.removeNode(rootNode, true),operations);
				}
				else if (numLeafNodes == 1 && numRootNodes > 1) {
					return perms(g.removeNode(leafNode, true),operations);
				}
				else if (numRootNodes == 1 && numLeafNodes == 1) {
					// Optimization? If single root node & single leaf node exist
					// then we save having to calculate numRootNodes twice.
					if (rootNode > leafNode) { int temp = rootNode; rootNode = leafNode; leafNode = temp;}
					return perms(g.removeNode(leafNode, true).removeNode(rootNode, true),operations);
					
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
						
						// Create parent/child DAG
						UnlabelledGraph parentDAG = g;
						UnlabelledGraph childDAG = g;
						int parentIndex = 0;
						int childIndex = 0;
						long parentMask = g.parentList[splitNode];
						long childMask = g.childList[splitNode];
						for (int i = 0; i < numNodes; i++) {						
							// If parentDAG and childDAG differ, we can overwrite either.
							boolean overwrite = !(parentDAG == childDAG);

							// "removedNode" should be removed from both child and parent.
							if (i==splitNode) {
								parentDAG = parentDAG.removeNode(parentIndex, overwrite);
								childDAG = childDAG.removeNode(childIndex, overwrite);
							}
							// if node[i] is a parent, remove it from childDAG
							else if ((parentMask & UnlabelledGraph.nodeMask[i]) != 0) {
								childDAG = childDAG.removeNode(childIndex, overwrite);
								parentIndex ++;
							}
							// if node[i] is a child, remove it from parentDAG
							else if ((childMask & UnlabelledGraph.nodeMask[i]) != 0) {
								parentDAG = parentDAG.removeNode(parentIndex, overwrite);
								childIndex ++;
							}
							else { throw new RuntimeException("Unreachable code reached!"); }
						}
						
						// We are now left with parentDAG and childDAG.
						// As each element in parentDAG must come before each element in childDAG
						// we can simply multiply the combinations.
						return perms(parentDAG,operations) * perms(childDAG,operations);
						
					}
				}
			}

			// Check if there are any disconnected subgraphs in the network.
			// This is a more thurough check than opCheckUnconnected
			//
			// If a subgraph is found we create two graphs g1 and g2.
			// g1 is the subgraph containing node[0], g2 is the remaining nodes.
			// We calculate permutations as perms(g1)*perms(g2)*interleve(g1.numNodes,g2.numNodes)
			// where interleave is the number of ways two lists of length 'a' and 'b' may be combined
			// while maintaining the relative ordering of each individual list.
			if ( (operations & opCheckDisconnectedSubgraph) != 0) {				
				long subGraphMask = g.getSubgraphMask(0);
				long fullGraphMask = -1l >>> (64-numNodes);
				if (subGraphMask != fullGraphMask) {
					UnlabelledGraph g1 = g;
					UnlabelledGraph g2 = g;
					
					int g1Index = 0;
					int g2Index = 0;
					for ( int i = 0; i < numNodes; i++) {

						// TODO:  Set to false as !(g1 == g2) produced a bug. Investigate further.
						boolean overwrite = !(g1 == g2);
						if ((subGraphMask & UnlabelledGraph.nodeMask[i]) != 0) {
							g2 = g2.removeNode(g2Index, overwrite);
							g1Index++;
						}
						else {
							g1 = g1.removeNode(g1Index, overwrite);
							g2Index++;							
						}
					}
					
					return interleave(g1Index,g2Index) * perms(g1,operations) * perms(g2,operations);
					
				}	// end if subGraph != fullGraphMask			
			}

			// Count every entry that has to split (or has it's split in the cache)
			dCounterCalls[numNodes]++;

			// Lookup dag in hashtable.
			Long hash = null;
			if (numNodes < maxHashableSize) {
				if ((operations & opCheckDAGHash) != 0) { hash = new Long(g.getHash());	}
				else if ((operations & opCheckCanonicalDAGHash) != 0) {	hash = new Long(g.getCanonicalHash()); }
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
			
			// If less root nodes than leaf nodes, split on root nodes. Else leaf noded.
			int numRootNodes = 0;
			int numLeafNodes = 0;
			
			for ( int i = 0; i < numNodes; i++) {
				if ((g.rootNodes & UnlabelledGraph.nodeMask[i]) != 0) { numRootNodes++;	}
				else if ((g.leafNodes & UnlabelledGraph.nodeMask[i]) != 0) { numLeafNodes++; } 
			}
	
	
			
			long splitMask = g.rootNodes;
			if ( numRootNodes > numLeafNodes ) { splitMask = g.leafNodes; }
			double total = 0;
			for ( int i = 0; i < numNodes; i++ ) {
				if ( (splitMask & UnlabelledGraph.nodeMask[i]) != 0 ) {
					UnlabelledGraph g2 = g.removeNode(i, (i == numNodes-1));
					total += perms(g2,operations);
				}				
			}
				
				
			// put valud in hashtable for later use.
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
		 *  (equivelent to N choose R)*/
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
		 *  This is a more thurough version ov opCheckSingleRootOrLeafNode
		 *  which only finds single disconnected nodes. */
		public final static int opCheckDisconnectedSubgraph = 0x08;

		/** Should cache lookups be attempted? (Hash of Labelled DAGs)*/
		public final static int opCheckDAGHash = 0x10;
	
		/** Should cache lookups be attempted? (Using hash of canonical unlabelled DAGs) */
		public final static int opCheckCanonicalDAGHash = 0x20;
		
		/** Dags with >= maxHashableSize are not inserted in hashtable. */
		public final int maxHashableSize = 60;
	}

}
