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
}
