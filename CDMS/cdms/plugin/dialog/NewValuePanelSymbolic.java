//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewValuePanelSymbolic.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;

public class NewValuePanelSymbolic extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2603784665028801780L;
private JComboBox cb = new JComboBox();
  private Type.Symbolic ts;

  public NewValuePanelSymbolic(Type.Symbolic t)
  {
    ts = t;
    if(ts.ckValues)
    {
      int count;
      for(count = 0; count < ts.UPB+1; count++)
      {
        cb.addItem(count + ": " + ts.ids[count]);
      }
      cb.setSelectedIndex(0);
      setLayout(new GridLayout(1,1));
      add(cb);
    }
  }

  public boolean getInitialOKState()
  {
    return true;
  }

  public Object getResult()
  {
    if(ts.ckValues)
    {
      return new Value.Discrete(ts, cb.getSelectedIndex());
    }
    else
    {
      return new Value.Discrete(ts, 0);
    }
  }
}
