//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: NewTypePanelString.java
// Authors: {joshc}@cs.monash.edu.au

package cdms.plugin.dialog;

import cdms.core.*;

public class NewTypePanelString extends NewPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1L;

public NewTypePanelString(Type.Str t){}

  public boolean getInitialOKState()
  {
    return true;
  }

  public Object getResult()
  {
    return Type.STRING;
  }
}
