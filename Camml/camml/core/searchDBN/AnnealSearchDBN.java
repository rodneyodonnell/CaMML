package camml.core.searchDBN;

import java.util.Random;

import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner;
import camml.core.search.AnnealSearch;
import camml.core.search.CaseInfo;
import camml.core.search.DoubleSkeletalChange;
import camml.core.search.NodeCache;
import camml.core.search.ParentSwapChange;
import camml.core.search.SECHash;
import camml.core.search.SkeletalChange;
import camml.core.search.TOM;
import camml.core.search.TOMHash;
import camml.core.search.TOMTransformation;
import camml.core.search.TemporalChange;
import cdms.core.Value;
import cdms.core.Value.Vector;

/**AnnealSearchDBN is an extension of CaMML's Anneal Search, designed to
 * search through the space of Dynamic Bayesian Networks (DBNs). <br>
 * AnnealSearch methods are overridden as required. 
 * @Author Alex Black
 */
public class AnnealSearchDBN extends AnnealSearch{
	
	
	/**Additional mutation operators, as per MetropolisSearchDBN.
	 * Other mutation operators are defined in BNetSearch */
	protected DBNTemporalArcChange dbnTemporalArcChange;				//Make a single change to a temporal arc in the DBN
	protected DBNDoubleTemporalArcChange dbnDoubleTemporalArcChange;	//Make two changes to temporal arcs in the DBN
	
	/** Prior on temporal arcs being present. Analogous to BNetSearch.arcProb, but for temporal arcs only */
	protected double arcProbTemporal = 0.5;
	
	/** Alternate constructor for AnnealSearchDBN. Creates a CaseInfo object automatically,
	 *   and uses a UniformDTOMCoster with arcProb=0.5 and arcProbTemporal=0.5  */
	public AnnealSearchDBN(Random rand, Vector data,
			ModelLearner mlModelLearner, ModelLearner mmlModelLearner) {
		super(rand, data, mlModelLearner, mmlModelLearner);
		
		//Set TOMCoster:
		this.tomCoster = new DTOMCoster.UniformDTOMCoster(0.5,0.5);
		this.caseInfo.tomCoster = this.tomCoster;
		
		//Create new DBN-specific Hash functions in place of ones set in AnnealSearch constructor
		caseInfo.tomHash = new DTOMHash( rand, numNodes );
		caseInfo.secHash = new DBNSECHash( rand, numNodes );
		caseInfo.secHash.caseInfo = caseInfo;
		caseInfo.tomHash.caseInfo = caseInfo;
		
		//Create new DBN Node Cache - to replace one set by BNetSearch constructor:
		caseInfo.nodeCache = new DNodeCache( data, mmlModelLearner, mlModelLearner );
		caseInfo.nodeCache.caseInfo = caseInfo;
		
		//Set new DTOM to tom variable, bestTOM variable, etc (replace TOM set by BNetSearch constructor)
		this.tom = new DTOM(caseInfo);
		tomCoster.repairTOM(tom);
		this.currentCost = costNetwork( mmlModelLearner, false );
		
		this.cleantom = (DTOM)tom.clone();
		
		this.bestTOM = (DTOM)tom.clone();
		this.bestCost = this.currentCost;
		
		setArcProbTemporal(0.5);
		
		//Set new TOMCleaner for DTOMs (instead of one used in CaseInfo, by default)
		caseInfo.tomCleaner = StandardDTOMCleaner.dtomCleaner;
	}

