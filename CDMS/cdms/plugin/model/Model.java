//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Model.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.model;

import cdms.core.*;

/** Model module. */
public class Model extends Module
{
  public static java.net.URL helpURL = Module.createStandardURL(Model.class);

  public String getModuleName() { return "Model"; }
  public java.net.URL getHelp() { return helpURL; }

  public void install(Value params) throws Exception
  {
    Environment.env.add("uniform","Model",Uniform.uniform,
                        "Uniform(y|lwb,upb)");

    Environment.env.add("normal","Model",Normal.normal,
                        "Normal(y|mu,sigma)");

    Environment.env.add("weighted_normal","Model",WeightedNormal.weightedNormal,
                        "Weighted Normal (y|mu,sigma)");

    Environment.env.add("multinomialCreator", "Model", new Multinomial.MultinomialCreator(),
                        "Multinomial Creator  (lwb,upb) -> multinomial");
  }

}
