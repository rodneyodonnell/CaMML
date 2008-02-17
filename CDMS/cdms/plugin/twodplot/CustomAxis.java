package cdms.plugin.twodplot;

import cdms.plugin.twodplot.Plot.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

public class CustomAxis
{
  /**  An upper or lower X-Axis.  Defaults to lower. */
  public static class DiscreteXAxis extends Axis
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7299125798087366477L;
	boolean bottomAxis = true;
//    java.text.DecimalFormat valFormat = new java.text.DecimalFormat();

//    GlyphVector labelGlyph, lLabelGlyph, rLabelGlyph, mLabelGlyph[];
//    Rectangle2D labelBounds, lLabelBounds, rLabelBounds, mLabelBounds[];


    String[] vals;
    GlyphVector labelGlyph, valGlyph[];
    Rectangle2D labelBounds, valBounds[];

    public DiscreteXAxis(String label)
    {
      super(label);
//      valFormat.setMaximumFractionDigits(prec);
    }

    public DiscreteXAxis(String label, String[] vals)
    {
      super(label);
      this.vals = vals;
    }

    public String toString()
    {
      if (bottomAxis) return "Lower X Axis";
        else return "Upper X Axis";
    }

    public boolean getIsBottomAxis()
    {
      return bottomAxis;
    }

    public void setIsBottomAxis(boolean isBottom)
    {
      bottomAxis = isBottom;
    }

    /** Reserves space for the axis labels. */
    public Reserved getReserved(PlotSpace ps)
    {
      Insets psInsets = ps.getInsets();
      float psHeight = ps.getHeight() - psInsets.top - psInsets.bottom;
      float psWidth = ps.getWidth() - psInsets.left - psInsets.right;

      // The font height will be FONT_PERC of the smaller dimension.
      float fh = (float) (FONT_PERC * Math.min(psHeight,psWidth));
      if (fh < MIN_FONT_HEIGHT) fh = MIN_FONT_HEIGHT;
      if (fh > MAX_FONT_HEIGHT) fh = MAX_FONT_HEIGHT;

      // The gap is specified as GAP_PERC of fh.
      float gap = GAP_PERC * fh;

      float xLi = Math.round(Math.ceil(ps.getXLower()));
      float xUi = Math.round(Math.floor(ps.getXUpper()));
      int nx = (int)(xUi - xLi) + 1;
      valGlyph = new GlyphVector[nx];
      valBounds = new Rectangle2D[nx];

      // Create the text.
      Graphics2D g2d = (Graphics2D) ps.getGraphics();
      if (g2d == null) return null;
      Font f = g2d.getFont().deriveFont(fh);
      FontRenderContext frc = g2d.getFontRenderContext();

      // Lower bound label.
//      String lLabel = valFormat.format(xL);
//      lLabelGlyph = f.createGlyphVector(frc, lLabel);
//      lLabelBounds = lLabelGlyph.getVisualBounds();

      // Upper bound label.
//      String rLabel = valFormat.format(xU);
//      rLabelGlyph = f.createGlyphVector(frc, rLabel);
//      rLabelBounds = rLabelGlyph.getVisualBounds();

      // Middle labels.
//      int numLabels = (int) Math.pow(2,labelDepth) - 1;
//      mLabelGlyph = new GlyphVector[numLabels];
//      mLabelBounds = new Rectangle2D[numLabels];
//      double dx = (xU - xL) / (numLabels + 1.0);
//      double cx = xL + dx;
//      for (int i = 0; i < numLabels; i++)
//      {
//        String mLabel = valFormat.format(cx);
//        mLabelGlyph[i] = f.createGlyphVector(frc, mLabel);
//        mLabelBounds[i] = mLabelGlyph[i].getVisualBounds();
//        cx += dx; 
//      }

      int count;
      if(vals != null)
      {
        for(count = 0; count < nx; count++)
        {
          valGlyph[count] = f.createGlyphVector(frc, vals[count]);
          valBounds[count] = valGlyph[count].getVisualBounds();
//          if(valBounds[count].getWidth() > xLabelSpace)
//          {
//            valGlyph[count] = null;
//            valBounds[count] = null;
//          }
        }
      }
      else
      {
        for(count = 0; count < nx; count++)
        {
          valGlyph[count] = f.createGlyphVector(frc, "" + (int)(count + xLi));
          valBounds[count] = valGlyph[count].getVisualBounds();
//          if(valBounds[count].getWidth() > xLabelSpace)
//          {
//            valGlyph[count] = null;
//            valBounds[count] = null;
//          }
        }
      }

      labelGlyph = f.createGlyphVector(frc, label);
      labelBounds = labelGlyph.getVisualBounds();

      // Total height for top or bottom is 3*gap + 2*fh (for the label and numerics).
      float th = gap + fh;
      if (label.compareTo("") != 0) th += 2*gap + fh; 

      // Total left.
      float tl = (float) gap; //(valBounds[0].getWidth() / 2.0 + gap); 

      // Total right.
      float tr = (float) gap; //(valBounds[nx-1].getWidth() / 2.0 + gap); 

      return new Reserved(bottomAxis ? 0 : th,tl,bottomAxis ? th : 0,tr); 
    }

