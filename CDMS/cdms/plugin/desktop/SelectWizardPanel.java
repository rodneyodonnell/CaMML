//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.desktop;

import cdms.core.*;
import cdms.plugin.desktop.Wizard.*;
import cdms.plugin.dialog.*;

/** A wizard panel that allows selection of a value or type from the environment. */
public abstract class SelectWizardPanel extends WizardPanel implements SelectPanel.SelectionChangeListener
{
  private SelectPanel sp;

  public SelectWizardPanel(String message, Type root, Type withMember, boolean showValues)
  {
    sp = new SelectPanel(message, root, withMember, showValues);
    add(sp);
    setNextEnabled(false);
    sp.addSelectionChangeListener(this);
  }

  public void selectionChanged(boolean somethingSelected)
  {
    setNextEnabled(somethingSelected);
  }

  public Object getResult()
  {
    return sp.getResult();
  }
}
