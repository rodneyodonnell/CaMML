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
// Prior over direct relationships
//

// File: DirectRelationPrior.java
// Author: rodo@dgs.monash.edu.aupackage camml.plugin.tomCoster;

package camml.plugin.tomCoster;

import camml.core.search.TOM;

/** Class containing various useful functions for dealing
 *  with our prior on connections between 2 nodes. */
public class DirectRelationPrior implements RelationPrior {
    public final int nodeI;
    public final int nodeJ;
    public final int numNodes;
        
    /** Types of direct relationships possible. <br>
     *  arcIJ = I 'direct parent of' J <br>
     *  noArcIJ = I 'before but not connected to' J
     * */
    private enum D { 
        arcIJ, arcJI,       // Directed arc between A and B
            noArcIJ, noArcJI,   // No direct connection between A,B
            }
    
    /** Prior on arc existence. */
    private double arcP;
    private boolean arcPFixed = false;
    
    /** Prior on arc direction. 1.0 implies I < J */
    private double dirP;
    private boolean dirPFixed = false;

    /** Prior on arc I->J existence. */
    private double arcIJP;
    private boolean arcIJFixed = false;
    
    /** Prior on arc J->I existence. */
    private double arcJIP;
    private boolean arcJIFixed = false;

    /** Direct probabilities, indexed by D.  Should sum to 1.0 */
    private final double pDirect[];
    
    /** logPDirect is -log transform of  pDirect */
    private final double logPDirect[];
        
    public double pArcIJ() { return pDirect[D.arcIJ.ordinal()]; }
    public double pArcJI() { return pDirect[D.arcJI.ordinal()]; }
    public double pNoArcIJ() { return pDirect[D.noArcIJ.ordinal()]; }
    public double pNoArcJI() { return pDirect[D.noArcJI.ordinal()]; }
    
    /** Initialise values to default prior. */
    public DirectRelationPrior( int nodeI, int nodeJ, int numNodes, double defaultArcP ) { 
        this.nodeI = nodeI;
        this.nodeJ = nodeJ;
        this.numNodes = numNodes;
        
        pDirect = new double[D.values().length];
        logPDirect = new double[D.values().length];
        this.dirP = 0.5;
        setArcP( defaultArcP );
    }    
    
    /** Update default priors */
    public void setArcP( double arcP ) {
        // Update default arcP, nothing changes if arcP is fixed.
        if (!arcPFixed) {
            this.arcP = arcP;
            updateProbs();
        }
    }
    
    /**  Reverse direction of operation. */
    public static String reverse( String s ) {
        if ("->".equals(s)) { return "<-"; }
        else if ("<-".equals(s)) { return "->"; }
        else if (">>".equals(s)) { return "<<"; }
        else if ("<<".equals(s)) { return ">>"; }
        else if ("=>".equals(s)) { return "<="; }
        else if ("<=".equals(s)) { return "=>"; }
        else if ("--".equals(s)) { return "--"; }
        else if ("==".equals(s)) { return "=="; }
        else {
            throw new RuntimeException("Unrecognised symbol : " + s);
        }
    }
    
    /** Set prior ov given type.  Valid types are : <br> 
     *  ">>" or "<<": temporal ordering <br>
     *  "->" or "<-": Directed arc <br>
     *  "--*        : Undirected arc <br>
     *  */
    public void setP( String type, double prob ) {
        // Flag an error
        boolean error = false;
        
        // Temporal link.
        if ( "<<".equals(type) ) {
            if (dirPFixed) {error = true; }
            dirP = prob;
            dirPFixed = true;
        }
        else if ( ">>".equals(type) ) { 
            if (dirPFixed) {error = true; }
            dirP = 1-prob; 
            dirPFixed = true;
        }
        // Direct Arc
        else if ( "->".equals(type) ) { 
            if (arcIJFixed) {error = true; }
            arcIJP = prob;
            arcIJFixed = true;            
        }
        else if ( "<-".equals(type) ) {    
            if (arcJIFixed) {error = true; }
            arcJIP = prob;
            arcJIFixed = true;
        }
        // Undirected arc
        else if ( "--" .equals(type) ) {
            if (arcPFixed) {error = true; }
            arcP = prob;
            arcPFixed = true;
        }
        // Unrecognised type
        else { 
            throw new RuntimeException("Unknown type " + type);
        }
        
        if (error) {
            throw new RuntimeException("Error specifying prior: " + 
                                       nodeI + type + nodeJ + " = " + prob +
                                       "\t possibly duplicate declaration");
        }
        
        updateProbs();
    }

