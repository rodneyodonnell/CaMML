package camml.core.searchDBN;

import java.util.Random;

import camml.core.search.SECHash;
import camml.core.search.TOM;

/**Extends SECHash, but for learning DBNs.
 * See SECHash for details.
 */
public class DBNSECHash extends SECHash {
	private static final long serialVersionUID = 6878978332304068417L;

	java.util.Random rand;
	
	/** Random n*n matrix used to create hash (matrix2 used for temporal arcs)*/ 
    private long[][] matrix2;

	public DBNSECHash(Random rand, int numNodes) {
		super(rand, numNodes);
		this.rand = rand;
		initTemporalMatrix( numNodes );
	}

    /** Initialise 64bit random matrix - for temporal arcs component of hash
     *  */
    protected void initTemporalMatrix(int numNodes) {
        matrix2 = new long[numNodes][numNodes];
        for(int i = 0; i < matrix2.length; i++ ) {
            for(int j = 0; j < matrix2[i].length; j++ ){
            	matrix2[i][j] = rand.nextLong();
        	}
        }
    }
    
    /** Get hash component for temporal arcs, x->y */
    public long getTemporalRandom(int x, int y) {
        return matrix2[x][y];
    }   
	
	
	/** hash = (int)(logL*128) + sum_{intrasliceArcs}( matrix[i][j] ) + sum_{temporalArcs}{ matrix2[i][j] }
	 * Overrides SECHash.hash(...)
	 */
	public long hash(TOM tom, double logL) {
		long skelHash = super.hash(tom, logL);		//Get (logL*128) + sum_{intrasliceArcs}( matrix[i][j] ) 
        int numNodes = tom.getNumNodes();
        
        //Temporal arc components of the hash function:
        for( int i = 0; i < numNodes; i++ ){
        	int[] temporalParent = ((DNode)tom.getNode(i)).temporalParent;
        	for( int p : temporalParent ){
        		skelHash += getTemporalRandom( p, i );
        	}
        }

        return skelHash;
	}
}
