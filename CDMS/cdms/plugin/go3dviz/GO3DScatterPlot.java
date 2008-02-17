package cdms.plugin.go3dviz;

/**
 * Title:        GO3DScatterPlot<p>
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
import com.sun.j3d.utils.geometry.Box;


public class GO3DScatterPlot extends GO3DSubObj
{
  private static final int MAX_DATA_ROWS = 200;

  public GO3DScatterPlot(Float Data[][], int cols, int rows, GO3DAxis Axes[], int Selections[])
  {
    this.setSample(1.0f);
    AllData = Data;
    MyValues = Data;
    Cols = cols;
    AllRows = rows;
    this.initRoot();
  }

  public GO3DScatterPlot(Float Data[][], int cols, int rows, GO3DAxis Axes[], int Selections[], float SP)
  {
    this.setSample(SP);
    AllData = Data;
    MyValues = Data;
    Cols = cols;
    AllRows = rows;
    this.initRoot();
  }

  void createBranch(int Selections[], GO3DAxis Axes[])
  {
    this.sampleValues(AllData, Cols, AllRows, Axes, Selections);
    Box Boxes[] = new Box[Rows];
    Appearance A[] = new Appearance[Rows];
    Color3f Black = new Color3f(0.0f, 0.0f, 0.0f);
    Color3f White = new Color3f(1.0f, 1.0f, 1.0f);

    for (int i = 0; i < Rows; i++)
    {
      Point3f P = new Point3f(this.getXVal(i, Axes, Selections),
			      this.getYVal(i, Axes, Selections),
			      this.getZVal(i, Axes, Selections));
      Color3f C = new Color3f(this.getRVal(i, Axes, Selections),
			      this.getGVal(i, Axes, Selections),
			      this.getBVal(i, Axes, Selections));

      //      System.out.println("Cube at "+P+" with colour "+C);

      A[i] = new Appearance();
      A[i].setMaterial(new Material(C, Black, C, White, 80.0f));

      Transform3D T = new Transform3D();
      T.setTranslation(new Vector3f(P));
      TransformGroup TG = new TransformGroup(T);

      Boxes[i] = new Box(0.05f, 0.05f, 0.05f, A[i]);

      TG.addChild(Boxes[i]);
      ObjRoot.addChild(TG);
    }    
  }

  public int getMaxDataRows()
  {
    return MAX_DATA_ROWS;
  }

}




