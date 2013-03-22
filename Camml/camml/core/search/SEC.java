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
// SEC for CaMML
//

// File: SEC.java
// Author: rodo@dgs.monash.edu.au


package camml.core.search;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import camml.core.models.ModelLearner;
import cdms.core.Value;
import cdms.core.VectorFN;

/**
 *  SEC contains a list of TOMs.  <br>
 *  A SEC is a group of TOMs, all of which have the same skeleton, but their arc directions may
 *   vary. <br>
 */
public class SEC implements Serializable
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 238869362228632969L;

    /** A vector of CompactTOMs */
    protected final ArrayList<CompactTOM> tomList;
    
    /** Layout of undirected edges for this SEC */
    protected final int[][] edgeList2;
    
    /** total weight of all TOMs in this SEC */
    protected double weight = 0;
    public double getWeight() { return weight; }
    
    /** total posterior of all TOMs in this SEC */
    protected double posterior;
    public double getPosterior() { return posterior; }
    
    /**
     * relativePrior is calculated as (Posterior(SEC|Data) / prob(Data|SEC)) / (prior of best SEC)
     * This value is calculated externally.  The relativePrior of the best SEC is 1.0
     * The value defaults to -1 when it has not been calculated. <br>
     *
     * relativePrior[i] = (exp(cost[i])*posterior[i]) / (exp(bestCost)*bestPosterior)
     */
    public double relativePrior = -1;
    
    /** clean MML cost of SEC.  All clean TOMs in SEC should have identical MML costs */
    public final double cleanMLCost;
    
    /** best MML cost of a TOM in this SEC */
    protected double bestMML = Double.POSITIVE_INFINITY;;
    
    
    /** best MML cost of a TOM found through sampling. */
    protected double bestUncleanMML = Double.POSITIVE_INFINITY;
    
    /** caseInfo contains various useful values. */
    public final CaseInfo caseInfo;
    
    /** Create a new SEC, addTOM must still be called to add the first TOM */
    public SEC( TOM cleanTom, double cleanMLCost, CaseInfo caseInfo )
    {
        this.caseInfo = caseInfo;
        
        // extract undirected edge listfrom TOM
        edgeList2 = new int[2][cleanTom.getNumEdges()];
        int numNodes = cleanTom.node.length;
        
        // NOTE: Previous behaviour "double cleaned" TOMs, this may have cause different behaviour.
        //cleanTom.clean();
        
        int edgeNum = 0;
        for ( int i = 0; i < numNodes; i++ ) {
            Node currentNode = cleanTom.getNode(i);
            for ( int j = 0; j < currentNode.parent.length; j++) {
                edgeList2[0][edgeNum] = i;
                edgeList2[1][edgeNum] = currentNode.parent[j];
                edgeNum ++;
            }
        }
        
        // Create tomList to store all sampled TOMs
        tomList = new java.util.ArrayList<CompactTOM>();
        this.cleanMLCost = cleanMLCost;
    }
    
    /**
     * Record a new visit to this SEC, if this TOM hasn't been visited before, it will be added 
     *  to this list tomVector.  Weight is updated to reflect a visit to the appropriate TOM.
     */
    public void addTOM( TOM cleanTom, double uncleanMML, double temperature )
    {
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
            tomList.add( new CompactTOM( cleanTom, hash ) );
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
    
    /** Update reference weight for SEC and all DAGs within SEC*/
    public void updateReferenceWeight( double multiplier )
    {
        weight *= multiplier;
        for ( int i = 0; i < tomList.size(); i++ ) {
            ((CompactTOM)tomList.get(i)).totalWeight *= multiplier;
        }
    }
    
    /** Comparator : compare TOM weight */
    public static Comparator<CompactTOM> tomWeightComparator =
        new TOMWeightComparator();
    
    public static class TOMWeightComparator implements Comparator<CompactTOM>, Serializable {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -7295458595635957815L;

        public int compare( CompactTOM a, CompactTOM b ) {
            double weightA = a.getWeight();
            double weightB = b.getWeight();
            if ( weightA > weightB ) return -1;
            if ( weightA < weightB ) return 1;
            if ( weightA == weightB ) return 0;
            // This will only occur when weightA or weightB == NaN
            throw new RuntimeException("Bad Comparison of (" + a + "," + b );
        }    
        
    }

    
    
    /** Comparator : compare TOM MML */
    public static Comparator<CompactTOM> tomMMLComparator = new TOMMMLComparator();
    public static class TOMMMLComparator implements Comparator<CompactTOM>, Serializable {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2921274702701945061L;

        public int compare( CompactTOM a, CompactTOM b ) {
            double mmlA = ((CompactTOM)a).bestMML;
            double mmlB = ((CompactTOM)b).bestMML;
            if ( mmlA < mmlB ) return -1;
            if ( mmlA > mmlB ) return 1;
            if ( mmlA == mmlB ) return 0;
            // This will only occur when mmlA or mmlB == NaN
            throw new RuntimeException("Bad Comparison of (" + a + "," + b );
        }
    };
    
    /** Sort tomList using tomComparator. */
    public void sortTOMs()
    {
        final Comparator<CompactTOM> tomComparator;
        
        // For regression testing choose best tom by MML, otherwise use Posterior.
        if ( caseInfo.regression ) { tomComparator = tomMMLComparator; }
        else { tomComparator = caseInfo.tomComparator; }
        
        Collections.sort( tomList, tomComparator );    
    }
    
    /** A compact representation of a TOM, edge[][] is not required as SEC.edge is used. */
    protected class CompactTOM implements Serializable
    {    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 4061434832572310885L;

        /** total ordering of TOM */
        protected final int[] order;
        
        /** Number of times TOM has been samples */
        protected int numVisits;
        
        /** total weight of TOM samples */
        protected double totalWeight;
        
        /** TOMHash() of this TOM */
        public final long hash;
        
        /** best MML cost found for this TOM so far (usually it's clean cost) */
        public double bestMML;
        
        /** Constructor : store totalOrder.clone() and hash */
        public CompactTOM( TOM tom, long hash )
        {
            this.order = (int[])tom.totalOrder.clone();
            this.hash = hash;        
        }
        
        /** Accessor function */
        public int getNumVisits() { return numVisits; }
        
        /** Accessor function */
        public void incrementVisits() { numVisits ++; }
        
        /** Accessor function */
        public void addWeight( double weight ) { totalWeight += weight; numVisits ++;}
        
        /** Accessor function */
        public double getWeight() { return totalWeight; }
        
        /** Accessor function */
        public double getPosterior() { return totalWeight / caseInfo.totalWeight; }
        
        protected double dataCost = -1;
        
        /** Calculate logP(Data|Model,Params) for tom[i] */
        public double getDataCost() {
            if ( dataCost != -1) { return dataCost;}
            TOM tom = makeTOM();
            Value.Vector params;
            
            try { params = tom.makeParameters(caseInfo.mmlModelLearner); }
            catch (ModelLearner.LearnerException e) { return Double.POSITIVE_INFINITY;}
            Value.Vector x = caseInfo.data;
            dataCost = -caseInfo.bNet.logP(x,params,new VectorFN.UniformVector(x.length(),Value.TRIV));
            return dataCost;
        }

        /** Create a full TOM from CompactTOM */
        public TOM makeTOM( )
        {
            // create TOM
            TOM tom = new TOM( caseInfo );
            
            // set total ordering to match CompactTOM
            // This must be done before adding arcs to avoid having too many parents present.
            for ( int i = 0; i < order.length; i++ ) {
                tom.swapOrder( tom.nodeAt(i), this.order[i], false);
            }
            
            for ( int i = 0; i < edgeList2[0].length; i++) {
                tom.addArc(edgeList2[0][i], edgeList2[1][i]);
            }
            
            return tom;
        }
        
        /** Return string containing details about TOM */
        public String toString() { 
            return "CompactTOM : numVisits = " + numVisits + "\t" + 
                "totalWeight = " + totalWeight + "\t" + 
                "bestMML = " + bestMML; 
        } 
        
    } // end of CompactTOM declaration.
    
    /** returns thenumber of arcs in this SEC */
    public int getNumArcs() { 
        return edgeList2[0].length;
    }
    /** Accessor function */
    public int getNumTOMs() { return tomList.size(); }
    
    /** Accessor function */
    public TOM getTOM( int i ) { return ((CompactTOM)tomList.get(i)).makeTOM(); }
    
    /** Accessor function */
    public int[] getTOMOrdering( int i ) { return ((CompactTOM)tomList.get(i)).order; }
    
    /** Accessor function */
    public int getNumVisitsToTOM( int i ) { return ((CompactTOM)tomList.get(i)).numVisits; }
    
    /** Accessor function */
    public double getWeightOfTOM( int i ) { return ((CompactTOM)tomList.get(i)).totalWeight; }
    
    /** Accessor function */
    public double getPosteriorOfTOM( int i ) { 
        return ((CompactTOM)tomList.get(i)).totalWeight/caseInfo.totalWeight; }
    
    /** Accessor function */
    public double getBestMMLOfTOM( int i ) { return ((CompactTOM)tomList.get(i)).bestMML; }

    /** Calculate logP(Data|Model,Params) for tom[i] */
    public double getDataCost(int i) {
        return tomList.get(i).getDataCost();
    }
    
    /** Return string containing details about SEC */
    public String toString() 
    {
        
        String s =  "numTOMs = " + tomList.size()
            + "\t" + "posterior = " + posterior
            + "\t" + "mlCost = " + cleanMLCost
            + "\t" + "weight = " + weight
            + "\t" + "best MML = " + bestMML + "\n";
        
        for ( int i = 0; i < tomList.size(); i++ ) {
            CompactTOM currentTOM = ((CompactTOM)tomList.get(i));
            s += "\n" + "numVisits = " + currentTOM.numVisits
                + "\t" + "mml = " + currentTOM.bestMML
                + "\t" + "posterior = " + currentTOM.getPosterior()
                + "\t" + "weight = " + currentTOM.totalWeight
                + "\n" + currentTOM.makeTOM();
        }
        
        return s;
        
    }
    
    /** SEC posterior comparator */
    public static Comparator<SEC> secWeightComparator = 
        new Comparator<SEC>() {
        public int compare( SEC a, SEC b ) {         
            double posteriorA = a.weight;
            double posteriorB = b.weight;
            if ( posteriorA > posteriorB ) { return -1; }
            if ( posteriorA < posteriorB ) { return  1; }
            if ( posteriorA == posteriorB ) { return 0; }
            throw new RuntimeException("Bad Comparison of (" + a + "," + b );
        }
    };
    
    
}
