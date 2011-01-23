/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// Anneal Search for CaMML
//

// File: AnnealSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import cdms.core.*;
import camml.core.models.ModelLearner;
import camml.core.library.WallaceRandom;

/**
 * Anneal search is based on the Anneal search in the original version of CaMML by
 * Chris Wallace. <br>
 * <br>
 * The search moves in several phases, at the end of each phase the best model (by MML score) is
 * kept and arcProb is recalculated based on the number of arcs present.  The search initially
 * tries beginning from highly connected models, then from unconnected models searching for a good
 * model.  The best model from these steps is used as an initial model for the simulated annealing
 * search. 
 */
public class AnnealSearch extends BNetSearch
{
    /** Calculate the best arcProb to use based on the current best TOM */
    public double getBestArcProb() { 
        int maxArcs = numNodes * (numNodes - 1) / 2;
        return ((double)bestTOM.getNumEdges()+0.5) / (double)(maxArcs+1.0); 
    }
    public int getBestNumArcs() { return bestTOM.getNumEdges(); }
    
    
    /** 
     * Reset the search. <br>
     * tom is set to an empty model (with a random ordering.)
     */
    public void reset() 
    {
        // randomise the initial order of nodes so it is not bias towards an a->b->c type ordering.
        tom.clearArcs();
        tom.randomOrder(rand);
        
        // Ensure any TOM constraints are honoured.
        caseInfo.tomCoster.repairTOM(tom);

        
        epoch = 0;
        searchDone = false;
        
        setTemperature( 1.0 );
        if (!fixedArcProb) { setArcProb( 0.5 ); }
        currentCost = costNetwork( mmlModelLearner, false );
        System.out.println( "Null cost = " + currentCost );
        
        bestTOM.clearArcs();
        bestCost = currentCost;
    }
    
    /**
     * perform a single mutation step.  Return true if model is changed.
     * bestTOM, bestCost and currentCost are updated.
     */
    public boolean step( long stepNum )
    {
        boolean accepted;
        
        // Mutate the network and see check if the resulting network is accepted or rejected.
        double rnd = rand.nextDouble();
        TOMTransformation transform;
        if (rnd < 0.16667) {         // 1/6 chance
            transform = parentSwapChange;
        } else if (rnd < 0.33333) {  // 1/6 chance
            transform = doubleSkeletalChange;
        } else if (rnd < 0.66667 ) { // 1/3 chance
            transform = skeletalChange;
        } else {                     // 1/3 chance
            transform = temporalChange;
        }
        accepted = transform.transform( tom, currentCost );
        
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
        
        // Save best TOM
        if ( currentCost + 0.001 < bestCost ) {
            bestTOM.setStructure(tom);
            bestCost = currentCost;
        }
        
        return accepted;
    }
    
    /** run n steps and return the best cost */
    public void doSteps( long numSteps )
    {
        // run for n steps
        currentCost = costNetwork( mmlModelLearner, false );
        for ( long i = 0; i < numSteps; i++ ) {
            step(i);
        }
        
        // set arcProb based on bestTOM
        int maxEdges = numNodes * (numNodes - 1) / 2;
        if ( !fixedArcProb ) { 
            setArcProb( (bestTOM.getNumEdges() + 0.5)/ (maxEdges + 1.0) ); 
        }
        
        // Clean and recalculate arcProb several times to ensure we have the best
        // clean model possible.
        for ( int i = 0; i < 10; i++ ) {
            // reclean the best tom to see if changing arcProb has any effect
            bestTOM.clean();
            
            // fix arcProb based on recleaned best TOM.
            if ( !fixedArcProb ) { 
                setArcProb( (bestTOM.getNumEdges() + 0.5)/ (maxEdges + 1.0) );
            }
        }
        
        tom.setStructure( bestTOM );
        currentCost = costNetwork( mmlModelLearner, false );
        bestCost = currentCost;
    }
    
    
    
