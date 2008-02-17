//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Menu.java
// Authors: {leighf}@csse.monash.edu.au

package cdms.plugin.desktop;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.net.*;
import cdms.core.*;

/** Classes for creating Java menus and toolbars from CDMS values. */
public class Menu
{
  public interface ValueProducer 
  {
    public Type getApplyType();
    public Value getApplyValue();
  }

  public static JMenuItem createMenu(Value.Structured v, ValueProducer vp, JLabel hintLabel)
  {
    if ( v.cmpnt(4) instanceof Value.Vector )
    {
      JMenu jm = new JMenu(((Value.Str) v.cmpnt(0)).getString());
      Value.Vector submenus = (Value.Vector) v.cmpnt(4);
      int numseps = 0;
      for (int i = 0; i < submenus.length(); i++)
      {
        Value.Structured submenu = (Value.Structured) submenus.elt(i);
        if ( ((Value.Str) submenu.cmpnt(0)).getString().compareTo("-") == 0)
        {
          jm.addSeparator();
          numseps++;
        }
        else 
        {
          JMenuItem jmi = createMenu ( (Value.Structured) submenus.elt(i), vp, hintLabel );
          if (jmi != null) jm.add(jmi);
        }
      }
      if (jm.getItemCount() > numseps) return jm;
        else return null;
    }
    else
    {
      // Check if value is compatible with FN.
      if (v.cmpnt(4) instanceof Value.Function) 
      {
        Type t = Type.TRIV;
        if (vp != null) t = vp.getApplyType();
        if ( ((Type.Function) v.cmpnt(4).t).param.hasMember(t) )
        {
          
          JMenuItem jm;
          String imageName = ((Value.Str) v.cmpnt(3)).getString();

          if ( imageName.compareTo("") == 0 )
            jm = new JMenuItem(((Value.Str) v.cmpnt(0)).getString());
          else 
          {
            URL imageURL = Menu.class.getResource(imageName);
            if (imageURL != null)
              jm = new JMenuItem(((Value.Str) v.cmpnt(0)).getString(),new ImageIcon(imageURL));
            else jm = new JMenuItem(((Value.Str) v.cmpnt(0)).getString());
          }

          if (v.cmpnt(4) != Value.TRIV) 
            jm.addActionListener(new FunctionListener((Value.Function) v.cmpnt(4),vp ));

          if (hintLabel != null)
          {
            String hint = ((Value.Str) v.cmpnt(2)).getString();
            if (hint.compareTo("") != 0 && hintLabel != null)
              new MenuHintChangeListener(jm, hint, hintLabel); 
          }

          return jm;
        }
      }
      return null;
    }
  } 


  public static class FunctionListener implements ActionListener
  {
    Value.Function f;
    ValueProducer vp;
    
    public FunctionListener(Value.Function f, ValueProducer vp)
    {
      this.f = f;
      this.vp = vp;
    }

    public void actionPerformed(ActionEvent e)
    {
      if (vp != null)
        f.apply(vp.getApplyValue()); 
      else f.apply(Value.TRIV); 
    }
  }


  public static class MenuHintChangeListener implements ChangeListener
  {
    private JMenuItem menuItem;
    private String helpText;
    private JLabel statusBar;

    public MenuHintChangeListener(JMenuItem menuItem, String helpText, JLabel statusBar)
    {
      this.menuItem = menuItem;
      this.helpText = helpText;
      this.statusBar = statusBar;
      menuItem.addChangeListener(this);
    }

    public void stateChanged(ChangeEvent evt)
    {
      if (menuItem.isArmed())
        statusBar.setText(helpText);
      else statusBar.setText(" ");
    }
  }


  public static class HintMouseListener extends MouseAdapter  
  {
    private JLabel label;
    private String hint;
        
    public HintMouseListener(String hint, JLabel label)  
    {
      this.label = label;
      this.hint = hint;
    }
        
    public void mouseEntered(MouseEvent evt)  
    {
      label.setText(hint);
    }

    public void mouseExited(MouseEvent evt)  
    {
      label.setText(" ");
    }
  }


  public static JButton createToolBarButton(Value.Structured v, ValueProducer vp, JLabel statusLabel)
  {
    URL imageURL = Menu.class.getResource(((Value.Str) v.cmpnt(2)).getString());

    JButton btn;
    if (imageURL != null) btn = new JButton(new ImageIcon(imageURL));
      else btn = new JButton();

    btn.setRolloverEnabled(true);
    btn.setToolTipText( ((Value.Str) v.cmpnt(0)).getString() ); 
    if (v.cmpnt(3) != Value.TRIV) 
      btn.addActionListener(new FunctionListener((Value.Function) v.cmpnt(3),vp));

    String hint = ((Value.Str) v.cmpnt(1)).getString();
    if (hint.compareTo("") != 0 && statusLabel != null)
      btn.addMouseListener(new HintMouseListener(hint, statusLabel)); 

    return btn;
  }



