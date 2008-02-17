package cdms.plugin.go3dviz;

/**
 * Title:        GO3DCubeMaker<p>
 * Package:      go3dviz<p>
 * Description:  The G3DCubeMaker is a simple class that creates a<p>
 *               cube to represent data.  It creates a cube that has<p>
 *               side lengths representitive of the ranges of the 3<p>
 *               values.<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import cdms.core.Value;

public class GO3DCubeMaker extends GO3DObjMaker
{
  GO3DCubeMaker(Value DataValues)
  {
    SamplingPercentage = 25;
    // NOTE: These lines present in original code, but don't compile.
    // MyValue = DataValues;
    // ObjRoot = new BranchGroup;
  }

  void createSceneGraph()
  {
    
  }

  void recreateSceneGraph()
  {
  }
}