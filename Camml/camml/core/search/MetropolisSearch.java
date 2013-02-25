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
// Metropolis Search for CaMML
//

// File: MetropolisSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import cdms.core.*;
import cdms.plugin.search.*;

import camml.core.models.bNet.BNet;
import camml.core.models.mixture.Mixture;

import java.util.Hashtable;
import java.util.ArrayList;

import norsys.netica.NeticaException;

import camml.core.models.ModelLearner;
import camml.core.library.WallaceRandom;
import camml.plugin.augment.AugmentFN3;
import camml.plugin.netica.BNetNetica;

/**
 * Metropolis search is based on the Metropolis search in the original version of CaMML by
 * Chris Wallace. <br>
 * <br>
 * Metroposis search moves through TOM space using a TOMTransformation operations.
 * The new TOM is either accepted or rejected based on its cost compared with the previous TOM.
 * <br> <br>
 * 
 * A heirachy of models is kept.  Each TOM is transformed into a DAG, which may represent several 
 * TOMs.  These DAGs are then "cleaned" into clean DAGs, which in turn may represent several unclean
 * DAGs.  These clean DAGs are then grouped to form clean SECs, which are in turn grouped to form
 * MMLECs, or MML Equivelence Classes.  These MMLECs attempt to use strict MML to join SECs.
 * <br>
 *
 *  To keep track of the various models a Hashtable secHashtable is used.  Each entry in 
 *  secHashTable is a SEC, which contains numerous TOMs.
 */
public class MetropolisSearch extends BNetSearch
{
    /** Hash Table containing the number of times each SEC is visited */
    protected Hashtable<SECHashKey,SEC> secHashtable;
    public Hashtable<SECHashKey,SEC> getSECHashTable() { return secHashtable; }
    
    /** Maximum number of epochs */
    // Long required as int boundary may be exceeded at ~= 200 nodes. 
    protected long max;
    
    /** SECs costing more than bestCost + ignoreCap are not added to the secList. */
    protected final double ignoreCap = 30;
    
    /** Total weight ignored by sampling.  A TOM is ignored if it's MML cost is > best+ignoreCap */
    protected double weightIgnored = 0;    
    
    /** Current clean Maximum Likelihood cost */    
    protected double currentMLCost;
    
    /** Should we treat the first epoch as special and do the anneal search there? */
    public boolean doAnnealOnFirstEpoch = true;

    /** Update currentCost and cleanMLCost. 
     * @param nodeChanged: list of nodes with parent changes since last call to updateCosts. 
     * If (nodeChanges == null) all nodes are considered changed.
     */
    public void updateCosts( int[] nodesChanged )
    {
        currentCost = costNodes( mmlModelLearner, false, nodesChanged ) + structureCost( false );
        currentMLCost = costNodes( mlModelLearner, true, nodesChanged );
    }
    
    /** 
     * Reset the search. <br>
     * tom is set to the same structure as bestTOM. <br>
     * secHashtable is reset
     */
    public void reset() 
    {
        // Start sampling from the best model found.
        tom.setStructure( bestTOM );
                
        secHashtable.clear();

        // Initial search conditions
        epoch = 0;
        searchDone = false;
        
        // Initialise mml, ml and best costs
        updateCosts( null );
        bestCost = currentCost;
    }
    
    /** Class used as key to secHashTable.  This is essentially a long (64 bit) value 
     *      turned into an object so it can be used as a key to a hashtable.
     */
    protected static class SECHashKey implements Cloneable { 
        private long key;
        public SECHashKey( long key ) { this.key = key; }
        public int hashCode() { return (int)((key >> 32) + key); }
        public boolean equals( Object o ) { 
            if (((SECHashKey)o).key == this.key ) return true; else return false; 
        }
        public Object clone() { return new SECHashKey(key);    }
        public void set( long key ) { this.key = key; }
    }
    
    /** We allocate a single key to save reallocation for each iteration. */
    protected SECHashKey tempKey = new SECHashKey(0);
    
