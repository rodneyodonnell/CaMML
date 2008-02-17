package cdms.plugin.go3dviz;

/**
 * Title:        GO3DStarFieldPlot<p>
 * Package:      go3dviz<p>
 * Description:  The G3DScatterPlot is a simple class that creates a<p>
 *               scatterplot of points to represent data.  Each data<p>
 *               point is positioned in space according to 3 data values<p>
 *               and has an RGB colour dependence on 3 more.<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import javax.media.j3d.*;
import javax.vecmath.*;

public class GO3DStarFieldPlot extends GO3DSubObj
{
  private static final int MAX_DATA_ROWS = 25000;

  public GO3DStarFieldPlot(Float Data[][], int cols, int rows, GO3DAxis Axes[], int Selections[])
  {
    this.setSample(25.0f);
    AllData = Data;
    AllRows = rows;
    Cols = cols;
    this.initRoot();
  }

  public GO3DStarFieldPlot(Float Data[][], int cols, int rows, GO3DAxis Axes[], int Selections[], float SP)
  {
    this.setSample(SP);
    AllData = Data;
    AllRows = rows;
    Cols = cols;
    this.initRoot();
  }

  void createBranch(int Selections[], GO3DAxis Axes[])
  {
  	// This line from original code does not compile
    // this.sampleValues(Data, Cols, AllRows, MAX_DATA_ROWS, Axes, Selections);

    PointArray PA = new PointArray(Rows, GeometryArray.COORDINATES |
				         GeometryArray.COLOR_3);
    for (int i = 0; i < Rows; i++)
    {
      Point3f P = new Point3f(this.getXVal(i, Axes, Selections),
			      this.getYVal(i, Axes, Selections),
			      this.getZVal(i, Axes, Selections));
      Color3f C = new Color3f(this.getRVal(i, Axes, Selections),
			      this.getGVal(i, Axes, Selections),
			      this.getBVal(i, Axes, Selections));

      PA.setCoordinate(i, P);
      PA.setColor(i, C);
    }    

    Shape3D SPlot = new Shape3D(PA);
    ObjRoot.addChild(SPlot);
  }

  public int getMaxDataRows()
  {
    return MAX_DATA_ROWS;
  }

}
