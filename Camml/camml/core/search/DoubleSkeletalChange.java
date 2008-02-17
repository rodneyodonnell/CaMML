//
// Double Skeletal Change operation for mutating TOMs
//
// Copyright (C) 2002 Rodney O'Donnell, Lucas Hope.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: DoubleSkeletalChange
// Author: {rodo,lhope}@csse.monash.edu.au

package camml.core.search;

import java.util.Random;

/**
 * DoubleSkeletalChange chooses 3 variabled at random such that a -> b -> c in the total ordering.
 * The arcs a->c and b->c are then toggled.  This can either add/remove two arcs or perform a
 * parent swap. 
 */
public class DoubleSkeletalChange extends TOMTransformation {
	
	/** cost to add an arc. */
	protected double arcCost;
	
	/** pre-allocated return value for getNodesChanged() */
	private int[] changes = new int[1];
	
	/** Pre-allocated value to pass to TOMCoster.toggleArcs()*/
	private int[][] nodesToggled = new int[2][2];
	
	/** Return array of changes nodes <br> 
	 *  NOTE: Values within returned array are volatile and will often be changed is transform()
	 *        is called again.  */
	public int[] getNodesChanged() { return changes; }
	
	/**
	 * Constructor for DoubleSkeletalChange.
	 */
	public DoubleSkeletalChange(Random generator, double arcProb, 
			CaseInfo caseInfo, double temperature) {
		super( generator, caseInfo, temperature );
		this.arcCost =  Math.log(arcProb / (1.0 - arcProb));
	}
	
	/**
	 * Toggle existence of two arcs into a single node.
	 */
	public boolean transform(TOM tom, double ljp) {
		int numNodes = tom.getNumNodes();
		
		// At least 3 nodes are required to do a double skeletal change.
		if (numNodes < 3) {
			return false;
		}
		
		// node[1..3] must be chosen so that they are all different, and node3 follows 
		// node1&2 in the temporal ordering. 
		int node1 = 0, node2 = 0, node3 = 0;
		
		// Create node1, node2, node3 such that all are different
		// and node1 > node2 > node3
		node1 = (int)( numNodes * rand.nextDouble() );
		node2 = (int)( (numNodes-1) * rand.nextDouble() );
		node3 = (int)( (numNodes-2) * rand.nextDouble() );
		
		if ( node2 >= node1 ) { node2 ++; }
		if ( node1 < node2 ) { int temp = node1; node1 = node2; node2 = temp; }
		if ( node3 >= node2 ) { node3++; }
		if ( node3 >= node1 ) { node3++; }
		if ( node1 < node3 ) { int temp = node1; node1 = node3; node3 = temp; }
		
		int childVar = tom.nodeAt( node1 );
		int parentVar1 = tom.nodeAt( node2 );
		int parentVar2 = tom.nodeAt( node3 );
		
		changes[0] = childVar;
		
		// The cost of node1 and node2 cannot be changed (as thier parents remain constant).
		// The modification of the parents of node3 means that is may change.
		// so we must find it's original cost before changing it.
		Node childNode = tom.getNode( childVar );	
		int[] originalParents = childNode.parent;
		double oldChildCost = caseInfo.nodeCache.getMMLCost( childNode );
		
		// Check if the mutation will add too many parents to NodeJ	
		//boolean isArc13 = tom.isArc( parentVar1, childVar );    // is there an arc from 1 -> 3
		//boolean isArc23 = tom.isArc( parentVar2, childVar );	// is there an arc from 2 -> 3
		// addArc is the total number of arcs which will be added/removed by this mutation.
		int addArc = 0;
		if ( tom.isArc( parentVar1, childVar ) ) addArc--; else addArc++;
		if ( tom.isArc( parentVar2, childVar ) ) addArc--; else addArc++;
		if ( childNode.parent.length + addArc > tom.maxNumParents ) 
			return false;  // return false if too many parents are present.
		
		// Links P1-->C & P2-->C are toggled.
		nodesToggled[0][0] = childVar;
		nodesToggled[0][1] = childVar;
		nodesToggled[1][0] = parentVar1;
		nodesToggled[1][1] = parentVar2;
		
		// Calculate change in TOM (structure) cost caused by toggle.
		double toggleCost = 
			caseInfo.tomCoster.costToToggleArcs(tom,nodesToggled[0],nodesToggled[1]);

		// toggle node1 -> node3 and node2 -> node3
		doubleMutate( tom, parentVar1, parentVar2, childVar);
		
		// update the parents of node3 and find the cost.
		double newChildCost = caseInfo.nodeCache.getMMLCost( childNode );
		
		// The new cost is original - (cost for old arcs and old node) 
		//                          + (cost for new arcs and new node)
		oldCost = 0;
		cost = newChildCost - oldChildCost + toggleCost;
		
		if (accept()) {
			if (caseInfo.updateArcWeights){
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
			doubleMutate( tom, parentVar1, parentVar2, childVar);
			childNode.parent = originalParents;
			return false;
		}
	}   
}
