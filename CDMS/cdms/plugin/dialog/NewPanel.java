//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewTypeDialog.java
// Authors: {joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import java.awt.*;

public abstract class NewPanel extends JPanel
{
  public OKListener okListener;

  public NewPanel()
  {
    super();
  }

  public static void recursiveDisable(Component n)
  {
    if (Container.class.isInstance(n))
    {
      Component[] comps = ((Container)n).getComponents();
      int count;
      for(count = 0; count < comps.length; count++)
      {
        recursiveDisable(comps[count]);
      }
    }
    n.setEnabled(false);
    if (JScrollPane.class.isInstance(n))
    {
      n.setEnabled(true);
    }
    if (JScrollBar.class.isInstance(n))
    {
      n.setEnabled(true);
    }
    if (JPanel.class.isInstance(n))
    {
      n.setEnabled(true);
    }
  }

  public abstract boolean getInitialOKState();

  public abstract Object getResult();

  public interface OKListener
  {
    public void okEvent(boolean enabled);
  }
}
