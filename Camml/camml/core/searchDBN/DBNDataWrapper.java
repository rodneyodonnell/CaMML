package camml.core.searchDBN;


import cdms.core.Type;
import cdms.core.Value;


/** Wrapper for a dataset, designed for use in learning DBNs - specifically, the 
 *    2nd time slice parameters. Generally used where a SelectedVector object would
 *    be used (if learning a standard (non-DBN) BN).<br>
 *  User passes full dataset, intraslice parents and temporal parents.
 *  Wrapper then provides access to the data for these variables only.
 *  Data ordered according to [ IP1, IP2, ..., IPN, TP1, TP2, ..., TPM ] for N
 *  intraslice parents, and M temporal parents.<br>
 *  Similar to camml.core.library.SelectedVector, but with a few tweaks for learning
 *  DBNs specifically. Data for interslice (temporal) parents are appropriately
 *  offset; intraslice variable data is indexes 1...length-1.
 * @author Alex Black
 */
public class DBNDataWrapper extends Value.Vector {
	
	private static final long serialVersionUID = 7856079126384141278L;
	private final Value.Vector fullDataset;
	private final int[] intrasliceVars;
	private final int[] temporalVars;

	public DBNDataWrapper( Value.Vector data, final int[] intrasliceVars, final int[] temporalVars ) {
		//Make new type from selected columns:
		super( makeVectorType((Type.Vector) data.t, intrasliceVars, temporalVars));
		this.fullDataset = data;
		this.intrasliceVars = intrasliceVars;
		this.temporalVars = temporalVars;
	}
	
	/**Make the type information for the vector.
	 * Adapted from SelectedVector.makeSelectedVectorType
	 */
	private static Type.Vector makeVectorType( Type.Vector vType, int[] iCol, int[] tCol ){
		if( iCol == null && tCol == null ){
			return vType;
		} else{
			return new Type.Vector( makeSelectedStructureType( (Type.Structured)vType.elt, iCol, tCol ) );
		}
	}

	/**Make the structure 
	 * Adapted from camml.core.library.SelectedStructure.makeSelectedStructureType
	 */
	public static Type.Structured makeSelectedStructureType( Type.Structured sType, int[] iCol, int[] tCol ){
		// If we don't have initial componenet types. we can't continue.
        if ( sType.cmpnts == null ) {
            throw new RuntimeException("cmpnts type not specified in makeSelectedStructureType.");
        }

        // list of cmpnts to be used.
        Type[] cmpnt;
    
        // names of cmpnts
        String[] name;
    
        // the new type being created
        Type.Structured newType;
    
        // If there are labels, make a new type from labels and cmpnts.
        int numCols = iCol.length + tCol.length;
        if (sType.labels != null) {
        	name = new String[ numCols ];
        	cmpnt = new Type[ numCols ];
        	for ( int i = 0; i < numCols; i++ ) {
        		if( i < iCol.length ){
        			name[i] = sType.labels[iCol[i]];
        			cmpnt[i] = sType.cmpnts[iCol[i]];
        		}else{
        			name[i] = sType.labels[tCol[i-iCol.length]];
        			cmpnt[i] = sType.cmpnts[tCol[i-iCol.length]];
        		}
            }
            newType = new Type.Structured( cmpnt, name );
        }
        // if there are no labels, make an unlabeled type.
        else {
        	cmpnt = new Type[ numCols ];
        	for ( int i = 0; i < numCols; i++ ) {
        		if( i < iCol.length ){
        			cmpnt[i] = sType.cmpnts[iCol[i]];
        		}else{
        			cmpnt[i] = sType.cmpnts[tCol[i-iCol.length]];
        		}
            }
            newType = new Type.Structured( cmpnt );
        }

        // return the newly created type.
        return newType;
    }
	
	/** Returns a row of the data.
	 *  Data is appropriately offset as required (i.e. data for parents that form
	 *  interslice arcs are from the previous time index compared to the data for
	 *  intraslice variables)  
	 */
	public Value elt(int i) {
		return new DBNStructureWrapper( fullDataset, i, intrasliceVars, temporalVars );
	}

