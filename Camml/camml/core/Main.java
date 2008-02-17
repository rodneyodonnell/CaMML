//
// Main entry point for CaMML.
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: Main.java
// Author: rodo@dgs.monash.edu.au

package camml.core;

import cdms.core.*;
import cdms.plugin.fpli.Fpli;
import java.io.*;

/** */
public class Main
{
	public static void main(String args[]) throws Exception
	{
		try	{
			// Load interpreted
			Value.Function interpreter = new Fpli.Interpreter();
				
			String script = "/script/cammlBootstrap.fp";
			if ( args.length > 0 ) {
				script = args[0];
			}

			// Attempt to open script in jarfile.
			InputStream is = Main.class.getResourceAsStream(script);
			
			// If not in jarfile, look in the regular file system.
			if ( is == null ) {
				is = new FileInputStream( script );
			}

			// Read stream into sb.
			StringBuffer sb = new StringBuffer();
			int chr;
			

			while ((chr = is.read()) != -1) {				
				sb.append((char) chr);
			}
			is.close();
      
			// Interpret the script.
			Value res = interpreter.apply(new Value.Str(sb.toString()));
			System.out.println("\n" + res.toString() + "\n");
	  
		}
		catch(Exception e)
			{
				if (true) throw new RuntimeException(e);
				System.out.println("An error occured during startup.\n");
				e.printStackTrace();
				System.exit(1);
			} 
		
	} 
	
}

