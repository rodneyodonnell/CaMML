//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567


// File: Desktop.java
// Authors: {leighf}@csse.monash.edu.au

package cdms.plugin.desktop;

import cdms.core.*;

// import java.lang.reflect.*;
import java.awt.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
//import javax.print.attribute.*;
//import javax.print.attribute.standard.*;

/** Desktop module containing GUI components and functions. */
public class Desktop extends Module.StaticFunctionModule
{
  public static final DesktopFrame desktop = new DesktopFrame();

  public Desktop()
  {
    super("Desktop",Module.createStandardURL(Desktop.class),Desktop.class);
  }

  /** <code>Boolean -> ()</code> Sets the desktop window visible property. */ 
  public static final SetVisible setVisible = new SetVisible();

  /** <code>Boolean -> ()</code> 
      <p>
      Sets the desktop window visible property. 
  */ 
  public static class SetVisible extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8233812414894934145L;
	public static final Type.Function TT = new Type.Function(Type.BOOLEAN,Type.TRIV);

    public SetVisible()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.setVisible(((Value.Discrete) v).getDiscrete() == Value.TRUE.getDiscrete());
      return Value.TRIV;
    }
  } 

  /** <code>String -> ()</code> Sets the desktop window title. */ 
  public static final SetTitle setTitle = new SetTitle();

  /** <code>String -> ()</code> 
      <p>
      Sets the desktop window title. 
  */ 
  public static class SetTitle extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2924353964832614419L;
	public static final Type.Function TT = new Type.Function(Type.STRING,Type.TRIV);

    public SetTitle()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.setTitle(((Value.Str) v).getString());
      return Value.TRIV;
    }
  } 

  /** <code>() -> ()</code> Disposes the desktop window. */ 
  public static final Dispose dispose = new Dispose();

  /** <code>() -> ()</code> 
      <p>
      Disposes the desktop window. 
  */ 
  public static class Dispose extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1213444086535534942L;
	public static final Type.Function TT = new Type.Function(Type.TRIV,Type.TRIV);

    public Dispose()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.dispose();
      return Value.TRIV;
    }
  } 

  /** <code>Menu -> ()</code> Sets the main menu on the desktop window.  
      See {@link Menu#MENU} for a definition of type <code>Menu</code>.
   */ 
  public static final SetMainMenu setMainMenu = new SetMainMenu();

  /** <code>Menu -> ()</code>
      <p>
      Sets the main menu on the desktop window.  
      See {@link Menu#MENU} for a definition of type <code>Menu</code>.
   */ 
  public static class SetMainMenu extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7455912277752707059L;
	public static final Type.Function TT = new Type.Function(Menu.MENU,Type.TRIV);

    public SetMainMenu()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.setMainMenu((Value.Vector) v); 
      return Value.TRIV;
    }
  } 

  /** <code>Toolbar -> ()</code> Sets the toolbar on the desktop window.  
      See {@link Menu#TOOLBAR} for a definition of type <code>Toolbar</code>.
   */ 
  public static final SetToolBar setToolBar = new SetToolBar();

  /** <code>Toolbar -> ()</code> 
      <p>
      Sets the toolbar on the desktop window.  
      See {@link Menu#TOOLBAR} for a definition of type <code>Toolbar</code>.
   */ 
  public static class SetToolBar extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1734700468734765737L;
	public static final Type.Function TT = new Type.Function(Menu.TOOLBAR,Type.TRIV);

    public SetToolBar()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.setToolbar((Value.Vector) v);
      return Value.TRIV;
    }
  } 

  /** <code>Menu -> ()</code> Sets the right-click popup environment menu on the desktop window.  
      See {@link Menu#MENU} for a definition of type <code>Menu</code>.
   */ 
  public static final SetEnvMenu setEnvMenu = new SetEnvMenu();

  /** <code>Menu -> ()</code> 
      <p>
      Sets the right-click popup environment menu on the desktop window.  
      See {@link Menu#MENU} for a definition of type <code>Menu</code>.
   */ 
  public static class SetEnvMenu extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6335175818320649181L;
	public static final Type.Function TT = new Type.Function(Menu.MENU,Type.TRIV);

    public SetEnvMenu()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.setEnvMenu((Value.Vector) v);
      return Value.TRIV;
    }
  } 

  /** <code>(t -> Obj(java.awt.Component)) -> ()</code> Sets the default value viewer for the
      desktop.  The viewer function is called when a user double clicks on a value in the
      environment tree.
  */
  public static final SetValueViewer setValueViewer = new SetValueViewer();

  /** <code>(t -> Obj(java.awt.Component)) -> ()</code> 
      <p>
      Sets the default value viewer for the desktop.  The viewer function is called when a 
      user double clicks on a value in the environment tree.
  */
  public static class SetValueViewer extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -353196088242723732L;
	public static final Type.Function TT = 
      new Type.Function(new Type.Function(new Type.Variable(),
        new Type.Obj(java.awt.Component.class.getName())),Type.TRIV);

    public SetValueViewer()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.setValueViewer((Value.Function) v);
      return Value.TRIV;
    }
  } 

  /** <code>(String,t) -> ()</code> Displays a value on the desktop with a given title.
      The first element of the pair is the title and the second is the value to display.
      If the value is sub-type of Obj(java.awt.Component) then it will be displayed, otherwise
      it will be passed to the default viewer ({@link #setValueViewer}) and displayed.
  */
  public static final Show show = new Show();

  /** <code>(String,t) -> ()</code> 
      <p>
      Displays a value on the desktop with a given title.
      The first element of the pair is the title and the second is the value to display.
      If the value is sub-type of Obj(java.awt.Component) then it will be displayed, otherwise
      it will be passed to the default viewer ({@link #setValueViewer}) and displayed.
  */
  public static class Show extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -861543540171563166L;
	public static final Type.Function TT = 
      new Type.Function(new Type.Structured(new Type[] { Type.STRING, new Type.Variable() },
                                            new String[] { "Title", "Value" }),Type.TRIV);

    public Show()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      Value.Structured sv = (Value.Structured) v;
      String title = ((Value.Str) sv.cmpnt(0)).getString();
      DesktopFrame.makeWindow(title,sv.cmpnt(1));
      return Value.TRIV;
    }
  } 

  /** <code>() -> ()</code> Arranges child windows on the desktop into columns. */
  public static final ArrangeColumns arrangeColumns = new ArrangeColumns();

  /** <code>() -> ()</code> 
      <p>
      Arranges child windows on the desktop into columns. 
  */
  public static class ArrangeColumns extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3463379430736443710L;
	public static final Type.Function TT = new Type.Function(Type.TRIV,Type.TRIV);

    public ArrangeColumns()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.arrangeColumns();
      return Value.TRIV;
    }
  } 

  /** <code>() -> ()</code> Arranges child windows on the desktop into rows. */
  public static final ArrangeRows arrangeRows = new ArrangeRows();

  /** <code>() -> ()</code>
      <p>
      Arranges child windows on the desktop into rows. 
  */
  public static class ArrangeRows extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7688809421807133685L;
	public static final Type.Function TT = new Type.Function(Type.TRIV,Type.TRIV);

    public ArrangeRows()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      desktop.arrangeRows();
      return Value.TRIV;
    }
  }


  /** <code>(String,t) -> Boolean</code> Sets the title of the child window that the
      value is being displayed in.  The first component of the pair is the 
      new title for the window, the second component is the value.  
      @result Returns true if the title could be set.
  */
  public static SetChildTitle setChildTitle = new SetChildTitle();

  /** <code>(String,t) -> Boolean</code> 
      <p>
      Sets the title of the child window that the
      value is being displayed in.  The first component of the pair is the 
      new title for the window, the second component is the value.
      @result Returns true if the title could be set.
  */
  public static class SetChildTitle extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1381164820314844252L;
	public static Type.Function thisType =
      new Type.Function(
        new Type.Structured(new Type[] { Type.STRING, new Type.Obj(Component.class.getName()) }),
                            Type.BOOLEAN);

    public SetChildTitle()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      Component o = (Component) ((Value.Obj) ((Value.Structured) v).cmpnt(1)).getObj();
      String title = ((Str) ((Value.Structured) v).cmpnt(0)).getString();

      Object p = o.getParent();
      if (p != null && p instanceof JInternalFrame)
      {
        ((JInternalFrame) p).setTitle(title);
        return Value.TRUE;
      }
      else if (p != null && p instanceof Frame)
      {
        ((Frame) p).setTitle(title);
        return Value.TRUE;
      }
      else
      {
        return Value.FALSE;
      }
    }

  }


  public static class PrintUtilities implements Printable 
  {
    private Component componentToBePrinted;
    private String title;

    public static void printComponent(Component c, String title) 
    {
      new PrintUtilities(c,title).print();
    }
  
    public PrintUtilities(Component componentToBePrinted, String title) 
    {
      this.title = title;
      this.componentToBePrinted = componentToBePrinted;
    }
  
    public void print() 
    {
	PrinterJob printJob = PrinterJob.getPrinterJob();	

	PageFormat pf_default = printJob.defaultPage();
	PageFormat pf = printJob.pageDialog(pf_default);
	if (pf == pf_default) return;   // Cancel clicked on pageDialog
	printJob.setPrintable(this,pf);
	printJob.setJobName(title);
	if (printJob.printDialog())
	    {
		try 
		    {
			printJob.print();
		    } 
		catch(PrinterException pe) 
		    {
			System.out.println("Error printing: " + pe);
		    }
	    }
    }


    public int print(Graphics g, PageFormat pageFormat, int pageIndex) 
    {
      if (pageIndex > 0) 
      {
        return(NO_SUCH_PAGE);
      } 
      else 
      {
        Graphics2D g2d = (Graphics2D)g;

        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        double scaleX = ((double)pageFormat.getImageableWidth())/
                       ((double)componentToBePrinted.getWidth());
        double scaleY = ((double)pageFormat.getImageableHeight())/
                       ((double)componentToBePrinted.getHeight());

        // Maintain aspect ratio.
        g2d.scale(Math.min(scaleX,scaleY), Math.min(scaleX,scaleY));

        disableDoubleBuffering(componentToBePrinted);
        componentToBePrinted.print(g2d);
        enableDoubleBuffering(componentToBePrinted);
        return(PAGE_EXISTS);
      }
    }

    public static void disableDoubleBuffering(Component c) 
    {
      RepaintManager currentManager = RepaintManager.currentManager(c);
      currentManager.setDoubleBufferingEnabled(false);
    }

    public static void enableDoubleBuffering(Component c) 
    {
      RepaintManager currentManager = RepaintManager.currentManager(c);
      currentManager.setDoubleBufferingEnabled(true);
    }
  }


  /** <code>Obj(java.awt.Component) -> Boolean</code> A function for printing an Obj 
      which is a descendant of java.awt.Component.  A print dialog is raised. 
  */
  public static final PrintObj printobj = new PrintObj();

  /** <code>Obj(java.awt.Component) -> Boolean</code>
      <p>
      A function for printing an Obj which is a descendant of java.awt.Component.  
      A print dialog is raised. 
  */
  public static class PrintObj extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4787725246830221810L;
	public static Type.Function thisType =
      new Type.Function(new Type.Obj(Component.class.getName()),Type.BOOLEAN);

    public PrintObj()
    {
      super(thisType);
    }

    public static boolean _apply(Component c)
    {
      try
      {
        Component cf = cdms.plugin.desktop.Desktop.desktop;
        while (cf !=null && !(cf instanceof Frame || cf instanceof JInternalFrame)) 
          cf = cf.getParent();
        String title = "";
        if (cf instanceof Frame) title = ((Frame) cf).getTitle();
          else if (cf instanceof JInternalFrame) title = ((JInternalFrame) cf).getTitle();

        cf = cdms.plugin.desktop.Desktop.desktop;
        while (cf != null && !(cf instanceof Frame)) cf = cf.getParent();

        if (cf == null)
        {
          cf = cdms.plugin.desktop.Desktop.desktop.getParent();
          while (cf != null && !(cf instanceof Frame)) cf = cf.getParent();
        }

        PrintUtilities.printComponent(c, title);
      }
      catch (Throwable t)
      {
        t.printStackTrace();
        return false;
      }
      return true;
    }

    public Value apply(Value v)
    {

      Component c = (Component) ((Value.Obj) v).getObj();
      if (_apply(c)) return Value.TRUE;
        else return Value.FALSE;
    }
  }


  /** <code>String -> Obj(java.awt.Component)</code> Returns a component displaying help
      for the specified module.
  */
  public static HelpM helpm = new HelpM();

  /** <code>String -> Obj(java.awt.Component)</code> 
      <p>
      Returns a component displaying help for the specified module.
  */
  public static class HelpM extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4302635333072153102L;
	public static Type.Function thisType = new Type.Function(Type.STRING, Type.OBJECT);
  
    public HelpM()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      // Lookup the module.
      Module m = Module.getModuleByName(((Value.Str) v).getString());

      if (m == null)
      {   
        Component c = new ViewHTML.ScrollHTMLPane("Module not found.");
        return new Value.Obj(c);
      }
      else
      {
        Component c;
        if (m.getHelp() == null)
          c = new ViewHTML.ScrollHTMLPane("Module does not have a valid help URL.");
        else c = new ViewHTML.ScrollHTMLPane(m.getHelp());
        return new Value.Obj(c);
      }
    }
  }


  /** <code>t -> Obj(java.awt.Component)</code> Returns a component displaying help
      for the specified value.
  */
  public static Help help = new Help();

  /** <code>t -> Obj(java.awt.Component)</code> 
      <p>
      Returns a component displaying help for the specified value.
  */
  public static class Help extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5438426394808573952L;
	public static Type.Function thisType = new Type.Function(Type.TYPE, Type.OBJECT);

    public Help()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      java.net.URL url = v.getHelp();
      Component c;
      if (url == null)
        c = new ViewHTML.ScrollHTMLPane("The value does not have a valid help URL");
      else c = new ViewHTML.ScrollHTMLPane(url);
      return new Value.Obj(c);
    }
  }


  /** <code>String -> Obj(java.awt.Component)</code> Returns a component displaying the
      specified HTML.
  */
  public static ViewHTML viewhtml = new ViewHTML();

  /** <code>String -> Obj(java.awt.Component)</code> 
      <p>
      Returns a component displaying the specified HTML.
  */
  public static class ViewHTML extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3570390638274289754L;
	public static Type.Function thisType = new Type.Function(Type.STRING, Type.OBJECT);

    public ViewHTML()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      return new Value.Obj(new ScrollHTMLPane(((Value.Str) v).getString()));
    }

    public static class ScrollHTMLPane extends JScrollPane
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -1569944602265564863L;

	public ScrollHTMLPane(java.net.URL url)
      {
        JEditorPane ep;
        try
        {
          ep = new JEditorPane(url);
        }
        catch (Exception e)
        {
          ep = new JEditorPane("text/html",e.toString());
        }
        ep.setEditable(false);
        ep.addHyperlinkListener(new Hyperactive());
        setPreferredSize(new Dimension(100000,100000));
        setViewportView(ep);
      }

      public ScrollHTMLPane(String str)
      {
        JEditorPane ep = new JEditorPane();
        ep.setContentType("text/html");
        ep.setEditable(false);
        ep.addHyperlinkListener(new Hyperactive());
        ep.setText(str);
        setPreferredSize(new Dimension(100000,100000));
        setViewportView(ep);
      }

      public static class Hyperactive implements HyperlinkListener
      {
        public void hyperlinkUpdate(HyperlinkEvent e)
        {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
          {
            JEditorPane pane = (JEditorPane) e.getSource();
            if (e instanceof HTMLFrameHyperlinkEvent)
            {
              HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
              HTMLDocument doc = (HTMLDocument)pane.getDocument();
              doc.processHTMLFrameHyperlinkEvent(evt);
            }
            else
            {
              try
              {
                pane.setPage(e.getURL());
              }
              catch (Throwable t)
              {
                t.printStackTrace();
              }
            }
          }
        }
      }
    }

  }


  public static VisualizerFN.DefaultVisualizer defaultVisualizer = new VisualizerFN.DefaultVisualizer();
  public static VisualizerFN.GaussianVisualizer gaussianVisualizer = new VisualizerFN.GaussianVisualizer();
  public static VisualizerFN.MultistateVisualizer multistateVisualizer = new VisualizerFN.MultistateVisualizer();
}
