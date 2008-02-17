package cdms.plugin.go3dviz;

/**
 * Title:        GO3DVizPanel<p>
 * Package:      go3dviz<p>
 * Description:  The G3DVizPanel that implements the 3D Visualizer.<p>
 *               Written as part of the Core Data Mining System.<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;

public class GO3DVizPanel extends JPanel
{
/**
 * The Canvas3D that draws the visualized data.  Part of the Java3D
 * classes.
 */
  BranchGroup Scene;
  TransformGroup ObjTransform;
  Canvas3D DrawPanel;

/*
 * The panels/boxes that segment the Visualization window:
 */

/** 
 * ToolBar is a button box. It is the eastern panel that will contain
 * all the operations that can be performed on the data being visualized
 * in iconic button format.
 */
  Box ToolBar;

/**
 * NavigationBar is a super-panel. It contains 3 subpanels that
 * contain the RTTogglePanel, the NavPanel and the ZoomPanel.
 */
  JPanel NavigationBar;

/**
 * RTTogglePanel is really a box.  It contains a pair of mutually
 * exclusive buttons that set whether the buttons in the NavPanel
 * execute translations or rotations.
 */
  Box RTTogglePanel;
  ButtonGroup RTToggle;

/**
 * NavPanel contains the set of navigation buttons.  The functions
 * of these buttons are altered by the buttons in RTTogglePanel.
 */
  JPanel NavPanel;

/**
 * ZoomPanel is really a box.  It contains two buttons which allow
 * the user to zoom in or out of the page.
 */
  Box ZoomPanel;

/** 
 * Menu variables.  Fairly self-explanatory.  These menu objects
 * will appear in the parent programs menu bar.
 */
  //  JMenu Fields, Sampling, Selection;

/**
 * The icons that will appear on each button in the various toolbars.
 */
  ImageIcon I_Home, I_Select, I_Label, I_Axes, I_Sample, I_Type, 
            I_Rotate, I_Translate,
            I_ZIn, I_ZOut,
            I_Up, I_Down, I_Left, I_Right, I_In, I_Out;

/**
 * The buttons that will control functions as an alternatives to
 * the menu bar.
 */
  JButton   B_Home, B_Select, B_Label, B_Axes, B_Sample, B_Type,
            B_ZIn, B_ZOut,
            B_Up, B_Down, B_Left, B_Right, B_In, B_Out;

  JToggleButton B_Rotate, B_Translate;

/**
 * The Translation Listeners for the buttons.
 */
  class TranslationListener implements ActionListener
  {
    private float t1, t2, t3;

    TranslationListener(float f1, float f2, float f3)
    {  this.t1 = f1; this.t2 = f2; this.t3 = f3;  }

    public void actionPerformed(ActionEvent E)
    {
      Matrix3f rot = new Matrix3f();
      Vector3f trans = new Vector3f();
      Vector3f moveby = new Vector3f(this.t1, this.t2, this.t3);
      Transform3D T = new Transform3D();

      ObjTransform.getTransform(T);
      T.get(rot, trans);
      trans.add(moveby);
      T.setTranslation(trans);
      ObjTransform.setTransform(T);
    }
  }

/**
 * The Rotation Listeners for the buttons.
 */
  class XRotationListener implements ActionListener
  {
    boolean AntiClock;

    XRotationListener(boolean AC)
    { AntiClock = AC; }

    public void actionPerformed(ActionEvent E)
    {
      float beta = (float) Math.PI / 6.0f;
      if (AntiClock) { beta = -beta; }
      Matrix3f rot = new Matrix3f();
      Vector3f trans = new Vector3f();
      Matrix3f rotby = new Matrix3f(1.0f, 0.0f                   , 0.0f,
				    0.0f, (float)  Math.cos(beta), (float) Math.sin(beta),
				    0.0f, (float) -Math.sin(beta), (float) Math.cos(beta));
      Transform3D T = new Transform3D();

      ObjTransform.getTransform(T);
      T.get(rot, trans);
      rotby.mul(rot);
      T.setRotation(rotby);
      ObjTransform.setTransform(T);
    }
  }
  class YRotationListener implements ActionListener
  {
    boolean AntiClock;

    YRotationListener(boolean AC)
    { AntiClock = AC; }

    public void actionPerformed(ActionEvent E)
    {
      float beta = (float) Math.PI / 6.0f;
      if (AntiClock) { beta = -beta; }
      Matrix3f rot = new Matrix3f();
      Vector3f trans = new Vector3f();
      Matrix3f rotby = new Matrix3f((float) Math.cos(beta) , 0.0f, (float) Math.sin(beta),
				    0.0f                   , 1.0f, 0.0f,
				    (float) -Math.sin(beta), 0.0f, (float) Math.cos(beta));
      Transform3D T = new Transform3D();

      ObjTransform.getTransform(T);
      T.get(rot, trans);
      rotby.mul(rot);
      T.setRotation(rotby);
      ObjTransform.setTransform(T);
    }
  }

  class ZRotationListener implements ActionListener
  {
    boolean AntiClock;

    ZRotationListener(boolean AC)
    { AntiClock = AC; }

    public void actionPerformed(ActionEvent E)
    {
      float beta = (float) Math.PI / 6.0f;
      if (AntiClock) { beta = -beta; }
      Matrix3f rot = new Matrix3f();
      Vector3f trans = new Vector3f();
      Matrix3f rotby = new Matrix3f((float) Math.cos(beta) , (float) Math.sin(beta), 0.0f,
                                    (float) -Math.sin(beta), (float) Math.cos(beta), 0.0f,
                                    0.0f                   , 0.0f                  , 1.0f);
      Transform3D T = new Transform3D();

      ObjTransform.getTransform(T);
      T.get(rot, trans);
      rotby.mul(rot);
      T.setRotation(rotby);
      ObjTransform.setTransform(T);
    }
  }

