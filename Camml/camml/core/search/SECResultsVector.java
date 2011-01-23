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
// Metropolis Search for CaMML
//

// File: MetropolisSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import cdms.core.*;
import camml.core.library.extensionCounter.BitSetBasedUnlabelledGraph;
import camml.core.library.extensionCounter.DynamicCounter;
import camml.core.library.extensionCounter.UnlabelledGraph;
import camml.core.models.ModelLearner;
import camml.core.models.ModelLearner.GetNumParams;

/** SECResultsVector contains classes used to view SECs and TOMs as CDMS objects. */
public class SECResultsVector extends Value.Vector {
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -3745982305307462038L;

    /** secArray contains a list of SECs from a MMLEC */
    protected SEC[] secArray;
    
    /** Element type of SECReesultsVector */
    public static Type.Vector tt = new Type.Vector( SECStructure.tt );
    
    /** Constructor*/
    public SECResultsVector( SEC[] secArray )
    {
        super( tt );
        this.secArray = secArray;
    }
    
    /** Return a new SECStructure() */
    public Value elt( int i ) {
        return new SECStructure( secArray[i] );
    }
    
    /** Return number of SECs in MMLEC */
    public int length() {
        return secArray.length;
    }
    
    /** toString function overidden to displal [n SECs].  The default toString tunnels through 
     *  layers of SECs and TOMs attempting to print everyhing including DAG parameters which is
     *  generally unwanted */
    public String toString() { return "[" + length() + " SECs]"; }
    
    /** A Value.Structured representation of a SEC and associated values */
    public static class SECStructure extends Value.Structured {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -8732099825741441944L;
        
        /** SEC represented by structure */
        protected final SEC sec;
        /** Create SEC type */
        public static final Type.Structured tt = 
            new Type.Structured( new Type[] {TOMVector.tt, Type.CONTINUOUS,
                                             Type.CONTINUOUS, Type.CONTINUOUS, Type.CONTINUOUS, Type.CONTINUOUS},
                new String[] {"TOMVector", "posterior",
                              "cleanML", "bestMML","relativePrior","dataCost"} );
        /** Constructor */
        public SECStructure( SEC sec ) {
            super(tt);
            this.sec = sec;
        }
        
        public Value cmpnt( int i ) {
            if ( i == 0 ) {
                return new TOMVector( sec );
            }
            else if ( i == 1 ) {
                return new Value.Continuous(sec.posterior);
            }
            else if ( i == 2 ) {
                return new Value.Continuous(sec.cleanMLCost);
            }
            else if ( i == 3 ) {
                return new Value.Continuous(sec.bestMML);
            }
            else if ( i == 4 ) {
                return new Value.Continuous(sec.relativePrior);
            }
            else if ( i == 5 ) {
                return new Value.Continuous(sec.getDataCost(0));
            }

            throw new RuntimeException("Invalid cmpnt specified in SECStructure");
        }
        
        /** return number of cmpnts*/
        public int length() { return 6; } 
        
        /** Print SEC details */
        public String toString()
        {
            return ("([TOM], posterior=" + sec.posterior + 
                    ",cleanML=" + sec.cleanMLCost + ",bestMML=" + sec.bestMML + 
                    ",dataCost = " + sec.getDataCost(0) + ")");  
        }
    }
    
    /** A Value.Vector of TOMs */
    public static class TOMVector extends Value.Vector {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 4212444245312926970L;

        /** SEC containing [TOM] */
        SEC sec;
        
        /** Vector Type */
        public static Type.Vector tt = new Type.Vector(TOMStructure.tt);
        
        /** Constructor */
        public TOMVector( SEC sec ) {
            super(tt);
            this.sec = sec;
        }
        
        /** Return number of TOMs in SEC */
        public int length() { return sec.getNumTOMs(); }
        
        /** Return specific TOM from SEC*/
        public Value elt( int i ) { return new TOMStructure(sec,i); }
        