    /** setP with operation reversed. setP2("->",.9) == setP("<-",.9), etc. */
    public void setP2( String type, double prob ) {
        setP( reverse(type), prob);
    }

    
    /** Recalculate probabilities for pDirect given fixed values. */
    private void updateProbs() {
        
        // shortcut to indexes ot pDirect
        int arcIJ = D.arcIJ.ordinal();       // i -> j
        int arcJI = D.arcJI.ordinal();       // j -> i
        int narcIJ = D.noArcIJ.ordinal();     // i .. j (preceeds, no arc)
        int narcJI = D.noArcJI.ordinal();    // j .. i (preceeds, no arc)

        // shortcut to pDirect;
        double p[] = pDirect;
        
        
        // if P(i->j) and P(j->i) fixed
        if ( arcIJFixed && arcJIFixed ) {
            p[arcIJ] = arcIJP;
            p[arcJI] = arcJIP;
            if ( arcPFixed && (arcP != p[arcIJ] + p[arcJI])) {
                throw new RuntimeException("Inconsistent priors. " +
                                           arcP + " != " + p[arcIJ] + " + " + p[arcJI] );
            }
            arcP = p[arcIJ] + p[arcJI];
            arcPFixed = true;
            
            if ( dirPFixed ) {
                p[narcIJ] = dirP - p[arcIJ];
                p[narcJI] = (1 - dirP) - p[arcJI];
            }
            else {
                p[narcIJ] = (1-arcP)/2;
                p[narcJI] = (1-arcP)/2;
            }
        }
        // if P(i->j) is fixed, but not P(j->i)
        else if ( arcIJFixed ) {
            p[arcIJ] = arcIJP;
            // if arcP is fixed, P(j->i) is implied. Use implied value and rerun. 
            if ( arcPFixed ) {                 
                p[arcJI] = arcJIP = arcP - p[arcIJ];
                arcJIFixed = true;
                updateProbs();
                return;
            }
            // if only P(i->j) and P(i < j) fixed
            else if ( dirPFixed ) {
                p[narcIJ] = dirP - p[arcIJ];
                p[arcJI] = (1-dirP) * arcP;
                p[narcJI] = (1-dirP) * (1-arcP);
            }
            // if only P(i->j) fixed
            else {
                // scale all other values by remaining prob.
                double mul =  (1 - p[arcIJ]) / (1 - dirP * arcP);
                p[arcJI] = arcP * (1-dirP) * mul;
                p[narcIJ] = (1-arcP) * dirP * mul;
                p[narcJI] = (1-arcP) * (1-dirP) * mul;
            }
        }
        // if P(j->i) is fixed, but not P(i->J)
        else if ( arcJIFixed ) {
            p[arcJI] = arcJIP;
            // if arcP is fixed, P(i->j) is implied. Use implied value and rerun. 
            if ( arcPFixed ) {
                p[arcIJ] = arcIJP = arcP - p[arcJI];
                arcIJFixed = true;
                updateProbs();
                return;
            }
            // if only P(i->j) and P(i < j) fixed
            else if ( dirPFixed ) {
                p[narcJI] = (1-dirP) - p[arcJI];
                p[arcIJ] = dirP * arcP;
                p[narcIJ] = dirP * (1-arcP);
            }
            // if only P(j->i) fixed
            else {
                // scale all other values by remaining prob.
                double mul =  (1 - p[arcJI]) / (1 - (1-dirP) * arcP);
                p[arcIJ] = arcP * dirP * mul;
                p[narcIJ] = (1-arcP) * dirP * mul;
                p[narcJI] = (1-arcP) * (1-dirP) * mul;
            }
        }
        // if neither P(i->j) or P(j->i) are fixed.
        else {
            p[arcIJ] = arcP * dirP;
            p[arcJI] = arcP * (1-dirP);
            p[narcIJ] = (1-arcP) * dirP;
            p[narcJI] = (1-arcP) * (1-dirP);
        }
        
        // Check all priors make sense.
        boolean error = false;
        double delta = 0.0000001; // Margin of error allowed in calculations.

        // If p is slightly above 1.0 or below 0.0 due to numeric errors,
        // assume that 1.0 or 0.0 was intended.
        for ( int i = 0; i < p.length ;i++) {
            if (p[i] < 0) {
                if (p[i] < -delta) {error=true;}
                else { p[i] = 0; }
            }
            else if (p[i] > 1) {
                if ( p[i] > 1 + delta) {error=true;}
                else { p[i] = 1; }
            }
        }
        if(error) { throw new RuntimeException("Probability out of range."+
                                               " p["+nodeI+"]["+nodeJ+"]\n" + toString());}
    
        // Ensure all prior constraints are met.
        if (arcIJFixed && Math.abs(p[arcIJ] - arcIJP) > delta) { error = true;}
        if (arcJIFixed && Math.abs(p[arcJI] - arcJIP) > delta) { error = true;}
        if (arcPFixed  && Math.abs(p[arcIJ] + p[arcJI] - arcP) > delta) { error = true; }
        if (dirPFixed  && Math.abs(p[arcIJ] + p[narcIJ] - dirP) > delta) {error = true; }
        if (error) { 
            throw new RuntimeException("Inconsistent priors["+nodeI+"]["+nodeJ+"]\n" + this);
        }
    
        // Log transform for easy access elsewhere.
        for ( int i = 0; i < p.length; i++) {
            logPDirect[i] = -Math.log(p[i]);
        }
    }
    
