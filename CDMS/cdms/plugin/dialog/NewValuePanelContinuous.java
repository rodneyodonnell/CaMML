//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewValuePanelContinuous.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;

public class NewValuePanelContinuous extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 489693844803313410L;
private JRadioButton relativeAcc = new JRadioButton("Relative Accuracy");
  private JRadioButton absoluteAcc = new JRadioButton("Absolute Accuracy");
  private JRadioButton asymetricAcc = new JRadioButton("Asymetric Accuracy");
  private ButtonGroup accuracyGroup = new ButtonGroup();
  private JPanel accuracyPanel = new JPanel();
  private JPanel accuracyButtonPanel = new JPanel();
  private JPanel boundPanel = new JPanel();
  private JPanel valuePanel = new JPanel();
  private JPanel relativePanel = new JPanel();
  private JPanel absolutePanel = new JPanel();
  private JPanel asymetricPanel = new JPanel();
  private JTextField valueField = new JTextField();
  private JTextField percentageField = new JTextField();
  private JTextField absField = new JTextField("" + Value.Continuous.DEFAULT_DELTA);
  private JTextField asymUpperField = new JTextField();
  private JTextField asymLowerField = new JTextField();
  private JLabel upperField = new JLabel();
  private JLabel lowerField = new JLabel();
  private JLabel accuracyLabel = new JLabel("Accuracy");
  private JLabel valueLabel = new JLabel("Value: ");
  private JLabel lowerLabel = new JLabel("Lower estimate");
  private JLabel upperLabel = new JLabel("Upper estimate");
  private JLabel absLabel = new JLabel("+/- (abs): ");
  private JLabel relLabel = new JLabel("+/- (%): ");
  private JLabel asymUpperLabel = new JLabel("Upper estimate: ");
  private JLabel asymLowerLabel = new JLabel("Lower estimate: ");

  private Type.Continuous tc;

  public NewValuePanelContinuous(Type.Continuous t)
  {
    this.tc = t;

    accuracyGroup.add(absoluteAcc);
    accuracyGroup.add(relativeAcc);
    accuracyGroup.add(asymetricAcc);
    absoluteAcc.setSelected(true);

    absoluteAcc.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        changeAccuracy();
      }
    });

    relativeAcc.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        changeAccuracy();
      }
    });

    asymetricAcc.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        changeAccuracy();
      }
    });

    valueField.addKeyListener(new ContinuousFieldKeyListener());
    percentageField.addKeyListener(new ContinuousFieldKeyListener());
    absField.addKeyListener(new ContinuousFieldKeyListener());
    asymUpperField.addKeyListener(new ContinuousFieldKeyListener());
    asymLowerField.addKeyListener(new ContinuousFieldKeyListener());

    absolutePanel.setLayout(new GridLayout(1,2));
    absolutePanel.add(absLabel);
    absolutePanel.add(absField);

    relativePanel.setLayout(new GridLayout(1,2));
    relativePanel.add(relLabel);
    relativePanel.add(percentageField);

    asymetricPanel.setLayout(new GridLayout(2,2));
    asymetricPanel.add(asymLowerLabel);
    asymetricPanel.add(asymUpperLabel);
    asymetricPanel.add(asymLowerField);
    asymetricPanel.add(asymUpperField);

    accuracyButtonPanel.setLayout(new GridLayout(3,1));
    accuracyButtonPanel.add(absoluteAcc);
    accuracyButtonPanel.add(relativeAcc);
    accuracyButtonPanel.add(asymetricAcc);

    valuePanel.setLayout(new BorderLayout());
    valuePanel.add(valueLabel, BorderLayout.WEST);
    valuePanel.add(valueField, BorderLayout.CENTER);

    accuracyPanel.setLayout(new BorderLayout());
    accuracyPanel.add(accuracyLabel, BorderLayout.NORTH);
    accuracyPanel.add(absolutePanel, BorderLayout.SOUTH);
    accuracyPanel.add(accuracyButtonPanel, BorderLayout.CENTER);

    boundPanel.setLayout(new GridLayout(2,2));
    boundPanel.add(lowerLabel);
    boundPanel.add(lowerField);
    boundPanel.add(upperLabel);
    boundPanel.add(upperField);

    setLayout(new BorderLayout());
    add(valuePanel, BorderLayout.NORTH);
    add(accuracyPanel, BorderLayout.CENTER);
    add(boundPanel, BorderLayout.SOUTH);

    changeAccuracy();
  }

  public boolean getInitialOKState()
  {
    return false;
  }

  private void changeAccuracy()
  {
    if(absoluteAcc.isSelected())
    {
      relativePanel.setVisible(false);
      asymetricPanel.setVisible(false);
      accuracyPanel.add(absolutePanel, BorderLayout.SOUTH);
      absolutePanel.setVisible(true);
      doAbsoluteAccuracy();
      validate();
    }
    if(relativeAcc.isSelected())
    {
      absolutePanel.setVisible(false);
      asymetricPanel.setVisible(false);
      accuracyPanel.add(relativePanel, BorderLayout.SOUTH);
      relativePanel.setVisible(true);
      doRelativeAccuracy();
      validate();
    }
    if(asymetricAcc.isSelected())
    {
      relativePanel.setVisible(false);
      absolutePanel.setVisible(false);
      accuracyPanel.add(asymetricPanel, BorderLayout.SOUTH);
      asymetricPanel.setVisible(true);
      doAsymetricAccuracy();
      validate();
    }
  }

  private void doAccuracy()
  {
    if(absolutePanel.isVisible())
    {
      doAbsoluteAccuracy();
    }
    else if(relativePanel.isVisible())
    {
      doRelativeAccuracy();
    }
    else if(asymetricPanel.isVisible())
    {
      doAsymetricAccuracy();
    }
  }

  private void doAbsoluteAccuracy()
  {
    try
    {
      double x = Double.parseDouble(valueField.getText());
      double d = Double.parseDouble(absField.getText());
      lowerField.setText(""+java.lang.Math.max(x-d,tc.LWB));
      upperField.setText(""+java.lang.Math.min(x+d,tc.UPB));
      if(okListener != null) okListener.okEvent(true);
    }
    catch(Exception e)
    {
      if(okListener != null) okListener.okEvent(false);
    }
  }

  private void doRelativeAccuracy()
  {
    try
    {
      double x = Double.parseDouble(valueField.getText());
      double d = Double.parseDouble(percentageField.getText());
      lowerField.setText(""+java.lang.Math.max(x-x*0.01*d,tc.LWB));
      upperField.setText(""+java.lang.Math.min(x+x*0.01*d,tc.UPB));
      if(okListener != null) okListener.okEvent(true);
    }
    catch(Exception e)
    {
      if(okListener != null) okListener.okEvent(false);
    }
  }

  private void doAsymetricAccuracy()
  {
    try
    {
      double x = Double.parseDouble(valueField.getText());
      double l = Double.parseDouble(asymLowerField.getText());
      double u = Double.parseDouble(asymUpperField.getText());
      if((l<=x) && (x<=u))
      {
        lowerField.setText(""+java.lang.Math.max(l,tc.LWB));
        upperField.setText(""+java.lang.Math.min(u,tc.UPB));
        if(okListener != null) okListener.okEvent(true);
      }
      else
      {
        if(okListener != null) okListener.okEvent(false);
      }
    }
    catch(Exception e)
    {
      if(okListener != null) okListener.okEvent(false);
    }
  }

  public Object getResult()
  {
    ValueStatus vs = Value.S_PROPER;
    if (Double.parseDouble(valueField.getText()) > tc.UPB)
      vs = Value.S_INVALID;
    if(Double.parseDouble(valueField.getText()) < tc.LWB)
      vs = Value.S_INVALID;

    if(absolutePanel.isVisible())
    {
      return new Value.Continuous(tc, vs, Double.parseDouble(valueField.getText()), 
                                  Double.parseDouble(absField.getText()));
    }
    else if(relativePanel.isVisible())
    {
      return new Value.RelativeContinuous(tc, vs, Double.parseDouble(valueField.getText()), 
                                          0.01 * Double.parseDouble(percentageField.getText()));
    }
    else if(asymetricPanel.isVisible())
    {
      return new Value.AsymetricContinuous(tc, vs, Double.parseDouble(valueField.getText()), 
                                           Double.parseDouble(asymLowerField.getText()), 
                                           Double.parseDouble(asymUpperField.getText()));
    }
    return Value.TRIV;
  }

  private class ContinuousFieldKeyListener implements KeyListener
  {
    public void keyPressed(KeyEvent e){}

    public void keyReleased(KeyEvent e)
    {
      doAccuracy();
    }

    public void keyTyped(KeyEvent e){}
  }
}
