//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Module.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.lang.reflect.*;

/** Modules represent the packaging of type and values together.  We use Haskell style
    modules which provide a flat namespace.  Modules will typically be installed by the
    system at startup (in the bootstrap).  Those wishing to package together a set
    of types and values will either extend the Module class or extend the StaticFunctionModule
    class which is a non-abstract descendant of Module which adds to the environment
    all static Value.Function variables in a set of names classes.
*/
public abstract class Module
{
  private static java.util.Vector<Module> installed = new java.util.Vector<Module>();

  public static Module getModuleByName(String name)
  {
    for (int i = 0; i < installed.size(); i++)
      if (installed.elementAt(i).getModuleName().compareTo(name) == 0)
        return installed.elementAt(i);
    return null;
  }

  /** The name of this module. */
  public abstract String getModuleName();

  /** The HTML help for this module. */
  public abstract java.net.URL getHelp();

  /** Creates a URL for a standard module.  The help URL is created using the class 
      package name: file://doc/api-docs/packagename/package-frame.html 
  */ 
  public static java.net.URL createStandardURL(Class c) 
  {
    try
    {
      return new java.net.URL("file:doc/api-docs/" + 
                              c.getPackage().getName().replace('.','/') + 
                              "/package-frame.html");
    }
    catch (Exception e)
    {
      System.out.println("Bad help URL for Java package " + c.getPackage());
      return null;
    }
  }

  /** Installs the module into the environment. */
  public abstract void install(Value params) throws Exception;

  /** Convenience method for adding values and types to the environment using the module
      name. 
  */
  public void add(String name, Object o, String desc)
  {
    Environment.env.add(name,getModuleName(),o,desc);
  }

  /** A simple function which given a pair whose first entry is the full class name of the
      module and whose second entry is a value representing parameters for the install method,
      installs the module: <code>(String,t) -> ()</code>
      <p>
      Throws an exception if the module cannot be installed. 
      <p>
      Note: There is no need to create an instance of InstallModule since this will 
      normally be the first function to be created in the bootstrap using <code>cv</code>.
  */
  public static class InstallModule extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8487047787911079399L;
	public static Type.Function thisType = 
      new Type.Function(new Type.Structured(new Type[] { Type.STRING, Type.TYPE },
                                            new boolean[] { false, false }),
                        Type.TRIV,false,false);

    public InstallModule()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      String className = ((Value.Str) ((Value.Structured) v).cmpnt(0)).getString();
      Value param = ((Value.Structured) v).cmpnt(1);

      try
      {
        Module m = (Module) Class.forName(className).newInstance();
        System.out.println("Installing module " + m.getModuleName() + " from " + className);
        m.install(param);
        installed.add(m);
      }
      catch (Exception e)
      {
	  throw new RuntimeException(e);
	  // throw new RuntimeException("Unable to install module " + className + ".  " + e.toString());
      }

      return Value.TRIV;
    }

  }

  /** A concrete version of Module which installs all Value.Function members found
      in a set of named classes. 
  */
  public static class StaticFunctionModule extends Module
  {
    protected String moduleName;
    protected java.net.URL help;
    protected Class c[];

    public StaticFunctionModule(String moduleName, java.net.URL help, Class c)
    {
      this.moduleName = moduleName;
      this.help = help;
      this.c = new Class[] { c };
    }

    public StaticFunctionModule(String moduleName, java.net.URL help, Class c[])
    {
      this.moduleName = moduleName;
      this.help = help;
      this.c = c;
    }
   
    public String getModuleName() { return moduleName; }
    public java.net.URL getHelp() { return help; }

    public void install(Value params) throws IllegalAccessException
    {
      for (int j = 0; j < c.length; j++)
      {
        Field fields[] = c[j].getFields();

        for (int i = 0; i < fields.length; i++)
        {
          if (Value.Function.class.isAssignableFrom(fields[i].getType()) &&  // Only FN's.
              Modifier.isStatic(fields[i].getModifiers()))
          {
            Environment.env.add(fields[i].getName(),getModuleName(),(Value) fields[i].get(null),
                                fields[i].getName() + " function");
          }
        }
      }
    }

  }

}
