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
// Multinomial model generator
//

// File: MultinomialGenerator.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.multinomial;

import java.util.Random;

import cdms.core.*;
import camml.core.library.StructureFN.FastContinuousStructure;



/**
   MultinomialGenerator is a model which generates fully parameterized decision trees. <br>
   This should theoretically allow us to use standard model functions (generate,logP,predict,etc)
   to deal with the creation and costing of Parameterized Decision Tree models.
 
   Parameter Space = (numParams,subModel,subParams)                         <br>
   where subParams = (splitArrtibute, [params]) for a split node  <br>
   and subParams = (params) for a leaf node.                      <br>
 
   input = ()                           <br>
   output = (Model,[params])            <br>
   sufficient = output                  <br>
   parameter = numStates  ]             <br>
*/
public class MultinomialGenerator extends Value.Model
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -7559216311418812085L;

    /** Static instance of MultinomialGenerator */
    public static final MultinomialGenerator generator = new MultinomialGenerator();
    
    /** Output type of MultinomialGeneratoe */
    public static Type xType = new Type.Structured(new Type[] {Type.MODEL,Type.STRUCTURED});
    
    /** Initialise the MultinomialGenerator.   */
    public MultinomialGenerator( )
    {
        super(new Type.Model( Type.TRIV,      // input space (x)
                              Type.DISCRETE,  // parameter space (y)
                              xType,          // shared space (z)
                              xType ));       // sufficient space(s)
    }
    
    
    
    /** logP(X|Y,Z) -- not implemented */
    
    public double logP(Value x, Value y, Value z)
    {
        throw new RuntimeException("Not Implemented");
    }
    
    /** logP(X|Y,Z) where v = (X,Y,Z) -- not implemented */
    public double logP(Value.Structured v)
    {
        throw new RuntimeException("Not Implemented");
    }
    
    
    /** Generate a vector of parameterized Multinomials */
    public Value.Vector generate(Random rand, int n, Value y, Value z) {    
        return generate( rand, y, new VectorFN.UniformVector( n, z ) );
    }
    
    /**
     * Does the real work of the generate function.  We use a java.util.Random as a seed instead
     * of an int.
     */
    public Value.Vector generate( java.util.Random rand, Value y, Value.Vector z ) {
        
        Value.Discrete params = (Value.Discrete)y;
        int numStates = params.getDiscrete();    
        
        Value.Structured[] structArray = new Value.Structured[z.length()];
        for ( int i = 0; i < structArray.length; i++ ) {        
            structArray[i] = generate( rand, numStates );
        }
        
        return new VectorFN.FatVector( structArray );
    }
    
    /**
     *  Generate parameters from a uniform distribution <br>
     *  To do this we use the following algorithm <br>
     *  - randArray[k+1] = generate(0..1).  randArray[0] = 0, randArray[k] = 1
     *  - Sort randArray[]
     *  - Parameter[i] = randArray[i+1] - randArray[i] 
     */
    public static Value.Structured generate( java.util.Random rand, int numStates )
    {
        Value.Model model = MultinomialLearner.getMultinomialModel( 0, numStates - 1 );
        
        
        // --- THIS DOES NOT CREATE A UNIFORM DISTRIBUTION! ---
        //     // Uniformly generate then normalise probabilities
        //     double[] p = new double[numStates];
        //     double total = 0;       
        //     for ( int j = 0; j < p.length; j++ ) { p[j] = rand.nextDouble(); total += p[j]; }
        //     for ( int j = 0; j < p.length; j++ ) { p[j] /= total; }
        //     FastContinuousStructure pStruct = new FastContinuousStructure( p );
        //     return new Value.DefStructured( new Value[] { model ,pStruct } );
        
        //      // Generate from uniform prior.
        //      double[] randArray = new double[numStates+1];
        //      for ( int i = 0; i < randArray.length; i++ ) { randArray[i] = rand.nextDouble(); }
        //     randArray[0] = 0.0; randArray[randArray.length-1] = 1.0;
        
        //     java.util.Arrays.sort(randArray);
        
        
        //     double[] p = new double[numStates];
        //     for ( int i = 0; i < p.length; i++ ) {
        //         p[i] = randArray[i+1] - randArray[i];
        //     }
        
        FastContinuousStructure pStruct = 
            new FastContinuousStructure( generateUniformArray( rand, numStates ) );
        return new Value.DefStructured( new Value[] { model ,pStruct } );
        
    }
    
    /** Return an array of doubles uniformly distributed and summing to 1.0 */
    public static double[] generateUniformArray( java.util.Random rand, int numStates )
    {
        // Generate from uniform prior.
        double[] randArray = new double[numStates+1];
        for ( int i = 0; i < randArray.length; i++ ) { randArray[i] = rand.nextDouble(); }
        randArray[0] = 0.0; randArray[randArray.length-1] = 1.0;
        
        java.util.Arrays.sort(randArray);
        
        double[] p = new double[numStates];
        for ( int i = 0; i < p.length; i++ ) {
            p[i] = randArray[i+1] - randArray[i];
        }
        return p;
    }
    
    
    /** not implemented */
    public Value predict(Value y, Value z)
    {
        throw new RuntimeException("Not implemented");
    } 
    
    public Value.Vector predict(Value y, Value.Vector z) {
        return new VectorFN.UniformVector(z.length(), predict(y, z));
    }
    
    /** returns x */
    public Value getSufficient(Value.Vector x, Value.Vector z)
    {
        return x;
    }
    
    /** not implemented */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
        throw new RuntimeException("Not implemented");
    }
    
    
    
    /** logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
        In this case, s is simply the vector x */
    public double logPSufficient(Value s, Value y)
    {
        Value.Vector x = (Value.Vector)((Value.Structured)s).cmpnt(0);;
        Value.Vector z = (Value.Vector)((Value.Structured)s).cmpnt(1);;
        
        return logP( x, y, z );
    }
    
    /** returns a representation of the MultinomialGenerator */
    public String toString()
    {
        return "MultinomialGenerator";
    } 
}