/**
 * Instances of the Movement Listeners for the buttons.
 */
  TranslationListener TL_Up, TL_Down, TL_Left, TL_Right, TL_In, TL_Out;
  XRotationListener RL_Up, RL_Down;
  YRotationListener RL_Left, RL_Right;
  ZRotationListener RL_In, RL_Out;

/**
 * A vector containing the data-space axes and the translations
 * between them and 3D-space.  They contain Type information as
 * well as the values necessary to convert a (possibly infinite)
 * "physical" dimension into a data dimension and vice versa. For
 * a full explanation of this class go see its own Javadoc. 
 */
  GO3DAxis Axes[];

/**
 * A vector of the data objects.  These are of the GO3DSubObj
 * class and contain -all- the data selected by the user (including
 * undisplayed data) and the function enabling them to be displayed
 * given a set of axes.  For more information see the classes own
 * Javadoc.  Note that only the first space in the array is used at
 * the moment.  It is an array to allow extension to multiple SOs at
 * some stage.
 */
  GO3DSubObj Objects[];

/**
 * A double array of Java Floats that contains -all- the data passed
 * to the VizPanel.
 */
  Float myData[][];

/**
 * Integers to store the size of the data array.
 */
  int Rows, Cols;

/**
 * An array of 6 integers that are indices into the data array.  They
 * are the columns selected to be along the X, Y, Z, R, G and B axes
 * respectively.
 */
  int Selections[];

/**
 * A default constructor that creates a set of data points with 10
 * attributes with random values between [0.0, 1.0).  The initial
 * selection of axes defaults to the first 6 columns of data.
 */
  public GO3DVizPanel()
  { 
    createRandomData();
    initSelections(10);
    initJComponents();
  }

/**
 * The most used constructor that takes a double array of data points
 * and the number of rows and columns the data has.  The initial
 * selection of axes defaults to the first 6 columns of data.  If less
 * than 6 columns are available then the remainder are set to the
 * default value for that axis.
 */
  public GO3DVizPanel(Float Data[][], int rows, int cols)
  {
    assignData(Data, rows, cols);
    initSelections(cols);
    initJComponents();
  }

/**
 * An internal function that creates random data in 10 columns with
 * 100,000 rows.  All data values are in the range [0.0, 1.0) using
 * Java's default pseudo-random number generator.
 */
  private void createRandomData()
  {
    Rows = 100000;
    Cols = 10;

    myData = new Float[Rows][Cols];

    for (int j = 0; j < Cols; j++)
      for (int i = 0; i < Rows; i++)
	myData[i][j] = new Float(Math.random());
  }

/**
 * Unsure as to whether this function needs to exist.  It copies
 * the externally passed data values into the internal array.
 */
  private void assignData(Float Data[][], int rows, int cols)
  {
    Rows = rows;
    Cols = cols;

    myData = new Float[Rows][Cols];

    for (int j = 0; j < Cols; j++)
      for (int i = 0; i < Rows; i++)
	myData[i][j] = Data[i][j];
  }

/**
 * Initialises the axes to represent the first 6 columns of data
 * available.  If there are less than 6 columns available the
 * remaining axes are assigned their default values.
 */
  private void initSelections(int cols)
  {
    Selections = new int[6];
    for (int i = 0; i < 6; i++)
      if (i < cols) {Selections[i] = i;} else { Selections[i] = -1; }
  }

