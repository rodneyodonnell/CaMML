//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewValuePanelDiscrete.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;

public class NewValuePanelDiscrete extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4222601717092464410L;
private JTextField valueField = new JTextField();
  private JLabel valueLabel = new JLabel("Value: ");
  protected int result = 0;
  private Type.Discrete td;

  public NewValuePanelDiscrete(Type.Discrete t)
  {
    td = t;
    valueField.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent e){}

      public void keyReleased(KeyEvent e)
      {
        try
        {
          result = Integer.parseInt(valueField.getText());
          if(okListener != null) okListener.okEvent(true);
        }
        catch(Exception ex)
        {
          if(okListener != null) okListener.okEvent(false);
        }
      }

      public void keyTyped(KeyEvent e){}
    });

    setLayout(new GridLayout(1,2));
    add(valueLabel);
    add(valueField);
  }

  public boolean getInitialOKState()
  {
    return false;
  }

  public Object getResult()
  {
    ValueStatus vs = Value.S_PROPER;
    if (result > td.UPB) vs = Value.S_INVALID;
    if(result < td.LWB) vs = Value.S_INVALID;
    return new Value.Discrete(td, vs, result);
  }
}
