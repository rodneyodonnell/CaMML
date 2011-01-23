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
// Functions for working with Tetrad IV in CDMS.
//

// File: Tetrad4FN.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.tetrad4;


import java.util.ArrayList;
import java.util.Random;

import cdms.core.*;
import camml.core.search.TOM;
import camml.core.models.ModelLearner;
import camml.core.models.mixture.Mixture;

import camml.core.models.bNet.BNet;
import camml.core.models.bNet.BNetStochastic;


import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.*;

/**
 * Class contains various functions for working with Tetrad IV in the CDMS environment,
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision $ $Date: 2006/11/13 14:01:11 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tetrad4/Tetrad4FN.java,v $
 */
public class Tetrad4FN {
    
    /** -1 = unlimited depth. */
    final static int depth = -1;

    /** Run tetrad4 with the given parameters. 
     *  Valid metrics include "fci", "pc" and "ges" <br>     
     *  If repair=true, the repair function is run to remove directed cycles introduced by tetrad.
     *  If rerun=true, tetrad will be rerun at lower significance levels until no directed
     *     cycles are present.  Rerun overrides repair.
     *  Prior constraints may be given, pass null to use default.
     */
    public static Graph runTetrad( Value.Vector data, String metric, double significance, 
                                   int depth, boolean repair, boolean rerun, 
                                   Knowledge prior ) {
        
        // Setup
        RectangularDataSet tetData = Tetrad4.cdms2TetradDiscrete( data );
        Knowledge knowledge = prior;
        if (knowledge == null) knowledge = new Knowledge(); 
        
        IndependenceTest independence = new IndTestGSquare((RectangularDataSet) tetData, significance);

        // Run Search
        Graph graph;
        if ( "pc".equals(metric) ) {
            PcSearch pcSearch = new PcSearch(independence, knowledge);
            pcSearch.setDepth(depth);
            graph = pcSearch.search();

            // If graph returned is cyclic, recursively run tetrad at lower significance levels.
            if ( rerun == true && graph.existsDirectedCycle() ) {                
                System.out.println("Rerunning at significance = " + (significance * 0.9) );
                graph = runTetrad( data, metric, significance * 0.9, depth, false, true, knowledge );
            }
        }
        else if ( "ges".equals(metric) ) {
            //System.out.println("tetData = " + tetData + "\t in runTetrad");
            GesSearch gesSearch = new GesSearch(tetData);
            gesSearch.setKnowledge(knowledge);
            // samplePrior of 3 works much better than default of 10.
            //gesSearch.setSamplePrior(3);
            graph = gesSearch.search();
        }
        else if ( "fci".equals(metric) ) {
            FciSearch fciSearch = new FciSearch(independence, knowledge);
            graph = fciSearch.search();
        }
        else {
            throw new RuntimeException("Invalid Metric: " + metric);
        }                  

        System.out.println( graph );

        if (repair)  {graph = Tetrad4.repair(graph); }
        return graph;
    }

