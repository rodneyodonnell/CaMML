package camml.core.newgui;

import java.text.DecimalFormat;

import javax.swing.table.AbstractTableModel;

import cdms.core.Type;
import cdms.core.Value;

/**A table model used only to translate the full search results into a
 * 	table format.
 * Currently only used in cammlGUI to display results.
 * Usage: myTable = new JTable( new FullResultsTableModel( fullSearchResultsFromCaMML ) ) 
 * 
 * @author Alex Black
 */
public class FullResultsTableModel extends AbstractTableModel{
	
	private static final DecimalFormat format4dp = new DecimalFormat("0.0000");		//Numbers to 4pt
	private static final DecimalFormat formatSmall = new DecimalFormat("0.0000E0");	//Scientific format for small numbers
	
	private Value.Vector results;
	private String[] colNames;
	
	public FullResultsTableModel( Value.Vector results ){
		this.results = results;
		colNames = ((Type.Structured)((Type.Vector)results.t).elt).labels;
	}
	
	public int getColumnCount() {
		//TODO: Find a better way - not sure if this works with datasets without names!
		return colNames.length;
	}

	public int getRowCount() {
		return results.length();
	}

	public Object getValueAt(int row, int col) {
		//return results.cmpnt(col).elt(row);
		String s = results.cmpnt(col).elt(row).toString();
		return formatString( s );
	}
	
	public String getColumnName( int col ){
		if( col > colNames.length ) return "";
		return colNames[ col ];
	}
	
	private static String formatString( String input ){
		double a;
		try{
			a = Double.parseDouble( input );
		} catch( Exception e ){		//Not a number...
			return input;
		}
		
		if( a < 0.001 ){
			return formatSmall.format( a );
		} else{
			return format4dp.format( a );
		}
	}

	
}


