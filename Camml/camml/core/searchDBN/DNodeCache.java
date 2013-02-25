package camml.core.searchDBN;

import camml.core.models.ModelLearner;
import camml.core.search.Node;
import camml.core.search.NodeCache;
import cdms.core.Value.Vector;

/**Extends NodeCache for learning DBNs. Minimal changes made */
public class DNodeCache extends NodeCache {
	
	/** Analogous to parentHashArray and childHashArray, but for temporal arcs 
	 * (i.e. parents in timeslice t-1)
	 */
	protected long[] parentTemporalHashArray;

	
	public DNodeCache(Vector data, ModelLearner mmlLearner,
			ModelLearner mlLearner) {
		super(data, mmlLearner, mlLearner);
	}

	public DNodeCache(Vector data, ModelLearner mmlLearner,
			ModelLearner mlLearner, double mbPerNode) {
		super(data, mmlLearner, mlLearner, mbPerNode);
	}
	
	
	/** Generate an index into the cache.
	 *  NOTE: Code largely taken from NodeCache.getIndex(). Only changes are for temporal (inter-slice) parents
	 *  OVERRIDES NodeCache.getIndex()
	 * */
    protected int getIndex( Node node ) {
        getIndexCalled ++;
        
        // if not allocated, create parentHash and childHash
        if ( parentHashArray == null ) {
            java.util.Random rand = new java.util.Random(123);
            parentHashArray = new long[numNodes];
            childHashArray = new long[numNodes];
            parentTemporalHashArray = new long[numNodes];
            for ( int i = 0; i < parentHashArray.length; i++ ) {
                parentHashArray[i] = rand.nextLong();
                childHashArray[i] = rand.nextLong();
                parentTemporalHashArray[i] = rand.nextLong();
            }
        }
        
        // create hash for node.  hash is a random 63 bit value representing each parent[]->child
        // combinations.  It is possible (though remarkably unlikely) for two nodes to hash to the
        // same value.  No checks are in place as this will probably never happen.
        long nodeHash = childHashArray[ node.var ];
        int[] intrasliceParents = node.getParentCopy();
        for ( int i = 0; i < intrasliceParents.length; i++ ) {
        	nodeHash += parentHashArray[ intrasliceParents[i] ];
        }
        
        //Consider temporal parents:
        int[] temporalParents = ((DNode)node).getTemporalParentCopy();
        for( int i=0; i < temporalParents.length; i++ ){
        	nodeHash += parentTemporalHashArray[ temporalParents[i] ];
        }
        
        // we use a 63 bit value so nodeHash is always positive.
        nodeHash &= 0x7FFFFFFFFFFFFFFFl;
        
        // Using nodeHash create an index into arrays.
        int nodeIndex = (int)(nodeHash % cacheSize);
        
        // use quadratic probing to find a free entry in the hashtable.  This has better behaviour
        // than linear probe (although it is not guarenteed to fill the entire table.)
        for ( int i = 0; i < maxCacheAttempts; i++ ) {
            
            // Quadratic probing, each failure we lookahead an additional 'i' spaces.
            nodeIndex = (nodeIndex + i) % cacheSize;
            
            // If the nodeHash is found, return nodeIndex.
            if ( hashLookup[nodeIndex] == nodeHash ) {
                hashSucceed ++;
                
                return nodeIndex;
            }
            
            // If an empty entry is found, assign nodeHash to this nodeIndex
            if ( hashLookup[ nodeIndex ] == -1 ) {
                hashLookup[ nodeIndex ] = nodeHash;
                
                newHash ++;
                numCacheEntries ++;
                return nodeIndex;
            }
            
            // current cache entry already used by another node, so try again with new index.
            hashFailed ++;
        }    
                
        // We are forced to overwrite a value.
        nodeIndex = (int)(nodeHash % cacheSize);
        overwriteCacheEntry( nodeIndex, nodeHash );
        
        return nodeIndex;
    }
}
