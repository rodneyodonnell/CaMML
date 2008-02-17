//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewTypeDialogFunction.java
// Authors: {joshc}@cs.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;

public class TypeFieldSelector extends JPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5358099099201566786L;
private JLabel label = new JLabel();
  private JLabel typeName = new JLabel("?");
  public JButton chooseButton = new JButton("Select...");
  private Type selection = Type.TYPE;
  private TypeFieldSelectorListener parent = null;
  private String selectorMessage;
  private JPanel typeNamePanel = new JPanel();
  private Type rootType;

  public TypeFieldSelector(String s, String sm, TypeFieldSelectorListener p)
  {
    this(s, sm, p, Type.TYPE);
  }

  public TypeFieldSelector(String s, String sm, TypeFieldSelectorListener p, Type root)
  {
    parent = p;
    selection = root;
    rootType = root;
    typeName.setText(selection.getTypeName());
    typeName.setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth("                  "), (int)getFontMetrics(getFont()).getHeight()));
    selectorMessage = sm;
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEtchedBorder());
    label.setText(s);
    add(label, BorderLayout.WEST);
    add(chooseButton, BorderLayout.EAST);
    add(typeNamePanel, BorderLayout.CENTER);
    typeNamePanel.add(typeName);
    chooseButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        selection = (Type)new SelectDialog(selectorMessage, rootType, null, false).getResult();
        if(selection==null)
        { 
          selection = rootType;
        }
        typeName.setText(getTypeName(selection));
        parent.selectionChanged();
      }
    });
  }

  private String getTypeName(Type t)
  {
    String tmp = Type.byType(t);
    if(tmp != null)
    {
      return tmp;
    }
    else
    {
      return t.getTypeName();
    }
  }

  public Type getSelection()
  {
    return selection;
  }

  public interface TypeFieldSelectorListener
  {
    public void selectionChanged();
  }
}
