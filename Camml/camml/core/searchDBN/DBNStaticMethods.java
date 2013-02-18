package camml.core.searchDBN;

import java.util.Random;

import camml.core.models.ModelLearner.LearnerException;
import camml.plugin.netica.NeticaFn;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;
import cdms.core.Value.Structured;

/**A set of static methods used during the process of learning DBNs.
 * Mostly the functions here are analogous to (or replacements for) some of the
 * functions in BNet. These methods are mostly used in the process of combining
 * SECs to MMLECs - i.e. in MetropolisSearchDBN.
 * @author Alex Black
 */
public class DBNStaticMethods {

	/**Analogous to BNet.noInputLogPVec(...), but specifically for DBNs
	 * Calculates logP(data|model,parameters)
	 * @param x	Data set to cost
	 * @param subModel Array of models used to parameterize the nodes
	 * @param subParam Parameters for each node
	 * @param order Order of the variables in the DBN
	 * @param parentList Array of parents (intraslice) for each node
	 * @param temporalParentList Array of parents (temporal) for each node
	 */
	public static double DBNlogP( Value.Vector x, Value.Model[] subModel, Value[] subParam,
			int[] order, int[][] parentList, int[][] temporalParentList ){
		
		double total = 0.0;
        
        for ( int i = 0; i < order.length; i++ ) {
            int current = order[i];
            
            // Select out parent data for the current node
            Value.Vector parentData = new DBNDataWrapper( x, parentList[current], temporalParentList[current] );
            
            //Select child data for the current node:
            int l = x.cmpnt(current).length();
            Value.Vector output = x.cmpnt(current).sub(1, l-1);		//Child data (t1 timeslice); 1 to length-1 (can't t=0, as no temporal parents for this instance)
            
            total += subModel[current].logP( output, subParam[current], parentData );
        }
        
        return total;
	}
	
	/**Extracts the subModels from the parameters vector returned by DTOM.makeParameters(...).
	 * Basically the same as BNet.makeSubModelList(...), but for input as follows - 
	 * [ ( [intraslice parents], [temporal parents], (submodel,subparams) ) ]
	 */
	protected static Value.Model[] makeSubModelListDBN( Value.Vector params ){
		Value.Model[] model = new Value.Model[ params.length() ];
        
        for ( int i = 0 ; i < model.length; i++ ) {
            Value.Structured temp = (Value.Structured)params.elt(i);
            //model[i] = (Value.Model)((Value.Structured)(temp.cmpnt(2))).cmpnt(0);
            model[i] = (Value.Model)((Value.Structured)(temp.cmpnt(3))).cmpnt(0);	//One extra component in input vs TOM params
        }
        return model;
	}
	
