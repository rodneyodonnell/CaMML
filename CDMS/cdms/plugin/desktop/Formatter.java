//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.desktop;

import cdms.core.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

/** A component for converting vectors of structured values into vectors of pairs whose
    first component is the output data and second component is the input data.
    @see Value.Model
*/
public class Formatter extends JSplitPane
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5666604870681222025L;

public static class FormatterFunction extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6898976168320057193L;

	public FormatterFunction()
    {
      super(new Type.Function(new Type.Vector(Type.TYPE, Type.STRUCTURED, false, false, false, false), Type.TRIV, false, false));
    }

    public Value apply(Value v)
    {
      OKPanel okp = new OKPanel(new Formatter((Value.Vector)v));
      DesktopFrame.makeWindow("Data Formatter", okp);
      return Value.TRIV;
    }
  }

  private static class OKPanel extends JPanel
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5829040569821660676L;
	private JButton okButton = new JButton("OK");
    private JPanel southPanel = new JPanel();
    private JPanel okPanel = new JPanel();
    public Formatter formatter;

    public OKPanel(Formatter f)
    {
      formatter = f;
      setLayout(new BorderLayout());
      add(southPanel, BorderLayout.SOUTH);
      add(f, BorderLayout.CENTER);
      southPanel.add(okPanel);
      okPanel.add(okButton);
      okButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          DesktopFrame.makeWindow("Formatted data", formatter.getData());
        }
      });
    }
  }

  /** Implement the <code>public WizardPanel getNext()</code> method to use the FormatterWizardPanel as part of a wizard. */
  public static abstract class FormatterWizardPanel extends Wizard.WizardPanel
  {
    private Formatter formatter;
  
    public FormatterWizardPanel(Value.Vector data)
    {
      super();
      formatter = new Formatter(data);
      add(formatter);
    }
  
    public Value.Vector getFormattedData()
    {
      return formatter.getData();
    }
  }

  private Value.Vector v;
  private Type.Structured elt;
  private JTable preview = new JTable();
  private JButton inputAdd = new JButton(">");
  private JButton inputRemove = new JButton("<");
  private JButton outputAdd = new JButton(">");
  private JButton outputRemove = new JButton("<");
  private JList inputList = new JList();
  private JList outputList = new JList();
  private JLabel inputLabel = new JLabel("Input");
  private JLabel outputLabel = new JLabel("Output");
  private JSplitPane ioPanel = new JSplitPane();
  private JPanel inputPanel = new JPanel();
  private JPanel outputPanel = new JPanel();
  private JPanel inputButtonPanel = new JPanel();
  private JPanel outputButtonPanel = new JPanel();
  private JScrollPane previewScroll = new JScrollPane();
  private JScrollPane inputScroll = new JScrollPane();
  private JScrollPane outputScroll = new JScrollPane();
  private JCheckBox outputCheckBox = new JCheckBox("Treat single column as structured", true);
  private JCheckBox inputCheckBox = new JCheckBox("Treat single column as structured", true);

  private java.util.Vector inputVector = new java.util.Vector();
  private java.util.Vector outputVector = new java.util.Vector();

  private VectorElement[] allElements;

  public Formatter(Value.Vector v)
  {
    this.v = v;
    elt = (Type.Structured)((Type.Vector)v.t).elt;
    allElements = new VectorElement[elt.cmpnts.length];
    int count;
    for(count = 0; count < allElements.length; count++)
    {
      allElements[count] = new VectorElement(count);
    }
    preview.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    preview.getTableHeader().setReorderingAllowed(false);
    inputList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    outputList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    preview.setModel(new FormatterTableModel(v));
    preview.setColumnSelectionAllowed(true);
    preview.setRowSelectionAllowed(false);
    preview.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

// BUTTONS..........

    inputAdd.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        int[] selectedColumns = preview.getSelectedColumns();
        Object[] newEntries = new Object[selectedColumns.length];
        int count2;
        for(count2 = 0; count2 < selectedColumns.length; count2++)
        {
          newEntries[count2] = allElements[columnToCmpnt(selectedColumns[count2])];
        }
        for(count2 = 0; count2 < selectedColumns.length; count2++)
        {
          inputVector.add(newEntries[count2]);
        }
        inputList.setListData(inputVector);
        ((FormatterTableModel)preview.getModel()).fireTableStructureChanged();
      }
    });

    inputRemove.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Object[] selectedValues = inputList.getSelectedValues();
        int count2;
        for(count2 = 0; count2 < selectedValues.length; count2++)
        {
          inputVector.remove(selectedValues[count2]);
        }
        inputList.setListData(inputVector);
        ((FormatterTableModel)preview.getModel()).fireTableStructureChanged();
      }
    });

    outputAdd.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        int[] selectedColumns = preview.getSelectedColumns();
        Object[] newEntries = new Object[selectedColumns.length];
        int count2;
        for(count2 = 0; count2 < selectedColumns.length; count2++)
        {
          newEntries[count2] = allElements[columnToCmpnt(selectedColumns[count2])];
        }
        for(count2 = 0; count2 < selectedColumns.length; count2++)
        {
          outputVector.add(newEntries[count2]);
        }
        outputList.setListData(outputVector);
        ((FormatterTableModel)preview.getModel()).fireTableStructureChanged();
      }
    });

    outputRemove.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Object[] selectedValues = outputList.getSelectedValues();
        int count2;
        for(count2 = 0; count2 < selectedValues.length; count2++)
        {
          outputVector.remove(selectedValues[count2]);
        }
        outputList.setListData(outputVector);
        ((FormatterTableModel)preview.getModel()).fireTableStructureChanged();
      }
    });

