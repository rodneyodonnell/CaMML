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
// Random data generation class.
//

// File: MetropolisSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import cdms.core.*;
import camml.core.models.cpt.CPT;
import cdms.plugin.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 *  SearchDataCreator creates (or loads) datasets of various types to test different search
 *  strategies with.
 */
public class SearchDataCreator {
    protected static final int defaultNumSamples = 1000;
    
    protected static Value.Vector commonCauseDataset;
    protected static Value.Vector fourVariableCommonCauseDataset;
    protected static Value.Vector wallaceKorbStyleDataset;
    protected static Value.Vector uncorrelatedDataset;
    protected static Value.Vector bivariateDataset;
    
    /**
     * Returns the commonCauseDataset.  3 binary variables. <br>
     *  0 -> 1 <br>
     *  2-> 1 <br>
     */
    public static Value.Vector getCommonCauseDataset() {
        if (commonCauseDataset == null) {
            commonCauseDataset = generateReducedCommonCauseDataset( new java.util.Random(123), 
                                                                    defaultNumSamples);
        }
        return commonCauseDataset;        
    }
    
    /**
     * Returns the fourVariableCommonCauseDataset.
     *  0 -> 1 <br>
     *  2 -> 1 <br>
     *  3 uncorrelated.
     */
    public static Value.Vector getFourVariableCommonCauseDataset() {
        if (fourVariableCommonCauseDataset == null) {
            fourVariableCommonCauseDataset = generateCommonCauseDataset( new java.util.Random(1234), defaultNumSamples );
        }
        return fourVariableCommonCauseDataset;
    }
    
    /**
     *  load the C5 file specified in fileName and return it.
     */
    public static Value.Vector getC5Dataset( String fileName ) {
        return (Value.Vector)cdms.plugin.c5.C5.c5loader.apply( new Value.Str(fileName) );
    }
    
    /**
     * Return a dataset simitar to that described in wallace&korb 99.
     * Basically a grid of N*N*N variables, each is connected to the s
     */
    public static Value.Vector getWallaceKorbStyleDataset() {
        if (wallaceKorbStyleDataset == null) {
            wallaceKorbStyleDataset = 
                generateWallaceKorbStyleDataset( new java.util.Random(123), defaultNumSamples*10,
                                                 2,3,3);  // length, width, height
        }
        return wallaceKorbStyleDataset;
    }
    
    /**
     *  Return a 3 variable uncorrelated dataset of arity 3,2 and 5 respectively.
     */
    public static Value.Vector getUncorrelatedDataset() {
        if (uncorrelatedDataset == null) {
            uncorrelatedDataset = generateUncorrelatedDataset( new java.util.Random(1234), 
                                                               defaultNumSamples,
                                                               new int[] {3,2,2} );
        }
        return uncorrelatedDataset;
    }
    
    /**
     * Generate a dataset as found in WallaceKorb99. <br>
     * The network involved contains 27 (3*3*3) variables in a grid indexed by i,j,k such that there
     * is a link from (i,j,k)->(i+1,j,k), (i,j,k)->(i,j+1,k), (i,j,k) -> (i,j,k+1).  
     * In WallaceKorb99 the nodes are linear, so we cannot use the same parameters.  Instead we 
     * make all variables binary and associate random values uniform between 0 and 1 to each weight.
     */
    public static Value.Vector generateWallaceKorbStyleDataset( java.util.Random rand, 
                                                                final int numSamples, int length, int width, int height)
    {
        return generateWallaceKorbStyleDataset(rand,numSamples,length,width,height,false);
    }
    
