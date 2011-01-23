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
// Interface for simplifying the use of different costs on TOMs.
//

// File: TOMCoster.java
// Author: rodo@dgs.monash.edu.au
// Created on 31/08/2005

package camml.core.search;

import java.io.Serializable;

import camml.core.library.extensionCounter.BitSetBasedUnlabelledGraph;
import camml.core.library.extensionCounter.DynamicCounter;
import camml.core.library.extensionCounter.ExtensionCounter;
import camml.core.library.extensionCounter.UnlabelledGraph;
import cdms.core.FN;

/**
 * A TOMCoster contains functions used to cost the structure of a TOM.  This makes it easy
 * to switch from uniform priors over TOMs to expert elicited priors etc.
 *
 * A Default implementation is provided for uniformity over TOMs.
 *
 * NOTE: Any costings implemented should give the cost to state a TOM not a DAG.
 * Using a DAG costing metric may "double bias" the search (as it already clumps the prior
 * of several TOMs together for each DAG). <br>
 * 
 * NOTE2: Supplying a "relative cost" should provide identical results to a normalised cost
 * when running CaMML.  The same models & posterior will be returned, naturally the MML cost
 * displayed will be incorrect though.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.14 $ $Date: 2007/02/21 07:12:37 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/search/TOMCoster.java,v $
 */
public interface TOMCoster extends Serializable {

    /** Return the structure cost of the given Totally Ordered Model */
    public double cost( TOM tom );
    
    /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
     *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
     *  This function is provided as an optimisation to Metropolis and Anneal searches.
     */
    public double costToSwapOrder( TOM tom, int node1, int node2 );
    
    /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
     *  ie. cost(tom) - cost(tom.addArc(i,j))
     *  This function is provided as an optimisation to Metropolis and Anneal searches.
     */
    public double costToToggleArc( TOM tom, int node1, int node2 );
    
    
    /** Return the difference in cost between tom and tom with several arcs toggled.
     *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
     *  This function is provided as an optimisation to Metropolis and Anneal searches.
     */
    public double costToToggleArcs( TOM tom, int node1[], int node2[] );

    /** Set the value of arcProb used to cost TOM.  Some implementations may ignore this.*/
    public void setArcProb(double arcProb);
    
    /** Repair TOM to conform to any 'hard' constraints implied by this TOMCoster */
    public TOM repairTOM(TOM tom);
        
    
    /**
     * 
     * DefaultTOMCosterImplementation is an easy to extend implementation of TOMCoster.
     * cost(TOM) is left abstract and must be extended before use, but the other optimised
     * functions are provided to aid in prototyping.
     */
    public abstract static class DefaultTOMCosterImplementation implements TOMCoster {
        
        abstract public double cost( TOM tom );
        
        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {
            // Make a deep copy of tom, swap order in copy, return cost difference.
            TOM tom2 = (TOM)tom.clone();        
            tom2.swapOrder( node1, node2, true );        
            return cost(tom2) - cost(tom);
        }
        
        /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
         *  ie. cost(tom) - cost(tom.addArc(i,j))
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArc( TOM tom, int node1, int node2 ) {
            double oldCost = cost(tom);
            
            if ( tom.isArc(node1,node2) ) { tom.removeArc(node1,node2); }
            else { tom.addArc( node1, node2 ); }
            double newCost = cost(tom);
            if ( tom.isArc(node1,node2) ) { tom.removeArc(node1,node2); }
            else { tom.addArc( node1, node2 ); }
            
            return newCost - oldCost;
        }
        
        
        /** Return the difference in cost between tom and tom with several arcs toggled.
         *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArcs( TOM tom, int node1[], int node2[] ) {
            // Make deep copy of tom, toggle each arc in sequence, return cost difference.
            TOM tom2 = (TOM)tom.clone();
            
            // We must remove all arcs before adding new ones to avoid problems
            // with Node.ExcessiveArcsException.
            boolean toggled[] = new boolean[node1.length];
            for ( int i = 0; i < node1.length; i++ ) {
                if ( tom2.isArc(node1[i],node2[i]) ) { 
                    tom2.removeArc(node1[i],node2[i]);
                    toggled[i] = true;
                }
            }

            for ( int i = 0; i < node1.length; i++ ) {
                if (!toggled[i]) { tom2.addArc(node1[i],node2[i]); }
            }
            return cost(tom2) - cost(tom);
        }
        
        /** No repairs required by default */
        public TOM repairTOM(TOM tom) { return tom; }

    }
    