    public void transformChanged(PlotSpace ps)
    {
      Graphics2D g2d = (Graphics2D) ps.getGraphics();
      if (g2d == null) return;

      reset();

      Insets psInsets = ps.getInsets();
      float psHeight = ps.getHeight() - psInsets.top - psInsets.bottom;
      float psWidth = ps.getWidth() - psInsets.left - psInsets.right;

      // The font height will be FONT_PERC of the smaller dimension.
      float fh = (float) (FONT_PERC * Math.min(psHeight,psWidth));
      if (fh < MIN_FONT_HEIGHT) fh = MIN_FONT_HEIGHT;
      if (fh > MAX_FONT_HEIGHT) fh = MAX_FONT_HEIGHT;

      // The gap is specified as GAP_PERC of fh.
      float gap = GAP_PERC * fh;

      float yL = ps.getYLower();
      float yU = ps.getYUpper();
      float xL = ps.getXLower();
      float xU = ps.getXUpper();
      float xLi = Math.round(Math.ceil(ps.getXLower()));
      float xUi = Math.round(Math.floor(ps.getXUpper()));
      int nx = (int)(xUi - xLi) + 1;

      // Axis line width is 10% of gap.
      Point2D xLPoint = ps.aft.transform(new Point2D.Float(xL,bottomAxis ? yL : yU),null);
      Point2D xUPoint = ps.aft.transform(new Point2D.Float(xU,bottomAxis ? yL : yU),null);
      append(new Rectangle2D.Double(xLPoint.getX()-0.05*gap,xLPoint.getY()-0.05*gap,
                                    xUPoint.getX()-xLPoint.getX()+0.1*gap,0.1*gap),false);

      // Lower bound.
//      Shape lLabelShape = lLabelGlyph.getOutline(
//        (float) (xLPoint.getX() - lLabelBounds.getWidth()/2.0),
//        (float) (xLPoint.getY() + fh + gap));
//      append(new Rectangle2D.Double(xLPoint.getX()-0.05*gap,xLPoint.getY(),0.1*gap,0.5*gap),false);
//      append(lLabelShape,false);

      // Upper bound.
//      Shape rLabelShape = rLabelGlyph.getOutline(
//        (float) (xUPoint.getX() - rLabelBounds.getWidth()/2.0),
//        (float) (xUPoint.getY() + fh + gap));
//      append(new Rectangle2D.Double(xUPoint.getX()-0.05*gap,xUPoint.getY(),0.1*gap,0.5*gap),false);
//      append(rLabelShape,false);

      // Middle labels.
//      int numLabels = (int) Math.pow(2,labelDepth) - 1;
//      double pdx = (xUPoint.getX() - xLPoint.getX()) / (numLabels + 1.0);
//     double pcx = xLPoint.getX() + pdx;
//      for (int i = 0; i < numLabels; i++)
//      {
//        if (mLabelBounds[i].getWidth() + 2*gap < pdx)
//        {
//          Shape mLabelShape = mLabelGlyph[i].getOutline(
//            (float) (pcx - mLabelBounds[i].getWidth()/2.0),
//            (float) (xUPoint.getY() + fh + gap));
//          append(new Rectangle2D.Double(pcx-0.05*gap,xUPoint.getY(),0.1*gap,0.5*gap),false);
//          append(mLabelShape,false);
//        }
//        pcx += pdx; 
//      }

      int count;
      double xls = (xUPoint.getX() - xLPoint.getX()) / nx;
      Point2D startPoint = ps.aft.transform(new Point2D.Float(xLi,bottomAxis ? yL : yU),null);
      for(count = 0; count < nx; count++)
      {
        if(valGlyph[count] != null)
        {
System.out.println("Adding " + (float) (xLPoint.getX() + (count * xls)) + ", " + (float) (xLPoint.getY() + fh + gap));
          Shape valShape = valGlyph[count].getOutline(
            (float) (startPoint.getX() + (count * xls) - valBounds[count].getWidth()/2.0),
            (float) (startPoint.getY() + fh + gap));
//          append(new Rectangle2D.Double(pcx-0.05*gap,xUPoint.getY(),0.1*gap,0.5*gap),false);
          append(valShape,false);
        }
      }

      // User label.
      Shape labelShape;
      if (bottomAxis)
        labelShape = labelGlyph.getOutline(
          (float) ((xUPoint.getX() + xLPoint.getX() - labelBounds.getWidth()) / 2.0),
          (float) (xUPoint.getY() + gap + fh + gap + labelBounds.getHeight()));
      else
        labelShape = labelGlyph.getOutline(
          (float) ((xUPoint.getX() + xLPoint.getX() - labelBounds.getWidth()) / 2.0),
          (float) (xUPoint.getY() - gap - fh - gap));
      append(labelShape,false);
    }
  }
}
