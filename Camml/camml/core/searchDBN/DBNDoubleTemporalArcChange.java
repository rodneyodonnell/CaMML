package camml.core.searchDBN;


import java.util.Random;

import camml.core.search.CaseInfo;
import camml.core.search.TOM;
import camml.core.search.TOMTransformation;

/**Attempts a change to two temporal arc in DTOM (addition or removal),
 * with a common child variable. i.e. toggles A->C and B->C.
 * based on DoubleSkeletalChange.
 */
public class DBNDoubleTemporalArcChange extends TOMTransformation {

	/** cost to add TEMPORAL arc = -log(arcProbTemporal) - -log(1.0-arcProbTemporal)*/
	double arcCostTemporal;
	
	/** pre-allocated return value for getNodesChanged() */
    private int[] changes = new int[1];
    
    /** Return array of changes nodes <br> 
     *  NOTE: Values within returned array are volatile and will often be changed is transform()
     *        is called again.  */
    public int[] getNodesChanged() { return changes; }

	public DBNDoubleTemporalArcChange(Random rand, double arcProbTemporal, CaseInfo caseInfo,
			double temperature) {
		super(rand, caseInfo, temperature);
		arcCostTemporal = Math.log(arcProbTemporal / (1.0 - arcProbTemporal));
	}

	/** Attempt a modification of the specified DTOM.
	 * Overrides TOMTransformation.transform(...). Requires a DTOM to be passed, not a TOM.
	 */
	public boolean transform(TOM tom, double ljp) {
		if( !(tom instanceof DTOM) ) throw new RuntimeException("Expected DTOM; passed TOM?");
		DTOM dtom = (DTOM)tom;
		
		int numNodes = tom.getNumNodes();
		
		//At least 2 nodes are required to do a double temporal arc change
		if( numNodes < 2 ) return false;
		
		//There are N^2 possible arcs in a DBN... can choose any two arcs...
		//Pick two arcs to toggle with a common child: i.e. toggle A->C and B->C
		
		int varA = (int)( numNodes * rand.nextDouble() );
		int varB = (int)( numNodes * rand.nextDouble() );
		int varC = (int)( numNodes * rand.nextDouble() );
		
		while( varA == varB ){
			varA = (int)( numNodes * rand.nextDouble() );
		}
		
		//Set node changed:
		changes[0] = varC;
		
		// Check if the mutation will add too many parents to NodeJ    
        // addArc is the total number of arcs which will be added/removed by this mutation.
        int addArc = 0;
        if ( dtom.isTemporalArc( varA, varC ) ) addArc--; else addArc++;
        if ( dtom.isTemporalArc( varB, varC ) ) addArc--; else addArc++;
        DNode nodeC = (DNode)dtom.getNode(varC);
        if( nodeC.getNumParents() + addArc >= dtom.getMaxNumParents() )
            return false;  // return false if too many parents are present after change.
        
        int[] parents = new int[]{ varA, varB };
        int[] children = new int[]{ varC, varC };
        
        double toggleCost = ((DTOMCoster)caseInfo.tomCoster).costToToggleTemporalArcs(dtom, parents, children);
        
        //Calculate old cost for node (before making changes)
        double oldChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
        
        //Toggle existence of arcs in DTOM:
        doubleTemporalMutate( dtom, varA, varB, varC );
        
        //New cost for node (after making changes)
        double newChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
        
        oldCost = 0;
        cost = newChildCost - oldChildCost + toggleCost;	//i.e. difference in node cost + structure cost difference
        //cost = toggleCost;
        
        
        if( accept() ){
        	//TODO: Implement tracking of arc weights, as per DoubleSkeletalChange etc...
        	
        	return true;
        } else{
        	//Undo change:
        	doubleTemporalMutate( dtom, varA, varB, varC );
        	return false;
        }
	}
	
	/** Toggle arcs varA -> varC and varB -> varC */
	private void doubleTemporalMutate( DTOM dtom, int varA, int varB, int varC ){
        if( dtom.isTemporalArc(varA, varC) ){
        	dtom.removeTemporalArc(varA, varC);
        } else {
        	dtom.addTemporalArc(varA, varC);
        }
        if( dtom.isTemporalArc(varB, varC) ){
        	dtom.removeTemporalArc(varB, varC);
        } else {
        	dtom.addTemporalArc(varB, varC);
        }
        
	}
}
