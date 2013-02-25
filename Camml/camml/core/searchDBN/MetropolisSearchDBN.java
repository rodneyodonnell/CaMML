package camml.core.searchDBN;

import java.util.ArrayList;
import java.util.Random;

import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner;
import camml.core.search.DoubleSkeletalChange;
import camml.core.search.MMLEC;
import camml.core.search.MetropolisSearch;
import camml.core.search.ParentSwapChange;
import camml.core.search.SEC;
import camml.core.search.SkeletalChange;
import camml.core.search.TOM;
import camml.core.search.TOMTransformation;
import camml.core.search.TemporalChange;
//import camml.core.search.MetropolisSearch.SECHashKey;
import cdms.core.Value;
import cdms.core.VectorFN;
import cdms.core.Value.Model;
import cdms.core.Value.Vector;
import cdms.plugin.search.Search;

/** Version of MetropolisSearch for learning DBNs from time series.
 * @author Alex Black
 */
public class MetropolisSearchDBN extends MetropolisSearch {

	/** Mutation operators for making temporal arc changes to DTOMs */
	protected DBNTemporalArcChange dbnTemporalArcChange;
	protected DBNDoubleTemporalArcChange dbnDoubleTemporalArcChange;
	
	
	/** Prior on TEMPORAL arcs being present in DBN. */
	protected double arcProbTemporal = 0.5;
	
	public MetropolisSearchDBN(Random rand, Vector data,
			ModelLearner mlModelLearner, ModelLearner mmlModelLearner) {
		super(rand, data, mlModelLearner, mmlModelLearner);
		
		//Set TOMCoster (Uniform DTOMCoster)
		this.tomCoster = new DTOMCoster.UniformDTOMCoster(0.5,0.5);
		this.caseInfo.tomCoster = this.tomCoster;
		
		//Create new DBN-specific Hash functions in place of ones set in MetropolisSearch constructor
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
		
		
		//TODO: Should a DBN-equivalent to the following MetropolisSearch line be implemented?
		//caseInfo.arcWeights = new double[numNodes][numNodes];
		
		setArcProbTemporal(0.5);
		
		//Set new TOMCleaner for DTOMs (instead of one used in CaseInfo, by default)
		caseInfo.tomCleaner = StandardDTOMCleaner.dtomCleaner;
	}
	
	/**Run AnnealSearch to find a good starting model and estimate arcProb and arcProbTemporal.
	 * Sets the best model in MetropolisSearchDBN to the best one found during AnnealSearch
	 * Overrides MetropolisSearch.runAnnealSearch()
	 * Code here adapted from MetropolisSearch.runAnnealSearch()
	 */
	private void runAnnealSearch(){
		//Create AnnealSearchDBN object:
		AnnealSearchDBN AS = new AnnealSearchDBN( rand, caseInfo );
		Search blockingSearch = new camml.core.library.BlockingSearch( AS );
		
		// fix arcProb if required.
        if ( fixedArcProb == true ) {
        	throw new RuntimeException("Running AnnealSearchDBN with fixed arc probability not implemented");
            //AS.setArcProb(arcProb);
            //Set annealSearch to have fixed arc probability... need to wrap the arcProb double value
            //AS.setOption("arcProb", ????? );
            //AS.recalculateCosts(); // update bestCost based on new arcProb.
        }
        //AS.tomCoster = tomCoster;
        AS.setTOMCoster(tomCoster);
        
        // run the search.  A blocking search is used as we need the result before
        //  metropolis may proceed.
        blockingSearch.start();
        
        // Copy required values from AnnealSearch.
        if (fixedArcProb != true) {
        	setArcProb( AS.getBestArcProb() );					//Estimated INTRASLICE arc density
        	setArcProbTemporal( AS.getBestArcProbTemporal() );	//Estimated TEMPORAL arc density
    	}	
        
        bestTOM.setStructure( AS.getBestTOM() );
        bestCost = AS.getBestCost();
        caseInfo.referenceWeight = bestCost;
	}

