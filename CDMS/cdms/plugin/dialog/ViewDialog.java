//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: ViewDialog.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ViewDialog extends CDMSDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5459623759748050338L;
private JButton ok = new JButton("OK");
  private JPanel panel = new JPanel();
  private JPanel buttonPanel = new JPanel();
  private NewPanel ntp;
  private JLabel commentLabel = new JLabel("Comment: ");
  private JLabel comment = new JLabel();

  public ViewDialog(String title,NewPanel ntp,String comment)
  {
    super(title);
    this.ntp = ntp;
    this.comment.setText(comment);
    genericSetup();
  }

  public ViewDialog(String title,NewPanel ntp)
  {
    super(title);
    this.ntp = ntp;
    genericSetup();
  }

  protected void genericSetup()
  {
    NewPanel.recursiveDisable(ntp);
    panel.setLayout(new BorderLayout());
    buttonPanel.add(ok);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    panel.add(commentLabel, BorderLayout.WEST);
    panel.add(comment, BorderLayout.CENTER);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panel, BorderLayout.SOUTH);
    getContentPane().add(ntp, BorderLayout.CENTER);
    ok.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dispose();
      }
    });
    pack();
    center();
    setVisible(true);
  }
}
