//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Environment.java
// Authors: {leighf}@csse.monash.edu.au

package cdms.core;

import java.util.*;
import java.io.*;

/**
    The type and value environment.  An entry contains the variable name, the name
    of the module it belongs to, the object instance, and a descriptive string.
    Each entry in stored in hash tables to allow lookup by object instance or name. 
    The environment has a listener system to signal the add, removal or clear
    operations to any interested parties.
    <p>
    This version of the environment is flat and is rather heavy-weight.  It is intended 
    to be used for global variables where there is a lot of overhead from listeners (eg
    the environment viewer listener).  Scripting language would generally define their own
    environment which uses this environment for top level lookups.
    <p>
    The following operations can be implemented using listeners and  
    have been left to the components that require them.
    <ul>
    <li>Maintain parsed name lists.
    <li>Maintain base type list.
    <li>Function type lists - functions that are applicable to a given type.   
    </ul>
    See the environment viewer ({@link cdms.plugin.enview.EnView}) for an example 
    of these operations.
*/
public class Environment implements Serializable
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6183749156442946648L;
public static Environment env = new Environment();   // The environment.

  /** Environment listener interface. */
  public interface EnvironmentListener
  {
    /** Signals that a new variable (re) was added to environment e.  */
    public abstract void entryAdded(Environment e, RegEntry re);

    /** Signals that a variable (re) was removed from environment e.  */
    public abstract void entryRemoved(Environment e, RegEntry re);

    /** Signals all variables were removed from environment e. */
    public abstract void cleared(Environment e);
  }

  /** Represents the set of environment entries with the same name.  The variables
      will have different module names.  
  */ 
  public static class RegEntries implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6516098458031334755L;
	public String name;
    public Hashtable entries = new Hashtable(1);  

    public RegEntries(String name)
    {
      this.name = name;
    }
  }

  /** An environment variable entry. */
  public static class RegEntry implements Serializable
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2360525130533465604L;
	public String name;
    public String module;
    public Object o;                // Strict.
    public String description;

    public RegEntry(String name, String module, String description)
    {
      this.name = name;
      this.module = module;
      this.description = description;
    }

    public RegEntry(String name, String module, Object o, String description)
    {
      this.name = name;
      this.module = module;
      this.o = o;
      this.description = description;
    }

  }

  private Hashtable nameHash = new Hashtable();
  private Hashtable objectHash = new Hashtable();  
  
  private Vector listeners = new Vector();

  public void addEnvironmentListener(EnvironmentListener l)
  {
    listeners.add(l);
    
    // Feed with existing entries.
    for (Enumeration e = nameHash.elements() ; e.hasMoreElements() ;) 
    {
      RegEntries res = (RegEntries) e.nextElement();
      for (Enumeration er = res.entries.elements(); er.hasMoreElements() ;)
        l.entryAdded(this,(RegEntry) er.nextElement());
    }
  }

  public void removeEnvironmentListener(EnvironmentListener l)
  {
    listeners.remove(l);
  }

  private void informAdded(RegEntry re)
  {
    for (Enumeration e = listeners.elements() ; e.hasMoreElements() ;) 
      ((EnvironmentListener) e.nextElement()).entryAdded(this,re);
  }

  private void informRemoved(RegEntry re)
  {
    for (Enumeration e = listeners.elements() ; e.hasMoreElements() ;) 
      ((EnvironmentListener) e.nextElement()).entryRemoved(this,re);
  }

  private void informCleared()
  {
    for (Enumeration e = listeners.elements() ; e.hasMoreElements() ;) 
      ((EnvironmentListener) e.nextElement()).cleared(this);
  }

  /** Removes all variables from the environment.  */
  public void clear()
  {
    nameHash.clear();
    objectHash.clear();
    informCleared();
  }

    /**
     * isDefined() returns true if addEntry would succeed for this name.
     * NOTE: This may be different to what is available from getEntryByName()?
     */
    public boolean isDefined( String name, String plugin ) {
	if ( nameHash.containsKey(name) && 
	     ((RegEntries)nameHash.get(name)).entries.containsKey(plugin)) {
	    return true;
	}
	return false;
    }
    
  /** Adds an object to the environment.  Throws an exception if the name is
      already in use.
  */
  private void addEntry(String name, String plugin, Object o, String desc)
  {
    RegEntry re = new RegEntry(name,plugin,o,desc);

    if (nameHash.containsKey(name))
    {
      RegEntries res = (RegEntries) nameHash.get(name);
      if (res.entries.containsKey(plugin))
        throw new RuntimeException("Name already exists in environment: <" + name + " -> " + plugin 
				   + "(Environment.env.getEntryByName(name,plugin) == null) = "
				   + (Environment.env.getEntryByName(name,plugin) == null) );
      res.entries.put(plugin,re);
    }
    else
    {
      RegEntries res = new RegEntries(name); 
      res.entries.put(plugin,re);
      nameHash.put(name,res); 
    }

    objectHash.put(o,re);
    informAdded(re);
  }

  /** Adds an object to the environment.  Throws an exception if the name is
      already in use.
  */
  public void add(String name, String plugin, Object o, String desc)
  {
    if (o instanceof Value || o instanceof Type)
      addEntry(name,plugin,o,desc);
  }

  /** Remove an entry by name and module. */
  public void remove(String name, String module)
  {
    RegEntries res = (RegEntries) nameHash.get(name);
    RegEntry re = (RegEntry) res.entries.get(module);
    res.entries.remove(module);
    objectHash.remove(re.o);
    nameHash.remove(re.name);
    informRemoved(re);
  }

  /** Local remove by Instance */
  public void remove(Object o)
  {
    RegEntry re = (RegEntry) objectHash.get(o);
    objectHash.remove(o);
    RegEntries res = (RegEntries) nameHash.get(re.name);
    res.entries.remove(re.module);
    nameHash.remove(re.name);
    informRemoved(re);
  }

  /** Name lookup.  Returns null if the object does not exist. */
  public String getName(Object o) 
  {
    RegEntry re = (RegEntry) objectHash.get(o);
    if (re != null) return re.name; else return null;
  }

  /** Object lookup.  Returns null if name does not exist. */
  public Object getObject(String name, String module) 
  {
    RegEntry re = getEntryByName(name,module);
    if (re != null) return re.o; else return null;
  }

  /** Returns null if no entries exist. */
  public RegEntries getEntriesByName(String name) 
  {
    RegEntries res = (RegEntries) nameHash.get(name);
    if (res != null) return res; else return null;
  }

  /** Returns null if no entry exists. */
  public RegEntry getEntryByName(String name, String module) 
  {
    RegEntries res = (RegEntries) nameHash.get(name);
    if (res != null)
    {
      RegEntry re = (RegEntry) res.entries.get(module);
      return re;
    }
    else return null;
  }

  /** Returns an array (of `RegEntry's) representing  
      every type and value in the environment.
  */
  public Object[] getAllEntries()
  {
    return objectHash.values().toArray();
  }

  /** RegEntry lookup by object.  Returns null if object does not exist. */
  public RegEntry getEntryByObject(Object o) 
  {
    RegEntry re = (RegEntry) objectHash.get(o);
    if (re != null) return re; else return null;
  }

}
