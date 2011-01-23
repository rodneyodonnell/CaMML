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
// TODO: 1 line description of WekaLearner.java
//

// File: WekaLearner.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.weka;


import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
//import weka.classifiers.trees.J48;
import weka.core.Instances;
import camml.core.models.ModelLearner;
import camml.core.models.bNet.BNetLearner;
import camml.core.models.cpt.CPTLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.Value.Model;
import cdms.core.Value.Structured;
import cdms.core.Value.Vector;

/**
 * TODO: Multi line description of WekaLearner.java
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.8 $ $Date: 2006/08/22 03:13:36 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/weka/WekaLearner.java,v $
 */
public class WekaLearner extends ModelLearner.DefaultImplementation {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -6079870257596106360L;
    /** Weka classifier used for learning model*/
    protected final Classifier classifier;
    
    /**
     * Create a WekaLearner which parameterizes using the given classifier.
     */
    public WekaLearner(Classifier classifier) {
        super(Type.MODEL, Type.TRIV);
        this.classifier = classifier;
    }

    /**
     * @see camml.core.models.ModelLearner#parameterize(cdms.core.Value, cdms.core.Value.Vector, cdms.core.Value.Vector)
     */
    public Structured parameterize(Value i, Vector x, Vector z) throws LearnerException {
        
        // Convert data
        Instances instances = Converter.vectorToInstances(x,z);
        Classifier c2;
        try {c2 = Classifier.makeCopy(classifier);}
        catch (Exception e){ throw new LearnerException("Could not make copy of Weka classifier.",e); }
        
        // Attempt to perform classification
        try { c2.buildClassifier(instances); } 
        catch (Exception e) { throw new LearnerException("Weka classification failed.", e); }
        
        Value.Obj y = new Value.Obj(c2);
        Value.Model m = new WekaModel((Type.Discrete)((Type.Vector)x.t).elt);
        Value s = m.getSufficient(x,z);
        return new Value.DefStructured(new Value[] {m,s,y});
        
    }

    /** @see camml.core.models.ModelLearner#sParameterize(cdms.core.Value.Model, cdms.core.Value) */
    public Structured sParameterize(Model m, Value stats) throws LearnerException {
        Value.Structured struct = (Value.Structured) stats;
        return parameterize(Value.TRIV,(Value.Vector)struct.cmpnt(0),(Value.Vector)struct.cmpnt(1));
    }

    /** 
     * Default costing function for WekaLearner is -LogLikelihood of data given the
     * learned model.  This is obviously bad as there is no mechanism to stop
     * overfitting.  As such this method should be overridden for any serious models
     * using this class.
     *  
     * @see camml.core.models.ModelLearner#sCost(cdms.core.Value.Model, cdms.core.Value, cdms.core.Value)
     */
    public double sCost(Model m, Value stats, Value params) throws LearnerException {
        return -m.logPSufficient(stats,params);
    }

    /** @see camml.core.models.ModelLearner#getName() */
    public String getName() { return "WekaLearner"; } 


    public static WekaLogitLearner wekaLogitLearner = new WekaLogitLearner();
    /** Weka logit learner with sCost function overwritten. */
    public static class WekaLogitLearner extends WekaLearner {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 8315350008915847383L;
        /** Create ModelLearner using weka Logistic model*/
        public WekaLogitLearner() {    super(new Logistic2()); }
        //public WekaLogitLearner() {    super(new Logistic()); }
        //public WekaLogitLearner() {    super(new J48()); }
        
        
        /** TODO: Fix sCost, currently nasty hack of log(numCases) * numParams
         *  where numParams is estimated by the number of lines output by toString()
         *  TODO: FIX! FIX! FIX!
         *  */
        public double sCost(Model m, Value stats, Value params) throws LearnerException {

            Object logistic = ((Value.Obj)params).getObj();
            if (logistic instanceof GetMMLScore) { return ((GetMMLScore)logistic).getMMLScore(m,stats,params); }
            
            System.out.println("GetMMLScore not implemented in " + m.getClass());
            
            int numCases = ((Value.Vector)((Value.Structured)stats).cmpnt(0)).length();
                        
            //System.out.println("zType = " + zType);
            //System.out.println("numCases = " + numCases);
            
            //Logistic logistic = (Logistic)((Value.Obj)params).getObj();
            String logisticString = logistic.toString();
            int count = 0;
            for (int i = 0; i < logisticString.length(); i++) {
                if (logisticString.charAt(i) == '\n') {count ++;}
            }
            /*
              System.out.println("<------------------------------------------------");
              System.out.println(logistic);
              System.out.println("------------------------------------------------>");
              System.out.println("count = " + count);
            */
            return -m.logPSufficient(stats,params) + Math.log(numCases)*count;
        }

    }
    
    /** Interface for weka classifiers to implement so MML score can be retrieved. */
    public static interface GetMMLScore {
        public double getMMLScore(Value.Model m, Value stats, Value params);
    }
    
    /** Weka Logistic classifier with getMMLScore() and getNumParams() implemented */
    public static class Logistic2 extends Logistic implements GetNumParams, GetMMLScore {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 5227316370198101334L;

        /** Return number of free parameters in model*/
        public int getNumParams(Value params) {
            return m_Par.length*m_Par[0].length;
        }

        /** Calculate MML score of model given parameters. */
        public double getMMLScore(Value.Model m, Value stats, Value params) {
            Value.Vector x = (Value.Vector)((Value.Structured)stats).cmpnt(0);
            return -m_LL + Math.log(x.length())*getNumParams(null);
        }
        
    }
    
    //    public static class WekaLogitModel extends WekaModel implements ModelLearner.GetNumParams {
    //
    //        public WekaLogitModel( Type.Discrete xType) { super(xType); }
    //        /* (non-Javadoc)
    //         * @see camml.core.models.ModelLearner.GetNumParams#getNumParams(cdms.core.Value)
    //         */
    //        public int getNumParams(Value params) {
    //            Logistic logistic = (Logistic)((Value.Obj)params).getObj();
    //            
    //            System.out.println(logistic);
    //            // TODO Auto-generated method stub
    //            return 0;
    //        }
    //        
    //    }
    
    public final static BNetLearner wekaBNetLogitLearner = new BNetLearner(CPTLearner.mlMultinomialCPTLearner, wekaLogitLearner,false,true);
}