	//Length 1 less than original dataset (effectively considering 'pairs' of data when learning DBNs)
	public int length() {
		return fullDataset.length()-1;
	}

	/**Private inner class for selecting structures...
	 * Similar function to camml.core.library.SelectedStructure
	 * Takes a row, and selects out a subset of the variables...
	 */
	private class DBNStructureWrapper extends Value.Structured{

		private static final long serialVersionUID = 1519531796471855059L;
		private int[] intrasliceVars;
		private int[] temporalVars;
		private int selectedElement;
		private Value.Vector fullData;
		private int len;
		
		
		public DBNStructureWrapper( Value.Vector fullData, int elementNumber, int[] intrasliceVars, int[] temporalVars ){
			super( makeSelectedStructureType( fullData, elementNumber, intrasliceVars, temporalVars ) );
			this.intrasliceVars = intrasliceVars;
			this.temporalVars = temporalVars;
			//this.origStructure = structure;
			this.fullData = fullData;
			this.selectedElement = elementNumber;
			this.len = intrasliceVars.length + temporalVars.length;
		}

		public Value cmpnt(int index) {
			if( index < 0 ) throw new RuntimeException("Variable index < 0!");
			if( index < intrasliceVars.length ){
				//return origStructure.cmpnt( intrasliceVars[index] );
				return fullData.cmpnt( intrasliceVars[index] ).elt( selectedElement + 1 );	//Intraslice parents: data 1...length-1
			}else{
				//return origStructure.cmpnt( temporalVars[index] );
				return fullData.cmpnt( temporalVars[index - intrasliceVars.length]).elt( selectedElement );		//Temporal parents: data 0...length-2
			}
		}

		public int length() {
			return this.len;
		}
	}
	
    
    /** make a structure type based on an existing structure type and a list of columns.
	 * Taken and adapted from camml.core.library.SelectedStructure
	 * NOTE: Used only in DBNStructureWrapper. Needs to be here because it needs to be static
	 *  (so that it can be used in constructor); static methods need to be in a top-level 
	 * */
    private static Type.Structured makeSelectedStructureType( Value.Vector fullData, int elt, int[] intrasliceVars, int[] temporalVars ){
    	//Get types from full dataset:
    	Type.Structured types = (Type.Structured)fullData.elt(0).t;
    	//Get names from full dataset:
    	String[] name;
    	//Components 
    	Type[] cmpnt;
    	// the new type being created
        Type.Structured newType;
    	
        int numVars = intrasliceVars.length + temporalVars.length;
        
        
        // If there are labels, make a new type from labels and cmpnts.
        if (types.labels != null) {
            name = new String[ numVars ];
            cmpnt = new Type[ numVars ];
            for ( int i = 0; i < numVars; i++ ) {
            	if( i < intrasliceVars.length ){
            		name[i] = types.labels[intrasliceVars[i]] + "1";	//Intraslice variables: add "1" to end (i.e. timeslice t1)
	                cmpnt[i] = types.cmpnts[intrasliceVars[i]];
            	}else{
            		name[i] = types.labels[temporalVars[i - intrasliceVars.length]] + "0";		//Temporal variables: add "0" to end (i.e. timeslice t0)
	                cmpnt[i] = types.cmpnts[temporalVars[i - intrasliceVars.length]];
            	}
                
            }
            newType = new Type.Structured( cmpnt, name );
        }
    	// if there are no labels, make an unlabeled type.
        else {
            cmpnt = new Type[ numVars ];
            for ( int i = 0; i < numVars; i++ ) {
                if( i < intrasliceVars.length ){
                	cmpnt[i] = types.cmpnts[intrasliceVars[i]];
                }else{
                	cmpnt[i] = types.cmpnts[temporalVars[i]];
                }
            }
            newType = new Type.Structured( cmpnt );
        }
    	return newType;
    }
	
}