/**
 * Internal function that controls the initialisation of the Swing
 * components and assembles them in the main GO3DVizPanel.
 */
  private void initJComponents()
  {
    this.setMinimumSize(new Dimension(300,400));
    this.setPreferredSize(new Dimension(600, 800));
    this.initIcons();
    this.initButtons();
    this.initPanels();

    GridBagLayout L = new GridBagLayout();
    GridBagConstraints C = new GridBagConstraints();
    this.setLayout(L);

    C.gridx = 10;
    C.gridy = 0;
    C.gridwidth = 1;
    C.gridheight= 10;
    C.weightx = 0.0;
    C.weighty = 0.0;
    L.setConstraints(ToolBar, C);
    this.add(ToolBar);

    C.gridx = 0;
    C.gridy = 10;
    C.gridwidth = 10;
    C.gridheight = 1;
    C.weightx = 0.0;
    C.weighty = 0.0;
    L.setConstraints(NavigationBar, C);
    this.add(NavigationBar);

    C.anchor = GridBagConstraints.NORTHWEST;
    C.fill = GridBagConstraints.BOTH;
    C.gridx = 0;
    C.gridy = 0;
    C.gridwidth = 10;
    C.gridheight = 10;
    C.weightx = 10.0;
    C.weighty = 10.0;
    L.setConstraints(DrawPanel, C);
    this.add(DrawPanel);
  }

/**
 * Internal function that initialises the icons for the buttons.
 */
  private void initIcons()
  {
    I_Home      = new ImageIcon("./images/Home.gif");
    I_Select    = new ImageIcon("./images/Select.gif");
    I_Label     = new ImageIcon("./images/Label.gif");
    I_Axes      = new ImageIcon("./images/Axes.gif");
    I_Sample    = new ImageIcon("./images/Sample.gif");
    I_Type      = new ImageIcon("./images/Type.gif");
    I_Rotate    = new ImageIcon("./images/Rotate.gif");
    I_Translate = new ImageIcon("./images/Translate.gif");
    I_ZIn       = new ImageIcon("./images/Z_In.gif");
    I_ZOut      = new ImageIcon("./images/Z_Out.gif");
    I_Up        = new ImageIcon("./images/Up.gif");
    I_Down      = new ImageIcon("./images/Down.gif");
    I_Left      = new ImageIcon("./images/Left.gif");
    I_Right     = new ImageIcon("./images/Right.gif");
    I_In        = new ImageIcon("./images/In.gif");
    I_Out       = new ImageIcon("./images/Out.gif");
  }

/**
 * Internal function that initialises the buttons, tooltexts and
 * disables the unimplemented ones.
 */
  private void initButtons()
  {
    B_Home      = new JButton(I_Home);
    B_Select    = new JButton(I_Select);
    B_Label     = new JButton(I_Label);
    B_Axes      = new JButton(I_Axes);
    B_Sample    = new JButton(I_Sample);
    B_Type      = new JButton(I_Type);
    B_Rotate    = new JToggleButton(I_Rotate, false);
    B_Translate = new JToggleButton(I_Translate, true);
    B_ZIn       = new JButton(I_ZIn);
    B_ZOut      = new JButton(I_ZOut);
    B_Up        = new JButton(I_Up);
    B_Down      = new JButton(I_Down);
    B_Left      = new JButton(I_Left);
    B_Right     = new JButton(I_Right);
    B_In        = new JButton(I_In);
    B_Out       = new JButton(I_Out);

    B_Home.setToolTipText("Move to Home View");
    B_Select.setToolTipText("Select Mode");
    B_Axes.setToolTipText("Alter Axis Attributes");
    B_Sample.setToolTipText("Alter Sample Rate");
    B_Type.setToolTipText("Alter Point Type");
    B_Rotate.setToolTipText("Rotate Mode");
    B_Translate.setToolTipText("Translate Mode");
    B_ZIn.setToolTipText("Zoom In");
    B_ZOut.setToolTipText("Zoom Out");

    B_Select.setEnabled(false);
    B_Label.setEnabled(false);
    B_Type.setEnabled(false);
    B_ZIn.setEnabled(false);
    B_ZOut.setEnabled(false);

    this.initListeners();
  }

