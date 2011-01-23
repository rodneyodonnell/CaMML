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
// Bayesian Network model class using Netica inference.
//

// File: BNetNetica.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.netica;

// LH: for serialisation
import java.io.*;

import java.util.Random;

import camml.core.models.bNet.BNet;
import cdms.core.*;

import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Net;
import norsys.netica.Streamer;

/**
 *  Bayesian Network model. <br>
 *
 *  A BNetNetica model is a CDMS implementation of a Bayesian Network. <br>
 *  
 *  Unlike many other models, the input and output of a Bayesian Net are not strictly defined and
 *  a variable which was once viewed as an input may later be viewed as an output.  To cope with
 *  this all variables are included as both inputs and outputs of the network. <br>
 *
 *  To "query" a variable it's value should be set to missing. <br>
 *
 *  Both discrete and continuous variables (or any other sort) may be used so long as the models
 *  and parameters which encode the local structure will accept these data types. <br>
 *
 *  The sufficient statistics of a model is simply taken as (X,Z).  X and Z will often be similar
 *  but for function such as logP() we need two seperate values (one with some values missing, one
 *  perhaps with those values included.)
 * 
 *  The parameterisation of the network is done as a vector of local structures.  Each local 
 *  structure consists of a vector of parents and a (model,params). <br>
 *
 *  Inference is performed using the Netica engine.
 *
 */


public class BNetNetica extends BNet
{

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 3880478828992356673L;

    /**
     *  Simple constructor.  Model type = { 
     *  X = Type.VECTOR,
     *  Z = Type.VECTOR, 
     *  Y = [ ( name, [parents], (model,params) ) ] 
     *  S = (Type.VECTOR, Type.VECTOR) }
     */
    public BNetNetica( Type.Structured dataType )
    {
        super( dataType );
        tempBNet = new camml.core.models.bNet.BNetStochastic(dataType);
    }

    /** ??? This will be removed soon. */
    private final camml.core.models.bNet.BNetStochastic tempBNet;    

    /** logP(X|Y,Z) where v = (X,Y,Z) */
    public double logP(Value.Structured v)
    {
        return logP(v.cmpnt(0),v.cmpnt(1),v.cmpnt(2));
    }



    /**
     *  Returns a stochastic vector of elements from the data-space conditional on Y,Z.
     *  This normalises the CPT. <br>
     *  Y = [ name, [parents], (model.params) ]       
     */
    public Value.Vector generate(Random rand, int n, Value y, Value z)
    {    
        return tempBNet.generate(rand,n,y,z);
    }

    /**
     * Predict returns the most probably state of the network.  As the optimal solution to this
     *  would (possibly) require searching through all states instead a sub-optimal solution is
     *  found by traversing the network from root to leaf choosing the most likely state at each
     *  point.
     */
    public Value predict(Value y, Value z)
    {
        throw new RuntimeException("Function not implemented");
    } 



    /** logP(X_1|Y,Z_1) + logP(X_2|Y,Z_2) + ... where s is a sufficient statistic of X for Y.
        In this case, s is simply the vector x */
    public double logPSufficient(Value s, Value y)
    {
        throw new RuntimeException("Function not implemented");
    }   

    /** Treat BNet as a classifier and calculate P( z_i[var] | z_i\var ) */
    public double[] probClassify( Value.Vector y, Value.Vector z, int var) {
        double logPArray[] = new double[z.length()];

        boolean mask[] = new boolean[y.length()];
        mask[var] = true;
        //Arrays.fill( mask, true );
        Value.Vector in = BNet.maskMissing(z,mask);
        Value.Vector out = z;

        for (int i = 0; i < z.length(); i++) {
            logPArray[i] = Math.exp(logP(out.elt(i), y, in.elt(i)));
        }

        return logPArray;
    }


    /** Calculate P(z[var] | z\var) for each possible value of z[var]. */
    public double[][] getClassProbs( Value.Vector y, Value.Vector z, int var) {
        // Extract individual vectors from z
        int numVars = y.length();
        Value.Vector[] vecs = new Value.Vector[numVars];
        for ( int i = 0; i < numVars; i++) {
            vecs[i] = z.cmpnt(i);
        }

        // Determine type of x
        Type.Vector xVecType = (Type.Vector)z.cmpnt(var).t;
        Type.Discrete xType = (Type.Discrete)xVecType.elt;
        int arity = (int)(xType.UPB - xType.LWB + 1);

        // Call probClassify to determine probabilities of each value of 'var' for
        // each row in 'z'
        double[][] probArray = new double[arity][];
        for (int i = 0; i < arity; i++) {
            Value.Vector vecI = new VectorFN.UniformDiscreteVector(xVecType,z.length(),i);
            vecs[var] = vecI;
            Value.Vector newZ = new VectorFN.MultiCol(new Value.DefStructured(vecs));
            probArray[i] = probClassify(y, newZ, var);
        }

        return probArray;
    }

