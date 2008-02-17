//
// Non-Lazy version of MAP
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: EMap.java
// Author: rodo@dgs.monash.edu.au

package camml.core.library;

import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;
import cdms.core.Value.Function;

/**
 * Eager (ie. non lazy) version of Map
 * @see cdms.core.VectorFN.Map
 * 
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.3 $ $Date: 2006/08/22 03:13:25 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/library/EMap.java,v $
 */
public class EMap extends Function {


	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7796125081018544902L;

	final static Type.Function t = 
		new Type.Function(Type.FUNCTION,EMapF.t);
	
	/** Static instance of EMap */
	public static final EMap emap = new EMap();

	public EMap() {	super(t); }

	public Value apply(Value f)	{ 
		return new EMapF((Value.Function)f); 
	}

	/** Convenience function for EMap*/
	public static Value.Vector _apply(Value.Function f, Value.Vector vec) {
		Value array[] = new Value[vec.length()];
		for ( int i = 0; i < array.length; i++ ) {
			array[i] = f.apply(vec.elt(i));
		}
		return new VectorFN.FatVector(array);	
	}
	
	/** Curried fn required for EMap*/
	private static class EMapF extends Value.Function
	{ 
		/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -486158663954200387L;

		final static Type.Function t = 
			new Type.Function(Type.VECTOR,Type.VECTOR);
		
		private Value.Function f;

		public EMapF(Value.Function f)
		{ 
			super(t);
			this.f = f;
		}
		
		public Value apply(Value v)
		{ 
			Value.Vector vec = (Value.Vector) v;
			return EMap._apply(f,vec);
		}
	}
}
