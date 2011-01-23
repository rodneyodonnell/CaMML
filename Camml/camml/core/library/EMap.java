/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// Non-Lazy version of MAP
//

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

    public EMap() {    super(t); }

    public Value apply(Value f)    { 
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
