//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewValuePanelString.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;

public class NewValuePanelString extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8556734151753140266L;
private JTextField s = new JTextField();
  private JLabel sl = new JLabel("Enter string: ");
  private Type.Str ts;

  public NewValuePanelString(Type.Str t)
  {
    ts = t;
    s.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent e){}

      public void keyReleased(KeyEvent e)
      {
        if(okListener != null)
        {
          if(s.getText().length() > 0)
          {
            okListener.okEvent(true);
          }
          else
          {
            okListener.okEvent(false);
          }
        }
      }

      public void keyTyped(KeyEvent e){}
    });
    setLayout(new GridLayout(1,2));
    add(sl);
    add(s);
  }

  public boolean getInitialOKState()
  {
    return false;
  }

  public Object getResult()
  {
    return new Value.Str(ts, s.getText());
  }
}
