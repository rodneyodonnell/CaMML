//
// TOMTransformation for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell and Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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
