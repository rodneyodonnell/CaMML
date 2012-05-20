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
// SkeletalChange for CaMML
//

// File: TemporalChange.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;


/**
 * A skeletal change is a stochastic transformation of a TOM in TOMSpace. It either adds or deletes
 * an arc between two nodes.
 */
public class SkeletalChange extends TOMTransformation {
    /**
     * cost to add arc = -log(arcProb) - -log(1.0-arcProb)
     */
    double arcCost;

    /**
     * Constructor
     */
    public SkeletalChange(java.util.Random generator, double arcProb, CaseInfo caseInfo, double temperature) {
        super(generator, caseInfo, temperature);
        arcCost = Math.log(arcProb / (1.0 - arcProb));
    }

    /**
     * Child changed by lastmutation
     */
    private int childChanged;

    /**
     * changed[] is allocated to save reallocating an array every time getNodesChanged() is called
     */
    private final int[] changed = new int[1];

    /**
     * Return array of changes nodes <br>
     * NOTE: Values within returned array are volatile and will often be changed is transform() is called again.
     */
    public int[] getNodesChanged() {
        changed[0] = childChanged;
        return changed;
    }

    /**
     * Choose two nodes and attempt to toggle the existence of an arc connecting them.
     * Arc direction is determined by the total ordering of the TOM.
     */
    public boolean transform(TOM tom) {
        // choose node.
        int pos1 = 0, pos2 = 0;

        // This could probably be more efficient, but oldCamml does it this way ...
        while (pos1 == pos2) {
            pos1 = (int) (rand.nextDouble() * tom.getNumNodes());
            pos2 = (int) (rand.nextDouble() * tom.getNumNodes());
        }

        // Ensure i < j
        final int i = tom.nodeAt(Math.min(pos1, pos2));
        final int j = tom.nodeAt(Math.max(pos1, pos2));

        childChanged = j;

        final Node nodeJ = tom.getNode(j);
        // If adding an extra parent would violate maxParents
        if (nodeJ.getParent().length == tom.coreTOM.getMaxNumParents() && !tom.isArc(i, j)) {
            return false;
        }

        final double oldCostJ = caseInfo.nodeCache.getMMLCost(nodeJ);
        final double costToToggleArc = caseInfo.tomCoster.costToToggleArc(tom, i, j);

        if (tom.isArc(i, j)) {
            tom.removeArc(i, j);
        } else {
            tom.addArc(i, j);
        }

        // calculate new cost        
        final double newCostJ = caseInfo.nodeCache.getMMLCost(nodeJ);

        if (accept(newCostJ - oldCostJ + costToToggleArc)) {
            if (caseInfo.updateArcWeights) {
                if (tom.isArc(i, j)) {
                    caseInfo.arcWeights[j][i] -= caseInfo.totalWeight;
                } else {
                    caseInfo.arcWeights[j][i] += caseInfo.totalWeight;
                }
            }
            return true;
        } else {
            // restore.
            if (tom.isArc(i, j)) {
                tom.removeArc(i, j);
            } else {
                tom.addArc(i, j);
            }

            return false;
        }
    }

}