// LAYOUT...........

    previewScroll.setViewportView(preview);
    inputScroll.setViewportView(inputList);
    outputScroll.setViewportView(outputList);
    setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    previewScroll.setBorder(BorderFactory.createEtchedBorder());
    setLeftComponent(previewScroll);

    ioPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);

    inputPanel.setLayout(new BorderLayout());
    inputPanel.add(inputScroll, BorderLayout.CENTER);
    inputButtonPanel.setLayout(new GridLayout(2,1));
    inputButtonPanel.add(inputAdd);
    inputButtonPanel.add(inputRemove);
    inputPanel.add(inputButtonPanel, BorderLayout.WEST);
    inputPanel.add(inputLabel, BorderLayout.NORTH);
    inputPanel.add(inputCheckBox, BorderLayout.SOUTH);
    inputPanel.setBorder(BorderFactory.createEtchedBorder());

    outputPanel.setLayout(new BorderLayout());
    outputPanel.add(outputScroll, BorderLayout.CENTER);
    outputButtonPanel.setLayout(new GridLayout(2,1));
    outputButtonPanel.add(outputAdd);
    outputButtonPanel.add(outputRemove);
    outputPanel.add(outputButtonPanel, BorderLayout.WEST);
    outputPanel.add(outputLabel, BorderLayout.NORTH);
    outputPanel.add(outputCheckBox, BorderLayout.SOUTH);
    outputPanel.setBorder(BorderFactory.createEtchedBorder());

    ioPanel.setTopComponent(inputPanel);
    ioPanel.setBottomComponent(outputPanel);
    ioPanel.setBorder(BorderFactory.createEtchedBorder());

    setRightComponent(ioPanel);
  }

  private int columnToCmpnt(int columnNumber)
  {
    int index = columnNumber;
    int count;
    for(count = 0; count <= index; count++)
    {
      if((inputVector.contains(allElements[count]))||(outputVector.contains(allElements[count]))) index++;
    }
    return index;
  }

  private class FormatterTableModel extends AbstractTableModel
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2380059399073932570L;
	private Value.Vector v;

    public FormatterTableModel(Value.Vector v)
    {
      this.v = v;
    }

    public int getRowCount()
    {
      return java.lang.Math.min(10, v.length());
    }

    public int getColumnCount()
    {
      return elt.cmpnts.length - inputVector.size() - outputVector.size();
    }

    public Object getValueAt(int row, int column)
    {
      return ((Value.Structured)v.elt(row)).cmpnt(columnToCmpnt(column));
    }

    public String getColumnName(int column)
    {
      return elt.labels[columnToCmpnt(column)];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return false;
    } 
  }

  private class VectorElement
  {
    public int cmpntId;

    public VectorElement(int id)
    {
      cmpntId = id;
    }

    public String toString()
    {
      if(elt.labels != null)
      {
        if(elt.labels[cmpntId] != null)
        {
          return elt.labels[cmpntId];
        }
      }
      return "Column " + cmpntId;
    }
  }

  public Type getInputType()
  {
    if((inputVector.size() == 1)&&(!inputCheckBox.isSelected()))
    {
      return elt.cmpnts[((VectorElement)inputVector.elementAt(0)).cmpntId];
    }
    else
    {
      Type[] types = new Type[inputVector.size()];
      String[] labels = new String[inputVector.size()];
      boolean[] checkNames = new boolean[inputVector.size()];
      int count, id;
      for(count = 0; count < inputVector.size(); count++)
      {
          id = ((VectorElement)inputVector.elementAt(count)).cmpntId;
          types[count] = elt.cmpnts[id];
          labels[count] = ((VectorElement)inputVector.elementAt(count)).toString();//elt.labels[id];
          checkNames[count] = elt.checkCmpntsNames[id];
      }
      return new Type.Structured(types, labels, checkNames);
    }
  }

  public Type getOutputType()
  {
    if((outputVector.size() == 1)&&(!outputCheckBox.isSelected()))
    {
      return elt.cmpnts[((VectorElement)outputVector.elementAt(0)).cmpntId];
    }
    else
    {
      Type[] types = new Type[outputVector.size()];
      String[] labels = new String[outputVector.size()];
      boolean[] checkNames = new boolean[outputVector.size()];
      int count, id;
      for(count = 0; count < outputVector.size(); count++)
      {
          id = ((VectorElement)outputVector.elementAt(count)).cmpntId;
          types[count] = elt.cmpnts[id];
          labels[count] = ((VectorElement)outputVector.elementAt(count)).toString();//elt.labels[id];
          checkNames[count] = elt.checkCmpntsNames[id];
      }
      return new Type.Structured(types, labels, checkNames);
    }
  }

  public Type.Structured getTotalType()
  {
    String outputLabel, inputLabel;
    if((outputVector.size() == 1)&&(!outputCheckBox.isSelected())) outputLabel = ((VectorElement)outputVector.elementAt(0)).toString();
    else outputLabel = "Output";
    if((inputVector.size() == 1)&&(!inputCheckBox.isSelected())) inputLabel = ((VectorElement)inputVector.elementAt(0)).toString();
    else inputLabel = "Input";

    return new Type.Structured(new Type[]{getOutputType(), getInputType()},
                               new String[]{outputLabel, inputLabel},
                               new boolean[]{false, false});
  }

  private Value getInputValue(int eltNumber)
  {
    if((inputVector.size() == 1)&&(!inputCheckBox.isSelected()))
    {
      return ((Value.Structured)v.elt(eltNumber)).cmpnt(((VectorElement)inputVector.elementAt(0)).cmpntId);
    }
    else
    {
      return new InputStructured(eltNumber);
    }
  }

  private Value getOutputValue(int eltNumber)
  {
    if((outputVector.size() == 1)&&(!outputCheckBox.isSelected()))
    {
      return ((Value.Structured)v.elt(eltNumber)).cmpnt(((VectorElement)outputVector.elementAt(0)).cmpntId);
    }
    else
    {
      return new OutputStructured(eltNumber);
    }
  }

  private class InputStructured extends Value.Structured
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8688778022970330980L;
	private int eltNumber;

    public InputStructured(int eltNumber)
    {
      super((Type.Structured)getInputType());
      this.eltNumber = eltNumber;
    }

    public Value cmpnt(int cmpnt)
    {
      Value.Structured vs = (Value.Structured)v.elt(eltNumber);
      return vs.cmpnt(((VectorElement)inputVector.elementAt(cmpnt)).cmpntId);
    }
 
    public int length()
    {
      return inputVector.size();
    }
  }

  private class OutputStructured extends Value.Structured
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6288306479749614936L;
	private int eltNumber;

    public OutputStructured(int eltNumber)
    {
      super((Type.Structured)getOutputType());
      this.eltNumber = eltNumber;
    }

    public Value cmpnt(int cmpnt)
    {
      Value.Structured vs = (Value.Structured)v.elt(eltNumber);
      return vs.cmpnt(((VectorElement)outputVector.elementAt(cmpnt)).cmpntId);
    }
 
    public int length()
    {
      return outputVector.size();
    }
  }

  private class TotalStructured extends Value.Structured
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -597198192987318738L;
	private int eltNumber;

    public TotalStructured(int eltNumber)
    {
      super(getTotalType());
      this.eltNumber = eltNumber;
    }

    public int length()
    {
      return 2;
    }

    public Value cmpnt(int i)
    {
      if(i == 0)
      {
        return getOutputValue(eltNumber);
      }
      else
      {
        return getInputValue(eltNumber);
      }
    }
  }

  private class InputVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8302201028550705885L;

	public InputVector()
    {
      super(new Type.Vector(Type.TYPE, 
                            getInputType(), 
                            ((Type.Vector)v.t).ckIsSequence, 
                            ((Type.Vector)v.t).isSequence, 
                            ((Type.Vector)v.t).checkEltName, 
                            ((Type.Vector)v.t).checkIndexName));
    }

    public int length()
    {
      return v.length();
    }

    public Value elt(int i)
    {
      return getInputValue(i);
    }
  }

  private class OutputVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3491929082081075498L;

	public OutputVector()
    {
      super(new Type.Vector(Type.TYPE, 
                            getOutputType(), 
                            ((Type.Vector)v.t).ckIsSequence, 
                            ((Type.Vector)v.t).isSequence, 
                            ((Type.Vector)v.t).checkEltName, 
                            ((Type.Vector)v.t).checkIndexName));
    }

    public int length()
    {
      return v.length();
    }

    public Value elt(int i)
    {
      return getOutputValue(i);
    }
  }

  public class TotalVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5616038132628530067L;

	public TotalVector()
    {
      super(new Type.Vector(Type.TYPE,
                            getTotalType(),
                            ((Type.Vector)v.t).ckIsSequence, 
                            ((Type.Vector)v.t).isSequence, 
                            ((Type.Vector)v.t).checkEltName, 
                            ((Type.Vector)v.t).checkIndexName));
    }

    public int length()
    {
      return v.length();
    }

    public Value elt(int i)
    {
      return new TotalStructured(i);
    }
  }

  public Value.Vector getInputData()
  {
    return new InputVector();
  }

  public Value.Vector getOutputData()
  {
    return new OutputVector();
  }

  public Value.Vector getData()
  {
    return new TotalVector();
  }
}
