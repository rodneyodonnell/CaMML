//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567


// File: Browser.java
// Author: leighf@csse.monash.edu.au

package cdms.plugin.desktop;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import cdms.core.*;

/** A component for viewing and editing values.  Vectors values are displayed in a table.
    Vectors of structured values are displayed in a grid.  Obj values are displayed
    if they are descendants of java.awt.Component.  All other values are displayed using
    their string representation.
*/
public class Browser extends JPanel implements Menu.ValueProducer
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2731495845855011082L;
JTable table;
  JLabel southLabel;
  JScrollPane scrollPane;
  Value subject;
  CDMSDataModel dataModel;
  Menu.PopupMenu menu;

  public Browser()
  {
    super(new BorderLayout());

    table = new JTable();
    table.setShowHorizontalLines(true);
    table.setShowVerticalLines(false);
    table.setRowSelectionAllowed(false);
    table.setColumnSelectionAllowed(true);
    table.getTableHeader().setReorderingAllowed(false);
    table.setCellSelectionEnabled(true);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.getTableHeader().addMouseListener(new ColumnSelector());
    table.addMouseListener(new PopupListener());
    ListSelectionListener lsl = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) 
      {
        int cols = table.getSelectedColumns().length;
        int rows = table.getSelectedRows().length;
        if ((cols == 0 && rows == 0) || 
            (cols == table.getColumnCount() && rows == table.getRowCount()))
          southLabel.setText("All " + table.getColumnCount() + "x" + 
                             table.getRowCount() + " cells selected");
        else
          southLabel.setText("Only" + table.getSelectedColumns().length + "x" + 
                             table.getSelectedRows().length + " cells selected");
      }
    };
    table.getSelectionModel().addListSelectionListener(lsl);
    table.getColumnModel().getSelectionModel().addListSelectionListener(lsl);

    scrollPane = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    southLabel = new JLabel("");
    add(scrollPane,BorderLayout.CENTER);
    add(southLabel,BorderLayout.SOUTH);
  }

  /** Set the environment menu for the browser.  @see Menu#MENU. */ 
  public void setEnvMenu(Value.Vector v)
  {
    menu = new Menu.PopupMenu("",this,v,null);
  }

  private class PopupListener extends MouseAdapter 
  {
    public void mousePressed(MouseEvent e) 
    {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) 
    {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) 
    {
      if (menu != null && e.isPopupTrigger()) 
      {
        menu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }


  // ColumnSelector allows columns to be selected by clicking
  // on the column header (possible with ctrl or shift)
  // Does not allow dragging selection of columns, possible interfer
  // with reordering of columns(?)
  private class ColumnSelector extends MouseAdapter
  {
    public void mouseClicked(MouseEvent e) {
      JTableHeader header = table.getTableHeader();
      TableColumnModel cols = header.getColumnModel();

      if (!cols.getColumnSelectionAllowed()) return;

      int col = header.columnAtPoint(e.getPoint());
      if (col == -1) return;

      int count = table.getRowCount();
      if (count != 0) table.setRowSelectionInterval(0, count - 1);

      ListSelectionModel selection = cols.getSelectionModel();

      if (e.isShiftDown()) {
        int anchor = selection.getAnchorSelectionIndex();
        int lead = selection.getLeadSelectionIndex();

        if (anchor != -1) {
          boolean old = selection.getValueIsAdjusting();
          selection.setValueIsAdjusting(true);

          boolean anchorSelected = selection.isSelectedIndex(anchor);

          if (lead != -1) {
            if (anchorSelected)
              selection.removeSelectionInterval(anchor, lead);
            else
              selection.addSelectionInterval(anchor, lead);
            // The latter is quite unintuitive.
          }

          if (anchorSelected)
            selection.addSelectionInterval(anchor, col);
          else
            selection.removeSelectionInterval(anchor, col);

          selection.setValueIsAdjusting(old);
        }
        else
          selection.setSelectionInterval(col, col);
      } else if (e.isControlDown()) {
        if (selection.isSelectedIndex(col))
            selection.removeSelectionInterval(col, col);
        else
            selection.addSelectionInterval(col, col);
      } else {
        selection.setSelectionInterval(col, col);
      }
    }
  }

  /** Returns the currently selected data as a Value. */
  public Value getApplyValue()
  {
    if (subject instanceof Value.Vector)
      return dataModel.getApplyValue();
    else return subject;
  }

  /** Returns the Type of the currently selected data. */
  public Type getApplyType()
  {
    if (subject instanceof Value.Vector)
      return dataModel.getApplyType();
    else return subject.t;
  }

  /** Sets the value that the browser is viewing. */
  public void setSubject(Value subject)
  {
    this.subject = subject;

    if (subject instanceof Value.Vector)
    {
      dataModel = new VectorDataModel(subject);
      table.setModel(dataModel);
      if (dataModel.getRowCount() != 0 && dataModel.getColumnCount() != 0) 
      {
        table.setColumnSelectionInterval(0, dataModel.getColumnCount() - 1);
        table.setRowSelectionInterval(0, dataModel.getRowCount() - 1);
      }
    }
    else
    if (subject instanceof Value.Obj)
    {
      Object o = ((Value.Obj) subject).getObj();
      if (o instanceof java.awt.Component && o != null)
      {
        ((java.awt.Component) o).addMouseListener(new PopupListener());
        scrollPane.setViewportView((java.awt.Component) o);
      }
      else
      {
        JTextArea ta = new JTextArea(subject.toString());
        ta.addMouseListener(new PopupListener());
        scrollPane.setViewportView(ta);
      }
    }
    else 
    {
      JTextArea ta = new JTextArea(subject.toString());
      ta.addMouseListener(new PopupListener());
      scrollPane.setViewportView(ta);
    }
  }

  private abstract class CDMSDataModel extends AbstractTableModel  
  {
    public abstract Value getApplyValue();
    public abstract Type getApplyType();
  }

  private class VectorDataModel extends CDMSDataModel  
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4133390296123167015L;
	boolean seenEdit = false;
    boolean isStructured;
    Value.Vector subject;
    Hashtable hash = new Hashtable();     // A hashtable of changes.

    public VectorDataModel(Value subject)
    {
      this.subject = (Value.Vector) subject;
      isStructured = ((Type.Vector) subject.t).elt instanceof Type.Structured;
    }

    public java.lang.Object getValueAt(int iRowIndex, int iColumnIndex)
    {
      if (seenEdit)
      {
        java.lang.Object v = hash.get(iColumnIndex + "," + iRowIndex);
        if (v != null) return v;
      }
  
      // The following test should be implemented more efficiently.
      Type eltType;
      if (isStructured) 
        eltType = ((Type.Structured) ((Type.Vector) subject.t).elt).cmpnts[iColumnIndex];
      else eltType = ((Type.Vector) subject.t).elt;

      if (eltType instanceof Type.Symbolic)
      {
        if (isStructured)
          return ((Type.Symbolic) eltType).int2string( 
                   ((Value.Discrete) ((Value.Structured) 
                     subject.elt(iRowIndex)).cmpnt(iColumnIndex)).getDiscrete());
        else return ((Type.Symbolic) eltType).int2string(subject.intAt(iRowIndex));
      }
      else
      {
        Value v;
        if (isStructured) v = ((Value.Structured) subject.elt(iRowIndex)).cmpnt(iColumnIndex);
          else v = subject.elt(iRowIndex);
        ValueStatus vs = v.status();
        if ((vs == Value.S_NA)||(vs == Value.S_PROPER)) return v;   // Return Value.
          else return vs;   // Return the ValueStatus.
      }
    }

    /** Overriden to allow editing. */
    public boolean isCellEditable(int rowIndex, int columnIndex) 
    {
      return true;
    }

    public void setValueAt(java.lang.Object aValue,int iRowIndex,int iColumnIndex) 
    {
      /** Perhaps a faster hash key could be implemented one day. */
      hash.put(iColumnIndex + "," + iRowIndex,aValue); 
      seenEdit = true;
    }
        
    public int getColumnCount()
    {
      if (isStructured)
        return ((Type.Structured) ((Type.Vector) subject.t).elt).cmpnts.length;
      else return 1;
    }       

    public int getRowCount()
    {
      return subject.length();
    }       

    public String getColumnName(int column) 
    {
      if (isStructured)
      {
        // Check if component or Type has a name.
        Type.Structured colType = (Type.Structured) ((Type.Vector) subject.t).elt;
        
        if (colType.labels != null && colType.labels[column] != null) 
        {
          return colType.labels[column];
        }
        else
        {
          String colName = Type.byType(colType.cmpnts[column]);
          if (colName != null) return colName;
            else return colType.cmpnts[column].getTypeName();
        }
      }
      else return ((Type.Vector) subject.t).elt.getTypeName();
    }

    /* Zip selected subranges together. */
    public Value.Function zip = new VectorFN.Zip();

    public Value getApplyValue()
    {
      int[] selcols = table.getSelectedColumns();
      int[] selrows = table.getSelectedRows();
      int i;

      if (selcols.length <= 0) return subject;

      int minrow = Integer.MAX_VALUE;
      int maxrow = 0;
      for (i = 0; i < selrows.length; i++)
      {
        if (selrows[i] < minrow) minrow = selrows[i];
        if (selrows[i] > maxrow) maxrow = selrows[i];
      }

      if (isStructured)
      {
        Value[] cmpnts = new Value[selcols.length];
        String[] names = new String[selcols.length];
        for (i = 0; i < selcols.length; i++)
        {
          cmpnts[i] = ((Value.Vector) subject).cmpnt(selcols[i]).sub(minrow,maxrow); 
          Type.Structured cmpntType = (Type.Structured) ((Type.Vector) subject.t).elt;
          if (cmpntType.labels != null) names[i] = cmpntType.labels[selcols[i]];
        }

        return zip.apply(new Value.DefStructured(cmpnts,names));
      }
      else
      {
        return subject.sub(minrow,maxrow); 
      }
    }


    public Type getApplyType()
    {
      int[] selcols = table.getSelectedColumns();

      if (selcols.length <= 0) return subject.t;

      if (isStructured)
      {
        Type.Structured colType = (Type.Structured) ((Type.Vector) subject.t).elt;
        Type[] cmpnts = new Type[selcols.length];
        String[] names = null;
        if (colType.labels != null) names = new String[selcols.length];
        for (int i = 0; i < selcols.length; i++)
        {
          cmpnts[i] = colType.cmpnts[selcols[i]];  // Assumes direct index mapping. 
          if (colType.labels != null)
            names[i] = colType.labels[selcols[i]];
        }
        
        return new Type.Vector(Type.DISCRETE,
                               new Type.Structured(cmpnts,names,
                                     Value.DefStructured.booleanArray(false,cmpnts.length)),
                               false,false,false,false);
      }
      else return ((Type.Vector) subject.t).elt;
    }

  }

}
