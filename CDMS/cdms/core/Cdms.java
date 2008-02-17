//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Cdms.java
// Authors: {leighf}@csse.monash.edu.au

package cdms.core;

import java.io.*;

/** A bootstrap class for starting CDMS.  The first argument is the full name of the class
    to use as an interpreter.  The interpreter is expected to be a function with type <code>String -> t</code>.
*/
public class Cdms
{
  public static void main(String args[]) throws Exception
  {
    try
    {
      // Load the interpreter.
      System.out.println("Loading interpreter: " + args[0]);
      Value.Function interpreter = (Value.Function) Class.forName(args[0]).newInstance();

      // Load the script. 
      File f = new File(args[1]);
      FileInputStream fis = new FileInputStream(f);
      byte b[] = new byte[ (int) f.length() ];
      fis.read(b);
      fis.close();
      
      
      // Interpret the script.
      Value res = interpreter.apply(new Value.Str(new String(b)));
      System.out.println("\n" + res.toString() + "\n");
    
    }
    catch(Exception e)
    {
      System.out.println("An error occured during startup.\n");
      e.printStackTrace();
      System.exit(1);
    } 

  } 

}
