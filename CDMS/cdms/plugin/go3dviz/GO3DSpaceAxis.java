package cdms.plugin.go3dviz;

/**
 * Title:        GO3DSpaceAxis<p>
 * Description:  An extension of the GO3DAxis that represents a basic
 *               axis in X, Y or Z.  Drawn as a thin gray box with a
 *               simple label with a span from [-5.0, 5.0] and a default
 *               value of 0.0.
 * Package:      go3dviz<p>
 * Copyright:    Copyright (c) Michael Moore<p>
 * Company:      Monash University<p>
 * @author Michael Moore
 * @version 2.0
 * @see G3DVisualizer
 */

//package go3dviz;

import java.lang.String;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.geometry.*;
import java.awt.Font;

public class GO3DSpaceAxis extends GO3DAxis
{
/** Creates an axis that defaults to [0.0, 1.0] -> [-5.0, 5.0].  0.0 is
 *  set to be the default value.  The parameter uses one of the constants
 *  defined in GO3DAxis to set the visual direction.
 *
 *  Dir should be one of:
 *    GO3DAxis.X_DIRECTION, GO3DAxis.Y_DIRECTION or GO3DAxis.Z_DIRECTION
 *
 *  GO3DAxis.UNDISPLAYED is valid, but unusual for a space axis.
 */
  public GO3DSpaceAxis(int Dir)
  {
    minAxisVal = -5.0f;
    maxAxisVal = 5.0f;

    minDataVal = 0.0f;
    maxDataVal = 1.0f;
    defaultVal = 0.0f;

    this.init(Dir);

    initAxisRoot();
  }

/** Creates an axis that defaults to [min, max] -> [-5.0, 5.0].  0.0 is
 *  set to be the default value.  The parameter uses one of the constants
 *  defined in GO3DAxis to set the visual direction.
 *
 *  Dir should be one of:
 *    GO3DAxis.X_DIRECTION, GO3DAxis.Y_DIRECTION or GO3DAxis.Z_DIRECTION
 *
 *  GO3DAxis.UNDISPLAYED is valid, but unusual for a space axis.
 */
  public GO3DSpaceAxis(int Dir, float min, float max)
  {
    minAxisVal = -5.0f;
    maxAxisVal = 5.0f;

    minDataVal = min;
    maxDataVal = max;
    defaultVal = 0.0f;

    this.init(Dir);

    initAxisRoot();
  }

/** Creates an axis that defaults to [min, max] -> [-5.0, 5.0].  def is
 *  set to be the default value.  The parameter uses one of the constants
 *  defined in GO3DAxis to set the visual direction.
 *
 *  Dir should be one of:
 *    GO3DAxis.X_DIRECTION, GO3DAxis.Y_DIRECTION or GO3DAxis.Z_DIRECTION
 *
 *  GO3DAxis.UNDISPLAYED is valid, but unusual for a space axis.
 */
  public GO3DSpaceAxis(int Dir, float min, float max, float def)
  {
    minAxisVal = -5.0f;
    maxAxisVal = 5.0f;

    minDataVal = min;
    maxDataVal = max;
    defaultVal = def;

    this.init(Dir);

    initAxisRoot();
  }

/* Initialises the subgraph containing the method of drawing this axis. */
  void initAxisRoot()
  {
    axisRoot = new BranchGroup();

    /* Create the "material" the axis is made out of... */
    Appearance A = new Appearance();
    Color3f C = new Color3f(0.8f, 0.8f, 0.8f);
    Color3f Black = new Color3f(0.0f, 0.0f, 0.0f);
    Color3f White = new Color3f(1.0f, 1.0f, 1.0f);
    A.setMaterial(new Material(C, Black, C, White, 80.0f));

    /* Creates a transform group that will position and orient the axis. */
    Transform3D T = new Transform3D();
    TransformGroup TG = new TransformGroup(T);

    /* Some pieces that will make a label */
    String Lbl = new String();
    Point3f LblPos = new Point3f();

    switch(Direction)
    {
      case X_DIRECTION: /* Set the transform to move and scale appropriately */
	                T.set(0.1, new Vector3d(0.5, 0.0, 0.0));
			/* Add a new box of the right size to the graph */
			TG.addChild(new Box(axisRange / 2.0f, 0.05f, 0.05f, A));
			/* Set the label and label position */
			Lbl = new String("X");
			LblPos = new Point3f(5.0f, 0.0f, 0.0f);
			break;
      case Y_DIRECTION: T.set(0.1, new Vector3d(0.0, 0.5, 0.0));
			TG.addChild(new Box(0.05f, axisRange / 2.0f, 0.05f, A));
			Lbl = new String("Y");
			LblPos = new Point3f(0.0f, 5.0f, 0.0f);
			break;
      case Z_DIRECTION: T.set(0.1, new Vector3d(0.0, 0.0, 0.5));
			TG.addChild(new Box(0.05f, 0.05f, axisRange / 2.0f, A));
			Lbl = new String("Z");
			LblPos = new Point3f(0.0f, 0.0f, 5.0f);
			break;
      default:          Lbl = null;
	                LblPos = null;
    }

    axisRoot.addChild(TG);

    /* If we have a label to make.. */
    if (Lbl != null)
    {
      /* Set the colour and appearance and font of the label, *
       * creating it's own transform to position it correctly */
      Color3f LblColor = new Color3f(0.7f, 0.7f, 0.7f);
      Text2D AxisLbl = new Text2D(Lbl, LblColor, "Times", 120, Font.BOLD);
      Appearance A2 = new Appearance();
      A2 = AxisLbl.getAppearance();
      PolygonAttributes PA = new PolygonAttributes();
      PA.setCullFace(PolygonAttributes.CULL_NONE);
      A2.setPolygonAttributes(PA);
      BoundingSphere BS = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
      Transform3D T2 = new Transform3D();
      T2.set(new Vector3f(LblPos), 1.0f);
      TransformGroup TG2 = new TransformGroup(T2);
      TG2.addChild(AxisLbl);

/* This code -should- initalise "Billboard Behaviour", in which the label always
   faces the camera, but it doesn't.  I'm not exactly sure why not, so I left it
   here, commented out. */

/*    Billboard BB = new Billboard(TG2, Billboard.ROTATE_ABOUT_POINT, LblPos);
      BB.setBounds(BS);
      BB.setSchedulingBounds(BS);
      BB.setEnable(true);
*/    axisRoot.addChild(TG2);
    }
  }
}