  /** A dynamic popup menu.  The contents change depending 
      on the type returned by the ValueProducer. 
      If vp is null then TRIV is passed as the parameter. */ 
  public static class PopupMenu extends JPopupMenu
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 599445696401405465L;
	protected ValueProducer vp;
    protected Value.Vector menu;
    protected JLabel hintLabel;
    protected Type lastType;
    protected boolean menuChanged;

    public PopupMenu(String title, ValueProducer vp, Value.Vector menu, JLabel hintLabel)
    {
      super(title);
      this.vp = vp; 
      this.hintLabel = hintLabel;
      setMenu(menu);
    }

    public void setMenu(Value.Vector menu)
    {
      this.menu = menu;
      menuChanged = true;
    }

    public void show(Component invoker, int x, int y)
    {
      Type t = vp.getApplyType();

      if (t != lastType || menuChanged)
      {
        // Build menu.
        removeAll();

        if (menu != null)
        {
          try
          {
            for (int i = 0; i < menu.length(); i++)
            {
              JMenuItem jmi = createMenu( (Value.Structured) menu.elt(i),vp,hintLabel);
              if (jmi != null) add(jmi);
            }
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }

        menuChanged = false;
        lastType = t;
      }
      super.show(invoker,x,y);
    }

  }


  /** The type of a menu item.  If name = "-" then a separator is used. See {@link Menu#MENU}. */
  public static final Type.Structured MENUITEM = 
    new Type.Structured(new Type[] { Type.STRING, Type.STRING, Type.STRING, Type.STRING, 
                          new Type.Union(new Type[] { new Type.Vector(Type.STRUCTURED/*MENUITEM*/), Type.FUNCTION, Type.TRIV }) },
                        new String[] { "Name", "Short desc.", "Long desc.", "Image filepath", "Function|Submenus|Triv" } );


  /** <code>type Menu = (Name,ShortDesc,LongDesc,ImageFile,[Menu]|(t -> t)|())</code>
      <p>
      The Menu type is the type of a recursive menu system.
      Name, ShortDesc, LongDesc and ImageFile are all of type <code>String</code>.
      <code>t -> t</code> represents the function that is called when the menu
      is selected.  Accelerators still need to be implemented.
      If Name == "-" then a separator is used.
  */
  public static final Type.Vector MENU = new Type.Vector(MENUITEM);
 
  /** A static menubar. */ 
  public static class MenuBar extends JMenuBar
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -291861698380870903L;
	protected ValueProducer vp;
    protected JLabel hintLabel;
    protected Type lastType;

    public MenuBar(ValueProducer vp, Value.Vector menu, JLabel hintLabel)
    {
      super();
      this.vp = vp; 
      this.hintLabel = hintLabel;
      setMenu(menu);
    }

    public void setMenu(Value.Vector menu)
    {
      try
      {
        removeAll();
        for (int i = 0; i < menu.length(); i++)
        {
          JMenuItem m = createMenu( (Value.Structured) menu.elt(i),vp,hintLabel);
          if (m != null) add(m);
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }


  /** The type of a toolbar item.  If name = "-" then a separator is used. See {@link Menu#TOOLBAR}. */
  public static final Type.Structured TOOLBARITEM = 
    new Type.Structured(new Type[] { Type.STRING, Type.STRING, Type.STRING,  
                          new Type.Union(new Type[] { Type.FUNCTION, Type.TRIV } ) },
                        new String[] { "Short desc.", "Long desc.", "Image filepath", "Function|Triv" } );
 
  /** <code>type Toolbar = [(ShortDesc,LongDesc,ImageFile,(t -> t))</code>
      <p>
      The Toolbar type is the type of a toolbar value.
      ShortDesc, LongDesc and ImageFile are all of type <code>String</code>.
      <code>t -> t</code> represents the function that is called when the toolbar button
      is pressed.  If Name == "-" then a separator is used.
  */
  public static final Type.Vector TOOLBAR = new Type.Vector(TOOLBARITEM);
 
  /** A static toolbar. */
  public static class ToolBar extends JToolBar
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6597128676757834584L;
	protected ValueProducer vp;
    protected JLabel hintLabel;
    protected Type lastType;

    public ToolBar(ValueProducer vp, Value.Vector menu, JLabel hintLabel)
    {
      super();
      this.vp = vp; 
      this.hintLabel = hintLabel;
      setMenu(menu);
    }

    public void setMenu(Value.Vector menu)
    {
      try
      {
        removeAll();
        for (int i = 0; i < menu.length(); i++)
        {
          Value.Structured elt = (Value.Structured) menu.elt(i);
          if ( ((Value.Str) elt.cmpnt(0)).getString().compareTo("-") == 0)
            addSeparator(); 
          else add( createToolBarButton(elt,vp,hintLabel) );
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

}