/**
 * Internal function that initialises (and defines some of) the
 * listener functions for the various buttons on the panel.
 */
  private void initListeners()
  {
    RL_Up    = new XRotationListener(true);  /* true  == anti-clockwise */
    RL_Down  = new XRotationListener(false); /* false == clockwise      */
    RL_Right = new YRotationListener(false);
    RL_Left  = new YRotationListener(true);
    RL_In    = new ZRotationListener(true);
    RL_Out   = new ZRotationListener(false);

    TL_Up    = new TranslationListener(0.0f, 0.1f, 0.0f);
    TL_Down  = new TranslationListener(0.0f, -0.1f, 0.0f);
    TL_Right = new TranslationListener(0.1f, 0.0f, 0.0f);
    TL_Left  = new TranslationListener(-0.1f, 0.0f, 0.0f);
    TL_In    = new TranslationListener(0.0f, 0.0f, -0.1f);
    TL_Out   = new TranslationListener(0.0f, 0.0f, 0.1f);

    B_Up.addActionListener(TL_Up);
    B_Down.addActionListener(TL_Down);
    B_Right.addActionListener(TL_Right);
    B_Left.addActionListener(TL_Left);
    B_In.addActionListener(TL_In);
    B_Out.addActionListener(TL_Out);

  /* On choosing Rotate / Translate, junk all the current *
   * button listeners and add the opposite set.           *
   * (T for translate, R for rotate)                      */
    B_Rotate.addActionListener(new ActionListener()
      { 
	public void actionPerformed(ActionEvent E)
	{
	  B_Up.removeActionListener(TL_Up);
	  B_Down.removeActionListener(TL_Down);
	  B_Right.removeActionListener(TL_Right);
	  B_Left.removeActionListener(TL_Left);
	  B_In.removeActionListener(TL_In);
	  B_Out.removeActionListener(TL_Out);
	  B_Up.addActionListener(RL_Up);
	  B_Down.addActionListener(RL_Down);
	  B_Right.addActionListener(RL_Right);
	  B_Left.addActionListener(RL_Left);
	  B_In.addActionListener(RL_In);
	  B_Out.addActionListener(RL_Out);
	}
      });

    B_Translate.addActionListener(new ActionListener()
      { 
	public void actionPerformed(ActionEvent E)
	{
	  B_Up.removeActionListener(RL_Up);
	  B_Down.removeActionListener(RL_Down);
	  B_Right.removeActionListener(RL_Right);
	  B_Left.removeActionListener(RL_Left);
	  B_In.removeActionListener(RL_In);
	  B_Out.removeActionListener(RL_Out);
	  B_Up.addActionListener(TL_Up);
	  B_Down.addActionListener(TL_Down);
	  B_Right.addActionListener(TL_Right);
	  B_Left.addActionListener(TL_Left);
	  B_In.addActionListener(TL_In);
	  B_Out.addActionListener(TL_Out);
	}
      });

    /* If we choose "Home" set the object transform back to the *
     * Identity matrix.  (ie no rotations / translations)       */
    B_Home.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent E)
        {
          Transform3D T = new Transform3D(); /* Identity Matrix */
          ObjTransform.setTransform(T);
        }
      });
    
    /* If we press the "Axes" button create the Axes Alteration *
     * Toolbox!  (And disable the button to stop copies!)       */
    B_Axes.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent E)
        {
	  B_Axes.setEnabled(false);

	  /* Get the window the GO3DVizPanel is in so that the *
	   * new Dialog can be placed sensibly.                */
	  JFrame Owner = (JFrame) NavPanel.getTopLevelAncestor();
	  final JDialog JD = new JDialog(Owner, "Alter Axes", false);
	  Point Origin = new Point();
	  Origin = Owner.getLocation(Origin);
	  Origin.translate(100, 100);
	  JD.setLocation(Origin);
	  JD.setSize(243,280);

	  /* Create some stuffs to put in the dialog box. */
	  JPanel Content = new JPanel(new FlowLayout(FlowLayout.LEADING));
	  JPanel Title1 = new JPanel(new GridLayout(2, 1));
	  JPanel SAxes = new JPanel(new GridLayout(3, 2));
	  JPanel Title2 = new JPanel(new GridLayout(1, 1));
	  JPanel CAxes = new JPanel(new GridLayout(3, 2));
	  JPanel Buttons = new JPanel(new GridLayout(1, 3));

	  String XChoices[] = new String[Cols+1];
	  String YChoices[] = new String[Cols+1];
	  String ZChoices[] = new String[Cols+1];
	  String RChoices[] = new String[Cols+1];
	  String GChoices[] = new String[Cols+1];
	  String BChoices[] = new String[Cols+1];
	  JLabel L[] = new JLabel[10];
	  final JComboBox CB[] = new JComboBox[6];

	  for (int i = 0; i < 10; i++) { L[i] = new JLabel(""); }
	  for (int i = 0; i < Cols ; i++)
	  {
	    XChoices[i] = new String("Column " + (i+1)+"");
	    YChoices[i] = new String("Column " + (i+1)+"");
	    ZChoices[i] = new String("Column " + (i+1)+"");
	    RChoices[i] = new String("Column " + (i+1)+"");
	    GChoices[i] = new String("Column " + (i+1)+"");
	    BChoices[i] = new String("Column " + (i+1)+"");
	  }
	  XChoices[Cols] = new String("- NONE -");
	  YChoices[Cols] = new String("- NONE -");
	  ZChoices[Cols] = new String("- NONE -");
	  RChoices[Cols] = new String("- NONE -");
	  GChoices[Cols] = new String("- NONE -");
	  BChoices[Cols] = new String("- NONE -");

	  CB[0] = new JComboBox(XChoices);
	  CB[1] = new JComboBox(YChoices);
	  CB[2] = new JComboBox(ZChoices);
	  CB[3] = new JComboBox(RChoices);
	  CB[4] = new JComboBox(GChoices);
	  CB[5] = new JComboBox(BChoices);

	  for (int i = 0; i < 6; i++) { CB[i].setSelectedIndex(Selections[i]); }

	  L[0].setText("AXIS ALTERATION TOOLBOX");
	  L[1].setText("Space Axes");
	  L[2].setText("X Axis");
	  L[3].setText("Y Axis");
	  L[4].setText("Z Axis");
	  L[5].setText("Colour Axes");
	  L[6].setText("R Axis");
	  L[7].setText("G Axis");
	  L[8].setText("B Axis");

	  /* If the "Apply" button is pressed, get the selected  *
	   * values and notify the current object of the changes *
	   * and get it to redraw.                               */
	  JButton Apply = new JButton("Apply");
	  Apply.addActionListener(new ActionListener()
	    {
	      public void actionPerformed(ActionEvent e)
	      {
		for (int i = 0; i < 6; i++)
	        {
		  int Choice = CB[i].getSelectedIndex();
		  if (Choice == Cols)
		    { Selections[i] = -1; }
		  else
		    { Selections[i] = CB[i].getSelectedIndex(); }
		}
		Objects[0].recreateBranch(Selections, Axes);
		ObjTransform.setChild(Objects[0].getBranch(), 3);
	      }
	    });
 
	  /* If the "Close" button is pressed, kill Dialog! */
	  JButton Close = new JButton("Close");
	  Close.addActionListener(new ActionListener()
	    {
	      public void actionPerformed(ActionEvent e)
	      {
		B_Axes.setEnabled(true);
		JD.dispose();
	      }
	    });

	  /* When the Dialog dies, enable the button again */
	  JD.addWindowListener(new WindowListener()
	    {
	      public void windowActivated(WindowEvent e) {}
	      public void windowClosed(WindowEvent e)
	      {
		if (!B_Axes.isEnabled())
		  { B_Axes.setEnabled(true); }
	      }
	      public void windowClosing(WindowEvent e) 
	      {
		if (!B_Axes.isEnabled())
		  { B_Axes.setEnabled(true); }
	      }
	      public void windowDeactivated(WindowEvent e) {}
	      public void windowDeiconified(WindowEvent e) {}
	      public void windowIconified(WindowEvent e) {}
	      public void windowOpened(WindowEvent e) {}
	    });
 
	  Title1.add(L[0]);
	  Title1.add(L[1]);

	  SAxes.add(L[2]);
	  SAxes.add(CB[0]);
	  SAxes.add(L[3]);
	  SAxes.add(CB[1]);
	  SAxes.add(L[4]);
	  SAxes.add(CB[2]);

	  Title2.add(L[5]);

	  CAxes.add(L[6]);
	  CAxes.add(CB[3]);
	  CAxes.add(L[7]);
	  CAxes.add(CB[4]);
	  CAxes.add(L[8]);
	  CAxes.add(CB[5]);

	  Buttons.add(Apply);
	  Buttons.add(L[9]);
	  Buttons.add(Close);

	  Content.add(Title1);
	  Content.add(SAxes);
	  Content.add(Title2);
	  Content.add(CAxes);
	  Content.add(Buttons);

	  JD.getContentPane().add(Content);
	  JD.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	  JD.setVisible(true);
        }
     });

    /* If we press the "Sample" button, create a new Sample Alteration *
     * Toolbox window (and disable the button).                        */
    B_Sample.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent E)
        {
	  B_Sample.setEnabled(false);

	  /* Get the window the GO3DVizPanel is in so that the *
	   * new Dialog can be placed sensibly.                */
	  JFrame Owner = (JFrame) NavPanel.getTopLevelAncestor();
	  final JDialog JD = new JDialog(Owner, "Alter Sample", false);
	  Point Origin = new Point();
	  Origin = Owner.getLocation(Origin);
	  Origin.translate(100, 100);
	  JD.setLocation(Origin);
	  JD.setSize(243,112);

	  /* Create some stuffs to put in the dialog box. */
	  JPanel Content = new JPanel(new FlowLayout(FlowLayout.LEADING));
	  JPanel Title = new JPanel(new GridLayout(1, 1));
	  JPanel Selection = new JPanel(new GridLayout(1, 2));
	  JPanel Buttons = new JPanel(new GridLayout(1, 3));

	  JLabel L[] = new JLabel[3];
	  for (int i = 0; i < 3; i++) { L[i] = new JLabel(""); }

	  L[0].setText("SAMPLE ALTERATION TOOLBOX");
	  L[1].setText("Sample % = ");

	  int curChoice = 10;
	  Vector SChoices = new Vector();
	  SChoices.add(new Float(0.1));
	  SChoices.add(new Float(0.2));
	  SChoices.add(new Float(0.5));
	  SChoices.add(new Float(1.0));
	  SChoices.add(new Float(2.0));
	  SChoices.add(new Float(5.0));
	  SChoices.add(new Float(10.0));
	  SChoices.add(new Float(20.0));
	  SChoices.add(new Float(50.0));
	  SChoices.add(new Float(100.0));
	  SChoices.add(new Float(Objects[0].getSample()));

	  for (int i = 0; i < 10; i++)
	    if (((Float) SChoices.elementAt(i)).compareTo((Float)SChoices.elementAt(10)) == 0)
	    {
	      SChoices.remove(SChoices.size() - 1);
	      curChoice = i;
	      break;
	    }

	  final JComboBox CB = new JComboBox(SChoices);
	  CB.setSelectedIndex(curChoice);

	  /* If the "Apply" button is pressed, get the selected *
	   * value and notify the current object of the changes *
	   * and get it to redraw.                              */
	  JButton Apply = new JButton("Apply");
	  Apply.addActionListener(new ActionListener()
	    {
	      public void actionPerformed(ActionEvent e)
	      {
		Objects[0].setSample((Float) CB.getItemAt(CB.getSelectedIndex()));
		Objects[0].recreateBranch(Selections, Axes);
		ObjTransform.setChild(Objects[0].getBranch(), 3);
	      }
	    });
 
	  /* If the "Close" button is pressed, kill Dialog! */
	  JButton Close = new JButton("Close");
	  Close.addActionListener(new ActionListener()
	    {
	      public void actionPerformed(ActionEvent e)
	      {
		JD.dispose();
	      }
	    });

	  /* When the Dialog dies, enable the button again */
	  JD.addWindowListener(new WindowListener()
	    {
	      public void windowActivated(WindowEvent e) {}
	      public void windowClosed(WindowEvent e)
	      {
		if (!B_Sample.isEnabled())
		  { B_Sample.setEnabled(true); }
	      }
	      public void windowClosing(WindowEvent e) 
	      {
		if (!B_Sample.isEnabled())
		  { B_Sample.setEnabled(true); }
	      }
	      public void windowDeactivated(WindowEvent e) {}
	      public void windowDeiconified(WindowEvent e) {}
	      public void windowIconified(WindowEvent e) {}
	      public void windowOpened(WindowEvent e) {}
	    });

	  Title.add(L[0]);

	  Selection.add(L[1]);
	  Selection.add(CB);

	  Buttons.add(Apply);
	  Buttons.add(L[2]);
	  Buttons.add(Close);

	  Content.add(Title);
	  Content.add(Selection);
	  Content.add(Buttons);

	  JD.getContentPane().add(Content);
	  JD.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	  JD.setVisible(true);
        }
     });

  }

