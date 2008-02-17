package cdms.plugin.nnet;

import cdms.core.*;
import cdms.plugin.search.*;

/** The Neural Network SearchObject wrapper for NNetInt. */
public class NNetSearchObject implements Search.SearchObject
{
  int counter = 0;

  public NNetSearchObject(Value.Vector inputData, Value.Vector outputData, NNetInt nnetInt)
  {
    ;
  }

  public void reset()
  {
    counter = 0;
  }

  public double doEpoch()
  {
    counter++;
    for (int i = 0; i < counter * 1000; i++) ;
    return counter;
  }

  public boolean isFinished()
  {
    return counter == 100;
  }

  public double getPercentage()
  {
    return counter / 100.0;
  }
}

