//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Plot.java
// Author: leighf@csse.monash.edu.au

package cdms.plugin.twodplot;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.image.*;

/**
    A two dimensional plotting framework.  For a simple demo run: 
    <code>java cdms.plugin.twodplot.Plot</code>
    <p>
    Notes:
    <ul>
    <li>We use shapes exclusively.
    <li>The Plot can choose how to render each shape.
    <li>Access to GUI classes should be through the messaging thread only so 
        use SwingUtilities.invokeLater() when necessary.
    <li> We use floats to match GeneralPath.
    </ul>

TODO:

  Fix problems with Multivariate plot.
  Don't fill text (i.e. create separate shape for axis borders).

*/
public class Plot extends JPanel implements java.io.Serializable
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5710407238991693866L;

public static JInternalFrame makeInternalFrame(Component c, String label, 
                                                 Color color,int x, int y)
  {
    JInternalFrame f = new JInternalFrame(label,true,false,true,true);
    f.getContentPane().setBackground(color);
    f.getContentPane().add(c);
    f.setLocation(x,y);
    f.setPreferredSize(new Dimension(200,100));
    f.pack();
    f.setVisible(true);
    return f;
  }

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
    f.setSize(650,480);

    java.util.Random random = new java.util.Random();

//     PlotSpace p1 = new PlotSpace(1,100,-3,3);
//     NativeLinePlot lp1 = new NativeLinePlot(); 
//     for (int i = 1; i < 100; i++)
//       lp1.addData(null,i,(float) random.nextGaussian());
//     p1.addDrawShape(lp1);
//     desktop.add(makeInternalFrame(p1,"LinePlot",Color.white,10,10));

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

    p1.addFillShape(new XAxis("X-Axis",2));
    p1.addFillShape(new YAxis("Y-Axis",2));
    desktop.add(makeInternalFrame(p1,"LinePlot",Color.white,10,10));



    PlotSpace p2 = new PlotSpace(-10,110,-3,3);
    NativeLinePlot lp2 = new NativeLinePlot(); 
    lp2.setConnected(false);
    lp2.setMarker(new CrossMarker(5));
    for (int i = 1; i < 100; i++)
      lp2.addData(null,i,(float) random.nextGaussian());
    p2.addDrawShape(lp2);
    desktop.add(makeInternalFrame(p2,"Scatter",Color.white,220,10));

    PlotSpace p3 = new PlotSpace(0,10000,-10,110);
    NativeLinePlot lp3 = new NativeLinePlot(); 
    for (int i = 0; i <= 10000; i++)
      lp3.addData(null,i,50 + 20*(float) random.nextGaussian());
    p3.addDrawShape(lp3);
    p3.addFillShape(new XAxis("X-Axis",2));
    p3.addFillShape(new YAxis("Y-Axis",2));
    desktop.add(makeInternalFrame(p3,"LinePlot with Axes",Color.white,430,10));

    PlotSpace p4 = new PlotSpace(-10,110,-3,3);
    NativeLinePlot lp4 = new NativeLinePlot(); 
    lp4.setConnected(false);
    lp4.setMarker(new CrossMarker(5));
    for (int i = 1; i < 100; i++)
      lp4.addData(null,i,(float) random.nextGaussian());
    p4.addDrawShape(lp4);
    p4.addFillShape(new XAxis("X-Axis",2));
    p4.addFillShape(new YAxis("Y-Axis",2));
    desktop.add(makeInternalFrame(p4,"Scatter with Axes",Color.white,10,120));


    // Overlayed Scatter Plot.
    PlotSpace p5 = new PlotSpace(-10,10,-10,10);

    NativeLinePlot lp5a = new NativeLinePlot(); 
    lp5a.setConnected(false);
    lp5a.setMarker(new CrossMarker(5));
    for (int i = 1; i < 120; i++)
      lp5a.addData(null,(float) random.nextGaussian() * 2 - 4,(float) random.nextGaussian());

    NativeLinePlot lp5b = new NativeLinePlot(); 
    lp5b.setConnected(false);
    lp5b.setMarker(new CircleMarker(5));
    for (int i = 1; i < 60; i++)
      lp5b.addData(null,(float) random.nextGaussian() + 5,(float) random.nextGaussian() + 5);

    NativeLinePlot lp5c = new NativeLinePlot(); 
    lp5c.setConnected(false);
    lp5c.setMarker(new SquareMarker(5));
    for (int i = 1; i < 40; i++)
      lp5c.addData(null,(float) random.nextGaussian() * 4 + 1,(float) random.nextGaussian() * 4 + 3);

    p5.addDrawShape(lp5a);
    p5.addDrawShape(lp5b);
    p5.addDrawShape(lp5c);
    p5.addFillShape(new XAxis("X-Axis",2));
    p5.addFillShape(new YAxis("Y-Axis",2));
    desktop.add(makeInternalFrame(p5,"Overlayed Scatter with Axes",Color.white,220,120));


    // Multiple Axes.
    Box box6 = new MultivariatePlot();
    PlotSpace p6a = new PlotSpace(-10,110,-6,6);
    PlotSpace p6b = new PlotSpace(-10,110,10,20);

    NativeLinePlot lp6a = new NativeLinePlot(); 
    float last = 0;
    for (int i = 1; i < 100; i++)
    {
      float next = last + (float) random.nextGaussian() * 0.5f;
      lp6a.addData(null,i,next);
      last = next;
    } 
    p6a.addDrawShape(lp6a);
    YAxis p6aYAxis = new YAxis("Y-Axis Left",2);
    p6a.addFillShape(p6aYAxis);

    NativeLinePlot lp6b = new NativeLinePlot(); 
    p6a.addFillShape(new YAxis("Y-Axis Left",2));
    for (int i = 1; i < 100; i++)
      lp6b.addData(null,i,(float) random.nextGaussian() * 2 + 15);
    p6b.addDrawShape(lp6b);
    YAxis p6bYAxis = new YAxis("Y-Axis Right",2);
    p6bYAxis.setIsLeftAxis(false);
    p6b.addFillShape(p6bYAxis);
    p6b.addFillShape(new XAxis("X-Axis",2));

    JPanel panel6 = new Panel();
    panel6.add(p6a);
    panel6.add(p6b);
    box6.add(panel6);
    desktop.add(makeInternalFrame(box6,"Multiple Axes",Color.white,430,120));


    // Multivariate.
    Box box7 = new MultivariatePlot();
    PlotSpace p7a = new PlotSpace(-10,110,-3,3);
    PlotSpace p7b = new PlotSpace(-10,110,10,20);

    NativeLinePlot lp7a = new NativeLinePlot(); 
    for (int i = 1; i < 100; i++)
      lp7a.addData(null,i,(float) random.nextGaussian());
    p7a.addDrawShape(lp7a);
    YAxis p7aYAxis = new YAxis("Y-Axis 2",2);
    p7a.addFillShape(p7aYAxis);

    NativeLinePlot lp7b = new NativeLinePlot(); 
    for (int i = 1; i < 100; i++)
      lp7b.addData(null,i,(float) random.nextGaussian() * 2 + 15);
    p7b.addDrawShape(lp7b);
    YAxis p7bYAxis = new YAxis("Y-Axis 1",2);
    p7b.addFillShape(p7bYAxis);
    p7b.addFillShape(new XAxis("X-Axis",2));

    box7.add(p7a);
    box7.add(p7b);
    desktop.add(makeInternalFrame(box7,"Multivariate",Color.white,10,230));


    // Real-time.
    JPanel rtd = new RealTimeDemo();
    desktop.add(makeInternalFrame(rtd,"Realtime",Color.white,220,230));

    f.setVisible(true);
  }

  public static class RealTimeDemo extends JPanel implements ActionListener
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2851650910013454415L;
	protected Timer timer;
    protected java.util.Random random = new java.util.Random();
    PlotSpace p = new PlotSpace(0,10,-3,3);
    NativeLinePlot lp = new NativeLinePlot(); 
    GeneralPath gp = new GeneralPath();
    int count = 0;

    public RealTimeDemo()
    {
      super(new BorderLayout());
      setOpaque(true);
      setBackground(Color.white);   // This component is opaque so that it can be repainted below.

      p.addDrawShape(lp);
      //      p.addFillShape(new XAxis("Time",2));
      p.addFillShape(new XAxis("Time",2));
      p.addFillShape(new YAxis("Random exp(N(0,1))",2));
      add(p,BorderLayout.CENTER);

      timer = new Timer(250,this);
      timer.start();
    }

    public void actionPerformed(ActionEvent a)
    {
      count++; 
      updateGraph(count);
    }

    /** Called in the event-dispatching thread. So no need to use invokeLater(). */
    public void updateGraph(final int count)
    {
      float next = (float) Math.exp(random.nextGaussian());

      boolean needLayout = false;
      if (count > p.getXUpper())
      {
        p.setXUpper(count + 40);
        p.setXLower(count - 40);
        needLayout = true;
      }
      if (next > p.getYUpper()) 
      {
        p.setYUpper(next);
        needLayout = true;
      }
      if (next < p.getYLower()) 
      {
        p.setYLower(next);
        needLayout = true;
      }

      if (needLayout) 
      {
        p.doLayout();
        repaint();
      }
      else 
      {
        gp.reset();
        lp.addData(gp,count,next);
        p.drawSubShape(lp,gp);
        repaint();
      }
    }
  }

  public static class Panel extends JPanel implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8954586024471515061L;

	public Panel()
    {
      super(null);
      setOpaque(false);
    }

    public void doLayout()
    {
      for (int i = 0; i < getComponentCount(); i++)
        getComponent(i).setBounds(0,0,getWidth(),getHeight());
      super.doLayout();
    }
  }

  /** Consists of PlotSpaces, or JPanels with PlotSpaces in them (for multiple axes). 
      Used a Box because I was feeling lazy. */ 
  public static class MultivariatePlot extends Box implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6602093660873913014L;

	public MultivariatePlot()
    {
      super(BoxLayout.Y_AXIS);
    }

    public void doLayout()
    {
      super.doLayout();

      Reserved res = new Reserved(0,0,0,0);
      for (int i = 0; i < getComponentCount(); i++)
      {
        if (getComponent(i) instanceof PlotSpace)
        {
          PlotSpace p = (PlotSpace) getComponent(i);
          Reserved r  = p.getPreferredReserved();
          if (r != null) 
          {
            if (r.left > res.left) res.left = r.left;
            if (r.right > res.right) res.right = r.right;
          } 
        } 
        if (getComponent(i) instanceof JPanel)
        {
          JPanel panel = (JPanel) getComponent(i);
          for (int j = 0; j < panel.getComponentCount(); j++)
          {
            if (panel.getComponent(j) instanceof PlotSpace)
            {
              PlotSpace p = (PlotSpace) panel.getComponent(j);
              Reserved r = p.getPreferredReserved();
              if (r != null) res.createUnion(r);
            }
          } 

          // Update top and bottom insets.
          Reserved fullRes = new Reserved(res);
          fullRes.left = -1;
          fullRes.right = -1;
          for (int j = 0; j < panel.getComponentCount(); j++)
          {
            if (panel.getComponent(j) instanceof PlotSpace)
            {
              PlotSpace p = (PlotSpace) panel.getComponent(j);
              p.setReserved(fullRes);
            }
          } 
        }
      }

      res.top = -1;
      res.bottom = -1;
      for (int i = 0; i < getComponentCount(); i++)
      {
        if (getComponent(i) instanceof PlotSpace)
        {
          PlotSpace p = (PlotSpace) getComponent(i);
          p.setReserved(res);
          p.doLayout();
        }
        if (getComponent(i) instanceof JPanel)
        {
          JPanel panel = (JPanel) getComponent(i);
          for (int j = 0; j < panel.getComponentCount(); j++)
          {
            if (panel.getComponent(j) instanceof PlotSpace)
            {
              PlotSpace p = (PlotSpace) panel.getComponent(j);
              Reserved r = p.getReserved();
              if (r != null)
              {
                r.left = res.left;
                r.right = res.right;
              }
              p.setReserved(res);
              p.doLayout();
            }
          }
        }
      }

    }  
  }


  /** We use Graphics2D affine transformation to do all of the work.  Shapes are 
      added to the PlotSpace and are drawn in value coordinate space.  
  */
  public static class PlotSpace extends JComponent implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5960911383667381945L;
	protected float xLower;
    protected float xUpper;
    protected float yLower;
    protected float yUpper;
    Reserved reserved, preferredReserved;
    Reserved userReserved = new Reserved(-1,-1,-1,-1);

    private java.util.Vector shapeData = new java.util.Vector();

    public final Composite defaultComposite = AlphaComposite.SrcOver;
    public final Paint defaultPaint = Color.black;
    public final RenderingHints defaultRenderingHints; // init in constructor.

    // Default variables set in doLayout.
    BasicStroke defaultStroke = null;
    Rectangle2D pclip = null;
    Shape cclip = null;
    private boolean layed = false;
    private boolean modified = false;  // Modified indicates that the VolatileImage needs updating.

    public class ShapeData
    {
      public Shape shape;
      public Composite composite;
      public Paint paint;
      public Stroke stroke;
      public RenderingHints renderingHints;
      public boolean fill = true;
      public boolean draw = true;

      public ShapeData(Shape shape, Composite composite, Paint paint, Stroke stroke)
      {
        this.shape = shape;
        this.composite = composite;
        this.paint = paint;
        this.stroke = stroke;
        renderingHints = null;
      }
    }

    public AffineTransform aft;

    public PlotSpace(float xLower, float xUpper, float yLower, float yUpper)
    {
      super();
      setLayout(null);
      this.xLower = xLower;
      this.xUpper = xUpper;
      this.yLower = yLower;
      this.yUpper = yUpper;
      addMouseListener(new PlotSpaceMouseAdapter());

      defaultRenderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
						 RenderingHints.VALUE_ANTIALIAS_ON); 
						 //  RenderingHints.VALUE_ANTIALIAS_OFF ); 

      // VolatileImage supposedly does not work with antialiasing.
      defaultRenderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      setBackground(Color.white);
      setPreferredSize(new Dimension(200,150));
    } 


    public AffineTransform getTransform()
    {
      return aft;
    }

    /** Add a shape.  The shape will be drawn and filled using the default 
        redendering attributes. 
    */
    public void addShape(Shape shape)
    {
      addShape(shape,true,true);
    }

    /** Add a filled shape.  The shape will be rendered using the default redendering attributes. */
    public void addFillShape(Shape shape)
    {
      addShape(shape,false,true);
    }

    /** Add a drawn shape.  The shape will be rendered using the default redendering attributes. */
    public void addDrawShape(Shape shape)
    {
      addShape(shape,true,false);
    }

    /** Add a shape.  The shape will be rendered using the default redendering attributes. */
    public void addShape(Shape shape, boolean draw, boolean fill)
    {
      ShapeData sd = new ShapeData(shape,null,null,null);
      sd.draw = draw;
      sd.fill = fill;
      shapeData.add(sd);
      modified = true;
    }

    /** Add an outlined shape with its redendering attributes. */
    public void addDrawShape(Shape shape, Composite composite, Paint paint, Stroke stroke)
    {
      addShape(shape,true,false,composite,paint,stroke,null);
    }

    /** Add a filled shape with its redendering attributes. */
    public void addFillShape(Shape shape, Composite composite, Paint paint, Stroke stroke)
    {
      addShape(shape,false,true,composite,paint,stroke,null);
    }

    /** Add a shape with its redendering attributes. */
    public void addShape(Shape shape, boolean draw, boolean fill, 
                         Composite composite, Paint paint, Stroke stroke, RenderingHints rh)
    {
      ShapeData sd = new ShapeData(shape,composite,paint,stroke);
      sd.fill = fill;
      sd.draw = draw;
      sd.renderingHints = rh;
      shapeData.add(sd);
      modified = true;
    }

    /** Draws the subShape over the existing volatileImage. */
    public void drawSubShape(Shape shape, Shape subShape)
    {
      // Find the ShapeData.
      for (int i = 0; i < shapeData.size(); i++)
      {
        ShapeData sd = (ShapeData) shapeData.elementAt(i);
        if (sd.shape == shape)
        {
          drawSubShape(subShape,sd);
          return;
        }
      }
    }

    public void removeShape(Shape shape)
    {
      for (int i = 0; i < shapeData.size(); i++)
      {
        if ( ((ShapeData) shapeData.elementAt(i)).shape == shape)
        {
          removeShapeDataAt(i);
          modified = true;
          return;
        }
      }
    }   

    public void removeShapeDataAt(int i)
    {
      modified = true;
      shapeData.removeElementAt(i);
    }

    public int shapeDataSize()
    {
      return shapeData.size();
    }

    public ShapeData getShapeDataAt(int i)
    {
      return (ShapeData) shapeData.elementAt(i); 
    }

    public void setXLower(float xLower)
    {
      this.xLower = xLower;
      if (getParent() != null) doLayout();
    }

    public float getXLower()
    {
      return xLower;
    }

    public void setXUpper(float xUpper)
    {
      this.xUpper = xUpper;
      if (getParent() != null) doLayout();
    }

    public float getXUpper()
    {
      return xUpper;
    }

    public void setYLower(float yLower)
    {
      this.yLower = yLower;
      if (getParent() != null) doLayout();
    }

    public float getYLower()
    {
      return yLower;
    }

    public void setYUpper(float yUpper)
    {
      this.yUpper = yUpper;
      if (getParent() != null) doLayout();
    }

    public float getYUpper()
    {
      return yUpper;
    }

    public String toString()
    {
      return "Plot Space";
    }

    /** Returns the preferred amount of reserved space around the plot area.  
        May return null if doLayout has not been called. 
    */
    public Reserved getPreferredReserved()
    {
      return preferredReserved;
    }

    /** Returns the reserved space around the plot area.  */
    public Reserved getReserved()
    {
      return userReserved;
    }

    /** Sets the required reserved space around the plot area.
        If this method is used then automatic calculation of the 
        reserved space is disabled except for the fields of
        r that are negative. 
    */
    public void setReserved(Reserved r)
    {
      userReserved = r;
    }


    public void doLayout() 
    {
      if (getGraphics() == null) return;

      Insets insets = getInsets();
      int width = getWidth() - insets.left - insets.right;
      int height = getHeight() - insets.top - insets.bottom;

      // Calculate the bounds of the component in user-space units.
      preferredReserved = new Reserved(insets.top,insets.left,insets.bottom,insets.right);
      for (int i = 0; i < shapeData.size(); i++)
      {
        ShapeData shapeDatai = (ShapeData) shapeData.elementAt(i);
        if (shapeDatai.shape instanceof SuperShape)
        {
          Reserved r = ((SuperShape) shapeDatai.shape).getReserved(this);
          if (r != null) preferredReserved.createUnion(r);
        }
      }

      reserved = new Reserved(preferredReserved);

      if (userReserved.top >= 0) reserved.top = userReserved.top;
      if (userReserved.left >= 0) reserved.left = userReserved.left;
      if (userReserved.bottom >= 0) reserved.bottom = userReserved.bottom;
      if (userReserved.right >= 0) reserved.right = userReserved.right;

      float xRange = getXUpper() - getXLower();
      float yRange = getYUpper() - getYLower();

      float pppY = (height - reserved.top - reserved.bottom) / yRange; 
      float pppX = (width - reserved.left - reserved.right) / xRange; 

      aft = new AffineTransform(pppX,0,0,-pppY,insets.left + reserved.left - getXLower()*pppX,
                                getHeight() - insets.bottom - reserved.bottom + getYLower()*pppY);

      for (int i = 0; i < shapeData.size(); i++)
      {
        ShapeData shapeDatai = (ShapeData) shapeData.elementAt(i);
        if (shapeDatai.shape instanceof TransformDependantShape)
          ((TransformDependantShape) shapeDatai.shape).transformChanged(this);
      }

      // Create the default stroke (as a fraction of the width).
      defaultStroke = new BasicStroke((float) (getWidth() / 1000.0f));

      // Set pclip.
      cclip = getGraphics().getClip();
      Point2D bLPoint = aft.transform(new Point2D.Float(xLower,yLower),null);
      Point2D tRPoint = aft.transform(new Point2D.Float(xUpper,yUpper),null);
      pclip = new Rectangle2D.Double(
        bLPoint.getX(),tRPoint.getY(),
        tRPoint.getX() - bLPoint.getX(),bLPoint.getY() - tRPoint.getY());

      createImg();
      layed = true;
      modified = true;
    }

    public class PlotSpaceMouseAdapter extends MouseAdapter implements ActionListener
    {
      private Object selected;
      private JPopupMenu menu;
      private JMenuItem removeMenuItem = new JMenuItem("Remove",'R');
      private JMenuItem propertiesMenuItem = new JMenuItem("Properties",'P');
      private JMenuItem printMenuItem = new JMenuItem("Print");

      public PlotSpaceMouseAdapter()
      {
        super();
        menu = new JPopupMenu();
        JMenuItem removeMenuItem = new JMenuItem("Remove",'R');
        JMenuItem propertiesMenuItem = new JMenuItem("Properties",'P');
        removeMenuItem.addActionListener(this);
        propertiesMenuItem.addActionListener(this);
        printMenuItem.addActionListener(this);
        menu.add(removeMenuItem);
        menu.add(propertiesMenuItem);
        menu.add(printMenuItem);
      }

      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 1)
        {
          // Check if right click on Value.
          if ( (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK )
          {
            // Find which shape was selected.
            java.util.Vector contained = new java.util.Vector();
            for (int i = 0; i < shapeData.size(); i++)
            {
              Shape shapei = ((ShapeData) shapeData.elementAt(i)).shape;
              System.out.print("Testing " + shapei + " ... ");
            
              boolean hit = false; 
              if (shapei instanceof NonTransformedShape)
              {
                hit = shapei.intersects(e.getX()-2,e.getY()-2,4,4);
              }
              else 
              {
                try
                {
                  Point2D tPoint = aft.createInverse().transform(
                    new Point2D.Float(e.getX()-2,e.getY()-2),null);
                  hit = shapei.intersects(tPoint.getX(),tPoint.getY(),
                                           4.0/aft.getScaleX(),
                                           4.0/-aft.getScaleY());
                } 
                catch (Exception exception)
                {
                  ;
                }
              }

              if (hit)
              {
                System.out.println("intersects.");
                contained.add(shapei);
              } else System.out.println("failed.");
            }
            
            // For now just use first one in list.
            // Anyone want to implement code to allow selection?
            if (contained.size() >= 1)
            {
              selected = contained.elementAt(0);
              removeMenuItem.setEnabled(true);
              propertiesMenuItem.setEnabled(true);
              printMenuItem.setEnabled(true);
              menu.setLabel(selected.toString());
              menu.pack();
              menu.show(PlotSpace.this,e.getX(),e.getY());
            }
            else      // Plotspace propery editor.
            {
              selected = PlotSpace.this; 
              removeMenuItem.setEnabled(false);
              propertiesMenuItem.setEnabled(true);
              printMenuItem.setEnabled(true);
              menu.setLabel(selected.toString());
              menu.pack();
              menu.show(PlotSpace.this,e.getX(),e.getY());
            }
          }

        }
      }

      public void actionPerformed(ActionEvent e)
      {
                 // Remove
        if (e.getActionCommand().compareTo("Remove") == 0)
        {
          PlotSpace.this.removeShape((Shape) selected);
          PlotSpace.this.repaint();
        }
        else if (e.getActionCommand().compareTo("Print") == 0)
        {
          cdms.plugin.desktop.Desktop.PrintObj._apply(PlotSpace.this);
        }
        else     // Properties.
        {
          new cdms.plugin.bean.PropertySheet(selected);
        }
      }

    }


    public void setBoundsToPreferred()
    {
      float xL = Float.MAX_VALUE;
      float xU = - Float.MAX_VALUE;
      float yL = Float.MAX_VALUE;
      float yU = - Float.MAX_VALUE; 

      for (int i = 0; i < shapeData.size(); i++)
      {
        ShapeData shapei = (ShapeData) shapeData.elementAt(i);
        if (shapei.shape instanceof PlotBounds)
        {
          PlotBounds p = (PlotBounds) shapei.shape;
          if (p.getPreferredXUpper() > xU) xU = p.getPreferredXUpper();
          if (p.getPreferredXLower() < xL) xL = p.getPreferredXLower();
          if (p.getPreferredYUpper() > yU) yU = p.getPreferredYUpper();
          if (p.getPreferredYLower() < yL) yL = p.getPreferredYLower();
        }
      }
      if (xU != -Float.MAX_VALUE) setXUpper(xU);
      if (xL != Float.MAX_VALUE) setXLower(xL);
      if (yU != -Float.MAX_VALUE) setYUpper(yU);
      if (yL != Float.MAX_VALUE) setYLower(yL);
    }


    VolatileImage vImg = null;

    public void createImg()
    {
      GraphicsConfiguration gc = getGraphicsConfiguration();
      if (gc != null) vImg = gc.createCompatibleVolatileImage(getWidth(),getHeight());
        else vImg = null;
    }

