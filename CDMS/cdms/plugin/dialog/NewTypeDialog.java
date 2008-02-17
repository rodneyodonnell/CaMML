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

import cdms.core.*;

public class NewTypeDialog extends NewDialog
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5790618442361262124L;

public NewTypeDialog(Type root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelType(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Triv root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelTriv(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Str root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelString(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Scalar root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelScalar(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Discrete root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelDiscrete(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Continuous root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelContinuous(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Symbolic root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelSymbolic(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Vector root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelVector(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Function root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelFunction(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Structured root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelStructured(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Obj root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelTriv(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }

  public NewTypeDialog(Type.Model root, String title, Boolean forceAdd)
  {
    super(title,new NewTypePanelModel(root),Environment.env);
    genericSetup(forceAdd.booleanValue());
  }
}
