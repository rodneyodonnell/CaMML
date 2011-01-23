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

// File: UltraSimpleSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.core.search;

import cdms.core.*;
import cdms.plugin.search.*;
import camml.core.models.cpt.CPTLearner;

/** 
 * UltraSimpleSearch as the name suggests is a very basic search algorithm to find the TOM with the
 * lowest mml cost.  This cost is simply the sum of costs of each node.  Each node being a single
 * CPT.
 * No attemts is made to take into consideration the number of orderings in the network, or any
 * other factors which a more complete searhc would address. <br>
 * This search is intended to be a prototype and nothing more.
 */
public class UltraSimpleSearch implements Search.SearchObject
{
    /** vector containing the full data of the network */
    protected Value.Vector fullData;
    
    /** Value.Function to return a view of the full data */
    protected Value.Function dataView;
    
    /** The tom being worked on */
    protected TOM tom;
    
    /** cost of each individual node*/
    protected double[] nodeCost;
    protected double fullCost;
    
    /** Number of links coming into/out of node[i] */
    int[] arcsConnecting;
    
    /** This is the cost of the TOM without the nodes.  ie. Total ordering + arcs     */
    protected double structureCost;
    
    /** Random number generator */
    protected java.util.Random rand;
    
    
    protected TOM bestTom;
    protected double bestCost;
    protected String bestTomString;
    
    protected int epoch = 0;          // current epoch
    protected int max = 1000;          // total number of epochs
    
    //private Value.Vector data;
    
    public void reset()
    {
        epoch = 0;
    }
    
    /**
     * For each Node {
     *  Update Parents;
     *  If (parents were changed) {
     *   create a new Parent Vector;
     *   Use this parent vector to create a view into the full data vector;
     *   node[i].params = parameterize( x, view );
     *   node[i].cost = cost(x,view,params);
     *  }
     *  totalcost += node[i].cost;
     * }
     *  
     */
    public double costNetwork()
    {
        double totalCost = 0;
        
        // for each node
        for (int i = 0; i < tom.getNumNodes(); i++) {
            Node currentNode = tom.getNode(i);
            
            nodeCost[i] = currentNode.cost( CPTLearner.multinomialCPTLearner, fullData );
            
            totalCost += nodeCost[i];
        }
        return totalCost;
        
    }
    
    
    public double doEpoch()
    {    
        
        // do not mutate on first epoch.  This gives the initial model zero arcs.
        if (epoch != 0) {
            // Select two (different) nodes at random.
            int a = rand.nextInt( nodeCost.length );
            int b;
            do {
                b = rand.nextInt( nodeCost.length );
            } while (a == b);
            
            // Remove current arc.
            if ( tom.isArc(a,b) ) {
                tom.removeArc(a,b);
                arcsConnecting[a]--;
                arcsConnecting[b]--;
            }
            // Add an arc.  Make sure no more than Node.maxNumParents connect to any one node to
            // avoid Node.ExcessiveArcsException.
            else {
                if ( arcsConnecting[a] < tom.maxNumParents &&
                     arcsConnecting[b] < tom.maxNumParents ) {
                    tom.addArc(a,b);
                    arcsConnecting[a]++;
                    arcsConnecting[b]++;
                }
            }
            
            tom.randomOrder( rand );
        }
        
        double cost = costNetwork() + structureCost;
        
        if ( cost < bestCost ) {
            System.out.println("New best TOM found");
            bestCost = cost;
            System.out.print(tom);
            System.out.println("Best Cost = " + bestCost + "\n");
        }
        
        epoch++;
        return bestCost;
    }
    
    public boolean isFinished()
    {
        return (epoch == max);
    }
    
    public double getPercentage()
    {
        return (epoch / (double)max);
    }
    
    
    /** Create a new TOM with no arcs present. */
    public UltraSimpleSearch( java.util.Random rand, Value.Vector data ) 
    {
        this.rand = rand; // save random number generator.
        fullData = data;  // save the full data set for future use.
        // Function to create views into the data.
        dataView = (Value.Function)camml.core.library.CammlFN.view.apply( fullData );
        tom = new TOM( fullData ); // create the tom to search over
        nodeCost = new double[ tom.getNumNodes() ];//Allocate memory to store individual node costs.
        this.arcsConnecting = new int[ nodeCost.length ];
        bestCost = Double.POSITIVE_INFINITY;
        bestTom = null;
        
        structureCost = FN.LogFactorial.logFactorial( nodeCost.length ) + 
            Math.log( nodeCost.length * (nodeCost.length - 1) / 2.0);
        System.out.println("Simple Search structureCost = " + structureCost);
        
    }
    
}
