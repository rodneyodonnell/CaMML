// File: NewTypeDialogVector.java
// Class: NewTypeDialogVector
// Authors: {joshc}@cs.monash.edu.au

package cdms.plugin.dialog;

import javax.swing.*;
import java.awt.*;
import cdms.core.*;

public class NewTypePanelModel extends NewPanel implements
  TypeFieldSelector.TypeFieldSelectorListener
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2056178084356578840L;
private Type result = null;
  private JPanel northPanel = new JPanel();
  private TypeFieldSelector paramSelector;
  private TypeFieldSelector dataSelector;
  private TypeFieldSelector sharedSelector;
  private TypeFieldSelector sufficientSelector;
  private Type.Model tf;

  public Object getResult()
  {
    return result;
  }

  public boolean getInitialOKState()
  {
    return true;
  }

  public NewTypePanelModel(Type.Model t)
  {
    super();
    tf = t;
    northPanel.setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());
    northPanel.setLayout(new GridLayout(4,1));
    paramSelector = new TypeFieldSelector("Parameter Space:", "Select Parameter Space...", this,
      tf.paramSpace);
    dataSelector = new TypeFieldSelector("Data Space:", "Select Data Space...", this,
      tf.dataSpace);
    sharedSelector = new TypeFieldSelector("Shared (input) Space:", "Select Shared Space...", this,
      tf.sharedSpace);
    sufficientSelector = new TypeFieldSelector("Sufficient Space:", "Select Sufficient Space...",
      this, tf.sufficientSpace);
    northPanel.add(paramSelector);
    northPanel.add(dataSelector);
    northPanel.add(sharedSelector);
    northPanel.add(sufficientSelector);
    add(northPanel, BorderLayout.CENTER);
  }

  public void selectionChanged()
  {
    if((paramSelector.getSelection() != null)&&(dataSelector.getSelection() !=
      null)&&(sharedSelector.getSelection() != null)&&(sufficientSelector.getSelection() != null))
    {
      result = new Type.Model(dataSelector.getSelection(),
        paramSelector.getSelection(), sharedSelector.getSelection(),
        sufficientSelector.getSelection());
      okListener.okEvent(true);
    }
    else
    {
      result = null;
      okListener.okEvent(false);
    }
  }
}