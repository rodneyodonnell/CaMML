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

public class NewTypePanelDiscrete extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6415984987271440091L;
private JPanel orderedPanel = new JPanel();
  private NewTypePanelScalar scalarPanel;
  private JLabel orderedLabel = new JLabel("Ordered?");
  private JRadioButton yesOrdered = new JRadioButton("Yes", false);
  private JRadioButton noOrdered = new JRadioButton("No", false);
  private JRadioButton unspecifiedOrdered = new JRadioButton("Unspecified", true);
  private ButtonGroup orderedGroup = new ButtonGroup();
  private int upb = 0;
  private int lwb = 0;
  private KeyListener listener;
  private ActionListener specifyUPBListener;
  private ActionListener specifyLWBListener;

  public NewTypePanelDiscrete(Type.Discrete t)
  {
    scalarPanel = new NewTypePanelScalar(t);
    orderedGroup.add(yesOrdered);
    orderedGroup.add(noOrdered);
    orderedGroup.add(unspecifiedOrdered);
    orderedPanel.setLayout(new GridLayout(4,1));
    orderedPanel.add(orderedLabel);
    orderedPanel.add(yesOrdered);
    orderedPanel.add(noOrdered);
    orderedPanel.add(unspecifiedOrdered);
    orderedPanel.setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());
    add(scalarPanel, BorderLayout.CENTER);
    add(orderedPanel, BorderLayout.EAST);
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
        scalarPanel.UPBLabel.setText("0");
        if(scalarPanel.specifyUPB.isSelected())
        {
          scalarPanel.UPBLabel.setVisible(true);
        }
        else
        {
          scalarPanel.UPBLabel.setVisible(false);
        }
        checkBoundsFields();
      }
    };
    specifyLWBListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        scalarPanel.LWBLabel.setText("0");
        if(scalarPanel.specifyLWB.isSelected())
        {
          scalarPanel.LWBLabel.setVisible(true);
        }
        else
        {
          scalarPanel.LWBLabel.setVisible(false);
        }
        checkBoundsFields();
      }
    };
    scalarPanel.yesCyclic.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(scalarPanel.yesCyclic.isSelected())
        {
          yesOrdered.setSelected(true);
        }
      }
    });
    noOrdered.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(noOrdered.isSelected())
        {
          scalarPanel.noCyclic.setSelected(true);
        }
      }
    });
    unspecifiedOrdered.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        if((unspecifiedOrdered.isSelected()) && (scalarPanel.yesCyclic.isSelected()))
        {
          scalarPanel.unspecifiedCyclic.setSelected(true);
        }
      }
    });
    scalarPanel.specifyUPB.removeActionListener(scalarPanel.specifyUPBListener);
    scalarPanel.specifyLWB.removeActionListener(scalarPanel.specifyLWBListener);
    scalarPanel.LWBLabel.removeKeyListener(scalarPanel.listener);
    scalarPanel.UPBLabel.removeKeyListener(scalarPanel.listener);
    scalarPanel.specifyUPB.addActionListener(specifyUPBListener);
    scalarPanel.specifyLWB.addActionListener(specifyLWBListener);
    scalarPanel.LWBLabel.addKeyListener(listener);
    scalarPanel.UPBLabel.addKeyListener(listener);
    setupTypeInfo(t);
  }

  private void setupTypeInfo(Type.Discrete t)
  {
    scalarPanel.yesUPB.setEnabled(false);
    scalarPanel.noUPB.setEnabled(false);
    scalarPanel.unspecifiedUPB.setEnabled(false);
    if (t.UPB < Integer.MAX_VALUE)
    {
      scalarPanel.yesUPB.setSelected(true);
      scalarPanel.specifyUPB.setVisible(true);
      if (t.UPB < Integer.MAX_VALUE - 1)
      {
        scalarPanel.specifyUPB.setEnabled(false);
        scalarPanel.specifyUPB.setSelected(true);
        scalarPanel.UPBLabel.setVisible(true);
        scalarPanel.UPBLabel.setEnabled(false);
        scalarPanel.UPBLabel.setText(""+t.UPB);
      }
      else
      {
        scalarPanel.specifyUPB.setEnabled(true);
        scalarPanel.specifyUPB.setSelected(false);
        scalarPanel.UPBLabel.setVisible(false);
        scalarPanel.UPBLabel.setEnabled(true);
      }
    }
    else
    {
      scalarPanel.unspecifiedUPB.setEnabled(true);
      scalarPanel.specifyUPB.setVisible(false);
      scalarPanel.UPBLabel.setVisible(false);
    }

    scalarPanel.yesLWB.setEnabled(false);
    scalarPanel.noLWB.setEnabled(false);
    scalarPanel.unspecifiedLWB.setEnabled(false);
    if (t.LWB > Integer.MIN_VALUE)
    {
      scalarPanel.yesLWB.setSelected(true);
      scalarPanel.specifyLWB.setVisible(true);
      if (t.LWB > Integer.MIN_VALUE+1)
      {
        scalarPanel.specifyLWB.setEnabled(false);
        scalarPanel.specifyLWB.setSelected(true);
        scalarPanel.LWBLabel.setVisible(true);
        scalarPanel.LWBLabel.setEnabled(false);
        scalarPanel.LWBLabel.setText(""+t.LWB);
      }
      else
      {
        scalarPanel.specifyLWB.setEnabled(true);
        scalarPanel.specifyLWB.setSelected(false);
        scalarPanel.LWBLabel.setVisible(false);
        scalarPanel.LWBLabel.setEnabled(true);
      }
    }
    else
    {
      scalarPanel.unspecifiedUPB.setEnabled(true);
      scalarPanel.specifyLWB.setVisible(false);
      scalarPanel.LWBLabel.setVisible(false);
    }

    if (t.ckIsOrdered)
    {
      yesOrdered.setEnabled(false);
      noOrdered.setEnabled(false);
      unspecifiedOrdered.setEnabled(false);
      if(t.isOrdered)
      {
        yesOrdered.setSelected(true);
      }
      else
      {
        noOrdered.setSelected(true);
        scalarPanel.noCyclic.setSelected(true);
        scalarPanel.yesCyclic.setEnabled(false);
        scalarPanel.noCyclic.setEnabled(false);
        scalarPanel.unspecifiedCyclic.setEnabled(false);
      }
    }
    else
    {
      yesOrdered.setEnabled(true);
      noOrdered.setEnabled(true);
      unspecifiedOrdered.setEnabled(true);
      unspecifiedOrdered.setSelected(true);
    }
    scalarPanel.setupTypeInfo(t);
  }

  private void checkBoundsFields()
  {
    if (scalarPanel.specifyUPB.isSelected() || scalarPanel.specifyLWB.isSelected())
    {
      try
      {
        if(scalarPanel.UPBLabel.isEnabled())
        {
          upb = Integer.parseInt(scalarPanel.UPBLabel.getText());
        }
        else
        {
          upb = (int)Double.parseDouble(scalarPanel.UPBLabel.getText());
        }
        if(scalarPanel.LWBLabel.isEnabled())
        {
          lwb = Integer.parseInt(scalarPanel.LWBLabel.getText());
        }
        else
        {
          lwb = (int)Double.parseDouble(scalarPanel.LWBLabel.getText());
        }        

        if(scalarPanel.specifyUPB.isSelected() && scalarPanel.specifyLWB.isSelected())
        {
          okListener.okEvent(upb >= lwb);
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

  public boolean getInitialOKState()
  {
    return true;
  }

  public Object getResult()
  {
    int rlwb;
    if (scalarPanel.yesLWB.isSelected())
    {
      if (scalarPanel.specifyLWB.isSelected())
        rlwb = lwb;
      else rlwb = Integer.MIN_VALUE + 1;
    }
    else rlwb = Integer.MIN_VALUE;

    double rupb;
    if (scalarPanel.yesUPB.isSelected())
    {
      if (scalarPanel.specifyUPB.isSelected())
        rupb = upb;
      else rupb = Integer.MAX_VALUE - 1;
    }
    else rupb = Integer.MAX_VALUE;

    return new Type.Discrete(rlwb,rupb, 
                               !scalarPanel.unspecifiedCyclic.isSelected(), 
                               !scalarPanel.yesCyclic.isSelected(),
                               !unspecifiedOrdered.isSelected(),
                               yesOrdered.isSelected());
  }
}
