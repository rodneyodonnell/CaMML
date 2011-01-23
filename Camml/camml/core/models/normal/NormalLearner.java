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
// Wrapper for cdms.mml87.normalParameterizer
//

// File: NormalLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.normal;

import cdms.core.*;
import cdms.plugin.model.*;
import cdms.plugin.mml87.*;
import camml.core.models.ModelLearner;

/**
 * NormalLearner is a wrapper class of type ModelLearner. <br>
 * This allows it's parameterizing and costing functions to interact with other CDMS models in a
 * standard way. <br>
 * <br>
 */
public class NormalLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 7954794142317457436L;

    /** Static instance of normalLearner, precision set to 0.001 */
    public static NormalLearner normalLearner = new NormalLearner(0.001); 
    
    public String getName() { return "NormalLearner"; }    
    
    /** return {mean, sd} given data */
    public static double[] getStats(double data[]) {
        Value.Vector xData = new VectorFN.FastContinuousVector(data);
        Value.Vector zData = new VectorFN.UniformVector(data.length,Value.TRIV);
        Value.Structured msy = normalLearner.parameterize(Value.TRIV,xData,zData);
        Value.Structured muSd = (Value.Structured)msy.cmpnt(2);
        return new double[] {muSd.doubleCmpnt(0), muSd.doubleCmpnt(1)};
    }
    
    /** 
     * Measurement precision assumed in costing. This MUST! be set before use <br>
     * both install(Value v) and NormalLearner(double precision) set it, simply using
     * NormalLearner() does not!
     */
    protected double precision;
    
    /** becomes true when precision is set. */
    protected boolean precisionSet = false;
    
    
    /**
     * Precision is set automatically by install( Value precision ) and NormalLearner(double precision)
     * but not by NormalLearner(). <br>'
     * NOTE: Precision may only ever be set once.  Attempting so set precision to a different value
     * will generate a RuntimeExcaption.
     */
    public void setPrecision( double precision )
    {
        if ( precisionSet == false || this.precision == precision ) { 
            precisionSet = true;
            this.precision = precision;
            sParameterizer = new UniformNormal.NormalEstimator2(precision);
            sCoster = new UniformNormal.NormalCoster2(precision);        
        }
        else {
            throw new RuntimeException("Precision already set in NormalLearner.setPrecision");
        }
    }
    
    
    /** parameterizer from UniformNormal */
    protected Value.Function sParameterizer = null;  
    /** coster from UniformNormal */
    protected Value.Function sCoster = null;         
    
    /** Warning : This constructor does not set precision, install os setPrecision must be called */
    public NormalLearner()
    {
        super( (Type.Model)Normal.normal.t, Type.TRIV ); 
    }
    
    /** Standard Constructor : Sets precision */
    public NormalLearner( double precision )
    {
        super( (Type.Model)Normal.normal.t, Type.TRIV ); 
        setPrecision(precision);
    }
    
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value i, Value.Vector x, Value.Vector z )
    {
        Value stats = Normal.normal.getSufficient(x,z);
        return sParameterize(Normal.normal, stats);
    } 
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value stats )
    {
        if (precisionSet == false)
            throw new RuntimeException("Precision not set in NormalLearner");
        
        return new Value.DefStructured( new Value[]
            {
                model,
                stats,
                sParameterizer.apply( stats )
            } );
    }
    
    
    /** return cost */
    public double cost(Value.Model m, Value i, Value.Vector x, Value.Vector z, Value params)
    {
        return sCost( m, m.getSufficient(x,z), params );
    }
    
    /** return cost */
    public double sCost( Value.Model m, Value stats, Value params )
    {
        if (precisionSet == false)
            throw new RuntimeException("Precision not set in NormalLearner");
        
        Value.Function coster = (Value.Function)sCoster.apply(params);
        return coster.applyDouble(stats);
    }
    
    public String toString() { return "NormalLearner"; }
}