    /** Run tetrad4 using the metric specified.
     *  A mixture model of BNets is returned.  All BNets consistent with the 
     *   structure returned by tetrad are enumerated.  A uniform prior is used
     *   over this group of DAGs. <br>
     *  metric = "pc", "ges" or "fci"
     */
    public static Value.Structured mixTetrad( Value.Vector data, String metric, double significance,
                                              int depth, ModelLearner learner, 
                                              boolean repair, boolean rerun,
                                              Knowledge prior ) 
        throws ModelLearner.LearnerException    {

        final Graph graph = runTetrad( data, metric, significance, depth, repair, rerun, prior );
        System.out.println("Tetrad Graph = " + graph );

        // Enumerate all Dags based on the learned tetrad model.
        TOM[] dag = Tetrad4.enumerateDAGs( data, graph );

        // Tetrad IV sometimes produces cyclic graphs.
        // When this happens (dag.length==0), very annoying.
        if ( dag.length == 0 ) {
            throw new ModelLearner.LearnerException("Invalid model found: " + graph);
        }

        // Parameterize each TOM
        Value.Vector[] params = new Value.Vector[dag.length];        
        for ( int i = 0;  i < dag.length; i++ ) {
            params[i] = dag[i].makeParameters( learner );
        }
        
        // Create a bNet model over the dataspace
        BNet bNet = new BNetStochastic( (Type.Structured)((Type.Vector)data.t).elt );

        // Calculate posterior of each model (ie. uniform)
        double posterior = 1.0/dag.length;

        // Create a mixture model of type (mixModel, [posterior,model,params] )
        Value.Vector paramVec = new VectorFN.FatVector( params );
        Value.Vector posteriorVec = new VectorFN.UniformVector( dag.length, 
                                                                new Value.Continuous(posterior) );
        Value.Vector modelVec = new VectorFN.UniformVector( dag.length,    bNet );        
        Value.Vector mixParams = new VectorFN.MultiCol( new Value.DefStructured( new Value[] 
            {posteriorVec, modelVec, paramVec } ) );

        return new Value.DefStructured( new Value[] { Mixture.mixture, mixParams} );
    }

    public static Graph arcMatrixToGraph( boolean[][] arcs, String[] names ) {
        int n = arcs.length;
        
        // empty graph containing arcs.length nodes.
        ArrayList<Node> nodeList = new ArrayList<Node>();
        for (int i = 0; i < arcs.length; i++) {
            if (names == null) { nodeList.add( new GraphNode("node_"+i) ); }
            else { nodeList.add( new GraphNode(names[i]) ); } 
        }
        EdgeListGraph g = new EdgeListGraph( nodeList );
        
        // Add appropriate arcs to graph
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (arcs[i][j]) {
                    if ( !arcs[j][i] ) { g.addDirectedEdge(nodeList.get(j), nodeList.get(i));}
                    else if (i < j) { g.addUndirectedEdge(nodeList.get(i), nodeList.get(j));}
                }
            }
        }
        
        // Return graph;
        return g;
    }
    
    /** Run tetrad4 using the metric specified.
     *  A single BNets consistent with the pattern learned by tetrad is returned.  
     *  BNet is chosen uniformly randomly over all BNets consistent with the
     *  returned tetrad SEC.
     *  
     *  metric = "pc", "ges" or "fci"
     */
    public static Value.Structured singleTetrad( Random rand,
                                                 Value.Vector data, String metric, 
                                                 double significance, int depth, 
                                                 ModelLearner learner,
                                                 boolean repair, boolean rerun,
                                                 Knowledge prior, boolean stringParams ) 
        throws ModelLearner.LearnerException    {

        final Graph graph = runTetrad( data, metric, significance, depth, repair, rerun, prior );
        System.out.println("Tetrad Graph = " + graph );

        // Enumerate all Dags based on the learned tetrad model.
        TOM[] dag = Tetrad4.enumerateDAGs( data, graph );

        // Tetrad IV sometimes produces cyclic graphs.
        // When this happens (dag.length==0), very annoying.
        if ( dag.length == 0 ) {
            throw new ModelLearner.LearnerException("Invalid model found: " + graph);
        }

        // Choose a dag at random from all consistent DAGs.
        TOM chosenTOM = dag[ rand.nextInt(dag.length) ];

        // Parameterize each TOM
        Value.Vector params = chosenTOM.makeParameters( learner );
        
        // Create a bNet model over the dataspace
        BNet bNet = new BNetStochastic( (Type.Structured)((Type.Vector)data.t).elt );

        Value.Str secString = new Value.Str( graph.toString() );
        
        // Return (model,params) pair
        if (stringParams) {return new Value.DefStructured( new Value[] { bNet, secString} );}
        else return new Value.DefStructured( new Value[] { bNet, params} );
    }

}
