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
// Functions to Augment a model.
//

// File: RodoCammlModelLearner.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.augment;

import cdms.core.*;
import camml.core.models.bNet.BNet;
import camml.core.models.cpt.CPT;
import camml.core.models.dTree.DTree; // dTree used in parameterisation
import camml.core.library.StructureFN.FastContinuousStructure;
import camml.plugin.netica.BNetNetica;

/** AugmentFN2 augments uses true model as priors for augmented model. */
public class AugmentFN2 extends Value.Function
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -4324967660555626832L;
    
    /** Initialise function type. */
    protected final static Type.Structured sTypeIn = 
        new Type.Structured( new Type[] {Type.MODEL,Type.VECTOR, Type.VECTOR},
                             new String[] {"bNet","params", "params-2"} );
    protected final static Type.Structured sTypeOut = 
        new Type.Structured( new Type[] {Type.MODEL,Type.VECTOR},
                             new String[] {"bNet","params"} );
    public final static Type.Function tt = new Type.Function(sTypeIn,sTypeOut);
    
    public final boolean uniformOverExperiments;
    
    public final boolean uniformOverInterventions;
    
    /** Static final function */
    // note : this line must remain after declaration of tt as tt used in constructor.
    public final static AugmentFN2 augment2 = new AugmentFN2(true,false);
    
    
    /** Constructor */
    public AugmentFN2( boolean uniformExpermients, boolean uniformInterventions ) { 
        super(tt);
        this.uniformOverExperiments = uniformExpermients;
        this.uniformOverInterventions = uniformInterventions;
        if ( uniformOverExperiments == false ) {
            throw new RuntimeException("Non-uniform experiments not implemented.");
        }
    }
    
    /** calls apply(model,params) */
    public Value apply(Value v) {
        Value.Structured struct = (Value.Structured)v;
        Value.Structured retVal = apply( (BNet)struct.cmpnt(0), (Value.Vector)struct.cmpnt(1),
                                         (Value.Vector)struct.cmpnt(2) ); 
        return retVal;
    } 
    
    /** Create a new type containing each entry of t1 three times. (<t1>...<t2> )*/
    public Type.Structured augmentType( Type.Structured t1 ) {
        //System.out.println( "t1 = " + t1);
        
        // Elements of Type.Structured
        // boolean[]     checkCmpntsNames
        // boolean     ckCmpnts
        // Type[]     cmpnts
        // java.lang.String[]     labels
        
        int numCmpnts = t1.cmpnts.length;
        
        Type[] cmpnts = null;
        String[] labels = null;
        boolean checkCmpntsNames[] = null;
        
        cmpnts = new Type[numCmpnts*3];
        if ( t1.labels != null ) { labels = new String[ cmpnts.length ]; }
        if ( t1.checkCmpntsNames != null ) { checkCmpntsNames = new boolean[ cmpnts.length ]; }
        
        for ( int i = 0; i < numCmpnts; i++ ) {
            cmpnts[i] = t1.cmpnts[i];
            cmpnts[i+numCmpnts] = cmpnts[i];
            
            Type.Discrete cmpnt = (Type.Discrete)cmpnts[i];
            int arity = (int)cmpnt.UPB - (int)cmpnt.LWB + 1;
            boolean ckIsCyclic = cmpnt.ckIsCyclic;
            boolean isCyclic = cmpnt.isCyclic;
            boolean ckIsOrdered = cmpnt.ckIsOrdered;
            boolean isOrdered = cmpnt.isOrdered;
            String[] newIDs;
            
            
            if ( (cmpnt instanceof Type.Symbolic) && ((Type.Symbolic)cmpnt).ids != null ) {
                String[] oldIDs = ((Type.Symbolic)cmpnt).ids;
                newIDs = new String[oldIDs.length+1];
                for ( int j = 0; j < oldIDs.length; j++ ) { newIDs[j] = oldIDs[j]; }                
                // cmpnts[i+numCmpnts] = new Type.Symbolic(ckIsCyclic, isCyclic, ckIsOrdered, isOrdered, oldIDs);
            }
            else {
                newIDs = new String[arity+1];
                for ( int j = 0; j < arity; j++ ) { newIDs[j] = "val"+j; }
            }
            newIDs[newIDs.length-1] = "No_Intervention";
            
            cmpnts[i+2*numCmpnts] = new Type.Symbolic(ckIsCyclic, isCyclic, ckIsOrdered, isOrdered, newIDs);
            
            
            if ( labels != null ) {
                labels[i] = t1.labels[i];
                labels[i+numCmpnts] = "t_" + t1.labels[i];
                labels[i+2*numCmpnts] = "int_" + t1.labels[i];
            }
            if ( checkCmpntsNames != null ) {
                checkCmpntsNames[i] = checkCmpntsNames[i+numCmpnts] = 
                    checkCmpntsNames[i+2*numCmpnts] = t1.checkCmpntsNames[i];
            }    
        }
        
        return new Type.Structured( cmpnts, labels, checkCmpntsNames );
    }
    
    /** Augment parameters.  50% chance of intervention, interventions uniform over states. 
     *  param[0-n] = Learned, params [n-2n] = true, params[2n-3n] = interventions */
    public Value.Vector augmentParams( Value.Vector params, Value.Vector params2, 
                                       Type.Structured dataType, Type.Structured augmentedType ) {
            
        int numVars = params.length();
        Value.Structured[] structArray = new Value.Structured[ numVars * 3 ];
        Type[] tArray = dataType.cmpnts;
        
        
        
        // Loop through old variables and add augmented versions.
        for ( int i = 0; i < numVars; i++ ) {
            
            // Extract type information
            // Type.Symbolic type = (Type.Symbolic)tArray[i];
            Type.Discrete type = (Type.Discrete)tArray[i];
            int arity = (int)type.UPB - (int)type.LWB + 1;
            
            // Extract old params information
            Value.Structured paramStruct = (Value.Structured)params.elt(i);
            Value.Str oldName = (Value.Str)paramStruct.cmpnt(0);
            Value.Vector oldParents = (Value.Vector)paramStruct.cmpnt(1);
            Value.Structured oldParams = (Value.Structured)paramStruct.cmpnt(2);
            
            Value.Str trueName = new Value.Str( "t_"+oldName.getString() );
            
            // build new parameterisation
            Value.Str newName = new Value.Str( "int_"+oldName.getString() );
            int[] newParentArray = new int[oldParents.length()+1];
            for ( int j = 0; j < newParentArray.length-1; j++ ) {
                newParentArray[j] = oldParents.intAt(j);
            }
            newParentArray[newParentArray.length-1] = 2*numVars+i;
            Value.Vector newParents = new VectorFN.FastDiscreteVector( newParentArray );
            
            ///////////////////////////////////////////
            // CREATE PARAMETERS FOR LEARNED NETWORK //
            ///////////////////////////////////////////
            
            
            // Parameter Space = (subModel,subParams)
            // where subParams = (splitArrtibute, [params]) for a split node
            // and subParams = (params) for a leaf node.         
            
            // model used in augmented parameterisation.
            DTree dTree = DTree.dTree;
            // split on final attribute
            Value.Discrete splitAttribute = new Value.Discrete(newParentArray.length-1);
            Value.Structured[] newParamArray = new Value.Structured[arity+1];
            
            newParamArray[arity] = new Value.DefStructured( new Value[] {
                    new Value.Discrete(-1), Value.TRIV, oldParams } );
            
            Value.Model model = camml.core.models.multinomial.MultinomialLearner.getMultinomialModel( 0, arity-1 );
            for ( int j = 0; j < arity; j++ ) {
                double[] array = new double[arity];
                array[j] = 1.0;
                Value.Structured subParams = new FastContinuousStructure(array);
                newParamArray[j] = new Value.DefStructured( new Value[] {
                        new Value.Discrete(-1), Value.TRIV,
                        new Value.DefStructured( new Value[] {model,subParams} ) } );
            }
            Value.Structured dTreeParams = new Value.DefStructured( new Value[] {splitAttribute, Value.TRIV,
                                                                                 new VectorFN.FatVector(newParamArray)} );
            
            
            structArray[i] = new Value.DefStructured( new Value[]{
                    oldName, newParents,
                    new Value.DefStructured(new Value[] {dTree,dTreeParams})} );
            
            
            
            ////////////////////////////////////////
            // CREATE PARAMETERS FOR TRUE NETWORK //
            ////////////////////////////////////////
            
            // Extract old params information
            Value.Structured trueParamStruct = (Value.Structured)params2.elt(i);
            Value.Vector trueParents = (Value.Vector)trueParamStruct.cmpnt(1);
            Value.Structured trueParams = (Value.Structured)trueParamStruct.cmpnt(2);

            // Fix parent array.
            int[] trueParentArray = new int[trueParents.length()];
            for ( int j = 0; j < trueParentArray.length; j++ ) {
                trueParentArray[j] = trueParents.intAt(j)+numVars;
            }                        
            Value.Vector trueParentVec = new VectorFN.FastDiscreteVector( trueParentArray );
            structArray[i+numVars] = new Value.DefStructured( new Value[]{
                    trueName, trueParentVec, trueParams } );

            
            
            //////////////////////////////////////////////
            // CREATE PARAMETERS FOR INTERVENTION NODES //
            //////////////////////////////////////////////
            Value.Vector intParents = new VectorFN.FastDiscreteVector( new int[] {i+numVars} );  
            
            Type.Discrete parentType = (Type.Discrete)tArray[i];
            int parentArity = (int)parentType.UPB - (int)parentType.LWB + 1;

            ///** Make a (cptModel, cptParams) struct from a list of doubles and input/output types */
            //public static Value.Structured makeCPTStruct( double[][] p, Type.Discrete x, Type.Structured z )

            // Parameterise as 50% chance of passing value, 50% chance of no intervention.
            double[][] cptParamArray = new double[parentArity][arity+1];
            for ( int j = 0; j < cptParamArray.length; j++ ) {
                cptParamArray[j][j] = 0.5;
                cptParamArray[j][arity] = 0.5;
            }
            Type.Structured parentTypeStruct = new Type.Structured( new Type[] { parentType } );
            Type.Discrete intType = (Type.Discrete)augmentedType.cmpnts[i+2*numVars];
            
            structArray[i+2*numVars] = new Value.DefStructured( new Value[] {
                    newName, intParents, CPT.makeCPTStruct(cptParamArray,
                                                           intType, parentTypeStruct)} );
            /*
              Value.Model model2 = 
              camml.core.models.multinomial.MultinomialLearner.getMultinomialModel( 0, arity );
              double[] array = new double[arity+1];
            
              for ( int j = 0; j < arity; j++ ) {
              array[j] = 0.5/arity;
              }
              array[arity] = 0.5;
              Value.Structured subParams = new FastContinuousStructure(array);
              structArray[i+2*numVars] = new Value.DefStructured( new Value[]{
              newName, new VectorFN.FastDiscreteVector( new int[] {}),
              new Value.DefStructured( new Value[] {model2,subParams} ) } );
            */
            

            

            
            // structArray[i+numVars] = (Value.Structured)params2.elt(i);
            //structArray[i+0*numVars] = (Value.Structured)params2.elt(i);
            //structArray[i+2*numVars] = (Value.Structured)params2.elt(i);
            
            
            
            
            //         System.out.println( "arity = " + arity );
            //         System.out.println( "dTreeParams = " + dTreeParams );
            //         System.out.println( "oldParams = " + oldParams );
            
            
            
            
            
            
            
            
            //         System.out.println( newName );
            //         System.out.println( newParents );
            //         System.out.println( newParams );
        }
        
        return new VectorFN.FatVector( structArray );
    }
    
    /** Take a model and params returning (model,params) struct, params2 will generally represent
     *   the true network and params a learned network. */
    public Value.Structured apply( BNet orig_model, Value.Vector params, Value.Vector params2)
    {
        // Augment the data type.
        Type.Structured xType = (Type.Structured)((Type.Model)orig_model.t).dataSpace;
        Type.Structured xType2 = augmentType( xType );
        
        BNet bNetAugmented;
        if ( orig_model instanceof BNetNetica) {
            bNetAugmented = new camml.plugin.netica.BNetNetica( xType2 );
        }
        else {
            bNetAugmented = new camml.core.models.bNet.BNetStochastic( xType2 );
        }
        
        
        
        
        //     System.out.println("\nXType = " + xType);
        //     System.out.println("\nXType2 = " + xType2);
        //     System.out.println("\nbNetAug = " + bNetAugmented );
        //     System.out.println("\n\n");
        
        //     System.out.println();
        //     for ( int i = 0; i < params.length(); i++ ) {
        //         System.out.println( params.elt(i) );
        //     }
        
        //     System.out.println("\n-- original params --\n" + params + "\n");
        
        
        Value.Vector augmentedParams = augmentParams( params, params2, xType, xType2 );
        
        //     System.out.println("\n-- augmented params --\n" + augmentedParams + "\n");
        
        return new Value.DefStructured( sTypeOut, new Value[]{bNetAugmented,augmentedParams} );
    }
    
}