    /** Return probability of relationship between nodeI and nodeJ in the given tom. */
    public double relationProb( TOM tom ) {
        boolean isArc = tom.isArc(nodeI,nodeJ);
        boolean before = tom.before(nodeI,nodeJ);
        if ( isArc && before ) { return pDirect[D.arcIJ.ordinal()]; }
        else if ( isArc && !before ) { return pDirect[D.arcJI.ordinal()]; }
        else if ( !isArc && before ) { return pDirect[D.noArcIJ.ordinal()]; }
        else if ( !isArc && !before ) { return pDirect[D.noArcJI.ordinal()]; }
        else { throw new RuntimeException("Unreachable state.");}
    }
    
    /** Return log probability of relationship betwee nodeI and nodeJ in the given tom.. */
    public double relationCost( TOM tom ){
        boolean isArc = tom.isArc(nodeI,nodeJ);
        boolean before = tom.before(nodeI,nodeJ);
        if ( isArc && before ) { return logPDirect[D.arcIJ.ordinal()]; }
        else if ( isArc && !before ) { return logPDirect[D.arcJI.ordinal()]; }
        else if ( !isArc && before ) { return logPDirect[D.noArcIJ.ordinal()]; }
        else if ( !isArc && !before ) { return logPDirect[D.noArcJI.ordinal()]; }
        else { throw new RuntimeException("Unreachable state.");}
    }

    /** Return P(I<J) */
    public double pBefore() {
        return pDirect[D.arcIJ.ordinal()] + pDirect[D.noArcIJ.ordinal()];
    }

    /** Return P(I--J) */
    public double pArc() {
        return pDirect[D.arcIJ.ordinal()] + pDirect[D.arcJI.ordinal()];
    }

    /** Return human readable version of prior */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for(D x : D.values()) {
            sb.append( x + "\t" + pDirect[x.ordinal()] + "\n" );
        }
        return sb.toString();
    }
}