    /** Run AnnealSearch to estimate arcProb and set bestModel */
    private void runAnnealSearch( ) {
        // create AnnealSearch object
        AnnealSearch annealSearch = new AnnealSearch( rand, caseInfo );
        Search blockingSearch = new camml.core.library.BlockingSearch( annealSearch );
        
        // fix arcProb if required.
        if ( fixedArcProb == true ) { 
            annealSearch.setArcProb(arcProb); 
            annealSearch.fixedArcProb = true; 
            annealSearch.recalculateCosts(); // update bestCost based on new arcProb.
        }
        annealSearch.tomCoster = tomCoster;
        
        // run the search.  A blocking search is used as we need the result before
        //  metropolis may proceed.
        blockingSearch.start();
        
        // Copy required values from AnnealSearch.
        if (fixedArcProb != true) { setArcProb( annealSearch.getBestArcProb() ); }
        bestTOM.setStructure( annealSearch.getBestTOM() );
        bestCost = annealSearch.bestCost;
        caseInfo.referenceWeight = bestCost;    
    }
    
    /** Update weights so that currentCost has a weight of 1.0
     * This should be done when a new best cost is found.
     */
    public void updateReferenceWeight( double currentCost ) 
    {
        double oldReferenceWeight = caseInfo.referenceWeight;
        //double oldTotalWeight = caseInfo.totalWeight;
        caseInfo.referenceWeight = currentCost;
        
        //     Fixed version of multiplier code.
        double multiplier =
            Math.exp( (currentCost - oldReferenceWeight) * (1.0 - (1.0/temperature)));
        
        caseInfo.totalWeight *= multiplier;
        weightIgnored *= multiplier;
        
        // dump secHashtable into an arraylist for easy management.
        ArrayList<SEC> secList = new ArrayList<SEC>( secHashtable.values() );
        
        // all weights must be multiplied by the multiplier.  referenceWeight always << oldWeight
        // so multiplies always fractional.  All Weights reduced.
        for ( int i = 0; i < secList.size(); i++ ) {
            secList.get(i).updateReferenceWeight( multiplier );
        }
        
        // Update arc portions
        if (caseInfo.updateArcWeights) {        
            double[][] arcWeights = caseInfo.arcWeights;
            for (int i = 0; i < arcWeights.length; i++) {
                for (int j = 0; j < arcWeights.length; j++) {
                    arcWeights[i][j] *= multiplier;
                }                
            }
        }
        
        System.out.println("newReference = " + caseInfo.referenceWeight + "\t" + 
                           "totalWeight = " + caseInfo.totalWeight + "\t" + 
                           "multiplier = " + multiplier );
    }
    

