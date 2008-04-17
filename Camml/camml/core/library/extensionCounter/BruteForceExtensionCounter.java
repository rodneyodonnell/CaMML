package camml.core.library.extensionCounter;

/** Algorithm implemented in original CaMML */
public class BruteForceExtensionCounter {

	/** Static instance of WallaceCounter */
	public static final BruteForceExtensionCounter wCounter = new BruteForceExtensionCounter();

	/** Extension Counter taken from Chris Wallace's original CaMML code. (with modifications) */
	long perms( UnlabelledGraph64 g, int lev ) {
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
		long[] vbit = ExtensionCounterLib.nodeMask;
	
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
	public long lperms(UnlabelledGraph64 g)
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