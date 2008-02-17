//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: EnView.java
// Authors: {leighf}@csse.monash.edu.au

package cdms.plugin.enview;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import cdms.core.*;

/**
  A tree for efficiently navigating the type-value environment.  

  The EnvironmentTreeModel is the foundation for classes that allow for selection of a 
  Named Type Instance or Named Value Instance (eg in a dialog box) 
  or for simply browsing and visualising the environment.

  To use the tree you specify whether you want values to be shown and
  the root (type) (this allows dialogs to restrict selection to a specific branch of the tree).
  You can also require that shown type are parents of a given type.

  The EnvironmentTreeModel has a static instance of a EnvData class which
  reads the environment when it is created and builds some tables which 
  allow the environment hierarchy to be accessed quickly.  The EnvironmentTreeModel
  registers a listener with the environment so that when entries are added or 
  removed the EnvironmentData tables and tree can be updated.
*/
public class EnView
{
  /** EnvData is a class that builds and maintains a
     tree of type information from the environment. 
     It also generates TreeModelListener events. 
  */
  public static class EnvData implements Environment.EnvironmentListener, java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -211969783321603349L;

	public class Node implements java.io.Serializable
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 4376487317293704575L;
	public Environment.RegEntry re;
      public boolean isType;
      public java.util.Vector types;   
      public java.util.Vector values;   

      public Node(Environment.RegEntry re, boolean isType)
      {
        this.re = re;
        this.isType = isType; 
        types = null;
        values = null;
      }

      public String toString()
      {
        if (re.module.compareTo("Standard") == 0) return re.name;
          else return re.module + " " + re.name;
      }

      public int insert(java.util.Vector v, Node n)
      {
        int i = 0;
        while (i < v.size() && ((Node)v.elementAt(i)).toString().compareTo(n.toString()) < 0) i++;  
        v.insertElementAt(n,i);
        return i;
      }


      public void addRemoveType(java.util.Vector path, boolean add, Environment.RegEntry nre)
      {
        if (isType)          // If this node is a type.
        {
          Type t = (Type) re.o;
          Type st = (Type) nre.o;

          if (t.hasMember(st))
          {
            path.add(this);
            boolean addHere = true;
            if (types != null)
            {
              for (int i = 0; i < types.size(); i++)
              {
                Node n = (Node) types.elementAt(i);
                if (n.isType && n.re != re)
                {
                  if ( ((Type) n.re.o).hasMember(st) )
                  {
                    addHere = false;
                    n.addRemoveType((java.util.Vector) path.clone(),add,nre);
                  } 
                }
              }
            }
            if (addHere)
            {

              if (add)         // Add entry.
              {
                if (types == null)
                  types = new java.util.Vector();
                Node newn = new Node(nre,true);
                int childIndex = insert(types,newn);
                notifyTreeNodesInserted(path.toArray(), childIndex, newn);

                // Drag down types and values.
                java.util.Vector newPath = (java.util.Vector) path.clone();
                newPath.add(newn);
                if (values != null)
                {
                  int i = 0;
                  int sz = values.size();
                  while (i < sz)
                  {
                    Node n = (Node) values.elementAt(i);
                    if ( st.hasMember( ((Value) n.re.o).t ) )
                    {
                      if (newn.values == null) 
                        newn.values = new java.util.Vector();
                      int newChildIndex = newn.types == null ? 
                        insert(newn.values,n) : newn.types.size() + insert(newn.values,n);
                      values.remove(i);
                      notifyTreeNodesRemoved(path.toArray(), types == null ? i : types.size() + i, n);
                      notifyTreeNodesInserted(newPath.toArray(), newChildIndex, n);
                      sz--;
                    } 
                    else i++;
                  }
                }
    
                if (types != null)
                {
                  int i = 0;
                  int sz = types.size();
                  while (i < sz)
                  {
                    Node n = (Node) types.elementAt(i);
                    if ( n != newn && st.hasMember( (Type) n.re.o ) )
                    {
                      if (newn.types == null) 
                        newn.types = new java.util.Vector();
                      int newChildIndex = insert(newn.types,n);
                      types.remove(i);
                      notifyTreeNodesRemoved(path.toArray(), i, n);
                      notifyTreeNodesInserted(newPath.toArray(), newChildIndex, n);
                      sz--;
                    }
                    else i++;
                  }
                }

              }
              else   // Remove entry.
              { 
                java.util.Vector prevPath = (java.util.Vector) path.clone();
                prevPath.removeElementAt(prevPath.size() - 1);
                for (int i = 0; i < types.size(); i++)
                {
                  Node n = (Node) types.elementAt(i);
                  if ( n.re == nre ) 
                  {
                    // Drag up types and values.
                    if (n.types != null)
                    {
                      for (int j = n.types.size() - 1; j >= 0; j--)
                      {
                        int newChildIndex = insert(types,(Node) n.types.elementAt(j));
                        n.types.removeElementAt(j);
                        notifyTreeNodesRemoved(path.toArray(), j, (Node) n.types.elementAt(j));
                        notifyTreeNodesInserted(prevPath.toArray(), newChildIndex, (Node) n.types.elementAt(j));
                      }    
                    }

                    if (n.values != null)
                    {
                      for (int j = n.values.size() - 1; j >= 0; j--)
                      {
                        int newChildIndex = types == null ? 
                          insert(values,(Node) n.values.elementAt(j)) : 
                          types.size() + insert(values,(Node) n.values.elementAt(j));
                        n.values.removeElementAt(j);
                        notifyTreeNodesRemoved(path.toArray(), n.types == null ? j : n.types.size() + j, 
                                              (Node) n.values.elementAt(j));
                        notifyTreeNodesInserted(prevPath.toArray(), newChildIndex, (Node) n.values.elementAt(j));
                      }    
                    }

                    types.removeElementAt(i);
                    notifyTreeNodesRemoved(path.toArray(), i, n);
                    break;
                  }
                }
                
              }
            }
          }
          else if (st.hasMember(t))           // Push this node down.
          {
            path.add(this);
            Node n = new Node(this.re,true);
            n.types = this.types;
            n.values = this.values;
            re = nre;
            types = new java.util.Vector();
            values = null;
            insert(this.types,n);
            notifyTreeStructureChanged(path.toArray());
          }
        }
      }

