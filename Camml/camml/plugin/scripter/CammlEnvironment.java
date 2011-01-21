//
// Camml plugin for CDMS
//
// Author        : Rodney O'Donnell
// Last Modifies : 21-5-02
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: CammlEnvironment.java
// Contains the camml environment.  This borrows heavily from cdms.plugin.fpli.Environment
//

// package cdms.plugin.cammlPlugin;
//package camml.cdmsPlugin.scripter;
package camml.plugin.scripter;

import cdms.plugin.fpli.*;


/** Defines the CammlEnvironment in which all variables related to Camml are stored. */
public class CammlEnvironment extends cdms.plugin.fpli.Environment
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 3674001595169857739L;

    public CammlEnvironment() {
        super (null);
    }

    public CammlEnvironment(Environment parent)    {
        super ( parent );
    }

    /** Add object to hashtable */
    public void add(String name, Object o)    {
        nameHash.put(name,o);
    }
}
