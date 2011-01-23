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
// TOMTransformation for CaMML
//

// File: TOMTransformation.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;

/**
   A TOMTransformation is a stochastic transformation of a TOM in TOMSpace.
*/
public abstract class TOMTransformation
{
    /** caseInfo contains various important values. */
    final protected CaseInfo caseInfo;
    
    /** Random number generator */
    final protected java.util.Random rand;
    
    /** Final cost from previous transformation */
    protected double cost;
    
    /** Initial cost from previous transformation */
    protected double oldCost;
    
    /** The nodes who's parents were changed by the last mutation. */
    public abstract int[] getNodesChanged();
    
    /** Temperature sampling is performed at.  
     *  Higher temperature increases chance of mutation acceptence */
    final protected double temperature;
    
    /** Constructor. */
    public TOMTransformation(java.util.Random rand, CaseInfo caseInfo, double temperature )
    {
        this.rand = rand;
        this.caseInfo = caseInfo;
        this.temperature = temperature;
    }
    
    /** Perform mutation, return true if mutation was successful */
    public abstract boolean transform(TOM tom, double ljp);
    
    /** Accesor for cost */
    public double getLastCost()
    {
        return cost;
    }
    
    
    /**
     * if ( Math.log(generator.nextDouble()) < (oldCost - cost) / temperature ), accept = true.
     */
    protected final boolean accept() {
        
        double diff = cost - oldCost;
        final boolean result;
        
        // When doing regression testing (and attempting to keep random generator in line)
        //  differences are less likely to occur if we always generate a number.    
        // We also check for INFINITY as oldCamml does not increment generator in these cases.
        if ( caseInfo.regression ) {
            if ( diff != Double.POSITIVE_INFINITY ) { 
                result = ( - Math.log(rand.nextDouble()) > diff / temperature ); 
            }
            else { result = false; }
        }
        else {
            if ( diff <= 0.0 ) { result = true; }
            else if ( caseInfo.safeMode ) { result = false; }
            else { result = ( - Math.log(rand.nextDouble()) > diff / temperature ); }
        }
        
        // update stats
        if (result == true) accepted++; else rejected++;
        return result;
    }
    int accepted = 0; int rejected = 0;
    
    /** Toggle existence of arc 2->1 and 3->1 */
    protected static void doubleMutate( TOM tom, int node1, int node2, int node3 ) 
    {
        // NOTE: It is important to remove arcs first to avoid going over 
        //       node.maxNumArcs.
        boolean isArc13 = tom.isArc( node1, node3 );
        boolean isArc23 = tom.isArc( node2, node3 );
        
        // remove excess arcs
        if ( isArc13 ) { tom.removeArc( node1, node3 ); }
        if ( isArc23 ) { tom.removeArc( node2, node3 ); }
        
        // add new arcs.
        if ( !isArc13 ) { tom.addArc( node1, node3 ); }
        if ( !isArc23 ) { tom.addArc( node2, node3 ); }
    }
    
}