/**
 * Internal function that initialises the 3 sections of the VizPanel.
 */
  private void initPanels()
  {
    this.initToolBar();
    this.initNavigationBar();
    this.init3D();
  }

/**
 * Internal function that initialises the panel containing the toolbar
 * buttons.
 */
  private void initToolBar()
  {
    ToolBar = new Box(BoxLayout.Y_AXIS);
    ToolBar.add(B_Home);
    ToolBar.add(B_Select);
    ToolBar.add(B_Axes);
    ToolBar.add(B_Sample);
    ToolBar.add(B_Type);
  }

/**
 * Internal function that initialises the panel containing the 3D
 * navigation panels.
 */
  private void initNavigationBar()
  {
    this.initZoomPanel();
    this.initRTTogglePanel();
    this.initNavPanel();

    NavigationBar = new JPanel(new BorderLayout());
    NavigationBar.add(ZoomPanel, BorderLayout.EAST);
    NavigationBar.add(RTTogglePanel, BorderLayout.WEST);
    NavigationBar.add(NavPanel, BorderLayout.CENTER);
  }

/**
 * Internal function that initialises the now defunct Zoom buttons.
 */
  private void initZoomPanel()
  {
    ZoomPanel = new Box(BoxLayout.Y_AXIS);
    ZoomPanel.add(B_ZIn);
    ZoomPanel.add(B_ZOut);
  }