        /** return "[n TOMs]" */
        public String toString() { return "[" + length() + " TOMs]"; }
    }
    
    /** A Value.Structured representation of a TOM and associated values */
    public static class TOMStructure extends Value.Structured {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 3679668008319554703L;

        /** Create Structure type */
        public static Type.Structured tt = 
            new Type.Structured( new Type[] {Type.MODEL, Type.TYPE,
                                             Type.CONTINUOUS, Type.CONTINUOUS, Type.CONTINUOUS,
                                             Type.DISCRETE, Type.DISCRETE, Type.DISCRETE,
                                             Type.CONTINUOUS, Type.CONTINUOUS },
                new String[] {"BNetModel", "[Node]",
                              "posterior", "cleanML", "bestMML", 
                              "numVisits","numArcs", "numParams",
                              "logNumExtensions", "datCost"} );
        
        /** SEC this TOM is a member of */
        SEC sec;
        
        /** Index into SEC */
        int tomIndex;
        
        /** Constructor*/
        public TOMStructure( SEC sec, int tomIndex  ) {
            super(tt);
            this.sec = sec;
            this.tomIndex = tomIndex;
        }
        
        /** return number of cmpnts */
        public int length() { return 10; }
        
        /** Return appropriate cmpnt <br>
         * cmpnt(0) = BNet Model<br>
         * cmpnt(1) = TOM parameters <br>
         * cmpnt(2) = TOM posterior <br>
         * cmpnt(3) = TOM ML cost <br>
         * cmpnt(4) = Tom MML cost <br>
         * cmpnt(5) = Number of visits to TOM <br>
         * cmpnt(6) = Number of arcs in TOM <br>
         * cmpnt(7) = Number of Parameters in TOM 
         * cmpnt(8) = Number of Total Ordering of TOM (-1 if too many found.) 
         * */
        public Value cmpnt( int i ) {          
            if ( i == 0 ) {    return sec.caseInfo.bNet; }
            else if ( i == 1 ) {
                try {
                    return sec.getTOM( tomIndex ).makeParameters( sec.caseInfo.mmlModelLearner );
                } catch ( ModelLearner.LearnerException e ) {
                    throw new RuntimeException(e);
                }
            }
            else if ( i == 2 ) { return new Value.Continuous( sec.getPosteriorOfTOM( tomIndex ) ); }
            else if ( i == 3 ) { return new Value.Continuous( sec.cleanMLCost ); }
            else if ( i == 4 ) { return new Value.Continuous( sec.getBestMMLOfTOM( tomIndex ) );}
            else if ( i == 5 ) { return new Value.Discrete( sec.getNumVisitsToTOM( tomIndex ) );}
            else if ( i == 6 ) { return new Value.Discrete( sec.getNumArcs() ); }
            else if ( i == 7 ) {
                Value params;
                try {
                    params = sec.getTOM( tomIndex ).makeParameters( sec.caseInfo.mmlModelLearner );
                } catch ( ModelLearner.LearnerException e ) {
                    throw new RuntimeException(e);
                }
                
                int numParams = 
                    ((GetNumParams)sec.caseInfo.bNet).getNumParams(params) ;
                return new Value.Discrete( numParams );
            }
            else if ( i == 8 ) {
                TOM tom = sec.getTOM(tomIndex);
                
                if ( tom.getNumNodes() <= 15) {
                    UnlabelledGraph ug = new BitSetBasedUnlabelledGraph(tom);
                    double perms = DynamicCounter.dCounter.countPerms(ug);
                    return new Value.Continuous(Math.log(perms));
                }
                else { return new Value.Continuous(-1); }
            }
            else if (i == 9 ) { return new Value.Continuous(sec.tomList.get(tomIndex).getDataCost()); }
            else {
                throw new RuntimeException("Invalid cmpnt requested from TOMStructure");
            }
        }
        
    }    
}          
