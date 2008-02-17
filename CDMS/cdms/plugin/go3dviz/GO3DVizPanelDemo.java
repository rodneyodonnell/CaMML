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
import javax.swing.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.ColorCube;

public class GO3DVizPanelDemo extends JPanel
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
  JMenu Fields, Sampling, Selection;

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
 * A vector containing the data-space axes and the translations
 * between them and 3D-space.  They contain Type information as
 * well as the values necessary to convert a (possibly infinite)
 * "physical" dimension into a data dimension and vice versa. For
 * a full explanation of this class go see its own Javadoc. 
 */
  //  GO3DAxis[] Axes;

/**
 * A vector of the data objects.  These are of the GO3DSubObj
 * class and contain -all- the data selected by the user (including
 * undisplayed data) and the function enabling them to be displayed
 * given a set of axes.  For more information see the classes own
 * Javadoc.
 */
  //  GO3DSubObj[] Objects;

  public GO3DVizPanelDemo()
  { 
    initJComponents();
  }

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

    B_Home.setEnabled(false);
    B_Select.setEnabled(false);
    B_Label.setEnabled(false);
    B_Axes.setEnabled(false);
    B_Sample.setEnabled(false);
    B_Type.setEnabled(false);
    B_ZIn.setEnabled(false);
    B_ZOut.setEnabled(false);

  }

  private void initPanels()
  {
    this.initToolBar();
    this.initNavigationBar();
    this.init3D();
  }

  private void initToolBar()
  {
    ToolBar = new Box(BoxLayout.Y_AXIS);
    ToolBar.add(B_Home);
    ToolBar.add(B_Select);
    ToolBar.add(B_Axes);
    ToolBar.add(B_Sample);
    ToolBar.add(B_Type);
  }

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

  private void initZoomPanel()
  {
    ZoomPanel = new Box(BoxLayout.Y_AXIS);
    ZoomPanel.add(B_ZIn);
    ZoomPanel.add(B_ZOut);
  }

  private void initRTTogglePanel()
  {
    RTTogglePanel = new Box(BoxLayout.Y_AXIS);
    RTToggle = new ButtonGroup();
    RTToggle.add(B_Translate);
    RTToggle.add(B_Rotate);
    
    RTTogglePanel.add(B_Translate);
    RTTogglePanel.add(B_Rotate);
  }

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

  private BranchGroup CreateScene()
  {
    BranchGroup SceneRoot = new BranchGroup();

    ObjTransform = new TransformGroup();
    ObjTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    SceneRoot.addChild(ObjTransform);

    TransformGroup ObjTransform2 = new TransformGroup();
    ObjTransform2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    ObjTransform.addChild(ObjTransform2);

    ObjTransform2.addChild(new ColorCube(0.4));

    Transform3D yAxis = new Transform3D();
    yAxis.rotZ(Math.PI/4.0f);
    Alpha rotationAlpha1 = new Alpha(-1, 4000);

    RotationInterpolator rotator1 =
      new RotationInterpolator(rotationAlpha1, ObjTransform, yAxis,
					 0.0f, (float) Math.PI*2.0f);

    BoundingSphere bounds =
      new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
    rotator1.setSchedulingBounds(bounds);
    SceneRoot.addChild(rotator1);

    Transform3D xAxis = new Transform3D();
    xAxis.rotX(Math.PI/2.0f);
    Alpha rotationAlpha2 = new Alpha(-1, 16000);

    RotationInterpolator rotator2 =
      new RotationInterpolator(rotationAlpha2, ObjTransform2, xAxis,
					 0.0f, (float) Math.PI*4.0f);

    rotator2.setSchedulingBounds(bounds);
    SceneRoot.addChild(rotator2);

    SceneRoot.compile();

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

  public static void main(String[] args)
  {
    GO3DVizPanelDemo MAIN = new GO3DVizPanelDemo();
    JFrame MF = new JFrame("Multi-Dimensional Graphic Visualizer");
    MF.getContentPane().add(MAIN);
    MF.setSize(600, 600);
    MF.setVisible(true);
    System.out.println("Should be displaying now.");
  }
}