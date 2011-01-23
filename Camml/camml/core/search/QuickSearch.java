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
// Quick Search for CaMML
//

// File: QuickSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import cdms.core.*;
import cdms.plugin.search.*;

import camml.core.models.ModelLearner;

/**
 * Deprecated first attempt at CaMML algorithm.  MetropolisSearch or AnnealSearch should be used.
 * 
 * Quick search is based on the Quick search in the original version of CaMML by
 * Chris Wallace. <br>
 * <br>
 * Metroposis search moves through TOM space using a TemporalChange and SkeletalChange operation.
 * The new TOM is either accepted or rejected based on its cost ccompared with the previous TOM.
 * <br> <br>
 * Each TOM visited is hashed to generate a number in the range [0,2^16] and its entry in a hash
 * table is incremented.  The TOM with the most hashed entries is taken as the best TOM. <br>
 *
 * In addition to this, cleaning of insignificant arcs should occur, this is a future extension.
 *
 *
 * Currently the sampling is more or less correct, but the interpretation is badly wrong.
 * TOMs should be sampled, each TOM should be hashed into a MML equivelence class, and a 
 *  as a TOM.  Structural equivelence use skeleton and ML estimator, TOM just uses skeleton
 *  note: TOM uses directed arcs in skeleton, MML equivelence does not.
 *
 * On top of this cleaning should be done.  This entails removing all arcs from each node which 
 *  would reduce the cost to state that node.  This "parentMask" is only used in hashing models
 *  and is irrelevant as far as generating a MML cost of a model.
 *
 * The top N MML equivelent models are kept.  Each of these can then be cleaned (I don't think
 *  cleaning is really required, it seems to have been initially included to allow a cheap and
 *  nasty test to have been used initially for removing insignificant arcs from nodes, then fixing
 *  the results in one big hit at the end.  Maybe also for the KL bit mentioned in the paper?)
 *
 * TOMs only need to be hashed to get the best representative TOM for a given MML class.
 */

public class QuickSearch implements Search.SearchObject 
{
    /** Function to make Skeletal changes to TOMs - includes modelLearner for costing/parameterising */
    protected SkeletalChange skeletalChange;
    
    /** Function to make Temporal changes to TOMs - includes modelLearner for costing/parameterising */
    protected TemporalChange temporalChange;
    
    /** Function to make double skeletal changes to TOMs - 
        includes modelLearner for costing/parameterising */
    protected DoubleSkeletalChange doubleSkeletalChange;
    
    /** vector containing the full data of the network */
    protected final Value.Vector fullData;
    protected final int numSamples;
    protected final int numVariables;
    
    /** Value.Function to return a view of the full data */
    protected final Value.Function dataView;
    
    /** The tom being worked on */
    protected final TOM tom;
    
    /** Accesor function for tom */
    public TOM getTOM() { return tom; }
    
    /** cost of each individual node*/
    protected double[] nodeCost;
    
    /** Random number generator */
    protected java.util.Random rand;
    
    /** Current Epoch */
    protected int epoch = 0;
    
    /** Maximum number of epochs */
    protected final int max;
    
    /** flag if search is complete or not. */
    protected boolean searchDone = false;
    
    protected double arcProb = 0.5;
    
    protected double temperature = 1.0;
    
    //     /** Modify the current arc probability.  This effects structureCost and the Tom mutation
    //      *  operators (skeletalChange, temporalChange, doubleSkeletalCgange)
    //      */
    //     protected void setArcProb( double arcProb )
    //     {
    //     this.arcProb = arcProb;    
    
    //     skeletalChange = new SkeletalChange( rand, mmlModelLearner, arcProb, caseInfo );
    //     temporalChange = new TemporalChange( rand, mmlModelLearner, arcProb, caseInfo );
    //     doubleSkeletalChange = new DoubleSkeletalChange( rand, mmlModelLearner, arcProb, caseInfo );
    
    
    //     }
    
    /** Modify the current arc probability.  This effects structureCost and the Tom mutation
     *  operators (skeletalChange, temporalChange, doubleSkeletalCgange)
     */
    protected void setArcProb( double arcProb )
    {
        this.arcProb = arcProb;    
        updateMutationOperators( arcProb, temperature );
    }
    
