//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Splash.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.desktop;

import java.awt.*;
import javax.swing.*;

/** Splash window. */
public class Splash extends JWindow
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3970499571445199143L;
public static Frame frame = new Frame();

  public static void main(String[] args) 
  {
    new Splash();
  }

  public Splash()
  {
    super(frame);
    JLabel l = new JLabel(new ImageIcon("cdms/plugin/desktop/images/Splash.gif"));
    getContentPane().add(l, BorderLayout.CENTER);
    pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension labelSize = l.getPreferredSize();
    setLocation(screenSize.width/2 - (labelSize.width/2),
                screenSize.height/2 - (labelSize.height/2));
    screenSize = null;
    labelSize = null;
    setVisible(true);
  }
}