	/**Stochastically attempt a transformation to the current DTOM.
	 * Returns true if DTOM changed.
	 * Overrides MetropolisSearch.doTransform().
	 * Only minor changes vs. MetropolisSearch version that it overrides: <br>
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
	public boolean doTransform(){
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
        
        // Recalculate the cost of the network if modified.
        if( accepted == true ){
        	// updateCosts updates dirtytom and cleantom.
            updateCosts( transform.getNodesChanged() );
            
            // Reference Weight should be set to the best model cost found so far
            // this avoids overflow / underflow issues.
        	if ( currentCost + 0.001 < bestCost ) {
                updateReferenceWeight( currentCost );
                bestCost = currentCost;
                this.bestTOM.setStructure( tom );
            }
        	
        	// if currentCost is really bad, only accept positive mutations.
            // this stops us getting too far away from the good models.
            caseInfo.safeMode = ( currentCost > bestCost + caseInfo.safeCap );
        }
        return accepted;
	}
	
	/**In a single epoch one mutation of the current DTOM is attempted.
	 * Overrides MetropolisSearch.doEpoch()
	 * Vast majority of code taken from MetropolisSearch.doEpoch(), but minor changes made
	 */
	public double doEpoch(){
		if (epoch == 0 && doAnnealOnFirstEpoch) {
            // Calculate the number of epochs in the search.
            long temp = numNodes;
            if (temp < 10) {
                temp = 10;
            }            
            max = (long)(temp * temp * temp * 200 * 3.0 * caseInfo.searchFactor);	//3x as many arcs -> attempt 3x as many changes...
            
            System.out.println( "Sampling " + max + " TOMs" );
            System.out.println( fullData.length() + " data points from " + numNodes + " variables." );

            
            // In our first epoch we must perform an anneal search to initialise parameters.
            // run AnnealSearch to estimate probs and find the best MML model to start sampling from
            System.out.println("Estimating arcProb");
            runAnnealSearch();
            System.out.println("Intraslice arcs: arcProb =         " + arcProb);
            System.out.println("Temporal arcs:   arcProbTemporal = " + arcProbTemporal );
            
            // Remove any excess arcs left by AnnealSearch (unlikely to be present.)
            if ( caseInfo.regression ) { bestTOM.clean(); }
            
            // Turn on arc weight count.
            tom.clearArcs();            
            caseInfo.updateArcWeights = true;
            
            // Set currentCost and mlCost based on starting model.
            tom.setStructure( bestTOM );
            updateCosts( null );
            
            // print out progress bar header.
            for (int i = 0; i < 100; i++) {    System.out.print(i % 10); }
            System.out.println();
        }
		
		// gradually print progress bar as search runs.
        if (max < 100 || epoch % (max / 100) == 0) { System.out.print("."); }
        if ( epoch == max-1) { System.out.println(); }
        
        // Randomise TOM order in the current DAG.  Arc directions remain unchanged.        
        if ( epoch == 0 && doAnnealOnFirstEpoch && caseInfo.regression) {
            tom.buildOrder( rand );
            // Ensure any TOM constraints are honoured.
            caseInfo.tomCoster.repairTOM(tom);
        }
        
        if ( epoch == 0 ) {     // Never transform on the first epoch.
            updateCosts(null); // This ensures starting model is sampled at least once.
        }
        else {
            doTransform();
        }
        
        
        // Extract SEC from Hash and update its posterior
        SEC sec = getSEC();        
        updatePosterior(sec);    
        
        // increment the number of epochs completed.
        epoch++;
        
        // If all epoch complete, finish.
        if ( epoch == max+1 ) {
            searchDone = true;
        }
        
        // debug logging.
        if ( caseInfo.logging ) {
            try {
                caseInfo.cammlLog.write( "STEP : " + epoch + 
                                         "\tMMLCost = " + caseInfo.costFormat.format(currentCost) + 
                                         "\trand: " + ((WallaceRandom)rand).numCalls + "\n");
                caseInfo.cammlLog.flush();
            } catch (Exception e) { 
                for ( int ii = 0; ii < 100; ii++ ) { System.out.println(e); }
                throw new RuntimeException(e);
            }
        }    
        
        // currentCost is required by search interface to draw "pretty graphs", etc.  with.
        return currentCost;
	}
	