	/** Constructor for AnnealSearchDBN. Random object is passed, as is caseInfo object.
	 *  Constructor assumes that caseInfo was set up by MetropolisSearchDBN */
	public AnnealSearchDBN(Random rand, CaseInfo caseInfo) {
		/* This is a hack-ish workaround to avoid a problem associated with the superclass constructors.
		 * BNetSearch constructor will cost a network (a new, empty TOM) using the NodeCache object. Here, the
		 * NodeCache is actually a DNodeCache (set in the caseInfo object by MetropolisSearchDBN), which cannot
		 * cost TOMs (only DTOMs). As such, a temporary CaseInfo object is created and passed.
		 */
		super( rand, makeTempCaseInfo(rand, caseInfo) );
		
		//Now, fix everything up
		this.caseInfo = caseInfo;
		
		this.tomCoster = caseInfo.tomCoster;
		
		this.tom = new DTOM(caseInfo);
		tomCoster.repairTOM(tom);
		this.currentCost = costNetwork( mmlModelLearner, false );
		
		this.cleantom = (DTOM)tom.clone();
		
		this.bestTOM = (DTOM)tom.clone();
		this.bestCost = this.currentCost;
		
		setArcProbTemporal(0.5);
		setArcProb(0.5);
	}
	
	/* An ugly workaround to avoid problems from using DBN classes in BNetSearch constructor, without having
	 * to rewrite these superclasses.
	 * If we pass the 'real' caseInfo object, the BNetSearch superclass constructor will ultimately use the
	 * caseInfo.DNodeCache is used to cost a TOM (not a DTOM) resulting in an error (DNodeCache objects can
	 * only be used to cost DTOMs, not TOM objects)
	 * */ 
	private static CaseInfo makeTempCaseInfo( Random rand, CaseInfo previousCaseInfo ){
		int numNodes = ((Value.Structured) previousCaseInfo.data.elt(0)).length();
		return new CaseInfo( new TOMHash(rand, numNodes), new SECHash(rand, numNodes), previousCaseInfo.data,
				previousCaseInfo.mmlModelLearner, previousCaseInfo.mmlModelLearner, 0,
				new NodeCache( previousCaseInfo.data, previousCaseInfo.mmlModelLearner, previousCaseInfo.mmlModelLearner ) );
	}
	
	/**Resets the search; sets the DTOM to an empty model (no arcs) with random ordering
	 * Overrides AnnealSearch.reset() (but also calls it)
	 */
	public void reset(){
		super.reset();
		if( !fixedArcProb ){ setArcProbTemporal(0.5); }
	}
	
	/** Calculates the temporal arc probability/density, based on the current best DTOM
	 *  Analogous to AnnealSearch.getBestArcProb(), but for temporal arcs only */
	public double getBestArcProbTemporal(){
		int maxArcsTemporal = numNodes * numNodes;		//N^2 possible temporal arcs in DBN (N vars per time slice)
		return ( (double)getBestNumArcsTemporal()+0.5) / (double)(maxArcsTemporal+1.0);
		//Regarding the +0.5 and +1.0: that's how it's done in AnnealSearch.getBestArcProb()
		//I'm guessing it would be to avoid arc probabilities of 0.0 and 1.0, which would lead to an
		// infinite cost for anything but full connectivity (for 1.0) or zero connectivity (0.0) in the DTOM
	}
	
