/**
 * Title:        GO3DVisualizer<p>
 * Package:      go3dviz<p>
 * Description:  The wrapper class for the GO3DVizPanel.<p>
 *               Written as part of the Core Data Mining System.<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 */

package cdms.plugin.go3dviz;

import cdms.core.Type;
import cdms.core.Value;

public class GO3DVisualizer
{

/** The internal component that contains the working Visualizer.
 *  This is the member that is to be returned and displayed by the
 *  parent program.  It is <I>not</I> a window, and therefore needs
 *  to be placed in one.
 */
  GO3DVizPanel MyVisualizer;

/** This constructor is the default constructor.  It initialises the
 *  lists without having any data.
 */
  public GO3DVisualizer()
  {
    MyVisualizer = new GO3DVizPanel();
  }

/** This constructor takes the data objects but assumes that the
 *  Visualizer can guess the best types for the axes.  The only
 *  drawback to this approach is that any labels that belong to
 *  a particular type will be lost.
 */
  public GO3DVisualizer(GO3DSubObj[] ObjList)
  {
  	// NOTE: This line from original code does not compile.
    // MyVisualizer = new GO3DVizPanel(ObjList);
  	
  	// Replacement line
  	MyVisualizer = new GO3DVizPanel();
  }

/** This constructor takes a vector of GO3DSubObjs that are created
 *  outside the class and a Type.Structured that contains the names
 *  and type of the preferred axes.
 */
  public GO3DVisualizer(GO3DSubObj[] ObjList, Type.Structured AxisTypes)
  {
  	// NOTE: This line from original code does not compile
    // MyVisualizer = new GO3DVizPanel(ObjList, AxisTypes);
  	
  	// Replaced with
  	MyVisualizer = new GO3DVizPanel();
  	
  }

/** Returns the working GO3DVizPanel.  Used by the parent program
 *  to display the module.  See notes on the <I>MyComponent</I>
 *  member.
 */
  public GO3DVizPanel getPanel()
  {
    return MyVisualizer;
  }

/** Sets the GO3DVizPanel component.  Should not need to be used as
 *  a component is created automatically.
 */
  public void setPanel(GO3DVizPanel V)
  {
    this.MyVisualizer = V;
  }

/** Returns a Value of Type.Vector that will contain the data fields
 *  the Visualizer was given to display.  This is -all- the data
 *  in its original form, not just as it is displayed or selected.
 *  c.f. <I>getApply*</I> and <I>getSubObjs</I>.
 */
  public Value getValue()
  {
  	//  NOTE: This line in original code does not compile
    // return this.getPanel().getValue();
  	
  	// replaced with
  	return null;
  }

/** Returns the vector of GO3DSubObjs given to the Visualizer.  This
 *  should be the identical vector to that passed to the constructor.
 *  c.f. <I>getApply*</I> and <I>getValue</I>.
 */
  public GO3DSubObj[] getSubObjs()
  {
  	//  NOTE: This line in original code does not compile
    // return this.getComponent().getSubObjs();

  	// replaced with
  	return null;
  }

/** Returns the currently selected data as a Value of Type.Vector which
 *  will contain the data fields in their original form.
 *  c.f. <I>getValue</I>, <I>getSubObjs</I> and <I>getApplySubObjs</I>.
 */
  public Value getApplyValue()
  {
  	//  NOTE: This line in original code does not compile
    // return this.getPanel().getApplyValue();

  	// replaced with
  	return null;
  }

/** Returns the currently selected data as a vector of GO3DSubObjs.
 *  c.f. <I>getValue</I>, <I>getSubObjs</I> and <I>getApplyValue</I>.
 */
  public GO3DSubObj[] getApplySubObjs()
  {
  	//  NOTE: This line in original code does not compile
    // return this.getComponent().getApplySubObjs();

  	// replaced with
  	return null;
  }

}