	/** Extract SEC from hashtable for the cleantom. Create the SEC if required 
	 *  OVERRIDES MetropolisSearch.getSEC() - creates DBNSEC instead of SECs... */
    public SEC getSEC() {
        // Find the SEC hash of (a cleaned version of) the current tom.
        // If we are not joining DAGs to form SECs then treat each
        //  DAG as a unique SEC.
        long hashValue;
        if ( caseInfo.joinDAGs ) {
            // cleantom supplied, so no need to reclean TOMs.
            hashValue = caseInfo.secHash.hash( cleantom, currentMLCost );
        } else {
            hashValue = caseInfo.tomHash.hash( cleantom, currentMLCost );
        }
        
        // Check if current SEC is in secHashtable
        tempKey.set( hashValue );
        SEC sec = (SEC)secHashtable.get( tempKey );
        
        
        // If SEC not found and current TOM has a reasonable posterior, add SEC to the list of SECs.
        if ( sec == null && currentCost < bestCost + ignoreCap ) {
            //sec = new SEC( cleantom, currentMLCost, caseInfo);
        	sec = new DBNSEC( cleantom, currentMLCost, caseInfo);
            secHashtable.put( new SECHashKey(hashValue), sec );        
        }
        return sec;
    }
	
	
	/**Overrides MetropolisSearch.getResults()
	 * See also: getResultsMMLEC() -> MMLEC array may be easier to deal with than Value.Vector.
	 */
	public Value.Vector getResults(){
		MMLEC[] mmlecArray = getResultsMMLEC();
		
		//Convert results into a CDMS vector. 
        Value.Structured[] structArray = new Value.Structured[ mmlecArray.length ];
        for ( int i = 0; i < structArray.length; i++ ) {
            structArray[i] = mmlecArray[i].makeSECListStruct();
        }
        results = new VectorFN.FatVector( structArray );

        // Print caseInfo.arcPortions (if caseInfo flag set).
        if (caseInfo.printArcWeights) {    printArcPortions(); }
        return results;
	}
	
