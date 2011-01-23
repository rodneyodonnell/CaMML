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
// This "Search" sets the network to a given structure and parameterizes it.
//

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
