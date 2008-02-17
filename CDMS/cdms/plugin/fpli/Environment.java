//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.fpli;

import java.util.*;

public class Environment implements java.io.Serializable
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5162597859621118231L;
protected Environment parent;
  protected Hashtable nameHash = new Hashtable();

  public Environment(Environment parent)
  {
    this.parent = parent;
  }

  public void add(String name, Object o)
  {
    if (parent == null) 
      cdms.core.Environment.env.add(name,"Boot",o,"");
    else nameHash.put(name,o);
  }

  public Object getObject(String name)
  {
    Object o = nameHash.get(name);
 
    if (o == null)
    {
      if (parent != null) 
      {
        o = parent.getObject(name); 
      }
      else 
      {
        int idx = name.indexOf(".");
        if (idx != -1)
        {
          String fst = name.substring(idx+1);
          String snd = name.substring(0,idx);
          o = cdms.core.Environment.env.getObject(fst,snd);
        }
        else 
        {
          o = cdms.core.Environment.env.getObject(name,"Boot");
          if (o == null) o = cdms.core.Environment.env.getObject(name,"Standard");
        }
      }
    }
    return o;
  }
}
