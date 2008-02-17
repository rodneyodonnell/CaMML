//
// Abstract model hashing class for CaMML
//
// Copyright (C) 2004 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: ModelHash.java
// Author: rodo@csse.monash.edu.au

package camml.core.search;

import java.io.Serializable;

/**
 *  Base class for TOMHash and SECHash.
 */
public abstract class ModelHash implements Serializable {
	
	/** return hash signature of TOM  */
	public abstract long hash(TOM tom, double logL);
	
	/** Accesor function so hash values can be calculated incrementally in NodeCache*/
	public abstract long getRandom(int x, int y);
	
	/** List of useful external variables. */
	public CaseInfo caseInfo;
}
