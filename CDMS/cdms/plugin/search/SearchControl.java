//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: SearchControl.java
// Author: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.search;

import java.awt.*;
import javax.swing.*;

/** A search control panel with an error plot, control buttons and summary view. */ 
public class SearchControl extends JPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7114367125527104636L;
protected Search search;
  protected JSplitPane centerPanel;
  protected Box southBox;

  public SearchControl(Search search, Component display)
  {
    super(new BorderLayout());
    this.search = search;

    centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    centerPanel.setDividerSize(10);
    centerPanel.setOneTouchExpandable(true);
    centerPanel.setLeftComponent(new SearchErrorPlot(search));
    if (display != null) centerPanel.setRightComponent(display);
    centerPanel.resetToPreferredSizes();
    add(centerPanel, BorderLayout.CENTER);

    southBox = new Box(BoxLayout.Y_AXIS);
    southBox.add(new SearchProgress(search,search.getSearchObject().getPercentage() >= 0)); 
    southBox.add(new SearchButtons(search)); 
    add(southBox, BorderLayout.SOUTH);

    setVisible(true);
  }


}

