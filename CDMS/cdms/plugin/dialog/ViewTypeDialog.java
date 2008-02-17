//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: ViewTypeDialog.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import cdms.core.*;

public class ViewTypeDialog extends ViewDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7179086188138762073L;

private static String getDescription(Type t)
  {
    Environment.RegEntry r = Environment.env.getEntryByObject(t);
    if(r != null) return r.description;
    return "";
  }

  public ViewTypeDialog(Type t)
  {
    super("View Type",new NewTypePanelType(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Triv t)
  {
    super("View Type",new NewTypePanelTriv(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Str t)
  {
    super("View Type",new NewTypePanelString(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Scalar t)
  {
    super("View Type",new NewTypePanelScalar(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Discrete t)
  {
    super("View Type",new NewTypePanelDiscrete(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Continuous t)
  {
    super("View Type",new NewTypePanelContinuous(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Vector t)
  {
    super("View Type",new NewTypePanelVector(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Function t)
  {
    super("View Type",new NewTypePanelFunction(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Symbolic t)
  {
    super("View Type",new NewTypePanelSymbolic(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Structured t)
  {
    super("View Type",new NewTypePanelStructured(t), getDescription(t));
  }

  /** Open a NewTypeTriv panel - cannot currently make new Type.Obj's. */
  public ViewTypeDialog(Type.Obj t)
  {
    super("View Type",new NewTypePanelTriv(t), getDescription(t));
  }

  public ViewTypeDialog(Type.Model t)
  {
    super("View Type",new NewTypePanelModel(t), getDescription(t));
  }

}
