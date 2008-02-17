package cdms.plugin.go3dviz;

/**
 * Title:        GO3DObjectMaker<p>
 * Package:      go3dviz<p>
 * Description:  The GO3DObjMaker is an abstract class that creates<p>
 *               graphical objects that a GO3DVizPanel can use.  The<p>
 *               derived classes are not drawable in themselves, but<p>
 *               contain methods to create a Java3D sub-tree and<p>
 *               attach it to the GO3DVizPanel Scene Graph.<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import javax.media.j3d.*;
import cdms.core.Value;

public abstract class GO3DObjMaker
{
  BranchGroup ObjRoot;
  Value MyValues;
  short SamplingPercentage;

  GO3DObjMaker() {}

  void setSample(short s)
  {
    if (s > 0 && s <= 100)
    { SamplingPercentage = s; }
  }

  short getSample()
  {
    return SamplingPercentage;
  }

  void setValues(Value v)
  {
    MyValues = v;
  }

  Value getValues()
  {
    return MyValues;
  }

  BranchGroup getSceneGraph()
  {
    return ObjRoot;
  }

  abstract void createSceneGraph();

  abstract void recreateSceneGraph();
}