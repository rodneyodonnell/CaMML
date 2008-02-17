//
// Friedman wrapper plugin.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: RodoCammlModelLearner.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.friedman;

import cdms.core.*;
import camml.core.models.ModelLearner;
import java.io.*;

/**
 *  ModelLearner wrapping around Friedman's BNet code.
 */
public class FriedmanLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3790372842332418408L;
	public String getName() { return "FriedmanLearner"; }    



    // public static FriedmanLearner modelLearner = new FriedmanLearner( );
    public static FriedmanLearner modelLearner_BDE = new FriedmanLearner( "" );
    public static FriedmanLearner modelLearner_MDL = new FriedmanLearner( "-t I" );

    public static FriedmanLearner modelLearner_MDL_Tree = new FriedmanLearner( "-t I -T" );

    public static FriedmanLearner modelLearner_Default = new FriedmanLearner( "-D" );
    public static FriedmanLearner modelLearner_Tree = new FriedmanLearner( "-T" );
    public static FriedmanLearner modelLearner_BinaryTree = new FriedmanLearner( "-Tb" );
    public static FriedmanLearner modelLearner_Fixed = new FriedmanLearner( "-F" );
    public static FriedmanLearner modelLearner_NaiveBayes = new FriedmanLearner( "-t N <class>" );
    public static FriedmanLearner modelLearner_TAN = new FriedmanLearner( "-t F <class>" );
    public static FriedmanLearner modelLearner_CLTree = new FriedmanLearner( "-t T" );

    public final String options;
    /** Constuctor currently only specifies Type.MODEL, this needs to be fixed. */
    public FriedmanLearner( )
    {
	super( Type.MODEL, Type.TRIV );
	options = "";
    }

    /** Constuctor currently only specifies Type.MODEL, this needs to be fixed. */
    public FriedmanLearner( String options )
    {
	super( Type.MODEL, Type.TRIV );
	this.options = options;
    }


    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
    {
 	if ( x.length() != z.length() ) {
 	    throw new RuntimeException("Length mismatch in FriedmanLearner.parameterize");
 	}

	try {
		java.util.Random rand = new java.util.Random();
		String prefix = "temp" + rand.nextInt();

	    // Create a temporary file in the current directory.
	    FileWriter nameFile = new FileWriter( prefix+".names" );
	    FileWriter dataFile = new FileWriter( prefix+".data" );	    
	    FriedmanWrapper.writeCases( x, dataFile, nameFile );
		
	    System.out.println("-----------------------------------------------------------------");
	    System.out.println("---      Running LearnBayes                                   ---");
	    System.out.println("-----------------------------------------------------------------");

	    Type.Structured dataType = (Type.Structured)((Type.Vector)x.t).elt;
	    String[] names = dataType.labels;

	    // replace <class> with name of final variable in file.
	    String options = this.options.replaceAll("<class>",names[names.length-1] );

	    // set up LearnBayes process
	    String cmd = "LearnBayes " + options + " -o "+prefix+".net "+prefix;
	    System.out.println("Running : " + cmd);
	    final Process p = Runtime.getRuntime().exec( cmd );
	    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

	    // TODO: calling LearneBayes doesn't seem to terminate anymore?  Why?
	    Thread t = new Thread() { public void run() { 
		System.out.println("Running thread");
		try {
		    p.waitFor();
		} catch ( Exception e ) { throw new RuntimeException(e); }
	    } };
	    t.start();

	    while ( t.isAlive() ) {
		while( in.ready() ) { System.out.println( in.readLine() ); }
		while( err.ready() ) { System.err.println( err.readLine() ); }
	    }

	    // load netica network from file. returns (model,params)
	    Value.Structured my = 
		(Value.Structured)FriedmanWrapper.readNetwork( new FileReader(prefix+".net") );

		// Delete temporary files.
		new File(prefix+".net").delete();
		new File(prefix+".names").delete();
		new File(prefix+".data").delete();

	    // turn results into a MSY struct.
	    Value.Model m = (Value.Model)my.cmpnt(0);
	    Value s = m.getSufficient( x, z );
	    Value y = my.cmpnt(1);


		
	    // Is this necesarry??
	    //Value.Structured reorderedMYStruct = 
		//NeticaFn.ReorderNet.apply( names, new Value.DefStructured( new Value[] {m,y} ) );

	    return new Value.DefStructured( new Value[] {m, s, y,} );

	}
	catch ( Exception e ) {
	    throw new RuntimeException(e);
	}

    }


    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
    {
	throw new RuntimeException("Not implemented");
    }

    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
    {	
	return parameterize( Value.TRIV, 
			     (Value.Vector)((Value.Structured)s).cmpnt(0), 
			     (Value.Vector)((Value.Structured)s).cmpnt(1) );
    }



    /**
     * return cost.  This is read directly out of parameters.  Ideally it should be calculated
     * using parameters and data as currently it entirely ignores data.
     */
    public double sCost(Value.Model m, Value s, Value y)
    {	
	throw new RuntimeException("Not implemented");
    } 


    public String toString() { return "FriedmanLearner"; }    

}

