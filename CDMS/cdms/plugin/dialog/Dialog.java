//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Dialog.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.dialog;

import cdms.core.*;
import cdms.plugin.desktop.*;

public class Dialog extends Module.StaticFunctionModule
{
  public Dialog() 
  {
    super("Dialog",Module.createStandardURL(Dialog.class),
          new Class[] { Dialog.class, FileLoader.class, DelimitedFile.class });
  }

  public static final ApplyFunction apply = new ApplyFunction();
  public static class ApplyFunction extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3200628117623694511L;

	public ApplyFunction()
    {
      super(Type.FUNCTION);
    }

    public Value apply(Value v)
    {
      // Only allow functions with the correct parameter type.
      Type.Function withMember = new Type.Function(v.t, new Type.Variable(), false, false); 

      String msg = "Select function to apply...";
      Value.Function f = 
        (Function) new SelectDialog(msg, Type.FUNCTION, withMember, true).getResult();

      if (f != null)
      {
        Value result = f.apply(v);
        if(result != Value.TRIV) DesktopFrame.makeWindow("Result",result);
        return result;
      }
      else return Value.TRIV;
    }
  }

  public static final AddToEnvironmentDialog addToEnvironmentDialog = new AddToEnvironmentDialog();
  public static class AddToEnvironmentDialog extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 9183808734140868004L;

	public AddToEnvironmentDialog()
    {
      super(new Type.Function(Type.TYPE, Type.TRIV));
    }

    public Value apply(Value v)
    {
      new cdms.plugin.dialog.AddToEnvironmentDialog(v);
      return Value.TRIV;
    }
  }

  public static final ViewTypeFN viewTypeFN = new ViewTypeFN();
  public static class ViewTypeFN extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4566053874208176795L;

	public ViewTypeFN()
    {
      super(new Type.Function(Type.TYPE, Type.TRIV));
    }

    public Value apply(Value v)
    {
      Type t = v.t;
      try
      {
        NewTypeDialog.class.getConstructor(new Class[] {t.getClass(), String.class, Boolean.class}).newInstance(new Object[] {t, "Type information", new Boolean(false)});
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
      }
      return Value.TRIV;
    }
  }
}
