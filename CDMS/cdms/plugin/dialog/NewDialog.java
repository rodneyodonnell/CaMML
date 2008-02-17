//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewDialog.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;

public class NewDialog extends CDMSDialog implements NewPanel.OKListener
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6447500765559666903L;
protected JPanel southPanel = new JPanel();
  protected JPanel okPanel = new JPanel();
  protected JPanel cancelPanel = new JPanel();
  protected JPanel okCancelPanel = new JPanel();
  protected NewPanel centerPanel;
  protected JButton cancelButton = new JButton("Cancel");
  protected JButton okButton = new JButton("OK");
  protected JCheckBox addToEnvironment = new JCheckBox("Add to Environment", false);
  protected JTextField name = new JTextField("123334567890");
  protected JTextField comment = new JTextField("");
  protected JLabel commentLabel = new JLabel("Comment: ");
  protected JPanel commentPanel = new JPanel();
  protected JLabel nameLabel = new JLabel("Name: ");
  protected JPanel namePanel = new JPanel();
  protected JPanel bigSouthPanel = new JPanel();
  protected Object result = null;
  protected boolean badText = true;
  protected boolean badDetails = false;
  protected Environment env;

  public NewDialog(String title, NewPanel centerPanel, Environment env)
  {
    super(title);
    this.centerPanel = centerPanel;
    this.env = env;
  }

  protected void genericSetup(boolean forceAdd)
  {
    centerPanel.okListener = this;
    getContentPane().setLayout(new BorderLayout());
    name.addKeyListener(new NewNameKeyListener());
    southPanel.setLayout(new BorderLayout());
    southPanel.add(addToEnvironment, BorderLayout.WEST);
    namePanel.setLayout(new BorderLayout());
    namePanel.add(name, BorderLayout.CENTER);
    namePanel.add(nameLabel, BorderLayout.WEST);
    southPanel.add(namePanel, BorderLayout.CENTER);

    if (forceAdd)
    {
      addToEnvironment.setSelected(true);
      addToEnvironment.setEnabled(false);
      name.setEnabled(true);
      name.setVisible(true);
      comment.setVisible(true);
    }
    else
    {
      addToEnvironment.setSelected(true);
      addToEnvironment.setEnabled(true);
      name.setEnabled(true);
      name.setVisible(true);
      comment.setVisible(true);
    }

    okPanel.add(okButton);
    badDetails = !centerPanel.getInitialOKState();
    cancelPanel.add(cancelButton);
    okCancelPanel.setLayout(new GridLayout(1,2));
    okCancelPanel.add(okPanel);
    okCancelPanel.add(cancelPanel);
    southPanel.add(okCancelPanel, BorderLayout.SOUTH);
    commentPanel.setLayout(new BorderLayout());
    commentPanel.add(comment, BorderLayout.CENTER);
    commentPanel.add(commentLabel, BorderLayout.WEST);
    bigSouthPanel.setLayout(new BorderLayout());
    bigSouthPanel.add(commentPanel, BorderLayout.CENTER);
    bigSouthPanel.add(southPanel, BorderLayout.SOUTH);
    
    getContentPane().add(bigSouthPanel, BorderLayout.SOUTH);
    getContentPane().add(centerPanel, BorderLayout.CENTER);

    addToEnvironment.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if(addToEnvironment.isSelected())
        {
          name.setEnabled(true);
          name.setVisible(true);
          comment.setVisible(true);
          setOKButton();
        }
        else
        {
          name.setText("");
          name.setEnabled(false);
          name.setVisible(false);
          comment.setVisible(false);
          setOKButton();
        }
      }
    });


    setupButtons();
    pack();
    name.setText("");
    center();
    setVisible(true);
  }

  private void setupButtons()
  {
    okButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        result = centerPanel.getResult();
        if(addToEnvironment.isSelected())
        {
          env.add(name.getText(), "Standard", result, comment.getText());
        }
        dispose();
      }
    });
    cancelButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        result = null;
        dispose();
      }
    });
    setOKButton();
  }

  public Object getResult()
  {
    return result;
  }

  private void setOKButton()
  {
    if(addToEnvironment.isSelected())
    {
      okButton.setEnabled((!badText)&&(!badDetails));
    }
    else
    {
      okButton.setEnabled(!badDetails);
    }
  }

  public void okEvent(boolean enabled)
  {
    badDetails = !enabled;
    setOKButton();
  }

  private class NewNameKeyListener implements java.awt.event.KeyListener
  {
    public void keyPressed(KeyEvent e){}
    public void keyTyped(KeyEvent e){} 
    public void keyReleased(KeyEvent e)
    {
      String tmp = name.getText();
      if ((env.getObject(tmp,"Standard") != null) || (tmp.length() == 0))
      {
        badText = true;
        setOKButton();
      }
      else
      {
        badText = false;
        setOKButton();
      }
    }
  }
}
