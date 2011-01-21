//
// Prior over indirect relationships
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: DirectRelationPrior.java
// Author: rodo@dgs.monash.edu.aupackage camml.plugin.tomCoster;

package camml.plugin.tomCoster;

import java.util.Arrays;

import camml.core.search.TOM;

/**
 * IndirectRelationPrior defines a prior over the indirect relationships
 * in terms of Ancestor, Common Cause and Uncorrelated relationships. 
 * This is used as part of ExpertElicitedTOMCoster
 * Direct relationships are handled by DirectRelationship 
 * 
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.2 $ $Date: 2006/09/03 13:45:04 $ 
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tomCoster/IndirectRelationPrior.java,v $
 */
public class IndirectRelationPrior implements RelationPrior {
    /** nodeI and nodeJ are the pair of nodes prior is defined for */
    public final int nodeI;

    /** nodeI and nodeJ are the pair of nodes prior is defined for */
    public final int nodeJ;

    /** Probability of I => J, I 'Ancestor of' J */
    private double ancestorIJP;
    private boolean ancestorIJPFixed;
    
    /** Probability of I => J, J 'Ancestor of' I */
    private double ancestorJIP;
    private boolean ancestorJIPFixed;

    /** Probability of I <=> J, 
     * I and J share a common cause, but neither is an ancestor of the other. */
    private double commonCauseP;
    private boolean commonCausePFixed;
    
    /** Probability of I and J uncorrelated */
    private double uncorrelatedP;
    private boolean uncorrelatedPFixed;
        
    /** Prior connection probability */
    private PriorProb priorProb;
    
    /** True is setP has been called. */
    boolean priorSet = false;
    
    /** Initialise values to default prior. */
    public IndirectRelationPrior( int nodeI, int nodeJ, int numNodes, double defaultArcP ) { 
        this.nodeI = nodeI;
        this.nodeJ = nodeJ;
        this.numNodes = numNodes;
        setArcP( defaultArcP );
    }    
    
    /** Number of nodes in TOM */
    final int numNodes;
    
    /** Update default priors. */
    public void setArcP( double arcP ) {
        this.priorProb = PriorProb.calcPrior( arcP, numNodes );
        
        updateProbs();        
    }
    
    public String reverse( String s ) {
        if ("=>".equals(s)) { return "<="; }
        else if ("<=".equals(s)) { return "=>"; }
        else if ("==".equals(s)) { return "=="; }
        else {
            throw new RuntimeException("Unrecognised symbol : " + s);
        }
    }
    
    /** Set prior ov given type.  Valid types are : <br> 
     *  "=>" or "<=": Ancestor of <br>
     *  "=="        : Correlated with <br>
     *  @return true if type recognised, else return false.
     *  */
    public void setP( String type, double prob ) {
        priorSet = true;
        
        // Flag an error
        boolean error = false;
        
        // Ancestor
        if ( "=>".equals(type) ) { 
            if (ancestorIJPFixed) {error = true; }
            ancestorIJP = prob;
            ancestorIJPFixed = true;            
        }
        else if ( "<=".equals(type) ) {    
            if (ancestorJIPFixed) {error = true; }
            ancestorJIP = prob;
            ancestorJIPFixed = true;            
        }
        // Correlation
        else if ( "==" .equals(type) ) {
            if (uncorrelatedPFixed) {error = true; }
            uncorrelatedP = 1-prob;
            uncorrelatedPFixed = true;
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
        
        // Portion of post-expert prior space fixed.
        double fixed = 0;
        
        // Portion of pre-expert prior space unused.
        double preExpert = 0;
        
        // Sum portions which are fixed or unused.
        if ( ancestorIJPFixed ) { fixed += ancestorIJP; }
        else { preExpert += priorProb.pIsAncestor; }        
        if ( ancestorJIPFixed ) { fixed += ancestorJIP; }
        else { preExpert += priorProb.pIsAncestor; }        
        if ( uncorrelatedPFixed ) { fixed += uncorrelatedP; }
        else { preExpert += (1-priorProb.pIsCorrelated); }        
        if ( commonCausePFixed ) { fixed += commonCauseP; }
        else { preExpert += priorProb.pIsCommonCause; }
        
        // Set values which were not previously fixed.
        double norm = (1-fixed)/preExpert; 
        if ( !ancestorJIPFixed ) { ancestorJIP = priorProb.pIsAncestor * norm; }
        if ( !ancestorIJPFixed ) { ancestorIJP = priorProb.pIsAncestor * norm; }
        if ( !uncorrelatedPFixed ) { uncorrelatedP = (1-priorProb.pIsCorrelated) * norm; };
        if ( !commonCausePFixed ) { commonCauseP = priorProb.pIsCommonCause * norm; }

        
        // Check all priors make sense.
        boolean error = false;
        double delta = 0.0000001; // Margin of error allowed in calculations.

        
        // If p is slightly above 1.0 or below 0.0 due to numeric errors,
        // assume that 1.0 or 0.0 was intended.
        double p[] = new double[] { ancestorIJP, ancestorJIP, commonCauseP, uncorrelatedP };
        double sum = 0;
        for ( int i = 0; i < p.length ;i++) {
            if (p[i] < 0) {
                if (p[i] < -delta) {error=true;}
                else { p[i] = 0; }
            }
            else if (p[i] > 1) {
                if ( p[i] > 1 + delta) {error=true;}
                else { p[i] = 1; }
            }
            sum += p[i];
        }
        if(error) { throw new RuntimeException("Probability out of range."+
                                               " p["+nodeI+"]["+nodeJ+"]\n" + toString());}
        if (Math.abs(sum - 1.0) > delta ) {
            throw new RuntimeException( "probability sum != 1 : " + sum + '\t' + Arrays.toString(p) );
        }
        
        // convert from p[] back to original names.
        ancestorIJP = p[0];
        ancestorJIP = p[1];
        commonCauseP = p[2];
        uncorrelatedP = p[3];
    }
    
    /** Return probability of relationship betwee nodeI and nodeJ in the given tom. */
    public double relationProb( TOM tom ) {
        if ( tom.isAncestor(nodeI,nodeJ) ) { return ancestorIJP; }
        else if ( tom.isAncestor(nodeJ,nodeI) ) { return ancestorJIP; }
        else if ( tom.isCorrelated(nodeI,nodeJ)) { return commonCauseP; }
        else { return uncorrelatedP; }
    }
    
    /** Return log probability of relationship betwee nodeI and nodeJ in the given tom.. */
    public double relationCost( TOM tom ){
        return -Math.log( relationProb(tom) );
    }

    /** Return human readable version of prior */
    public String toString() {
        return 
            "P("+nodeI+"  => "+nodeJ+") = " + ancestorIJP + "\n" +
            "P("+nodeJ+"  => "+nodeI+") = " + ancestorJIP + "\n" +
            "P("+nodeI+" <=> "+nodeJ+") = " + commonCauseP + "\n" +
            "P("+nodeI+" =/= "+nodeJ+") = " + uncorrelatedP;
    }
}
