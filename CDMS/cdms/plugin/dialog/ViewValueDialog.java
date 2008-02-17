//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: ViewValueDialog.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import cdms.core.*;

public class ViewValueDialog extends ViewDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1251926838837235569L;

public ViewValueDialog(Type t)
  {
    super("View Value",new NewValuePanelType(t));
  }

  public ViewValueDialog(Type.Triv t)
  {
    super("View Value",new NewValuePanelTriv(t));
  }

  public ViewValueDialog(Type.Str t)
  {
    super("View Value",new NewValuePanelString(t));
  }

  public ViewValueDialog(Type.Scalar t)
  {
    super("View Value",new NewValuePanelScalar(t));
  }

  public ViewValueDialog(Type.Discrete t)
  {
    super("View Value",new NewValuePanelDiscrete(t));
  }

  public ViewValueDialog(Type.Continuous t)
  {
    super("View Value",new NewValuePanelContinuous(t));
  }

  public ViewValueDialog(Type.Vector t)
  {
    super("View Value",new NewValuePanelVector(t));
  }

  public ViewValueDialog(Type.Function t)
  {
    super("View Value",new NewValuePanelFunction(t));
  }

  public ViewValueDialog(Type.Symbolic t)
  {
    super("View Value",new NewValuePanelSymbolic(t));
  }

  public ViewValueDialog(Type.Structured t)
  {
    super("View Value",new NewValuePanelStructured(t));
  }

}
