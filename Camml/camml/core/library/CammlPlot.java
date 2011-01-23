/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Leigh Fitzgibbon, Josh Comley, Lloyd Allison, Rodney O'Donnell
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// CDMS
//

// File: TestPlot.java
// Author: leighf@csse.monash.edu.au
// Modified : rodo@dgs.monash.edu.au

package camml.core.library;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.font.*;
import cdms.plugin.twodplot.Plot.*;
import cdms.plugin.twodplot.*;

import cdms.core.*;

/**
 * TaggedPlot allows us to draw a graph with labels on the X and Y axis.
 */
public class CammlPlot extends JPanel implements java.io.Serializable
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -1867222887867832971L;


    public static void main(String args[])
    {
        JDesktopPane desktop = new JDesktopPane();
        JFrame f = new JFrame("2d Plot Examples");

        f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });


        f.getContentPane().add(desktop);
        f.setSize(640,480);

        java.util.Random random = new java.util.Random();

        PlotSpace p1 = new PlotSpace(0,16,-3,3);
        NativeLinePlot lp1a = new NativeLinePlot();
        NativeLinePlot lp1b = new NativeLinePlot();
        NativeLinePlot lp1c = new NativeLinePlot();

        lp1a.setMarker(new SquareMarker(5));
        lp1b.setMarker(new CircleMarker(5));
        lp1c.setMarker(new CrossMarker(5));

        for (int j = 0; j < 16; j++) {
            lp1a.addData(null,j,(float) random.nextGaussian() - 1);
            lp1b.addData(null,j,(float) random.nextGaussian() + 0);
            lp1c.addData(null,j,(float) random.nextGaussian() + 1);
        }

        p1.addDrawShape(lp1a);
        p1.addDrawShape(lp1b);
        p1.addDrawShape(lp1c);

        XAxis2 xAxis = new XAxis2("Fancy X-Axis");
        xAxis.setTag( 0, 16, 17 ); 
        p1.addFillShape( xAxis );

        YAxis2 yAxis = new YAxis2("Fancy Y-Axis");
        yAxis.setTag( new String[] {"low", "medium", "high"} );
        p1.addFillShape( yAxis );


        JInternalFrame iFrame = Plot.makeInternalFrame(p1,"LinePlot",Color.white,0,0);
        iFrame.setPreferredSize(new Dimension(600,400));
        iFrame.pack();
        desktop.add( iFrame );


        f.setVisible(true);
    }

    public static final FancyPlot fancyPlot = new FancyPlot();

    /** ( (Label,xLabel,yLabel), [xLabel], (xMin,xMax), [yLabel], (yMin,yMax), [yName], 
        [(x,y0,y1,y2..yn)] -> Obj() */
    public static class FancyPlot extends Value.Function implements Serializable
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -5044786986992661332L;
        public static final Type.Function tt = new Type.Function(Type.STRUCTURED, 
                                                                 new Type.Obj( "PlotSpace" ) );

        public FancyPlot() { super(tt); } 
      
        public Value apply(Value v)
        {
            // extract all details from input struct.
            Value.Structured sv = (Value.Structured) v;
            Value.Structured labelStruct = (Value.Structured)sv.cmpnt(0);
            Value.Vector xLabel = (Value.Vector)sv.cmpnt(1);
            Value.Structured xMinMax = (Value.Structured)sv.cmpnt(2);
            Value.Vector yLabel = (Value.Vector)sv.cmpnt(3);
            Value.Structured yMinMax = (Value.Structured)sv.cmpnt(4);
            //Value.Vector yName  = (Value.Vector)sv.cmpnt(5);
            Value.Vector input  = (Value.Vector)sv.cmpnt(6);
      
            System.out.println("Plotting");

            // create Plot Space
            PlotSpace plotSpace = new PlotSpace( (float)xMinMax.doubleCmpnt(0), 
                                                 (float)xMinMax.doubleCmpnt(1),
                                                 (float)yMinMax.doubleCmpnt(0), 
                                                 (float)yMinMax.doubleCmpnt(1) );
      
            // Make an array of different marker types to place on the lines.
            Shape[] markerArray = new Shape[] { new SquareMarker(5),
                                                new CircleMarker(5),
                                                new CrossMarker(5) };

            // create space to store the lines on the plot.
            NativeLinePlot[] plotArray = 
                new NativeLinePlot[ ((Value.Structured)input.elt(0)).length() - 1 ];
            Value.Vector xVec = input.cmpnt(0);
            for ( int i = 0; i < plotArray.length; i++ ) {
                plotArray[i] = new NativeLinePlot();
                Value.Vector yVec = input.cmpnt(i+1);
                Type yType = ((Type.Vector)yVec.t).elt;
          
                // regular case.  Simply plot appropriate line
                if ( yType instanceof Type.Scalar ) {
                    for ( int j = 0; j < xVec.length(); j++ ) {
                        plotArray[i].addData( null, 
                                              (float)xVec.doubleAt(j), 
                                              (float)yVec.doubleAt(j) );
                    }
                }
                // We also accept vectors of type (y, standard deviation) and plot error bars.
                else if ( yType instanceof Type.Structured ) {
                    for ( int j = 0; j < xVec.length(); j++ ) {
                        Value.Structured elt = (Value.Structured)yVec.elt(j);
                        if ( elt.length() != 2 ) {
                            throw new RuntimeException("Invalid number of elements");
                        }
              
                        float mean = (float)elt.doubleCmpnt(0);
                        float sd = (float)elt.doubleCmpnt(1);


                        // Draw error bars around this point.
                        NativeLinePlot errorBar = new Plot.NativeLinePlot();
                        float barWidth = (plotSpace.getXUpper() - plotSpace.getXLower()) / 100;
                        errorBar.addData( null, (float)xVec.doubleAt(j) - barWidth, mean + sd );
                        errorBar.addData( null, (float)xVec.doubleAt(j) + barWidth, mean + sd );
                        errorBar.addData( null, (float)xVec.doubleAt(j) + 0, mean + sd );
                        errorBar.addData( null, (float)xVec.doubleAt(j) + 0, mean - sd );
                        errorBar.addData( null, (float)xVec.doubleAt(j) - barWidth, mean - sd );
                        errorBar.addData( null, (float)xVec.doubleAt(j) + barWidth, mean - sd );

                        // Draw line segment.
                        plotSpace.addDrawShape( errorBar );

                        plotArray[i].addData( null, (float)xVec.doubleAt(j), mean );

                    }
                }
                else {
                    throw new RuntimeException("Unsupported type : " + yType );
                }

                // cycle through the different marker choices.
                plotArray[i].setMarker( markerArray[ i % markerArray.length ] );
                plotSpace.addDrawShape( plotArray[i] );
            }


            // create the axis.
            Axis xAxis = makeAxis( ((Value.Str)labelStruct.cmpnt(1)).getString(), xLabel, true );
            Axis yAxis = makeAxis( ((Value.Str)labelStruct.cmpnt(2)).getString(), yLabel, false );

            plotSpace.addFillShape( xAxis );
            plotSpace.addFillShape( yAxis );


            return new Value.Obj( plotSpace );
        }

        /** Horizontal line */
        public static class HLine extends FreeGeneralPath
        {
            /** Static horizontal line of length 5. */
            public static final HLine len5 = new HLine(5);
            public HLine(float size)
            {
                moveTo(-size,0);
                lineTo(size,0);
            } 
        }

        /**
         * Create an axis from it's name and a list of it's tags (labels along axis) 
         * if xAxis == true an XAxis2 will be created, else a YAxis2 will be created.
         */
        public static Axis makeAxis( String name, Value.Vector tagVec, boolean xAxis ) 
        {
      
            //Type.Vector vecType = (Type.Vector)tagVec.t;
      
            String[] tagArray;
            if ( tagVec.length() == 0) { tagArray = null; }
            else {
                tagArray = new String[tagVec.length()];
                for ( int i = 0; i < tagArray.length; i++ ) {
                    tagArray[i] = ((Value.Str)tagVec.elt(i)).getString();
                }
            }

            if ( xAxis == true ) {
                XAxis2 axis = new XAxis2( name );
                axis.setTag( tagArray );      
                return axis;
            }
            else {
                YAxis2 axis = new YAxis2( name );
                axis.setTag( tagArray );      
                return axis;
            }
        }
    }

    


    /**  Custom XAxis: <br>
     *   labal = label of axis <br>
     *   prec = precision to which each label is stated. <br>
     */
    public static class XAxis2 extends Axis
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3506955782636649207L;

        boolean bottomAxis = true;

        /** Tag of each value on the axis, not a very good name but label was already taken. */
        String[] tag = null;

        /** We need to know precision to round decimals to a convenient number of places. */
        public final int precision;
        protected final java.text.DecimalFormat valFormat;

        /** Set tags to be displayed along axis. */
        public void setTag( String[] tag ) { this.tag = tag; }

        /** Set tags to be displayed along axis. */
        public void setTag( double min, double max, int numTags ) {      
            double[] vals = new double[numTags];
            double step = (max - min)/(numTags-1);
            for ( int i = 0; i < vals.length; i++ ) {
                vals[i] = min + step * i;
            }
            setTag(vals);
        }      

        /** Set tag from an array of doubles */
        public void setTag( double[] doubleTag ) {
            this.tag = new String[ doubleTag.length ];
            for ( int i = 0; i < doubleTag.length; i++ ) {
                tag[i] = valFormat.format( doubleTag[i] );
            }
        }
      

        GlyphVector labelGlyph, lLabelGlyph, rLabelGlyph, mLabelGlyph[];
        Rectangle2D labelBounds, lLabelBounds, rLabelBounds, mLabelBounds[];

        public XAxis2( String label, int precision )
        {
            super(label);
            this.precision = precision;
            valFormat = new java.text.DecimalFormat();
            valFormat.setMaximumFractionDigits(precision);

        }

        public XAxis2( String label )
        {
            super(label);
            precision = 2;
            valFormat = new java.text.DecimalFormat();
            valFormat.setMaximumFractionDigits(precision);
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

            //float yL = ps.getYLower();
            //float yU = ps.getYUpper();
            float xL = ps.getXLower();
            float xU = ps.getXUpper();

            // if necesarry create axis labels on the fly.
            if ( tag == null ) { setTag( xL, xU, 4 ); }

            // Create the text.
            Graphics2D g2d = (Graphics2D) ps.getGraphics();
            if (g2d == null) return null;
            Font f = g2d.getFont().deriveFont(fh);
            FontRenderContext frc = g2d.getFontRenderContext();

            // Lower bound label.
            lLabelGlyph = f.createGlyphVector(frc, tag[0] );
            lLabelBounds = lLabelGlyph.getVisualBounds();

            // Upper bound label.
            rLabelGlyph = f.createGlyphVector(frc, tag[tag.length-1]);
            rLabelBounds = rLabelGlyph.getVisualBounds();

            // Middle labels.
            int numMiddleLabels = tag.length - 2;
            mLabelGlyph = new GlyphVector[numMiddleLabels];
            mLabelBounds = new Rectangle2D[numMiddleLabels];
            for (int i = 0; i < numMiddleLabels; i++) {          
                mLabelGlyph[i] = f.createGlyphVector(frc, tag[i+1]);
                mLabelBounds[i] = mLabelGlyph[i].getVisualBounds();
            }

            labelGlyph = f.createGlyphVector(frc, label);
            labelBounds = labelGlyph.getVisualBounds();

            // Total height for top or bottom is 3*gap + 2*fh (for the label and numerics).
            float th = gap + fh + gap;
            if (label.compareTo("") != 0) th += gap + fh; 
      
            // Total left.
            float tl = (float) (lLabelBounds.getWidth() / 2.0 + gap); 

            // Total right.
            float tr = (float) (rLabelBounds.getWidth() / 2.0 + gap); 

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

            // Axis line width is 10% of gap.
            Point2D xLPoint = ps.aft.transform(new Point2D.Float(xL,bottomAxis ? yL : yU),null);
            Point2D xUPoint = ps.aft.transform(new Point2D.Float(xU,bottomAxis ? yL : yU),null);
            append(new Rectangle2D.Double(xLPoint.getX()-0.05*gap,xLPoint.getY()-0.05*gap,
                                          xUPoint.getX()-xLPoint.getX()+0.1*gap,0.1*gap),false);

            // Lower bound.
            Shape lLabelShape = lLabelGlyph.getOutline(
                                                       (float) (xLPoint.getX() - lLabelBounds.getWidth()/2.0),
                                                       (float) (xLPoint.getY() + fh + gap));
            append( new Rectangle2D.Double(xLPoint.getX()-0.05*gap,xLPoint.getY(),0.1*gap,0.5*gap),
                    false );
            append(lLabelShape,false);

            // Upper bound.
            Shape rLabelShape = rLabelGlyph.getOutline(
                                                       (float) (xUPoint.getX() - rLabelBounds.getWidth()/2.0),
                                                       (float) (xUPoint.getY() + fh + gap));
            append( new Rectangle2D.Double(xUPoint.getX()-0.05*gap,xUPoint.getY(),0.1*gap,0.5*gap),
                    false );
            append(rLabelShape,false);

            // Middle labels.
            int numMiddleLabels = tag.length - 2;
            double pdx = (xUPoint.getX() - xLPoint.getX()) / (numMiddleLabels + 1.0);
            double pcx = xLPoint.getX() + pdx;
            for (int i = 0; i < numMiddleLabels; i++) {          
                if (mLabelBounds[i].getWidth() + 2*gap < pdx) {              
                    Shape mLabelShape = mLabelGlyph[i].getOutline(
                                                                  (float) (pcx - mLabelBounds[i].getWidth()/2.0),
                                                                  (float) (xUPoint.getY() + fh + gap));
                    append(new Rectangle2D.Double(pcx-0.05*gap,xUPoint.getY(),0.1*gap,0.5*gap),false);
                    append(mLabelShape,false);
                }
                pcx += pdx; 
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
    

    /**  A left or right Y-Axis.  Defaults to left. */
    public static class YAxis2 extends Axis
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3365736956103232132L;

        protected boolean leftAxis = true;

        /** Tag of each value on the axis, not a very good name but label was already taken. */
        String[] tag = null;

        /** We need to know precision to round decimals to a convenient number of places. */
        public final int precision;
        protected final java.text.DecimalFormat valFormat;

        /** Set tags to be displayed along axis. */
        public void setTag( String[] tag ) { this.tag = tag; }

        /** Set tags to be displayed along axis. */
        public void setTag( double min, double max, int numTags ) {      
            double[] vals = new double[numTags];
            double step = (max - min)/(numTags-1);
            for ( int i = 0; i < vals.length; i++ ) {
                vals[i] = min + step * i;
            }
            setTag(vals);
        }      

        /** Set tag from an array of doubles */
        public void setTag( double[] doubleTag ) {
            this.tag = new String[ doubleTag.length ];
            for ( int i = 0; i < doubleTag.length; i++ ) {
                tag[i] = valFormat.format( doubleTag[i] );
            }
        }
      
        GlyphVector labelGlyph, tLabelGlyph, bLabelGlyph, mLabelGlyph[];
        Rectangle2D labelBounds, tLabelBounds, bLabelBounds, mLabelBounds[];

        public YAxis2( String label, int precision )
        {
            super(label);
            this.precision = precision;
            valFormat = new java.text.DecimalFormat();
            valFormat.setMaximumFractionDigits(precision);

        }

        public YAxis2( String label )
        {
            super(label);
            precision = 2;
            valFormat = new java.text.DecimalFormat();
            valFormat.setMaximumFractionDigits(precision);
        }


        public String toString()
        {
            if (leftAxis) return "Left Y Axis";
            else return "Right Y Axis";
        }

        public boolean getIsLeftAxis()
        {
            return leftAxis;
        }

        public void setIsLeftAxis(boolean isLeft)
        {
            leftAxis = isLeft;
        }

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

            float yL = ps.getYLower();
            float yU = ps.getYUpper();
            //float xL = ps.getXLower();
            //float xU = ps.getXUpper();

            // Create the text.
            Graphics2D g2d = (Graphics2D) ps.getGraphics();
            if (g2d == null) return null;
            Font f = g2d.getFont().deriveFont(fh);
            FontRenderContext frc = g2d.getFontRenderContext();

            if ( tag == null ) {  setTag( yL, yU, 4 ); }

            // Top label (yUpper).
            String tLabel = tag[tag.length-1];
            tLabelGlyph = f.createGlyphVector(frc, tLabel);
            tLabelBounds = tLabelGlyph.getVisualBounds();

            // Bottom label (yLower).
            String bLabel = tag[0];
            bLabelGlyph = f.createGlyphVector(frc, bLabel);
            bLabelBounds = bLabelGlyph.getVisualBounds();

            // Middle labels.
            int numLabels = tag.length-2;
            mLabelGlyph = new GlyphVector[numLabels];
            mLabelBounds = new Rectangle2D[numLabels];
            double dy = (yU - yL) / (numLabels + 1.0);
            double cy = yL + dy;
            double maxMWidth = Math.max(tLabelBounds.getWidth(),bLabelBounds.getWidth());
            for (int i = 0; i < numLabels; i++)
                {
                    String mLabel = tag[i+1];
                    mLabelGlyph[i] = f.createGlyphVector(frc, mLabel);
                    mLabelBounds[i] = mLabelGlyph[i].getVisualBounds();
                    if (mLabelBounds[i].getWidth() > maxMWidth) maxMWidth = mLabelBounds[i].getWidth(); //???
                    cy += dy; 
                }

            // User label.
            labelGlyph = f.createGlyphVector(frc, label);
            labelBounds = labelGlyph.getVisualBounds();

            // Total width is 3*gap + fh + length of longest numeric.
            float tw = gap + (float) maxMWidth + gap;
            if (label.compareTo("") != 0) tw += gap + labelBounds.getHeight(); 

            // Top and bottom space
            float tb = 0.5f * fh + gap;

            return new Reserved(tb,leftAxis ? tw : 0,tb,leftAxis ? 0 : tw); 
        }

        public void transformChanged(PlotSpace ps)
        {
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

            // Axis line width is 10% of gap.
            Point2D yLPoint = ps.aft.transform(new Point2D.Float(leftAxis ? xL : xU,yL),null);
            Point2D yUPoint = ps.aft.transform(new Point2D.Float(leftAxis ? xL : xU,yU),null);
            append(new Rectangle2D.Double(yUPoint.getX()-0.05*gap,yUPoint.getY()-0.05*gap,
                                          0.1*gap,yLPoint.getY()-yUPoint.getY()+0.1*gap),false);

            // Lower bound.
            if (bLabelGlyph != null)
                {
                    Shape bLabelShape = null;
                    if (leftAxis)
                        {
                            bLabelShape = bLabelGlyph.getOutline(
                                                                 (float) (yLPoint.getX() - gap - bLabelBounds.getWidth()),
                                                                 (float) (yLPoint.getY() + bLabelBounds.getHeight() / 2.0));
                            append(new Rectangle2D.Double(
                                                          yLPoint.getX() - 0.5*gap,yLPoint.getY()-0.05*gap,0.5*gap,0.1*gap),false); 
                        }
                    else
                        {
                            bLabelShape = bLabelGlyph.getOutline(
                                                                 (float) (yLPoint.getX() + gap),
                                                                 (float) (yLPoint.getY() + bLabelBounds.getHeight() / 2.0));
                            append(new Rectangle2D.Double(
                                                          yLPoint.getX(),yLPoint.getY()-0.05*gap,0.5*gap,0.1*gap),false); 
                        }
                    append(bLabelShape,false);
                }

            // Upper bound.
            if (tLabelGlyph != null)
                {
                    Shape tLabelShape = null;
                    if (leftAxis)
                        {
                            tLabelShape = tLabelGlyph.getOutline(
                                                                 (float) (yUPoint.getX() - gap - tLabelBounds.getWidth()),
                                                                 (float) (yUPoint.getY() + tLabelBounds.getHeight() / 2.0));
                            append(new Rectangle2D.Double(
                                                          yUPoint.getX() - 0.5*gap,yUPoint.getY()-0.05*gap,0.5*gap,0.1*gap),false); 
                        }
                    else
                        {
                            tLabelShape = tLabelGlyph.getOutline(
                                                                 (float) (yUPoint.getX() + gap),
                                                                 (float) (yUPoint.getY() + tLabelBounds.getHeight() / 2.0));
                            append(new Rectangle2D.Double(
                                                          yUPoint.getX(),yUPoint.getY()-0.05*gap,0.5*gap,0.1*gap),false); 
                        }
                    append(tLabelShape,false);
                }

            // Middle labels.
            int numLabels = tag.length - 2;
            double pdy = (yLPoint.getY() - yUPoint.getY()) / (numLabels + 1.0);
            double pcy = yLPoint.getY() - pdy;
            double maxMWidth = Math.max(tLabelBounds.getWidth(),bLabelBounds.getWidth());
            for (int i = 0; i < numLabels; i++)
                {
                    if (mLabelBounds[i].getHeight() + 2*gap < pdy)
                        {
                            Shape mLabelShape = null;
                            if (leftAxis)
                                {
                                    mLabelShape = mLabelGlyph[i].getOutline(
                                                                            (float) (yUPoint.getX() - gap - mLabelBounds[i].getWidth()),
                                                                            (float) (pcy + mLabelBounds[i].getHeight() / 2.0));
                                    append(new Rectangle2D.Double(
                                                                  yUPoint.getX() - 0.5*gap,pcy-0.05*gap,0.5*gap,0.1*gap),false); 
                                }
                            else
                                {
                                    mLabelShape = mLabelGlyph[i].getOutline(
                                                                            (float) (yUPoint.getX() + gap),
                                                                            (float) (pcy + mLabelBounds[i].getHeight() / 2.0));
                                    append(new Rectangle2D.Double(
                                                                  yUPoint.getX(),pcy-0.05*gap,0.5*gap,0.1*gap),false); 
                                }
                            if (mLabelBounds[i].getWidth() > maxMWidth) maxMWidth = mLabelBounds[i].getWidth();
                            append(mLabelShape,false);
                        }
                    pcy -= pdy; 
                }

            // User label.
            AffineTransform labelTransform = new AffineTransform();
            if (leftAxis)
                labelTransform.translate(
                                         (float) (yUPoint.getX() - gap - maxMWidth) - gap,
                                         (float) ((yUPoint.getY() + yLPoint.getY() + labelBounds.getWidth()) / 2.0));
            else
                labelTransform.translate(
                                         (float) (yUPoint.getX() + gap + maxMWidth) + gap + fh,
                                         (float) ((yUPoint.getY() + yLPoint.getY() + labelBounds.getWidth()) / 2.0));
            labelTransform.rotate(1.5*Math.PI);
            Shape labelShape = labelTransform.createTransformedShape(labelGlyph.getOutline());
            append(labelShape,false);
        }
    }


    
}

// End of file.
