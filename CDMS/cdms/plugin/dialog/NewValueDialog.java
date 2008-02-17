//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewValueDialog.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import cdms.core.*;

public class NewValueDialog extends NewDialog
{

  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5946129392887657748L;

public NewValueDialog(Type root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelType(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Triv root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelTriv(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Str root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelString(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Scalar root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelScalar(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Discrete root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelDiscrete(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Continuous root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelContinuous(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Symbolic root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelSymbolic(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Vector root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelVector(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Function root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelFunction(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewValueDialog(Type.Structured root, String title, Boolean forceAdd)
  {
    super(title,new NewValuePanelStructured(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

}
