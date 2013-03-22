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