    /** Modify the current search temperature (defaults to 1.0).  Higher temperatures increace the
        likelyhood of a bad (ie. high mml cost) model being accepted.
    */
    protected void setTemperature( double temperature )
    {
        this.temperature = temperature;    
        updateMutationOperators( arcProb, temperature );
    }
    
    /** Create new skeletalChange, temporalChange and doubleSkeletalChange operators */
    protected void updateMutationOperators( double arcProb, double temperature ) 
    {
        skeletalChange = new SkeletalChange( rand, arcProb, caseInfo, temperature );
        temporalChange = new TemporalChange( rand, arcProb, caseInfo, temperature );
        doubleSkeletalChange = new DoubleSkeletalChange( rand, arcProb, caseInfo, temperature );
        
    }
    
    
    protected double currentCost;
    protected double bestCost = Double.POSITIVE_INFINITY;
    protected TOM bestTOM = null;
    
    public TOM getBestTOM() { return bestTOM; }
    public double getBestCost() { return bestCost; }
    public double getBestArcProb() { return ((double)bestTOM.getNumEdges()+0.5) / 
            (double)(((numVariables * (numVariables - 1)) / 2)+1.0); }
    public int getBestNumArcs() { return bestTOM.getNumEdges(); }
    
    
    //    public NodeCache nodeCache;
    public CaseInfo caseInfo;
    
    /** 
     * Reset the search. <p>
     * NOTE:TOM is not reset here (but possibly should be?) 
     */
    public void reset() 
    {
        System.out.println( "Sampling " + max + " TOMs" );
        System.out.println( numSamples + " data points from " + numVariables + " variables." );
        
        
        // randomise the initial order of nodes so it is not bias towards an a->b->c type ordering.
        tom.randomOrder(rand);
        epoch = 0;
        searchDone = false;
    }
    
    /** 
     * Calculate the cost of stating all CPT's in the network + the structure cost of a 
     * single TOM <br>
     * Individual costs are not recalculated if their parents haven't been changed. <br>
     * Parameters of Nodes are updated <br>
     */
    public double costNetwork(    ModelLearner modelLearner ) 
    {
        double totalCost = 0;
        
        // for each node
        for (int i = 0; i < tom.getNumNodes(); i++) {
            
            Node currentNode = tom.getNode(i);
            
            nodeCost[i] = caseInfo.nodeCache.getCost( currentNode, modelLearner );
            
            totalCost += nodeCost[i];
        }
        
        if ( Double.isNaN( totalCost ) ) {
            throw new RuntimeException("totalCost == NAN");
        }
        
        return totalCost + structureCost();
    }
    
    /**
     * In a single epoch, one mutation of the network is attempted.  If this mutation is accepted
     * then the current TOM is modified.  If it is rejected the current TOM remains unchanged.
     * Either way, the count for the number of visits is incremented for the resulting TOM.
     */
    public double doEpoch() 
    {
        // Print a dot 100 times throughout the sampling process.
        //     if (epoch == 0) {
        //         for (int i = 0; i < 100; i++)
        //         System.out.print(i % 10);
        //         System.out.println();
        //     }
        if (epoch % (max / 100) == 0) {
            // System.out.print(".");
            double bestArcProb = getBestArcProb();
            if ( bestArcProb != arcProb && epoch != 0) {
                if ( bestArcProb < 0.5 ) {  // arcProb is capped at 0.5
                    setArcProb( bestArcProb );
                }
            }
            System.out.print("<"+bestCost+","+arcProb+">\t");
            if ( epoch % (max/50) == 0 ) {
                System.out.println();
            }
        }
        
        
        boolean accepted;
        
        // Network must be costed on the first epoch.
        if (epoch == 0) {
            currentCost = costNetwork( mmlModelLearner );
        } 
        else { // Mutate the network and see check if the resulting network is accepted or rejected.
            double rnd = rand.nextDouble();
            if (rnd < 0.333) {
                accepted = skeletalChange.transform(tom, currentCost);
            } else if (rnd < 0.666) {
                accepted = temporalChange.transform(tom, currentCost);
            } else {
                accepted = doubleSkeletalChange.transform(tom, currentCost);
            }
            
            // if new model is accepted, update currentCost
            if (accepted) {
                currentCost = costNetwork( mmlModelLearner );
            }
        }
        
        if ( currentCost < bestCost ) {
            bestTOM = (TOM)tom.clone();
            bestCost = currentCost;
        }
        
        
        // increment the number of epochs completed.
        epoch++;
        
        // currentCost is required by search interface to draw "pretty graphs", etc.  with.
        return currentCost;
    }
    
    
    /** If finished, print results. */
    public boolean isFinished() 
    {
        boolean done = (epoch == max || searchDone == true);
        return done;
    }
    
