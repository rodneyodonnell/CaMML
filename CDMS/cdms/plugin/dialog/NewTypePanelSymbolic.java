//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewTypePanelSymbolic.java
// Authors: {joshc}@cs.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class NewTypePanelSymbolic extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3860632693501744298L;
private JPanel namesPanel = new JPanel();
  private JScrollPane scroll = new JScrollPane(namesPanel);
  private NamePanel[] panels = new NamePanel[0];
  private JPanel scrollPanel = new JPanel();
  private JButton applyButton = new JButton("Apply");
  private JPanel applyPanel = new JPanel();
  private JTextField upbField = new JTextField("");
  private JCheckBox ckNcmpnts = new JCheckBox("Define number of values", false);
  private JLabel ordered = new JLabel("Ordered");
  private JPanel orderedPanel = new JPanel();
  private JRadioButton yesOrdered = new JRadioButton("Yes", false);
  private JRadioButton noOrdered = new JRadioButton("No", false);
  private JRadioButton unspecifiedOrdered = new JRadioButton("Unspecified", true);
  private JLabel cyclic = new JLabel("Cyclic");
  private JPanel cyclicPanel = new JPanel();
  private JRadioButton yesCyclic = new JRadioButton("Yes", false);
  private JRadioButton noCyclic = new JRadioButton("No", false);
  private JRadioButton unspecifiedCyclic = new JRadioButton("Unspecified", true);
  private JPanel orderedCyclicPanel = new JPanel();
  private ButtonGroup orderedGroup = new ButtonGroup();
  private ButtonGroup cyclicGroup = new ButtonGroup();

  public NewTypePanelSymbolic(Type.Symbolic t)
  {
    setUpperBound(1);
    applyPanel.setBorder(BorderFactory.createEtchedBorder());
    applyPanel.setLayout(new GridLayout(1,3));
    applyPanel.add(ckNcmpnts);
    applyPanel.add(upbField);
    applyPanel.add(applyButton);
    upbField.setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth("        "), getFontMetrics(getFont()).getHeight()));
    scroll.setPreferredSize(new Dimension(10 * getFontMetrics(getFont()).stringWidth("        "), 10 * getFontMetrics(getFont()).getHeight()));
    scrollPanel.setBorder(BorderFactory.createEtchedBorder());
    scrollPanel.setPreferredSize(scroll.getPreferredSize());
    scrollPanel.setLayout(new GridLayout(1,1));
    scrollPanel.add(scroll);
    orderedPanel.setBorder(BorderFactory.createEtchedBorder());
    orderedPanel.setLayout(new GridLayout(4,1));
    orderedPanel.add(ordered);
    orderedPanel.add(yesOrdered);
    orderedPanel.add(noOrdered);
    orderedPanel.add(unspecifiedOrdered);
    cyclicPanel.setBorder(BorderFactory.createEtchedBorder());
    cyclicPanel.setLayout(new GridLayout(4,1));
    cyclicPanel.add(cyclic);
    cyclicPanel.add(yesCyclic);
    cyclicPanel.add(noCyclic);
    cyclicPanel.add(unspecifiedCyclic);
    orderedCyclicPanel.setLayout(new GridLayout(1,2));
    orderedCyclicPanel.add(orderedPanel);
    orderedCyclicPanel.add(cyclicPanel); 
    orderedGroup.add(yesOrdered);
    orderedGroup.add(noOrdered);
    orderedGroup.add(unspecifiedOrdered);
    cyclicGroup.add(yesCyclic);
    cyclicGroup.add(noCyclic);
    cyclicGroup.add(unspecifiedCyclic);
    setLayout(new BorderLayout());
    add(applyPanel, BorderLayout.NORTH);
    add(scrollPanel, BorderLayout.CENTER);
    add(orderedCyclicPanel, BorderLayout.SOUTH);
    ckNcmpnts.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if(ckNcmpnts.isSelected())
        {
          upbField.setVisible(true);
          checkNcmpnts();
        }
        else
        {
          scroll.setVisible(false);
          upbField.setVisible(false);
          applyButton.setEnabled(false);
        }
        checkOK();
      }
    });
    upbField.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent e){}
      public void keyTyped(KeyEvent e){} 
      public void keyReleased(KeyEvent e)
      {
        checkNcmpnts();
      }
    });
    applyButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        scroll.setVisible(true);
        setUpperBound(Integer.parseInt(upbField.getText()));
      }
    });
    yesOrdered.addChangeListener(new ChangeListener ()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(!yesOrdered.isSelected() && yesCyclic.isSelected())
        {
          noCyclic.setSelected(true);
        }
      }
    });
    noOrdered.addChangeListener(new ChangeListener ()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(noOrdered.isSelected())
        {
          noCyclic.setSelected(true);
        }
      }
    });
    yesCyclic.addChangeListener(new ChangeListener ()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(yesCyclic.isSelected())
        {
          yesOrdered.setSelected(true);
        }
      }
    });
    unspecifiedCyclic.addChangeListener(new ChangeListener ()
    {
      public void stateChanged(ChangeEvent e)
      {
        if(unspecifiedCyclic.isSelected())
        {
          if(noOrdered.isSelected())
          {
            noCyclic.setSelected(true);
          }
        }
      }
    });
    setupTypeInfo(t);
  }

  private void setupTypeInfo(Type.Symbolic t)
  {
    if(t.ckValues)
    {
      ckNcmpnts.setEnabled(false);
      ckNcmpnts.setSelected(true);
      upbField.setVisible(true);
      upbField.setText("" + (t.ids.length));
      upbField.setEnabled(false);
      applyButton.setEnabled(false);
      scroll.setVisible(true);
      int count;
      setUpperBound(t.ids.length);
      for(count = 0; count < t.ids.length; count++)
      {
        if(t.ids[count] != null)
        {
          panels[count].defineAndFinaliseText(t.ids[count]);
        }
      }
    }
    else
    {
      ckNcmpnts.setEnabled(true);
      ckNcmpnts.setSelected(false);
      scroll.setVisible(false);
      upbField.setVisible(false);
      applyButton.setEnabled(false);
    }
    if(t.ckIsCyclic)
    {
      unspecifiedCyclic.setEnabled(false);
      yesCyclic.setEnabled(false);
      noCyclic.setEnabled(false);
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
      unspecifiedCyclic.setEnabled(true);
      yesCyclic.setEnabled(true);
      noCyclic.setEnabled(true);
    }
    if(t.ckIsOrdered)
    {
      unspecifiedOrdered.setEnabled(false);
      yesOrdered.setEnabled(false);
      noOrdered.setEnabled(false);
      if(t.isOrdered)
      {
        yesOrdered.setSelected(true);
      }
      else
      {
        noOrdered.setSelected(true);
        noCyclic.setSelected(true);
      }
    }
    else
    {
      unspecifiedOrdered.setSelected(true);
      unspecifiedOrdered.setEnabled(true);
      yesOrdered.setEnabled(true);
      noOrdered.setEnabled(true);
    }
    scroll.setViewportView(namesPanel);
  }

  private void checkNcmpnts()
  {
    try
    {
      int n = Integer.parseInt(upbField.getText());
      if(n > 0)
      {
        applyButton.setEnabled(true);
      }
      else
      {
        applyButton.setEnabled(false);
      }
    }
    catch(Exception e)
    {
      applyButton.setEnabled(false);
    }
  }

  private void setUpperBound(int u)
  {
    namesPanel.removeAll();
    int count;
    NamePanel[] tmpPanels = new NamePanel[u];
    if(panels.length < u)
    {
      for(count = 0; count < panels.length; count++)
      {
        tmpPanels[count] = panels[count];
      }
      for(count = panels.length; count < u; count++)
      {
        tmpPanels[count] = new NamePanel("" + count);
      }
    }
    else
    {
      for(count = 0; count < u; count++)
      {
        tmpPanels[count] = panels[count];
      }
    }
    panels = tmpPanels;
    namesPanel.setLayout(new GridLayout(panels.length,1));
    for(count = 0; count < panels.length; count++)
    {
      namesPanel.add(panels[count]);
    }
    scroll.setViewportView(namesPanel);
  }

  private class NamePanel extends JPanel
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3079663747166081271L;
	private JLabel label;
    private JPanel txtPanel = new JPanel();
    private JTextField txt;
    private JCheckBox define;

    public NamePanel(String message)
    {
      label = new JLabel(message);
      txt = new JTextField(12);
      define = new JCheckBox("Defined", false);
      txtPanel.setVisible(false);
      setLayout(new GridLayout(1,3));
      add(label);
      add(define);
      txtPanel.add(txt);
      add(txtPanel);
      define.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          if(define.isSelected())
          {
            txtPanel.setVisible(true);
          }
          else
          {
            txtPanel.setVisible(false);
          }
          checkOK();
        }
      });
      txt.addKeyListener(new KeyListener()
      {
        public void keyPressed(KeyEvent e){}
        public void keyTyped(KeyEvent e){} 
        public void keyReleased(KeyEvent e)
        {
          checkOK();
        }
      });
    }

    public void defineAndFinaliseText(String text)
    {
      txt.setText(text);
      txtPanel.setVisible(true);
      txt.setEnabled(false);
      define.setSelected(true);
      define.setEnabled(false);
//      checkOK();
    }

    public boolean defined()
    {
      return define.isSelected();
    }

    public String name()
    {
      if(define.isSelected())
      {
        return txt.getText();
      }
      else
      {
        return null;
      }
    }
  }

  public void checkOK()
  {
    if(ckNcmpnts.isSelected())
    {
      if(scroll.isVisible())
      {
        okListener.okEvent(true);
        int count, count2;
        for(count = 0; count < panels.length; count++)
        {
          for(count2 = 0; count2 < panels.length; count2++)
          {
            if(panels[count].defined() && panels[count2].defined())
            {
              if((count != count2)&&(panels[count].name().equals(panels[count2].name())))
              {
                okListener.okEvent(false);
              }
            }
          }
        }
      }
      else
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
    if(ckNcmpnts.isSelected())
    {
      String[] ids = new String[panels.length];
      int count;
      for(count = 0; count < panels.length; count++)
      {
        ids[count] = panels[count].name();
      }
      return new Type.Symbolic(!unspecifiedCyclic.isSelected(), yesCyclic.isSelected(), !unspecifiedOrdered.isSelected(), yesOrdered.isSelected(), ids);
    }
    else
    {
      return new Type.Symbolic(!unspecifiedCyclic.isSelected(), yesCyclic.isSelected(), !unspecifiedOrdered.isSelected(), yesOrdered.isSelected());
    }
  }
}