    /** Number of fill epochs to perform */
    final int fillEpochs = 10;
    /** Number of clear epochs to perform */
    final int clearEpochs = 10;
    /** Number of cool epochs to perform */
    final int annealEpochs = 12;
    /** Total number of epochs to perform (= initial + fill + clear + cool) */
    final int totalEpochs = fillEpochs + clearEpochs + annealEpochs + 1;    
    /** Maximum temperature of SA search */
    final double maxTemperature = 2.0;
    
    
    /**
     * Each time doEpoch is called it checks what it should be doing based on the current epoch.
     *  doEpoch should be called until isFinished() returns true.  The search is done this way to
     *  fit into the CDMS idea of a search. <br>
     * 
     * During each epoch a starting model is chosen, and from here a number of mutations 
     *  proportional to (numVars^3) is performed.  The best MML model is stored. After each epoch
     *  arcProb is recalculated based on the best model found so far. <br>
     *
     * The first epoch begins with an empty model.  The next 7 begin with a mostly connected model,
     *  then 7 more from an empty model.  From this point we move into simulated annealing mode
     *  reducing the search temperature from 2.0 to 1.0 in 12 steps.
     *
     * At the end of each epoch arcProb is recalculated based on the best model found.
     *
     * Once isFinished() returns true the best model will be obtained by calling getBestTOM().
     *
     * The best cost found so far is returned after each call to doEpoch.
     */
    public double doEpoch() 
    {        
        // We search the TOM space sampling n^3 * X TOMs
        long nCubed = (long)numNodes * numNodes * numNodes;
        if ( nCubed < 1000 ) { nCubed = 1000; }
        nCubed *= caseInfo.searchFactor;
        if (nCubed < 1) { nCubed = 1; }
        
        // Set up during initial epoch.
        if ( epoch == 0 ) {
            for ( int i = 0; i < totalEpochs; i++ ) {
                System.out.print( i % 10 );
            }
            System.out.println();        
            
            System.out.print('I');
            doSteps( 7 * nCubed );
        }
        // Try starting from a fully connected model
        // If searchfactor too low, skip fill step.
        else if ( epoch <= fillEpochs && caseInfo.searchFactor != 0) {
            tom.clearArcs();
            tom.randomOrder( rand );        
            tom.fillArcs( tom.maxNumParents ); // fill arcs (max of 7 parents per node) 
            caseInfo.tomCoster.repairTOM(tom);
            
            // This line does nothing (apart from advancing random number seed) 
            // and is entirely for regression.
            if ( caseInfo.regression ) { tom.buildOrder( rand ); }
            
            System.out.print('F');    
            doSteps( 7 * nCubed );        
        }
        // Try starting from an empty model.
        else if ( epoch <= fillEpochs + clearEpochs ) {
            tom.clearArcs();
            tom.randomOrder( rand ); 
            caseInfo.tomCoster.repairTOM(tom);
            
            System.out.print('C');
            doSteps( 7 * nCubed );
        }
        // Try simulated annealing.
        else if ( epoch <= fillEpochs + clearEpochs + annealEpochs ) {
            long coolEpochNum = epoch - fillEpochs - clearEpochs-1;
            // temperature starts at 2.0 - 1/12
            double temperature = maxTemperature - 
                ((coolEpochNum+1) * (maxTemperature - 1.0) / (annealEpochs));
            setTemperature( temperature );
            
            tom.buildOrder( rand );
            caseInfo.tomCoster.repairTOM(tom);
            
            System.out.print('A');
            doSteps( 10 * nCubed );
        }
        else {
            throw new RuntimeException("Too many epochs?");
        }
        
        epoch ++;
        
        // If all epochs are finished, flag the search as being completed.
        if ( epoch == totalEpochs ) { 
            System.out.println();
            searchDone = true;   
        }
        
        return bestCost;
    }
    
    /** return percentage of search completed. */
    public double getPercentage() 
    {
        return 100.0 * (double)epoch / totalEpochs;
    }
    
    /** print out the total ordering of the given TOM */
    public static String getTotalOrderingString( TOM tom ) {
        String s = "";
        for ( int i = 0; i < tom.getNumNodes(); i++ ) {
            s += tom.nodeAt(i) + " ";
        }
        return s;
    }
    
    /** AnnealSearch constructor, creates caseInfo from data supplies */
    public AnnealSearch(java.util.Random rand, Value.Vector data,  
                        ModelLearner mlModelLearner, ModelLearner mmlModelLearner ) 
    {
        this( rand, 
              new CaseInfo(null, null, data, mmlModelLearner, mlModelLearner, -1, 
                           new NodeCache( data, mmlModelLearner, mlModelLearner )) );
        caseInfo.nodeCache.caseInfo = caseInfo;


        // Create temporary RNG so regression is not thrown out.
        java.util.Random rand2 = new java.util.Random(123);
        
        // create hashing functions.
        caseInfo.tomHash = new TOMHash( rand2, numNodes );
        caseInfo.secHash = new SECHash( rand2, numNodes );

        // we have to manually set caseInfo 
        caseInfo.secHash.caseInfo = caseInfo;
        caseInfo.tomHash.caseInfo = caseInfo;
        caseInfo.nodeCache.caseInfo = caseInfo;    
    }
    
    /** CaseInfo constructor. */
    public AnnealSearch(java.util.Random rand, CaseInfo caseInfo ) 
    {
        super( rand, caseInfo );
        // For perfect regression random generators must be alligned.
        if ( caseInfo.regression ) {
            ((WallaceRandom)rand).setSeed( new int[] {377777, -888} );
            ((WallaceRandom)rand).numCalls = 0;
            int numDummyRuns = 1000 + numNodes * numNodes * 2;
            for( int i = 0; i < numDummyRuns; i++ ) { rand.nextInt(); }
            // ((WallaceRandom)rand).setVerbose( caseInfo.verbose );
        }
    }
    
}