      public void addRemoveValue(java.util.Vector path, boolean add, Environment.RegEntry nre)
      {
        if (isType)          // If this node is a type.
        {
          Type t = (Type) re.o;
          Type st = (Type) ((Value) nre.o).t;
          if (t.hasMember(st))
          {
            path.add(this);
            boolean addHere = true;
            if (types != null)
            {
              for (int i = 0; i < types.size(); i++)
              {
                Node n = (Node) types.elementAt(i);
                if (n.isType)
                {
                  if ( ((Type) n.re.o).hasMember(st) )
                  {
                    addHere = false;
                    n.addRemoveValue((java.util.Vector) path.clone(),add,nre);
                  } 
                }
              }
            }
            if (addHere)
            {
              if (add)
              {
                if (values == null)
                  values = new java.util.Vector();
                Node n = new Node(nre,false);
                int childIndex = types == null ? insert(values,n) : types.size() + insert(values,n); 
                notifyTreeNodesInserted(path.toArray(), childIndex, n);
              }
              else
              {
                for (int i = 0; i < values.size(); i++)
                {
                  Node n = (Node) values.elementAt(i);
                  if ( n.re == nre ) 
                  {
                    values.removeElementAt(i);
                    notifyTreeNodesRemoved(path.toArray(), types == null ? i : types.size() + i, n);
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }

    public Node root;
    public Type withMember;
    public java.util.Vector listeners = new java.util.Vector();

    public EnvData(Environment.RegEntry rootTypeRe, Type withMember)
    {
      // Create a hidden root type.
      this.withMember = withMember;
      root = new Node(rootTypeRe,true);
      Environment.env.addEnvironmentListener(this);
    }

    public Value[] valuesOfType(Type t)
    {
      return null;
    }

    /** EnvironmentListener methods */
    public void entryAdded(Environment e, Environment.RegEntry re)
    {
      java.util.Vector path = new java.util.Vector();

      if (re.o instanceof Value)              // Value added.
      {
        if ( ((Type) root.re.o).hasMember(((Value) re.o).t) )
        {
          if (withMember != null) 
          {
            if ( ((Value) re.o).t.hasMember(withMember) ) 
              root.addRemoveValue(path,true,re);
          }
          else root.addRemoveValue(path,true,re);      
        }
      }
      else                                    // Type added.
      {
        if (re != root.re && ((Type) root.re.o).hasMember((Type) re.o))
        {
          if (withMember != null)
          {
            if ( ((Type) re.o).hasMember(withMember) )
              root.addRemoveType(path,true,re);
          } else root.addRemoveType(path,true,re);
        }
      }

    }

    public void entryRemoved(Environment e, Environment.RegEntry re)
    {
      java.util.Vector path = new java.util.Vector();

      if (re.o instanceof Value)
        root.addRemoveValue(path,false,re);
      else root.addRemoveType(path,false,re);
    }

    public void cleared(Environment e)
    {
      root = null;
      notifyTreeStructureChanged(new Object[] {});
    }

    public void addTreeModelListener(TreeModelListener l) { listeners.add(l); } 
    public void removeTreeModelListener(TreeModelListener l) { listeners.remove(l); }

    /** Notify the any registered EnvironmentTreeModel.  They only expect to receive messages
        containing one child. */
    protected void notifyTreeNodesInserted(Object[] path, int childIndex, Node child)
    {
      TreeModelEvent evt = new TreeModelEvent(this,path, new int[] { childIndex }, new Node[] { child });
      for (int i = 0; i < listeners.size(); i++)
        ((TreeModelListener) listeners.elementAt(i)).treeNodesInserted(evt);
    }

    protected void notifyTreeNodesRemoved(Object[] path, int childIndex, Node child)
    {
      TreeModelEvent evt = new TreeModelEvent(this,path,new int[] { childIndex },new Node[] { child });
      for (int i = 0; i < listeners.size(); i++)
          ((TreeModelListener) listeners.elementAt(i)).treeNodesRemoved(evt);
    }

    protected void notifyTreeStructureChanged(Object[] path)
    {
      TreeModelEvent evt = new TreeModelEvent(this,path);
      for (int i = 0; i < listeners.size(); i++)
          ((TreeModelListener) listeners.elementAt(i)).treeStructureChanged(evt);
    }

  }

  /** The environment TreeModel.  @see EnView */
  public static class EnvironmentTreeModel implements TreeModel, TreeModelListener, java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5711378667765172986L;
	private java.util.Vector listeners = new java.util.Vector();
    private EnvData envData;
    private EnvData.Node rootNode;
    private Type rootType;
    public boolean showValues = true; 

    public EnvironmentTreeModel(EnvData envData, boolean showValues, Type rootType)
    {
      this.showValues = showValues;
      this.envData = envData;
      envData.addTreeModelListener(this);
      setRootType(rootType);
    }

    protected void finalize()
    {
      envData.removeTreeModelListener(this);
    }

    public void setRootType(Type root)
    {
      rootType = root;

      // Find the root Type or create new Node. 
      rootNode = envData.root;
      boolean moved = true;
      while ( rootNode.re.o != rootType && moved )
      {
        moved = false;
        if (rootNode.types != null)
        {
          for (int i = 0; i < rootNode.types.size(); i++)
          {
            EnvData.Node n = (EnvData.Node) rootNode.types.elementAt(i);
            if (n.re.o == rootType) 
            {
              rootNode = n;
              break;
            }

            if (n.isType)
            {
              if ( ((Type) n.re.o).hasMember(rootType) )
              {
                rootNode = n;
                moved = true;
                break;
              } 
            }
          }
        }
      }
    }

    // This method is invoked by the JTree only for editable trees.  
    // This TreeModel does not allow editing, so we do not implement 
    // this method.  The JTree editable property is false by default.
    public void valueForPathChanged(TreePath path, Object newValue)
    {
      ;
    }

    public void addTreeModelListener(TreeModelListener l) { listeners.add(l); } 
    public void removeTreeModelListener(TreeModelListener l) { listeners.remove(l); }

    /** Converts a EnvData TreePath into one which is consistent with our rootType.
        Returns null if the TreePath is not in our tree. */
    public Object[] makePath(Object[] path)
    {
      int i = 0;
      while (i < path.length && path[i] != rootNode) i++;
      if (i < path.length && path[i] == rootNode)
      {
        java.util.Vector v = new java.util.Vector();
        while (i < path.length)
        {
          v.add(path[i]);
          i++;
        }
        return v.toArray();
      }
      else return null;
    }

    public void treeNodesChanged(TreeModelEvent e) 
    {
      Object[] path = e.getPath();
      if ( ((EnvData.Node) path[path.length-1]).isType || showValues ) 
      {
        Object[] realPath = makePath(path);
        if (realPath != null)
        {
          TreeModelEvent tme = new TreeModelEvent(this,realPath);
          for (int i = 0; i < listeners.size(); i++)
            ((TreeModelListener) listeners.elementAt(i)).treeNodesChanged(tme);
        }
      }
    }

    public void treeNodesInserted(TreeModelEvent e) 
    {
      Object[] children = e.getChildren();
      if ( ((EnvData.Node) children[0]).isType || showValues )  // We only expect 1 child.
      {
        Object[] realPath = makePath(e.getPath());
        if (realPath != null)
        {
          TreeModelEvent tme = new TreeModelEvent(this,realPath,e.getChildIndices(),e.getChildren());
          for (int i = 0; i < listeners.size(); i++)
            ((TreeModelListener) listeners.elementAt(i)).treeNodesInserted(tme);
        }
      }
    }

    public void treeNodesRemoved(TreeModelEvent e) 
    {
      Object[] children = e.getChildren();
      if ( ((EnvData.Node) children[0]).isType || showValues )  // We only expect 1 child.
      {
        Object[] realPath = makePath(e.getPath());
        if (realPath != null)
        {
          TreeModelEvent tme = new TreeModelEvent(this,realPath,e.getChildIndices(),e.getChildren());
          for (int i = 0; i < listeners.size(); i++)
            ((TreeModelListener) listeners.elementAt(i)).treeNodesRemoved(tme);
        }
      }
    }

    public void treeStructureChanged(TreeModelEvent e) 
    {
      Object[] path = e.getPath();
      if ( ((EnvData.Node) path[path.length-1]).isType || showValues ) 
      {
        Object[] realPath = makePath(path);
        if (realPath != null)
        {
          TreeModelEvent tme = new TreeModelEvent(this,realPath);
          for (int i = 0; i < listeners.size(); i++)
            ((TreeModelListener) listeners.elementAt(i)).treeStructureChanged(tme);
        }
      }
    }

    public Object getChild(Object parent, int index)
    {
      EnvData.Node n = (EnvData.Node) parent;

      if (n.types != null)
      {
        if (index < n.types.size())
          return n.types.elementAt(index);
        else return n.values.elementAt(index - n.types.size());
      }
      else return n.values.elementAt(index);
    }

    public int getChildCount(Object parent)
    {
      EnvData.Node p = (EnvData.Node) parent;
      int c = 0;
      if (p.types != null) c += p.types.size();
      if (showValues && p.values != null) c += p.values.size();
      return c;
    }

    public int getIndexOfChild(Object parent, Object child)
    {
      EnvData.Node p = (EnvData.Node) parent;

      if (p.types != null)
      {
        int r = p.types.indexOf(child);
        if (r == -1) r = p.types.size() + p.values.indexOf(child);
        return r;
      }
      else
      {
        return p.values.indexOf(child);
      }
    }

    public Object getRoot()
    {
      if (rootNode == null)
        return "ENVIRONMENT IS EMPTY";
      else return rootNode;
    }

    public boolean isLeaf(Object node)
    {
      EnvData.Node p = (EnvData.Node) node;

      if (node instanceof String) return true;
        else return p.types == null && (p.values == null || !showValues);
    }
  } 

  public static class TreeCellRenderer extends DefaultTreeCellRenderer implements java.io.Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6962324387782552374L;
	ImageIcon typeIcon;

    public TreeCellRenderer() 
    {
      typeIcon = new ImageIcon("cdms/plugin/desktop/images/Type.gif");
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                      boolean sel, boolean expanded,
                                      boolean leaf, int row, boolean hasFocus) 
    {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

      EnvData.Node n = (EnvData.Node) value;
      if (n.re.description.compareTo("") != 0)
        setToolTipText(n.re.description);
      else setToolTipText(null);

      if (n.isType) 
        setIcon(typeIcon);
      else setIcon(null); 

      return this;
    }
  }


  /** A scrollpane containing an environment tree. */ 
  public static class CdmsEnvironmentTree extends JScrollPane implements java.io.Serializable 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 765682683868279465L;
	public EnvironmentTreeModel etm;
    public JTree tree;

    /** Has root type rootTypeRe, and filters out types that don't have withMember.  
        If hasMember is null then the filter is not used. */
    public CdmsEnvironmentTree(Environment.RegEntry rootTypeRe, Type withMember, boolean showValues)
    {
      this(new EnvData(rootTypeRe,withMember),(Type) rootTypeRe.o,showValues);
    }

    public CdmsEnvironmentTree(EnvData envData, Type rootType, boolean showValues)
    {
      super(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      etm = new EnvironmentTreeModel(envData,showValues,rootType);
      tree = new JTree(etm);
      tree.putClientProperty("JTree.lineStyle", "Angled");
      setViewportView(tree);
      tree.setCellRenderer(new TreeCellRenderer());
    }
  }

}
