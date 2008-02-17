package cdms.plugin.desktop; 

import cdms.core.*;
import java.awt.*;
import javax.swing.*;

public class VisualizerFN
{
    public static abstract class Function extends Value.Function
    {
	public Function(Type.Function t)
	{
	    super(t);
	}
  
	public abstract JComponent _apply(Value v);
    }

    public static class DefaultVisualizer extends Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -6938884231460576837L;

	public DefaultVisualizer()
	{
	    super(new Type.Function(Type.TYPE, new Type.Obj("cdms.plugin.desktop.Browser"), false, false));
	}
    
	public JComponent _apply(Value v)
	{
	    Browser browser = new Browser();
	    browser.setSubject(v);
		return browser;
	}
    
	public Value apply(Value v)
	{
	    return new Value.Obj(_apply(v));
	}
    }

    public static class GaussianVisualizer extends Value.Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -5373038206208369746L;
	public static Type.Function TT = new Type.Function(Type.SCALAR, GaussianVisualizer2.TT, false, false);
  
	public GaussianVisualizer()
	{
	    super(TT);
	}
    
	/** Use this method to avoid creating a series of curried functions - just get the JComponent using doubles instead of
	    functions and values.
	*/
	public JComponent _apply(double mn, double mx, double mean, double sd)
	{
	    return new GaussianVisualizer3.GaussianComponent(mn, mx, mean, sd);
	}
    
	public Value apply(Value v)
	{
	    return new GaussianVisualizer2(((Value.Scalar)v).getContinuous());
	}
    
	public String toString()
	{
	    return "Gaussian visualization function: min -> max -> (mean, sd) -> Obj";
	}
    }
  
    public static class GaussianVisualizer2 extends Value.Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -4422564334233886133L;
	public static Type.Function TT = new Type.Function(Type.SCALAR, GaussianVisualizer3.TT, false, false);
	private double min;
  
	public GaussianVisualizer2(double min)
	{
	    super(TT);
	    this.min = min;
	}
    
	public Value apply(Value v)
	{
	    return new GaussianVisualizer3(min, ((Value.Scalar)v).getContinuous());
	}
    
	public String toString()
	{
	    return "Gaussian visualization function: max -> (mean, sd) -> Obj";
	}
    }

    public static class GaussianVisualizer3 extends Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -8819583223654394855L;
	public static Type.Function TT = new Type.Function(
							   Type.STRUCTURED, 
							   new Type.Obj("cdms.plugin.desktop.VisualizerFN$GaussianVisualizer3$GaussianComponent"),
							   false, false);
	private double min;
	private double max;
  
	public GaussianVisualizer3(double min, double max)
	{
	    super(TT);
	    this.min = min;
	    this.max = max;
	}
    
	/** Use this method in place of apply(Value v) to return a JComponent instead of a JComponenet wrapped up in a Value.Obj. */
	public JComponent _apply(Value v)
	{
	    return new GaussianComponent(min, max, v);
	}
    
	/** Use this method to avoid wrapping mean and sd up in a structured value. */
	public JComponent _apply(double mean, double sd)
	{
	    return new GaussianComponent(min, max, mean, sd);
	}
    
	public Value apply(Value v)
	{
      
	    return new Value.Obj(new GaussianComponent(min, max, v));
	}
    
	public String toString()
	{
	    return "Gaussian visualization function: (mean, sd) -> Obj";
	}
    
	public static class GaussianComponent extends JComponent
	{
	    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 8367879305237718526L;

		public GaussianComponent(double min, double max, double mean, double sd)
	    {
		this(min, max, new Value.DefStructured(new Value[]{new Value.Continuous(mean), new Value.Continuous(sd)}));
	    }
      
	    public GaussianComponent(double min, double max, Value params)
	    {
		setBackground(Color.white);
		java.text.DecimalFormat df = new java.text.DecimalFormat();
		df.setMaximumFractionDigits(3);
		double x;
		Value[] array = new Value[200];
		int count;
		for(count = 0; count < 200; count++)
		    {
			x = min + (count * 0.005 * (max - min));
			array[count] = new Value.DefStructured(new Value[]{new Value.Continuous(x), 
									   new Value.Continuous(java.lang.Math.exp(cdms.plugin.model.Normal.normal.logP(new Value.Continuous(x),params,null)))});
		    }
		JLabel l = new JLabel("m: " + df.format(((Value.Scalar)((Value.Structured)params).cmpnt(0)).getContinuous()) + ", sd: " + df.format(((Value.Scalar)((Value.Structured)params).cmpnt(1)).getContinuous()));
		l.setBackground(Color.white);
		Component comp = (Component)((Value.Obj)cdms.plugin.twodplot.TwoDPlot.ploto.apply(new VectorFN.FatVector(array))).getObj();
		comp.setBackground(Color.white);
		setLayout(new BorderLayout());
		add(l, BorderLayout.SOUTH);
		add(comp, BorderLayout.CENTER);      
	    }
      
	    public void paint(Graphics g)
	    {
		doLayout();
		paintChildren(g);
	    }
	}
    }
  
    public static class MultistateVisualizer extends Value.Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -3353946555648941209L;
	public static Type.Function TT = new Type.Function(Type.DISCRETE, MultistateVisualizer3.TT, false, false);
  
	public MultistateVisualizer()
	{
	    super(TT);
	}
    
	public Value apply(Value v)
	{
	    return new MultistateVisualizer3(((Value.Scalar)v).getDiscrete());
	}
    }
  
    public static class MultistateVisualizer3 extends Function
    {
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -2502721033686936219L;
	public static Type.Function TT = new Type.Function(Type.STRUCTURED, new Type.Obj("cdms.plugin.desktop.VisualizerFN$MultistateVisualizer3$MultistateComponent"), false, false);
	private int min;
    
	public MultistateVisualizer3(int min)
	{
	    super(TT);
	    this.min = min;
	}
    
	public JComponent _apply(Value v)
	{
	    java.text.DecimalFormat df = new java.text.DecimalFormat();
	    df.setMaximumFractionDigits(3);
	    Value.Structured ys = (Value.Structured)v;
	    double[] probs = new double[ys.length()];
	    String[] probStrings = new String[probs.length];
	    String[] labels = new String[probs.length];
	    double sum = 0;
	    int count;
	    for(count = 0; count < probs.length; count++)
		{
		    probs[count] = ys.doubleCmpnt(count);
		    sum += probs[count];
		}
	    for(count = 0; count < probs.length; count++)
		{
		    probs[count] /= sum;
		    probStrings[count] = df.format(probs[count]);
		    labels[count] = "Pr("+(min+count)+")";
		}
	    Type.Structured ts = (Type.Structured)v.t;
	    if(ts.labels != null)
		{
		    for(count = 0; count < probs.length; count++)
			{
			    if(ts.labels[count] != null)
				{
				    labels[count] = ts.labels[count];
				}
			}
		}

	    return new MultistateComponent(labels, probStrings, probs);    
	}
    
	public Value apply(Value v)
	{
	    return new Value.Obj(_apply(v));
	}
    
	public static class MultistateComponent extends JComponent
	{
	    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 2781802176650902779L;
		private String[] labels;
	    private String[] probStrings;
	    private double[] probs;
	    private int maxLabelLength = 0;
	    private int maxBar = 0;
	    private static Font font = new Font("Serif",Font.PLAIN,10);
	    private static java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, false, false);
	    private static int height = (int)font.getStringBounds("Get the height of This.1234567890", frc).getHeight();
      
	    public MultistateComponent(String[] labels, String[] probStrings, double[] probs)
	    {
		this.labels = labels;
		this.probStrings = probStrings;
		this.probs = probs;
	    }
    
	    public Dimension getPreferredSize()
	    {
		int count;
		for(count = 0; count < labels.length; count++)
		    {
			maxLabelLength = (int)java.lang.Math.max(maxLabelLength, font.getStringBounds(labels[count], frc).getWidth());
		    }
		maxBar = (int)font.getStringBounds("0.000", frc).getWidth();
		return new Dimension(20 + maxLabelLength + 2 * maxBar, 10 + (int)font.getStringBounds("0.000", frc).getHeight() * labels.length);
	    }
      
	    public void paint(Graphics g)
	    {
		getPreferredSize(); // - makes sure all the fields are set up properly.
		Graphics2D gr = (Graphics2D)g;
		gr.setFont(font);
		gr.setColor(Color.white);
		gr.fillRect(0,0,getWidth(),getHeight());
		int count;
		for(count = 0; count < labels.length; count++)
		    {
			gr.setColor(Color.gray);
			gr.fillRect(10 + maxLabelLength, 5 + count * height, maxBar, height -1);
			gr.setColor(Color.green);
			gr.fillRect(10 + maxLabelLength, 5 + count * height, (int)(probs[count] * maxBar), height -1);
		    }
		gr.setColor(Color.black);
		for(count = 0; count < labels.length; count++)
		    {
			gr.drawString(labels[count], 5, 5 + (int)((count+0.75) * height));
			gr.drawString(probStrings[count], getWidth() - 5 - maxBar, 5 + (int)((count+0.75) * height));
			gr.drawRect(10 + maxLabelLength, 5 + count * height, maxBar/*(int)(probs[count] * maxBar)*/, height -1);
		    }
		gr.drawRect(0,0,getWidth()-1,getHeight()-1);
	    }
	}
    }
}
