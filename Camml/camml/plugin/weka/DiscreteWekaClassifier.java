//
// Model wrapper for Weka Classifiers
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: DisceteWekaClassifier
// Author: rodo@csse.monash.edu.au


package camml.plugin.weka;

import java.util.Random;
import cdms.core.*;
import cdms.core.Value.*;


/**
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.3 $ $Date: 2006/08/22 03:13:35 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/weka/DiscreteWekaClassifier.java,v $
*/
public class DiscreteWekaClassifier extends Model {
	/*
	public static void printMemFree() {
		System.gc();
		Runtime rt = Runtime.getRuntime();
		System.out.println( rt.totalMemory()-rt.freeMemory());
	}
	*/
	
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5839554896332542127L;
	/** t = (DISCRETE,OBJ,STRUCTURED,STRUCTURED)*/
	public static final Type.Model t = new Type.Model(Type.DISCRETE,Type.OBJECT,Type.STRUCTURED,Type.STRUCTURED);
	
	
	public DiscreteWekaClassifier() { super(t); }
	
	public double logP(Value x, Value y, Value z) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Vector generate(Random rand, int n, Value y, Value z) {
		// TODO Auto-generated method stub
		return null;
	}

	public Value predict(Value y, Value z) {
		// TODO Auto-generated method stub
		return null;
	}

	public Vector predict(Value y, Vector z) {
		// TODO Auto-generated method stub
		return null;
	}

	public Value getSufficient(Vector x, Vector z) {
		// TODO Auto-generated method stub
		return null;
	}

	public double logPSufficient(Value s, Value y) {
		// TODO Auto-generated method stub
		return 0;
	}

}
