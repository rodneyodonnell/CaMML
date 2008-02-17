//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.desktop;

/** Listener for the {@link Wizard}. */
public interface WizardEnableListener
{
  public void enableNext(boolean e);
  public void enableBack(boolean e);
}