/**
 * Internal function that initialises the Rotate / Translate toggle
 * pair of buttons.
 */
  private void initRTTogglePanel()
  {
    RTTogglePanel = new Box(BoxLayout.Y_AXIS);
    RTToggle = new ButtonGroup();
    RTToggle.add(B_Translate);
    RTToggle.add(B_Rotate);
    
    RTTogglePanel.add(B_Translate);
    RTTogglePanel.add(B_Rotate);
  }

/**
 * Internal function that initialises the panel containing the 6
 * directional buttons in the navigation panel.
 */
  private void initNavPanel()
  {
    JLabel Nulls[] = new JLabel[9];
    for (int i = 0; i < 9; i++) { Nulls[i] = new JLabel(""); }
    NavPanel = new JPanel(new GridLayout(3, 5));
    NavPanel.add(Nulls[0]);
    NavPanel.add(Nulls[1]);
    NavPanel.add(B_Up);
    NavPanel.add(Nulls[2]);
    NavPanel.add(Nulls[3]);
    NavPanel.add(B_In);
    NavPanel.add(B_Left);
    NavPanel.add(Nulls[4]);
    NavPanel.add(B_Right);
    NavPanel.add(B_Out);
    NavPanel.add(Nulls[5]);
    NavPanel.add(Nulls[6]);
    NavPanel.add(B_Down);
    NavPanel.add(Nulls[7]);
    NavPanel.add(Nulls[8]);
  }

