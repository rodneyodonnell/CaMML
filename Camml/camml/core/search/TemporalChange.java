/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lucas Hope, Lloyd Allison, Kevin Korb
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
// TemporalChange for CaMML
//

// File: TemporalChange.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;

/**
   A temporal change is a stochastic transformation of a TOM in TOMSpace. It attempts to reverse
   the total order of a node j and j - 1.
*/
public class TemporalChange extends TOMTransformation
{
    /** Cost to add an arc : log(arcProb) - log(1-arcProb) */
    double arcCost;
    
    /** Constructor */
    public TemporalChange(java.util.Random generator, double arcProb,
                          CaseInfo caseInfo, double temperature )
    {    
        super( generator, caseInfo, temperature );
        
        arcCost = Math.log(arcProb / (1.0 - arcProb));
    }
    
    /** last parent modified */
    private int lastParent = 0;
    
    /** last child modified*/
    private int lastChild = 0;
    
    /** were the last parent-child pair connected? */
    private boolean lastConnected = false;
    
    /** Pre-allocated return value for getNodeChanged */
    private int[] parentChangeArray = new int[2];
    /** Pre-allocated return value for getNodeChanged*/
    private int[] empty = new int[0];
    
    /** Return array of changes nodes <br> 
     *  NOTE: Values within returned array are volatile and will often be changed is transform()
     *        is called again.  */
    public int[] getNodesChanged()
    {
        parentChangeArray[0] = lastParent;
        parentChangeArray[1] = lastChild;
        if ( lastConnected )
            return parentChangeArray;
        else
            return empty;
    }
    
    /**
     * Choose a pair of consecutive nodes and try to swap their position in the Total Ordering.
     */
    public boolean transform(TOM tom, double ljp) {
        // choose pair to attempt swap on
        final int orderI = (int)(rand.nextDouble() * (tom.getNumNodes() - 1));
        final int orderJ = orderI + 1;
        
        final Node nodeI = tom.getNode(tom.nodeAt( orderI ));
        final Node nodeJ = tom.getNode(tom.nodeAt( orderJ ));
        
        lastParent = tom.nodeAt( orderI );
        lastChild = tom.nodeAt( orderJ );
        
        // If there is a link between nodeI and nodeJ
        if( tom.isArc( nodeI.var, nodeJ.var ) ) {
            lastConnected = true;
            
            // If maxNumParents has been reached we fail.
            if ( nodeI.getNumParents() == tom.maxNumParents ) return false;
            
            // record old node costs.
            double oldCostI = caseInfo.nodeCache.getMMLCost( nodeI );
            double oldCostJ = caseInfo.nodeCache.getMMLCost( nodeJ ); 
            
            // Cost to swap nodes in total ordering
            double costToSwapOrder = 
                caseInfo.tomCoster.costToSwapOrder( tom, nodeI.var, nodeJ.var );
            
            oldCost = 0;
            
            // swap ordering
            tom.swapOrder( nodeI.var, nodeJ.var, true );
            
            // calculate new costs        
            double newCostI = caseInfo.nodeCache.getMMLCost( nodeI );
            double newCostJ = caseInfo.nodeCache.getMMLCost( nodeJ );
            
            cost = newCostI + newCostJ - oldCostI - oldCostJ + costToSwapOrder;
            
            if(accept()){
                if (caseInfo.updateArcWeights) {
                    double w = caseInfo.totalWeight;
                    // replace i -> j with j -> i
                    caseInfo.arcWeights[nodeJ.var][nodeI.var] += w;  // i -> j
                    caseInfo.arcWeights[nodeI.var][nodeJ.var] -= w;  // j -> i
                }
                return true; 
            }
            else {
                // restore.
                tom.swapOrder( nodeI.var, nodeJ.var, true );
                return false;
            }
        }
        else { 
            
            if ( caseInfo.regression ) {
                //    if unconnected, we always succeed.
                accepted ++;
                lastConnected = false;
                tom.swapOrder( nodeI.var, nodeJ.var, true );
                cost = 0;
                return true;
            }

            // If a non uniform prior over TOM space is used a change in the total ordering
            // of unconnected nodes may have an effect on the total cost.
            oldCost = 0;
            cost = caseInfo.tomCoster.costToSwapOrder( tom, nodeI.var, nodeJ.var );
            if ( accept() ) {
                accepted ++;
                lastConnected = false;
                tom.swapOrder( nodeI.var, nodeJ.var, true );
                return true;            
            } else {
                return false;
            }
        }
    }
    
}
