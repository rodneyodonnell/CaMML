package camml.core.searchDBN;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import camml.core.models.bNet.BNet;
import camml.plugin.netica.NeticaFn;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.Value.Structured;

/** Class for exporting DBNs (specifically, DTOMs) to Netica format. */
public class ExportDBNNetica {

	/**Export a DTOM to a specified location.
	 * @param filePath The location to export the Netica file
	 * @param dtom The DTOM to export
	 * @param t0_suffix Suffix to append to variable name (first time slice): i.e. "_0" - "Var" becomes "Var_0" 
	 * @param t1_suffix Suffix to append to variable name (second time slice): i.e. "_1" - "Var" becomes "Var_1"
	 * @throws Exception For IOExceptions, LearnerException, etc.
	 */
	public static void export( String filePath, DTOM dtom, String t0_suffix, String t1_suffix ) throws Exception
	{
		String s = makeNeticaFileString( dtom, t0_suffix, t1_suffix );
		
		//Export:
		try {
            FileWriter out = new FileWriter(filePath);
            out.write( s );
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception(e);
        }
	}
	
	/**Generate a string that constitutes a Bayesian Network from a DTOM.
	 * String can be written to a file (see export function) or used in a BNet viewer such as
	 * camml.core.newgui.BNetViewer
	 * 
	 * @param dtom The DTOM to export
	 * @param t0_suffix Suffix to append to variable name (first time slice): i.e. "_0" - "Var" becomes "Var_0" 
	 * @param t1_suffix Suffix to append to variable name (second time slice): i.e. "_1" - "Var" becomes "Var_1"
	 * @throws Exception For IOExceptions, LearnerException, etc.
	 */
	public static String makeNeticaFileString( DTOM dtom, String t0_suffix, String t1_suffix ) throws Exception {
		
		StringBuffer s = new StringBuffer();	//Main output string...
		Value.Vector data = dtom.caseInfo.data;
		int numNodes = dtom.getNumNodes();
		
		/** Type of data network is based on. */
	    final Type.Structured datatype = (Type.Structured)((Type.Vector)data.t).elt;
		
		String[] name = ((cdms.core.Type.Structured)((cdms.core.Type.Vector)data.t).elt).labels;
		
		String[] name_t0 = new String[numNodes];
		String[] name_t1 = new String[numNodes];
		for( int i=0; i<numNodes; i++ ){
			name_t0[i] = name[i] + t0_suffix;
			name_t1[i] = name[i] + t1_suffix;
		}
		
		//Write file header information
		s.append( "// ~->[DNET-1]->~\n" );
		s.append( "// File created by CaMML - DBN Exporter\n\n" );
		s.append( "bnet CaMML_DBN {\n" );
		s.append( "AutoCompile = TRUE;\n" );
		s.append( "autoupdate = TRUE;\n" );
		
		//Determine the arity of each node:
		int[] arity = new int[ numNodes ];
		for( int i=0; i<numNodes; i++ ){
			Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];
            arity[i] = NeticaFn.makeValidNeticaNames(sType.ids,true).length;
		}
		
		//Calculate the parameters for each node:
		Value.Vector[] params = new Value.Vector[numNodes];			//T1 parameters (inc. temporal parents/intraslice arcs)
		Value.Vector[] paramsT0 = new Value.Vector[numNodes];		//T0 parameters (no interslice arcs)
		for( int i=0; i<numNodes; i++ ){
			//Learn parameters - T1 (inc. temporal arcs)
			DNode n = (DNode)dtom.getNode(i);
			Value.Structured model = n.learnModel( dtom.caseInfo.mmlModelLearner, dtom.caseInfo.data );
			params[i] = (Value.Vector)model.cmpnt(2);
			
			//Learn parameters - T0 (no temporal arcs)
			Value.Structured modelT0 = n.learnModelT0( dtom.caseInfo.mmlModelLearner, dtom.caseInfo.data );
			paramsT0[i] = (Value.Vector)modelT0.cmpnt(2);
		}
		
		
		//Go through each node and print... _T0 NODES_
		for( int i=0; i<numNodes; i++ ){
			s.append( "node " + name_t0[i] + " {\n" );
            s.append( "\tkind = NATURE;\n" );
            s.append( "\tdiscrete = TRUE;\n" );
            s.append( "\tnumstates = " + arity[i] + ";\n" );
            
            //Print the states, if appropriate: (Stolen from NeticaFN class)
            if (datatype.cmpnts[i] instanceof Type.Symbolic) {
                Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];                
                String str = Arrays.toString(NeticaFn.makeValidNeticaNames(sType.ids,true));                
                s.append( "\tstates = (" + str.subSequence(1,str.length()-1) + ");\n" );
            }
            
