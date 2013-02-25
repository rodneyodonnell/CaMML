package camml.plugin.dbn;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import camml.plugin.netica.NeticaFn;
import camml.plugin.weka.Converter;
import cdms.core.Type;
import cdms.core.Value;


/**Class (with one static method) is designed to convert a time series type dataset (.arff format) to one where
 * the variables are copied and offset (i.e. for learning a DBN with a standard BN learner,
 * such as CaMML without the newer DBN code).<br>
 * If: <br>
 * TimeSeries = [ VarA, VarB, VarC ], then <br>
 * CopiedVariableData = [ VarA_0, VarB_0, VarC_0, VarA_1, VarB_1, VarC_1 ]
 * 
 * @author Alex Black
 */
public class DBNTimeseriesToCopiedVariableData {

	/**
	 * 
	 * @param inputARFF_path Input file/path for time series data (must end with .arff)
	 * @param exportARFF_path Output file/path for data set (must end with .arff)
	 * @param t0_suffix Suffix to append to variable names in first time slice - i.e. "_0"
	 * @param t1_suffix Suffix to append to variable names in second time slice - i.e. "_1"
	 */
	public static void timeseriesToCopiedVariables( String inputARFF_path, String exportARFF_path, String t0_suffix, String t1_suffix ) throws Exception
	{
		if( !inputARFF_path.endsWith(".arff") || !exportARFF_path.endsWith(".arff") ){
			throw new Exception("File path/names must end with .arff!");
		}
		if( t0_suffix == null || t1_suffix == null || t1_suffix.equals("") || t0_suffix.equals(t1_suffix) ){
			throw new Exception("Error in t0_suffix or t1_suffix (null, empty or equal)");
		}
		
		//Try opening the file:
		Value.Vector data = Converter.load( inputARFF_path , false, false);
		
		//Get some basic info about the data:
		String[] varNames = ((cdms.core.Type.Structured)((cdms.core.Type.Vector)data.t).elt).labels;
		int numVariables = varNames.length;
		
		//Determine the arity of each node:
		final Type.Structured datatype = (Type.Structured)((Type.Vector)data.t).elt;
		int[] arity = new int[ numVariables ];
		for( int i=0; i<numVariables; i++ ){
			Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];
            arity[i] = NeticaFn.makeValidNeticaNames(sType.ids,true).length;
		}
		
		//Create object for exporting:
		StringBuffer s = new StringBuffer();	//Main output string...
		
		//Add header information:
		String filename = null;
		for( int i=exportARFF_path.length()-1; i>=0; i-- ){
			if( exportARFF_path.charAt(i) == '\\' ){
				filename = exportARFF_path.substring(i+1, exportARFF_path.length() - ".arff".length() );
				break;
			}
		}
		s.append("@relation " + filename + "\n\n");
		
		//Create attribute (variable) lines:
			//t0 variables:
		for( int i=0; i<numVariables; i++ ){
			s.append("@attribute " + varNames[i] + t0_suffix);
			//append the set of possible values:
			s.append(" {");
			//Print the states, if appropriate: (Stolen from NeticaFN class)
            if (datatype.cmpnts[i] instanceof Type.Symbolic) {
                Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];                
                String str = Arrays.toString(NeticaFn.makeValidNeticaNames(sType.ids,true));                
                s.append( str.subSequence(1,str.length()-1) );
            }
			s.append("}\n");
		}
			//t1 variables:
		for( int i=0; i<numVariables; i++ ){
			s.append("@attribute " + varNames[i] + t1_suffix);
			//append the set of possible values:
			s.append(" {");
			//Print the states, if appropriate: (Stolen from NeticaFN class)
            if (datatype.cmpnts[i] instanceof Type.Symbolic) {
                Type.Symbolic sType = (Type.Symbolic)datatype.cmpnts[i];                
                String str = Arrays.toString(NeticaFn.makeValidNeticaNames(sType.ids,true));                
                s.append( str.subSequence(1,str.length()-1) );
            }
			s.append("}\n");
		}
		
		//Data:
		s.append("\n@data\n");
			//Loop through data instances 0...length-2
		int datalength = data.length();
		for( int i=0; i<datalength-1; i++ ){
			
			//Print the data for t0:
			for( int j=0; j< numVariables; j++ ){
				s.append( data.cmpnt(j).elt(i) + "," );
			}
			
			//Print the data for t1:
			for( int j=0; j< numVariables; j++ ){
				s.append( data.cmpnt(j).elt(i+1) );
				if( j != numVariables-1 ) s.append(",");
			}
			
			if( i != datalength-2 ) s.append("\n");
		}
		
		
		//Export the resulting string to a file:
		try {
            FileWriter out = new FileWriter(exportARFF_path);
            out.write(s.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
