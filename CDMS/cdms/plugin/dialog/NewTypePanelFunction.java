//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewTypeDialogVector.java
// Authors: {joshc}@cs.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import cdms.core.*;

public class NewTypePanelFunction extends NewPanel implements
  TypeFieldSelector.TypeFieldSelectorListener
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1816981290637326096L;
private Type result = null;
  private JPanel northPanel = new JPanel();
  private JPanel checkBoxPanel = new JPanel();
  private TypeFieldSelector paramSelector;
  private TypeFieldSelector resultSelector;
  private JCheckBox checkParamName = new JCheckBox("Check Paramater Type Name", false);
  private JCheckBox checkResultName = new JCheckBox("Check Result Type Name", false);
  private Type.Function tf;

  public Object getResult()
  {
    return result;
  }

  public boolean getInitialOKState()
  {
    return true;
  }

  public NewTypePanelFunction(Type.Function t)
  {
    super();
    tf = t;
    checkParamName.addActionListener(new checkListener());
    checkResultName.addActionListener(new checkListener());
    northPanel.setBorder(BorderFactory.createEtchedBorder());
    checkBoxPanel.setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());
    northPanel.setLayout(new GridLayout(2,1));
    checkBoxPanel.setLayout(new GridLayout(2,1));
    checkBoxPanel.add(checkParamName);
    checkBoxPanel.add(checkResultName);
    checkParamName.setSelected(tf.checkParamName);
    checkResultName.setSelected(tf.checkResultName);
    paramSelector = new TypeFieldSelector("Parameter Type:", "Select Parameter Type...", this,
      tf.param);
    resultSelector = new TypeFieldSelector("Result Type:", "Select Result Type...", this,
      tf.result);
    northPanel.add(paramSelector);
    northPanel.add(resultSelector);
    add(checkBoxPanel, BorderLayout.CENTER);
    add(northPanel, BorderLayout.NORTH);
  }

  private class checkListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      selectionChanged();
    }
  }

  public void selectionChanged()
  {
    if((paramSelector.getSelection() != null)&&(resultSelector.getSelection() != null))
    {
      result = new Type.Function(paramSelector.getSelection(), resultSelector.getSelection(),
        checkParamName.isSelected(), checkResultName.isSelected());
      okListener.okEvent(true);
    }
    else
    {
      result = null;
      okListener.okEvent(false);
    }
  }
}