            int[] parentsIntraslice = dtom.getNode(i).getParentCopy();
            
            // print list of INTRASLICE parents
            s.append( "\tparents = ( " );  
            for ( int j = 0; j < parentsIntraslice.length; j++ ) {
                s.append( name_t0[ parentsIntraslice[j] ] );
                if ( j != parentsIntraslice.length-1) { s.append( ", "); }
            }
            s.append( " );\n" );
            
            // Now many parent combinations exist?
            int parentComs = 1;
            for ( int j = 0; j < parentsIntraslice.length; j++ ) { parentComs *= arity[ parentsIntraslice[j] ]; }
            
            //Go through each parent combination and print...
            s.append("\tprobs = ");
            
            Value.Vector param = paramsT0[i].cmpnt(1);	//Vector of parameters for this node
            
            int numParents = parentsIntraslice.length;
            int[] parentArity = new int[ numParents ];
            for( int j=0; j<numParents; j++ ){
            	parentArity[j] = arity[ parentsIntraslice[j] ];
            }
            
            int[] parentAssignment = new int[ numParents ];	//starts off [0,0,0] etc
            
            //Write open brackets - equal to number of parents
            for( int j=0; j<numParents; j++ ){
            	s.append("(");
            }
            
            //Need to loop through parent assignments: [0,0,0], [0,0,1], [0,0,2], ..., [N,N,N]
            //BUT: CPT parameters are ordered by parent assignment as per [0,0,0], [1,0,0], [2,0,0], ..., [N,N,N]
            for( int j=0; j<parentComs; j++ ){
            	
            	//Write close brackets, commas, etc:
            	if( j > 0 ){
		        	int[] lastParentAssignment = parentAssignment.clone();
		        	BNet.incrementBitfield( parentAssignment, parentArity );	//Update current parent assignment
		        	
		        	//Work out how many have changed:
		        	int parentAssignmentsChanged = 0;
		        	for( int k=0; k<parentAssignment.length; k++ ){
		        		if( lastParentAssignment[k] != parentAssignment[k] ) parentAssignmentsChanged++;
		        	}
		        	
		        	//Write close brackets, comma, and open brackets:
		        	for( int k=0; k<parentAssignmentsChanged-1; k++ ){
		        		s.append(")");
		        	}
		        	s.append(",");
		        	for( int k=0; k<parentAssignmentsChanged-1; k++ ){
		        		s.append("(");
		        	}
            	}
            	
            	//Given a current assignment of parents for netica (i.e. [0,0,0]), find the appropriate index for
            	// the correct set of parameters, given that CPTs are indexed (0,0,0), (1,0,0), (2,0,0) etc
            	int paramIndex = DBNStaticMethods.assignmentToIndexReverse(parentAssignment, parentArity );
            	
            	//Write the current set of parameters:
            	Structured values = (Value.Structured)param.elt( paramIndex );		//Structure of values...
            	int numValues = values.length();
            	s.append("(");
            	for( int k=0; k<numValues; k++ ){
            		s.append(values.doubleCmpnt(k) );
            		if( k != numValues-1 ) s.append(",\t");
            	}
            	s.append(")");	//Close bracket for set of parameters
            }
            //Write close brackets - equal to number of parents
            for( int j=0; j<numParents; j++ ){
            	s.append(")");
            }
            s.append(";\n");	//End of probs
            
