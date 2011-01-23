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
// Model wrapper for Weka Classifiers
//

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
