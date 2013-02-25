package camml.core.searchDBN;

import java.io.IOException;

import camml.core.models.ModelLearner;
import camml.core.search.CaseInfo;
import camml.core.search.SEC;
import camml.core.search.TOM;
import cdms.core.Value;

/**Extends SEC class. Exactly the same function an the SEC class, but with a few additions
 * specific to DBNs - i.e. storing of temporal arcs, dealing with DTOMs etc. 
 */
public class DBNSEC extends SEC {
	private static final long serialVersionUID = -5558151576419093733L;
	
	/** List of temporal (interslice) edges in this SEC.
	 *  Arc direction is: edgeListTemporal[1][i] -> edgeListTemporal[0][i] */
	private final int[][] edgeListTemporal;

	/** Required - need to set posterior when combining SECs into MMLECs, but DBN code
	 *  is in a different package (and double posterior) is protected...
	 */
	protected void setPosterior( double newPosterior ){
		posterior = newPosterior;
	}
	
	/**Constructor: Creates new DBN SEC.
	 * As per SEC class, addTOM must still be called to add the first DTOM
	 */
	public DBNSEC(TOM cleanTom, double cleanMLCost, CaseInfo caseInfo) {
		super(cleanTom, cleanMLCost, caseInfo);	//Sets intraslice arcs, stores ML score, creates ArrayList of CompactTOMs
		
		if( !(cleanTom instanceof DTOM) ) throw new RuntimeException("Expected DTOM, passed TOM?");
		
		//Extract temporal edges from DTOM:
		DTOM dtom = (DTOM)cleanTom;
		int numEdges = dtom.getNumEdges() + dtom.getNumTemporalEdges();	//Intraslice edges + temporal edges
		edgeListTemporal = new int[2][numEdges];
		
		int numNodes = dtom.getNumNodes();
		
		int edgeNum = 0;
		for( int i=0; i<numNodes; i++ ){	//Loop through all nodes/variables
			DNode currNode = (DNode)dtom.getNode(i);
			int[] nodeTemporalParents = currNode.getTemporalParentCopy();
			for( int j : nodeTemporalParents ){
				edgeListTemporal[0][edgeNum] = i;		//Child
				edgeListTemporal[1][edgeNum] = j;		//Parent
				edgeNum++;
			}
		}
	}
	
	
	/**Overrides SEC.addTOM(...)
	 * Code very similar to SEC.addTOM(...) but will create a CompactDTOM instead of a CompactTOM
	 * if required.
	 */
	public void addTOM( TOM cleanTom, double uncleanMML, double temperature ){
		// hash a clean version of this TOM
        long hash = caseInfo.tomHash.hash( cleanTom, cleanMLCost );
        int foundAt = -1;
        CompactTOM currentTOM = null;
        
        
        if ( uncleanMML < bestMML ) { bestMML = uncleanMML; }
        
        // In rodocamml, the MML model which represents a SEC is chosen by the best
        // (lowest MML) cost sampled.  After sampling the MML cost kept is the cost of
        // the CLEAN version of the best sampled TOM.  Realistically this is probably
        // a bug which should be fixed in rodocamml but is modified here for convenience.
        // NOTE: Using this code (inside if(caseInfo.regression){}) when a new TOM is
        // sampled is probably a better solution (which however fails regression tests...)
        if ( caseInfo.regression ) {
            if ( uncleanMML < bestUncleanMML ) { 
                bestUncleanMML = uncleanMML; 
                
                // The tom visited may not be clean.  
                // The mml cost of the clean TOM is required.
                TOM tempTOM = (TOM)cleanTom.clone();
                tempTOM.clean();
                double tempCost = tempTOM.getCost();
                if ( tempCost < bestMML ) { bestMML = tempCost; }
            }
        }
        
        // Check if this TOM has already been sampled
        for ( int i = 0; i < tomList.size(); i++ ) {
            if ( ((CompactTOM)tomList.get(i)).hash == hash ) { foundAt = i; break;}
        }
        
        // If TOM not already in the list, add it. 
        if ( foundAt == -1 ) {
            tomList.add( new CompactDTOM( cleanTom, hash ) );		//Only change in this method vs. SEC.addTOM(...)
            foundAt = tomList.size() - 1;
            currentTOM = ((CompactTOM)tomList.get(foundAt));
            currentTOM.bestMML = uncleanMML;        
        }
        else { // if TOM is already in the list, make sure it's best MML is recorded.
            currentTOM = ((CompactTOM)tomList.get(foundAt));
            if ( currentTOM.bestMML > uncleanMML ) {
                currentTOM.bestMML = uncleanMML;
            }
        }
        
        // update TOM and SEC weights
        double diff = caseInfo.referenceWeight - uncleanMML;
        double tomWeight = Math.exp( diff * (1.0 - 1.0/temperature));
        currentTOM.addWeight( tomWeight );  // add weight to TOM
        this.weight += tomWeight;           //            and SEC
        caseInfo.totalWeight += tomWeight;  //            and total
        
        if ( caseInfo.logging ) {
            java.text.DecimalFormat format = caseInfo.weightFormat;
            try {
                caseInfo.cammlLog.write( "weight = " + format.format(tomWeight) + 
                                         "\ttomWeight = " + format.format(this.weight) + 
                                         "\tdiff = " + format.format(diff) +  
                                         "\ttotalWeight = " + format.format(caseInfo.totalWeight) + "\n");
                caseInfo.cammlLog.flush();
            } catch (IOException e) { /* Ignore Exception */ }
        }   
	}
	