            s.append("\t};\n");	//End of node	
		}
		
		
		//Go through each node and print... T1 NODES
		for( int i=0; i<numNodes; i++ ){
			s.append( "node " + name_t1[i] + " {\n" );
            s.append( "\tkind = NATURE;\n" );
            s.append( "\tdiscrete = TRUE;\n" );
            s.append( "\tnumstates = " + arity[i] + ";\n" );
            
            //Print the states, if appropriate:
            if (datatype.cmpnts[i] instanceof Type.Symbolic) {
                Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];                
                String str = Arrays.toString(NeticaFn.makeValidNeticaNames(sType.ids,true));                
                s.append( "\tstates = (" + str.subSequence(1,str.length()-1) + ");\n" );
            }
            
            int[] parentsIntraslice = dtom.getNode(i).getParentCopy();
            int[] parentsTemporal = ((DNode)dtom.getNode(i)).getTemporalParentCopy();
            
            // print list of INTRASLICE parents
            s.append( "\tparents = ( " );  
            for ( int j = 0; j < parentsIntraslice.length; j++ ) {
                s.append( name_t1[ parentsIntraslice[j] ] );
                if ( j != parentsIntraslice.length-1) { s.append( ", "); }
            }
            // ...and TEMPORAL parents:
            for( int j = 0; j < parentsTemporal.length; j++ ){
            	if( j==0 && parentsIntraslice.length > 0 ) s.append(",\t");
            	s.append( name_t0[ parentsTemporal[j] ] );
            	if( j != parentsTemporal.length-1 ) s.append( ",\t" );
            }
            s.append( " );\n" );
            
            // Now many parent combinations exist?
            int parentComs = 1;
            for ( int j = 0; j < parentsIntraslice.length; j++ ) { parentComs *= arity[ parentsIntraslice[j] ]; }
            for ( int j = 0; j < parentsTemporal.length; j++ ) { parentComs *= arity[ parentsTemporal[j] ]; }
            
            //Go through each parent combination and print...
            s.append("\tprobs = ");
            
            Value.Vector param = params[i].cmpnt(1);	//Vector of parameters for this node
            
            int numParents = parentsIntraslice.length + parentsTemporal.length;
            int[] parentArity = new int[ numParents ];
            for( int j=0; j<numParents; j++ ){
            	if( j < parentsIntraslice.length ) parentArity[j] = arity[ parentsIntraslice[j] ];
            	else parentArity[j] = arity[ parentsTemporal[j - parentsIntraslice.length] ];
            }
            
            int[] parentAssignment = new int[ numParents ];	//starts off [0,0,0] etc
            
            //Write open brackets - equal to number of parents
            for( int j=0; j<numParents; j++ ){
            	s.append("(");
            }
            
            //Need to loop through parent assignments: [0,0,0], [0,0,1], [0,0,2], ..., [N,N,N]
            //BUT: CPT parameters are ordered by parent assignment as per [0,0,0], [1,0,0], [2,0,0], ..., [N,N,N]
            for( int j=0; j<parentComs; j++ ){
            	
            	//Write close brackets, commas, etc:
            	if( j > 0 ){
		        	int[] lastParentAssignment = parentAssignment.clone();
		        	BNet.incrementBitfield( parentAssignment, parentArity );	//Update current parent assignment
		        	
		        	//Work out how many have changed:
		        	int parentAssignmentsChanged = 0;
		        	for( int k=0; k<parentAssignment.length; k++ ){
		        		if( lastParentAssignment[k] != parentAssignment[k] ) parentAssignmentsChanged++;
		        	}
		        	
		        	//Write close brackets, comma, and open brackets:
		        	for( int k=0; k<parentAssignmentsChanged-1; k++ ){
		        		s.append(")");
		        	}
		        	s.append(",");
		        	for( int k=0; k<parentAssignmentsChanged-1; k++ ){
		        		s.append("(");
		        	}
            	}
            	
            	//Given a current assignment of parents for Netica (i.e. [0,0,0]), find the appropriate index for
            	// the correct set of parameters, given that CPTs are indexed (0,0,0), (1,0,0), (2,0,0) etc
            	int paramIndex = DBNStaticMethods.assignmentToIndexReverse(parentAssignment, parentArity );
            	
            	//Write the current set of parameters:
            	Structured values = (Value.Structured)param.elt( paramIndex );		//Structure of values...
            	int numValues = values.length();
            	s.append("(");
            	for( int k=0; k<numValues; k++ ){
            		s.append(values.doubleCmpnt(k) );
            		if( k != numValues-1 ) s.append(",\t");
            	}
            	s.append(")");	//Close bracket for set of parameters
            }
            //Write close brackets - equal to number of parents
            for( int j=0; j<numParents; j++ ){
            	s.append(")");
            }
            
            s.append(";\n");	//End of probs
            
            s.append("\t};\n");	//End of node
		}
		
		//Finish...
		s.append( "};\n" );		//End of file
		
		return s.toString();	//Convert StringBuffer to String
	}
}
