//
// This "Search" sets the network to a given structure and parameterizes it.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: FixedStructureSearch.java
// Author: rodo@dgs.monash.edu.au
// Created on 8/02/2005

package camml.core.models.bNet;

import java.util.Random;

import camml.core.models.ModelLearner;
import camml.core.search.BNetSearch;
import camml.core.search.CaseInfo;
import cdms.core.Value.Vector;

/**
 * This "Search" sets the network to a given structure and parameterizes it. <br>
 * To set structure, "currentTOM" option must be set.
 */
public class FixedStructureSearch extends BNetSearch {
	
	/**
	 * @param rand
	 * @param caseInfo
	 */
	public FixedStructureSearch(Random rand, CaseInfo caseInfo) {
		super(rand, caseInfo); 
	}
	
	/**
	 * @param rand
	 * @param data
	 * @param mlModelLearner
	 * @param mmlModelLearner
	 */
	public FixedStructureSearch(Random rand, Vector data,
			ModelLearner mlModelLearner, ModelLearner mmlModelLearner) {
		super(rand, data, mlModelLearner, mmlModelLearner);
	}
	
	/* (non-Javadoc)
	 * @see cdms.plugin.search.Search.SearchObject#reset()
	 */
	public void reset() { }
	
	/* (non-Javadoc)
	 * @see cdms.plugin.search.Search.SearchObject#doEpoch()
	 */
	public double doEpoch() {
		searchDone = true;
		bestTOM.setStructure( tom );
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see cdms.plugin.search.Search.SearchObject#getPercentage()
	 */
	public double getPercentage() {
		if ( searchDone ) return 100;
		else return 0;
	}
	
}
