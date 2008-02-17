//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.ml;

import cdms.core.*;

/** Maximum likelihood module. */
public class Ml extends Module
{
  public static java.net.URL helpURL = Module.createStandardURL(Ml.class);

  public String getModuleName()
  {
    return "ML";
  }

  public java.net.URL getHelp()
  {
    return helpURL;
  }

  public void install(Value params) throws Exception
  {
    add("normalEstimator", Normal.normalEstimator, 
        "Function to compute the maximum likelihood model for the normal distribution.");
  }
}
