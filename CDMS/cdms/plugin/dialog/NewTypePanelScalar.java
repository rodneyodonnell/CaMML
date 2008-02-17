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
import javax.swing.event.*;

public class NewTypePanelScalar extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7508154881603411798L;
private JPanel UPBPanel = new JPanel();
  private JPanel LWBPanel = new JPanel();
  private JPanel cyclicPanel = new JPanel();
  private JLabel UPBExistsLabel = new JLabel("UPB Exists?");
  private JLabel LWBExistsLabel = new JLabel("LWB Exists?");
  private JLabel cyclicLabel = new JLabel("Cyclic?");
  private ButtonGroup hasLWBGroup = new ButtonGroup();
  private ButtonGroup hasUPBGroup = new ButtonGroup();
  private ButtonGroup cyclicGroup = new ButtonGroup();
  public JCheckBox specifyLWB = new JCheckBox("Specify LWB", false);
  public JCheckBox specifyUPB = new JCheckBox("Specify UPB", false);
  public JTextField LWBLabel = new JTextField("0");
  public JTextField UPBLabel = new JTextField("0");
  public JRadioButton unspecifiedLWB = new JRadioButton("Unspecified", true);
  public JRadioButton yesLWB = new JRadioButton("Yes", false);
  public JRadioButton noLWB = new JRadioButton("No", false);
  public JRadioButton unspecifiedUPB = new JRadioButton("Unspecified", true);
  public JRadioButton yesUPB = new JRadioButton("Yes", false);
  public JRadioButton noUPB = new JRadioButton("No", false);
  public JRadioButton unspecifiedCyclic = new JRadioButton("Unspecified", true);
  public JRadioButton yesCyclic = new JRadioButton("Yes", false);
  public JRadioButton noCyclic = new JRadioButton("No", false);
  private double upb = 0;
  private double lwb = 0;
  public KeyListener listener;
  public ActionListener specifyUPBListener;
  public ActionListener specifyLWBListener;

  public NewTypePanelScalar(Type.Scalar t)
  {
    super();
    hasLWBGroup.add(unspecifiedLWB);
    hasLWBGroup.add(yesLWB);
    hasLWBGroup.add(noLWB);
    hasUPBGroup.add(unspecifiedUPB);
    hasUPBGroup.add(yesUPB);
    hasUPBGroup.add(noUPB);
    cyclicGroup.add(unspecifiedCyclic);
    cyclicGroup.add(yesCyclic);
    cyclicGroup.add(noCyclic);

    UPBPanel.setLayout(new GridLayout(6,1));
    UPBPanel.add(UPBExistsLabel);
    UPBPanel.add(yesUPB);
    UPBPanel.add(noUPB);
    UPBPanel.add(unspecifiedUPB);
    UPBPanel.add(specifyUPB);
    UPBPanel.add(UPBLabel);

    LWBPanel.setLayout(new GridLayout(6,1));
    LWBPanel.add(LWBExistsLabel);
    LWBPanel.add(yesLWB);
    LWBPanel.add(noLWB);
    LWBPanel.add(unspecifiedLWB);
    LWBPanel.add(specifyLWB);
    LWBPanel.add(LWBLabel);

    cyclicPanel.setLayout(new GridLayout(4,1));
    cyclicPanel.add(cyclicLabel);
    cyclicPanel.add(yesCyclic);
    cyclicPanel.add(noCyclic);
    cyclicPanel.add(unspecifiedCyclic);

    UPBPanel.setBorder(BorderFactory.createEtchedBorder());
    LWBPanel.setBorder(BorderFactory.createEtchedBorder());
    cyclicPanel.setBorder(BorderFactory.createEtchedBorder());

    setLayout(new GridLayout(1,3));
    add(LWBPanel);
    add(UPBPanel);
    add(cyclicPanel);

    listener = new KeyListener()
    {
      public void keyPressed(KeyEvent e){}
      public void keyTyped(KeyEvent e){} 
      public void keyReleased(KeyEvent e)
      {
        checkBoundsFields();
      }
    };

    specifyUPBListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        UPBLabel.setText("0");
        if(specifyUPB.isSelected())
        {
          UPBLabel.setVisible(true);
        }
        else
        {
          UPBLabel.setVisible(false);
        }
        checkBoundsFields();
      }
    };

    specifyLWBListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        LWBLabel.setText("0");
        if(specifyLWB.isSelected())
        {
          LWBLabel.setVisible(true);
        }
        else
        {
          LWBLabel.setVisible(false);
        }
        checkBoundsFields();
      }
    };

    setupTypeInfo(t);
    setupEvents();
  }

  public void setupTypeInfo(Type.Scalar t)
  {
    if(t.ckIsCyclic)
    {
      yesCyclic.setEnabled(false);
      noCyclic.setEnabled(false);
      unspecifiedCyclic.setEnabled(false);
      if(t.isCyclic)
      {
        yesCyclic.setSelected(true);
        yesUPB.setEnabled(false);
        noUPB.setEnabled(false);
        unspecifiedUPB.setEnabled(false);
        yesLWB.setEnabled(false);
        noLWB.setEnabled(false);
        unspecifiedLWB.setEnabled(false);
      }
      else
      {
        noCyclic.setSelected(true);
      }
    }
    else
    {
      unspecifiedCyclic.setSelected(true);
      yesCyclic.setEnabled(true);
      noCyclic.setEnabled(true);
      unspecifiedCyclic.setEnabled(true);
    }
  }

  public boolean getInitialOKState()
  {
    return true;
  }

  private void setupEvents()
  {
    specifyLWB.addActionListener(specifyLWBListener);

    specifyUPB.addActionListener(specifyUPBListener);

    yesUPB.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(yesUPB.isSelected())
        {
          specifyUPB.setVisible(true);
          UPBLabel.setVisible(specifyUPB.isSelected());
        }
        else
        {
          if(yesCyclic.isSelected())
          {
            specifyUPB.setVisible(true);
            yesUPB.setSelected(true);
            UPBLabel.setVisible(specifyUPB.isSelected());
          }
          else
          {
            specifyUPB.setSelected(false);
            specifyUPB.setVisible(false);
            UPBLabel.setText("0");
            UPBLabel.setVisible(false);
          }
        }
      }
    });

    yesLWB.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(yesLWB.isSelected())
        {
          specifyLWB.setVisible(true);
          LWBLabel.setVisible(specifyLWB.isSelected());
        }
        else
        {
          if(yesCyclic.isSelected())
          {
            specifyLWB.setVisible(true);
            yesLWB.setSelected(true);
            LWBLabel.setVisible(specifyLWB.isSelected());
          }
          else
          {
            specifyLWB.setSelected(false);
            specifyLWB.setVisible(false);
            LWBLabel.setText("0");
            LWBLabel.setVisible(false);
          }
        }
      }
    });

    UPBLabel.addKeyListener(listener);

    LWBLabel.addKeyListener(listener);

    yesCyclic.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(yesCyclic.isSelected())
        {
          if(yesUPB.isEnabled() && yesLWB.isEnabled())
          {
            specifyLWB.setVisible(true);
            yesLWB.setSelected(true);
            specifyUPB.setVisible(true);
            yesUPB.setSelected(true);
            LWBLabel.setVisible(specifyLWB.isSelected());
            UPBLabel.setVisible(specifyUPB.isSelected());
          }
          else
          {
            if((!yesUPB.isSelected()) || (!yesLWB.isSelected()))
            {
              noCyclic.setSelected(true);
            }
          }
        }
      }
    });
  }

  private void checkBoundsFields()
  {
    if(specifyUPB.isSelected() || specifyLWB.isSelected())
    {
      try
      {
        upb = Double.parseDouble(UPBLabel.getText());
        lwb = Double.parseDouble(LWBLabel.getText());
        if(specifyUPB.isSelected() && specifyLWB.isSelected())
        {
          okListener.okEvent(upb > lwb);
        }
        else
        {
          okListener.okEvent(true);
        }
      }
      catch(Exception e)
      {
        okListener.okEvent(false);
      }
    }
    else
    {
        okListener.okEvent(true);
    }
  }

  public Object getResult()
  {
    double rlwb;
    if (yesLWB.isSelected())
    {
      if (specifyLWB.isSelected()) rlwb = lwb;
      else rlwb = -Double.MAX_VALUE + Double.MIN_VALUE;
    }
    else rlwb = -Double.MAX_VALUE;

    double rupb;
    if (yesUPB.isSelected())
    {
      if (specifyUPB.isSelected()) rupb = upb;
      else rupb = Double.MAX_VALUE - Double.MIN_VALUE;
    }
    else rupb = Double.MAX_VALUE;

    return new Type.Scalar(rlwb, rupb, !unspecifiedCyclic.isSelected(), yesCyclic.isSelected());
  }
}
