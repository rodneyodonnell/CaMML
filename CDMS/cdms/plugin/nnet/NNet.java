//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.nnet;

import cdms.core.*;

/** Neural Network module. */
public class NNet extends Module
{
  public static java.net.URL helpURL = Module.createStandardURL(NNet.class);

  public String getModuleName()
  {
    return "NNet";
  }

  public java.net.URL getHelp()
  {
    return helpURL;
  }

  public void install(Value params) throws Exception
  {
    add("wizard", NNetFn.wizard, "Neural Network analysis wizard.");
  }
}
