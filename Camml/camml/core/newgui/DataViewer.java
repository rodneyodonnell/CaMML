package camml.core.newgui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import cdms.core.Value;

/**Very basic data viewer.
 * Basically a JFrame with a JTable in it that displays the contents of the
 * 	selected data.
 * @author Alex Black
 */
public class DataViewer extends JFrame{
	private static final long serialVersionUID = 3193733811587674115L;
	
	private final Value.Vector data;
	String[] colNames;
	TableModel tableModel;
	JTable table;
	
	/**Constructor for the data viewer. Expects a CDMS Value.Vector data
	 * object - i.e. what CaMML uses to represent data.
	 */
	public DataViewer( Value.Vector data ){
		this.data = data;
		
		colNames = ((cdms.core.Type.Structured)((cdms.core.Type.Vector)data.t).elt).labels;
		
		tableModel = new DataTable();
		table = new JTable( tableModel );
		add( new JScrollPane( table ) );
	}
	
	
	public class DataTable extends AbstractTableModel{
		private static final long serialVersionUID = 9173322447957720914L;

		public int getColumnCount() {
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
