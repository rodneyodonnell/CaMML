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

package camml.core.search;

/** Interface class for a function to clean TOMs */
public interface TOMCleaner
{
    public static class StandardTOMCleaner implements TOMCleaner
    {        
        public static StandardTOMCleaner tomCleaner = new StandardTOMCleaner();
        private StandardTOMCleaner() {}
        public void cleanTOM(TOM tom)
        {
            // loop through nodes cleaning each in turn.
            for ( int i = 0; i < tom.getNumNodes(); i++ ) {
                int nodeI = tom.nodeAt(i);
                int[] dirtyParent = tom.node[nodeI].parent;
                
                double oldCost = tom.caseInfo.nodeCache.getMMLCost( tom.node[nodeI] );
                for (int j = dirtyParent.length-1; j >= 0; j--) {
                    int nodeJ = dirtyParent[j];
                    double structureDiff = tom.caseInfo.tomCoster.costToToggleArc(tom,nodeI,nodeJ);                
                    tom.removeArc(nodeI,nodeJ);
                    double newCost = tom.caseInfo.nodeCache.getMMLCost(tom.node[nodeI]);
                    if ( newCost > oldCost - structureDiff) {
                        tom.addArc(nodeI,nodeJ);
                    }
                    else {
                        oldCost = newCost;
                    }
                }
            }            
        }
    }

    /** Don't perform any cleaning  */
    public static class NoCleanTOMCleaner implements TOMCleaner
    {        
        public static NoCleanTOMCleaner tomCleaner = new NoCleanTOMCleaner();
        private NoCleanTOMCleaner() {}
        public void cleanTOM(TOM tom) {}
    }

    /** remove all arcs which are not a parent of the specified target node */
    public static class TargetOnlyTOMCleaner implements TOMCleaner
    {        
        int target;
        public TargetOnlyTOMCleaner(int target) {this.target = target;}
        public void cleanTOM(TOM tom) {
            // loop through nodes cleaning each in turn.
            for ( int nodeI = 0; nodeI < tom.getNumNodes(); nodeI++ ) {
                if (nodeI != target) {                
                    int[] dirtyParent = tom.node[nodeI].parent;                    
                                
                    for (int j = 0; j < dirtyParent.length; j++) {
                        int nodeJ = dirtyParent[j];
                        tom.removeArc(nodeI,nodeJ);
                    }
                }
            }            
        }
    }

    /** Clean away all nodes not in the markov blanket of the specified variable */
    public static class MarkovBlanketTOMCleaner implements TOMCleaner
    {        
        int target;
        public MarkovBlanketTOMCleaner(int target) {this.target = target;}
        public void cleanTOM(TOM tom) {
            // loop through nodes cleaning each in turn.
            for ( int nodeI = 0; nodeI < tom.getNumNodes(); nodeI++ ) {
                if (nodeI != target) {                
                    int[] dirtyParent = tom.node[nodeI].parent;                    
                    boolean childOfTarget = false;
                    for (int j = 0; j < dirtyParent.length; j++) {
                        if (dirtyParent[j] == target) {
                            childOfTarget = true;
                            break;
                        }
                    }
                    if (childOfTarget) {continue;}
                    
                    for (int j = 0; j < dirtyParent.length; j++) {
                        int nodeJ = dirtyParent[j];
                        tom.removeArc(nodeI,nodeJ);
                    }
                }
            }            
        }
    }

    void cleanTOM(TOM tom);
}