/**
 * Internal function that initalises the 3D Universe and the
 * Canvas3D on which everything is drawn.
 */
  private void init3D()
  {
    GraphicsConfiguration Config =
      SimpleUniverse.getPreferredConfiguration();

    DrawPanel = new Canvas3D(Config);
    Scene = CreateScene();

    SimpleUniverse Univ = new SimpleUniverse(DrawPanel);
    Univ.getViewingPlatform().setNominalViewingTransform();
    Univ.addBranchGraph(Scene);
  }

/**
 * Internal function that initialises the scene graph including
 * lighting and the transforms that the button listeners are
 * connected to.  It also creates the default axes and default
 * SubObj and adds them to the scene.
 */
  private BranchGroup CreateScene()
  {
    BranchGroup SceneRoot = new BranchGroup();

    /* Create a very dark blue background */
    BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
    Color3f bgColor = new Color3f(0.05f, 0.05f, 0.05f);
    Background BG = new Background(bgColor);
    BG.setApplicationBounds(bounds);
    SceneRoot.addChild(BG);

    /* Create a pale ambient light */
    AmbientLight aLgt = new AmbientLight(new Color3f(0.2f, 0.2f, 0.2f));
    aLgt.setInfluencingBounds(bounds);
    SceneRoot.addChild(aLgt);

    /* Create a single spotlight */
    Vector3f V = new Vector3f(-1.0f, -1.0f, -1.0f);
    DirectionalLight Lgt1 = new DirectionalLight(new Color3f(0.7f, 0.7f, 0.7f), V);
    Lgt1.setInfluencingBounds(bounds);
    SceneRoot.addChild(Lgt1);

    /* Create the transform that the user alters */
    ObjTransform = new TransformGroup();
    ObjTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    ObjTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    SceneRoot.addChild(ObjTransform);

    /* Create the default axes */
    Axes = new GO3DAxis[6];
    Axes[0] = new GO3DSpaceAxis(GO3DAxis.X_DIRECTION);
    Axes[1] = new GO3DSpaceAxis(GO3DAxis.Y_DIRECTION);
    Axes[2] = new GO3DSpaceAxis(GO3DAxis.Z_DIRECTION);
    Axes[3] = new GO3DRGBAxis();
    Axes[4] = new GO3DRGBAxis();
    Axes[5] = new GO3DRGBAxis();

    /* Create (a single) default SubObj of type "ScatterPlot" */
    Objects = new GO3DSubObj[1];
    Objects[0] = new GO3DScatterPlot(myData, Cols, Rows, Axes, Selections, 100.0f);
    Objects[0].createBranch(Selections, Axes);

    ObjTransform.addChild(Axes[0].getBranch());
    ObjTransform.addChild(Axes[1].getBranch());
    ObjTransform.addChild(Axes[2].getBranch());
    ObjTransform.addChild(Objects[0].getBranch());

    /* Make sure we can change the graph later... */
    ObjTransform.setCapability(Group.ALLOW_CHILDREN_WRITE);

    return SceneRoot;
  }

