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
// ParentSwapChange for CaMML
//

// File: ParentSwapChange.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;

/**
   A skeletal change is a stochastic transformation of a TOM in TOMSpace. It either adds or deletes
   an arc between two nodes.
*/
public class ParentSwapChange extends TOMTransformation
{
    /** Cost to add arc */
    double arcCost;
    
    /** Constructor */
    public ParentSwapChange(java.util.Random generator, double arcProb,
                            CaseInfo caseInfo, double temperature )
    {
        super( generator,caseInfo, temperature );
        arcCost = Math.log(arcProb / (1.0 - arcProb));
    }
    
    /** Pre-allocated return values for getNodesChanged() */
    private int[] nodesChanged = new int[1];
    
    /** Pre-allocated value to pass to TOMCoster.toggleArcs()*/
    private int[][] nodesToggled = new int[2][2];
    
    /** Return array of changes nodes <br> 
     *  NOTE: Values within returned array are volatile and will often be changed is transform()
     *        is called again.  */
    public int[] getNodesChanged() { return nodesChanged; }
    
    public boolean transform(TOM tom, double ljp) {
        
        // At least 3 nodes are required to do a double skeletal change.
        if (tom.getNumNodes() < 3) {
            return false;
        }
        
        //    Choose a random node in order 2...(NumVariables-1) 
        //    Bias the choice towards late orders, because these have more non-parents
        double moveran = rand.nextDouble();
        moveran = 1.0 - moveran * moveran;
        
        // get the jth node in the total ordering.
        int nj = (int)( (tom.getNumNodes() - 2) * moveran );
        
        /*Fix to avoid numerical precision issue: When moveran is sufficiently small, moveran * moveran = 0
         * (i.e. the square of the random number can be too small to represent with a double)
         * As a result, maybe one in a million (or less) calls to this function will result in an array index error */
        while( nj >= tom.getNumNodes() - 2 ){ 
        	double r = rand.nextDouble();
        	nj = (int)( (tom.getNumNodes()-2) * (1.0 - r*r ) );
        }
        
        Node nodeJ = tom.getNode( tom.nodeAt(nj+2) );
        int numParents = nodeJ.parent.length;
        
        
        // If there are zero parents or all parents, we cannot do a swap.
        // NOTE: should this read (numParents >= tom.nodeAt(j))?  
        // Written here (and in the original camml code) we require at least 3 non-parents before
        //  j in the total order.  This seems strange.
        if ( (numParents == 0) || (numParents >= nj) ) {
            return false;
        }
        
        // Randomly select a parent from nodeJ
        // int ni = nodeJ.parent[ generator.nextInt(numParents) ];
        Node nodeI = tom.getNode( nodeJ.parent[ (int)(rand.nextDouble() * numParents) ] );
        
        // Choose a non-parent which appears before nodeJ in the total ordering.
        int temp = (int)(rand.nextDouble() * ( nj - numParents ));
        int nk = 0;
        for ( int i = 0; i <= nj; i++ ) {
            if ( !tom.isArc(tom.nodeAt(i), nodeJ.var) ) {
                if ( temp == 0 ) {
                    nk = i;
                    break;
                }
                temp --;
            }
        }
        Node nodeK = tom.getNode( tom.nodeAt(nk) );
        
        // At this time ni,nj,nk, nodeI, nodeJ, nodeK should all be set.
        // n{i,j,k} = position of total ordering of Node node{I,J,K}
        // An arc should exist from nodeI -> nodeJ
        // No arc should exist between nodeJ and nodeK
        // in the total ordering j < i, j < k
        
        
        // The cost of node1 and node2 cannot be changed (as thier parents remain constant).
        // The modification of the parents of node3 means that it may change.
        // so we must find it's original cost before changing it.    
        int[] originalParents = nodeJ.parent;
        double oldJCost = caseInfo.nodeCache.getMMLCost( nodeJ );
        
        // Links I--J & K--J are toggled.
        nodesToggled[0][0] = nodeJ.var;
        nodesToggled[0][1] = nodeJ.var;
        nodesToggled[1][0] = nodeI.var;
        nodesToggled[1][1] = nodeK.var;
        
        // Calculate change in TOM (structure) cost caused by toggle.
        double toggleCost = 
            caseInfo.tomCoster.costToToggleArcs(tom,nodesToggled[0],nodesToggled[1]);

        
        nodesChanged[0] = nodeJ.var;
        
        // toggle node1 -> node3 and node2 -> node3
        // doubleMutate( tom, tom.nodeAt(nj), tom.nodeAt(ni), tom.nodeAt(nk));
        doubleMutate( tom, nodeI.var, nodeK.var, nodeJ.var );
        
        // update the parents of node3 and find the cost.
        double newJCost = caseInfo.nodeCache.getMMLCost( nodeJ );
        
        
        // Calculate the new cost.  As the number of arcs remains constant, we do not have to
        // take arcs into account.
        oldCost = 0;
        cost = newJCost - oldJCost + toggleCost;
        
        if (accept()) {
            if (caseInfo.updateArcWeights){
                int childVar = nodeJ.var;
                int parentVar1 = nodeK.var;
                int parentVar2 = nodeI.var;
                
                if(tom.isArc(childVar,parentVar1)) {
                    caseInfo.arcWeights[childVar][parentVar1] -= caseInfo.totalWeight;                
                }                
                else {
                    caseInfo.arcWeights[childVar][parentVar1] += caseInfo.totalWeight;
                }

                if(tom.isArc(childVar,parentVar2)) {
                    caseInfo.arcWeights[childVar][parentVar2] -= caseInfo.totalWeight;                
                }                
                else {
                    caseInfo.arcWeights[childVar][parentVar2] += caseInfo.totalWeight;
                }
            }

            return true;
        }
        else {
            // toggle node1 -> node3 and node2 -> node3 back to their original state.
            doubleMutate( tom, nodeI.var, nodeK.var, nodeJ.var );
            nodeJ.parent = originalParents;
            return false;
        }
    }    
}


