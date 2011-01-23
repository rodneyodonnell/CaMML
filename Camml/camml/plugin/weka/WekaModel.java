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
// TODO: 1 line description of WekaModel.java
//

// File: WekaModel.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.weka;

import java.util.Random;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import camml.core.library.StructureFN;
import camml.core.models.ModelLearner.GetNumParams;
import camml.core.models.multinomial.MultinomialLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;
import cdms.core.Value.Model;

/**
 * TODO: Multi line description of WekaModel.java
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.5 $ $Date: 2006/08/22 03:13:36 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/weka/WekaModel.java,v $
 */

public class WekaModel extends Model implements GetNumParams {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -6946494796150649671L;
    Type.Discrete xType;
    /**    */
    public WekaModel(Type.Discrete xType) { 
        super(new Type.Model(xType,Type.OBJECT,Type.STRUCTURED,Type.STRUCTURED));
        this.xType = xType;
    }

    /* (non-Javadoc)
     * @see cdms.core.Value.Model#logP(cdms.core.Value, cdms.core.Value, cdms.core.Value)
     */
    public double logP(Value x, Value y, Value z) {
        Value.Vector xVec = new VectorFN.FatVector( new Value[] {x} );
        Value.Vector zVec = new VectorFN.FatVector( new Value[] {z} );
        return logP( xVec, y, zVec );
    }
    
    /* (non-Javadoc)
     * @see cdms.core.Value.Model#logP(cdms.core.Value.Vector, cdms.core.Value, cdms.core.Value.Vector)
     */
    public double logP(Value.Vector x, Value y, Value.Vector z)         
    {
        return logPSufficient(getSufficient(x,z),y);
    }
    
    /* (non-Javadoc)
     * @see cdms.core.Value.Model#generate(java.util.Random, int, cdms.core.Value, cdms.core.Value)
     */
    public Vector generate(Random rand, int n, Value y, Value z) {
        
        // Extract Parameters
        Value.Obj params = (Value.Obj) y;
        Classifier classifier = (Classifier)params.getObj();

        // Create an 'instance' containing 'z'
        Instance inst;
        Type.Structured zType = (Type.Structured)z.t;
        if (zType.cmpnts.length != 0) {
            try {
                Value.Vector zVec = new VectorFN.FatVector( new Value[] {z} );
                Instances instances = Converter.vectorToInstances(zVec);
                int numAttribs = zType.cmpnts.length;
                instances.insertAttributeAt(new Attribute("target"), numAttribs);
                instances.setClassIndex(numAttribs);
                inst = instances.instance(0);
            } catch (RuntimeException e) { System.out.println("z = " + z); throw e;}
        } else {
            inst = new Instance(0);
        }

        // Calculate distribution given instance 'z'
        double[] dist;
        try {dist = classifier.distributionForInstance(inst);}
        catch (Exception e) {throw new RuntimeException(e);}
        
        // Use a multinomial to generate given an instance.
        Value.Model multi = MultinomialLearner.getMultinomialModel(xType);
        return multi.generate(rand,n,new StructureFN.FastContinuousStructure(dist),Value.TRIV);
    }

    /* (non-Javadoc)
     * @see cdms.core.Value.Model#predict(cdms.core.Value, cdms.core.Value)
     */
    public Value predict(Value y, Value z) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see cdms.core.Value.Model#predict(cdms.core.Value, cdms.core.Value.Vector)
     */
    public Vector predict(Value y, Vector z) {
        // TODO Auto-generated method stub
        return null;
    }

    /** return (x,z) structure */
    public Value getSufficient(Vector x, Vector z) {
        return new Value.DefStructured(new Value[] {x,z});
    }

    /* (non-Javadoc)
     * @see cdms.core.Value.Model#logPSufficient(cdms.core.Value, cdms.core.Value)
     */
    public double logPSufficient(Value s, Value y) {
        Value.Structured struct = (Value.Structured) s;
        Value.Vector x = (Value.Vector)struct.cmpnt(0);
        Value.Vector z = (Value.Vector)struct.cmpnt(1);

        Value.Obj params = (Value.Obj) y;
        Classifier classifier = (Classifier)params.getObj();
        
        Instances instances = Converter.vectorToInstances(x,z);
        
        double total = 0.0;
        try {
            for (int i = 0; i < instances.numInstances(); i++) {
                Instance inst = instances.instance(i);
                double[] dist = classifier.distributionForInstance(inst);
                total += Math.log(dist[x.intAt(i)]);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        
        return total;
    }

    public int getNumParams(Value params) {
        Classifier c = (Classifier)((Value.Obj)params).getObj();
        return ((GetNumParams)c).getNumParams(params);
    }

}
