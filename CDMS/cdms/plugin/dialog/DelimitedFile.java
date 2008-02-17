//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: DelimitedFile.java
// Authors: {joshc,leighf}@csse.monash.edu.au
  
package cdms.plugin.dialog;
  
import javax.swing.*;
import java.awt.*;
import cdms.core.*;
import java.awt.event.*;
import java.io.*;
  
  
public class DelimitedFile
{

  // Str -> (ctypes,...)
  public static LoadDelimitedFileGui loadDelimitedFileGui = new LoadDelimitedFileGui();
  public static class LoadDelimitedFileGui extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8449672636700052572L;
	public static final Type.Function TT =  
      new Type.Function(Type.STRING,IO.LoadDelimitedFile.partParamType);

    public LoadDelimitedFileGui()
    {
       super(TT); 
    }

    public Value apply(Value param)
    {
      String fname = ((Value.Str) param).getString();
      LoadDelimitedFileDialog loadDelimitedFileDialog = new LoadDelimitedFileDialog(fname);
      loadDelimitedFileDialog.setVisible(true);
      if (loadDelimitedFileDialog.cancelled)  // user pressed cancel.
      {
        return Value.TRIV;
      }
    
      // Create types - use serialization until type system parser available.  LF.
      try
      {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream p = new ObjectOutputStream(os);
        p.writeObject(loadDelimitedFileDialog.ctypes);
        os.close();

        return new Value.DefStructured(IO.LoadDelimitedFile.partParamType, new Value[] {
          new Value.Str(os.toString()),
          new Value.Str(loadDelimitedFileDialog.unobservedStr),
          new Value.Str(loadDelimitedFileDialog.irrelevantStr),
          new Value.Str(loadDelimitedFileDialog.startDelim),
          new Value.Str(loadDelimitedFileDialog.endDelim),
          new Value.Str(loadDelimitedFileDialog.wordDelim),
          loadDelimitedFileDialog.quotes ? new Value.Str(String.valueOf(loadDelimitedFileDialog.quoteChar)) : new Value.Str(""),
          loadDelimitedFileDialog.consume ? Value.TRUE : Value.FALSE,
          loadDelimitedFileDialog.titleLine ? Value.TRUE : Value.FALSE });
      }
      catch (Exception e)
      {
        System.out.println("Error serializing types.");
        e.printStackTrace();
        return Value.TRIV;
      }
    }
  }

  
  /** This class is a JDialog prompting the user to provide information
      for the parsing of a text file.  Delimiting strings, Type information
      of columns, presence of quotes, representation of unobserved and irrelevent 
      values are all chosen here. */
    private static class LoadDelimitedFileDialog extends CDMSDialog
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8178307033958597759L;
	JPanel northPanel = new JPanel();
      JPanel southPanel = new JPanel();
      JPanel startEndPanel = new JPanel();
      JPanel leftPanel = new JPanel();
      JPanel delimiterPanel = new JPanel();
      JPanel labelPanel = new JPanel();
      JPanel fieldPanel = new JPanel();
      JPanel okPanel = new JPanel();
      JPanel cancelPanel = new JPanel();
      JPanel selectTypePanel = new JPanel();

      JButton selectTypeButton = new JButton("Select Type...");
      FilePreviewTableModel dm;
      JTable table;
      JScrollPane scroll;
      JCheckBox quotedBox = new JCheckBox();
      JCheckBox consumeBox = new JCheckBox();
      JCheckBox titleBox = new JCheckBox();
      JTextField lineStartField  = new JTextField(10);
      JTextField lineEndField = new JTextField(10);
      JTextField delimiterField = new JTextField(10);
      JLabel lineStartLabel = new JLabel("Line Start String", javax.swing.SwingConstants.RIGHT);
      JLabel lineEndLabel = new JLabel("Line End String", javax.swing.SwingConstants.RIGHT);
      JLabel delimiterLabel = new JLabel("Field Separator", javax.swing.SwingConstants.RIGHT);
      JButton okButton = new JButton("OK");
      JButton cancelButton = new JButton("Cancel");
      JLabel unobservedLabel = new JLabel("Unobserved Value", javax.swing.SwingConstants.RIGHT);
      JLabel irrelLabel = new JLabel("Irrelevant Value", javax.swing.SwingConstants.RIGHT);
      JTextField irrelField = new JTextField(10);
      JTextField unobservedField = new JTextField(10);
  
      private javax.swing.FocusManager oldManager = javax.swing.FocusManager.getCurrentManager();

      public String unobservedStr;
      public String irrelevantStr;
      public String startDelim;
      public String endDelim;
      public String wordDelim;
      public boolean quotes;
      public boolean consume;
      public boolean titleLine;
      public char quoteChar;
      private Type[] tmp;
      public Type.Structured ctypes;
      public String fname;
      public boolean cancelled;
      private boolean tab = false;
  
      public LoadDelimitedFileDialog(String fname) 
      {
        super("Load Data from " + fname);
        pack();
        javax.swing.FocusManager.setCurrentManager(new LDFDFocusManager());
        this.tmp = new Type[1];
        tmp[0] = Type.STRING;
        ctypes = new Type.Structured(tmp,new boolean[] { false });        
        this.fname = fname;
        unobservedStr = "";
        irrelevantStr = "";
        startDelim = "";
        endDelim = ""; 
        wordDelim = ",";
        quotes = false;
        consume = true;
        titleLine = false;
        quoteChar = '\"';
        cancelled = false;
        dm = new FilePreviewTableModel(this,5);
        table =  new JTable(dm);
  
        try
        {
          consumeBox.setSelected(true);
          titleBox.setSelected(titleLine);
          quoteChar = this.autoDetectQuotes();
          if(quoteChar == '\0')
          {
            quotes = false;
            quotedBox.setSelected(false);
            wordDelim = this.autoDetectDelimiter();
          }
          else
          {
            quotes = true;
            quotedBox.setSelected(true);
          }
          jbInit();
          if(tab)
          {
            delimiterField.setForeground(Color.red);
            delimiterField.setText("<TAB>");
          }
          else
          {
            delimiterField.setText(this.wordDelim);
          }
          lineStartField.setText(this.startDelim);
          lineEndField.setText(this.endDelim);
          irrelField.setText(this.irrelevantStr);
          unobservedField.setText(this.unobservedStr);
        }
        catch(Exception e) 
        {
          e.printStackTrace();
        }
        setTitle("Load Data from " + fname);
        pack();
        center();
      }
      
      private void jbInit() throws Exception
      {
        selectTypeButton.addActionListener(new java.awt.event.ActionListener() 
        {
          public void actionPerformed(ActionEvent e) 
          {
            selectTypeButton_actionPerformed(e);
          }
        });
        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(false);
        table.setBorder(BorderFactory.createEtchedBorder());
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        table.setSelectionBackground(java.awt.Color.cyan);
        table.getTableHeader().setReorderingAllowed(false);
        DefaultListSelectionModel lsm = new DefaultListSelectionModel();
        lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().setSelectionModel(lsm);
  
        consumeBox.addActionListener(new java.awt.event.ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            updateFields();
            autoDetectColumnTypes(true);
          }
        });
        titleBox.addActionListener(new java.awt.event.ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            updateFields();
            autoDetectColumnTypes(true);
          }
        });
        quotedBox.addActionListener(new java.awt.event.ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            updateFields();
            autoDetectColumnTypes(true);
          }
        });
        lineStartField.addKeyListener(new java.awt.event.KeyListener() 
        {
          public void keyTyped(KeyEvent e){}
          public void keyPressed(KeyEvent e){}
          public void keyReleased(KeyEvent e)
          {
            updateFields();
            autoDetectColumnTypes(true);
          }
        });
  
        lineEndField.addKeyListener(new java.awt.event.KeyListener() 
        {
          public void keyTyped(KeyEvent e){}
          public void keyPressed(KeyEvent e){}
          public void keyReleased(KeyEvent e)
          {
            updateFields();
            autoDetectColumnTypes(true);
          }
        });
  
        delimiterField.addKeyListener(new java.awt.event.KeyListener() 
        {
          public void keyTyped(KeyEvent e){}
          public void keyPressed(KeyEvent e)
          {
            if(e.getKeyCode() == KeyEvent.VK_TAB)
            {
              tab = true;
              delimiterField.setForeground(Color.red);
              delimiterField.setText("<TAB>");
            }
            else
            {
              tab = false;
              delimiterField.setForeground(Color.black);
              System.out.println(e.getKeyCode() + ", " + KeyEvent.VK_TAB + ", " + KeyEvent.VK_UNDEFINED);
            }
          }
          public void keyReleased(KeyEvent e)
          {
            updateFields();
            autoDetectColumnTypes(true);
          }
        });
  
        okButton.addActionListener(new java.awt.event.ActionListener() 
        {
          public void actionPerformed(ActionEvent e) 
          {
            okButton_actionPerformed(e);
          }
        });
  
        cancelButton.addActionListener(new java.awt.event.ActionListener() 
        {
          public void actionPerformed(ActionEvent e) 
          {
            cancelButton_actionPerformed(e);
          }
        });
  
        consumeBox.setText("Consume consecutive delimiters");
        quotedBox.setText("Quoted");
        titleBox.setText("Title Line");
        table.setToolTipText("File preview");
        scroll = new JScrollPane(table);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(northPanel, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(southPanel, BorderLayout.SOUTH);
        northPanel.setLayout(new BorderLayout());
        northPanel.add(leftPanel, BorderLayout.WEST);
        northPanel.add(startEndPanel, BorderLayout.CENTER);
        leftPanel.setLayout(new GridLayout(4,1));
        leftPanel.add(delimiterPanel);
        leftPanel.add(titleBox);
        leftPanel.add(consumeBox);
        leftPanel.add(quotedBox);
        delimiterPanel.setLayout(new BorderLayout());
        delimiterPanel.add(delimiterLabel, BorderLayout.WEST);
        delimiterPanel.add(delimiterField, BorderLayout.CENTER);
        startEndPanel.setLayout(new BorderLayout());
        startEndPanel.add(labelPanel, BorderLayout.WEST);
        startEndPanel.add(fieldPanel, BorderLayout.CENTER);
        labelPanel.setLayout(new GridLayout(4,1));
        labelPanel.add(lineStartLabel);
        labelPanel.add(lineEndLabel);
        labelPanel.add(irrelLabel);
        labelPanel.add(unobservedLabel);
        fieldPanel.setLayout(new GridLayout(4,1));
        fieldPanel.add(lineStartField);
        fieldPanel.add(lineEndField);
        fieldPanel.add(irrelField);
        fieldPanel.add(unobservedField);
        southPanel.setLayout(new GridLayout(1,3));
        southPanel.add(selectTypePanel);
        southPanel.add(okPanel);
        southPanel.add(cancelPanel);
        selectTypePanel.add(selectTypeButton);
        okPanel.add(okButton);
        cancelPanel.add(cancelButton);

        updateFields();
        autoDetectColumnTypes(true);
      }
  
      void okButton_actionPerformed(ActionEvent e) {
        updateFields();
        setVisible(false);
        javax.swing.FocusManager.setCurrentManager(oldManager);
        dispose();
      }
  
      void selectTypeButton_actionPerformed(ActionEvent e) 
      {
        int count;
        int[] columns;
        Type tmp2;
        if (table.getSelectedColumn() != -1)
        {
          tmp2 = (Type)(new cdms.plugin.dialog.SelectDialog("Select Column Type...", Type.TYPE, null, false)).getResult();
          if(tmp2 != null)                                                         // User didn't press "cancel" on the typeSelectorDialog...
          {
            columns = this.table.getSelectedColumns();
            for(count = 0; count < columns.length; count++)
            {
              this.tmp[columns[count]] = tmp2;
            }
          }
        }
        updateColumnTypes();
      }
  
      void updateFields()
      {
        irrelevantStr = irrelField.getText();
        unobservedStr = unobservedField.getText();
        startDelim = lineStartField.getText();
        endDelim = lineEndField.getText();
        if(!this.delimiterField.getText().equals(""))
        {
          wordDelim = delimiterField.getText();
          if(tab)
          {
            wordDelim = "\t";
          }
        }
        quotes = quotedBox.isSelected();
        consume = consumeBox.isSelected();
        titleLine = titleBox.isSelected();
        updateColumnTypes();
      }
  
      void updateColumnTypes()
      {
        ctypes = new Type.Structured(tmp, Value.DefStructured.booleanArray(false,tmp.length));
        dm.fireTableDataChanged();
        dm.fireTableStructureChanged();
      }
  
      void autoDetectColumnTypes(boolean showChange)
      {
        dm.fireTableStructureChanged();
        this.tmp = new Type[this.table.getColumnCount()];
        int count;
        int rowCount;
        int offset = 0;
        String val;
  
        if(titleLine)
        {
          offset++;
        }
  
        for(count = 0; count < this.table.getColumnCount(); count++)
        {
          this.tmp[count] = Type.DISCRETE;
          for(rowCount = offset; (rowCount < this.table.getRowCount()) && (this.tmp[count] != Type.STRING); rowCount++)
          {
            val = (String)this.table.getValueAt(rowCount, count);
  
            if(this.tmp[count] == Type.CONTINUOUS)            // If we think the column is CONTINUOUS....
            {
              try                                                  // Try to make it a double.
              {
            	  Double.parseDouble(val);
              }
              catch(NumberFormatException e)                       // If that fails, make it a string.
              {
                this.tmp[count] = Type.STRING;
              }
            }
            else
            {
              if(this.tmp[count] == Type.DISCRETE)         // If we think the column is DISCRETE....
              {
                if(val.indexOf('.') != -1)                         // If there is a decimal point...
                {
                  try                                                         // try to make it a double.
                  {
                    Double.parseDouble(val);
                    this.tmp[count] = Type.CONTINUOUS;
                  }
                  catch(NumberFormatException e)                              // if that fails, make it a string.
                  {
                    this.tmp[count] = Type.STRING;
                  }
                }
                else                                               // If there is no decimal point...
                {       
                  try                                                         // try to make it an integer.
                  {
                    Integer.parseInt(val);
                  }
                  catch(NumberFormatException e)                              // if that fails, make it a string.
                  {
                    this.tmp[count] = Type.STRING;
                  }
                }
              }
              else                                        // If we think the column is neither DISCRETE or CONTINUOUS...
              {
                this.tmp[count] = Type.STRING;                      // make it STRING.
              }
            }
          }
        }
        if(showChange)
        {
          this.updateColumnTypes();
        }
      }
  
      void cancelButton_actionPerformed(ActionEvent e) 
      {
        cancelled = true;
        javax.swing.FocusManager.setCurrentManager(oldManager);
        dispose();
      }
  
  /**  This class defines the behaviour of the file preview table displayed in the
       LoadDelimitedFileDialog.  It provides methods of calculating the number of
       rows and columns required, and a method to retrieve the relevent String from any cell.
  */
      public class FilePreviewTableModel extends javax.swing.table.AbstractTableModel
      {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1170026997162085459L;
		LoadDelimitedFileDialog params;
        public String[] preview;
        int numberOfRows;
  
        FilePreviewTableModel(LoadDelimitedFileDialog params, int rows)
        {
          try
          {
            int count;
            this.params = params;
            preview = new String[rows];
            FileReader fr = new FileReader(params.fname.toString());
            BufferedReader reader = new BufferedReader(fr);          
            preview[0] = reader.readLine();
            for(count = 1; (count < rows) && (preview[count-1] != null); count++)
            {
              preview[count] = reader.readLine();
            }
            numberOfRows = count-1;
          }
          catch (Exception e)
          {
            System.out.println("Error opening file " + fname);
          }
        }
  
        public int getRowCount()
        {
          return this.numberOfRows;
        }
  
        public int getColumnCount()
        {
          int max = 0;
          int count1;
          int count2;
          String tmp;
          for(count1 = 0; count1 < getRowCount(); count1++)
          {
            tmp = preview[count1].substring(0);  // make a copy of the relevent line...
            if(!params.startDelim.equals(""))
            {
              if(tmp.indexOf(params.startDelim) != -1)
              {
                tmp = tmp.substring(tmp.indexOf(params.startDelim)+params.startDelim.length());  // chop off start of line...
              }
            }
            if(!params.endDelim.equals(""))
            {
              if(tmp.indexOf(params.endDelim) != -1)
              {
                tmp = tmp.substring(0, tmp.indexOf(params.endDelim));  // chop off end of line...
              }
            }
            if(!params.quotes)
            {
              for(count2 = 0; IO.DataStructured.cmpntExists(tmp, params.wordDelim, count2, consume); count2++);
            }
            else
            {
              for(count2 = 0; IO.DataStructured.quotedCmpntExists(tmp, params.wordDelim, count2, consume, new String(new char[]{params.quoteChar})); count2++);
            }
            if(count2 > max)
            {
              max = count2;
            }
          }
          return max;
        }
  
        public String getColumnName(int columnIndex)
        {
          if(columnIndex < ctypes.cmpnts.length)
          {
            String res = "";
            if(params.titleLine)
            {
              res = (String)getValueAt(0, columnIndex);
            }
            String res2 = Type.byType(ctypes.cmpnts[columnIndex]);
            if(res2 != null)
            { 
              return res + " (" + res2 + ")";
            }
            else
            {
              return res + " (" + ctypes.cmpnts[columnIndex].getTypeName() + ")";
            }
          }
          else
          {
            return "No Name";
          }
        }
  
        public java.lang.Object getValueAt(int rowIndex, int columnIndex)
        {
          String res = preview[rowIndex].substring(0);  // make a copy of the relevent line...
          if(!params.startDelim.equals(""))
          {
            if(res.indexOf(params.startDelim) != -1)
            {
              res = res.substring(res.indexOf(params.startDelim)+params.startDelim.length());  // chop off start of line...
            }
          }
          if(!params.endDelim.equals(""))
          {
            if(res.indexOf(params.endDelim) != -1)
            {
              res = res.substring(0, res.indexOf(params.endDelim));  // chop off end of line...
            }
          }
          if(params.quotes)
          {
            if(IO.DataStructured.quotedCmpntExists(res, params.wordDelim, columnIndex, consume, new String(new char[]{params.quoteChar})))
            {
              res = IO.DataStructured.getQuotedCmpnt(res, params.wordDelim, columnIndex, consume, new String(new char[]{params.quoteChar}));  // get relevent word...
            }
            else
            {
              res = "";
            }
          }
          else
          {
            if(IO.DataStructured.cmpntExists(res, params.wordDelim, columnIndex, consume))
            {
              res = IO.DataStructured.getCmpnt(res, params.wordDelim, columnIndex, consume);  // get relevent word...
            }
            else
            {
              res = "";
            }
          }
          return res;
        }
      }
  
      private int pairsOfQuotes(String s, char quote, char delim)
      {
        String start = new Character(delim).toString() + new Character(quote).toString();
        String end = new Character(quote).toString() + new Character(delim).toString();
        int pos = s.indexOf(start);
        int pos2 = s.indexOf(end, pos+2);
        int count;
        for(count = 0; (pos != -1) && (pos2 != -1); count++)
        {
          pos = s.indexOf(start, pos2 + 2);
          pos2 = s.indexOf(end, pos + 2);
        }
        return count;
      }
  
      private boolean testQuoteDelim(char quote, char delim)
      {
        int count;
        int pairs;
        int oldPairs;
        int offset = 0;
  
        if(titleLine)
        {
          offset++;
        }
  
        oldPairs = pairsOfQuotes(this.dm.preview[offset], quote, delim);
        for(count = offset + 1; count < this.dm.numberOfRows; count++)
        {
          String s = this.dm.preview[count];
          pairs = pairsOfQuotes(s, quote, delim);
          if((pairs == 0) || (pairs != oldPairs))
          {
            return false;
          }
        }
        return true;
      }
  
      public char autoDetectQuotes()
       {
         if(testQuoteDelim('\"', ' '))
         {
           this.wordDelim = " ";
           return '\"';
         }
         if(testQuoteDelim('\"', ','))
         {
           this.wordDelim = ",";
           return '\"';
         }
         if(testQuoteDelim('\"', '\t'))
         {
           this.wordDelim = "\t";
           return '\"';
         }
         if(testQuoteDelim('\"', ';'))
         {
           this.wordDelim = ";";
           return '\"';
         }
         if(testQuoteDelim('\"', ':'))
         {
           this.wordDelim = ":";
           return '\"';
         }
         if(testQuoteDelim('\"', '|'))
         {
           this.wordDelim = "|";
           return '\"';
         }
  
         if(testQuoteDelim('\'', ' '))
         {
           this.wordDelim = " ";
           return '\'';
         }
         if(testQuoteDelim('\'', ','))
         {
           this.wordDelim = ",";
           return '\'';
         }
         if(testQuoteDelim('\'', '\t'))
         {
           this.wordDelim = "\t";
           return '\'';
         }
         if(testQuoteDelim('\'', ';'))
         {
           this.wordDelim = ";";
           return '\'';
         }
         if(testQuoteDelim('\'', ':'))
         {
           this.wordDelim = ":";
           return '\'';
         }
         if(testQuoteDelim('\'', '|'))
         {
           this.wordDelim = "|";
           return '\'';
         }
         return '\0';
      }
  
      private int nFields(String s, String delim)
      {
        int count;
        if(this.quotes)
        {
          for(count = 0; IO.DataStructured.quotedCmpntExists(s, delim, count, this.consume, new String(new char[]{this.quoteChar})); count++);
        }
        else
        {
          for(count = 0; IO.DataStructured.cmpntExists(s, delim, count, this.consume); count++);
        }
        return count;
      }
  
      private boolean testDelim(String delim)
      {
        int n;
        int oldN;
        int count;
        int offset = 0;
  
        if(titleLine)
        {
          offset++;
        }
  
        oldN = nFields(this.dm.preview[offset], delim);
        for(count = offset + 1; count < this.dm.numberOfRows; count++)
        {
          n = nFields(this.dm.preview[count], delim);
          if((n < 2) || (n != oldN))
          {
            return false;
          }
        }
        return true;
      }
  
      public String autoDetectDelimiter()
      {
        String s = ",";
        int nStrings;
        int count;
        int minStrings = 100000000;
  
        if(testDelim(" "))
        {
          this.wordDelim = " ";
          this.autoDetectColumnTypes(false);
          for(nStrings = 0, count = 0; count < tmp.length; count++)
          {
            if(tmp[count] == Type.STRING)
            {
              nStrings++;
            }
          }
          if(nStrings < minStrings)
          {
            s = " ";
            minStrings = nStrings;
          }
        }
  
        if(testDelim(","))
        {
          this.wordDelim = ",";
          this.autoDetectColumnTypes(false);
          for(nStrings = 0, count = 0; count < tmp.length; count++)
          {
            if(tmp[count] == Type.STRING)
            {
              nStrings++;
            }
          }
          if(nStrings < minStrings)
          {
            s = ",";
            minStrings = nStrings;
          }
        }
  
        if(testDelim("\t"))
        {
          this.wordDelim = "\t";
          this.autoDetectColumnTypes(false);
          for(nStrings = 0, count = 0; count < tmp.length; count++)
          {
            if(tmp[count] == Type.STRING)
            {
              nStrings++;
            }
          }
          if(nStrings < minStrings)
          {
            s = "\t";
            tab = true;
            minStrings = nStrings;
          }
        }
  
        if(testDelim(";"))
        {
          this.wordDelim = ";";
          this.autoDetectColumnTypes(false);
          for(nStrings = 0, count = 0; count < tmp.length; count++)
          {
            if(tmp[count] == Type.STRING)
            {
              nStrings++;
            }
          }
          if(nStrings < minStrings)
          {
            s = ";";
            minStrings = nStrings;
          }
        }
  
        if(testDelim(":"))
        {
          this.wordDelim = ":";
          this.autoDetectColumnTypes(false);
          for(nStrings = 0, count = 0; count < tmp.length; count++)
          {
            if(tmp[count] == Type.STRING)
            {
              nStrings++;
            }
          }
          if(nStrings < minStrings)
          {
            s = ":";
            minStrings = nStrings;
          }
        }
  
        if(testDelim("|"))
        {
          this.wordDelim = "|";
          this.autoDetectColumnTypes(false);
          for(nStrings = 0, count = 0; count < tmp.length; count++)
          {
            if(tmp[count] == Type.STRING)
            {
              nStrings++;
            }
          }
          if(nStrings < minStrings)
          {
            s = "|";
            minStrings = nStrings;
          }
        }
  
        if(minStrings == 100000000)
        {
          s = ",";
        }
        return s;
      }
  
      private class LDFDFocusManager extends javax.swing.DefaultFocusManager
      {
        public void processKeyEvent(Component focusedComponent, KeyEvent anEvent){}
      }
    }
}