    /**
     * UniformTOMCoster implements TOMCoster so costs are "uniform" over TOMs. <br>
     * All orders are considered equally likely and coster at log(N!) nits.
     * Arcs are costed at log(arcP) & log(1-arcP) for existence/absence.
     */
    public static class UniformTOMCoster implements TOMCoster {
        //public static class UniformTOMCoster extends DefaultTOMCosterImplementation {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 6100186868899904624L;

        /** Probability of arc existence */
        private double arcProb;
        
        /** Cost to add an arc to the network (= logP(arc) - logP(1-arc) )*/
        private double costToAddArc;
    
        public void setArcProb(double arcProb) {
            this.arcProb = arcProb;
            this.costToAddArc = -Math.log(arcProb) + Math.log(1-arcProb);
        }
        
        
        /** */
        public UniformTOMCoster( double arcP ) {            
            setArcProb(arcP);
            this.costToAddArc = -Math.log(arcProb) + Math.log(1-arcProb);
        }
        
        
        
        /** Return the structre cost of the given TOM */
        public double cost( TOM tom ) { 
            // Calculate the cost to state the total ordering
            double totalOrderCost = 0;//FN.LogFactorial.logFactorial( tom.node.length );
            
            // now calculate the cost to state all the links in the model.
            double linkCost = 0;
            int numLinks = tom.getNumEdges();
            
            int numNodes = tom.getNumNodes(); 
            int maxNumLinks = (numNodes * (numNodes - 1)) / 2;
            
            linkCost = -1.0 * ( numLinks * Math.log( arcProb ) + 
                                (maxNumLinks - numLinks) * Math.log(1-arcProb));
            
            return linkCost + totalOrderCost;
             
        }        

        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {
            /*
            // Make a deep copy of tom, swap order in copy, return cost difference.
            TOM tom2 = (TOM)tom.clone();        
            tom2.swapOrder( node1, node2, true );        
            return cost(tom2) - cost(tom);
            */
            return 0.0;
        }
        
        /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
         *  ie. cost(tom) - cost(tom.addArc(i,j))
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArc( TOM tom, int node1, int node2 ) {
            if ( tom.isArc(node1,node2) ) { return -costToAddArc; }
            else { return costToAddArc; }
            /*
            // Make a deep copy of tom, toggle arc in copy, return cost difference.
            TOM tom2 = (TOM)tom.clone();
            if ( tom2.isArc(node1,node2) ) { tom2.removeArc(node1,node2); }
            else { tom2.addArc( node1, node2 ); }
            return cost(tom2) - cost(tom);
            */
        }
        
        
        /** Return the difference in cost between tom and tom with several arcs toggled.
         *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArcs( TOM tom, int node1[], int node2[] ) {

            int added = 0;
            for ( int i = 0; i < node1.length; i++) {
                if (tom.isArc(node1[i],node2[i])) {added--;} else {added++;}
            }
            return added * costToAddArc;
            
            /*
            // Make deep copy of tom, toggle each arc in sequence, return cost difference.
            TOM tom2 = (TOM)tom.clone();
            
            // We must remove all arcs before adding new ones to avoid problems
            // with Node.ExcessiveArcsException.
            boolean toggled[] = new boolean[node1.length];
            for ( int i = 0; i < node1.length; i++ ) {
            if ( tom2.isArc(node1[i],node2[i]) ) { 
            tom2.removeArc(node1[i],node2[i]);
            toggled[i] = true;
            }
            }

            for ( int i = 0; i < node1.length; i++ ) {
            if (!toggled[i]) { tom2.addArc(node1[i],node2[i]); }
            }
            return cost(tom2) - cost(tom);
            */
        }