    /**
     * Generate a dataset as found in WallaceKorb99. <br>
     * The network involved contains 27 (3*3*3) variables in a grid indexed by i,j,k such that there
     * is a link from (i,j,k)->(i+1,j,k), (i,j,k)->(i,j+1,k), (i,j,k) -> (i,j,k+1).  
     * In WallaceKorb99 the nodes are linear, so we cannot use the same parameters.  Instead we 
     * make all variables binary and associate random values uniform between 0 and 1 to each weight.
     */
    public static Value.Vector generateWallaceKorbStyleDataset( java.util.Random rand, 
                                                                final int numSamples,
                                                                int length, int width, int height, boolean neticaSafeNames)
    {
        // parameters of each node indexed by (i,j,k)
        Value cptParams[][][] = new Value[width][height][length];
        
        // Vector of generated data
        Value.Vector dataVector[][][] = new Value.Vector[width][height][length];
        
        // The CPT used by the given node.
        CPT cpt[][][] = new CPT[width][height][length];
        
        // Create a binomial distribution to use with CPTs
        Multinomial binomial = new Multinomial(0,1);
        
        // We need a different form of CPT model for 0-3 parents.  
        //  Each uses a binomial as its child model.        
        CPT zeroParentCPT  = new CPT(binomial, new int[] { },           new int[] { });
        CPT oneParentCPT   = new CPT(binomial, new int[] { 0 },        new int[] { 1 });
        CPT twoParentCPT   = new CPT(binomial, new int[] { 0, 0 },    new int[] { 1, 1 });
        CPT threeParentCPT = new CPT(binomial, new int[] { 0, 0,0 }, new int[] { 1, 1, 1 });
        
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < length; k++) {
                    // calculate how many parameters are required in this CPT
                    int numParams = 1;
                    if (i != 0)  { numParams *= 2; }
                    if (j != 0)  { numParams *= 2; }
                    if (k != 0) { numParams *= 2; }
                    
                    // Put the correct CPT into place.
                    if ( numParams == 1 ) {cpt[i][j][k] = zeroParentCPT; }
                    if ( numParams == 2 ) {cpt[i][j][k] = oneParentCPT; }
                    if ( numParams == 4 ) {cpt[i][j][k] = twoParentCPT; }
                    if ( numParams == 8 ) {cpt[i][j][k] = threeParentCPT; }
                    
                    
                    // create an array storing parameters for the current CPT.  
                    // Each space in the CPT needs a binomial parameter.
                    Value[] cptParamArray = new Value[numParams];
                    for (int n = 0; n < cptParamArray.length; n++) {
                        double[] binomialParamArray = new double[2];
                        binomialParamArray[0] = rand.nextDouble();
                        
                        binomialParamArray[1] = 1 - binomialParamArray[0];
                        cptParamArray[n] = makeStruct( binomialParamArray );
                        
                        // System.out.print( "" + binomialParamArray[0]  + '\t' );
                    }
                    //             System.out.println();
                    
                    // make CDMS stype parameters for CPT
                    cptParams[i][j][k] = makeCPTParams(binomial, cptParamArray);
                    
                    // parentVector contains data from parents (i-1,j,k),(i,j-1.k),(i,j,k-1), if any
                    // of these do not exist then they are not included.  This gets a bit confusing 
                    // as we have a java.util.Vector of cdms.Value.Vector
                    // To reduce confusion I attempt to use the fully qualified names.
                    ArrayList<Value.Vector> parentVec = new ArrayList<Value.Vector>();
                    if (i != 0)  { parentVec.add(dataVector[i-1][j][k]); }
                    if (j != 0)  { parentVec.add(dataVector[i][j-1][k]); }
                    if (k != 0) { parentVec.add(dataVector[i][j][k-1]); }
                    
                    // this is the CDMS vector of parent values
                    Value[] tempValue = new Value[parentVec.size()];
                    parentVec.toArray(tempValue);
                    
                    // slight hash used to make sure the currect vector length is maintained.
                    // without this, a vector with no columns cannot find it's length;
                    Value.Vector parentVector = 
                        new VectorFN.MultiCol( new Value.DefStructured( tempValue ) ) {
                            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
                            private static final long serialVersionUID = -8053566463894679406L;

                            public int length() { return numSamples; }
                        };
                    
                    dataVector[i][j][k] = cpt[i][j][k].generate( rand,
                                                                 cptParams[i][j][k],
                                                                 parentVector );
                }
            }
        }
        
        // take dataVector[][][] and turn it into a single dimensional array dataVectorArray[]
        Value.Vector[] dataVectorArray = new Value.Vector[width * height * length];
        String[] name = new String[ width * height * length ];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < length; k++) {
                    dataVectorArray[i * height * length + j * length + k] = dataVector[i][j][k];
                    if (neticaSafeNames) {
                        name[ i * height * length + j * length + k ] = "_"+i+"_"+j+"_"+k+"_";
                    }
                    else {
                        name[ i * height * length + j * length + k ] = "("+i+","+j+","+k+")";
                    }
                }
            }
        }
        
        return new VectorFN.MultiCol( new Value.DefStructured( dataVectorArray, name ));
    }
    
    /*
     * Generate a vector of uncorrelated data of length numSamples using given seed.  Each attribute
     *  is discrete and uniformly distributed with an arity of arity[i].  The numbe of columns is
     *  determined by arity.length.
     */
    public static Value.Vector generateUncorrelatedDataset(  Random rand,    int numSamples,    int[] arity)
    {
        Value.Vector[] vectorArray = new Value.Vector[arity.length];
        Multinomial.MultinomialCreator multinomialCreator =
            new Multinomial.MultinomialCreator();
        
        /*
          Multinomial multinomialModel =
          (Multinomial) multinomialCreator.apply( new Value.DefStructured( new Value[] {
          new Value.Discrete(0),
          new Value.Discrete(1)
          }));
        */
        
        Value.Discrete zero = new Value.Discrete(0);
        
        for (int i = 0; i < arity.length; i++) {
            Value.Discrete upb = new Value.Discrete(arity[i] - 1);
            Value.Structured multinomialBounds =
                new Value.DefStructured(new Value[] { zero, upb });
            Value.Model multinomial =
                (Multinomial) multinomialCreator.apply(multinomialBounds);
            
            Value[] continuousVal = new Value[arity[i]];
            for (int j = 0; j < arity[i]; j++) {
                continuousVal[j] =
                    new Value.Continuous(1.0 / (double) arity[i]);
            }
            Value.Structured parameterSet =
                new Value.DefStructured(continuousVal);
            vectorArray[i] =
                multinomial.generate(
                                     rand,
                                     numSamples,
                                     parameterSet,
                                     Value.TRIV);
        }
        
        String names[] = new String[arity.length];
        for (int i = 0; i < names.length; i++) {names[i] = "v"+i;}
        return new VectorFN.MultiCol(new Value.DefStructured(vectorArray,names));
    }
    
    /** 
     * Generate a set of uncorrelated binary data of the given size.
     */
    public static Value.Vector generateData( int numCases, int numNodes ) {
        int[] arity = new int[numNodes];
        Arrays.fill(arity,2);
        Random rand = new Random(123);
        return generateUncorrelatedDataset(rand,numCases,arity);
    }
    
    /*
      Generate a vector of data type a -> c <- b      <br>
      This is the same as generateCommonCauseDataset() but is missing variable d. <br>
      a = [0.3,0.7]                                   <br>
      b = [0.6,0.4]                                   <br>
      c | !a & !b = [0.8.0.2]                         <br>
      c | !a &  b = [0.4.0.6]                         <br>
      c |  a & !b = [0.7.0.3]                         <br>
      c |  a &  b = [0.9.0.1]                         <br>
      <br>
      returns [ (a,b,c) ]                           <br> 
    */
    public static Value.Vector generateReducedCommonCauseDataset( Random rand, int numSamples) 
    {
        Value.Vector originalVector =
            generateCommonCauseDataset(rand, numSamples);
        
        Value.Function dataView =
            (Value.Function) camml.core.library.CammlFN.view.apply(originalVector);
        
        Value.Vector parentVector =
            new VectorFN.FastDiscreteVector(new int[] { 0, 1, 2 });
        return (Value.Vector) dataView.apply(parentVector);
    }
    
    /**
       Generate a vector of data type a -> c <- b , d is an uncorrelated variable     <br>
       a = [0.3,0.7]                                   <br>
       b = [0.6,0.4]                                   <br>
       c | !a & !b = [0.8.0.2]                         <br>
       c | !a &  b = [0.4.0.6]                         <br>
       c |  a & !b = [0.7.0.3]                         <br>
       c |  a &  b = [0.9.0.1]                         <br>
       d = [0.8,0.2]                                   <br>
       <br>
       returns [ (a,b,c,d) ]                           <br> 
    */
    public static Value.Vector generateCommonCauseDataset(    Random rand,    int numSamples) 
    {
        int numCombinations = 4;
        
        Multinomial.MultinomialCreator multinomialCreator =
            new Multinomial.MultinomialCreator();
        Multinomial multinomialModel =
            (Multinomial) multinomialCreator.apply( new Value.DefStructured( new Value[] {
                        new Value.Discrete(0),
                        new Value.Discrete(1)
                    }));
        
        Value.Structured aParams =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.3),
                                        new Value.Continuous(0.7)});
        // p(a)
        Value.Structured bParams =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.6),
                                        new Value.Continuous(0.4)});
        // p(b)
        
        Value.Structured[] cParams = new Value.Structured[numCombinations];
        cParams[0] =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.8),
                                        new Value.Continuous(0.2)});
        // p(c | !a & !b)
        
        cParams[1] =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.4),
                                        new Value.Continuous(0.6)});
        // p(c | !a &  b)
        
        cParams[2] =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.7),
                                        new Value.Continuous(0.3)});
        // p(c |  a & !b)
        
        cParams[3] =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.9),
                                        new Value.Continuous(0.1)});
        // p(c |  a &  b)
        
        Value.Structured dParams =
            new Value.DefStructured(
                                    new Value[] {
                                        new Value.Continuous(0.8),
                                        new Value.Continuous(0.2)});
        // p(d)
        
        // We have to do make sure the values are not correlated.
        Value.Vector aVec =
            multinomialModel.generate(
                                      rand,
                                      numSamples,
                                      aParams,
                                      Value.TRIV);
        Value.Vector bVec =
            multinomialModel.generate(
                                      rand,
                                      numSamples,
                                      bParams,
                                      Value.TRIV);
        Value.Vector dVec =
            multinomialModel.generate(
                                      rand,
                                      numSamples,
                                      dParams,
                                      Value.TRIV);
        
        CPT cpt =
            new CPT(multinomialModel, new int[] { 0, 0 }, new int[] { 1, 1 });
        Value.Structured[] paramArray = new Value.Structured[numCombinations];
        
        for (int i = 0; i < numCombinations; i++) {
            paramArray[i] =
                new Value.DefStructured(
                                        new Value[] { multinomialModel, cParams[i] });
        }
        Value.Vector paramVector = new VectorFN.FatVector(paramArray);
        
        // [ (a,b) ] 
        Value.Vector abVec =
            new VectorFN.MultiCol(
                                  new Value.DefStructured(new Value[] { aVec, bVec }));
        
        // create [c] based on [ (a,b) ]
        Value[] cArray = new Value[numSamples];
        for (int i = 0; i < numSamples; i++) {
            cArray[i] =
                cpt.generate(
                             rand,
                             1,
                             paramVector,
                             abVec.elt(i)).elt(
                                               0);
        }
        Value.Vector cVec = new VectorFN.FatVector(cArray);
        
        // return the vector [ (a,b,c,d) ]
        return new VectorFN.MultiCol( new Value.DefStructured(new Value[] { 
                    aVec, bVec, cVec, dVec 
                }, new String[] { "aVec", "bVec", "cVec", "dVec"}
                ));
    }
    
    
    /** takes an array of doubles and returned a structure of doubles. */
    protected static Value.Structured makeStruct( double d[] )
    {
        Value valArray[] = new Value[d.length];
        for (int i = 0; i < d.length; i++)
            valArray[i] = new Value.Continuous(d[i]);
        return new Value.DefStructured(valArray);
    }
    
    
    /** Takes a model and an array of parameters and returns a (cptModel, [(subModel,params)] ) */
    protected static Value.Vector makeCPTParams(       Value.Model subModel,  Value[] params )
    {
        Value.Structured struct[] = new Value.Structured[params.length];
        for (int i = 0; i < params.length; i++)
            struct[i] = new Value.DefStructured( new Value[] {subModel,params[i]} );
        
        Value.Vector paramVector = new VectorFN.FatVector( struct );
        return paramVector;
    }
    
    /**
       Generate a large data set with several variables interacting in different ways.  <br>
       a = [0.3,0.7]                                   <br>
       b = [0.6,0.4]                                   <br>
       c | !a & !b = [0.2,0.8]                         <br>
       c | !a &  b = [0.4,0.6]                         <br>
       c |  a & !b = [0.7,0.3]                         <br>
       c |  a &  b = [0.9,0.1]                         <br>
       d = [0.8,0.2]                                   <br>
       e | !a & !c = [0.5,0.5]                         <br>
       e | !a &  c = [0.5,0.5]                         <br>
       e |  a & !c = [0.5,0.5]                         <br>
       e |  a &  c = [1.0,0.0]                         <br>
       f |  d = [0.5,0.4,0.1]                          <br>
       f | !d = [0.6,0.3,0.1]                          <br>
       g | (f=0) = [0.1,0.9]                           <br>
       g | (f=1) = [0.5,0.5]                           <br>
       g | (f=2) = [0.3,0.7]                           <br>
       <br>
       returns [ (a,b,c,d,e,f,g) ]                     <br> 
    */
    public static Value.Vector generateLargeDataset( Random rand, int numSamples) {
        Multinomial.MultinomialCreator multinomialCreator =
            new Multinomial.MultinomialCreator();
        Multinomial binomial =
            (Multinomial) multinomialCreator.apply( new Value.DefStructured( new Value[] {
                        new Value.Discrete(0),
                        new Value.Discrete(1)
                    }));
        
        Multinomial trinomial = 
            (Multinomial) multinomialCreator.apply( new Value.DefStructured( new Value[] {
                        new Value.Discrete(0),
                        new Value.Discrete(1),
                        new Value.Discrete(2)
                    }));
        
        Value.Structured aParams = makeStruct(new double[] { 0.6, 0.4 });
        // p(a)
        Value.Structured bParams = makeStruct(new double[] { 0.3, 0.7 });
        // p(b)
        
        // a or b
        Value.Structured[] cParams = new Value.Structured[4];
        cParams[0] = makeStruct(new double[] { 0.1, 0.9 }); // p(c | !a & !b)
        cParams[1] = makeStruct(new double[] { 0.9, 0.1 }); // p(c | !a &  b)
        cParams[2] = makeStruct(new double[] { 0.9, 0.1 }); // p(c |  a & !b)
        cParams[3] = makeStruct(new double[] { 0.9, 0.1 }); // p(c |  a &  b)
        
        Value.Structured dParams = makeStruct(new double[] { 0.8, 0.2 });
        // p(d)
        
        // e and c
        Value.Structured[] eParams = new Value.Structured[4];
        eParams[0] = makeStruct(new double[] { 0.1, 0.9 }); // p(e | !a & !c)
        eParams[1] = makeStruct(new double[] { 0.1, 0.9 }); // p(e | !a &  c)
        eParams[2] = makeStruct(new double[] { 0.1, 0.9 }); // p(e |  a & !c)
        eParams[3] = makeStruct(new double[] { 0.9, 0.1 }); // p(e |  a &  c)
        
        Value.Structured[] fParams = new Value.Structured[2];
        fParams[0] = makeStruct(new double[] { 0.5, 0.4, 0.1 }); // p(f | !d)
        fParams[1] = makeStruct(new double[] { 0.1, 0.4, 0.5 }); // p(f |  d)
        
        Value.Structured[] gParams = new Value.Structured[3];
        gParams[0] = makeStruct(new double[] { 0.8, 0.2 }); // p(g | (f=0))
        gParams[1] = makeStruct(new double[] { 0.2, 0.8 }); // p(g | (f=1))
        gParams[2] = makeStruct(new double[] { 0.6, 0.4 }); // p(g | (f=2))
        
        // Create CPT Models based on parent aritys.
        //CPT binaryCPT = new CPT(binomial, new int[] { 0 }, new int[] { 1 });
        // single binary parent
        CPT binary3CPT = new CPT(trinomial, new int[] { 0 }, new int[] { 1 });
        // single binary parent
        CPT dualBinaryCPT =
            new CPT(binomial, new int[] { 0, 0 }, new int[] { 1, 1 });
        // 2 binary parents
        CPT ternaryCPT = new CPT(binomial, new int[] { 0 }, new int[] { 1 });
        // single ternary parent
        
        // Create parameters for the CPT's c,e,f & g (a,b&d do not need this as they are simple.) 
        Value.Vector cCPTParams = makeCPTParams(binomial, cParams);
        Value.Vector eCPTParams = makeCPTParams(binomial, eParams);
        Value.Vector fCPTParams = makeCPTParams(trinomial, fParams);
        Value.Vector gCPTParams = makeCPTParams(binomial, gParams);
        
        // Now that all parameterization is done, create data vectors.
        // rand.nextInt is used as a seed value.
        Value.Vector aVec =
            binomial.generate(rand, numSamples, aParams, Value.TRIV);
        Value.Vector bVec =
            binomial.generate(rand, numSamples, bParams, Value.TRIV);
        Value.Vector dVec =
            binomial.generate(rand, numSamples, dParams, Value.TRIV);
        
        // [ (a,b) ] 
        Value.Vector abVec =
            new VectorFN.MultiCol(
                                  new Value.DefStructured(new Value[] { aVec, bVec }));
        
        // create [c] based on [ (a,b) ]
        Value[] cArray = new Value[numSamples];
        for (int i = 0; i < numSamples; i++) {
            cArray[i] =
                dualBinaryCPT.generate(
                                       rand,
                                       1,
                                       cCPTParams,
                                       abVec.elt(i)).elt(
                                                         0);
        }
        Value.Vector cVec = new VectorFN.FatVector(cArray);
        
        // [ (a,c) ] 
        Value.Vector acVec =
            new VectorFN.MultiCol(
                                  new Value.DefStructured(new Value[] { aVec, cVec }));
        
        // create [e] based on [ (a,c) ]
        Value[] eArray = new Value[numSamples];
        for (int i = 0; i < numSamples; i++) {
            eArray[i] =
                dualBinaryCPT.generate(
                                       rand,
                                       1,
                                       eCPTParams,
                                       acVec.elt(i)).elt(
                                                         0);
        }
        Value.Vector eVec = new VectorFN.FatVector(eArray);
        
        // [ (d) ] 
        Value.Vector ddVec =
            new VectorFN.MultiCol(
                                  new Value.DefStructured(new Value[] { dVec }));
        
        // create [f] based on [ (d) ]
        Value[] fArray = new Value[numSamples];
        for (int i = 0; i < numSamples; i++) {
            fArray[i] =
                binary3CPT.generate(
                                    rand,
                                    1,
                                    fCPTParams,
                                    ddVec.elt(i)).elt(
                                                      0);
        }
        Value.Vector fVec = new VectorFN.FatVector(fArray);
        
        // [ (f) ] 
        Value.Vector ffVec =
            new VectorFN.MultiCol(
                                  new Value.DefStructured(new Value[] { fVec }));
        
        // create [g] based on [ (f) ]
        Value[] gArray = new Value[numSamples];
        for (int i = 0; i < numSamples; i++) {
            gArray[i] =
                ternaryCPT.generate(
                                    rand,
                                    1,
                                    gCPTParams,
                                    ffVec.elt(i)).elt(
                                                      0);
        }
        Value.Vector gVec = new VectorFN.FatVector(gArray);
        
        return new VectorFN.MultiCol( new Value.DefStructured( new Value[] { 
                    aVec, bVec, cVec, dVec, eVec, fVec, gVec 
                }));
    }
    
}

