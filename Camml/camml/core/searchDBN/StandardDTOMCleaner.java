package camml.core.searchDBN;

import camml.core.search.TOM;
import camml.core.search.TOMCleaner;

/** A TOMCleaner specifically for cleaning DTOMs. */
public class StandardDTOMCleaner implements TOMCleaner {

	public static StandardDTOMCleaner dtomCleaner = new StandardDTOMCleaner();
	
	/** Default (no argument) constructor */
	public StandardDTOMCleaner() { }

	public void cleanTOM(TOM tom) {
		if( !(tom instanceof DTOM ) ) throw new RuntimeException("Expected DTOM, passed TOM?");
		
		DTOM dtom = (DTOM)tom;
		
		//Loop through each node in DTOM and clean (both intraslice and temporal arcs)
		for( int i=0; i < dtom.getNumNodes(); i++ ){
			//Check intraslice parents: (code/loop taken from StandardTOMCleaner)
			int nodeI = tom.nodeAt(i);
			int[] dirtyParent = tom.getNode(nodeI).getParentCopy();
            
			double oldCost = tom.caseInfo.nodeCache.getMMLCost( tom.getNode(nodeI) );
            for (int j = dirtyParent.length-1; j >= 0; j--) {
                int nodeJ = dirtyParent[j];
                double structureDiff = tom.caseInfo.tomCoster.costToToggleArc(tom,nodeI,nodeJ);                
                tom.removeArc(nodeI,nodeJ);
                double newCost = tom.caseInfo.nodeCache.getMMLCost(tom.getNode(nodeI));
                if ( newCost > oldCost - structureDiff) {
                    tom.addArc(nodeI,nodeJ);
                }
                else {
                    oldCost = newCost;
                }
            }
			
            //Check TEMPORAL parents:
            int[] dirtyParentTemporal = ((DNode)dtom.getNode(nodeI)).getTemporalParentCopy();
            double oldCostTemporal = dtom.caseInfo.nodeCache.getMMLCost( dtom.getNode(nodeI) );
            for (int j = dirtyParentTemporal.length-1; j >= 0; j--) {
                int nodeJ = dirtyParentTemporal[j];
                double structureDiff = ((DTOMCoster)dtom.caseInfo.tomCoster).costToToggleTemporalArc(dtom, nodeJ, nodeI);	//nodeJ is parent, nodeI is child; order matters in this function call...
                dtom.removeTemporalArc( nodeJ, nodeI );	//nodeJ is parent, nodeI is child...
                double newCost = dtom.caseInfo.nodeCache.getMMLCost(dtom.getNode(nodeI));
                if ( newCost > oldCostTemporal - structureDiff) {
                	dtom.addTemporalArc(nodeJ, nodeI);	//nodeJ is parent, nodeI is child...
                }
                else {
                    oldCostTemporal = newCost;
                }
            }
		}
	}
}
