//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: SearchButtons.java
// Author: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.search;

import java.awt.*;
import javax.swing.*;

/** A panel containing search control buttons: start, stop, reset, step. */
public class SearchButtons extends JPanel implements Search.SearchListener
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5490704765096228393L;
protected Search search;
  protected JButton startButton = new JButton("Start");
  protected JButton stopButton = new JButton("Stop");
  protected JButton resetButton = new JButton("Reset");
  protected JButton stepButton = new JButton("Step");

  public SearchButtons(Search search)
  {
    super(new FlowLayout(FlowLayout.CENTER));

    this.search = search;
    search.addSearchListener(this);

    startButton.setMnemonic('a');
    stopButton.setMnemonic('o');
    resetButton.setMnemonic('r');
    stepButton.setMnemonic('S');

    add(startButton);
    add(stopButton);
    add(resetButton);
    add(stepButton);

    startButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        SearchButtons.this.search.start();
      }
    });

    stopButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        SearchButtons.this.search.stop();
        stopButton.setEnabled(false);
      }
    });

    resetButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        SearchButtons.this.search.reset();
      }
    });

    stepButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent e)
      {
        SearchButtons.this.search.step();
      }
    });

  }
 
  public void reset(final boolean fin)
  {
    Runnable doReset = new Runnable()
    { 
      public void run()
      {
        if (fin)
        {
          startButton.setEnabled(false);
          stopButton.setEnabled(false);
          stepButton.setEnabled(false);
          resetButton.setEnabled(true);
        }
        else
        {
          startButton.setEnabled(true);
          stepButton.setEnabled(true);
          stopButton.setEnabled(false);
          resetButton.setEnabled(true);
        }
      }
    };
    SwingUtilities.invokeLater(doReset);
  }

  public void reset(Search sender)
  {
    reset(search.getSearchObject().isFinished());
  }

  public void beforeEpoch(final boolean stepping)
  {
    Runnable doBefore = new Runnable()
    { 
      public void run()
      {
        startButton.setEnabled(false); // After an epoch has started, we only want the stop button to be available.
        stepButton.setEnabled(false);
        resetButton.setEnabled(false);
        if(stepping)        // ... unless the epoch is just a single step, after which the search will stop anyway.
        {                              //     in this case we want no buttons available.
          stopButton.setEnabled(false);
        }
        else
        {
          stopButton.setEnabled(true);
        }
      }
    };
    SwingUtilities.invokeLater(doBefore);
  }

  public void beforeEpoch(Search sender)
  {
    beforeEpoch(search.isStepping());
  }

  public void afterEpoch(Search sender)
  {
    if (search.getSearchObject().isFinished())   // search has finished...
    {
      Runnable doAfter = new Runnable()
      {  
        public void run()
        {
          startButton.setEnabled(false); // After an epoch has started, we only want the stop button to be available.
          startButton.setEnabled(false);
          stopButton.setEnabled(false);
          stepButton.setEnabled(false);
          resetButton.setEnabled(true);
        }
      };
      SwingUtilities.invokeLater(doAfter);
    }
    else
    {
      if (search.st.stop || search.isStepping()) // if search is about to stop...
      {
        Runnable doAfter = new Runnable()
        {  
          public void run()
          {
            startButton.setEnabled(true);
            stepButton.setEnabled(true);
            stopButton.setEnabled(false);
            resetButton.setEnabled(true);
          }
        };
        SwingUtilities.invokeLater(doAfter);
      }
      else                                       // search is just running...
      {
        Runnable doAfter = new Runnable()
        {  
          public void run()
          {
            startButton.setEnabled(false);
            stepButton.setEnabled(false);
            stopButton.setEnabled(true);
            resetButton.setEnabled(false);
          }
        };
        SwingUtilities.invokeLater(doAfter);
      }
    }
  }

  public void onCompletion(Search sender){}
}

