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
// CDMS distribution interface to Weka
//

// File: CammlClassifier.java
// Author: rodo@dgs.monash.edu.au


// TODO: This uses an old version of weka, new version should be used.
package camml.plugin.weka;

import weka.core.*;
import weka.classifiers.*; 

import cdms.core.*;
import camml.core.models.ModelLearner;
import camml.core.models.bNet.*;
import camml.core.search.SearchPackage;

import weka.filters.supervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * Class to interface with the java version of Camml.
 *
 * Learn a Bayes Net from data.
 * 
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.11 $ $Date: 2006/08/22 03:13:35 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/weka/ModelLearnerClassifier.java,v $

 */
public class ModelLearnerClassifier extends Classifier //DistributionClassifier 
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -5205480025287640030L;

    /** Static class with an empty constructor as required to link to weka */
    public static class RodoCamml extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -6441009178035054471L;

        public RodoCamml() { 
            super( camml.plugin.rodoCamml.RodoCammlLearner.modelLearner ); 
        } 
    }
    
    /** Static class with an empty constructor as required to link to weka */
    public static class FriedmanCPT extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3643794503666892909L;

        public FriedmanCPT() { 
            super( camml.plugin.friedman.FriedmanLearner.modelLearner_BDE ); 
        } 
    }
    
    /** Static class with an empty constructor as required to link to weka */
    public static class CPTMetropolis extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -843519911361222161L;

        public CPTMetropolis() { 
            super( new BNetLearner( SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner, false, false) );
        } 
    }
    
    /** Static class with an empty constructor as required to link to weka */
    public static class CPTMixMetropolis extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 6686087229083682371L;

        public CPTMixMetropolis() { 
            super( new BNetLearner( SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner, true, false) );            
        } 
    }
    
    /** Static class with an empty constructor as required to link to weka */
    public static class DTreeMetropolis extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 1579298883628991663L;

        public DTreeMetropolis() { 
            super( new BNetLearner( SearchPackage.mlCPTLearner, SearchPackage.dTreeLearner, false, false) );
        } 
    }
    
    /** Static class with an empty constructor as required to link to weka */
    public static class DTreeMixMetropolis extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 487706249580196506L;

        public DTreeMixMetropolis() { 
            super( new BNetLearner( SearchPackage.mlCPTLearner, SearchPackage.dTreeLearner, true, false) );            
        } 
    }
    

    /** Static class with an empty constructor as required to link to weka */
    public static class DualMetropolis extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 2417706190152711458L;

        public DualMetropolis() { 
            super( new BNetLearner( SearchPackage.mlCPTLearner, SearchPackage.dualLearner, false, false) );
        } 
    }
    
    /** Static class with an empty constructor as required to link to weka */
    public static class DualMixMetropolis extends ModelLearnerClassifier {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -5813370934012958776L;

        public DualMixMetropolis() { 
            super( new BNetLearner( SearchPackage.mlCPTLearner, SearchPackage.dualLearner, true, false) );            
        } 
    }
    

    ///** Static class with an empty constructor as required to link to weka */
    //public static class DualMetropolis extends ModelLearnerClassifier {
    //    public DualMetropolis() { 
    //        super( SearchPackage.MetropolisLearner.dualMetropolisLearner ); 
    //    } 
    //}
    
    //     /** Static class with an empty constructor as required to link to weka */
    //     public static class CPTAnneal extends ModelLearnerClassifier {
    //     public CPTAnneal() { 
    //         super( SearchPackage.AnnealLearner.cptAnnealLearner ); 
    //     } 
    //     }
    
    //     /** Static class with an empty constructor as required to link to weka */
    //     public static class DTreeAnneal extends ModelLearnerClassifier {
    //     public DTreeAnneal() { 
    //         super( SearchPackage.AnnealLearner.dTreeAnnealLearner ); 
    //     } 
    //     }
    
    //     /** Static class with an empty constructor as required to link to weka */
    //     public static class DualAnneal extends ModelLearnerClassifier {
    //     public DualAnneal() { 
    //         super( SearchPackage.AnnealLearner.dualAnnealLearner ); 
    //     } 
    //     }
    
    
    
    /** ModelLearner function used in this classifier.  This class does most of the work. */
    public final ModelLearner modelLearner;
    
    
    
    /** Model generated by modelLearner */
    protected Value.Model model = null;
    
    /** parameters generated by modelLearner */
    protected Value params = null;
    
    
    /** ModelLearnerClassifier constructor simply sets modelLearner variable */
    public ModelLearnerClassifier( ModelLearner modelLearner ) {
        this.modelLearner = modelLearner;
    }
    
    /**
     * Filter to discretize missing values.  This is initialised in buildClassifier. <br>
     * If the instances passed to buildClassifier have a discrete target attribute MDL is used for
     *  optimal binning.  If not a less intelligent method bins each attribute into 10 bins.
     */
    protected Discretize discreteFilter;
    //protected DiscretizeFilter discreteFilter;
    
    /**
     * missingFilter replaces each missing value with the mode likely value for that attribute.
     */
    protected ReplaceMissingValues missingFilter;
    //protected ReplaceMissingValuesFilter missingFilter;
    
    
    /**
     * Initialize filters based on instances provided.  <br>
     * NOTE: This function does NOT actually do the filtering
     */
    public void initializeFilters( Instances instances ) throws Exception
    {
        // Initialise the discrete filter.
        discreteFilter = new Discretize();
        //discreteFilter = new DiscretizeFilter();
        if ( (instances.classIndex() == -1) || (!instances.classAttribute().isNominal()) ) {
            //discreteFilter.setUseMDL(false);
        }
        else {        
            discreteFilter.setUseBetterEncoding(true);
        }    
        discreteFilter.setInputFormat(instances);
        
        // We need this to get the correct output input format into missingFilter
        Instances temp = weka.filters.Filter.useFilter( new Instances(instances), discreteFilter );
        
        
        // Initialise and run the missing value filter.
        missingFilter = new ReplaceMissingValues();
        //missingFilter = new ReplaceMissingValuesFilter();
        missingFilter.setInputFormat( temp );
    }
    
    /** return a copy of instances with discrete and missingValue filters applied to it. */
    public Instances filterInstances( Instances instances ) throws Exception
    {
        Instances instancesCopy = new Instances( instances );
        instancesCopy =  weka.filters.Filter.useFilter( instancesCopy, discreteFilter );
        instancesCopy = weka.filters.Filter.useFilter( instancesCopy, missingFilter );
        return instancesCopy;
    }
    
    
    /**
     * Generates the classifier.
     *
     * @param instances set of instances serving as training data 
     * @exception Exception if the classifier has not been generated 
     * successfully
     */
    public void buildClassifier(Instances instances) throws Exception {
        
        // Set up data for Camml
        initializeFilters( instances );
        Instances instancesCopy = filterInstances( instances  );
        Value.Vector data = Converter.instancesToVector( instancesCopy );
        
        
        
        Value.Structured msy = modelLearner.parameterize( Value.TRIV, data, data );    
        this.model = (Value.Model)msy.cmpnt(0);
                
        this.params = msy.cmpnt(2);
    }
    
    
    
    /**
     * Calculates the class membership probabilities for the given test 
     * instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * @exception Exception if there is a problem generating the prediction
     */
    public double [] distributionForInstance(Instance instance) 
        throws Exception { 
        
        // Apply discreteFilter to instance.
        // NOTE: Missing value filter is NOT required as Bayes Networks can cope with missing vals.
        discreteFilter.input( instance );
        Instance filteredInstance = discreteFilter.output( );
        
        
        
        int classVariable = filteredInstance.classAttribute().index();
        
        Value.Structured instanceStruct = Converter.instanceToStruct( filteredInstance );
        Type.Structured sType = ((Type.Structured)instanceStruct.t);
        Type[] type = sType.cmpnts;
        String[] nameArray = sType.labels;
        Type.Discrete classType = (Type.Discrete)type[classVariable];
        int arity = (int)classType.UPB - (int)classType.LWB + 1;    
        
        Value[] inputArray = new Value[instanceStruct.length()];
        Value[][] outputArray = new Value[arity][instanceStruct.length()];
        
        
        // Loop through each variable in order.
        for ( int i = 0; i < inputArray.length; i++ ) {
            
            // Input of target variable is missing, output is set to each possible value to create
            // a multistate distribution.
            if ( i == classVariable ) {
                inputArray[i] = new Value.Discrete( (Type.Discrete)type[i], 
                                                    Value.S_UNOBSERVED, 0 );
                for ( int j = 0; j < arity; j++ ) {
                    outputArray[j][i] = new Value.Discrete( (Type.Discrete)type[i],
                                                            Value.S_PROPER, 
                                                            (int)classType.LWB + j );
                }
            }
            // Missing values are missing on both input and output.
            else if ( filteredInstance.isMissing(i) ) {
                inputArray[i] = new Value.Discrete( (Type.Discrete)type[i], Value.S_UNOBSERVED, 0 );
                for ( int j = 0; j < arity; j++ ) {
                    outputArray[j][i] = inputArray[i];
                }
                
            }
            // Regular values are present in both input and output.
            else {
                inputArray[i] = instanceStruct.cmpnt(i);
                for ( int j = 0; j < arity; j++ ) {
                    outputArray[j][i] = instanceStruct.cmpnt(i);
                }
            }
        }
        
        Value.Structured input = new Value.DefStructured( inputArray, nameArray );
        Value.Structured[] output = new Value.Structured[arity];
        for ( int i = 0; i < output.length; i++ ) {
            output[i] = new Value.DefStructured( outputArray[i], nameArray );
        }
        
        
        double[] prob = new double[arity];
        for ( int i = 0; i < prob.length; i++ ) {
            prob[i] = Math.exp( model.logP( output[i], params, input ) );
        }
        
        return prob;
        
    }
    
    public String toString()
    {
        if ( model instanceof camml.plugin.netica.BNetNetica ) {
            camml.plugin.netica.BNetNetica bNet = (camml.plugin.netica.BNetNetica)model;        
            return bNet.toString( (Value.Vector)params, "NET_TITLE","COMMENT" );
        }
        else if ( model instanceof BNet ) {
            return ((BNet)model).makeString( (Value.Vector)params );
        }
        else return "(" + model + "," + params + ")";
    }
}