	/** Similar to MetropolisSearch.getResults() but returns an array of MMLECs
	 *  instead of an unwieldy Value.Vector object.
	 * @return
	 */
	public MMLEC[] getResultsMMLEC(){
        // Shortcut if getResults is called twice.
        //if ( results != null ) { return results; }
        
        // dump all SECs from secHashTable into an ArrayList for easy manipulation.
        ArrayList<SEC> secList = new ArrayList<SEC>( secHashtable.values() );
        int uniqueSECs = secList.size();
        
        // Hopefully this should never happen.
        if ( uniqueSECs == 0 ) { throw new RuntimeException("No SECs kept during sampling."); }
        
        // Print (usefull?) statistics
        double totalWeight = caseInfo.totalWeight;        
        System.out.println("total weight kept : " + (totalWeight-weightIgnored) );
        System.out.println("total weight ignored : " + weightIgnored);
        System.out.println("" + uniqueSECs + " unique SECs sampled" );
        
        // calculate SEC posteriors from weights.
        for (int i = 0; i < uniqueSECs; i++) {
            ((DBNSEC)secList.get(i)).setPosterior( ((SEC) secList.get(i)).getWeight() / totalWeight );
        }
        
        // Sort all SECs by their posterior probability.  Highest posterior first
        java.util.Collections.sort(secList, SEC.secWeightComparator );
        
        // Each element of SECList contains a list of TOMs, these should also be sorted by
        // posterior.  (or weight which is proportional to posterior.)
        for ( int i = 0; i < secList.size(); i++ ) { ((SEC)secList.get(i)).sortTOMs(); }
        
        // Create a trimmed version of secList containing at most 30 SECs.
        ArrayList<SEC> trimmedSECList = new ArrayList<SEC>();
        double posteriorUsed = 0;
        for ( SEC sec: secList ) {                 
            if ( (posteriorUsed > caseInfo.minTotalPosterior) || 
                 (trimmedSECList.size() >= caseInfo.maxNumSECs) )  {
                break;
            }
            trimmedSECList.add( sec );
            //posteriorUsed += sec.posterior;
            posteriorUsed += sec.getPosterior();
        }
        System.out.println("Calculting KL distance from " + 
                           trimmedSECList.size() + " highest posterior SECs " +
                           "("+secList.size() + " kept)\n" + 
                           "Total posterior used = " + caseInfo.posteriorFormat.format(posteriorUsed) + 
                           //" highest Posterior SEC = " + caseInfo.posteriorFormat.format(secList.get(0).posterior) );           
                           " highest Posterior SEC = " + caseInfo.posteriorFormat.format(secList.get(0).getPosterior()) );
        secList = trimmedSECList;

        
        
        System.out.println("Calculating relative priors.");
        // calculate the relative prior for each SEC (needed for joinByKLDistance.)
        SEC bestSEC = secList.get(0);
        for ( int i = 0; i < secList.size(); i++ ) {
            SEC sec = secList.get(i);
            // oldcamml uses the wrong value to calculate relativePrior.
            // It uses mml instead of -logP(Data|model,params)
            if ( caseInfo.regression) {
                // retivePrior = ((e^mml[i])*posterior[i]) / ( (e^bestMML)*bestPosterior);
            	sec.relativePrior = Math.exp( sec.getBestMMLOfTOM(0) - bestSEC.getBestMMLOfTOM(0) ) * sec.getPosterior() / bestSEC.getPosterior();
            }
            else {
            	sec.relativePrior = Math.exp( sec.getDataCost(0) - bestSEC.getDataCost(0) ) * sec.getPosterior() / bestSEC.getPosterior();
            }
        }
        
        
        // If (joinSECs==true) attempt to join all SECs into MMLECs, if not simply copy each
        // SEC into an individual MMLEC.
        MMLEC[] mmlecArray;
        if ( caseInfo.joinSECs == true ) {
            SEC[] secArray = (SEC[])secList.toArray( new SEC[secList.size()] );
            mmlecArray = joinByKLDistance( secArray, fullData.length() );
        }
        else {    
            mmlecArray = new MMLEC[ secList.size() ];
            for ( int i = 0; i < mmlecArray.length; i++ ) { 
                mmlecArray[i] = new MMLEC( (SEC)secList.get(i) ); 
            }            
        }        
        
        // sort mmlecArray by posterior
        java.util.Arrays.sort( mmlecArray, MMLEC.posteriorComparator );
        
        return mmlecArray;
	}
	
	
	/**
	 * Overrides MetropolisSearch.makeKL(...)
	 * Can't use MetropolisSearch.makeKL(...) due to the way that DTOM.makeParameters() works
	 * (i.e. DBN parameters have an extra component for temporal parents)
	 * Vast majority of this function is the same as MetropolisSearch.makeKL(...)
	 */
	protected double[][] makeKL( SEC[] secArray, int n ){
		// how many SECs it is worth looking at?  It is slow to look at them all.
        int secUsed = secArray.length;
        
        // Shortcut if a single model is returned.
        if (secUsed == 1) { return new double[1][1]; }
        
        // create array of  parameters        
        Value.Vector[] params = new Value.Vector[ secUsed ];        
        for ( int i = 0; i < params.length; i++ ) {            
            try {
                params[i] = secArray[i].getTOM(0).makeParameters( mmlModelLearner );
            }        
            catch ( ModelLearner.LearnerException e ) {
                throw new RuntimeException("Exception recreating parameters",e);
            }
        }
        
        // print progress bar.
        for ( int i = 0; i < secUsed; i++ ) { System.out.print((i%10)); } System.out.println();
        
        // Allocate klArray
        double[][] klArray = new double[secUsed][secUsed];
        
        if ( caseInfo.useExactKL ) {
            throw new RuntimeException("Exact KL Joining not implemented for learning DBNs.");
        }
        else { // else use stochastic KL
            for ( int i = 0; i < params.length; i++ ) {
                // print progress.
                System.out.print("X");
            
                // Test what metric is being joined by
                Value.Vector augParams[];
                // if KL, do nothing.
                if ( caseInfo.cklJoinType == 0) {
                    augParams = params;
                }
                // if CKL3, augment all parameters.
                else if (caseInfo.cklJoinType == 3) {
                	throw new RuntimeException("Joining by CKL for DBNs not implemented.");
                }
                // Other methods not implemented.
                else { throw new RuntimeException("Unhandled CKL join type."); }
                
                
                //First: Generate a TIME SERIES of length N from the current DBNSEC
                DTOM dtom = (DTOM)secArray[i].getTOM(0);
                Value.Vector tempData = DBNStaticMethods.generateTimeSeriesDTOM(rand, dtom, n);
                
                
                //Calculate cost of stating data with the current model:
                //double selfCost = augBNet.logP(x, y, z);
                Model[] subModel = DBNStaticMethods.makeSubModelListDBN(augParams[i]);
                Value[] subParam = DBNStaticMethods.makeSubParamListDBN(augParams[i]);
                int[] order = dtom.getTotalOrderCopy();
                int[][] parents = DBNStaticMethods.makeParentsArray( (DTOM)secArray[i].getTOM(0) );
                int[][] parentsTemporal = DBNStaticMethods.makeParentsTemporalArray( (DTOM)secArray[i].getTOM(0) );
                	//selfCost is cost of stating  the generated time series using the model it was generated from
                double selfCost = -DBNStaticMethods.DBNlogP( tempData, subModel, subParam, order, parents, parentsTemporal );
                
                
                //Loop through all models calculating cost to state the generated time series data with each
                for ( int j = 0; j < augParams.length; j++ ) {
                    if( i == j ){	//KL divergence between a model and itself is necessarily zero.
                    	klArray[i][j] = 0.0;
                    	continue;
                    }
                	
                    //Get all the parameters, parents etc. for the other model:
                    Model[] subModelJ = DBNStaticMethods.makeSubModelListDBN(augParams[j]);
                    Value[] subParamJ = DBNStaticMethods.makeSubParamListDBN(augParams[j]);
                    int[] orderJ = ((DTOM)secArray[j].getTOM(0)).getTotalOrderCopy();
                    int[][] parentsJ = DBNStaticMethods.makeParentsArray( (DTOM)secArray[j].getTOM(0) );
                    int[][] parentsTemporalJ = DBNStaticMethods.makeParentsTemporalArray( (DTOM)secArray[j].getTOM(0) );
                    	//Cost the generated time series data (from model i) using the jth model 
                    double cost = -DBNStaticMethods.DBNlogP(tempData, subModelJ, subParamJ, orderJ, parentsJ, parentsTemporalJ );
                    
                    //klArray[i][j] = caseInfo.data.length() * (cost - selfCost) / n;	//Original...
                    klArray[i][j] = (caseInfo.data.length()-1) * (cost - selfCost) / (n-1);
                    //Using length-1 for both original data and generated time series, because we are encoding
                    // all data using second time slice CPDs only (except very first instance of data, which is
                    // ignored - hence length-1 )
                }
            }
        }
               
        return klArray;
	}
	
