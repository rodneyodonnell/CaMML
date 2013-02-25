package camml.core.newgui;

import javax.swing.table.AbstractTableModel;
import camml.core.search.MMLEC;
import cdms.core.Value;

/**A table model used only to translate the full search results into a
 * 	table format.
 * Currently only used in cammlGUI to display results.
 * Usage: myTable = new JTable( new FullResultsTableModel( fullSearchResultsFromCaMML ) ) 
 * 
 * @author Alex Black
 */
public class FullResultsTableModel extends AbstractTableModel{
	private static final long serialVersionUID = -1579190707379509629L;
	
	protected boolean isDBNResults;
	protected Value.Vector results;
	protected MMLEC[] resultsDBN;
	protected String[] colNames = new String[]{"Num SECs", "Posterior", "Relative Prior", "Best MML score", "Weight"};
	
	/**Constructor: Expects results from MetropolisSearch.getResults()
	 */
	public FullResultsTableModel( Value.Vector results ){
		this.results = results;
		this.resultsDBN = null;
		isDBNResults = false;
	}
	
	/**Constructor: Expects results from MetropolisSearchDBN.getResultsMMLEC()*/
	public FullResultsTableModel( MMLEC[] resultsDBN ){
		this.results = null;
		this.resultsDBN = resultsDBN;
		
		isDBNResults = true;
	}
	
	public int getColumnCount() {
		//return colNames.length;
		return 5;
	}

	public int getRowCount() {
		if( isDBNResults ) return resultsDBN.length;
		else return results.length();
	}

	/**Used to get the actual values to put into the table, for a given position.
	 * Formats the results using the format objects in GUIParameters
	 */
	public Object getValueAt(int row, int col) {
		MMLEC.MMLECStructure m;
		if( !isDBNResults ){	//Standard BN results (Value.Vector) in 'results'
			m = (MMLEC.MMLECStructure)results.elt(row);
		} else {			//DBN results (MMLEC[]) in 'resultsDBN'
			m = resultsDBN[row].new MMLECStructure();
		}
		
		//SECs, posterior, relative prior, best MML, weight
		switch( col ){
			case 0:		//SECs
				return m.cmpnt(col).toString();
			case 1:		//Posterior
				try{ 
					return GUIParameters.formatPosterior.format( m.doubleCmpnt(col) );
				} catch( Exception e ){	}
				break;
			case 2:		//Relative prior
				try{ 
					return GUIParameters.formatRelativePrior.format( m.doubleCmpnt(col) );
				} catch( Exception e ){	}
				break;
			case 3:		//Best MML
				try{ 
					return GUIParameters.formatBestMML.format( m.doubleCmpnt(col) );
				} catch( Exception e ){	}
				break;
			case 4:		//Weight
				try{ 
					return GUIParameters.formatWeight.format( m.doubleCmpnt(col) );
				} catch( Exception e ){	}
				break;
		}
		return m.cmpnt(col).toString();
	}
	
	public String getColumnName( int col ){
		if( col > colNames.length ) return "";
		return colNames[ col ];
	}
	
}


