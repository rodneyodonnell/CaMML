//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RandomSeedDialog extends CDMSDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8565255585981144935L;
private JLabel label = new JLabel("Enter new seed:");
  private JTextField field = new JTextField("0");
  private JButton ok = new JButton("OK");
  private JButton cancel = new JButton("Cancel");
  private JPanel buttonPanel = new JPanel();
  private JPanel okPanel = new JPanel();
  private JPanel cancelPanel = new JPanel();
  private long seed;

  public RandomSeedDialog()
  {
    super("Choose new random-number-generator seed...");
    field.setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth("12345678901234567890"), getFontMetrics(getFont()).getHeight()));
    buttonPanel.setLayout(new GridLayout(1,2));
    okPanel.add(ok);
    cancelPanel.add(cancel);
    buttonPanel.add(okPanel);
    buttonPanel.add(cancelPanel);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(label, BorderLayout.WEST);
    getContentPane().add(field, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    field.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent e){}
      public void keyTyped(KeyEvent e){} 
      public void keyReleased(KeyEvent e)
      {
        try
        {
          seed = Long.parseLong(field.getText());
          ok.setEnabled(true);
        }
        catch(Exception ex)
        {
          ok.setEnabled(false);
          seed = -1;
        }
      }
    });
    ok.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dispose();
      }
    });
    cancel.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        seed = -1;
        dispose();
      }
    });
    pack();
    center();
    setVisible(true);
  }

  public long getResult()
  {
    return seed;
  }
}
