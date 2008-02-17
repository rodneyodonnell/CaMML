//
// Metropolis Search for CaMML
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MetropolisSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import cdms.core.*;
import camml.core.library.ExtensionCounter;
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
			if ( i == 0 ) {	return sec.caseInfo.bNet; }
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
					ExtensionCounter.UnlabelledGraph ug = new ExtensionCounter.UnlabelledGraph(tom);
					double perms = ExtensionCounter.dCounter.countPerms(ug);
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
