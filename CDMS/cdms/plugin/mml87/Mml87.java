//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.mml87;

import cdms.core.*;

/** Minimum Message Length using the Fisher information (MML87) module. */
public class Mml87 extends Module
{
  public static java.net.URL helpURL = Module.createStandardURL(Mml87.class);

  public String getModuleName()
  {
    return "MML87";
  }

  public java.net.URL getHelp()
  {
    return helpURL;
  }

  public void install(Value params) throws Exception
  {
    add("multinomialEstimator", UniformMultinomialEstimator.uniformMultinomialEstimator, 
        "Function to estimate parameters for a multinomial distribution (MML87 uniform prior)");
    add("multinomialCoster", UniformMultinomialCoster.uniformMultinomialCoster,
        "Function to give message length of (Multinomial Model, Params, Data)");
    add("multinomialCoster_1", UniformMultinomialCoster_1.uniformMultinomialCoster_1,
        "Function to give 1st part of the message length of (Multinomial Model, Params, Data)");
    add("normalEstimator", UniformNormal.normalEstimator, 
        "Function to estimate the parameters for the normal distribution with uniform prior on mu and log(sigma).");
    add("normalCoster", UniformNormal.normalCoster, 
        "Function to compute the message length for the normal distribution with uniform prior on mu and log(sigma).");
    add("jcNormalCoster", UniformNormal.arbitraryNormalCoster, 
        "Function to compute the message length for the normal distribution with uniform prior on mu and log(sigma), with mu and sigma as parameters.");
    add("jcNormalCoster_1", UniformNormal.arbitraryNormalCosterHypothesisOnly, 
        "Function to compute the 1st part of the message length (hypothesis only) for the normal distribution with uniform prior on mu and log(sigma), with mu and sigma as parameters.");
    add("weightedNormalEstimator", UniformNormal.weightedNormalEstimator,
        "Function to estimate the parameters for the weighted normal distribution with uniform prior on mu and log(sigma).");

  }
}
