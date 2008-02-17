package cdms.plugin.twodplot;

import cdms.core.*;
import cdms.plugin.desktop.*;

/** ("xlabel","ylabel",[(y_val, y_error, discrete_x)])*/
public class DiscreteErrorBarPlot extends Value.Function
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2602883866658277304L;

public DiscreteErrorBarPlot()
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
    double[] xv = new double[bigVector.length()];
    double[] yv = new double[bigVector.length()];
    double[] ye = new double[bigVector.length()];
    float xmin = Float.MAX_VALUE, xmax = -Float.MAX_VALUE, ymin= Float.MAX_VALUE, ymax = -Float.MAX_VALUE;
    int eltCount;
    for(eltCount = 0; eltCount < bigVector.length(); eltCount++)
    {
      s = (Value.Structured)bigVector.elt(eltCount);
      yv[eltCount] = ((Value.Scalar)s.cmpnt(0)).getContinuous();
      ye[eltCount] = ((Value.Scalar)s.cmpnt(1)).getContinuous();
      xv[eltCount] = ((Value.Scalar)s.cmpnt(2)).getContinuous();

      if(xv[eltCount] < xmin) xmin = (float)(xv[eltCount]);
      if(xv[eltCount] > xmax) xmax = (float)(xv[eltCount]);

      if(yv[eltCount] - ye[eltCount] < ymin) ymin = (float)(yv[eltCount] - ye[eltCount]);
      if(yv[eltCount] + ye[eltCount] > ymax) ymax = (float)(yv[eltCount] + ye[eltCount]);
    }

    float y_border = (float)(0.05 * (ymax - ymin));

    Plot.PlotSpace plotSpace = new Plot.PlotSpace((float)(xmin - 0.5), (float)(xmax + 0.5), ymin - y_border, ymax + y_border);

    for(eltCount = 0; eltCount < xv.length; eltCount++)
    {
      nlp = new Plot.NativeLinePlot();
      nlp.addData(null,(float)(xv[eltCount] - 0.5), (float)(yv[eltCount] - ye[eltCount]));
      nlp.addData(null,(float)(xv[eltCount] - 0.5), (float)(yv[eltCount] + ye[eltCount]));
      nlp.addData(null,(float)(xv[eltCount] + 0.5), (float)(yv[eltCount] + ye[eltCount]));
      nlp.addData(null,(float)(xv[eltCount] + 0.5), (float)(yv[eltCount] - ye[eltCount]));
      nlp.addData(null,(float)(xv[eltCount] - 0.5), (float)(yv[eltCount] - ye[eltCount]));
      plotSpace.addDrawShape(nlp);
      nlp = new Plot.NativeLinePlot();
      nlp.addData(null,(float)(xv[eltCount] - 0.5), (float)yv[eltCount]);
      nlp.addData(null,(float)(xv[eltCount] + 0.5), (float)yv[eltCount]);
      plotSpace.addDrawShape(nlp);
    }

    plotSpace.addFillShape(new Plot.YAxis(yLabel,2));
    plotSpace.addFillShape(new CustomAxis.DiscreteXAxis(xLabel));

    DesktopFrame.makeWindow("discrete error bar plot",plotSpace);
    return Value.TRIV;
  }
}