    /** Stochastically attempt transform.  Return true if TOM changed.*/
    public boolean doTransform()
    {
        // If we always accept the first model we avoid some strange situations where
        // the optimum MML model is not samples.
        TOMTransformation transform;

        // Randomly choose class of transformation to attempt.
        double rnd = rand.nextDouble();
        if (rnd < 0.16667) {         // 1/6 chance
            transform = parentSwapChange;
        } else if (rnd < 0.33333) {  // 1/6 chance
            transform = doubleSkeletalChange;
        } else if (rnd < 0.66667 ) { // 1/3 chance
            transform = skeletalChange;
        } else {                     // 1/3 chance
            transform = temporalChange;
        }
            
        // was it successful?
        boolean accepted = transform.transform( tom, currentCost );
                
        // Recalculate the cost of the network if modified.    
        if ( accepted == true ) {
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
    
    /**
     * In a single epoch, one mutation of the network is attempted.  If this mutation is accepted
     * then the current TOM is modified.  If it is rejected the current TOM remains unchanged.
     * Either way, the clean SEC and clean DAG posterior of the chosen model is incremented using
     * the unclean TOM MML score.
     */
    public double doEpoch() 
    {
        if (epoch == 0 && doAnnealOnFirstEpoch) {
            // Calculate the number of epochs in the search.
            long temp = numNodes;
            if (temp < 10) {
                temp = 10;
            }
            max = (long)(temp * temp * temp * 200 * caseInfo.searchFactor);            
            
            System.out.println( "Sampling " + max + " TOMs" );
            System.out.println( fullData.length() + " data points from " + numNodes + " variables." );

            
            // In our first epoch we must perform an anneal search to initialise parameters.
            // run AnnealSearch to estimate probs and find the best MML model to start sampling from
            System.out.println("Estimating arcProb");        
            runAnnealSearch();
            System.out.println("arcProb = " + arcProb);
            
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

    /**
     * Update the posterior of SEC based on currentCost and temperature
     * If SEC == NULL, add posterior to "ignored" instead
     */
    public void updatePosterior(SEC sec) {
        // Add TOM posterior to SEC.  This also adds the current DAG to the SECs list of DAGs if
        // it is not already present.
        if ( sec != null ) {
            sec.addTOM( cleantom, currentCost, temperature );
        }
        // If TOM and SEC are not good enough, add posterior to weightIgnored instead.
        else {
            double diff = caseInfo.referenceWeight - currentCost;
            double weight = Math.exp( diff * (1.0 - 1.0/temperature));
            
            weightIgnored += weight;
            caseInfo.totalWeight += weight;
            
            // debug logging.
            if ( caseInfo.logging ) {
                try {            
                    caseInfo.cammlLog.write( "weight = " + caseInfo.weightFormat.format(weight) + 
                                             "\ttomWeight = " + caseInfo.weightFormat.format(0.0) + 
                                             "\tdiff = " + caseInfo.weightFormat.format(diff) + 
                                             "\ttotalWeight = " + 
                                             caseInfo.weightFormat.format(caseInfo.totalWeight)+"\n");
                    caseInfo.cammlLog.flush();
                } catch (java.io.IOException e) { /* ignore exception */ }
            }
        }
    }

    /** Extract SEC from hashtable for the cleantom. Create the SEC if required */
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
            sec = new SEC( cleantom, currentMLCost, caseInfo);
            secHashtable.put( new SECHashKey(hashValue), sec );        
        }
        return sec;
    }
    
    /** If results have already been calculated, cache the value. */
    protected Value.Vector results = null;
    
    /** 
     * Returns a vector of results.  Each element in the vector contains a 
     * (Model,params,MesgLen,logLH,Posterior) structure.
     * Elements are sorted by posterior. 
     */
    public Value.Vector getResults()
    {
        // Shortcut if getResults is called twice.
        if ( results != null ) { return results; }
        
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
            secList.get(i).posterior = ((SEC) secList.get(i)).weight / totalWeight;
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
            posteriorUsed += sec.posterior;
        }
        System.out.println("Calculting KL distance from " + 
                           trimmedSECList.size() + " highest posterior SECs " +
                           "("+secList.size() + " kept)\n" + 
                           "Total posterior used = " + caseInfo.posteriorFormat.format(posteriorUsed) + 
                           " highest Posterior SEC = " + caseInfo.posteriorFormat.format(secList.get(0).posterior) );           
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
                sec.relativePrior = Math.exp( sec.bestMML - bestSEC.bestMML ) * sec.posterior / bestSEC.posterior;
            }
            else {
                sec.relativePrior = Math.exp( sec.getDataCost(0) - bestSEC.getDataCost(0) ) * sec.posterior / bestSEC.posterior;
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
        
        //    Convert results into a CDMS vector. 
        Value.Structured[] structArray = new Value.Structured[ mmlecArray.length ];
        for ( int i = 0; i < structArray.length; i++ ) {
            structArray[i] = mmlecArray[i].makeSECListStruct();
        }
        results = new VectorFN.FatVector( structArray );

        // Print caseInfo.arcPortions (if caseInfo flag set).
        if (caseInfo.printArcWeights) {    printArcPortions(); }
        return results;
    }
    
    //
    // Calculate the proportion of time arc[i][j] is set in the TOM being sampled
    //
    public double[][] getArcPortions() {
        double arcWeights[][] = caseInfo.arcWeights;
        double arcProbs[][] = new double[arcWeights.length][arcWeights.length];
        for (int i = 0; i < arcWeights.length; i++) {
            for (int j = 0; j < arcWeights.length; j++) {
                arcProbs[i][j] = arcWeights[i][j];
                if (tom.isDirectedArc(j,i)) { arcProbs[i][j] += caseInfo.totalWeight; }
                arcProbs[i][j] /= caseInfo.totalWeight;
            }
        }
        return arcProbs;
    }
    
    public void printArcPortions() {        
        double p[][] = getArcPortions();
        for (int i = 0; i < p.length; i++)
            {
                for (int j = 0; j < p.length; j++) {
                    System.out.print(caseInfo.posteriorFormat.format(p[i][j]) + "\t");
                }
                System.out.println();
            }        
    }

    
    /** Using relative prior, KL distance and SMML formula, which models should be joined? */
    protected MMLEC[] joinByKLDistance( SEC[] secArray, int dataLength  )
    {    
        System.out.println("Joining by KL distance");
        
        // oldCamml uses klSamples = max(2000,numSamples) but this seems to give eratic and
        // occasionally negative KL values.
        int klSamples = (int)(10000 * caseInfo.searchFactor);
        if ( dataLength > klSamples ) { klSamples = dataLength; }
        double[][] kl = makeKL( secArray, klSamples );
        
        // turn each SEC into a single element MMLEC
        MMLEC[] mmlec = new MMLEC[kl.length];
        for ( int i = 0; i < mmlec.length; i++ ) {
            mmlec[i] = new MMLEC( secArray[i] );
        }
        
        
        
        
        double bestGain = -1;
        int bestI = -1;
        int bestJ = -1;
        
        while (true) { 
            
            // loop through all possible joins to see which has the highest gain.
            for ( int i = 0; i < kl.length; i++ ) {
                for ( int j = 0; j < kl[i].length; j++ ) {
                    // ensure mmlec[i] and mmlec[j] each contain at least one SEC
                    if ( mmlec[i].length() > 0 && mmlec[j].length() > 0 ) {
                        
                        // extract representative SECs
                        SEC secI = mmlec[i].getSEC(0);
                        SEC secJ = mmlec[j].getSEC(0);
                        
                        // extract numArcs from representative SECs
                        int arcsI = secI.getNumArcs();
                        int arcsJ = secJ.getNumArcs();
                        
                        if ( i != j && 
                             (arcsI <= arcsJ || (caseInfo.allowMergeToModelWithMoreArcs == true))
                             ) {  
                            // extract relative priors from MMLECs
                            double p1 = mmlec[i].relativePrior;
                            double p2 = mmlec[j].relativePrior;
                            double p3 = p1 + p2;
                            
                            // Use SMML formula to test if SECs should be merged 
                            //             double gain = p1 * Math.log( p1 ) + p2 * Math.log( p2 ) 
                            //                 - ( p1 + p2 ) * Math.log( p1 + p2 ) 
                            //                 + p2 * kl[j][i] * dataLength ;
                            //             gain = (-gain) / (p1+p2);
                            double before = -(p1 * Math.log(p1) + p2 * Math.log( p2 ));
                            double after = -(p3 * Math.log(p3)) + p2 * kl[j][i];
                            double gain = before - after;
                            
                            if ( gain > bestGain ) {
                                bestGain = gain;
                                bestI = i;
                                bestJ = j;
                            }
                        }
                    }
                }        
            } // end : for ( int i = 0; i < kl.length; i++ ) { ... }
            
            
            // if best gain found is positive, join models.
            // if not, model joining is done.
            if ( bestGain > 0 ) {
                System.out.println("Joining model " + bestI + " and " + bestJ  + "\t" +
                                   "["+caseInfo.posteriorFormat.format(mmlec[bestI].getPosterior())+" + "+
                                   caseInfo.posteriorFormat.format(mmlec[bestJ].getPosterior())+
                                   " = "+caseInfo.posteriorFormat.format(mmlec[bestI].getPosterior()+
                                                                         mmlec[bestJ].getPosterior())+"]");
                
                mmlec[bestI].merge( mmlec[bestJ] );
                
                bestGain = -1;
                bestI = -1;
                bestJ = -1;
            } 
            else {
                break;
            }
            
        } // end while(true) { ... }
        
        
        // compact all non empty MMLECs and return resuls.
        int uniqueMMLECs = 0;
        for ( int i = 0; i < mmlec.length; i++ ) {
            if ( mmlec[i].secList.size() != 0 ) {
                uniqueMMLECs ++;
            }
        }
        
        MMLEC[] finalMMLECArray = new MMLEC[uniqueMMLECs];
        int n = 0;
        for ( int i = 0; i < mmlec.length; i++ ) {
            if ( mmlec[i].secList.size() != 0 ) {
                finalMMLECArray[n] = mmlec[i];
                n++;
            }
        }
        
        // System.out.println("Highest posterior model : " + secArray[0].posterior );
        return finalMMLECArray;
    }
    
    /**
     * Find the KL distance between each SEC and return it as an array of doubles.
     *
     * secList is an ArrayList full of SECs. <br>
     * From each SEC we take the highest posterior TOM. <br>
     * This TOM is parameterised as a BNet. <br>
     * This leaves us with a list of fuly parameterised BNets. <br>
     * We must now find the KL distance between each of these BNets.  We approximate this by 
     *  generating N data points from each BNet and finding the logLH of each generated dataset
     *  using every other BNet.  
     *  klDistance[i][j] = (net[i].logLH( net[i].generate ) - net[j].logLH( net[i].generate )) / N;
     */
    protected double[][] makeKL( SEC[] secArray, int n ) 
    {
        // how many SECs it is worth looking at?  It is slow to look at them all.
        int secUsed = secArray.length;
        
        // Shortcut if a single model is returned.
        if (secUsed == 1) { return new double[1][1]; }
        
        // create array of  parameters        
        Value.Vector[] params = new Value.Vector[ secUsed ];        
        BNet bNet = secArray[0].caseInfo.bNet;
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
        Value.Vector inputVec = new VectorFN.ConstantVector( n, Value.TRIV ); 
        
        if ( caseInfo.useExactKL ) {
            if (caseInfo.cklJoinType != 0) {
                throw new RuntimeException("exact CKL Joining not implemented.");
            }
            
            if ( caseInfo.useNetica) {
                for ( int i = 0; i < params.length; i++) {
                    try {
                        System.out.print('X');
                        klArray[i] = BNetNetica.exactKLNetica(bNet,params,i);
                    } catch ( NeticaException e) {
                        throw new RuntimeException(e);
                    }
                }                
            }
            else {
                for (int i = 0; i < klArray.length; i++) {
                    System.out.print("X");
                
                    for (int j = 0; j < klArray.length; j++) {
                        klArray[i][j] = bNet.kl(params[i],params[j]) * caseInfo.data.length();
                        //klArray[i][j] = bNet.ckl2(params[i],params[j]) * caseInfo.data.length();
                    }
                }
            }
        }
        else { // else use stochastic KL
            for ( int i = 0; i < params.length; i++ ) {
                // print progrsss.
                System.out.print("X");
            
                // Test what metric is being joined by
                Value.Vector augParams[];
                BNet augBNet = null;
                // if KL, do nothing.
                if ( caseInfo.cklJoinType == 0) {
                    augParams = params;
                    augBNet = bNet;
                }
                // if CKL3, augment all parameters.
                else if (caseInfo.cklJoinType == 3) {
                    augParams = new Value.Vector[params.length];
                    for (int j = 0; j < params.length; j++) {
                        Value.Structured s = AugmentFN3.augment3.apply(bNet,params[j],params[i]);
                        augParams[j] = (Value.Vector)s.cmpnt(1);
                        if (i == j) { augBNet = (BNet)s.cmpnt(0); }
                    }
                }
                // Other methods not implemented.
                else { throw new RuntimeException("Unhandled CKL join type."); }
            
            
            
                // generate data from current parameter set
                Value.Vector tempData = augBNet.generate( rand, n, augParams[i], Value.TRIV );
            
                // Calculate cost of stating data with current model
                double selfCost = - augBNet.logP( tempData, augParams[i], inputVec );
            
                // Loop through all models calculating cost to state data with each.
                // kl[i][j] = (logP( data|params[i] ) - logP(data|params[j])) / n
                for ( int j = 0; j < augParams.length; j++ ) {
                    double cost = - augBNet.logP( tempData, augParams[j], inputVec );
                    klArray[i][j] = caseInfo.data.length() * (cost - selfCost) / n;        
                }
            }
        }
        System.out.println();
        return klArray;
    }
    
    /** Using values from getResults() return (m,y) for a mixture model. */
    public Value.Structured getMixResults( ) {
        return getMixResults( getResults(), fullData, caseInfo.useNetica );
    }
    
    /** Static function to turn a resultVec (as returned by getResults()) into a mixture model. */
    public static Value.Structured getMixResults( Value.Vector resultVec, Value.Vector data, boolean useNetica )
    {
        // get result array
        //Value.Vector resultVec = getResults();
        
        // extract data type
        //Type.Structured dataType = ((Type.Structured)((Type.Vector)fullData.t).elt);
        Type.Structured dataType = ((Type.Structured)((Type.Vector)data.t).elt);
        
        // allocate space to store posterior params and model for each cmpnt in the mixture.
        int numMixers = resultVec.length();
        double[] posterior = new double[ numMixers ];
        Value[] params = new Value[ numMixers ];
        Value.Model[] model = new Value.Model[ numMixers ];
        
        
        // We can read Posterior directly from MMLEC, but we have to tunnel through a few
        // layers to find the DAG parameters which represent the MMLEC.
        double posteriorUsed = 0;
        for ( int i = 0; i < numMixers; i++ ) {
            // extract mmlec
            Value.Structured mmlecStruct = (Value.Structured)resultVec.elt(i);
            posterior[i] = mmlecStruct.doubleCmpnt( 1 );
            posteriorUsed += posterior[i];
            
            // Extract representative SEC from MMLEC
            Value.Structured secStruct =
                (Value.Structured)((Value.Vector)mmlecStruct.cmpnt(0)).elt(0);
            
            // Extract representative DAG from MMLEC
            Value.Structured dagStruct = 
                (Value.Structured)(((Value.Vector)secStruct.cmpnt(0)).elt(0));
            
            // extract params from dagStruct
            if (useNetica) {
                model[i] = new camml.plugin.netica.BNetNetica( dataType );
            } else {
                model[i] = new camml.core.models.bNet.BNetStochastic( dataType );
            }
            params[i] = dagStruct.cmpnt(1);
        }    
        
        // We need to correct from posterior to weights.  As we do not collect all models
        // metropolis sampling posteriors will not sum to 1.0, so we normalise them to make
        // sure they do.
        for ( int i = 0; i < posterior.length; i++ ) {
            posterior[i] *= 1.0/posteriorUsed;
        }
        
        // Roll everything into a big parameter vector of type [ (proportion,model,param) ]
        Value.Vector paramVec = new VectorFN.FatVector( params );
        Value.Vector proportions = new VectorFN.FastContinuousVector( posterior );
        Value.Vector modelVec = new VectorFN.FatVector( model );
        Value.Structured vecStruct = 
            new Value.DefStructured( new Value[] {proportions,modelVec,paramVec});
        
        Value.Vector mixParams = new VectorFN.MultiCol( vecStruct );
        
        // return (model,mixParams) tupple.
        return new Value.DefStructured( new Value[] { Mixture.mixture, mixParams} );
    }
    
    /** Using getResults(), print out results of the search to stdout. */
    public void printFullReport() 
    {
        System.out.println( getResults() );
        
        System.out.println("GetIndex Called     \t" + caseInfo.nodeCache.getIndexCalled );
        System.out.println("Cache New           \t" + caseInfo.nodeCache.newHash );
        System.out.println("Cache Success       \t" + caseInfo.nodeCache.hashSucceed );
        System.out.println("Cache Failed        \t" + caseInfo.nodeCache.hashFailed );
        System.out.println("NumCacheEntries     \t" + caseInfo.nodeCache.getNumCacheEntries() );
        System.out.println("Cache Size          \t" + caseInfo.nodeCache.cacheSize );
        System.out.println("Cache Recalculations\t" + caseInfo.nodeCache.recalculations );
        System.out.println("Node Cleanings      \t" + caseInfo.nodeCache.cleanCalled );
    }
    
    /** Convenience function : return 100 * epoch / max */
    //public double getPercentage() {    return 100.0 * (epoch / (double) max);    }    
    public double getPercentage() {    return (epoch / (double) max);    }
    
    /** Return parameterization of DAG representing best MMLEC. */
    public Value.Vector getBestParams( ModelLearner modelLearner) 
        throws ModelLearner.LearnerException {
        Value.Vector mmlSECVector = getResults();
        Value.Vector secVector = (Value.Vector)((Value.Structured)mmlSECVector.elt(0)).cmpnt(0);
        Value.Vector tomVector = (Value.Vector)((Value.Structured)secVector.elt(0)).cmpnt(0);
        Value.Structured bestTomStruct = (Value.Structured)tomVector.elt(0);
        Value.Vector tomParams = (Value.Vector)bestTomStruct.cmpnt(1);
        return tomParams;
    }
    
    /**  */
    public MetropolisSearch( java.util.Random rand, Value.Vector data,
                             ModelLearner mlModelLearner, ModelLearner mmlModelLearner ) 
    {
        super( rand, data, mlModelLearner, mmlModelLearner );
        
        System.out.println("Metropolis search : ML Model Glue  = " + mlModelLearner );
        System.out.println("Metropolis search : MML Model Glue = " + mmlModelLearner );
        System.out.println("Metropolis search : TomCoster = " + caseInfo.tomCoster );
                
        // create hashing functions.
        caseInfo.tomHash = new TOMHash( rand, numNodes );
        caseInfo.secHash = new SECHash( rand, numNodes );
        caseInfo.arcWeights = new double[numNodes][numNodes];
        
        // Initialise SEC hash table
        secHashtable = new Hashtable<SECHashKey,SEC>();

        // Number of epochs is calculated properly during first epoch.
        // max must be > 0 or it won't reach the first epoch.
        max = 1;
        
        // we have to manually set caseInfo 
        caseInfo.secHash.caseInfo = caseInfo;
        caseInfo.tomHash.caseInfo = caseInfo;
        caseInfo.nodeCache.caseInfo = caseInfo;
        
        // This is done in the original CaMML.
        setTemperature( 1.8 );
    }
}
