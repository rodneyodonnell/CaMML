//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: DesktopFrame.java
// Authors: {leighf}@csse.monash.edu.au

package cdms.plugin.desktop;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import cdms.core.*;
import cdms.plugin.dialog.*;
import cdms.plugin.enview.*;

/** The main frame for the desktop. */
public class DesktopFrame extends JFrame 
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4037728223355378898L;

public static JDesktopPane desktop = new JDesktopPane();

  protected static JToolBar toolbar = new JToolBar();
  protected static JLabel statusLabel = new JLabel(" ",JLabel.LEFT);
  protected EnView.CdmsEnvironmentTree symbolTable;
  protected EnvMouseAdapter envMouseAdapter = new EnvMouseAdapter();

  public static Value.Function defaultValueViewer;   // The default value viewer.

  public static void main(String[] args) 
  {
    @SuppressWarnings("unused") 
    Desktop cdmsDesktop = new Desktop();
  }

  public DesktopFrame()
  {
    this("CDMS Data Mining Desktop");
  }

  public DesktopFrame(String title)
  {
    super(title);

    new cdms.plugin.bean.WindowCloser(this,true);

    // Window size
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int xinset = screenSize.width / 5;
    int yinset = screenSize.height / 5;
    setBounds(xinset, yinset, screenSize.width - xinset * 2, 
              screenSize.height - yinset * 2);

    // GUI.
    getContentPane().setLayout(new BorderLayout());

    JSplitPane spane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    spane.setOpaque(false);
    getContentPane().add(spane,BorderLayout.CENTER);

    getContentPane().add(toolbar,BorderLayout.NORTH);

    JPanel statusPanel = new JPanel();
    statusPanel.add(statusLabel);
    getContentPane().add(statusPanel,BorderLayout.SOUTH);
    symbolTable = new EnView.CdmsEnvironmentTree(Environment.env.getEntryByObject(Type.TYPE),null,true);

    desktop.setDoubleBuffered(true);
    spane.setLeftComponent(symbolTable);
    spane.setRightComponent(new JScrollPane(desktop));
    spane.setDividerSize(10);
    spane.setOneTouchExpandable(true);
    spane.resetToPreferredSizes();

    symbolTable.tree.addMouseListener(envMouseAdapter);

//    (new FN.SendMail5("bruce",System.getProperty("user.name"),"leighf","CDMS Gui Started")).
//      apply(new Value.Str((new java.util.Date()).toString()));
//    (new FN.SendMail5("bruce",System.getProperty("user.name"),"joshc","CDMS Gui Started")).
//      apply(new Value.Str((new java.util.Date()).toString()));
//    (new FN.SendMail5("bruce",System.getProperty("user.name"),"lloyd","CDMS Gui Started")).
//      apply(new Value.Str((new java.util.Date()).toString()));
  }

  public static class EnvMouseAdapter extends MouseAdapter implements Menu.ValueProducer, java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3090688114238511369L;
	private Value.Vector envmenu;
    private Menu.PopupMenu valueMenu;
    private Value applyValue;

    public EnvMouseAdapter()
    {
      super();
      valueMenu = new Menu.PopupMenu("",this,null,statusLabel);
    }

    public Value getApplyValue()
    {
      return applyValue;
    }

    public Type getApplyType()
    {
      return applyValue.t;
    }

    public void mouseClicked(MouseEvent e) 
    {
      JTree tree = (JTree) e.getSource();
      int selRow = tree.getRowForLocation(e.getX(), e.getY());
      TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());

      if (selRow != -1)  
      {
        Environment.RegEntry re = 
          ((EnView.EnvData.Node) selPath.getLastPathComponent()).re;

        // Check if left click on Value.
        statusLabel.setText(" ");
        if ( (e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK )
        {
          if (re.description.compareTo("") != 0)
            statusLabel.setText(re.description);
        }

        if (e.getClickCount() == 1) 
        {
          // Check if right click on Value.
          if ( (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK )
          {
            if (selPath != null) tree.setSelectionPath(selPath);
            if (re.o instanceof Value)
            {
              // Right click on a value.
              applyValue = (Value) re.o;  // set value for ValueProducer.
              valueMenu.show(tree,e.getX(),e.getY());
            }
            else
            {
              // Right click on a type.
              getTypeMenu((Type) re.o).show(tree,e.getX(),e.getY());
            } 
          }
        }
        else if (e.getClickCount() == 2) 
        {
          if (re.o instanceof Value)
            makeWindow(re.name,(Value) re.o);
        }
      }
    }

    public void setMenu(Value.Vector envmenu)
    {
      this.envmenu = envmenu;
      valueMenu.setMenu(envmenu);
    }

    public JPopupMenu getTypeMenu(Type t)
    {
      JPopupMenu m = new JPopupMenu();
      NewSubTypeListener jmiNewTypeListener = new NewSubTypeListener(t);
      ViewTypeListener jmiViewTypeListener = new ViewTypeListener(t);
      NewValueListener jmiNewValueListener = new NewValueListener(t);

      try
      {
        JMenuItem jmiViewType = new JMenuItem("View type");
        m.add(jmiViewType);  
        jmiViewType.addActionListener(jmiViewTypeListener);
        new Menu.MenuHintChangeListener(jmiViewType, "View type information.", statusLabel); 

        JMenuItem jmiNewType = new JMenuItem("New subtype");
        m.add(jmiNewType);  
        jmiNewType.addActionListener(jmiNewTypeListener);
        new Menu.MenuHintChangeListener(jmiNewType, "Create a new subtype.", statusLabel); 

        JMenuItem jmiNewValue = new JMenuItem("New value");
        m.add(jmiNewValue); 
        jmiNewValue.addActionListener(jmiNewValueListener);
        new Menu.MenuHintChangeListener(jmiNewValue, "Create a new value of this type.", statusLabel); 

        if (envmenu != null)
        {
//          m.add(Menu.createMenu(envmenu,null,statusLabel));
        }
      }
      catch (Exception e)
      {
        System.out.println(e);
        e.printStackTrace();
      }

      return m;
    }

  }

  /** Expects (string,obj). */
  public static void makeWindow(Value.Structured v)
  {
    makeWindow(((Value.Str) v.cmpnt(0)).getString(),(Value.Obj) v.cmpnt(1));
  }

  public static void makeWindow(String title, Value v)
  {
    if (v instanceof Value.Obj)
    {
      Value.Obj o = (Value.Obj) v;
      if ( o.getObj() instanceof Component )
      {
        makeWindow(title, (Component) o.getObj());
        return;
      }
    }

    // Launch the default value viewer.
    if (defaultValueViewer != null)
    {
      Value res = defaultValueViewer.apply(v); 
      makeWindow(title,res);  // res is a Obj(JComponent).
    }
    else
    {
      System.out.println("Cannot display value because default viewer is not configured.");
    }
  }

  public static void makeWindow(String title, Component c)
  {
    JInternalFrame frame = new JInternalFrame(title,true,true,true,true);
    frame.getContentPane().setLayout(new GridLayout(1,1));
    frame.getContentPane().add(c);
    frame.setOpaque(true);
    frame.setBackground(Color.white);
    frame.pack();
    int width = frame.getWidth();
    int height = frame.getHeight();
    if (width > desktop.getWidth()) width = (int) (desktop.getWidth() * 0.9);
    if (height > desktop.getHeight()) height = (int) (desktop.getHeight() * 0.9);
    frame.setSize(width,height);
//    frame.setLocation((int) (Math.random() * (desktop.getWidth() - width)),
//                      (int) (Math.random() * (desktop.getHeight() - height)));
    frame.show();
    desktop.add(frame);
    desktop.moveToFront(frame);
  }

  public void setVisible(Value.Discrete v)
  {
    setVisible(v.getDiscrete() == Value.TRUE.getDiscrete()); 
  }

  public void setEnvMouseAdapter(Value.Vector v)
  {
    envMouseAdapter.setMenu(v);
  }

  public void setMainMenu(Value.Vector v)
  {
    setJMenuBar(new Menu.MenuBar(null,v,statusLabel));
  }


  public void setToolbar(Value.Vector v)
  {
    if (toolbar != null) getContentPane().remove(toolbar);
    toolbar = new Menu.ToolBar(null,v,statusLabel);
    getContentPane().add(toolbar,BorderLayout.NORTH);
  }

  public void setEnvMenu(Value.Vector v)
  {
    envMouseAdapter.setMenu(v);
  }

  /** The default value viewer should return an Obj(Component) */
  public void setValueViewer(Value.Function f)
  {
    defaultValueViewer = f;
  }

  protected static class ViewTypeListener implements ActionListener, java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 408776260730410307L;
	private Type t;

    public ViewTypeListener(Type t)
    {
      this.t = t;
    }

    public void actionPerformed(ActionEvent e) 
    {
      try
      {
        ViewTypeDialog.class.getConstructor(new Class[] {t.getClass()}).newInstance(new Object[] {t});
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }

  protected static class NewSubTypeListener implements ActionListener, java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5857865694035852169L;
	private Type t;

    public NewSubTypeListener(Type t)
    {
      this.t = t;
    }

    public void actionPerformed(ActionEvent e) 
    {
      try
      {
        NewTypeDialog.class.getConstructor(
          new Class[] {t.getClass(), String.class, Boolean.class}).newInstance(
            new Object[] {t, "Define new type...", new Boolean(true)});
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }

  protected static class NewValueListener implements ActionListener, java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8051489558078367199L;
	private Type t;

    public NewValueListener(Type t)
    {
      this.t = t;
    }

    public void actionPerformed(ActionEvent e) 
    {
      try
      {
        NewValueDialog.class.getConstructor(
          new Class[] { t.getClass(), String.class, Boolean.class }).newInstance(
            new Object[] {t, "Create new value...", new Boolean(true)});
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }


  public void arrangeRows()
  {
    JInternalFrame[] frames = desktop.getAllFrames();
    DesktopManager manager = desktop.getDesktopManager();

    int nonIconicFrames = 0;
    for (int i = 0; i < frames.length; i++)
     if (!frames[i].isIcon()) nonIconicFrames++;

    if (nonIconicFrames != 0)
    {
      int fh = desktop.getHeight() / nonIconicFrames;
      int fw = desktop.getWidth();
      for (int i = 0; i < frames.length; i++)
      {
        if (!frames[i].isIcon())
          manager.setBoundsForFrame(frames[i], 0, i * fh, fw, fh);
      }
    }
  }

   
  public void arrangeColumns()
  {
    JInternalFrame[] frames = desktop.getAllFrames();
    DesktopManager manager = desktop.getDesktopManager();

    int nonIconicFrames = 0;
    for (int i = 0; i < frames.length; i++)
     if (!frames[i].isIcon()) nonIconicFrames++;

    if (nonIconicFrames != 0)
    {
      int fw = desktop.getWidth() / nonIconicFrames;
      int fh = desktop.getHeight();
      for (int i = 0; i < frames.length; i++)
      {
        if (!frames[i].isIcon())
          manager.setBoundsForFrame(frames[i], fw * i, 0, fw, fh);
      }
    }
  }


}
