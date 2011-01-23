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
// Simple Search for CaMML
//

// File: GreedyLookaheadSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.core.search;

import cdms.core.*;


/** 
 * GreedyLookaheadSearch as the name suggests is a very basic search algorithm to find the TOM with 
 * the lowest mml cost.  This cost is simply the sum of costs of each node.  Each node being a
 * single CPT.
 * No attemts is made to take into consideration the number of orderings in the network, or any
 * other factors which a more complete searhc would address. <br>
 * This search is intended to be a prototype and nothing more.
 */
public class GreedyLookaheadSearch 
    extends BNetSearch 
{
    /** Maximum number of steps to look ahead */
    protected final int maxLookahead;
    
    /** Current Epoch */
    protected int epoch = 0;
    
    /** Maximum number of epochs */
    protected final int max = 1000;
    
    /** flag if search is complete or not. */
    protected boolean searchDone = false; 
    
    /** 
     * Reset the search. <p>
     * NOTE:TOM is not reset here (but possibly should be?) 
     */
    public void reset()
    {
        // randomise the initial order of nodes so it is not bias towards an a->b->c type ordering.
        tom.randomOrder(rand);
        epoch = 0;
        searchDone = false;
    }
    
    /** mutate the TOM.  a & b are nodes, operation = (toggleArc,swapOrder)*/
    void mutate( int operation, int a, int b)
    {
        if (operation == 0)
            toggleArc(a,b);
        else if (operation == 1)
            swapOrder(a,b);
    }
    
    /** If arc is present remove it, if are is not present create it */
    void toggleArc( int a, int b)
    {
        if ( tom.isArc(a,b) ) {
            tom.removeArc(a,b);
        }
        else {
            tom.addArc(a,b);
        }
    }
    
    /** Swap the order of a abd b in the network */
    void swapOrder( int a, int b )
    {
        tom.swapOrder( a, b, true );
    }
    
    /** 
     * Given a single mutation, look ahead k operations and return the best cost seen. <br>
     * If k = 0 then no lookahead is performed.  <br>
     * To speed things up, all operations are given an ordering.
     **/
    double lookahead( int currentOperation, int currentA, int currentB, int k )
    {    
        // Perform the initial mutation on the network and use this cost as bestCost
        mutate( currentOperation, currentA, currentB );
        // double bestCost = costNetwork() + structureCost;
        double bestCost = costNetwork( mmlModelLearner, false );
        
        if (k != 0) {
            
            // Do a single iteration of a greedy search.
            for ( int a = 0; a < tom.getNumNodes(); a++ ) {
                for (int b = a+1; b < tom.getNumNodes(); b++ ) {
                    // operation = toggleArc(a,b) or swapOrder(a,b) 
                    for (int operation = 0; operation < 2; operation ++ ) { 
                        
                        // always try to toggle an arc's existance, but only it's direction if it
                        // exists in the network.
                        if ( operation == 0 || tom.isArc(a,b) ) {            
                            double cost = lookahead(operation,a,b,k-1);
                            // If this is the best operation found so far remember it.
                            if ( cost < bestCost ) {
                                bestCost = cost;
                            }
                        }                        
                    }
                }
            }                
        }
        
        // reverse the initial mutation.  This puts the TOM into it's original orientation.
        mutate( currentOperation, currentA, currentB );
        
        return bestCost;
    } 
    
    /** 
     * In a single epoch we try all single mutations and take the one which lookahead()
     * tells us has the best prospects.
     */
    public double doEpoch()
    {    
        int bestA = -1;
        int bestB = -1;
        int bestOperation = -1;
        
        double baseNetworkCost = costNetwork( mmlModelLearner, false );
        double bestCost = baseNetworkCost;
        double cost;
        
        boolean networkUpdated = false;
        
        
        
        // do not mutate on first epoch.  This gives the initial model zero arcs.
        if (epoch != 0) {    
            
            
            for (int a = 0; a < tom.getNumNodes(); a++)
                for (int b = a+1; b < tom.getNumNodes(); b++)
                    for (int op = 0; op < 2; op++) {
                        if ( op == 0 || tom.isArc(a,b) ) {
                            cost = lookahead(op,a,b,maxLookahead);
                            
                            // if 2 costs are equal, resolve the tiebreak by checking which 
                            // cost is best with a reduced lookahead.  Without this cycles
                            // occur in the search space
                            if ( cost == bestCost ) {
                                //                 System.out.println(" -- Equal Best Model -- ");
                                
                                int tempLookahead = maxLookahead - 1;
                                while (tempLookahead >=0 ) {
                                    // Work out if the current or previous best has the best cost
                                    // with a reduced lookahead.
                                    double cost1 = lookahead( op,a,b,tempLookahead );
                                    double cost2 = lookahead( bestOperation,bestA,
                                                              bestB,tempLookahead );
                                    
                                    if ( cost1 < cost2 ) {
                                        bestCost = cost;
                                        bestA = a;
                                        bestB = b;
                                        bestOperation = op;
                                        networkUpdated = true;
                                        System.out.println(" --- New Best Model --- " + bestCost );
                                        break;
                                    }
                                    else if ( cost1 > cost2 ) {
                                        //                     System.out.print(" -- Worse Best Model -- ");
                                        break;                    
                                    }
                                    else if ( cost1 == cost2 ) {
                                        tempLookahead --;
                                        //                     System.out.println("Looking ahead with k = "+tempLookahead);
                                    }
                                }
                                
                            }
                            else if ( cost > bestCost ) {
                                //                 System.out.print(" -- Worse Best Model -- ");
                            }
                            else if (cost < bestCost) {
                                bestCost = cost;
                                bestA = a;
                                bestB = b;
                                bestOperation = op;
                                networkUpdated = true;
                                System.out.println(" --- New Best Model --- " + bestCost );
                            }
                        }
                    }        
        }
        // else if epoch == 0
        else {
            // bestCost = costNetwork() + structureCost;
            bestCost = costNetwork( mmlModelLearner, false );
            bestA = 0;
            bestB = 0;
            bestOperation = 0;
        }
        
        
        
        if ( networkUpdated || epoch == 0 ) {
            System.out.println("New best TOM found : epoch = " + epoch);
            
            // re-mutate the network to the best found so far.
            mutate( bestOperation,bestA,bestB );
            
            System.out.print(tom);
            System.out.println("Network Cost = " + costNetwork( mmlModelLearner,false) + "\n");
        }
        else {
            searchDone = true;
        }
        
        epoch++;
        return bestCost;
    }
    
    public boolean isFinished()
    {
        return (epoch == max || searchDone == true);
    }
    
    public double getPercentage()
    {
        return (epoch / (double)max);
    }
    
    
    /** Create a new TOM with no arcs present. */
    public GreedyLookaheadSearch( java.util.Random rand, Value.Vector data, int maxLookahead ) 
    {
        super( rand, data, SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner );
        
        this.maxLookahead = maxLookahead;
        
    }
    
}
