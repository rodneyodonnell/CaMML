package cdms.plugin.go3dviz;

/**
 * Title:        GO3DAxis<p>
 * Package:      go3dviz<p>
 * Description:  The GO3DAxis class provides the ability to<p>
 *               translate points in data-space to their<p>
 *               corresponding points in Canvas3D space.<p>
 *               It should also store the nature of the current<p>
 *               axis type and the ability to change to a<p>
 *               different type.<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import java.lang.String;
import javax.media.j3d.*;

public abstract class GO3DAxis
{
/* A set of constants describing which visual dimension the axis is    *
 * displayed in.  Created here so that it appears in all child classes */

/**
 * Constant value meaning that this Axis is not a space axis.
 */
  final static public int UNDISPLAYED = 0;
/**
 * Constant value meaning that this Axis is displayed as an X-axis.
 */
  final static public int X_DIRECTION = 1;
/**
 * Constant value meaning that this Axis is displayed as a Y-axis.
 */
  final static public int Y_DIRECTION = 2;
/**
 * Constant value meaning that this Axis is displayed as a Z-axis.
 */
  final static public int Z_DIRECTION = 3;

/* The minimum, maximum and default data value along this axis.  The max   *
 * and min values are altered to be the max and min of all the -displayed- *
 * values so that the data is spread out nicely.                           */
  float minDataVal;
  float maxDataVal;
  float defaultVal;

/* The range, minimum and maximum axis value for this axis.  These are set *
 * by the extending class for the type of axis needed.                     */
  float maxAxisVal;
  float minAxisVal;
  float axisRange;

/* The direction in which the axis will be displayed.  (See the constants) */ 
  int Direction;

/* A Java3D class that contains the subgraph that will draw this axis.     */
  BranchGroup axisRoot;

/** 
 * A default constructor that doesn't do anything.
 */
  public GO3DAxis(){}

/**
 * Fixes the direction and range values of this axis, in case they are not
 * set correctly.
 */
  public final void init(int Dir)
  {
    if (Dir < UNDISPLAYED) { Dir = UNDISPLAYED; }
    if (Dir > Z_DIRECTION) { Dir = Z_DIRECTION; }
    Direction = Dir;
    axisRange = maxAxisVal - minAxisVal;
  }

/**
 * Displays the properties of this axis as a string.  It shows the range of
 * data values and how it maps to the range of axis values.
 */
  public String toString()
  {
    StringBuffer Str = new StringBuffer("Axis = ");
    Str.append("[" + minDataVal + ", " + maxDataVal + "]");
    Str.append(" -> ");
    if (Direction != UNDISPLAYED)
    { Str.append("[" + minAxisVal + ", " + maxAxisVal + "]"); }
    else
    { Str.append("[0.0, 1.0]"); }
    return new String(Str);
  }

/**
 * Function that subclasses override to create the axis subgraph.
 */
  abstract void initAxisRoot();

/**
 * Takes an integer value and turns it into the correspoonding float value
 * that lies in the range of the axis.
 */
  public float Translate(int Input)
  {
    float Answer;
    float Range = maxDataVal - minDataVal;
    if (Range == 0.0f) {Range = 1.0f;}
    float RelativePos = (float) (Input - minDataVal) / Range;
    Answer = RelativePos * axisRange + minAxisVal;
    if (Answer > maxAxisVal || Answer < minAxisVal)
    {
      System.out.println("HUH? "+this.toString()+ " but " + Input + " -> " + Answer); 
      System.out.println("\t\tRange = "+Range+"; RelativePos = "+RelativePos+";");
    }
    return Answer;
  }

/**
 * Takes a float value and turns it into the correspoonding float value
 * that lies in the range of the axis.
 */
  public float Translate(float Input)
  {
    float Answer;
    float Range = maxDataVal - minDataVal;
    if (Range == 0.0f) {Range = 1.0f;}
    float RelativePos = (Input - minDataVal) / Range;
    Answer = RelativePos * axisRange + minAxisVal;
    if (Answer > maxAxisVal || Answer < minAxisVal)
    {
      System.out.println("HUH? "+this.toString()+ " but " + Input + " -> " + Answer); 
      System.out.println("\t\tRange = "+Range+"; RelativePos = "+RelativePos+";");
    }
    return Answer;
  }

/**
 * Takes a Float value and turns it into the correspoonding float value
 * that lies in the range of the axis.  This one is used exclusively in
 * the base implementation.
 */
  public float Translate(Float Input)
  {
    if (Input.isNaN()) { return this.getDefault(); }
    float Answer;
    float Range = maxDataVal - minDataVal;
    if (Range == 0.0f) {Range = 1.0f;}
    float RelativePos = (Input.floatValue() - minDataVal) / Range;
    Answer = RelativePos * axisRange + minAxisVal;
    if (Answer > maxAxisVal || Answer < minAxisVal)
    {
      System.out.println("HUH? "+this.toString()+ " but " + Input.toString() + " -> " + Answer); 
      System.out.println("\t\tRange = "+Range+"; RelativePos = "+RelativePos+";");
    }
    return Answer;
  }

/**
 * Takes a double value and turns it into the correspoonding float value
 * that lies in the range of the axis.
 */
  public float Translate(double Input)
  {
    float Answer;
    float Range = maxDataVal - minDataVal;
    if (Range == 0.0f) {Range = 1.0f;}
    float RelativePos = (float) (Input - minDataVal) / Range;
    Answer = RelativePos * axisRange + minAxisVal;
    if (Answer > maxAxisVal || Answer < minAxisVal)
    {
      System.out.println("HUH? "+this.toString()+ " but " + Input + " -> " + Answer); 
      System.out.println("\t\tRange = "+Range+"; RelativePos = "+RelativePos+";");
    }
    return Answer;
  }

/**
 * Place holder for when an axis can translate characters into floats that
 * lie in the axis range.
 */
  public float Translate(char Input)
  {
    return 0.0f;
  }

/**
 * Place holder for when an axis can translate strings into floats that lie
 * in the axis range.
 */
  public float Translate(String Input)
  {
    return 0.0f;
  }

/**
 * Sets the minimum value for this axis.  Used when the data values of a
 * particular 3DSubObj change.
 */
  public void setDataMin(float min)
  {
    minDataVal = min;
  }

/**
 * Sets the maximum value for this axis.  Used when the data values of a
 * particular 3DSubObj change.
 */
  public void setDataMax(float max)
  {
    maxDataVal = max;
  }

/**
 * Returns the default value for this axis.
 */
  public float getDefault()
  {
    return defaultVal;
  }

/**
 * Returns the subgraph containing the 3D construction of the axis.
 */
  public BranchGroup getBranch()
  {
    return axisRoot;
  }
}




