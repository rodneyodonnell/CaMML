//
// SkeletalChange for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell and Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TemporalChange.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;


/**
 A skeletal change is a stochastic transformation of a TOM in TOMSpace. It either adds or deletes
 an arc between two nodes.
 */
public class SkeletalChange extends TOMTransformation
{
	/** cost to add arc = -log(arcProb) - -log(1.0-arcProb)*/
	double arcCost;
	
	/** Constructor */
	public SkeletalChange(java.util.Random generator,  double arcProb,
			CaseInfo caseInfo, double temperature )
	{
		super( generator,caseInfo, temperature );
		arcCost = Math.log(arcProb / (1.0 - arcProb));
	}
	
	/** Child changed by lastmutation */
	private int childChanged;
	
	/** Parnt involved in last mutation*/    
	//private int parentChanged;
	
	/** changed[] is allocated to save reallocating an array every time 
	 * getNodesChanged() is called */
	private final int[] changed = new int[1];
	
	/** Return array of changes nodes <br> 
	 *  NOTE: Values within returned array are volatile and will often be changed is transform()
	 *        is called again.  */
	public int[] getNodesChanged() { changed[0] = childChanged; return changed; }
	
	/** Choose two nodes and attempt to toggle the existence of an arc connecting them.
	 *  Arc direction is determined by the total ordering of the TOM. */
	public boolean transform(TOM tom, double ljp) {
		// choose node.
		int i = 0, j = 0;
		
		// This could probably be more efficient, but oldCamml does it this way ...
		while ( i == j ) {
			i = (int)(rand.nextDouble() * tom.getNumNodes() );
			j = (int)(rand.nextDouble() * tom.getNumNodes() );
		}
		
		// Ensure i < j
		if ( i > j ) {
			int temp = i; i = j; j = temp;
		}
		
		i = tom.nodeAt(i);
		j = tom.nodeAt(j);
		
		//parentChanged = i;
		childChanged = j;
		
		//Node nodeI = tom.getNode(i);
		Node nodeJ = tom.getNode(j);
		// If adding an extra parent would violate maxParents
		if ( nodeJ.parent.length == tom.maxNumParents && !tom.isArc(i,j) ) {
			return false;
		}
		//Value.Vector data = tom.getData();
		
		// record old node j.
		int[] oldParentsJ = nodeJ.parent;
		
		double oldCostJ = caseInfo.nodeCache.getMMLCost( nodeJ );
		final double costToToggleArc = caseInfo.tomCoster.costToToggleArc(tom,i,j);
		
		if(tom.isArc(i, j)) {
			tom.removeArc(i, j);			
		}
		else {	    
			tom.addArc(i, j); 
		}
		
		// calculate new cost	    
		double newCostJ = caseInfo.nodeCache.getMMLCost( nodeJ );
		

		oldCost = 0;
		cost = newCostJ - oldCostJ + costToToggleArc;

		if(accept()) { 
			if (caseInfo.updateArcWeights){
				if(tom.isArc(i,j)) {
					caseInfo.arcWeights[j][i] -= caseInfo.totalWeight;
				}				
				else {
					caseInfo.arcWeights[j][i] += caseInfo.totalWeight;
				}
			}
			return true; 
		}
		else {
			// restore.
			if(tom.isArc(i, j))
			{ tom.removeArc(i, j); }
			else
			{ tom.addArc(i, j); }
			
			nodeJ.parent = oldParentsJ;
			return false;
		}
	}
	
}