	/**Extracts the parameters for the subModels from the parameters vector returned by DTOM.makeParameters(...)
	 * Basically the same as BNet.makeSubParamList(...), but for input as follows - 
	 * [ ( [intraslice parents], [temporal parents], (submodel,subparams) ) ] 
	 */
    protected static Value[] makeSubParamListDBN( Value.Vector params )
    {
        Value[] value = new Value[ params.length() ];
        
        for ( int i = 0 ; i < value.length; i++ ) {
            Value.Structured temp = (Value.Structured)params.elt(i);
            //value[i] = ((Value.Structured)(temp.cmpnt(2))).cmpnt(1);
            value[i] = ((Value.Structured)(temp.cmpnt(3))).cmpnt(1);	//One extra component in input vs TOM params
        }
        return value;
    }
    
    
    /**Returns a time series (a vector) sampled from the DBN (passed as a DTOM)
     * Based on BNetStochastic.generate(...). Used (perhaps amongst other things) during the calculation
     * of (approximate) KL divergence between models
     * @param rand	RNG to use
     * @param dtom DTOM, parameters of which to generate data from (must have caseInfo set in DTOM)
     * @param n	length of time series to generate from parameters
     * @return Time series sampled/generated from the DTOM
     */
    public static Value.Vector generateTimeSeriesDTOM( Random rand, DTOM dtom, int n ){
    	//Get the variable names and number of nodes; also get the ordering of the nodes (used for forward sampling)
    	int numNodes = dtom.getNumNodes();
    	String[] name = ((cdms.core.Type.Structured)((cdms.core.Type.Vector)(dtom.caseInfo.data).t).elt).labels;
    	int[] order = dtom.getTotalOrderCopy();
    	
    	//Initialize the vectors and structures etc for storing the time series data: (Value.Vector)
    	Value.Vector origData = dtom.caseInfo.data;
    	Type.Structured inputTypes = (Type.Structured)((Type.Vector)origData.t).elt;	//Type info for generated data is same as original data
    	
    		//Store the assignments in an integer array:
    	int[][] newData = new int[numNodes][n];
    	
    	//Get the parameters for each node (parameters for first time slice, and second time slice)
    	Value.Vector[] paramsT0 = new Value.Vector[numNodes];
    	Value.Vector[] paramsT1 = new Value.Vector[numNodes];
		for( int i=0; i<numNodes; i++ ){
			DNode node = (DNode)dtom.getNode(i);
			try{
				//Learn parameters - T1 (inc. temporal arcs)
				Value.Structured model = node.learnModel( dtom.caseInfo.mmlModelLearner, dtom.caseInfo.data );
				paramsT1[i] = (Value.Vector)model.cmpnt(2);
				
				//Learn parameters - T0 (no temporal arcs)
				Value.Structured modelT0 = node.learnModelT0( dtom.caseInfo.mmlModelLearner, dtom.caseInfo.data );
				paramsT0[i] = (Value.Vector)modelT0.cmpnt(2);
			} catch( LearnerException e ){
				throw new RuntimeException("Error learning models. " + e );
			}
		}
    	
    	
    	//Determine the arity of each node: (Using a very inelegant method...)
	    final Type.Structured datatype = (Type.Structured)((Type.Vector)(dtom.caseInfo.data).t).elt;
		int[] arity = new int[ numNodes ];
		for( int i=0; i<numNodes; i++ ){
			Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];
            arity[i] = NeticaFn.makeValidNeticaNames(sType.ids,true).length;
		}
    	
    	//Generate a set of assignments for the FIRST time slice
    	// (This needs to be done in order, to avoid sampling children before parents...)
		int[] assignmentT0 = new int[numNodes];
    	for( int i=0; i<numNodes; i++ ){
    		DNode currNode = (DNode)dtom.getNode( order[i] );	//ith node in total order
    		Value.Vector currParams = paramsT0[ order[i] ].cmpnt(1);		//parameters for ith node in total order
    		
    		//Get the appropriate distribution to sample from (given values of parents)
    		Structured vals;
    		if( currNode.getNumParents() == 0 ){	//No parents
    			 vals = (Value.Structured)currParams.elt(0);	//Contains the actual probability values; only one element in structure if no parents...
    		} else {	//This node has parents (which already have assigned values)
    			//Need to work out the index of the relevent parameters given the assignments of parents
    				//Parameters are in order of [0,0,0], [0,0,1], [0,0,2], ..., [A,B,C]
    				//Index given by: sum_x( val[pa[x]]*prod( arity[pa[x+1...end]] )
    			
    			int[] currParents = currNode.getParentCopy();		//Intraslice parents

    			//Collect assignments and arity for the current parents
    			int[] assignment = new int[ currParents.length ];
    			int[] ar = new int[ currParents.length ];
    			for( int z=0; z<currParents.length; z++ ){
    				assignment[z] = assignmentT0[ currParents[z] ];
    				ar[z] = arity[ currParents[z] ];
    			}
    			int index = assignmentToIndexReverse( assignment, ar );
    			
    			//Find the set of parameters for the current parent assignment:
    			vals = (Value.Structured)currParams.elt(index);	//Contains the actual probability values for the current assignment of parents
    		}
    		
    		//Now, sample a value according to the probability distribution:
    		double rv = rand.nextDouble();						//Random value b/w 0 and 1
    		double cumProb = 0.0;
    		for( int idx=0; idx<arity[order[i]]; idx++ ){	//i.e. loop through each value
				cumProb += vals.doubleCmpnt(idx);
				if( rv < cumProb ){	//Assignment to node[ order[i] ] is idx
					assignmentT0[ order[i] ] = idx;
					break;
				}
			}
    	}
    	
