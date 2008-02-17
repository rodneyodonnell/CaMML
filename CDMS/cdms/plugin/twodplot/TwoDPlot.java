//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: TwoDPlot.java
// Authors: leighf@csse.monash.edu.au

package cdms.plugin.twodplot;

import java.awt.*;
import java.io.*;
import java.awt.geom.*;

import cdms.core.*;
import cdms.plugin.desktop.DesktopFrame;

/** CDMS interface to {@link Plot}. */
public class TwoDPlot extends Module.StaticFunctionModule 
{

  public TwoDPlot() 
  {
    super("TwoDPlot",Module.createStandardURL(TwoDPlot.class),TwoDPlot.class);
  }


  /** <code>RendtAttrib :: (boolean,boolean,Obj(Composite),Obj(Paint),Obj(Stroke)) </code>A type 
      representing rendering attributes.
  */
  public static final Type.Structured RENDATTRIB = 
    new Type.Structured(new Type[] { Type.BOOLEAN, Type.BOOLEAN,new Type.Obj(Composite.class.getName()),
                          new Type.Obj(Paint.class.getName()),
                          new Type.Obj(Stroke.class.getName()) }, 
                        new String[] { "Draw", "Fill", "Composite", "Paint", "Stroke" } );

  /** The default composite value is AlphaComposite.SrcOver. */
//  public static final Value.Obj dComposite = new Value.Obj(AlphaComposite.SrcOver);

  /** The default paint value is Color.black. */
//  public static final Value.Obj dPaint = new Value.Obj(Color.black);

  /** The default stroke is one pixel wide. */
//  public static final Value.Obj dStroke = new Value.Obj(new BasicStroke(1));

 
  /** <code>(xl,xu,yl,yu,[(Obj(java.awt.Shape),RENDATTRIB)] -> Obj(Plot.PlotSpace)</code>Creates a 
      PlotSpace with bounds specified and containing the given shapes.
  */
  public static final Plotxy plotxy = new Plotxy();

  /** <code>(xl,xu,yl,yu,[(Obj(java.awt.Shape),RENDATTRIB)] -> Obj(Plot.PlotSpace)</code>Creates a 
      PlotSpace with bounds specified and containing the given shapes.
  */
  public static class Plotxy extends Value.Function implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -836149132905067212L;
	public static final Type.Function TT = 
      new Type.Function(new Type.Structured(
          new Type[] { Type.CONTINUOUS, Type.CONTINUOUS, Type.CONTINUOUS, Type.CONTINUOUS, 
                       new Type.Vector(new Type.Structured(
                         new Type[] { new Type.Obj(Shape.class.getName()), RENDATTRIB })) } ),
        new Type.Obj(Plot.PlotSpace.class.getName()));