    /** Treat BNet as a classifier and calculate most likely value */
    public Value.Vector classify( Value.Vector y, Value.Vector z, int var) {

        // Determine type of x
        Type.Discrete xType = (Type.Discrete)((Type.Vector)z.cmpnt(var).t).elt;

        // Calculate all probs.
        double probs[][] = getClassProbs(y,z,var);

        // Find highest probability in each row, save as bestVal
        int bestVal[] = new int[z.length()];
        for (int i = 0; i < bestVal.length; i++) {
            int best = -1;
            double bestProb = 0;
            for (int j = 0; j < probs.length; j++) {
                if ( probs[j][i] > bestProb ) {
                    bestProb = probs[j][i];
                    best = j;
                }
            }
            bestVal[i] = best;
        }


        return new VectorFN.FastDiscreteVector(bestVal, xType);

    }

    /** Calculate posterior marginals over 'var' given each element of z\var */
    public double[][] distribution( Value.Vector y, Value.Vector z, int target) {

        // Allocate memory for retun value.
        double logPArray[][] = new double[z.length()][];

        // Extract information from parameter list.
        String[] name = makeNameList( (Value.Vector)y );
        name = NeticaFn.makeValidNeticaNames( name, true );

        // Compile network.
        Net net;
        try {
            net = buildNet( "BNetNetica_distribution", (Value.Vector)y );
            net.compile();

            for ( int i = 0; i < z.length(); i++ ) {
                Value.Structured zStruct = (Value.Structured)z.elt(i);
                net.retractFindings();

                // set inputs
                for ( int j = 0; j < zStruct.length(); j++ ) {
                    Value inputJ = zStruct.cmpnt(j);

                    if ( j != target && inputJ.status() == Value.S_PROPER ) {
                        Node nodeJ = net.getNode( name[j] );
                        int val = ((Value.Discrete)inputJ).getDiscrete() +
                            (int)((Type.Discrete)inputJ.t).LWB;
                        nodeJ.enterFinding( val );
                    }
                }

                // marginalise on 'var'
                Node targetNode = net.getNode( name[target] );
                float[] marginals = targetNode.getBeliefs();
                logPArray[i] = new double[marginals.length];
                for (int j = 0; j < marginals.length; j++) {
                    logPArray[i][j] = marginals[j];
                }
            }

        }
        catch ( NeticaException e ) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return logPArray;
    }













