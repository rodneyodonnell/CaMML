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

public class NewTypePanelContinuous extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2068239475026837731L;
private JPanel mainPanel = new JPanel();
  private JPanel UPBPanel = new JPanel();
  private JPanel UPBDetailPanel = new JPanel();
  private JPanel LWBPanel = new JPanel();
  private JPanel LWBDetailPanel = new JPanel();
  private JPanel cyclicPanel = new JPanel();
  private JLabel UPBExistsLabel = new JLabel("UPB Exists?");
  private JLabel LWBExistsLabel = new JLabel("LWB Exists?");
  private JLabel cyclicLabel = new JLabel("Cyclic?");
  private JLabel UPBDetailLabel = new JLabel("UPB Details");
  private JLabel LWBDetailLabel = new JLabel("LWB Details");
  private ButtonGroup hasLWBGroup = new ButtonGroup();
  private ButtonGroup hasUPBGroup = new ButtonGroup();
  private ButtonGroup cyclicGroup = new ButtonGroup();
  private ButtonGroup UPBDetailGroup = new ButtonGroup();
  private ButtonGroup LWBDetailGroup = new ButtonGroup();
  private JCheckBox specifyLWB = new JCheckBox("Specify LWB", false);
  private JCheckBox specifyUPB = new JCheckBox("Specify UPB", false);
  private JTextField LWBLabel = new JTextField("0");
  private JTextField UPBLabel = new JTextField("0");
  private JRadioButton unspecifiedLWB = new JRadioButton("Unspecified", true);
  private JRadioButton yesLWB = new JRadioButton("Yes", false);
  private JRadioButton noLWB = new JRadioButton("No", false);
  private JRadioButton unspecifiedUPB = new JRadioButton("Unspecified", true);
  private JRadioButton yesUPB = new JRadioButton("Yes", false);
  private JRadioButton noUPB = new JRadioButton("No", false);
  private JRadioButton unspecifiedCyclic = new JRadioButton("Unspecified", true);
  private JRadioButton yesCyclic = new JRadioButton("Yes", false);
  private JRadioButton noCyclic = new JRadioButton("No", false);
  private JRadioButton incUPBButton = new JRadioButton("Inclusive", false);
  private JRadioButton excUPBButton = new JRadioButton("Exclusive", false);
  private JRadioButton incLWBButton = new JRadioButton("Inclusive", false);
  private JRadioButton excLWBButton = new JRadioButton("Exclusive", false);
  private double upb = 0;
  private double lwb = 0;
  private KeyListener listener;
  private ActionListener specifyUPBListener;
  private ActionListener specifyLWBListener;

  public NewTypePanelContinuous(Type.Continuous t)
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
    UPBDetailGroup.add(incUPBButton);
    UPBDetailGroup.add(excUPBButton);
    LWBDetailGroup.add(incLWBButton);
    LWBDetailGroup.add(excLWBButton);

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

    UPBDetailPanel.setLayout(new GridLayout(4,1));
    UPBDetailPanel.add(UPBDetailLabel);
    UPBDetailPanel.add(excUPBButton);
    UPBDetailPanel.add(incUPBButton);

    LWBDetailPanel.setLayout(new GridLayout(4,1));
    LWBDetailPanel.add(LWBDetailLabel);
    LWBDetailPanel.add(excLWBButton);
    LWBDetailPanel.add(incLWBButton);

    cyclicPanel.setLayout(new GridLayout(4,1));
    cyclicPanel.add(cyclicLabel);
    cyclicPanel.add(yesCyclic);
    cyclicPanel.add(noCyclic);
    cyclicPanel.add(unspecifiedCyclic);

    UPBPanel.setBorder(BorderFactory.createEtchedBorder());
    LWBPanel.setBorder(BorderFactory.createEtchedBorder());
    UPBDetailPanel.setBorder(BorderFactory.createEtchedBorder());
    LWBDetailPanel.setBorder(BorderFactory.createEtchedBorder());
    cyclicPanel.setBorder(BorderFactory.createEtchedBorder());

    mainPanel.setLayout(new GridLayout(2,2));
    mainPanel.add(LWBPanel);
    mainPanel.add(UPBPanel);
    mainPanel.add(LWBDetailPanel);
    mainPanel.add(UPBDetailPanel);
    setLayout(new BorderLayout());
    add(mainPanel, BorderLayout.CENTER);
    add(cyclicPanel, BorderLayout.EAST);

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

  private void setupTypeInfo(Type.Continuous t)
  {
    UPBDetailPanel.setVisible(false);
    LWBDetailPanel.setVisible(false);

    yesUPB.setEnabled(false);
    noUPB.setEnabled(false);
    unspecifiedUPB.setEnabled(false);
    if (t.UPB < Double.MAX_VALUE)
    {
      yesUPB.setSelected(true);
      specifyUPB.setVisible(true);
      UPBDetailPanel.setVisible(true);
      if (t.UPB < Double.MAX_VALUE - Double.MIN_VALUE)
      {
        specifyUPB.setEnabled(false);
        specifyUPB.setSelected(true);
        UPBLabel.setVisible(true);
        UPBLabel.setEnabled(false);
        UPBLabel.setText(""+t.UPB);
      }
      else
      {
        specifyUPB.setEnabled(true);
        specifyUPB.setSelected(false);
        UPBLabel.setVisible(false);
        UPBLabel.setEnabled(true);
      }
    }
    else
    {
      unspecifiedUPB.setEnabled(true);
      specifyUPB.setVisible(false);
      UPBLabel.setVisible(false);
    }

    yesLWB.setEnabled(false);
    noLWB.setEnabled(false);
    unspecifiedLWB.setEnabled(false);
    if (t.LWB > -Double.MAX_VALUE)
    {
      yesLWB.setSelected(true);
      specifyLWB.setVisible(true);
      LWBDetailPanel.setVisible(true);
      if (t.LWB > -Double.MAX_VALUE + Double.MIN_VALUE)
      {
        specifyLWB.setEnabled(false);
        specifyLWB.setSelected(true);
        LWBLabel.setVisible(true);
        LWBLabel.setEnabled(false);
        LWBLabel.setText(""+t.LWB);
      }
      else
      {
        specifyLWB.setEnabled(true);
        specifyLWB.setSelected(false);
        LWBLabel.setVisible(false);
        LWBLabel.setEnabled(true);
      }
    }
    else
    {
      unspecifiedLWB.setEnabled(true);
      specifyLWB.setVisible(false);
      LWBLabel.setVisible(false);
    }

    if (t.ckIsCyclic)
    {
      yesCyclic.setEnabled(false);
      noCyclic.setEnabled(false);
      unspecifiedCyclic.setEnabled(false);
      if(t.isCyclic)
      {
        yesCyclic.setSelected(true);
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

    excUPBButton.setEnabled(true);
    incUPBButton.setEnabled(true);
    incUPBButton.setSelected(true);

    excLWBButton.setEnabled(true);
    incLWBButton.setEnabled(true);
    incLWBButton.setSelected(true);
  }


  public boolean getInitialOKState()
  {
    return true;
  }

  private void setupEvents()
  {
    specifyUPB.addActionListener(specifyUPBListener);

    specifyLWB.addActionListener(specifyLWBListener);

    yesUPB.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(yesUPB.isSelected())
        {
          specifyUPB.setVisible(true);
          UPBLabel.setVisible(specifyUPB.isSelected());
          UPBDetailPanel.setVisible(true);
        }
        else
        {
          if(yesCyclic.isSelected())
          {
            specifyUPB.setVisible(true);
            yesUPB.setSelected(true);
            UPBDetailPanel.setVisible(true);
            UPBLabel.setVisible(specifyUPB.isSelected());
          }
          else
          {
            specifyUPB.setSelected(false);
            specifyUPB.setVisible(false);
            UPBLabel.setText("0");
            UPBLabel.setVisible(false);
            UPBDetailPanel.setVisible(false);
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
          LWBDetailPanel.setVisible(true);
        }
        else
        {
          if(yesCyclic.isSelected())
          {
            specifyLWB.setVisible(true);
            yesLWB.setSelected(true);
            LWBDetailPanel.setVisible(true);
            LWBLabel.setVisible(specifyLWB.isSelected());
          }
          else
          {
            specifyLWB.setSelected(false);
            specifyLWB.setVisible(false);
            LWBLabel.setText("0");
            LWBLabel.setVisible(false);
            LWBDetailPanel.setVisible(false);
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
          specifyLWB.setVisible(true);
          yesLWB.setSelected(true);
          specifyUPB.setVisible(true);
          yesUPB.setSelected(true);
          LWBLabel.setVisible(specifyLWB.isSelected());
          UPBLabel.setVisible(specifyUPB.isSelected());
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
      if (specifyLWB.isSelected())
      {
        if (excLWBButton.isSelected())
          rlwb = lwb + Double.MIN_VALUE;
        else rlwb = lwb; 
      }
      else rlwb = -Double.MAX_VALUE + Double.MIN_VALUE;
    }
    else rlwb = -Double.MAX_VALUE;

    double rupb;
    if (yesUPB.isSelected())
    {
      if (specifyUPB.isSelected())
      {
        if (excUPBButton.isSelected())
          rupb = upb - Double.MIN_VALUE;
        else rupb = upb; 
      }
      else rupb = Double.MAX_VALUE - Double.MIN_VALUE;
    }
    else rupb = Double.MAX_VALUE;

    return new Type.Continuous(rlwb,rupb, !unspecifiedCyclic.isSelected(), yesCyclic.isSelected());
  }
}
