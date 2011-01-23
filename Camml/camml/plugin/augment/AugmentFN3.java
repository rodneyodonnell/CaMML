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
//Functions to Augment a model.
//

//File: AugmentFN4.java
//Author: rodo@csse.monash.edu.au

package camml.plugin.augment;

import cdms.core.*;
import camml.core.models.bNet.BNet;
import camml.plugin.netica.BNetNetica;

/**
 * AugmentFN4 augments uses true model as priors for augmented model. AugmentFN4
 * is similar to AugmentFN2 except instead of a uniform prior over the
 * intervention set, the only valid intervention sets intervene on all except
 * one nodes. Our prior is uniform ovet the non-intervened on node. <br>
 * 
 * This is an alternate implementation of AugmentFN3 which requires less
 * arcs and less parameters. <br>
 *
 * Node <0..k-1> represents the variables from the true network and
 * Node <k..2k-1> is the learned network.  All variables in the "learned"
 * network are attached to parents in the "true" network.  This gives us
 * the correct distribution over interventions without the need for a
 * selector variable.  The value of CKL3(P,Q) = CKL4(P,Q)/numVars.
 *  This iccurs as all variables are examined simultaneously instead
 *  of one at a time as CKL3 with a selector does.
 * 
 */
public class AugmentFN3 extends Value.Function {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7835992408377522096L;

    /** Initialise function type. */
    protected final static Type.Structured sTypeIn = new Type.Structured(
                                                                         new Type[] { Type.MODEL, Type.VECTOR, Type.VECTOR }, new String[] {
                                                                             "bNet", "params", "params-2" });

    protected final static Type.Structured sTypeOut = new Type.Structured(
                                                                          new Type[] { Type.MODEL, Type.VECTOR }, new String[] { "bNet",
                                                                                                                                 "params" });

    public final static Type.Function tt = new Type.Function(sTypeIn, sTypeOut);

    /** Static final function */
    // note : this line must remain after declaration of tt as tt used in
    // constructor.
    public final static AugmentFN3 augment3 = new AugmentFN3();

    /** Constructor */
    public AugmentFN3() { super(tt); }

    /** calls apply(model,paramsP, paramsQ) */
    public Value apply(Value v) {
        Value.Structured struct = (Value.Structured) v;
        Value.Structured retVal = apply((BNet) struct.cmpnt(0),
                                        (Value.Vector) struct.cmpnt(1), (Value.Vector) struct.cmpnt(2));
        return retVal;
    }

    /** Create a new type containing each entry of t1 twice. (<t1>...<t2> ) */
    public Type.Structured augmentType(Type.Structured t1) {
        int numCmpnts = t1.cmpnts.length;

        Type[] cmpnts = null;
        String[] labels = null;
        boolean checkCmpntsNames[] = null;

        cmpnts = new Type[numCmpnts * 2];
        if (t1.labels != null) {
            labels = new String[cmpnts.length];
        }
        if (t1.checkCmpntsNames != null) {
            checkCmpntsNames = new boolean[cmpnts.length];
        }

        for (int i = 0; i < numCmpnts; i++) {
            cmpnts[i] = t1.cmpnts[i];
            cmpnts[i + numCmpnts] = cmpnts[i];

            if (labels != null) {
                labels[i] = "t_" + t1.labels[i];
                labels[i + numCmpnts] = t1.labels[i];
            }
            if (checkCmpntsNames != null) {
                checkCmpntsNames[i] = checkCmpntsNames[i + numCmpnts] = t1.checkCmpntsNames[i];
            }
        }

        return new Type.Structured(cmpnts, labels, checkCmpntsNames);
    }

    /**
     * Top k nodes is the true network.
     * Next k nodes is the learned network with connections to parents in the true net
     * instead of the learned net.  This is much more straight forward than other
     * augmentation functions as parameters remain unchanged.
     */
    public Value.Vector augmentParams(Value.Vector paramsQ,
                                      Value.Vector paramsP, Type.Structured dataType,
                                      Type.Structured augmentedType) {

        int numVars = paramsP.length();
        Value.Structured[] structArray = new Value.Structured[numVars * 2];

        
        // Loop through old variables and add augmented versions.
        for (int i = 0; i < numVars; i++) {

            // Extract old params information
            Value.Structured pStruct = (Value.Structured) paramsP.elt(i);
            Value.Structured qStruct = (Value.Structured) paramsQ.elt(i);
            Value.Vector pParents = (Value.Vector) pStruct.cmpnt(1);
            Value.Vector qParents = (Value.Vector) qStruct.cmpnt(1);
            Value.Str name = (Value.Str)pStruct.cmpnt(0);
            Value.Str t_Name = new Value.Str( "t_" + name.getString() );
            Value.Structured pParams =  (Value.Structured) pStruct.cmpnt(2);
            Value.Structured qParams =  (Value.Structured) qStruct.cmpnt(2);
            
            // "True" part of network is identical to paramsP
            structArray[i] = new Value.DefStructured(
                                                     new Value[] { t_Name, pParents, pParams } );
            
            // "Learned" part of network is identical to paramsQ, but with
            // parents hooked up to valued in paramsP.
            structArray[i+numVars] = new Value.DefStructured(
                                                             new Value[] { name, qParents, qParams } );
                            
        }
    
        return new VectorFN.FatVector(structArray);
    }

    /**
     * Take a model and params returning (model,params) struct, params2 will
     * generally represent the true network and params a learned network.
     */
    public Value.Structured apply(BNet orig_model, Value.Vector paramsP,
                                  Value.Vector paramsQ) {
        // Augment the data type.
        Type.Structured xType = (Type.Structured) ((Type.Model) orig_model.t).dataSpace;
        Type.Structured xType2 = augmentType(xType);

        BNet bNetAugmented;
        if (orig_model instanceof BNetNetica) {
            bNetAugmented = new camml.plugin.netica.BNetNetica(xType2);
        } else {
            bNetAugmented = new camml.core.models.bNet.BNetStochastic(xType2);
        }

        Value.Vector augmentedParams = augmentParams(paramsP, paramsQ, xType, xType2);

        return new Value.DefStructured(sTypeOut, new Value[] { bNetAugmented,
                                                               augmentedParams });
    }

}