/** Returns a Value of Type.Vector that will contain the data fields
 *  the Visualizer was given to display.  This is -all- the data
 *  in its original form, not just as it is displayed or selected.
 *  c.f. <I>getApply*</I> and <I>getSubObjs</I>.
 */
/*  public Value getValue()
  {
    int numObjs = this.Objects.getLength();
    Value [] myValues = new Value[numObjs];
    for (int i = 0; i < numObjs; i++)
  }
*/
/** Returns the vector of GO3DSubObjs given to the Visualizer.  This
 *  should be the identical vector to that passed to the constructor.
 *  c.f. <I>getApply*</I> and <I>getValue</I>.
 */
/*  public GO3DSubObj[] getSubObjs()
  {
    return this.Objects;
  }
*/
/** Returns the currently selected data as a Value of Type.Vector which
 *  will contain the data fields in their original form.
 *  c.f. <I>getValue</I>, <I>getSubObjs</I> and <I>getApplySubObjs</I>.
 */
/*  public Value getApplyValue()
  {
    return this.getApplyValue();
  }
*/
/** Returns the crreuntly selected data as a vector of GO3DSubObjs.
 *  c.f. <I>getValue</I>, <I>getSubObjs</I> and <I>getApplyValue</I>.
 */
/*  public GO3DSubObj[] getApplySubObjs()
  {
    return this.getSubObjects();
  }
*/

/**
 * A main function that enables the xD Visualiser to be run as a separate
 * entity with a string parameter that specifies the file to be visualised.
 * If no parameter is specified it defaults to "glass_ok.txt" as an example.
 *
 * NOTE:
 * Files to be read are in .txt format!
 * The field separator is expected to be a comma. (,)
 * The record separator is expected to be a newline. (\n)
 * There should be the same number of columns in every row! 
 */
  public static void main(String[] args)
  {
    String S = new String("glass_ok.txt");
    if (args.length > 0) { S = new String(args[0]); }
    try
    {
      Float dataset[][];
      FileReader FR = new FileReader(S);
      char str[] = new char[12000];
      Vector readData = new Vector();
      int tempRows = -1;
      int tempCols = -1;

      try
      { 
	FR.read(str, 0, 12000); 

	StringTokenizer STRow = new StringTokenizer(new String(str), "\n\0");
	int j;
	for (j = 0; STRow.hasMoreTokens(); j++)
	{
	  StringTokenizer STCol = new StringTokenizer(STRow.nextToken(), ",\n");
	  int i;
	  for (i = 0; STCol.hasMoreTokens(); i++)
	  {
	    String R = new String(STCol.nextToken());
	    if (R.compareTo("?") == 0)
	      { readData.addElement(new Float(Float.NaN)); }
	    else
	      {	readData.addElement(new Float(R)); }
	  }
	  if (i != tempCols && tempCols != -1)
	    { System.out.println("Error in reading datafile... Rows have uneven Columns"); }
	  tempCols = i;
	}
	tempRows = j;
	
	dataset = new Float [tempRows][tempCols];

	for (int k = 0; k < tempRows; k++)
	  for (int l = 0; l < tempCols; l++)
	    { dataset[k][l] = (Float) readData.get(k*tempCols + l); }

	/*	for (int m = 0; m < tempRows; m++)
	{
	  System.out.print(dataset[m][0]);
	  for (int n = 1; n < tempCols; n++)
	    { System.out.print(","+dataset[m][n]); }
	  System.out.println("");
	}
	*/
	GO3DVizPanel MAIN = new GO3DVizPanel(dataset, tempRows, tempCols);
	JFrame MF = new JFrame("Multi-Dimensional Graphic Visualizer - "+S);
	MF.getContentPane().add(MAIN);
	MF.setSize(600, 600);
	MF.setVisible(true);
	MF.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//	System.out.println("Should be displaying now.");
      }
      catch (java.io.IOException E)
	{ System.out.println("Error on read..."); }
    }
    catch (java.io.FileNotFoundException E)
      { System.out.println("File not found!"); }

  }
}