    public Plotxy()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      Plot.PlotSpace p = new Plot.PlotSpace((float)sv.doubleCmpnt(0),(float)sv.doubleCmpnt(1),
                                            (float)sv.doubleCmpnt(2),(float)sv.doubleCmpnt(3));
      Value.Vector shapes = (Value.Vector) sv.cmpnt(4);
      for (int i = 0; i < shapes.length(); i++)
      {
        Value.Structured elti = (Value.Structured) shapes.elt(i);
        Value.Structured attr = (Value.Structured) elti.cmpnt(1);
        p.addShape((Shape) ((Value.Obj) elti.cmpnt(0)).getObj(), 
                   ((Value.Discrete) attr.cmpnt(0)).getDiscrete() == Value.TRUE.getDiscrete(),
                   ((Value.Discrete) attr.cmpnt(1)).getDiscrete() == Value.TRUE.getDiscrete(),
                   (Composite) ((Value.Obj) attr.cmpnt(2)).getObj(),
                   (Paint) ((Value.Obj) attr.cmpnt(3)).getObj(),
                   (Stroke) ((Value.Obj) attr.cmpnt(4)).getObj(),null);
      }
      return new Value.Obj(p);
    }
  }

  


  /** <code>[(a,b,...)] | [a] -> Obj(Plot.PlotSpace)</code> Convenience function for {@link 
      TwoDPlot.Ploto}.  Pops up plot in a window on the desktop. 
  */ 
  public static final PlotF plot = new PlotF();

  /** <code>[(a,b,...)] | [a] -> Obj(Plot.PlotSpace)</code>
      <p>
      Convenience function for {@link TwoDPlot.Ploto}.  Pops up plot in a window on the desktop. 
  */ 
  public static class PlotF extends Value.Function implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1414176859464555621L;
	public static final Type.Function TT = new Type.Function(Ploto.PARAMT,Type.TRIV);

    public PlotF()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      String caption = Value.byValue(v);
      if (caption == null) caption = "Plot";
        else caption = "Plot of " + caption;
      Value.Obj o = (Value.Obj) ploto.apply(v);
      DesktopFrame.makeWindow(caption,o);
      return Value.TRIV;
    } 
  }


  /** <code>[(a,b,...)] | [a] -> Obj(Plot.PlotSpace)</code> Creates an overlay plot from a 
      vector of structured or scalar values.  @see Ploto
  */
  public static final Ploto ploto = new Ploto();


  /** <code>[(a,b,...)] | [a] -> Obj(Plot.PlotSpace)</code> Creates an overlay plot from a 
      vector of structured or scalar values.  There are two special cases:
      <ul>
      <li><code>[(y1),(y2),...,(yn)] | [y1,...,yn]</code> - A vector of single component 
      structures or scalars.  In this case, Iota is used for the X-Axis (and no label).
      <li><code>[(x1,y11,y12,...,y1k),....]</code> - A vector of structures whose first 
      component is the x coordinate.
      </ul>
      @result Returns an Obj(Plot.PlotSpace).
  */
  public static class Ploto extends Value.Function implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 526834149229413067L;
	public static final Type.Union PARAMT = 
      new Type.Union(new Type[] { new Type.Vector(Type.STRUCTURED),new Type.Vector(Type.SCALAR) });
    public static final Type.Function TT = 
      new Type.Function(PARAMT,new Type.Obj(Plot.PlotSpace.class.getName()));

    public Ploto()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      Value.Vector data = (Value.Vector) v;
      Plot.PlotSpace p;
 
      Type.Structured eltType = null;
      boolean needIota = false;
      if ( ((Type.Vector) data.t).elt instanceof Type.Structured)
      {
        eltType = (Type.Structured) ((Type.Vector) data.t).elt;
        needIota = eltType.cmpnts.length == 1;
      }
      else needIota = true;   // must be vector of scalar.

      if (needIota)           // Single component structured.
      {
        String label = "";
        if ( eltType != null && eltType.labels != null ) label = eltType.labels[0];
        if (label == null) label = "";

        if (eltType != null) data = data.cmpnt(0);

	float min = (float) VectorFN.minimum.applyDouble(data);
	float max = (float) VectorFN.maximum.applyDouble(data);

        p = new Plot.PlotSpace(0,data.length()-1,min,max);
        p.setOpaque(true);
        p.setBackground(Color.white);

        ValueLinePlot lp = new ValueLinePlot((Value.Vector) VectorFN.iota.apply(
                                             new Value.Discrete(data.length())),data);
        p.addDrawShape(lp);
        p.addFillShape(new Plot.XAxis("",2));
        p.addFillShape(new Plot.YAxis(label,2));
      }
      else 
      {
        String label = "";

        int cmpnts = ((Type.Structured) ((Type.Vector) data.t).elt).cmpnts.length;
        Value.Vector xData = data.cmpnt(0);
        Value.Vector yData[] = new Value.Vector[cmpnts-1];
     
        float tmp; 
        float ymin = Float.MAX_VALUE;
        float ymax = - Float.MAX_VALUE; 
        for (int i = 0; i < cmpnts - 1; i++)
        {
          yData[i] = data.cmpnt(i+1);
          tmp = (float) VectorFN.minimum.applyDouble(yData[i]);
          if (tmp < ymin) ymin = tmp;
          tmp = (float) VectorFN.maximum.applyDouble(yData[i]);
          if (tmp > ymax) ymax = tmp;
        }

        boolean connected = true;
        float xmin = Float.MAX_VALUE;
        float xmax = -Float.MAX_VALUE;
        for (int i = 0; i < xData.length(); i++)
        {
          float x = (float) xData.doubleAt(i);
          if (x < xmax) connected = false;
          if (x < xmin) xmin = x;
          if (x > xmax) xmax = x;
        }
        p = new Plot.PlotSpace(xmin,xmax,ymin,ymax);

        for (int i = 0; i < cmpnts - 1; i++)
        {
          ValueLinePlot lp = new ValueLinePlot(xData,yData[i]);
          if (!connected) lp.setMarker(new Plot.CircleMarker(5));
          // Only use markers if absolutely necessary.  They slow the rendering
          // to a crawl in the current implementation.
          lp.connected = connected;
          p.addDrawShape(lp);
        }

        label = "";
        if (eltType.labels != null && eltType.labels[0] != null) label = eltType.labels[0];
        p.addFillShape(new Plot.XAxis(label,2));

        label = "";
        if (eltType.labels != null && eltType.labels[1] != null) label = eltType.labels[1];
        p.addFillShape(new Plot.YAxis(label,2));
      }

      return new Value.Obj(p);
    }
  }


  /** <code>[(a)] | [a] -> Obj(Plot.PlotSpace)</code> Convenience function for 
      {@link TwoDPlot.Histogramo}.  Pops up the histogram in a window on the desktop. 
  */ 
  public static final Histogram histogram = new Histogram();

  /** <code>[(a)] | [a] -> Obj(Plot.PlotSpace)</code> 
      <p>
      Convenience function for {@link TwoDPlot.Histogramo}.  Pops up the histogram in a window 
      on the desktop. 
  */ 
  public static class Histogram extends Value.Function implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6087752006287226523L;
	public static final Type.Function TT = new Type.Function(Histogramo.PARAMT,Type.TRIV);

    public Histogram()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      String caption = Value.byValue(v);
      if (caption == null) caption = "Histogram";
        else caption = "Histogram of " + caption;
      Value.Obj o = (Value.Obj) histogramo.apply(v);
      DesktopFrame.makeWindow(caption,o);
      return Value.TRIV;
    } 
  }

  /** <code>[(a)] | [a] -> Obj(Plot.PlotSpace)</code> Histogram plot.  */ 
  public static final Histogramo histogramo = new Histogramo();

  /** <code>[(a)] | [a] -> Obj(Plot.PlotSpace)</code>
      <p>
      Histogram plot.  
  */ 
  public static class Histogramo extends Value.Function implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8044262331017141423L;
	public static final Type.Union PARAMT = 
      new Type.Union( new Type[] { new Type.Vector(Type.SCALAR), 
        new Type.Vector(new Type.Structured(new Type[] { Type.SCALAR }) )});
    public static final Type.Function TT = 
      new Type.Function(PARAMT,new Type.Obj(Plot.PlotSpace.class.getName()));

    public Histogramo()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      Value.Vector data = (Value.Vector) v;
      Plot.PlotSpace p;

      String label = "";
      if ( ((Type.Vector) data.t).elt instanceof Type.Structured) 
      {
        Type.Structured eltType = (Type.Structured) ((Type.Vector) data.t).elt;
        if ( eltType.labels != null && eltType.labels[0] != null ) label = eltType.labels[0];
        data = data.cmpnt(0);
      }

      // Find min and max.  Ignore "missing" values.
      float min = Float.MAX_VALUE;
      float max = -Float.MAX_VALUE;
      
      for (int i = 0; i < data.length(); i++) {
	  Value.Scalar elt = (Value.Scalar)data.elt(i);
	  if (elt.status() == Value.S_PROPER ) {
	      float x = (float)elt.getContinuous();
	      if (x < min) min = x;
	      if (x > max) max = x;
	  }
      }
      

      p = new Plot.PlotSpace(min,max,0,data.length());
      p.setOpaque(true);
      p.setBackground(Color.white);

      HistogramPlot lp = new HistogramPlot(data);
      lp.setIntervalSize((max-min) / 10f);
      p.addFillShape(lp,null,Color.yellow,null);   // Filled yellow.
      p.addDrawShape(lp);                          // Outline.
      p.addFillShape(new Plot.XAxis(label,2));
      p.addFillShape(new Plot.YAxis("Count",1));
      p.setBoundsToPreferred();
      p.setYLower(0);
      return new Value.Obj(p);
    }
  }


  /** A simple line plotter that operates of CDMS vectors. */
  public static class ValueLinePlot extends Plot.LinePlot
  {
    Value.Vector datax, datay;

    public ValueLinePlot(Value.Vector datax, Value.Vector datay)
    {
      this.datax = datax;
      this.datay = datay;

      boolean connected = true;

      for (int i = 0; i < size(); i++) {

	  // Extract x and y elements.
	  Value.Scalar xElt = (Value.Scalar)datax.elt(i);
	  Value.Scalar yElt = (Value.Scalar)datay.elt(i);

	  // Make sure both x and y are proper.
	  boolean proper = false;
	  if ( xElt.status() == Value.S_PROPER && yElt.status() == Value.S_PROPER )
	      proper = true;


	  if ( proper ) {
	      if (connected) {
		  addDataToPath(null,getX(i),getY(i));
	      }
	      else { // if not connected
		  addDataToPath(null,getX(i),getY(i));
		  setConnected(true);
		  connected = true;
	      }
	  }
	  else { // if not proper 
	      if (connected) {
		  setConnected(false);
		  connected = false;
	      }
	      else {// if not connected
		  // do nothing.
	      }
	  }

	  


      }
    }

    public int size()
    {
      return datax.length();
    }

    public float getX(int i)
    {
      return (float) datax.doubleAt(i);
    }

    public float getY(int i)
    {
      return (float) datay.doubleAt(i);
    }

    public String toString()
    {
      return "Data Plot";
    }
  }

  /** Plots a CDMS function. */
  public static class FnLinePlot extends Plot.FreeGeneralPath implements 
    Plot.TransformDependantShape
  {
    public final Value.Vector fns;

    public FnLinePlot(Value.Vector fns)
    {
      this.fns = fns;
    }

    public String toString()
    {
      return "Plot of " + fns.toString();
    }

    public void transformChanged(Plot.PlotSpace ps)
    {
      reset();

      float xL = ps.getXLower();
      float xU = ps.getXUpper();

      int num = 20;
      float dx = (xU - xL) / (float) num;
      Value.VariableContinuous vcx = new Value.VariableContinuous(0);

      for (int k = 0; k < fns.length(); k++)
      {
        Value.Function fnk = (Value.Function) fns.elt(k);

        float x = xL;
        vcx.setContinuous(x);
        float y = (float) fnk.applyDouble(vcx);
        moveTo(x,y);

        for (int i = 0; i < num; i++)
        {
          float c1x = x + dx/3.0f;
          vcx.setContinuous(c1x);
          float c1y = (float) fnk.applyDouble(vcx);

          float c2x = x + 2*dx/3.0f;
          vcx.setContinuous(c2x);
          float c2y = (float) fnk.applyDouble(vcx);

          x = x + dx;
          vcx.setContinuous(x);
          y = (float) fnk.applyDouble(vcx);
          curveTo(c1x,c1y,c2x,c2y,x,y);
        }
      }
    }
  }

  /** Histogram plot. */
  public static class HistogramPlot extends Plot.FreeGeneralPath implements Plot.PlotBounds
  {
    private float intervalSize;

    Value.Vector data;

    int freq[];
    float min;
    float max;
    int maxFreq;
    int minFreq;

    public HistogramPlot(Value.Vector data)
    {
      this.data = data;
      intervalSize = 1;
      intervalChanged();
    }

    public String toString()
    {
      return "Histogram";
    }

    public float getPreferredXUpper()
    {
      return max;
    }

    public float getPreferredXLower()
    {
      return min;
    }

    public float getPreferredYUpper()
    {
      return maxFreq;
    }

    public float getPreferredYLower()
    {
      return minFreq;
    }


    public void setIntervalSize(float intervalSize)
    {
      this.intervalSize = intervalSize;
      if ( ((Type.Vector) data.t).elt instanceof Type.Discrete && intervalSize < 1 )
        this.intervalSize = 1; 
      intervalChanged();
    }

    public float getIntervalSize()
    {
      return intervalSize;
    }

    protected void intervalChanged()
    {
      reset();

      // Find min and max.  Ignore "missing" values.
      min = Float.MAX_VALUE;
      max = -Float.MAX_VALUE;
      
      for (int i = 0; i < data.length(); i++) {
	  Value.Scalar elt = (Value.Scalar)data.elt(i);
	  if (elt.status() == Value.S_PROPER ) {
	      float x = (float)elt.getContinuous();
	      if (x < min) min = x;
	      if (x > max) max = x;
	  }
      }


      if ( ((Type.Vector) data.t).elt instanceof Type.Discrete ) max++;

      int intervals = (int) Math.ceil((max-min) / intervalSize) + 1;   
      freq = new int[intervals];   
      for (int i = 0; i < data.length(); i++) 
      {      
	  // only include in the histogram if status is S_PROPER
	  Value.Scalar elt = (Value.Scalar)data.elt(i);
	  if (elt.status() == Value.S_PROPER ) {
	      int index = (int) Math.floor((elt.getContinuous() - min) / intervalSize);
	      if (index < 0) index = 0;
	      if (index < freq.length) freq[index] ++;
	  }
      }

      maxFreq = - Integer.MAX_VALUE;
      minFreq = Integer.MAX_VALUE;
      for (int i = 0; i < freq.length; i++)
      {
        if (freq[i] > maxFreq) maxFreq = freq[i];
        if (freq[i] < minFreq) minFreq = freq[i];
      }

      // Bar 
      float barWidth = intervalSize;
      float interval = min;
      float lastX = 0;
      for (int i = 0; i < freq.length; i++)
      {
        float newY = freq[i];
        float newX = interval;
        if (i == 0)
          append(new Rectangle2D.Float(newX,0,barWidth,newY),false);
        else append(new Rectangle2D.Float(lastX + barWidth,0,newX - lastX,newY),false);
        interval = interval + intervalSize;
        if (interval > max - intervalSize) interval = max - intervalSize;
        lastX = newX;
      }
    }
  }

}

// End of file.
