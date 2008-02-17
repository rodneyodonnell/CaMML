//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: FileLoader.java
// Authors: {leighf}@csse.monash.edu.au
  
package cdms.plugin.dialog;
  
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

import cdms.core.*;
  
public class FileLoader
{



  /** A function that loads a recently opened file.  You must supply the vector of 
      loaders first.  The second parameter is the name of the recently opened file.
      <code>[ ("Name",["extension"],Name -> a,(Name,a) -> b) ] -> Str -> b </code> 
  */
  public static LoadRecent loadRecent = new LoadRecent();

  /** A function that loads a recently opened file.  <code>Str -> t</code> */
  public static class LoadRecent extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8581152936044305439L;
	public static final Type.Function TT = new Type.Function(LoadFileDialog.PARAMT,LoadRecent2.TT);

    public LoadRecent()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new LoadRecent2((Value.Vector) v);
    }
  }

  /** A function that loads a recently opened file.  <code>Str -> t</code> */
  public static class LoadRecent2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4355803188653990233L;

	public static final Type.Function TT = new Type.Function(Type.STRING,new Type.Variable());

    Value.Vector vparam;

    public LoadRecent2(Value.Vector vparam)
    {
      super(TT);
      this.vparam = vparam;
    }

    public Value apply(Value v)
    {
      String name = ((Value.Str) v).getString();
 
      Recent recent = new Recent();

      for (int j = 0; j < recent.size(); j++)
      {
        Recent.ListItem li = recent.getListItem(j);
        if (((Value.Str) li.params.cmpnt(0)).getString().compareTo(name) == 0)
        { 
          // Find the loader function and post viewer.
          for (int i = 0; i < vparam.length(); i++)
          {
            Value.Structured elti = (Value.Structured) vparam.elt(i);
            String namei = ((Value.Str) elti.cmpnt(0)).getString();
            if (namei.compareTo(li.name) == 0)
            {
              Value.Function loader = (Value.Function) elti.cmpnt(3);
              Value data = ((Value.Structured) loader.apply(li.params)).cmpnt(1);
              recent.opened(li);   
              return data;
            }
          }
        }
      }
      throw new RuntimeException("Cannot load recent file " + v + 
                                 ".  The name was not found in the recent list.");
    }
  }

  /** Manages a list of recent files. The list is stored in a java vector in a file 
      called .cdmsrecent in the users home directory.  This class is used by LoadFileDialog. */
  public static class Recent implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5779124234176241333L;
	protected final int WINDOW = 20;
    protected final int MAX = 2000;

    private String fname = System.getProperty("user.home") + 
                           System.getProperty("file.separator") + ".cdmsrecent";

    private java.util.Vector list = new java.util.Vector();
    private java.util.Vector order = new java.util.Vector();

    protected class ListItem implements Serializable
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -6529004697716255360L;
	String name;
      Value.Structured params;
      java.util.Date firstDate, lastDate; 
      float pr;
    }

    public Recent()
    {
      load();
    }

    public int size()
    {
      return list.size();
    }

    public ListItem getListItem(int idx)
    {
      return (ListItem) list.elementAt(idx);
    } 

    public void add(String name, Value.Structured params)
    {
      // Remove existing.
      String newFilePath = ((Value.Str) params.cmpnt(0)).getString();
      for (int i = 0; i < size(); i++)
      {
        ListItem li = getListItem(i);
        if (((Value.Str) li.params.cmpnt(0)).getString().compareTo(newFilePath) == 0) 
        {
          list.remove(i);
          break;
        }
      }

      ListItem li = new ListItem();
      li.params = params;
      li.name = name;
      li.firstDate = new java.util.Date();
      li.lastDate = li.firstDate;
      list.add(li);

      order.add(li);
      while (order.size() > WINDOW) order.removeElementAt(0);

      save();
    }

    public void opened(ListItem li)
    {
      list.remove(li);
      list.insertElementAt(li,0);
      li.lastDate = new java.util.Date();
      order.add(li);
      while (order.size() > WINDOW) order.removeElementAt(0);
      save();
    }

    /** Remove ListItem entries to reduce the number to <= MAX.  Assumes sorted list. */
    private void purge()
    {
      for (int i = MAX; i < size(); i++)
      {
        ListItem li = getListItem(i);
        while (order.remove(li)) order.remove(li); 
        list.remove(li); 
      }
    }

    /** Update the ListItem probabilities. */
    private void setProbs()
    {
      for (int i = 0; i < size(); i++)
      {
        ListItem li = getListItem(i);
        int count = 0;
        for (int j = 0; j < order.size(); j++)
          if (order.elementAt(j) == li) count++;
        li.pr = (count + 0.5f) / (order.size() + 0.5f * size());
      }
    }

    /** Sort the list by their probabilities. */
    private void sort()
    {
      boolean moved = true;
      while (moved)
      {
        moved = false;
        for (int i = 0; i < size() - 1; i++)
        {
          ListItem lii = getListItem(i);
          ListItem liip1 = getListItem(i+1);
          if (liip1.pr > lii.pr) 
          {
            list.remove(lii);
            list.remove(liip1);
            list.insertElementAt(liip1,i);
            list.insertElementAt(lii,i+1);
            moved = true;
          }
        }
      }
    }

    private void load()
    {
      if ((new File(fname)).exists())
      {
        try
        {
          FileInputStream is = new FileInputStream(fname);
          ObjectInputStream p = new ObjectInputStream(is);
          list = (java.util.Vector) p.readObject();
          order = (java.util.Vector) p.readObject();
          is.close();
        }
        catch (Exception e)
        {
          System.out.println("Error reading recent file file: " + fname + ".  " + e.toString());
          list = new java.util.Vector();  // just in case.
        }
      }
      setProbs();
      sort();
      purge();
    }

    private void save()
    {
      try
      {
        FileOutputStream os = new FileOutputStream(fname);
        ObjectOutputStream p = new ObjectOutputStream(os);
        p.writeObject(list);
        p.writeObject(order);
        os.close();
      }
      catch (Exception e)
      {
        System.out.println("Error writing to recent file file.  " + e);
      }
    }

  }



  public static LoadFileDialog loadFileDialog = new LoadFileDialog();
  /**

    A dialog box for opening a file in a given file format or selecting from a list of 
    recently opened files.  Both the file and parameters used are stored for recently 
    opened files.  The function takes a vector of loaders.  The first component is the 
    name of the loader.  The second is a vector of valid file extensions for this format.  
    The third is a function which allows for a gui to return the parameters for the loader.  
    The fourth is the loader function itself which loads the data.

    [ ("Name",["extension"],Name -> a,(Name,a) -> b,b -> ()) ] -> ()

  */
  public static class LoadFileDialog extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4601972526057310546L;
	public static final Type.Variable a = new Type.Variable();
    public static final Type.Variable b = new Type.Variable();
    public static final Type.Vector PARAMT = new Type.Vector(new Type.Structured( new Type[] {
                                 Type.STRING,
                                 new Type.Vector(Type.STRING),
                                 new Type.Function(Type.STRING,a),
                                 new Type.Function(
                                   new Type.Structured(new Type[] { Type.STRING, a }, 
                                                       new String[] { "Filepath", "Params" }) ,b),
                                 new Type.Function(b,Type.TRIV) }));
    public static final Type.Function TT = new Type.Function(PARAMT,Type.TRIV);

    public class FnFileFilter extends GenericFileFilter
    {
      public String name;
      public Value.Function fn, ldfn, postfn;

      public FnFileFilter(String exts[], String name, Value.Function fn, 
                          Value.Function ldfn, Value.Function postfn)
      {
        super(exts,name);
        this.name = name;
        this.fn = fn;
        this.ldfn = ldfn;
        this.postfn = postfn;
      }
    }

    public LoadFileDialog()
    {
      super(TT);
    }

    private class RecentListDialog extends CDMSDialog
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -2731266717051486571L;
	private int result = CANCEL;
      private static final int CANCEL = -1;
      private static final int CHOOSE = -2;

      public RecentListDialog(final Recent recent)
      {
        super("Recent files");

        TableModel dataModel = new AbstractTableModel() 
        {
          /** Serial ID required to evolve class while maintaining serialisation compatibility. */
			private static final long serialVersionUID = -9013728379409886256L;
		java.text.DecimalFormat valFormat = new java.text.DecimalFormat("#.###");

          public int getColumnCount() { return 4; }
          public int getRowCount() { return recent.size(); }
          public Object getValueAt(int row, int col) 
          { 
            Recent.ListItem li = recent.getListItem(row);
            if (col == 0 || col == 1)
            {
              File f = new File(((Value.Str) li.params.cmpnt(0)).getString());
              if (col == 0) return f.getName(); else return f.getPath();
            } 
            else if (col == 2) return li.lastDate;
            else return valFormat.format(li.pr);
          }
          public String getColumnName(int idx)
          {
            if (idx == 0) return "File";
              else if (idx == 1) return "Path";
              else if (idx == 2) return "Last opened";
              else return "Pr";
          }
        };
        final JTable table = new JTable(dataModel);
        table.setRowSelectionInterval(0,0);

        // Set column widths.
        TableColumn column = null; 
        column = table.getColumnModel().getColumn(0); 
        column.setPreferredWidth(120); 
        column = table.getColumnModel().getColumn(1); 
        column.setPreferredWidth(280); 
        column = table.getColumnModel().getColumn(2); 
        column.setPreferredWidth(160); 
        column = table.getColumnModel().getColumn(3); 
        column.setPreferredWidth(80); 

        getContentPane().setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Select a recent file"));
        centerPanel.add(new JScrollPane(table));
        getContentPane().add(centerPanel,BorderLayout.CENTER);

        // Double click for recent list.
        MouseListener recentMouseListener = new MouseAdapter() 
        {
          public void mouseClicked(MouseEvent e) 
          {
            if (e.getClickCount() == 2) 
            {
              result = table.getSelectedRow();
              dispose();
            }
          }
        };
        table.addMouseListener(recentMouseListener);

        JPanel southPanel = new JPanel();
        JButton okBtn = new JButton("OK");
        okBtn.setMnemonic('O');
        okBtn.addActionListener(new java.awt.event.ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
              result = table.getSelectedRow();
              dispose();
            }
          });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setMnemonic('C');
        cancelBtn.addActionListener(new java.awt.event.ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
              result = CANCEL;
              dispose();
            }
          });

        JButton chooseBtn = new JButton("Choose");
        chooseBtn.setMnemonic('o');
        chooseBtn.addActionListener(new java.awt.event.ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
              result = CHOOSE;
              dispose();
            }
          });

        southPanel.add(okBtn);
        southPanel.add(cancelBtn);
        southPanel.add(chooseBtn);
        getContentPane().add(southPanel,BorderLayout.SOUTH);

        pack();
        center();
        setVisible(true);
      }

      public int getResult()
      {
        return result;
      }
    }

    public static final JFileChooser fc = new JFileChooser();

    public Value apply(Value param)
    {
      Value.Vector vparam = (Value.Vector) param;

      // First display list of recently opened files (if they exist).
      Recent recent = new Recent();
      if (recent.size() > 0)
      {
        RecentListDialog rld = new RecentListDialog(recent); 
        if (rld.getResult() >= 0)
        {
          Recent.ListItem li = recent.getListItem(rld.getResult());
 
          // Find the loader function and post viewer.
          for (int i = 0; i < vparam.length(); i++)
          {
            Value.Structured elti = (Value.Structured) vparam.elt(i);
            String name = ((Value.Str) elti.cmpnt(0)).getString();
            if (name.compareTo(li.name) == 0)
            {
              Value.Function loader = (Value.Function) elti.cmpnt(3);
              Value.Function postfn = (Value.Function) elti.cmpnt(4);
              postfn.apply(loader.apply(li.params));
              recent.opened(li);   
              return Value.TRIV;
            }
          }
          System.out.println("Format does not exist: " + li.name);
        }
        else if (rld.getResult() != RecentListDialog.CHOOSE) return Value.TRIV;
      }

      // Display open dialog.
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fc.setMultiSelectionEnabled(false);
      fc.resetChoosableFileFilters();
      fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());

      // Setup file Filters.
      for (int i = 0; i < vparam.length(); i++)
      {
        Value.Structured ft = (Value.Structured) vparam.elt(i);
        String exts[] = new String[((Value.Vector) ft.cmpnt(1)).length()];
        String fname = ((Value.Str) ft.cmpnt(0)).getString();
        Value.Function fn = (Value.Function) ft.cmpnt(2);
        Value.Function ldfn = (Value.Function) ft.cmpnt(3);
        Value.Function postfn = (Value.Function) ft.cmpnt(4);
        for (int j = 0; j < ((Value.Vector) ft.cmpnt(1)).length(); j++)
          exts[j] = ((Value.Str) ((Value.Vector) ft.cmpnt(1)).elt(j)).getString();
        fc.addChoosableFileFilter(new FnFileFilter(exts, fname, fn, ldfn, postfn));
      }

      if (fc.showOpenDialog(new Frame()) == JFileChooser.APPROVE_OPTION)
      {
        java.io.File selFile = fc.getSelectedFile();

        if (!selFile.toString().equals(""))
        {
          Value.Str fnameAsStr = new Value.Str(selFile.toString());
          FnFileFilter filt = (FnFileFilter) fc.getFileFilter();
          Value ldparams = filt.fn.apply(fnameAsStr);
          if (ldparams == null || ldparams == Value.TRIV) return Value.TRIV;
          Value.Structured finalParams = new Value.DefStructured(new Value[] { fnameAsStr, ldparams });
          recent.add(filt.name,finalParams);
          filt.postfn.apply(filt.ldfn.apply(finalParams));
          return Value.TRIV;
        }
      }
      return Value.TRIV;
    }

  }

  
  
  
  
  
  
  /** Choose file with 'load' dialogue */
  public static FileChooser loadFileChooser = new FileChooser(true);

  /** Choose file with 'save' dialogue */
  public static FileChooser saveFileChooser = new FileChooser(false);

  /**
   * () -> String
   * When fileChooser is run it pops up a window to select a file.
   * The files name is returned. <br>
   * 
   *  if (open == true) the load dialogue is used, <br>
   *  else the save dialogue is used  <br>
   */
  public static class FileChooser extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7909631879903323478L;

	public static final Type.Function TT = new Type.Function(Type.TRIV, Type.STRING);

    final boolean open;
    
    private static String lastFileChosen = null;
    
    public FileChooser(boolean open)
    {
        super(TT);
    	this.open = open;
    }


    public Value apply(Value v)
    {
    	Value.Vector vec = (Value.Vector)v;

    	// Display open dialog.
    	JFileChooser fc = new JFileChooser();
    	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    	fc.setMultiSelectionEnabled(false);
    	fc.resetChoosableFileFilters();
    	fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());

    	if (lastFileChosen != null) {
    		fc.setSelectedFile( new File(lastFileChosen) );
    	}
    	
    	final String exts[] = new String[vec.length()];
    	for (int i = 0; i < vec.length(); i++) {
    		exts[i] = ((Value.Str)vec.elt(i)).getString();
    	}

    	fc.setFileFilter(new FileFilter() {
    		public String getDescription() { return Arrays.toString(exts); }
    		public boolean accept(File f) {    			 
    			String fName = f.getName();
    			for (String s : exts) { if (fName.endsWith(s)) return true; }
    			return false;
    		}
    	});
    	
        

    	try {
    		if (open) {	fc.showOpenDialog(new Frame()); }
    		else { fc.showSaveDialog(new Frame()); }
    		String s = fc.getSelectedFile().getCanonicalPath();
    		lastFileChosen = s;
    		return new Value.Str(s);
    	  
    	} catch (IOException e) {
    		throw new RuntimeException("No file selected.",e);  
    	}
    }

  }

}