    /** logP(X|Y,Z) */
    public double logP(Value x, Value y, Value z)
    {    
        synchronized ( Netica.env ) {
            // Extract information from parameter list.
            String[] name = makeNameList( (Value.Vector)y );
            int[][] parentList = makeParentList( (Value.Vector)y );
            int[] order = makeConsistantOrdering( parentList );
            Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
            Value[] subModelParamList = makeSubParamList( (Value.Vector)y );

            //     for ( int i = 0; i < name.length; i++ ) {
            //         name[i] = NeticaFn.makeValidNeticaName( name[i] );
            //     }
            name = NeticaFn.makeValidNeticaNames( name, true );

            Value.Structured input;
            if ( z == Value.TRIV ) {
                // with no imput, stochastic network is exact and more efficient.
                // It is also wrong and can't handle missing data in x/
                //        return tempBNet.noInputLogP( (Value.Structured)x, 
                //                         subModelList,
                //                         subModelParamList,
                //                         order,
                //                         parentList );

                input = makeUnobservedInputStruct();
            }
            else {
                input = (Value.Structured)z;
            }

            Value.Structured output = (Value.Structured)x;


            Net net;
            try {
                net = buildNet( "Net_constructed_for_logP", (Value.Vector)y );
                net.compile();
                net.retractFindings();


                // set inputs
                for ( int i = 0; i < input.length(); i++ ) {
                    Value inputI = input.cmpnt(i);

                    if ( inputI.status() == Value.S_PROPER ) {
                        Node nodeI = net.getNode( name[i] );
                        int val = ((Value.Discrete)inputI).getDiscrete() +
                            (int)((Type.Discrete)inputI.t).LWB;
                        nodeI.enterFinding( val );
                    }
                }

                double logProb = 0;

                // marginalise on outputs
                for ( int ii = 0; ii < input.length(); ii++ ) {
                    int i = order[ii];
                    Value.Discrete outputI = (Value.Discrete)output.cmpnt(i);

                    if ( outputI.status() == Value.S_PROPER ) {
                        Node nodeI = net.getNode( name[i] );
                        int val = outputI.getDiscrete() + (int)((Type.Discrete)outputI.t).LWB;
                        float[] f = nodeI.getBeliefs();

                        logProb += Math.log( f[val] );
                        if ( f[val] == 0.0 ) {  // impossible evidence
                            break;
                        }
                        nodeI.enterFinding( val );
                    }
                }

                return logProb;
            }
            catch ( NeticaException e ) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }



            //throw new RuntimeException("Not implemented (yet)");

            //// Extract information from parameter list.
            //String[] name = makeNameList( (Value.Vector)y );
            //int[][] parentList = makeParentList( (Value.Vector)y );
            //int[] order = makeConsistantOrdering( parentList );
            //Value.Model[] subModelList = makeSubModelList( (Value.Vector)y );
            //Value[] subModelParamList = makeSubParamList( (Value.Vector)y );

            //Value.Structured input;
            //if ( z == Value.TRIV ) {
            //    input = makeUnobservedInputStruct();
            //}
            //else {
            //    input = (Value.Structured)z;
            //}

            //Value.Structured output = (Value.Structured)x;


            //// Initialise all weights to 1.0
            //double[] weight = new double[numSamples];
            //boolean[] match = new boolean[numSamples];
            //for ( int i = 0; i < weight.length; i++ ) {
            //    weight[i] = 1.0;
            //    match[i] = true;
            //}


            //Value.Vector sample =
            //            generateUnweightedSample( (Value.Vector)y, input );


            //// data[i] is the i'th subvector of data.  ie. the i'th variable

            //for ( int i = 0; i < order.length; i++ ) {

            //    // We need to sample the variable from a topologically sorted list so parent data
            //    //  is always evaluated before its childrens data
            //    int current = order[i];




            //    // Next we must collect all parent data of the current variable into a single
            //    //  Value.Vector.  This is done by making an array of Value.Vectors then creating a
            //    //  MultiCol Vector from this.  If the current variable has no parents than we need
            //    //  to create an empty vector of the appropriate length instead.
            //    Value.Vector[] parentArray = new Value.Vector[ parentList[current].length ];
            //    Value.Vector parentData;
            //    if ( parentArray.length != 0 ) {
            //        for ( int j = 0; j < parentArray.length; j++ ) {
            //        parentArray[j] = sample.cmpnt( parentList[current][j] );
            //         }
            //         parentData = new VectorFN.MultiCol( new Value.DefStructured(parentArray) );
            //         }
            //         else { // Have to manually create an empty vector, otherwise length == 0
            //         parentData =
            //             new VectorFN.UniformVector(numSamples ,
            //                            new Value.DefStructured(new Value[] {}) );
            //         }







            //         //
            //         // If the current value is not known at input or output, simply generate a vector
            //         // of values based on parent data.
            //         //
            //         if ( input.cmpnt(current).status() == Value.S_UNOBSERVED &&
            //          output.cmpnt(current).status() == Value.S_UNOBSERVED  ) { // if value unobserved


            //         }
            //         //
            //         //  If the current attribute was unobserved as an input, but obvserved as an output we
            //         //  must make all outputs equal to the observed value.  In addition to this we must
            //         //  scale all weight using likelyhood weighting.
            //         //
            //         else if (input.cmpnt(current).status() == Value.S_UNOBSERVED &&
            //              output.cmpnt(current).status() == Value.S_PROPER ){

            //         Value.Vector vec = sample.cmpnt( current );
            //         for ( int j = 0; j < match.length; j++ ) {
            //             if ( !vec.elt(j).equals( output.cmpnt(current) ) ) {
            //             match[j] = false;
            //             }
            //         }

            //         }
            //         //
            //         // If the current value is known at input and output, do a check to make sure they
            //         // are the same.  If so, simply set the output sample to the appropriate value.
            //         // If the two values are different, throw an exception.
            //         // In addition to this all values must be weighted for likelyhood weighting.
            //         //
            //         else if (input.cmpnt(current).status() == Value.S_PROPER &&
            //              output.cmpnt(current).status() == Value.S_PROPER ){

            //         // This should only occur when querying the probability of a state when a value
            //         // for that state has already been given.  Prob should equal zero.
            //         if ( !input.cmpnt(current).equals(output.cmpnt(current)) ) {
            //             for ( int j = 0; j < match.length; j++ ) {
            //             weight[j] = 0.0;
            //             }
            //         }
            //         else {
            //             for ( int j = 0; j < weight.length; j++ ) {
            //             double likelyhood = subModelList[current].logP( output.cmpnt(current),
            //                                     subModelParamList[current],
            //                                     parentData.elt(j) );
            //             weight[j] *= Math.exp( likelyhood );
            //         }

            //         }

            //         }
            //         //
            //         // If there is an intervention on the input
            //         //
            //         else if ( input.cmpnt(current).status() == S_INTERVENTION &&
            //               input.cmpnt(current).status() == S_INTERVENTION ) {

            //         // Make sure the value has not been modified
            //         if ( !input.cmpnt(current).equals(output.cmpnt(current)) ) {
            //             throw new RuntimeException("Intervened upon variable differs from in->out");
            //         }
            //         else {
            //         }

            //         }
            //         //
            //         // If we have a value at input, but not on output throw an exception.
            //         //
            //         else {
            //         throw new RuntimeException( "Invalid Status flag combination" );
            //         }
            //     }

            //     double totalWeight = 0;
            //     double totalMatches = 0;
            //     for ( int i = 0; i < weight.length; i++) {
            //         if ( match[i] == true ) {
            //         totalMatches += weight[i];
            //         }
            //         totalWeight += weight[i];
            //     }

            //     if ( totalWeight == 0 ) {
            //         return Math.log(0);
            //     }
            //     else {
            //         return Math.log(totalMatches / totalWeight);
            //     }
        }
    }

    /** Return a copy of the network as a string. */
    public String toString( Value.Vector params, String name, String comment ) {
        synchronized ( Netica.env ) {
            try {
                Net net = buildNet( name, params );
                net.setComment( comment );
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();


                Streamer stringStreamer = new Streamer( out, name, Netica.env );
                net.write( stringStreamer );
                stringStreamer.finalize(); // close file.

                return out.toString();
            }
            catch ( NeticaException e ) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

    }

    transient Net lastNet;
    String lastName;
    Value.Vector lastParams;

    /**
     *  Create a netica network from a set of parameters.
     *  If buildNet 
     */
    Net buildNet( String networkName, Value.Vector params ) throws NeticaException
    {
        if ( lastParams == params && lastName == networkName ) {
            return lastNet;
        }
        //     else {
        //         if ( Environ.getDefaultEnviron() != null ) {
        //         Environ.getDefaultEnviron().finalize();
        //         }
        //     }

        //     // Node.setConstructorClass ("norsys.neticaEx.aliases.Node");
        //     Environ env = new Environ (null);


        Net net = new Net();
        net.setName( networkName );

        int numVars = params.length();
        Node[] nodeList = new Node[numVars];

        //    Type.Structured sType = (Type.Structured)((Type.Vector)params.t).elt;
        Type.Structured dataType = (Type.Structured)((Type.Model)t).dataSpace;
        Type.Discrete[] typeArray = new Type.Discrete[numVars];
        String[] nameArray = new String[numVars];
        for ( int i = 0; i < numVars; i++ ) {
            typeArray[i] = (Type.Discrete)dataType.cmpnts[i];
        }
        nameArray = NeticaFn.makeValidNeticaNames( dataType.labels, false );


        for ( int i = 0; i < nodeList.length; i++ ) {

            // put each state name into an array of strings
            String[] stateName;
            if ( typeArray[i] instanceof Type.Symbolic ) {
                //         String[] temp = ((Type.Symbolic)typeArray[i]).ids;
                //         stateName = new String[temp.length];
                //         for ( int j = 0; j < stateName.length; j++ ) {
                //             stateName[j] = NeticaFn.makeValidNeticaName( temp[j] );
                //         }
                stateName =
                    NeticaFn.makeValidNeticaNames(((Type.Symbolic)typeArray[i]).ids, false);

            }
            else {
                int arity = (int)typeArray[i].UPB - (int)typeArray[i].LWB + 1;
                stateName = new String[arity];
                // Initialise names to integer values.
                for ( int j = 0; j < stateName.length; j++ ) {
                    stateName[j] = Integer.toString( (int)typeArray[i].LWB + j );
                }
                // Make sure netica will accept these names.
                stateName = NeticaFn.makeValidNeticaNames( stateName, true );
            }


            // netica wants names in the format "var1,var2,var3"
            // ... not sure of the logic there.
            StringBuffer buf = new StringBuffer();
            for ( int j = 0; j < stateName.length; j++ ) {
                buf.append( /*NeticaFn.makeValidNeticaName(*/ stateName[j] /*)*/ );
                if ( j != stateName.length - 1 ) {
                    buf.append(",");
                }
            }

            String stateNames = buf.toString();
            nodeList[i] = new Node( nameArray[i], stateNames, net );
        }

        // now loop through and add links between nodes.
        Value.Structured[] nodeStruct =
            new Value.Structured[nodeList.length];
        for ( int i = 0; i < nodeList.length; i++ ) {

            nodeStruct[i] = (Value.Structured)params.elt(i);
            Value.Vector parents = (Value.Vector)nodeStruct[i].cmpnt(1);
            Value.Structured modelParamStruct =
                (Value.Structured)nodeStruct[i].cmpnt(2);
            Value.Model model = (Value.Model)modelParamStruct.cmpnt(0);
            Value modelParams = modelParamStruct.cmpnt(1);
            int targetNodeArity = (int)typeArray[i].UPB - (int)typeArray[i].LWB + 1;


            for ( int j = 0; j < parents.length(); j++ ) {
                nodeList[i].addLink( nodeList[parents.intAt(j)] );
            }

            int[] parentArray = new int[parents.length()];
            int[] parentArity = new int[parentArray.length];

            int parentCombinations = 1;
            for( int j = 0; j < parentArray.length; j++ ) {
                parentArray[j] = parents.intAt(j);
                parentArity[j] = (int)typeArray[parentArray[j]].UPB -
                    (int)typeArray[parentArray[j]].LWB + 1;
                parentCombinations *= parentArity[j];
            }


            int[] parentValue = new int[parentArray.length];

            for ( int j = 0; j < parentCombinations; j++ ) {

                float[] prob = new float[ targetNodeArity ];
                double totalP = 0.0;
                for ( int k = 0; k < targetNodeArity; k++ ) {
                    Value.Discrete output =    new Value.Discrete(  typeArray[i], k );
                    Value.Discrete[] inputArray =     new Value.Discrete[parentValue.length];
                    for ( int l = 0; l < inputArray.length; l++ ) {
                        inputArray[l] = new Value.Discrete( typeArray[parentArray[l]], parentValue[l] );
                    }
                    Value.Structured input = new Value.DefStructured( inputArray );
                    prob[k] = (float)Math.exp(model.logP( output, modelParams, input ));
                    totalP += prob[k];
                }
                // Make sure prob[] sums to exactly 1.0, netica is very picky about this.
                for ( int k = 0; k < prob.length; k++) {
                    prob[k] /= totalP;
                }

                nodeList[i].setCPTable( parentValue, prob );


                // increment parentValue
                for ( int k = parentValue.length - 1; k >= 0; k-- ) {
                    parentValue[k] ++;
                    if ( parentValue[k] == parentArity[k] ) {
                        parentValue[k] = 0;
                    }
                    else {
                        break;
                    }
                }
            }

        }

        lastNet = net;
        lastParams = params;
        lastName = networkName;

        return net;
    }



    public String toString() {
        return "BNet Netica Model";
    }

    /** Calculate KL distance exactly if k <= 30, or stochastically if k > 30. */
    public double kl( Value.Vector params1, Value.Vector params2 ) {
        return klExact(params1,params2);
    }

    /** Calculate KL divergence between params1 and params2.
     *  This is a convenince function for exactKLNetica.*/
    public double klExact( Value.Vector params1, Value.Vector params2 ) {
        try {
            return exactKLNetica(
                                 new BNetNetica[] {this, new BNetNetica(getDataType())},
                                 new Value.Vector[] {params1,params2}, 0 )[1];
        } catch (NeticaException e) {
            throw new RuntimeException(e);
        }
    }

    /** Convenience function for exactKLNetica(m[],y[],trueModel) */
    public static double[] exactKLNetica( BNet m, Value.Vector[] y, int trueModel) 
        throws NeticaException {
        BNetNetica nets[] = new BNetNetica[y.length];
        for (int i = 0;i < nets.length; i++) { nets[i] = new BNetNetica(m.getDataType());}
        return exactKLNetica( nets, y, trueModel );
    }


    /** Calculate exact KL distance between networks. <br>
     *  Ideally, m should contain unique models as information about the compiled
     *  network is stored in the model, not the parameters. <br>
     *  An array of KL distances from m[trueModel] to m[i] is returned. 
     *  (ie. kl.length = m.length, kl[trueModel] = 0) 
     *  
     *  Our algorithm is taken from Cowell (Private communications): <br>
     *  
     *  suppose that P(X) = \prod_x p(x|pa_p(x)) and Q(X) = \prod_x q(x|pa_q(x))
     *  Where pa_p(x) are the parents of x in P, and pa_q(x) are those in Q.
     *  then the KL distance between P and Q simplifies as:
     *  KL = \sum_X  P(X)log(P(X)/Q(X)) 
     *     = \sum_x \sum_fap(x) p(x,pa_p(x))log[p(x|pa_p(x)]
     *     - \sum_x \sum_faq(x) p(x,pa_q(x))log[q(x|pa_q(x))]
     * 
     *  Where \sum_fap(x) means sum of all states of the family of the variable 
     *  x in P, and ditto for \sum_faq(x).
     *
     *    <p>
     *  */    
    public static double[] exactKLNetica(BNetNetica[] m, Value.Vector[] y, int trueModel) 
        throws NeticaException {


        // Create & Compile networks.
        Net net[] = new Net[m.length];
        for (int i = 0; i < m.length; i++) {
            if (m[i] == m[trueModel] && i != trueModel)
                { m[i] = new BNetNetica(m[trueModel].dataType); }
            net[i] = m[i].buildNet( "KL_Network_"+i, y[i] );
            net[i].compile();
        }


        int[] arity = m[0].getArity();
        int n = arity.length;


        // Create array of "Netica compatible" variable names.
        // Without this we can write net.getNode( nameArray[i] )
        // Trying to get node[i] directly gives the wrong result as
        // netica modifies variable ordering.  Annoying.
        Value.Vector nameVec = y[0].cmpnt(0);
        String nameArray[] = new String[nameVec.length()];
        for (int i = 0; i < nameVec.length(); i++) {
            nameArray[i] = ((Value.Str)nameVec.elt(i)).getString();
        }
        NeticaFn.makeValidNeticaNames(nameArray,true);


        // partialKL[i] = \sum_x \sum_faq(x) p(x,pa_q(x))log[q(x|pa_q(x))]
        // when i == 0, this is equivalent to \sum_x \sum_fap(x) p(x,pa_p(x))log[p(x|pa_p(x)]
        // So, kl[i] = partialKL[0] - partialKL[i]
        // (Check javadoc for slightly more verbose explanation).
        double partialKL[] = new double[m.length];
        for (int i = 0; i < m.length; i++) {
            for (int node = 0; node < n; node++) {

                int parentCombinations = 1;

                // Create list of parents/children for this node.
                // A special list must be created using nodes fromt net[0] as we need this later.
                NodeList parentList = new NodeList(net[i]);
                NodeList parentChildList = new NodeList(net[i]);
                NodeList parentChildListP = new NodeList(net[trueModel]);

                parentChildList.add( net[i].getNode(nameArray[node]) );
                parentChildListP.add( net[trueModel].getNode(nameArray[node]) );

                // Extract model structure,
                Value.Vector parentVec =
                    (Value.Vector)((Value.Structured)y[i].elt(node)).cmpnt(1);
                int[] parentArity = new int[parentVec.length()];
                int[] parentChildArity = new int[parentArity.length+1];


                for (int j = 0; j < parentVec.length(); j++) {
                    int parent = parentVec.intAt(j);
                    parentArity[j] = arity[parent];
                    parentChildArity[j+1] = arity[parent];
                    parentCombinations *= arity[parent];

                    parentChildList.add( net[i].getNode(nameArray[parent]));
                    parentChildListP.add( net[trueModel].getNode(nameArray[parent]));
                    parentList.add( net[i].getNode(nameArray[parent]));
                }


                // Create data to use as input.
                int[] parentChildData = new int[parentChildArity.length];
                int[] parentData = new int[parentArity.length];



                // Calculate partialKL for this node.
                // ie. \sum_faq(x) p(x,pa_q(x))log[q(x|pa_q(x))] where x = node[i]
                for (int pState = 0; pState < parentCombinations; pState++) {
                    for (int x = 0; x < arity[node]; x++) {
                        parentChildData[0] = x;

                        // getJointProbability() is often slow here as
                        // we cannot guarentee that parentChildList0
                        // will be part of a single clique in net[0].
                        // This causes netica to use belief updating.
                        // There doesn't seem to be an easy way around
                        // this problem.
                        double parentChildProb_P =
                            net[trueModel].getJointProbability( parentChildListP, parentChildData);

                        if (parentChildProb_P != 0) {
                            // Calls to getJointProbability here are
                            // quick as all nodes are part of a single
                            // clique.
                            double parentChildProb_Q =
                                net[i].getJointProbability( parentChildList, parentChildData);
                            double parentProb_Q =
                                net[i].getJointProbability( parentList, parentData);
                            double logProbChildGivenParent_Q =
                                Math.log(parentChildProb_Q) - Math.log(parentProb_Q);
                            partialKL[i] += parentChildProb_P * logProbChildGivenParent_Q;
                        }
                    }
                    // Update parent and child data values for the next iteration.
                    BNet.incrementBitfield(parentChildData, parentChildArity);
                    BNet.incrementBitfield(parentData, parentArity);
                }
            }
        }

        double kl[] = new double[partialKL.length];
        for (int i = 0; i < kl.length; i++) { kl[i] = partialKL[trueModel] - partialKL[i];}

        return kl;
    }

    /***************************************
     * LH: Netica Serialization methods    *
     * copied almost verbatim from         *
     * mltools.learners.camml.CaMMLWrapper *
     ***************************************/

    /**
       Saves the netica net, then stores it to out.
    */
    private void writeObject(ObjectOutputStream out)
        throws IOException, NeticaException
    {
        if(lastNet != null) {
            // create a temporary netfile
            File netFile;
            try
                { netFile = File.createTempFile("cmlwrite", ".net"); }
            catch(IOException e)
                { throw new RuntimeException("Can't create temporary file!"); }

            netFile.deleteOnExit();

            netFile.delete();

            // save using netica (not sure whether the java file needs
            // to be deleted manually before this.
            lastNet.write(new Streamer(netFile.getPath()));

            // read back the file, store it as a stringbuffer
            BufferedReader in =
                new BufferedReader(new InputStreamReader(new FileInputStream(netFile)));
            StringBuffer buf = new StringBuffer();
            String line;

            while((line = in.readLine()) != null) {
                buf.append(line);
                buf.append('\n');
            }

            // write the length of the file as an int to out
            out.writeInt(buf.length());

            // write the whole file as a string to out.
            out.writeChars(buf.toString());

        }
        else { // this is so readObject knows to set net to null.
            out.writeInt(0);
        }

        // do the normal serialization
        out.defaultWriteObject();

    }

    /**
       recovers the netica save file from in, then loads it to restore lastNet.
    */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException, NeticaException
    {
        int len = in.readInt();

        if(len <= 0) {
            lastNet = null;
        }
        else {

            // save it to a temporary netfile
            File netFile;
            try
                { netFile = File.createTempFile("cmlread", ".net"); }
            catch(IOException e)
                { throw new RuntimeException("Can't create temporary file!"); }
            netFile.deleteOnExit();

            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(netFile)));

            // read and write the file
            for(int i = 0; i < len; i ++) {
                out.write((int)(in.readChar()));
            }

            out.close();

            // reinstate the netica net
            Streamer str = new Streamer(netFile.getPath());
            lastNet = new Net(str);

            lastNet.compile();

        }

        // do normal serialization
        in.defaultReadObject();
    }

    /** 
     *     Similar to exactCKLNetica but instead of using:
     *  KL = \sum_X  P(X)log(P(X)/Q(X))
     *     = \sum_x \sum_fap(x) p(x)p(x|pa_p(x)) log[p(x|pa_p(x)]
     *     - \sum_x \sum_faq(x) p(x)p(x|pa_q(x)) log[q(x|pa_q(x))]
     *
     *  we use:
     * CKL3= \sum_X  P(X)log(P(X)/Q(X))
     *     = \sum_x \sum_fap(x) p(x)p(x|int_pa_p(x)) log[p(x|int_pa_p(x)]
     *     - \sum_x \sum_faq(x) p(x)p(X|int_pa_q(x)) log[q(x|int_pa_q(x))]
     *
     * NOTE: p(x|int_pa_p(x)) == p(x)p(x|pa_p(x))
     *       q(x|pa_q(x)) != q(x|int_pa_q(x)) (in general)
     *    <p>
     *  */
    public static double[] exactCKL3Netica(BNetNetica[] m, Value.Vector[] y,
                                           int trueModel) throws NeticaException {

        // Create & Compile networks.
        Net net[] = new Net[m.length];
        for (int i = 0; i < m.length; i++) {
            if (m[i] == m[trueModel] && i != trueModel)
                m[i] = new BNetNetica(m[trueModel].dataType);
            net[i] = m[i].buildNet("KL_Network_" + i, y[i]);
            net[i].compile();
        }

        int[] arity = m[0].getArity();
        int n = arity.length;

        // Create array of "Netica compatible" variable names.
        // Without this we can write net.getNode( nameArray[i] )
        // Trying to get node[i] directly gives the wrong result as
        // netica modifies variable ordering.  Annoying.
        Value.Vector nameVec = y[0].cmpnt(0);
        String nameArray[] = new String[nameVec.length()];
        for (int i = 0; i < nameVec.length(); i++) {
            nameArray[i] = ((Value.Str) nameVec.elt(i)).getString();
        }
        NeticaFn.makeValidNeticaNames(nameArray, true);

        // partialKL[i] = \sum_x \sum_faq(x) p(x,pa_q(x))log[q(x|pa_q(x))]
        // when i == 0, this is equivalent to \sum_x \sum_fap(x) p(x,pa_p(x))log[p(x|pa_p(x)]
        // So, kl[i] = partialKL[0] - partialKL[i]
        // (Check javadoc for slightly more verbose explanation).
        double partialKL[] = new double[m.length];
        for (int i = 0; i < m.length; i++) {
            for (int node = 0; node < n; node++) {

                int parentCombinations = 1;

                // Create list of parents/children for this node.
                // A special list must be created using nodes fromt net[0] as we need this later.
                NodeList parentList = new NodeList(net[i]);
                NodeList parentChildList = new NodeList(net[i]);
                NodeList parentChildListP = new NodeList(net[trueModel]);
                NodeList parentListP = new NodeList(net[trueModel]);

                parentChildList.add(net[i].getNode(nameArray[node]));
                parentChildListP.add(net[trueModel].getNode(nameArray[node]));

                // Extract model structure,
                Value.Vector parentVec = (Value.Vector) ((Value.Structured) y[i]
                                                         .elt(node)).cmpnt(1);
                int[] parentArity = new int[parentVec.length()];
                int[] parentChildArity = new int[parentArity.length + 1];

                // Add all parent nodes to parentChildList and parentChildListP
                // A seperate list is required for 'p' as netica gets confused.
                for (int j = 0; j < parentVec.length(); j++) {
                    int parent = parentVec.intAt(j);
                    parentArity[j] = arity[parent];
                    parentChildArity[j + 1] = arity[parent];
                    parentCombinations *= arity[parent];

                    Node n1 = net[i].getNode(nameArray[parent]);
                    Node n2 = net[trueModel].getNode(nameArray[parent]);
                    parentChildList.add(n1);
                    parentChildListP.add(n2);
                    parentList.add(n1);
                    parentListP.add(n2);

                }

                // Create data to use as input.
                int[] parentChildData = new int[parentChildArity.length];
                int[] parentData = new int[parentArity.length];

                //                System.out.print("i = " + i + "\t");
                //                System.out.print("node = " + node + "\t");
                //                System.out.print("parentListP = " + parentListP + "\t");
                //                System.out.println("parentChildListP = " + parentChildListP
                //                        + "\t in exactCKL3Netica");

                // Calculate partialKL for this node.
                // ie. \sum_faq(x) p(x,pa_q(x))log[q(x|pa_q(x))] where x = node[i]
                for (int pState = 0; pState < parentCombinations; pState++) {
                    for (int x = 0; x < arity[node]; x++) {
                        parentChildData[0] = x;

                        // getJointProbability() is often slow here as we cannot guarentee that parentChildList0
                        // will be part of a single clique in net[0].  This causes netica to use belief updating.
                        // There doesn't seem to be an easy way around this problem.
                        double parentProb_P = net[trueModel]
                            .getJointProbability(parentListP, parentData);
                        double parentChildProb_P = net[trueModel]
                            .getJointProbability(parentChildListP,
                                                 parentChildData);

                        //double parentChildProb_P = net[trueModel].getJointProbability( parentChildListP, parentChildData);
                        // We actually want to intervene on these parents.
                        // One way to do this is to chop all arcs entering these nodes.
                        for (Object parentNode : parentListP) {
                            //                            System.out.println("" + parentNode + ".parents = "
                            //                                    + ((Node) parentNode).getParents() + "\t"
                            //                                    + ((Node) parentNode).getNet());
                            //NodeEx.deleteLinksEntering( (Node)parentNode );
                        }
                        net[trueModel].compile();

                        /*
                          double intChildProb_P = net[trueModel].getJointProbability( parentChildListP, parentChildData) /
                          net[trueModel].getJointProbability( parentListP, parentData);
                          double parentChildProb_P = parentProb_P * intChildProb_P;
                        */

                        // Reload networks (as we destroyed them)
                        net[trueModel] = m[trueModel].buildNet("KL_Network_"
                                                               + trueModel, y[trueModel]);
                        net[trueModel].compile();

                        if (parentChildProb_P != 0) {
                            // Calls to getJointProbability here are quick as all nodes are part of a single clique.
                            double parentChildProb_Q = net[i]
                                .getJointProbability(parentChildList,
                                                     parentChildData);
                            double parentProb_Q = net[i].getJointProbability(
                                                                             parentList, parentData);
                            double logProbChildGivenParent_Q = Math
                                .log(parentChildProb_Q)
                                - Math.log(parentProb_Q);
                            partialKL[i] += parentChildProb_P
                                * logProbChildGivenParent_Q;

                            //System.out.println("pp = " + parentProb_P + "\tint_cp = " + intChildProb_P + "\tpp*i_cpt = " + parentChildProb_P
                            //        + "\tpcq = " + (parentChildProb_Q / parentProb_Q) );

                        }
                    }
                    // Update parent and child data values for the next iteration.
                    BNet.incrementBitfield(parentChildData, parentChildArity);
                    BNet.incrementBitfield(parentData, parentArity);
                }

            }
        }

        double kl[] = new double[partialKL.length];
        for (int i = 0; i < kl.length; i++) {
            kl[i] = partialKL[trueModel] - partialKL[i];
        }

        return kl;
    }

}