	/**Return the number of TEMPORAL arcs (only) in the current best DTOM 
	 * Directly analogous to AnnealSearch.getBestNumArcs(), but for temporal arcs only */
	public int getBestNumArcsTemporal(){
		return ((DTOM)bestTOM).getNumTemporalEdges();
	}
	
	
	/**Perform a single mutation step. Return true if model is changed.
	 * Overrides AnnealSearch.step(...)
	 * Only minor changes vs. AnnealSearch version that it overrides: <br>
	 * - Addition of new DTOM temporal arc changes<br>
	 * - Change in mutation probabilities<br>
	 * Note: Mutation probabilities are such that:<br>
	 * - DBN_CaMML( NumEpochs * parentSwapChangeProb ) = CaMML( NumEpochs * parentSwapChangeProb )<br>
	 * - DBN_CaMML( NumEpochs * doubleSkeletalChangeProb ) = CaMML( NumEpochs * doubleSkeletalChangeProb )<br>
	 * - DBN_CaMML( NumEpochs * parentSwapChangeProb ) = CaMML( NumEpochs * parentSwapChangeProb )<br>
	 * - DBN_CaMML( NumEpochs * temporalChangeProb ) = CaMML( NumEpochs * temporalChangeProb )<br>
	 * - DBN_CaMML( NumEpochs * dbnTemporalArcChangeProb / maxTemporalArcs ) = CaMML( NumEpochs * skeletalChangeProb / maxArcs )<br>
	 * - DBN_CaMML( NumEpochs * dbnDoubleTemporalArcChangeProb / maxTemporalArcs ) = CaMML( NumEpochs * doubleSkeletalChangeProb / maxArcs )<br>  
	 * The reasoning being: The average number of each mutation type attempted should be approximately the same (overall or on a per-arc basis)
	 * as original (non-DBN) CaMML. 
	 */
	public boolean step( long stepNum ){
		TOMTransformation transform;

        // Randomly choose class of transformation to attempt.
        double rnd = rand.nextDouble();
        if (rnd < 0.07143) {         // 1/14 chance
            transform = parentSwapChange;
        } else if (rnd < 0.14286) {  // 1/14 chance
            transform = doubleSkeletalChange;
        } else if (rnd < 0.28571 ) { // 1/7 chance
            transform = skeletalChange;
        } else if (rnd < 0.42857 ){  // 1/7 chance
            transform = temporalChange;
        } else if (rnd < 0.71429 ){  // 2/7 chance
        	transform = dbnTemporalArcChange;
        } else {					 // 2/7 chance
        	transform = dbnDoubleTemporalArcChange;
        }
        
        // was it successful?
        boolean accepted = transform.transform( tom, currentCost );
        
        //Code below: As per AnnealSearch.
        if ( caseInfo.annealLogging ) {
            try {
                caseInfo.cammlLog.write( "STEP : " + stepNum +
                                         "\tMMLCost = " + caseInfo.costFormat.format(currentCost) +
                                         "\trand: " + ((WallaceRandom)rand).numCalls + "\n");
                caseInfo.cammlLog.flush();
            } catch (Exception e) {
                for ( int ii = 0; ii < 100; ii++ ) { System.out.println(e); }
                throw new RuntimeException(e);
            }
        }
        
        // if new model is accepted, update currentCost
        if (accepted) {
            int[] nodesChanged = transform.getNodesChanged();
            currentCost = costNodes( mmlModelLearner, false, nodesChanged ) + structureCost( false );
        }    
        
        // Save best DTOM, if better cost that the current best cost
        if ( currentCost + 0.001 < bestCost ) {
            bestTOM.setStructure(tom);		
            bestCost = currentCost;
        }
        
        return accepted;
	}
	
	/**Run N mutation steps.
	 * Overrides AnnealSearch.doSteps(...)
	 * Only minor changes regarding setting temporal arc probability.
	 * Code otherwise taken directly from AnnealSearch.doSteps(...)
	 */
	public void doSteps( long numSteps ){
		// run for n steps
        currentCost = costNetwork( mmlModelLearner, false );
        for ( long i = 0; i < numSteps; i++ ) {
            step(i);
        }
        
        // set arcProb based on bestTOM
        int maxEdges = numNodes * (numNodes - 1) / 2;
        int maxEdgesTemporal = numNodes * numNodes;
        if ( !fixedArcProb ) { 
            setArcProb( (bestTOM.getNumEdges() + 0.5)/ (maxEdges + 1.0) );
            setArcProbTemporal( (((DTOM)bestTOM).getNumTemporalEdges() + 0.5) / (maxEdgesTemporal + 1.0 ) );
        }
        
        // Clean and recalculate arcProb several times to ensure we have the best
        // clean model possible.
        for ( int i = 0; i < 10; i++ ) {
            // reclean the best tom to see if changing arcProb has any effect
            bestTOM.clean();
            
            // fix arcProb based on recleaned best TOM.
            if ( !fixedArcProb ) { 
                setArcProb( (bestTOM.getNumEdges() + 0.5)/ (maxEdges + 1.0) );
                setArcProbTemporal( (((DTOM)bestTOM).getNumTemporalEdges() + 0.5) / (maxEdgesTemporal + 1.0 ) );	//TODO: Is this correct?
            }
        }
        
        tom.setStructure( bestTOM );
        currentCost = costNetwork( mmlModelLearner, false );
        bestCost = currentCost;
	}
	
	
	/**
	 * Overrides AnnealSearch.doEpoch(); should be called until isFinished() is true.
	 * Only change in doEpoch() method is to run more loops (to account for the fact that
	 * we are learning N(N-1)/2 + N^2 arcs, not just N(N-1)/2 (as in a standard BN),
	 * But number of loops run (in AS) is based on number of Nodes (which is same in DBN as a BN).
	 * Simple workaround (rather that reimplementing the whole method to change one number!) is to
	 * change caseInfo.searchFactor, and then restore it to the original value.
	 */
	public double doEpoch(){
		double sfOld = caseInfo.searchFactor;
		caseInfo.searchFactor *= 3.0;		//Originally: ~0.5N^2 arcs; now: ~1.5N^2 arcs - i.e. 3x as many arcs to learn
		double result = super.doEpoch();	//Call AnnealSearch.doEpoch()
		caseInfo.searchFactor = sfOld;		//Restore
		return result;
	}
	
