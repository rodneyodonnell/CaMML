//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.dialog;

import cdms.core.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;


public class AddToEnvironmentDialog extends CDMSDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5637347071372177576L;
	
  private JTextField comment = new JTextField("012345678901234567890");
  private JLabel lComment = new JLabel("Comment: ");
  private JPanel pComment = new JPanel();
  private JTextField name = new JTextField("01234567890");
  private JLabel lName = new JLabel("Name: ");
  private JPanel pName = new JPanel();
  private JButton ok = new JButton("OK");
  private JButton cancel = new JButton("Cancel");
  private JPanel pButton = new JPanel();
  private Value val;
  

  public AddToEnvironmentDialog(Value v)
  {
    super("Add Value to Environment...");
    val = v;
    ok.addActionListener(new ActionListener()
    { 
      public void actionPerformed(ActionEvent e)
      {
        Environment.env.add(name.getText(), "Standard", val, comment.getText());
        dispose();
      }
    });
    cancel.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dispose();
      }
    });
    name.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent e)
      {}
      public void keyTyped(KeyEvent e)
      {}
      public void keyReleased(KeyEvent e)
      {
        try
        {
          if(Environment.env.getObject(name.getText(), "Standard") != null)
          {
            ok.setEnabled(false);
          }
          else
          {
            ok.setEnabled(true);
          }
        }
        catch(Exception ex)
        {
          ok.setEnabled(true);
        }
      }
    });
    getContentPane().setLayout(new GridLayout(3,1));
    pName.setLayout(new BorderLayout());
    pName.add(name, BorderLayout.CENTER);
    pName.add(lName, BorderLayout.WEST);
    pComment.setLayout(new BorderLayout());
    pComment.add(comment, BorderLayout.CENTER);
    pComment.add(lComment, BorderLayout.WEST);
    pButton.setLayout(new GridLayout(1,2));
    pButton.add(ok);
    pButton.add(cancel);
    getContentPane().add(pName);
    getContentPane().add(pComment);
    getContentPane().add(pButton);
    pack();
    comment.setText("");
    name.setText("");
    center();
    setVisible(true);
  }
}
