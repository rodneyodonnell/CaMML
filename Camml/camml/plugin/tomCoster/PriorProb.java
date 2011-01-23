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
// Custom Structure containing variious prior probabilities.
//


package camml.plugin.tomCoster;

import java.io.Serializable;
import java.util.Random;

import camml.core.library.WallaceRandom;
import camml.core.search.TOM;

/** 
 * Custom structure to contain various prior probabilities. 
 * 
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.1 $ $Date: 2006/09/03 10:35:53 $ 
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tomCoster/PriorProb.java,v $
 * */
public class PriorProb implements Serializable {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 4543003379114820895L;
    
    
    /////////////////////////////////////
    // Elements of PriorProb structure //
    /////////////////////////////////////
    
    /** Probability A is an ancestor of B (or vice versa). <br>
     *  pIsAncestor = P(A => B) = P(B => A) */
    public double pIsAncestor = -1;
    
    /** Probability A and B are correlated <br>
     *  pIsCorrelated = P(A => B) + P(B => A) + P(A <=> B) */
    public double pIsCorrelated = -1;
    
    /** Probability A and B have a common cause <br>
     *  pIsCommonCause = P(A <=> B) */
    public double pIsCommonCause = -1;
    
    
    /** Probability A is an parent of B (or vice versa). <br>
     *  pIsParent = P(A -> B) = P(B -> A) */
    public double pIsParent = -1;

    /** Probability A and B are directly connected <br>
     *  pIsCorrelated = P(A -> B) + P(B -> A) */
    public double pIsConnected = -1;
    
    /** Probability A preceeds B in the TOMs total ordering. <br>
     *  pIsBefore = P(A < B) = P(B < A)*/    
    public double pIsBefore = -1;
    
    //////////////////////////////////
    // cache for calcIndirectPriors //
    //////////////////////////////////
    
    /** lastArcP, lastNumNodes and lastCalc used to 'cache' last call to calcIndirectPrior*/
    private static double lastArcP = -1;
    /** lastArcP, lastNumNodes and lastCalc used to 'cache' last call to calcIndirectPrior*/
    private static int lastNumNodes = -1;
    /** lastArcP, lastNumNodes and lastCalc used to 'cache' last call to calcIndirectPrior*/
    private static PriorProb lastCalc = null;
    
    /** Calculate prior probability of various forms of arc connection
     *  given arcP (ie. p(a--b)) and a given number of nodes.
     *  This is too hard to do perfectly, so a sampling process is used. <br>
     *  
     *  When called twice with identical parameters, the same structure is returned. */
    public static PriorProb calcPrior( double arcP, int numNodes ) {
        if ( arcP == lastArcP && numNodes == lastNumNodes) { return lastCalc; }
        
        Random rand = new WallaceRandom( new int[] {123,456} );
        
        // Create a TOM using a fake dataset.
        TOM tom = new TOM(numNodes);
        tom.setMaxNumParents(numNodes);
        
        
        // Count #connected, #ancestors and #commonCause.
        // Everything else can be inferred.
        int nConnected = 0, nAncestor = 0, nCommonCause = 0;
        
        // sample n-1 TOMs, record frequency of node relationships.
        int n = 10000;
        for (int i = 1; i < n; i++ ) {
            tom.clearArcs();
            tom.randomOrder(rand);
            tom.randomArcs( rand, arcP );
        
            if ( tom.isArc(0,1) || tom.isArc(1,0)) { 
                nConnected ++;
            }
            else if ( tom.isAncestor(0,1) || tom.isAncestor(1,0) ) {
                nAncestor ++;
            }
            else if ( tom.isCorrelated(0,1) ) {
                nCommonCause ++;
            }
        }
        

        // Use MML estimate for p.pIsXXX
        PriorProb p = new PriorProb();
        // Use nConencted2 so we never have cases where pIsAncestor < pIsParent, etc.
        double nConnected2 = (n-1)*arcP;
        // n2 is a fudge factor so sum = 1.
        double n2 = n - nConnected + nConnected2;
        p.pIsAncestor = (nConnected2 + nAncestor + 0.5) / n2 / 2;
        p.pIsCorrelated = (nConnected2 + nAncestor + nCommonCause + 0.5) / n2;
        p.pIsCommonCause = (nCommonCause + 0.5) / n2;
        p.pIsParent = arcP * .5;
        p.pIsConnected = arcP;
        p.pIsBefore = 0.5;
        
        // Cache 'p' incase next call has identical parameters.
        lastArcP = arcP;
        lastNumNodes = numNodes;
        lastCalc = p;
        
        return p;
    }

    
    public String toString() {
        return 
            "P(=>) = " + pIsAncestor + "\n" +
            "P(==) = " + pIsCorrelated + "\n" +
            "P(<=>)= " + pIsCommonCause + "\n" +
            "P(->) = " + pIsParent + "\n" +
            "P(--) = " + pIsConnected + "\n" +
            "P(>>) = " + pIsBefore;
    }
}