    	//Generate data from SECOND time slice CPDs - repeatedly...
    	int[] assignmentT1 = new int[numNodes];
    	for( int lineNum=0; lineNum<n; lineNum++ ){
    		//First: record the first time slice assignemnts.
    		//Then: copy the second time slice assignments to the first time slice assignments
    		if( lineNum > 0 ){
    			//System.out.println("Assignments line " + (lineNum-1) + " - " + Arrays.toString(assignmentT0) );
    			for( int j=0; j<numNodes; j++ ){ //j is variable number
    				newData[j][lineNum-1] = assignmentT0[j];
    			}
    			
    			assignmentT0 = assignmentT1;
    			assignmentT1 = new int[numNodes];
    		}
    		
    		//Now, generate data for second time slice given values of first time slice:
    		for( int i=0; i<numNodes; i++ ){
        		DNode currNode = (DNode)dtom.getNode( order[i] );	//ith node in total order
        		Value.Vector currParams = paramsT1[ order[i] ].cmpnt(1);		//parameters for ith node in total order
        		
        		//Get the appropriate distribution to sample from (given values of parents)
        		Structured vals;
        		if( currNode.getNumParents() == 0 ){	//No parents
        			 vals = (Value.Structured)currParams.elt(0);	//Contains the actual probability values; only one element in structure if no parents...
        		} else {	//This node has parents (which already have assigned values)
        			//Need to work out the index of the relevent parameters given the assignments of parents
        				//Parameters are in order of [0,0,0], [0,0,1], [0,0,2], ..., [A,B,C]
        				//Index given by: sum_x( val[pa[x]]*prod( arity[pa[x+1...end]] )
        				//Complicated by the fact that we have temporal parents and intraslice parents...
        				//[intraslice_parents, temporal_parents]
        			
        			int[] currParents = currNode.getParentCopy();		//Intraslice parents
        			int[] currParentsTemporal = currNode.getTemporalParentCopy();	//Temporal (interslice) parents
        			
        			//Collect the parent assignments and arities
        			int numParents = currParents.length + currParentsTemporal.length;
        			int[] assignment = new int[ numParents ];
        			int[] ar = new int[ numParents ];
        			for( int z=0; z<numParents; z++ ){
        				if( z < currParents.length ){	//Dealing with intraslice parents
        					assignment[z] = assignmentT1[ currParents[z] ];
        					ar[z] = arity[ currParents[z] ];
        				} else {	//Dealing with interslice (t0) parents
        					assignment[z] = assignmentT0[ currParentsTemporal[ z-currParents.length] ];
        					ar[z] = arity[ currParentsTemporal[ z-currParents.length] ];
        				}
        			}
        			int index = assignmentToIndexReverse( assignment, ar );
        			
        			//Find the set of parameters for the current parent assignment:
        			vals = (Value.Structured)currParams.elt(index);	//Contains the actual probability values for the current assignment of parents
        		}
        		
        		//Now, sample a value according to the probability distribution:
        		double rv = rand.nextDouble();						//Random value b/w 0 and 1
        		double cumProb = 0.0;
        		for( int idx=0; idx<arity[order[i]]; idx++ ){	//i.e. loop through each value
    				cumProb += vals.doubleCmpnt(idx);
    				if( rv < cumProb ){	//Assignment to node[ order[i] ] is idx
    					assignmentT1[ order[i] ] = idx;
    					break;
    				}
    			}
        	}
    	}
    	
