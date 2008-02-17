//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewTypeDialog.java
// Authors: {joshc}@cs.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;

public class SelectDialog extends CDMSDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 391347010580577849L;

private SelectPanel selectPanel;

  private JButton okButton = new JButton("OK");
  private JButton cancelButton = new JButton("Cancel");
  private JPanel buttonPanel = new JPanel();
  private JPanel okPanel = new JPanel();
  private JPanel cancelPanel = new JPanel();
  private Object result = null;

  public SelectDialog(String message, Type root, Type withMember, boolean showValues)
  {
    super(message);
    selectPanel = new SelectPanel("",root,withMember,showValues);
    getContentPane().setLayout(new BorderLayout());
    okPanel.add(okButton);
    cancelPanel.add(cancelButton);
    buttonPanel.setLayout(new GridLayout(1,2));
    buttonPanel.add(okPanel);
    buttonPanel.add(cancelPanel);
    getContentPane().add(selectPanel, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    
    cancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        result = null;
        dispose();
      }
    });

    okButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // Getting the selected Type or Value.
        result = selectPanel.getResult();
        dispose();
      }
    });

    pack();
    center();
    setVisible(true);
  }  

  public Object getResult()
  {
    return result;
  }
}
