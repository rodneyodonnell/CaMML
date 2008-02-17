//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.dialog;

import javax.swing.*;
import cdms.core.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import cdms.plugin.enview.*;
import javax.swing.tree.*;

public class SelectPanel extends JPanel implements TreeSelectionListener
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6993442978460163193L;
private JButton defineTypeButton = new JButton("Define New Sub-Type");
  private JButton defineValueButton = new JButton("Define New Value");
  private JPanel buttonPanel = new JPanel();
  private JPanel defineTypePanel = new JPanel();
  private JPanel defineValuePanel = new JPanel();
  private EnView.CdmsEnvironmentTree symbolTable;
  private Object result = null;
  private JLabel messageLabel = new JLabel();
  private boolean definedNew = false, _showValues = true;
  private java.util.Vector selectionChangeListeners = new java.util.Vector();
  private JPanel treePanel = new JPanel();
  
  private JTextField expressionField = new JTextField();
  private JCheckBox expressionBox = new JCheckBox("Expression", false);
  private JPanel expressionPanel = new JPanel();
  
  public SelectPanel(String message, Type root, Type withMember, boolean showValues)
  {
    _showValues = showValues;
    messageLabel.setText(message);
    Environment.RegEntry re = Environment.env.getEntryByObject(root);
    if (re == null) re = new Environment.RegEntry("*","",root,"");
    symbolTable = new EnView.CdmsEnvironmentTree(re,withMember,showValues);
    symbolTable.tree.addTreeSelectionListener(this);

    setLayout(new BorderLayout());
    defineTypePanel.add(defineTypeButton);
    if(showValues)
    {
      defineValuePanel.add(defineValueButton);
      buttonPanel.setLayout(new GridLayout(1,4));
    }
    else
    {
      buttonPanel.setLayout(new GridLayout(1,3));
    }
    buttonPanel.add(defineTypePanel);
    if(showValues) buttonPanel.add(defineValuePanel);
    add(messageLabel, BorderLayout.NORTH);
    treePanel.setLayout(new BorderLayout());
    treePanel.add(symbolTable, BorderLayout.CENTER);
    treePanel.add(buttonPanel, BorderLayout.SOUTH);
    add(treePanel, BorderLayout.CENTER);
    expressionPanel.setLayout(new GridLayout(2,1));
    expressionPanel.add(expressionBox);
    expressionPanel.add(expressionField);
    add(expressionPanel, BorderLayout.SOUTH);
    expressionField.setEnabled(false);
    
    defineTypeButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          // Getting the selected Type.
          TreePath selPath = symbolTable.tree.getSelectionPath();
          Environment.RegEntry re = ((EnView.EnvData.Node) selPath.getLastPathComponent()).re;
          result = (Type) ((NewTypeDialog)NewTypeDialog.class.getConstructor(new Class[] {re.o.getClass(), String.class, Boolean.class}).newInstance(new Object[] {re.o, "Define new type...", new Boolean(false)})).getResult();
          definedNew = true;
        }
        catch(Exception ex)
        {
          ex.printStackTrace();
        }
      }
    });

    defineValueButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          TreePath selPath = symbolTable.tree.getSelectionPath();
          Environment.RegEntry re = ((EnView.EnvData.Node) selPath.getLastPathComponent()).re;
          result = (Value) ((NewValueDialog)NewValueDialog.class.getConstructor(new Class[] {re.o.getClass(), String.class, Boolean.class}).newInstance(new Object[] {re.o, "Define new value...", new Boolean(false)})).getResult();
          definedNew = true;
        }
        catch(Exception ex)
        {
          ex.printStackTrace();
        }
      }
    });
    
/*    expressionField.addKeyListener(new KeyListener()                 // This listener makes the expression text red if it does not evaluate to a function.
    {
      public void keyPressed(KeyEvent e) {}
      public void keyReleased(KeyEvent e) 
      {
        try
        {
          cdms.plugin.fpli.Syntax syn = new cdms.plugin.fpli.Syntax(new cdms.plugin.fpli.Lexical(new java.io.ByteArrayInputStream(expressionField.getText().getBytes())));
          cdms.plugin.fpli.Expression exp = syn.exp();    // parse the Expression
          Value res = exp.eval(new cdms.plugin.fpli.Environment(new cdms.plugin.fpli.Environment(null)));  // Don't use boot environment.
          if(_root.hasMember(res.t) && res.t.hasMember(_withMember))
          {
            expressionField.setForeground(Color.black);
            expressionField.setToolTipText("This expression is OK.");
            valueChanged(true);
          }
          else
          {
            expressionField.setForeground(Color.orange);
            expressionField.setToolTipText("This expression is not of the required type.");
            valueChanged(false);
          }
        }
        catch (Exception ex)
        {
          expressionField.setForeground(Color.red);
          expressionField.setToolTipText("This expression has syntactic problems.");
          valueChanged(false);
        }
      }
      public void keyTyped(KeyEvent e) {}
    });
*/
    expressionBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        expressionField.setEnabled(expressionBox.isSelected());
      }
    });
  }  

  public Object getResult()
  {
    if(!definedNew)
    {
      if(expressionBox.isSelected())
      {
        try
        {
          result = cdms.plugin.fpli.Fpli.interpreter.apply(new Value.Str(expressionField.getText()));
          if(!_showValues)
          {
            result = null; 
            System.out.println("\n\nJosh you are a cowboy: \n  " + 
                               "  ((josh.TypeValue.V)result).getType();\n" +
                               "    ^^^^ - what is this?  I have temporarily replaced it with null.  Leroy.");
          }
        }
        catch(Exception e)
        {
          result = Value.TRIV;
        }
      }
      else
      {
        TreePath selPath = symbolTable.tree.getSelectionPath();
        Environment.RegEntry re = ((EnView.EnvData.Node) selPath.getLastPathComponent()).re;
        result = re.o;
      }
    }
    return result;
  }

  public void valueChanged(boolean good)
  {
    int count;
    for(count = 0; count < selectionChangeListeners.size(); count++)
    {
      ((SelectionChangeListener)selectionChangeListeners.elementAt(count)).selectionChanged(good);
    }
  }

  public void valueChanged(TreeSelectionEvent e)
  {
    int count;
    boolean somethingSelected = (getResult() != null);
    for(count = 0; count < selectionChangeListeners.size(); count++)
    {
      ((SelectionChangeListener)selectionChangeListeners.elementAt(count)).selectionChanged(somethingSelected);
    }
  }

  public void addSelectionChangeListener(SelectionChangeListener scl)
  {
    selectionChangeListeners.add(scl);
  }

  public void removeSelectionChangeListener(SelectionChangeListener scl)
  {
    selectionChangeListeners.remove(scl);
  }

  public interface SelectionChangeListener
  {
    public void selectionChanged(boolean somethingSelected);
  }
}