	/**Modify the current TEMPORAL arc probability.
	 * Method analogous to BNetSearch.setArcProb(...) but for temporal arcs.
	 */
	public void setArcProbTemporal( double arcProbTemporal ){
		if ( fixedArcProb ) { throw new RuntimeException("ArcProbTemporal value unchangable"); }
        this.arcProbTemporal = arcProbTemporal;
        //TODO: current CaseInfo doesn't store arcProbTemporal...
        //caseInfo.arcProbTemporal = arcProbTemporal;
        updateMutationOperators( this.arcProb, arcProbTemporal, temperature );
	}
	
	/**
	 * Overrides BNetSearch.updateMutationOperators(...).
	 * Calls AnnealSearch.updateMutationOperators( double arcProb, double arcProbTemporal, double temperature )
	 */
	protected void updateMutationOperators( double arcProb, double temperature ){
		updateMutationOperators( arcProb, this.arcProbTemporal, temperature );
	}
	
	/**
	 * Overloads MetropolisSearchDBN.updateMutationOperators(...) with ability to set temporal arc probability
	 */
	protected void updateMutationOperators( double arcProb, double arcProbTemporal, double temperature ){
		skeletalChange = new SkeletalChange( rand, arcProb, caseInfo, temperature );
        temporalChange = new TemporalChange( rand, arcProb, caseInfo, temperature );
        doubleSkeletalChange = new DoubleSkeletalChange( rand, arcProb, caseInfo, temperature );
        parentSwapChange = new ParentSwapChange( rand, arcProb, caseInfo, temperature );
        dbnTemporalArcChange = new DBNTemporalArcChange( rand, arcProbTemporal, caseInfo, temperature );
        dbnDoubleTemporalArcChange = new DBNDoubleTemporalArcChange( rand, arcProbTemporal, caseInfo, temperature );
        
        //As per BNetSearch.updateMutationOperators(...)
        tomCoster.setArcProb( arcProb );								//Intraslice arcs
        
        //Work-around: BNetSearch constructor calls updateMutationOperators(...), but has a TOMCoster (not a DTOMCoster)
        //Only later (i.e. AnnealSearchDBN constructor) it replaced with a UniformDTOMCoster
        if( tomCoster instanceof DTOMCoster ){
        	//Workaround required to avoid BNetSearch constructor call of updateMutationOperators failing...
        	((DTOMCoster)tomCoster).setArcProbTemporal( arcProbTemporal );	//Temporal arcs
        }
        
        caseInfo.tomCoster = tomCoster;		//Maybe this should be in constructor, but this is how BNetSearch does it...
	}
	
	/**Overrides BNetSearch.setTOMStructure(...)
	 * Not implemented.
	 */
	public static void setTOMStructure( TOM tom, Value.Vector structure ) {
		throw new RuntimeException("METHOD NOT IMPLEMENTED");
	}

}
