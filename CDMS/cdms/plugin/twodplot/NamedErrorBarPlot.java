package cdms.plugin.twodplot;

import cdms.core.*;
import cdms.plugin.desktop.*;

/** ("xlabel","ylabel",[(y_val, y_error, string_x)])*/
public class NamedErrorBarPlot extends Value.Function
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4392210102924528524L;

public NamedErrorBarPlot()
  {
    super(new Type.Function(new Type.Vector(new Type.Structured(new Type[]{Type.SCALAR, Type.SCALAR, Type.DISCRETE})), Type.TRIV));
  }

  public Value apply(Value v)
  {
    Value.Structured vs = (Value.Structured)v;
    String xLabel = ((Value.Str)vs.cmpnt(0)).getString();
    String yLabel = ((Value.Str)vs.cmpnt(1)).getString();
    Value.Vector bigVector = (Value.Vector)vs.cmpnt(2);
    Value.Structured s;
    Plot.NativeLinePlot nlp;
    double[] yv = new double[bigVector.length()];
    double[] ye = new double[bigVector.length()];
    String[] xs = new String[bigVector.length()];
    float xmin = 0, xmax = bigVector.length() - 1, ymin= Float.MAX_VALUE, ymax = -Float.MAX_VALUE;
    int eltCount;
    for(eltCount = 0; eltCount < bigVector.length(); eltCount++)
    {
      s = (Value.Structured)bigVector.elt(eltCount);
      yv[eltCount] = ((Value.Scalar)s.cmpnt(0)).getContinuous();
      ye[eltCount] = ((Value.Scalar)s.cmpnt(1)).getContinuous();
      xs[eltCount] = ((Value.Str)s.cmpnt(2)).getString();
      if(yv[eltCount] - ye[eltCount] < ymin) ymin = (float)(yv[eltCount] - ye[eltCount]);
      if(yv[eltCount] + ye[eltCount] > ymax) ymax = (float)(yv[eltCount] + ye[eltCount]);
    }

    float y_border = (float)(0.05 * (ymax - ymin));

    Plot.PlotSpace plotSpace = new Plot.PlotSpace((float)(xmin - 0.5), (float)(xmax + 0.5), ymin - y_border, ymax + y_border);

    for(eltCount = 0; eltCount < xs.length; eltCount++)
    {
      nlp = new Plot.NativeLinePlot();
      nlp.addData(null,(float)(eltCount - 0.5), (float)(yv[eltCount] - ye[eltCount]));
      nlp.addData(null,(float)(eltCount - 0.5), (float)(yv[eltCount] + ye[eltCount]));
      nlp.addData(null,(float)(eltCount + 0.5), (float)(yv[eltCount] + ye[eltCount]));
      nlp.addData(null,(float)(eltCount + 0.5), (float)(yv[eltCount] - ye[eltCount]));
      nlp.addData(null,(float)(eltCount - 0.5), (float)(yv[eltCount] - ye[eltCount]));
      plotSpace.addDrawShape(nlp);
      nlp = new Plot.NativeLinePlot();
      nlp.addData(null,(float)(eltCount - 0.5), (float)yv[eltCount]);
      nlp.addData(null,(float)(eltCount + 0.5), (float)yv[eltCount]);
      plotSpace.addDrawShape(nlp);
    }

    plotSpace.addFillShape(new Plot.YAxis(yLabel,2));
    plotSpace.addFillShape(new CustomAxis.DiscreteXAxis(xLabel, xs));

    DesktopFrame.makeWindow("discrete error bar plot",plotSpace);
    return Value.TRIV;
  }
}
