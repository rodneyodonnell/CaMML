package camml.core.searchDBN;

import camml.core.search.TOM;
import camml.core.search.TOMCoster;

public interface DTOMCoster extends TOMCoster {
	
	/** Returns the difference in cost between DTOM and DTOM with an arc added/removed.
	 * Arc is node1 -> node2
	 */
	public double costToToggleTemporalArc( DTOM dtom, int node1, int node2 );
	
	/** Returns the difference in cost between DTOM and DTOM with multiple arcs added/removed.
	 * Arcs are node1[i] -> node2[i]
	 */
	public double costToToggleTemporalArcs( DTOM dtom, int[] node1, int[] node2 );
	
	/** Modify the current TEMPORAL arc probability. Affects structure cost */
	public void setArcProbTemporal( double arcProbTemporal );
	
	public class UniformDTOMCoster extends UniformTOMCoster implements DTOMCoster {
		private static final long serialVersionUID = -8918732440596097584L;

		/** Probability of a temporal (inter-slice) arc existing */
		private double temporalArcProb;
		
		private double costToAddTemporalArc;
		
		/** Constructor. Prior over intraslice arcs (arcP) and temporal arcs (temporalArcP) passed.
		 */
		public UniformDTOMCoster(double arcP, double temporalArcP ) {
			super(arcP);
			this.temporalArcProb = temporalArcP;
			costToAddTemporalArc = -Math.log(temporalArcProb) + Math.log(1.0-temporalArcProb);
		}
		
		public void setArcProbTemporal( double arcProbTemporal ){
			this.temporalArcProb = arcProbTemporal;
			this.costToAddTemporalArc = -Math.log(temporalArcProb) + Math.log( 1.0 - temporalArcProb );		}
		
		/** Return the structure cost of the given DTOM
		 *  Overrides UniformTOMCoster.cost(...). Must pass DTOM (not a TOM)
		 * */
	    public double cost( TOM tom ){ 
	        if( !(tom instanceof DTOM) ){
	        	throw new RuntimeException("Expected DTOM, passed TOM into UniformDTOMCoster.cost(...)?");
	        }
	        double intrasliceArcCost = super.cost( tom );
	        DTOM dtom = (DTOM)tom;
	        
	        int maxNumTemporalArcs = ((DTOM)tom).getNumNodes() * ((DTOM)tom).getNumNodes();
	        
	        int numTemporalArcs = dtom.getNumTemporalEdges();
	        
	        double temporalArcCost = -1.0 * ( numTemporalArcs * Math.log( temporalArcProb ) + 
	                (maxNumTemporalArcs - numTemporalArcs) * Math.log(1.0-temporalArcProb));
	        
	        return intrasliceArcCost + temporalArcCost;
	    }
	    
	    /** STRUCTURE Cost to toggle a TEMPORAL arc, nodeParent -> nodeChild.
	     *  Does not take into account difference in cost due to parameters/data
	     *  As with TOMCoster.UniformTOMCoster.costToToggleArc(...) this function is provided
	     *  as an optimization for MetropolisSearchDBN and AnnealSearchDBN */
	    public double costToToggleTemporalArc( DTOM dtom, int nodeParent, int nodeChild ) {
	        if ( dtom.isTemporalArc(nodeParent,nodeChild) ) { return -costToAddTemporalArc; }	//Removing an arc
	        else { return costToAddTemporalArc; }												//Adding an arc
	    }
	    
	    /** STRUCTURE Cost to toggle MULTIPLE temporal arcs; temporalParent[i] -> children[i]
	     *  Does not take into account difference in cost due to parameters/data
	     *  As with TOMCoster.UniformTOMCoster.costToToggleArcs(...) this function is provided
	     *  as an optimization for MetropolisSearchDBN and AnnealSearchDBN*/
	    public double costToToggleTemporalArcs( DTOM dtom, int temporalParents[], int children[] ) {
	    	
	    	int added = 0;
	    	for( int i=0; i<temporalParents.length; i++ ){
	    		if( dtom.isTemporalArc( temporalParents[i], children[i] ) ) added--;
	    		else added++;
	    	}
	    	return added * costToAddTemporalArc;
	    }
	}
}
