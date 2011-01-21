//
// Case Information for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: CaseInfo.java
// Author: rodo@dgs.monash.edu.au


package camml.core.search;

import cdms.core.*;
import camml.core.models.bNet.BNet;
import camml.core.models.bNet.BNetStochastic;

import camml.core.models.*;
import camml.core.search.SEC.CompactTOM;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 * CaseInfo contains numerous variables and acts as a centralised place to keep track of 
 * data relating to a particular search/dataset combination.
 * 
 * Some parts of the file are only relevent for particular search types.
 */
public class CaseInfo implements Serializable {
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 6773205128107956893L;

    /** BNet model with appropriate data type */
    public BNet bNet;
    
    /** Hash function using directed arcs. */
    public ModelHash tomHash;
    
    /** Hash function using undirected arcs and ML cost */
    public ModelHash secHash;
    
    /** Data set being used. */
    public Value.Vector data;
    
    /** MML Model Learner */
    public ModelLearner mmlModelLearner;
    
    /** ML Model Learner */
    public ModelLearner mlModelLearner;
    
    /** NodeCache caches values for nodes such as ML and MML scores. */
    public NodeCache nodeCache;
    
    /** Function used to cost the structure of TOMs. */
    public TOMCoster tomCoster;
    
    /** arcWeights[i][j] = total weight of all unclean toms where j -> i exists. */
    public double[][] arcWeights; 

    /** Update arcPortion based on weight. */
    public boolean updateArcWeights = false;
    
    /** If true, arc weights will be printed at end of metropolis search. */
    public boolean printArcWeights = false;

    ///////////////////
    // Useful Values //
    ///////////////////
    
    /** MML cost all weights are created in reference too.  */
    public double referenceWeight;
    
    /** Total weight acumulated in all SECs */
    public double totalWeight = 0;
    
    /** Current arc probability */
    public double arcProb = 0.5;
    
    /** Current temperature value */
    public double temperature = 1.0;
    
    /** How long should the search go for? 1.0 is default Value, Setting to 0.5 halves search time, 2.0 doubles it, etc.*/
    public double searchFactor = 1.0; //0.00001;

    
    ////////////////////
    // SEARCH OPTIONS //
    ////////////////////
    
    /** Should the merging by KL distance step consider mergers to models with more arcs? */
    protected boolean allowMergeToModelWithMoreArcs = false;
    
    /** Function used for cleaning. */
    public TOMCleaner tomCleaner = TOMCleaner.StandardTOMCleaner.tomCleaner;
    
    /** Type of SEC joining used. 0 -> KL, 3 -> CKL3, ignored if joinSECs = false. */
    public int cklJoinType = 0;
    
    /** Should DAGs be grouped into SECs in Metropolis search? */
    public boolean joinDAGs = true;
    
    /** Should SECs be joined by KL distance in Metropolis search? */
    public boolean joinSECs = true;    
    
    /** Calaulate KL distance exactly for merging. */
    public boolean useExactKL = false;

    /** Use BNetNetica instead of BNetStochastic. */
    public boolean useNetica = false;

    
    /** If safeMode is true, the next model must have a better MML score than the current model. */
    public boolean safeMode = false;
    
    /** If (currentCost >= currentCost + safeCap) safeMode = true */
    public final double safeCap = 40.0;
    
    /** Maximum number of SECs to retain post Metropolis Sampling. 
     *  note: Making this value large slows down SEC joining step. */
    public int maxNumSECs = 30;
    
    /** Minimal total SEC posterior to retain from Metropolis Sampling. 
     *  This value is ignored if > metropolisMaxSECs is required. */
    public double minTotalPosterior = 0.999;
    
    /** Some code should only be used in regression testing. */
    public boolean regression = false;
    
    /** How should DAGs within an SEC be sorted? */
    public Comparator<CompactTOM> tomComparator = SEC.tomWeightComparator;
    
    /////////////////////
    // LOGGING OPTIONS //
    /////////////////////
    /** Log all actions in Metroplis Search */
    public boolean logging = false;
    /** Log all actions in Anneal Search */
    public boolean annealLogging = false;    
    /** Log all cleaning */
    public boolean logCleaning = false;   
    /** Format uses when logging */
    public DecimalFormat weightFormat = new DecimalFormat("######0.000000");
    /** Format uses when logging */
    public DecimalFormat costFormat = new DecimalFormat("######0.000000");
    /** Format uses when logging */
    public DecimalFormat posteriorFormat = new DecimalFormat("######0.0000");
    
    // Initialise log file if required
    public java.io.FileWriter cammlLog;
    {
        if ( logging || annealLogging || logCleaning ) {
            try { cammlLog = new java.io.FileWriter("newCamml.log");}
            catch(Exception e) {cammlLog = null;}
        }
    }
    
    
    
    /** Initialise Caseinfo */
    public CaseInfo( TOMHash tomHash, SECHash secHash, Value.Vector data, ModelLearner mmlModelLearner, 
                     ModelLearner mlModelLearner,     double referenceWeight, NodeCache nodeCache ) {
        this.tomHash = tomHash; this.secHash = secHash;
        this.data = data; this.mmlModelLearner = mmlModelLearner;
        this.nodeCache = nodeCache;
        this.mlModelLearner = mlModelLearner; this.referenceWeight = referenceWeight;
        this.totalWeight = 0;
        Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
        bNet = new BNetStochastic(dataType);
    }
    
    
    /** Print details. */
    public String toString() {
        return "bNet = " + bNet + "\n" + 
            "tomHash = " + tomHash + "\n" + 
            "secHash = " + secHash + "\n" + 
            "data.t = " + data.t + "\n" +
            "data.length()" + data.length() + "\n" +
            "mmlModelLearner = " + mmlModelLearner + "\n" + 
            "mlModelLearner = " + mlModelLearner + "\n" + 
            "referenceWeight = " + referenceWeight + "\n" + 
            "totalWeight = " + totalWeight + "\n" + 
            "nodeCache = " + nodeCache;
    }    
}