	/**
	 * Overrides MetropolisSearch.getMixResults(...)
	 */
	public static Value.Structured getMixResults( Value.Vector resultVec, Value.Vector data, boolean useNetica ){
		throw new RuntimeException("METHOD NOT IMPLEMENTED");
	}
	
	/**
	 * Overrides BNetSearch.setOption(...)
	 * So far: No changes.
	 */
	public boolean setOption( final String option, Value v ) {
		//TODO: Add new options; call super.setOption(...) for all other options
		return super.setOption(option, v);
	}
	
	/**Not implemented.
	 * Overrides BNetSearch.setTOMStructure(...)
	 */
	public static void setTOMStructure( TOM tom, Value.Vector structure ) {
		throw new RuntimeException("METHOD NOT IMPLEMENTED");
	}
	
	/**Modify the current arc probability (for temporal arcs only).
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
	 * Overrides BNetSearch.updateMutationOperators(...)
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
        //Only later (i.e. MetropolisSearchDBN constructor) it replaced with a UniformDTOMCoster
        if( tomCoster instanceof DTOMCoster ){	//Ugly workaround
        	//Required to avoid failure in BNetSearch constructor...
        	((DTOMCoster)tomCoster).setArcProbTemporal( arcProbTemporal );	//Temporal arcs
        }
        
        caseInfo.tomCoster = tomCoster;		//Maybe this should be in constructor, but this is how BNetSearch does it...
	}
	
	/**
	 * Overrides BNetSearch.printDetailedCost(...)
	 */
	public void printDetailedCost( TOM tom ){
		throw new RuntimeException("METHOD NOT IMPLEMENTED");
	}
	
}
