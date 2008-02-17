//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: CDMSDialog.java
// Author: {joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import java.awt.*;

public abstract class CDMSDialog extends JDialog
{
  private Dimension screenSize;

  public CDMSDialog(String title)
  {
    setTitle(title);
    setModal(true);
//    setResizable(false);
    screenSize = Toolkit.getDefaultToolkit().getScreenSize();
  }

  public void center()
  {
    Dimension panelSize = new Dimension(this.getWidth(), this.getHeight());
    this.setBounds((int)(0.5 * (screenSize.width - panelSize.width)), 
      (int)(0.5 * (screenSize.height - panelSize.height)), panelSize.width, panelSize.height);
  }
}