	/** Compact representation of DTOM. As with CompactTOM, list of edges
	 *    (temporal or intraslice) are not required, as these are the same
	 *    for each TOM in the SEC
	 */
	protected class CompactDTOM extends SEC.CompactTOM{
		private static final long serialVersionUID = 6136384853723114359L;

		public CompactDTOM(TOM tom, long hash) {
			super(tom, hash);
			//Variable order stored using CompactTOM constructor
			//Arcs stored in SEC and DBNSEC, so not needed to be stored in CompactDTOM
		}
		
		/** Create full DTOM from CompactDTOM
		 *  Overrides SEC.CompactTOM.makeTOM()
		 */
		public TOM makeTOM(){
			//Create DTOM:
			DTOM tom = new DTOM( caseInfo );
			
			
			//Following code taken from SEC.CompactTOM.makeTOM()
			// set total ordering to match CompactTOM
            // This must be done before adding arcs to avoid having too many parents present.
            for ( int i = 0; i < order.length; i++ ) {
                tom.swapOrder( tom.nodeAt(i), this.order[i], false);
            }
            
            for ( int i = 0; i < edgeList2[0].length; i++) {
                tom.addArc(edgeList2[0][i], edgeList2[1][i]);
            }
            
            
            //Add the temporal arcs for the DTOM:
            for( int i=0; i < edgeListTemporal[0].length; i++ ){
            	tom.addTemporalArc( edgeListTemporal[1][i], edgeListTemporal[0][i] );
            	//i.e. add temporal edge from parent variable to child variable
            }
			
            return tom;
		}
		
		/**Overrides SEC.CompactTOM.toString() */
		public String toString(){
			return "CompactDTOM : numVisits = " + numVisits + "\t" + 
	                "totalWeight = " + totalWeight + "\t" + 
	                "bestMML = " + bestMML; 
		}
		
		/** Calculate logP(Data|Model,Params) for tom[i] 
		 *  OVERRIDES SEC.CompactTOM.getDataCost()   */
		public double getDataCost(){
			//throw new RuntimeException("Method not implemented!");
			if ( dataCost != -1) { return dataCost;}
            DTOM tom = (DTOM)makeTOM();
            int numNodes = tom.getNumNodes();
            Value.Vector params;
            
            try { params = tom.makeParameters(caseInfo.mmlModelLearner); }
            catch (ModelLearner.LearnerException e) { return Double.POSITIVE_INFINITY;}
            
            
            //Calculate the various pieces required, as per BNet.logP(...)
            Value.Model[] subModel = DBNStaticMethods.makeSubModelListDBN(params);		//BNet.makeSubModelList( (Value.Vector)y )
            Value[] subParam = DBNStaticMethods.makeSubParamListDBN(params);			//BNet.makeSubParamList( (Value.Vector)y )
            int[] order = this.order.clone();
            int[][] parentList = new int[numNodes][0];
            int[][] temporalParentList = new int[numNodes][0];
            
            //Create arrays of parents, and arrays of temporal parents:
            for( int i=0; i < numNodes; i++ ){
            	parentList[i] = tom.getNode(i).getParentCopy();
            	temporalParentList[i] = ((DNode)tom.getNode(i)).getTemporalParentCopy();
            }
            
            double dataCost = -DBNStaticMethods.DBNlogP( caseInfo.data, subModel, subParam, order, parentList, temporalParentList );
            
            return dataCost;
		}
	}
}
