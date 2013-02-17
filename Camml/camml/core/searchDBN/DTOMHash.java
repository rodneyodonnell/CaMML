package camml.core.searchDBN;

import camml.core.search.TOM;
import camml.core.search.TOMHash;

/** Extends TOMHash. Same as TOMHash, but for hashing DTOMs instead of TOMs. */
public class DTOMHash extends TOMHash {
	private static final long serialVersionUID = -1983585885890421801L;

	java.util.Random rand;
	
	/** Random n*n matrix used to create hash (matrix2 used for temporal arcs)*/ 
    private long[][] matrix2;
	
	public DTOMHash(java.util.Random rand, int numNodes) {
		super(rand, numNodes);
		this.rand = rand;		//Ideally, TOMHash.rand would be protected, so we don't have two separate references...
		initTemporalMatrix( numNodes );
	}
	
    /** Initialise 64bit random matrix - for temporal arcs
     *  Virtually exactly the same as init(numNodes) in TOMHash
     *  matrix[][] used for intraslice arcs; matrix2[][] for temporal arcs
     *  */
    protected void initTemporalMatrix(int numNodes) {
        matrix2 = new long[numNodes][numNodes];
        for(int i = 0; i < matrix2.length; i++ ) {
            for(int j = 0; j < matrix2[i].length; j++ ){
            	matrix2[i][j] = rand.nextLong();
        	}
        }
    }
    
    /** Accessor function for temporal arc hash components; arc is x->y (order matters) */
    public long getTemporalRandom(int x, int y) {
        return matrix2[x][y];
    }   
	
    /**hash = sum_{intrasliceArcs}( matrix[i][j] ) + sum{temporalArcs}( matrix2[i][j] );
     * Overrides TOMHash.hash(...)
     */
	public long hash( TOM tom, double logL ){
		if( !(tom instanceof DTOM) ) throw new RuntimeException("Cannot hash TOM with DTOMHash!");
		
		long skelHash = super.hash( tom, logL);		//Get the hash component for the intraslice arcs
        int numNodes = tom.getNumNodes();
		
        for( int i = 0; i < numNodes; i++ ){
        	int[] temporalParent = ((DNode)tom.getNode(i)).temporalParent;
        	for( int p : temporalParent ){
        		skelHash += getTemporalRandom( p, i );
        	}
        }
		return skelHash;
	}
}
