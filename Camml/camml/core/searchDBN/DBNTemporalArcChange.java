package camml.core.searchDBN;

import java.util.Random;

import camml.core.search.CaseInfo;
import camml.core.search.TOM;
import camml.core.search.TOMTransformation;

/**Attempts a change to one temporal arc in DTOM (addition or removal).
 * Analogous to SkeletalChange but for DBN interslice arcs.
 */
public class DBNTemporalArcChange extends TOMTransformation {

	/** cost to add TEMPORAL arc = -log(arcProbTemporal) - -log(1.0-arcProbTemporal)*/
	double arcCostTemporal;
	
	public DBNTemporalArcChange(Random rand, double arcProbTemporal, CaseInfo caseInfo,
			double temperature) {
		super(rand, caseInfo, temperature);
		arcCostTemporal = Math.log(arcProbTemporal / (1.0 - arcProbTemporal));
	}

	/** Child node changed by last mutation */
    private int childChanged;
	
    /** changed[] is allocated to save reallocating an array every time 
     * getNodesChanged() is called */
    private final int[] changed = new int[1];
    
	public int[] getNodesChanged() {
		changed[0] = childChanged;
		return changed;
	}

	/** Attempt a modification of the specified DTOM.
	 * Overrides TOMTransformation.transform(...). Requires a DTOM to be passed, not a TOM.
	 */
	public boolean transform(TOM tom, double ljp){
		if( !(tom instanceof DTOM) ) throw new RuntimeException("Invalid TOM - must be DTOM");
		
		DTOM dtom = (DTOM)tom;
		
		//Pick a random set of variables, then toggle the existence of an arc
		int parent = (int)(rand.nextDouble() * dtom.getNumNodes() );
        int child = (int)(rand.nextDouble() * dtom.getNumNodes() );
        
        childChanged = child;	//Set childChanged variable for getNodesChanged();
        
        //Check if mutation will add too many parents to node:
        DNode node = (DNode)dtom.getNode(child);
        int change;
        if( dtom.isTemporalArc(parent, child) ){
        	change = -1;	//Arc already exists -> remove
        }else{
        	change = 1;		//Arc does not exist -> add
        }
        
        if( node.getNumParents() + change >= dtom.getMaxNumParents() )
        	return false;	//Return false if too many parents are present for this node already
		
        //Cost to change network:
        double oldCostJ = caseInfo.nodeCache.getMMLCost( node );	//Node cost before
        final double costToToggleArc = ((DTOMCoster)caseInfo.tomCoster).costToToggleTemporalArc(dtom,parent,child); //Structure cost difference
        
        //Change network:
        if( dtom.isTemporalArc(parent, child)) {
            dtom.removeTemporalArc(parent, child);	            
        }else{        
            dtom.addTemporalArc(parent, child);
        }
        
        //Calculate new cost:
        double newCostJ = caseInfo.nodeCache.getMMLCost( node );	//Node cost after
        
        oldCost = 0;	//Used by TOMTransformation; set to 0, as per SkeletalChange etc...
        
        cost = newCostJ - oldCostJ + costToToggleArc;		//i.e. difference in node cost + change in structure cost
        
        
        if( accept() ){
        	//TODO: Add code for tracking of arc weights, as in SkeletalChange etc...
        	return true;
        } else{	//Undo change:
        	if( dtom.isTemporalArc(parent, child)) {
                dtom.removeTemporalArc(parent, child);	//Current arc: must have previously added...
            }
            else {        
                dtom.addTemporalArc(parent, child);		//No current arc: must have previously removed...
            }
        	return false;
        }
	}
}
