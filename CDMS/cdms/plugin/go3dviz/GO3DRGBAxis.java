package cdms.plugin.go3dviz;

/**
 * Title:        GO3DRGBAxis<p>
 * Package:      go3dviz<p>
 * Description:  An extension of the GO3DAxis that represents a colour
 *               axis in R, G or B.  Not displayed on the screen at all
 *               with a span from [0.0, 1.0] and a default value of 0.7.
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import javax.media.j3d.*;

public class GO3DRGBAxis extends GO3DAxis
{
/** Creates an axis that defaults to [0.0, 1.0] -> [0.0, 1.0].  0.7 is
 *  set to be the default value.
 */
  public GO3DRGBAxis()
  {
    minAxisVal = 0.0f;
    maxAxisVal = 1.0f;

    minDataVal = 0.0f;
    maxDataVal = 1.0f;
    defaultVal = 0.7f;

    this.init(GO3DAxis.UNDISPLAYED);

    initAxisRoot();
  }

/** Creates an axis that defaults to [min, max] -> [0.0, 1.0].  0.7 is
 *  set to be the default value.
 */
  public GO3DRGBAxis(float min, float max)
  {
    minAxisVal = 0.3f;
    maxAxisVal = 1.0f;

    minDataVal = min;
    maxDataVal = max;
    defaultVal = 0.7f;

    this.init(GO3DAxis.UNDISPLAYED);

    initAxisRoot();
  }

/** Creates an axis that defaults to [min, max] -> [0.0, 1.0].  def is
 *  set to be the default value.
 */
  public GO3DRGBAxis(float min, float max, float def)
  {
    minAxisVal = 0.3f;
    maxAxisVal = 1.0f;

    minDataVal = min;
    maxDataVal = max;
    defaultVal = def;

    this.init(GO3DAxis.UNDISPLAYED);

    initAxisRoot();
  }

/**
 * This axis is not displayed and therefore returns a null subgraph.
 */
  void initAxisRoot()
  {
    axisRoot = (BranchGroup) null;
  }
}




