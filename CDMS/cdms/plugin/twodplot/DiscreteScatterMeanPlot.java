package cdms.plugin.twodplot;

import cdms.core.*;
import cdms.plugin.desktop.*;

/** ("xlabel","ylabel",[([y_val], string_x_label)])*/
public class DiscreteScatterMeanPlot extends Value.Function
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 134223718741065012L;

public DiscreteScatterMeanPlot()
  {
    super(new Type.Function(new Type.Structured(new Type[]{Type.STRING, Type.STRING, new Type.Vector(new Type.Structured(new Type[]{new Type.Vector(Type.SCALAR), Type.STRING}))}), Type.TRIV));
  }

  public Value apply(Value v)
  {
    Value.Structured vs = (Value.Structured)v;
    String xLabel = ((Value.Str)vs.cmpnt(0)).getString();
    String yLabel = ((Value.Str)vs.cmpnt(1)).getString();
    Value.Vector bigVector = (Value.Vector)vs.cmpnt(2);
    Value.Vector eltVector;
    Value.Structured s;
    String[] xs = new String[bigVector.length()];
    Plot.NativeLinePlot nlp;
    double[][] yv = new double[bigVector.length()][];
    float ymin= Float.MAX_VALUE, ymax = -Float.MAX_VALUE;
    int eltCount, eltCount2;
    float[] mean = new float[bigVector.length()];
    for(eltCount = 0; eltCount < bigVector.length(); eltCount++)
    {
      s = (Value.Structured)bigVector.elt(eltCount);      
      eltVector = (Value.Vector)s.cmpnt(0);
      yv[eltCount] = new double[eltVector.length()];
      xs[eltCount] = ((Value.Str)s.cmpnt(1)).getString();
      mean[eltCount] = 0;
      for(eltCount2 = 0; eltCount2 < eltVector.length(); eltCount2++)
      {
        yv[eltCount][eltCount2] = ((Value.Scalar)eltVector.elt(eltCount2)).getContinuous();
        mean[eltCount] += yv[eltCount][eltCount2];
        if(yv[eltCount][eltCount2] < ymin) ymin = (float)(yv[eltCount][eltCount2]);
        if(yv[eltCount][eltCount2] > ymax) ymax = (float)(yv[eltCount][eltCount2]);      }
      mean[eltCount] /= eltVector.length();
    }

    float y_border = (float)(0.05 * (ymax - ymin));

    Plot.PlotSpace plotSpace = new Plot.PlotSpace((float)(-0.5), (float)(bigVector.length() - 0.5), ymin - y_border, ymax + y_border);

    for(eltCount = 0; eltCount < bigVector.length(); eltCount++)
    {
      nlp = new Plot.NativeLinePlot();
      nlp.addData(null,(float)(eltCount - 0.5), mean[eltCount]);
      nlp.addData(null,(float)(eltCount + 0.5), mean[eltCount]);
      plotSpace.addDrawShape(nlp);

      nlp = new Plot.NativeLinePlot();
      nlp.setConnected(false);
      nlp.setMarker(new Plot.CrossMarker(5));
      for(eltCount2 = 0; eltCount2 < yv[eltCount].length; eltCount2++)
      {
        nlp.addData(null,(float)(eltCount), (float)yv[eltCount][eltCount2]);
      }
      plotSpace.addDrawShape(nlp);
    }

    plotSpace.addFillShape(new Plot.YAxis(yLabel,2));
    plotSpace.addFillShape(new CustomAxis.DiscreteXAxis(xLabel, xs));

    DesktopFrame.makeWindow("discrete scatter plot with means",plotSpace);
    return Value.TRIV;
  }
}
