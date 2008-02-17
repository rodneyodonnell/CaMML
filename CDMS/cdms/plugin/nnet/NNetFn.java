//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567
package cdms.plugin.nnet;

import cdms.core.*;
import cdms.plugin.desktop.*;
import cdms.plugin.search.*;
import java.awt.*;

/** Neural Network Functions. */
public class NNetFn
{

  public static Wizard wizard = new Wizard();
  public static class Wizard extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1055897427061673410L;
	public static final Type.Function TT = new Type.Function(Type.VECTOR,Type.TRIV);

    public Wizard()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      cdms.plugin.desktop.Wizard w = new cdms.plugin.desktop.Wizard(new WizardPanel((Value.Vector) v));
      w.setPreferredSize(new Dimension(600,400));
      DesktopFrame.makeWindow("Neural Network Wizard",w);
      return Value.TRIV;
    }
  }

  /** The first panel for the NNet wizard.  Allows selection of input and output columns of data for training. */
  public static class WizardPanel extends cdms.plugin.desktop.Wizard.WizardPanel
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2341622273623487144L;
	cdms.plugin.desktop.Formatter formatter;

    public WizardPanel(Value.Vector data)
    {
      setLayout(new GridLayout(1,1));
      formatter = new cdms.plugin.desktop.Formatter(data);
      add(formatter);
      setBackEnabled(false);
    }

    public boolean wantScroller() { return false; }

    public cdms.plugin.desktop.Wizard.WizardPanel getNext()
    {
      return new WizardPanel2(formatter.getInputData(),formatter.getOutputData());
    }
  }
 
  /** The second panel for the NNet wizard.  Allows selection of network parameters. */ 
  public static class WizardPanel2 extends cdms.plugin.desktop.Wizard.WizardPanel
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8465077120162298775L;
	Value.Vector inputData, outputData;
    NNetControlPanel nnetControlPanel;

    public WizardPanel2(Value.Vector inputData, Value.Vector outputData)
    {
      setLayout(new GridLayout(1,1));
      this.inputData = inputData;
      this.outputData = outputData;
      nnetControlPanel = new NNetControlPanel();
      add(nnetControlPanel);
    }

    public boolean wantScroller() { return false; }

    public cdms.plugin.desktop.Wizard.WizardPanel getNext()
    {
      Type inputType = ((Type.Vector)(inputData.t)).elt;
      Type outputType = ((Type.Vector)(outputData.t)).elt;
      /* the next line is unnecessary if the appropriate check has already been made elsewhere... so either remove it, or this comment!*/
      if ((inputData.length()==0)||(outputData.length()==0)) 
        throw new IllegalArgumentException("Training data for neural net search must be non-empty");
      int inVectorLength = (inputType instanceof Type.Vector) ? ((Value.Vector)(inputData.elt(0))).length() : 0;
      int outVectorLength = (outputType instanceof Type.Vector) ? ((Value.Vector)(outputData.elt(0))).length() : 0;

      // Hidden layer sizes should be set here...temporarily set to 1 hidden layer with 10 units.
      int[] hiddenLayerSizes = new int[1];
      hiddenLayerSizes[0] = 10;

      NNetInt nnetInt = new NNetInt(inputType, inVectorLength, outputType, outVectorLength, hiddenLayerSizes);

      // Initialize nnetInt according to nnetControlPanel settings.
      nnetInt.nguyenWidrowInitWeights();

      NNetSearchObject nnetso = new NNetSearchObject(inputData,outputData,nnetInt);
      SearchControl sc = new SearchControl(new Search(nnetso),null);
      sc.setPreferredSize(new Dimension(600,400));
      DesktopFrame.makeWindow("NNet Training",sc);
      return null;
    }
  }

}
