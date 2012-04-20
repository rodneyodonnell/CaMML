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
// Wrapper for cdms.mml87.multinomialParameterizer
//

// File: MLMultinomialLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import camml.core.library.StructureFN;
import camml.core.models.ModelLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.plugin.model.Multinomial;

// import cdms.plugin.mml87.*;

/**
 * MLMultinomialLearner is a wrapper class of type ModelLearner. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br>
 */
public class MLMultinomialLearner extends ModelLearner.DefaultImplementation {
    /**
     * Serial ID required to evolve class while maintaining serialisation compatibility.
     */
    private static final long serialVersionUID = 377380353677126451L;
    /**
     * Static instance of class
     */
    public static MLMultinomialLearner mlMultinomialLearner = new MLMultinomialLearner();

    public String getName() {
        return "MLMultinomialLearner";
    }


    public MLMultinomialLearner() {
        // super( modelType, iType )
        super(new Type.Model(Type.DISCRETE, Type.STRUCTURED, Type.TRIV, Type.STRUCTURED)
                , Type.TRIV);
    }

    /**
     * Parameterize and return (m,s,y)
     */
    public Value.Structured parameterize(Value i, Value.Vector x, Value.Vector z) {
        Type.Discrete xType = (Type.Discrete) ((Type.Vector) x.t).elt;
        Value.Model multinomialModel = new Multinomial((int) xType.LWB, (int) xType.UPB);
        Value.Structured stats = (Value.Structured) multinomialModel.getSufficient(x, z);
        return sParameterize(multinomialModel, stats);
    }


    /**
     * Parameterize and return (m,s,y)
     */
    public Value.Structured sParameterize(Value.Model model, Value s) {
        Value.Structured stats = (Value.Structured) s;

        double params[] = new double[stats.length()];
        double total = 0;

        for (int i = 0; i < params.length; i++) {
            params[i] = stats.doubleCmpnt(i);
            total += params[i];
        }

        // find MML estimate of params[i]
        for (int i = 0; i < params.length; i++) {
            params[i] = (params[i]) / total;
        }

        return new Value.DefStructured(new Value[]{
                model, stats, new StructureFN.FastContinuousStructure(params)});
    }


    /**
     * return cost
     */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value y) {
        Type.Discrete xType = (Type.Discrete) ((Type.Vector) x.t).elt;
        Value.Model multinomialModel = new Multinomial((int) xType.LWB, (int) xType.UPB);
        Value.Structured stats = (Value.Structured) multinomialModel.getSufficient(x, z);

        return sCost(m, stats, (Value.Structured) y);
    }

    /**
     * return cost
     */
    public double sCost(Value.Model m, Value stats, Value params) {
        Value.Structured s = (Value.Structured) stats;
        Value.Structured y = (Value.Structured) params;

        double total = 0;
        for (int i = 0; i < s.length(); i++) {
            int count = s.intCmpnt(i);

            if (count != 0) {
                total -= java.lang.Math.log(y.doubleCmpnt(i)) * count;
            }
        }
        return total;

    }

    public String toString() {
        return "MLMultinomialLearner";
    }
}

