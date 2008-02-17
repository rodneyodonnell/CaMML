package cdms.plugin.go3dviz;

/**
 * Title:        GO3DSubObj<p>
 * Package:      go3dviz<p>
 * Description:  
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import javax.media.j3d.*;

public abstract class GO3DSubObj
{
  /* The branch containing the subgraph that will draw this graphic sub-object */
  BranchGroup ObjRoot;

  /* Arrays containing all data points and all the sampled data point respectively */
  Float AllData[][];
  Float MyValues[][];

  /* The percentage at which to sample the data */
  float SamplingPercentage;

  /* The number of columns, total number of rows and number of rows sampled */
  int Cols;
  int AllRows;
  int Rows;

  /* A non-interesting never-called constructor */
  GO3DSubObj() {}

  /* An internal function that initialises/resets the subgraph */
  void initRoot()
  {
    ObjRoot = new BranchGroup();
    ObjRoot.setCapability(BranchGroup.ALLOW_DETACH);
  }

  /* Two simple functions that ensure sensible sample rates */
  void setSample(float S)
  {
    if (S >= 0.0f && S <= 100.0f)
    { SamplingPercentage = S; }
  }

  void setSample(Float F)
  {
    float S = F.floatValue();

    if (S >= 0.0f && S <= 100.0f)
    { SamplingPercentage = S; }
  }

  /* Returns the sample rate */
  float getSample()
  {
    return SamplingPercentage;
  }

  /* Choose sample data points from the total data set */
  void sampleValues(Float Data[][], int cols, int rows, GO3DAxis Axes[], int Selections[])
  {
    int maxrows = (int) (rows / SamplingPercentage);
    if (maxrows > this.getMaxDataRows()) { this.setSample((int) (rows / this.getMaxDataRows())); }

    float maxes[] = new float[cols];
    float mins[] = new float[cols];

    int Rate = (int) (100 / SamplingPercentage);
    //    System.out.println("Rate = " + Rate);

    MyValues = new Float[rows][cols];

    int spork = (int) (Math.random() * Rate);
    int j = 0;
    for (int i = 0; i < rows && j < this.getMaxDataRows(); i += ((spork > 0) ? spork : 1), j++)
    {
      for (int k = 0; k < cols; k++)
      {
	if (!Data[i][k].isNaN())
	{
	  if (i == 0)
          {
	    maxes[k] = Data[i][k].floatValue();
	    mins[k] = Data[i][k].floatValue();
          }
	  else
	  {
	    if (Data[i][k].floatValue() > maxes[k]) { maxes[k] = Data[i][k].floatValue(); }
	    if (Data[i][k].floatValue() < mins[k]) { mins[k] = Data[i][k].floatValue(); }
	  }
	}
	else
	  { /*System.out.println("Found a Nan!"); */}
	MyValues[j][k] = new Float(Data[i][k].floatValue());
      }
      spork = (int) (Math.random() * Rate);
    }
    for (int L = 0; L < cols; L++)
      for (int M = 0; M < 6; M++)
	if (Selections[M] == L)
	{
	  Axes[M].setDataMin(mins[L]);
	  Axes[M].setDataMax(maxes[L]);
	}
    AllRows = rows;
    Rows = j;
    Cols = cols;
    //    System.out.println("Rows = " + Rows);
  }

  /* Return the array of sampled data points */
  Float[][] getValues()
  {
    return MyValues;
  }

  /* Returns the branch containing the subgraph */
  BranchGroup getBranch()
  {
    return ObjRoot;
  }

  /* The function that creates the subgraph that draws the object */
  abstract void createBranch(int Selections[], GO3DAxis Axes[]);

  /* This function recreates the branch with different(?) data */
  void recreateBranch(int Selections[], GO3DAxis Axes[])
  {
    initRoot();
    this.createBranch(Selections, Axes);
  }

  /* Resample the values with the current sample rate */
  void resample(GO3DAxis Axes[], int Selections[])
  {
    this.sampleValues(AllData, Cols, Rows, Axes, Selections);
  }

  /* Function that returns the maximum number of data points that *
   * the particular graphic sub-object can handle.                */
  abstract int getMaxDataRows();

  /* Series of functions that return the axes values of a given   *
   * data values.                                                 */

  float getXVal(int row, GO3DAxis Axes[], int Selections[])
  {
    if (Selections[0] == -1)
      { return Axes[0].getDefault(); }
    return Axes[0].Translate(MyValues[row][Selections[0]]);
  }

  float getYVal(int row, GO3DAxis Axes[], int Selections[])
  {
    if (Selections[1] == -1)
      { return Axes[1].getDefault(); }
    return Axes[1].Translate(MyValues[row][Selections[1]]);
  }

  float getZVal(int row, GO3DAxis Axes[], int Selections[])
  {
    if (Selections[2] == -1)
      { return Axes[2].getDefault(); }
    return Axes[2].Translate(MyValues[row][Selections[2]]);
  }

  float getRVal(int row, GO3DAxis Axes[], int Selections[])
  {
    if (Selections[3] == -1)
      { return Axes[3].getDefault(); }
    return Axes[3].Translate(MyValues[row][Selections[3]]);
  }

  float getGVal(int row, GO3DAxis Axes[], int Selections[])
  {
    if (Selections[4] == -1)
      { return Axes[4].getDefault(); }
    return Axes[4].Translate(MyValues[row][Selections[4]]);
  }

  float getBVal(int row, GO3DAxis Axes[], int Selections[])
  {
    if (Selections[5] == -1)
      { return Axes[5].getDefault(); }
    return Axes[5].Translate(MyValues[row][Selections[5]]);
  }

}





