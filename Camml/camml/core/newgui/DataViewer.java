package camml.core.newgui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import cdms.core.Value;
import cdms.core.Type;

/**Very basic data viewer.
 * Basically a JFrame with a JTable in it that displays the contents of the
 * 	selected data.
 * 
 * @author Alex Black
 */
public class DataViewer extends JFrame{
	
	private final Value.Vector data;
	String[] colNames;
	TableModel tableModel;
	JTable table;
	
	public DataViewer( Value.Vector data ){
		this.data = data;
		
		colNames = ((Type.Structured)((Type.Vector)data.t).elt).labels;
		
		tableModel = new DataTable();
		table = new JTable( tableModel );
		add( new JScrollPane( table ) );
		
	}
	
	
	public class DataTable extends AbstractTableModel{
		public int getColumnCount() {
			//TODO: Find a better way - not sure if this works with datasets without names!
			return colNames.length;
		}

		public int getRowCount() {
			return data.length();
		}

		public Object getValueAt(int row, int col) {
			return data.cmpnt(col).elt(row);
		}
		
		public String getColumnName( int col ){
			return colNames[ col ];
		}
		
	}

}