        /** No repairs required by default */
        public TOM repairTOM(TOM tom) { return tom; }

    }

    /**
     * DAGCoster returns the cost of a DAG instead of the cost of a TOM. <br>
     * It uses camml.core.library.ExtensionCounter.DynamicCouner to count extensions to
     *  try and reduce the complexity of performing an NP calculation.
     */
    public static class DAGCoster extends DefaultTOMCosterImplementation {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -1664157958478015601L;

        /** Probability of arc existence */
        private double arcProb;

        /** log(arcP) - log(1.0-arcP) */
        private double costToAddArc;
        
        public void setArcProb(double arcProb) {
            this.arcProb = arcProb;
            this.costToAddArc = Math.log(arcProb) - Math.log(1.0-arcProb);
        }
        
        /** */
        public DAGCoster( double arcP ) {
            setArcProb(arcP);
        }
        
        
        
        // Linear extension caounter.
        final static DynamicCounter counter = DynamicCounter.dCounter;
        
        /** Cache of last N tom hashes counted. */
        private static final double[] countCache = new double[16];

        /** Cache of last N tom hashes counted. */
        private static final long[] countCacheKey = new long[16];

        /** Cache of last N tom hashes counted. */
        private static int countCacheIndex = 0;
        
        /** Count the number of linear extensins for the supplied TOM */
        public static double countExtensions(TOM tom) {
            
            // Check if TOM perms is already in cache.
            int tomHash = tom.hashCode();
            if (tomHash != 0) {
                for (int i = 0; i < countCache.length; i++) {
                    int index =  (i+countCacheIndex)&0x0F;
                    if ( countCacheKey[index] == tomHash ) {
                        return countCache[index];
                    }
                }
            }
            
            // Create UnlabelledGraph of TOM
            UnlabelledGraph g = new BitSetBasedUnlabelledGraph(tom.node.length);
            for (int i = 0; i < tom.node.length; i++) {
                for (int j = 0; j < tom.node[i].parent.length; j++) {
                    int a = tom.node[i].parent[j];
                    if (!g.isDirectedArc(a,i)) { g.addArc(a,i,true); };
                }
            }
            
            // Count permutations.
            double perms = counter.countPerms(g);
            
            // add TOM perms to cache.
            countCacheIndex = (countCacheIndex+1) & 0x0F;
            countCache[countCacheIndex] = perms;
            countCacheKey[countCacheIndex] = tomHash;
            
            return perms;
        }
        
        
        
        /** Return the structre cost of the given TOM */
        public double cost( TOM tom ) { 
            // Calculate the cost to state the total ordering
            double totalOrderCost = FN.LogFactorial.logFactorial( tom.node.length );
            
            double ext = countExtensions(tom);
            totalOrderCost -= Math.log(ext);
            
            
            // now calculate the cost to state all the links in the model.
            double linkCost = 0;
            int numLinks = tom.getNumEdges();
            
            int numNodes = tom.getNumNodes(); 
            int maxNumLinks = (numNodes * (numNodes - 1)) / 2;
            
            linkCost = -1.0 * ( numLinks * Math.log( arcProb ) + 
                                (maxNumLinks - numLinks) * Math.log(1-arcProb));
            
            return linkCost + totalOrderCost;
             
        }        
        
        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {

            if (tom.isArc(node1,node2)) {
                double ext1 = countExtensions(tom);
                tom.swapOrder(node1,node2,true);
                double ext2 = countExtensions(tom);
                tom.swapOrder(node1,node2,true);
            
                return Math.log(ext1) - Math.log(ext2);
            } else {
                return 0.0;
            }
            
            
            
        }
        
        /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
         *  ie. cost(tom) - cost(tom.addArc(i,j))
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArc( TOM tom, int node1, int node2 ) {
            
            double ext1 = countExtensions(tom);
            double ext2;
            double arcCost = costToAddArc;
            
            if (tom.isArc(node1,node2)) {
                tom.removeArc(node1,node2);
                ext2 = countExtensions(tom);
                tom.addArc(node1,node2);
                // Negate arcCost as we need to add an arc.
                arcCost = -arcCost;
            }
            else {
                tom.addArc(node1,node2);
                ext2 = countExtensions(tom);
                tom.removeArc(node1,node2);
            }
            
            return Math.log(ext1) - Math.log(ext2) - arcCost;
            
        }
        
        
        /** Return the difference in cost between tom and tom with several arcs toggled.
         *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArcs( TOM tom, int node1[], int node2[] ) {

            double ext1 = countExtensions(tom);
            // We must remove all arcs before adding new ones to avoid problems
            // with Node.ExcessiveArcsException.
            int added = 0;
            boolean toggled[] = new boolean[node1.length];
            for ( int i = 0; i < node1.length; i++ ) {
                if ( tom.isArc(node1[i],node2[i]) ) { 
                    tom.removeArc(node1[i],node2[i]);
                    toggled[i] = true;
                    added--;
                }
            }
            for ( int i = 0; i < node1.length; i++ ) {
                if (!toggled[i]) { tom.addArc(node1[i],node2[i]); added++; }
            }
            
            // Count extensions with arcs toggled.
            double ext2 = countExtensions(tom);
            
            // Untoggle arcs.
            for ( int i = 0; i < node1.length; i++ ) {
                if ( tom.isArc(node1[i],node2[i]) ) { 
                    tom.removeArc(node1[i],node2[i]);
                    toggled[i] = false;
                }
            }
            for ( int i = 0; i < node1.length; i++ ) {
                if (toggled[i]) { tom.addArc(node1[i],node2[i]); }
            }
            
            return Math.log(ext1) - Math.log(ext2) - added * costToAddArc;
        }

        /** No repairs required by default */
        public TOM repairTOM(TOM tom) { return tom; }

    }
    
    
}