    	//Copy the very last line of data:
    	for( int j=0; j<numNodes; j++ ){
			newData[j][n-1] = assignmentT1[j];
		}
    	
    	//Now, combine type and value (i.e. assignments) together for each variable:
    	Value.Vector[] vecArray = new Value.Vector[numNodes];
    	for( int i=0; i<numNodes; i++ ){
    		vecArray[i] = new VectorFN.FastDiscreteVector( newData[i], (Type.Symbolic)inputTypes.cmpnts[i] );
    	}
    	//And create the overall data structure:
    	Value.Structured vecStruct = new Value.DefStructured(vecArray,name);
    	Value.Vector newDataVector = new VectorFN.MultiCol(vecStruct);
    	
    	//Return the new time series vector...
    	return newDataVector;
    }
    
    /**Extracts and returns a multi-dimensional array of intraslice parents from a DTOM.
    * int[i][j] = the jth intraslice parent for node i in the DTOM
    * Note that arrays are of different lengths: i.e. int[a].length may not equal int[b].length,
    * for a =\= b. int[i] is of length 0 if node i has no intraslice parents.
    */
    public static int[][] makeParentsArray( DTOM dtom ){
    	int numNodes = dtom.getNumNodes();
    	int[][] parents = new int[numNodes][0];
    	
    	for( int i=0; i<numNodes; i++ )	parents[i] = dtom.getNode(i).getParentCopy();
    	
    	return parents;
    }
    
    /**Extracts and returns a multi-dimensional array of interslice (temporal) parents from a DTOM.
     * int[i][j] = the jth interslice (temporal) parent for node i in the DTOM
     * Note that arrays are of different lengths: i.e. int[a].length may not equal int[b].length,
     * for a =\= b. int[i] is of length 0 if node i has no temporal parents.
     */
    public static int[][] makeParentsTemporalArray( DTOM dtom ){
    	int numNodes = dtom.getNumNodes();
    	int[][] parents = new int[numNodes][0];
    	
    	for( int i=0; i<numNodes; i++ )	parents[i] = ((DNode)dtom.getNode(i)).getTemporalParentCopy();
    	
    	return parents;
    }
    
    /**Calculates an index to an array of parameters for a node, where node has multiple parents, and
     * parameters are indexed as follows:
     * [0,0,0]=0, [0,0,1]=1, [0,0,2]=2, ..., etc
     * Argument takes current assignment to a set of variables; also arity for those variables.
     * @see Also BNet.incrementBitfield(...)
     */
    public static int assignmentToIndex( int[] assignment, int[] arity ){
    	if( assignment.length == 0 ) return 0;
    	
    	int index = 0;
		for( int i=0; i < assignment.length; i++ ){
			int mult = 1;
			for( int x=i+1; x<assignment.length; x++ ){
				mult *= arity[ x ];
			}
			index += assignment[ i ]*mult;
		}
    	
		return index;
    }
    
    /**Calculates an index to an array of parameters for a node, where node has multiple parents, and
     * parameters are indexed as follows:
     * (0,0,0), (1,0,0), (0,1,0), (1,1,0), (0,0,1), (1,0,1), (0,1,1), (1,1,1)  
     * @param assignment Current assignment to the parents
     * @param arity Arity of the parents
     * @see Also BNet.reverseIncrementBitfield(...)
     */
    public static int assignmentToIndexReverse( int[] assignment, int[] arity ){
    	if( assignment.length == 0 ) return 0;
    	
    	int index = 0;
		for( int i=0; i < assignment.length; i++ ){
			int mult = 1;
			for( int x=i-1; x>=0; x-- ){
				mult *= arity[ x ];
			}
			index += assignment[ i ]*mult;
		}
		return index;
    }
}