    public double getPercentage() 
    {
        return (epoch / (double) max);
    }
    
    /** return the cost to state the total ordering and the link matrix. */
    protected double structureCost()
    {
        int numNodes = tom.getNumNodes();
        
        // Calculate the cost to state the total ordering
        double totalOrderCost = 0; //? FN.LogFactorial.logFactorial( numNodes );
        
        // now calculate the cost to state all the links in the model.
        double linkCost = 0;
        int numLinks = tom.getNumEdges();
        
        
        int maxNumLinks = (numNodes * (numNodes - 1)) / 2;
        
        linkCost = -1.0 * ( numLinks * Math.log( arcProb ) + 
                            (maxNumLinks - numLinks) * Math.log(1-arcProb));
        
        // Total cost to state the structure of a TOM
        return linkCost + totalOrderCost;
    }
    
    
    /** Learner to find MML estimates of nodes cost and parameters */
    protected final ModelLearner mmlModelLearner;
    
    /**
     * ml model learner is not strictly needed for this search, but it makes for easy reusability 
     * of NodeCache
     */
    protected final ModelLearner mlModelLearner;
    
    /** Create a new TOM with no arcs present. */
    public QuickSearch(java.util.Random rand, Value.Vector data,  
                       ModelLearner mlModelLearner, ModelLearner mmlModelLearner ) 
    {
        
        this.rand = rand; // save random number generator.
        fullData = data; // save the full data set for future use.    
        numVariables = ((Value.Structured) fullData.elt(0)).length();
        numSamples = data.length();
        
        // Defind learner used for MML and ML estimates of parameters.
        // note: ML is not actually used in QuickSearch but is included so the nodeCache
        //       can also find ML costs if required.
        //    this.mmlModelLearner = camml.models.WallaceCPTLearner.multinomialCPTLearner;
        //     this.mmlModelLearner = camml.models.DTreeLearner.multinomialDTreeLearner;
        
        //      this.mmlModelLearner = new camml.models.DualLearner( camml.models.WallaceCPTLearner.multinomialCPTLearner, camml.models.DTreeLearner.multinomialDTreeLearner );
        //     this.mlModelLearner = camml.models.CPTLearner.mlMultinomialCPTLearner;
        
        this.mlModelLearner = mlModelLearner;    
        this.mmlModelLearner = mmlModelLearner;
        
        
        // set number of epochs to run as (min(10,numVars) ^ 3 * 200)
        int temp = numVariables;
        if (temp < 10) {
            temp = 10;
        }
        
        max = temp * temp * 200;
        //    max = temp * temp * temp * 200;
        //    max = 200000;
        
        // Function to create views into the data.
        dataView = (Value.Function) camml.core.library.CammlFN.view.apply(fullData);
        tom = new TOM(fullData); // create the tom to search over
        nodeCost = new double[tom.getNumNodes()];
        
        // Create the node cache.  mmlLearner is used to clean models.
        NodeCache nodeCache = new NodeCache( data, mmlModelLearner, mlModelLearner );
        
        // Set up the nodeCache to be used by transformation operations.
        //    TOMTransformation.nodeCache = nodeCache;
        
        caseInfo = new CaseInfo(  null, null, data, mmlModelLearner, mlModelLearner,
                                  -1, nodeCache );
        nodeCache.caseInfo = caseInfo;
        caseInfo.tomCoster = new TOMCoster.UniformTOMCoster(arcProb);
        
        //     // Initialise transformation operations.
        //     skeletalChange =  new SkeletalChange( rand, mmlModelLearner, arcProb, caseInfo );
        //     temporalChange =  new TemporalChange( rand, mmlModelLearner, arcProb, caseInfo );
        //     doubleSkeletalChange =  new DoubleSkeletalChange( rand, mmlModelLearner, arcProb, caseInfo );
        
        
        setArcProb(0.5);
        bestTOM = (TOM)tom.clone();
        bestCost = costNetwork( mmlModelLearner );
    }
    
    
    
}