//  Is this necessary?
/*    public Graphics getGraphics()
    {
      if (!RepaintManager.currentManager(this).isDoubleBufferingEnabled())
      {
        return super.getGraphics();
      }
      else
      {
        if (vImg == null) createImg();
        if (vImg == null) return null;
        if (vImg.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE)
          vImg = createVolatileImage(getWidth(), getHeight());
        return vImg.createGraphics();
      }
    }
*/

    public void paintChildren(Graphics g)
    {
      if (aft != null)
      {
        if (!RepaintManager.currentManager(this).isDoubleBufferingEnabled())
        {
          paintImg((Graphics2D) g); 
        }
        else
        {
          do 
          {
            if (vImg == null)
            { 
              createImg();
              if (vImg == null) return;
              paintImg();
            }
            else
            {
              int returnCode = vImg.validate(getGraphicsConfiguration());
              if (returnCode == VolatileImage.IMAGE_RESTORED) paintImg();
	        else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) paintImg();
                  else if (modified) paintImg();
            }
            g.drawImage(vImg, 0, 0, this);
          } while (vImg.contentsLost());
        }
        modified = false;
      }      
    }

    /* Override the default update method and skip unnecessary operations. */
    public void update(Graphics g)
    {
      paintChildren(g);
    }

    public void paintImg()
    {
      Graphics2D g2d = null;
      try
      {
        g2d = (Graphics2D) vImg.createGraphics();
        paintImg(g2d);
      }
      finally
      {
        if (g2d != null) g2d.dispose();
      }
    }


    /* Only draws to the volatile image it it exists and can be restored. 
       Otherwise does nothing. */
    public void drawSubShape(Shape shape, ShapeData sd)
    {
      if (layed)
      {
        if (!RepaintManager.currentManager(this).isDoubleBufferingEnabled())
        {
          ;  // Do nothing.
        }
        else
        {
          if (vImg != null)
          {
            int returnCode = vImg.validate(getGraphicsConfiguration());
            if (returnCode == VolatileImage.IMAGE_OK) 
            {
              Graphics2D g2d = null;

              try
              {
                g2d = (Graphics2D) vImg.createGraphics();
                drawShape(g2d,shape,sd);
              }
              finally
              {
                if (g2d != null) g2d.dispose();
              }
            }
          }
        }
      }      

    }

    /** Draws shape using the attributes in ShapeData. 
        Normally shape = sd.shape but this is not the case for 
        sub-shapes.
    */
    public void drawShape(Graphics2D g2d, Shape shape, ShapeData sd)
    {
      if (!layed) return;
      if (sd.composite != null) g2d.setComposite(sd.composite);
        else g2d.setComposite(defaultComposite);
      if (sd.paint != null) g2d.setPaint(sd.paint);
        else g2d.setPaint(defaultPaint);
      if (sd.stroke != null) g2d.setStroke(sd.stroke);
        else g2d.setStroke(defaultStroke);
      if (sd.renderingHints != null) g2d.setRenderingHints(sd.renderingHints);
        else g2d.setRenderingHints(defaultRenderingHints);
  
      if (shape instanceof SuperShape) g2d.setClip(cclip);
        else g2d.setClip(pclip);

      if (shape instanceof NonTransformedShape)
      {
        if (sd.draw) g2d.draw(shape);
        if (sd.fill) g2d.fill(shape);
      }
      else 
      {
        Shape tshapei = aft.createTransformedShape(shape);
        if (sd.draw) g2d.draw(tshapei);
        if (sd.fill) g2d.fill(tshapei);
      }
    }

    public void paintImg(Graphics2D g2d)
    {
      g2d.setColor(getBackground());
      g2d.fillRect(0,0,getWidth(),getHeight());

      //super.paintChildren(g2d);

      if (!layed) return;

      for (int i = 0; i < shapeData.size(); i++)
      {
        ShapeData shapeDatai = (ShapeData) shapeData.elementAt(i);
        drawShape(g2d, shapeDatai.shape,shapeDatai);
      }
    }

  }

  public static class Reserved implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5856606798915963142L;
	float top, left, bottom, right;

    public Reserved(float top, float left, float bottom, float right)
    {
      this.top = top;
      this.left = left;
      this.bottom = bottom;
      this.right = right;
    }

    public Reserved(Reserved r)
    {
      top = r.top;
      left = r.left;
      bottom = r.bottom;
      right = r.right;
    }

    public void createUnion(Reserved r)
    {
      if (r == null) return;
      if (r.top > top) top = r.top;
      if (r.left > left) left = r.left;
      if (r.bottom > bottom) bottom = r.bottom;
      if (r.right > right) right = r.right;
    }
  }

  /** SuperShapes are not clipped and will be made entirely visible.  They can
      also reserve space outside the plotting region.  */
  public static interface SuperShape
  {
    /** The required space outside the plotting region. */
    public Reserved getReserved(PlotSpace ps);
  }

  public static interface TransformDependantShape
  {
    /** PlotSpace calls this method to notify the Shape that the 
        affine transformation has changed. */
    public void transformChanged(PlotSpace ps);
  }

  /** NonTransformedShapes are not transformed. */
  public static interface NonTransformedShape extends TransformDependantShape
  {
  }

  public static interface PlotBounds
  {
    public float getPreferredXUpper();
    public float getPreferredXLower();
    public float getPreferredYUpper();
    public float getPreferredYLower();
  }


  /** The GeneralPath class is final so we create a wrapper. */
  public static class FreeGeneralPath implements Shape
  {
    GeneralPath gp = new GeneralPath();

    public void moveTo(Point2D point)
    {
      moveTo((float)point.getX(),(float)point.getY());
    }

    public void moveTo(double x, double y)
    {
      gp.moveTo((float) x,(float) y);
    }

    public void lineTo(double x, double y)
    {
      gp.lineTo((float) x,(float) y);
    }

    public void moveTo(float x, float y)
    {
      gp.moveTo(x,y);
    }

    public void lineTo(float x, float y)
    {
      gp.lineTo(x,y);
    }

    public void quadTo(float x1, float y1, float x2, float y2)
    {
      gp.quadTo(x1,y1,x2,y2);
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
    {
      gp.curveTo(x1,y1,x2,y2,x3,y3);
    }

    public void append(Shape s, boolean connect)
    {
      gp.append(s,connect);
    }

    public void reset()
    {
      gp = new GeneralPath();
    }

    public boolean contains(double x, double y)
    {
      return gp.contains(x,y);
    }

    public boolean contains(double x, double y, double w, double h)
    { 
      return gp.contains(x,y,w,h); 
    }

    public boolean contains(Point2D p)
    {
      return gp.contains(p);
    }

    public boolean contains(Rectangle2D r)
    {
      return gp.contains(r);
    }

    public Rectangle getBounds()
    {
      return gp.getBounds();
    }

    public Rectangle2D getBounds2D()
    {
      return gp.getBounds2D();
    }

    public PathIterator getPathIterator(AffineTransform at)
    { 
      return gp.getPathIterator(at); 
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness)
    { 
      return gp.getPathIterator(at,flatness); 
    }

    public boolean intersects(double x, double y, double w, double h)
    { 
      return gp.intersects(x,y,w,h); 
    }

    public boolean intersects(Rectangle2D r) 
    {
      return gp.intersects(r);
    }
  }


  public static abstract class Axis extends FreeGeneralPath implements 
    SuperShape, NonTransformedShape, Serializable
  {
    /** The font height as a percentage of Min(graph height, graph width). */
    protected float FONT_PERC = 0.05f;

    /** The minimum font height. */
    protected float MIN_FONT_HEIGHT = 4f; 

    /** The maximum font height. */
    protected float MAX_FONT_HEIGHT = 12f; 

    /** The gap between labels, etc., as a percentage of the font height. */
    protected float GAP_PERC = 0.6f;

    /** labelDepth is the number of divisions to make for the numeric labels (space permitting). */
    protected int labelDepth = 2;   

    protected String label;

    public Axis(String label)
    {
      this.label = label;
    }

    public void setLabel(String s)
    {
      label = s;
    }

    public String getLabel()
    {
      return label;
    }

    public void setLabelDepth(int ld)
    {
      labelDepth = ld;
    }

    public int getLabelDepth()
    {
      return labelDepth;
    }


    public void setMinimumFontHeight(float fh)
    {
      MIN_FONT_HEIGHT = fh;
    }

    public float getMinimumFontHeight()
    {
      return MIN_FONT_HEIGHT;
    }

    public void setMaximumFontHeight(float fh)
    {
      MAX_FONT_HEIGHT = fh;
    }

    public float getMaximumFontHeight()
    {
      return MAX_FONT_HEIGHT;
    }
  }


  /**  An upper or lower X-Axis.  Defaults to lower. */
  public static class XAxis extends Axis
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5742191088329270218L;
	boolean bottomAxis = true;
    java.text.DecimalFormat valFormat = new java.text.DecimalFormat();

    GlyphVector labelGlyph, lLabelGlyph, rLabelGlyph, mLabelGlyph[];
    Rectangle2D labelBounds, lLabelBounds, rLabelBounds, mLabelBounds[];

    public XAxis(String label, int prec)
    {
      super(label);
      valFormat.setMaximumFractionDigits(prec);
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

      float xL = ps.getXLower();
      float xU = ps.getXUpper();

      // Create the text.
      Graphics2D g2d = (Graphics2D) ps.getGraphics();
      if (g2d == null) return null;
      Font f = g2d.getFont().deriveFont(fh);
      FontRenderContext frc = g2d.getFontRenderContext();

      // Lower bound label.
      String lLabel = valFormat.format(xL);
      lLabelGlyph = f.createGlyphVector(frc, lLabel);
      lLabelBounds = lLabelGlyph.getVisualBounds();

      // Upper bound label.
      String rLabel = valFormat.format(xU);
      rLabelGlyph = f.createGlyphVector(frc, rLabel);
      rLabelBounds = rLabelGlyph.getVisualBounds();

      // Middle labels.
      int numLabels = (int) Math.pow(2,labelDepth) - 1;
      mLabelGlyph = new GlyphVector[numLabels];
      mLabelBounds = new Rectangle2D[numLabels];
      double dx = (xU - xL) / (numLabels + 1.0);
      double cx = xL + dx;
      for (int i = 0; i < numLabels; i++)
      {
        String mLabel = valFormat.format(cx);
        mLabelGlyph[i] = f.createGlyphVector(frc, mLabel);
        mLabelBounds[i] = mLabelGlyph[i].getVisualBounds();
        cx += dx; 
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
      append(new Rectangle2D.Double(xLPoint.getX()-0.05*gap,xLPoint.getY(),0.1*gap,0.5*gap),false);
      append(lLabelShape,false);

      // Upper bound.
      Shape rLabelShape = rLabelGlyph.getOutline(
        (float) (xUPoint.getX() - rLabelBounds.getWidth()/2.0),
        (float) (xUPoint.getY() + fh + gap));
      append(new Rectangle2D.Double(xUPoint.getX()-0.05*gap,xUPoint.getY(),0.1*gap,0.5*gap),false);
      append(rLabelShape,false);

      // Middle labels.
      int numLabels = (int) Math.pow(2,labelDepth) - 1;
      double pdx = (xUPoint.getX() - xLPoint.getX()) / (numLabels + 1.0);
      double pcx = xLPoint.getX() + pdx;
      for (int i = 0; i < numLabels; i++)
      {
        if (mLabelBounds[i].getWidth() + 2*gap < pdx)
        {
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
  public static class YAxis extends Axis
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2828673851118385853L;
	protected boolean leftAxis = true;
    java.text.DecimalFormat valFormat = new java.text.DecimalFormat();

    GlyphVector labelGlyph, tLabelGlyph, bLabelGlyph, mLabelGlyph[];
    Rectangle2D labelBounds, tLabelBounds, bLabelBounds, mLabelBounds[];

    public YAxis(String label, int prec)
    {
      super(label);
      valFormat.setMaximumFractionDigits(prec);
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

      // Create the text.
      Graphics2D g2d = (Graphics2D) ps.getGraphics();
      if (g2d == null) return null;
      Font f = g2d.getFont().deriveFont(fh);
      FontRenderContext frc = g2d.getFontRenderContext();

      // Top label (yUpper).
      String tLabel = valFormat.format(yU);
      tLabelGlyph = f.createGlyphVector(frc, tLabel);
      tLabelBounds = tLabelGlyph.getVisualBounds();

      // Bottom label (yLower).
      String bLabel = valFormat.format(yL);
      bLabelGlyph = f.createGlyphVector(frc, bLabel);
      bLabelBounds = bLabelGlyph.getVisualBounds();

      // Middle labels.
      int numLabels = (int) Math.pow(2,labelDepth) - 1;
      mLabelGlyph = new GlyphVector[numLabels];
      mLabelBounds = new Rectangle2D[numLabels];
      double dy = (yU - yL) / (numLabels + 1.0);
      double cy = yL + dy;
      double maxMWidth = Math.max(tLabelBounds.getWidth(),bLabelBounds.getWidth());
      for (int i = 0; i < numLabels; i++)
      {
        String mLabel = valFormat.format(cy);
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
      int numLabels = (int) Math.pow(2,labelDepth) - 1;
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


  /** Simple line plot. */
  public static abstract class LinePlot extends FreeGeneralPath implements TransformDependantShape
  {
    GeneralPath linePath = new GeneralPath();
    GeneralPath markerPath = new GeneralPath();

    private boolean first = true;

    boolean connected = true;
    Shape marker, inverseMarker;
    float lastX, lastY;

    public abstract int size();
    public abstract float getX(int i);
    public abstract float getY(int i);

    public String toString()
    {
      return "Data Plot";
    }

    public void setConnected(boolean c)
    {
      if (connected != c)
      {
        connected = c;
        rebuild();
      }
    }

    public boolean getConnected()
    {
      return connected;
    }

    /** Sets the marker for each point.  Using markers slows rendering to a crawl in 
        the current implementation. */
    public void setMarker(Shape marker)
    {
      if (this.marker != marker)
      {
        this.marker = marker;
        rebuild();
      }
    }

    public Shape getMarker()
    {
      return marker;
    }

    public void rebuild()
    {
      reset();
      linePath.reset();
      first = true;
      markerPath.reset();
      for (int i = 0; i < size(); i++)
        addDataToPath(null,getX(i), getY(i));
    }

    public void transformChanged(PlotSpace ps)
    {
      reset();

      if (marker != null)
      {
        try
        {
          AffineTransform at = ps.aft.createInverse();
          AffineTransform scale = AffineTransform.getScaleInstance(at.getScaleX(),at.getScaleY());
          inverseMarker = scale.createTransformedShape(marker);
          rebuild();
        }
        catch (Exception e)
        {
          // silent.
        }
      }
      if (connected) append(linePath,false);
      append(markerPath,false);
    }

    private AffineTransform markerTransform = new AffineTransform();

    /* Adds the pair (x,y) to the line plot.  Appends the required shapes to 
       subShape if subShape is not null. */
    protected void addDataToPath(GeneralPath subShape, float x, float y)
    {
      if (connected)
      {
        if (first) 
        {
          linePath.moveTo(x,y);
        }
        else 
        {
          linePath.lineTo(x,y);
          if (subShape != null) 
          {
            subShape.moveTo(lastX,lastY); 
            subShape.lineTo(x,y); 
          }
        }
        first = false;
      }

      if (inverseMarker != null)
      {
        markerTransform.setToTranslation(x,y);	
        Shape transMarker = markerTransform.createTransformedShape(inverseMarker);
        markerPath.append(transMarker,false);
        if (subShape != null) subShape.append(transMarker,false);
      }
      lastX = x;
      lastY = y;
    }
  }


  public static class NativeLinePlot extends LinePlot
  {
    java.util.Vector data;

    public class Data
    {
      float x, y;

      public Data(float x, float y)
      {
        this.x = x;
        this.y = y;
      }
    }

    public NativeLinePlot()
    {
      data = new java.util.Vector();
    }

    public int size()
    {
      return data.size();
    }

    public float getX(int i)
    {
      return ((Data) data.elementAt(i)).x;
    }

    public float getY(int i)
    {
      return ((Data) data.elementAt(i)).y;
    }

    public void addData(GeneralPath subShape, float x, float y)
    {
      data.add(new Data(x,y));
      addDataToPath(subShape,x,y);
    }
  }

  public static class CrossMarker extends FreeGeneralPath
  {
    public CrossMarker(float size)
    {
      moveTo(-size,0);
      lineTo(size,0);
      moveTo(0,size);
      lineTo(0,-size);
    } 
  }

  public static class CircleMarker extends Ellipse2D.Float
  {
    public CircleMarker(float size)
    {
      super(-size/2.0f,-size/2.0f,size,size); 
    } 
  }

  public static class SquareMarker extends Rectangle2D.Float
  {
    public SquareMarker(float size)
    {
      super(-size/2.0f,-size/2.0f,size,size); 
    } 
  }
}

// End of file.
