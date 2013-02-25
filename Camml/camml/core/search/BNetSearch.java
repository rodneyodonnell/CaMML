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
// Base BNet Search for CaMML
//

// File: BNetSearch.java
// Author: rodo@dgs.monash.edu.au

package camml.core.search;

import java.io.StringReader;

import cdms.core.*;
import cdms.plugin.search.*;

import camml.core.models.ModelLearner;
import camml.core.models.bNet.BNet;
import camml.core.models.bNet.BNetStochastic;
import camml.plugin.netica.BNetNetica;
import camml.plugin.tomCoster.ExpertElicitedTOMCoster;

/**
 *  BNetSearch is designed as a base for BNet searched to be built upon.  Several commonly used
 *   functions are implemented.
 */
public abstract class BNetSearch implements Search.SearchObject 
{
    /** Constructor */
    public BNetSearch( java.util.Random rand, CaseInfo caseInfo )
    {
        this.rand = rand;
        this.fullData = caseInfo.data;
        this.numNodes = ((Value.Structured) fullData.elt(0)).length();
        this.mlModelLearner = caseInfo.mlModelLearner;
        this.mmlModelLearner = caseInfo.mmlModelLearner;
        this.caseInfo = caseInfo;
        
        // Arrays for caching partial costs
        this.mlCostArray = new double[numNodes];
        this.cleanMLCostArray = new double[numNodes];
        this.mmlCostArray = new double[numNodes];
        this.cleanMMLCostArray = new double[numNodes];
        
        // Initialise arcProb and search temperature.
        if ( caseInfo.tomCoster != null ) {
            tomCoster = caseInfo.tomCoster;
        } else {
            tomCoster = new TOMCoster.UniformTOMCoster( 0.5 );
        }
        setArcProb( 0.5 );
        setTemperature( 1.0 );
        
        // Create and cost empty TOM
        this.tom = new TOM( caseInfo );
        tomCoster.repairTOM(tom);     // Ensure any constraints are met
        this.currentCost = costNetwork( mmlModelLearner, false );
        
        this.cleantom = (TOM)tom.clone();
        
        // Save empty TOM as best TOM
        this.bestTOM = (TOM)tom.clone();
        this.bestCost = this.currentCost;
    }
    
    /** Alternate constructor.  CaseInfo created automatically. Hash function set to NULL */
    public BNetSearch(java.util.Random rand, Value.Vector data,
                      ModelLearner mlModelLearner, ModelLearner mmlModelLearner ) 
    {
        this( rand, new CaseInfo( null, null, data, mmlModelLearner, mlModelLearner, 
                                  Double.POSITIVE_INFINITY,
                                  new NodeCache( data, mmlModelLearner, mlModelLearner ) ));
        caseInfo.nodeCache.caseInfo = caseInfo;
    }
    
    /** recalculate bestCost and currentCost, usually done after updating arcProb */
    public void recalculateCosts() {
        this.bestCost = bestTOM.getCost();
        this.currentCost = costNetwork( mmlModelLearner, false );
    }
    
    /** Accesor function for MML Model Learner */
    public ModelLearner getMMLModelLearner() { return mmlModelLearner; }
    
    /** Accesor function for ML Model Learner */
    public ModelLearner getMLModelLearner() { return mlModelLearner; }
    
    /** Return a BNet Model from caseInfo*/
    public BNet getBNet() { return caseInfo.bNet; }
    
    /** Return parameterization of the current TOM */
    public Value.Vector getCurrentParams( ModelLearner modelLearner) 
        throws ModelLearner.LearnerException {
        return getTOM().makeParameters( modelLearner );
    }
    
    /** Return parameterization of best TOM. */
    public Value.Vector getBestParams( ModelLearner modelLearner) 
        throws ModelLearner.LearnerException {
        return getBestTOM().makeParameters( modelLearner );
    }
    
    /** The tom being worked on */
    protected TOM tom;

    /** Clean version of working TOM */
    protected TOM cleantom;
    
    /** The cost of the current TOM */
    protected double currentCost;
    
    /** The best TOM found so far*/
    protected TOM bestTOM;
    
    /** The cost of bestTOM */
    protected double bestCost;
    
    /** caseUnfo holds assorted values relevent to the current search. */
    public CaseInfo caseInfo;
    
    /** Function to make Skeletal changes to TOMs  */
    protected SkeletalChange skeletalChange;
    
    /** Function to make Temporal changes to TOMs  */
    protected TemporalChange temporalChange;
    
    /** Function to make double skeletal changes to TOMs  */
    protected DoubleSkeletalChange doubleSkeletalChange;
    
    /** Remove one parent and replace with a non parent (similar to doubleSwap) */
    protected ParentSwapChange parentSwapChange;
    
    /** Costing function for TOM structure */
    protected TOMCoster tomCoster;
    
    /** vector containing the full data of the network */
    protected final Value.Vector fullData;
    
    /** Number of nodes/variables in the network */
    protected final int numNodes;
    
    /** Random number generator */
    protected final java.util.Random rand;
    
    /** Learner to find MML estimates of node costs and parameters */
    protected final ModelLearner mmlModelLearner;
    
    /** Leraner to find ML estimated of node costs and parameters */
    protected final ModelLearner mlModelLearner;
    
    /** Prior on arcs being present. */
    protected double arcProb = 0.5;
    
    /** Has arcProb been fixed to a single value? */
    protected boolean fixedArcProb;
    
    /** Temperature to run search at. */
    protected double temperature = 1.0;
    
    /** Has temperature been fixed to a single value? */
    protected boolean fixedTemperature;
    
    /** flag if search is complete or not. */
    protected boolean searchDone = false;
    
    /** Current Epoch */
    public /*protected*/ long epoch = 0; // temporarily made public for jython
    
    /** 
     * Set an option by name to a given value. Some options are search specific. <br> 
     * Return false if option name is unknown <p>
     * 
     *  General options include: <br>
     *    <b>arcProb</b>     -- Fix the probability of arc existance at a value                            <br>
     *    <b>temperature</b> -- Fix temperature at a set value                                             <br>
     *    <b>currentTOM</b>  -- Set initial TOM to a given value                                           <br>
     *    <b>clean</b>       -- Should model cleaning be used?                                             <br>
     *  <b>searchFactor</b>-- Multiplier to search/sample time. Default is 1.0.                          <br> 
     *    <p>
     *    Metropolis only options <br>
     *  <b>joinDAGs</b>    -- Join DAGs to form SECs. (default == true)<br>
     *  <b>joinSECs</b>    -- Join SECs to form MMLECs. (defalut == true) <br>
     *  <b>allowMergeToModelWithMoreArcs</b> -- When joining SECs to MMLECs, allow the representative
     *                   SEC to have more arcs than SEC it is being joined too. (default == false) <br>
     *  <b>sortTOMsBy</b>  -- "weight"|"mml", What metric should we use to rank TOMs within a SEC? 
     *                  Default == "weight" <br>
     *  <b>regression</b>  -- Sets options to the values used in oldCamml. Also makes an attempt to ensure
     *                  random number generators line up, etc. Only useful for regressiont testing. <br>
     *    <b>TOMPrior</b>    -- Use expert ellicited prior over network.  String passed as option reflects the
     *                   prior specified.  For details see {@link ExpertElicitedTOMCoster}
     *    <b>maxNumSECs</b>     -- Maximum number of SECs retained post metropolis sampling. 
     *    <b>minTotalPosterior</b> -- Minimal posterior retained post metropolis sampling.
     *                    If more than 'maxSECs' SECs are required, this condition is ignored. 
     *    <b>useNetica</b> -- Return BNetNetica instead of BNetStochastic models.
     */ 
    public boolean setOption( final String option, Value v ) {
        if ( option.equals("arcProb") ) {        
            setArcProb( ((Value.Scalar)v).getContinuous() );
            fixedArcProb = true;        
            System.out.println( "Setting arcProb = " + v );
        }
        else if ( option.equals("temperature") ) {
            setTemperature( ((Value.Scalar)v).getContinuous() );
            fixedTemperature = true;        
            System.out.println( "Setting temperature = " + v );
        }
        else if ( option.equals("currentTOM") ) {
            setTOMStructure( tom, (Value.Vector)v );
        }
        else if ( option.equals("clean") ) {
            boolean clean = (((Value.Discrete)v).getDiscrete() == 0);
            if (clean == false) { caseInfo.tomCleaner = TOMCleaner.NoCleanTOMCleaner.tomCleaner; }
            System.out.println( "Setting arc cleaning: " + clean );
        }
        else if ( option.equals("joinDAGs") ) {
            caseInfo.joinDAGs = (((Value.Discrete)v).getDiscrete() == 0);        
            System.out.println( "Joining DAGs to form SECs: " + caseInfo.joinDAGs );
        }
        else if ( option.equals("joinSECs") ) {
            caseInfo.joinSECs = (((Value.Discrete)v).getDiscrete() == 0);        
            System.out.println( "Joining SECs by KL distance: " + caseInfo.joinSECs );
        }
        else if ( option.equals("allowMergeToModelWithMoreArcs") ) {
            caseInfo.allowMergeToModelWithMoreArcs = (((Value.Discrete)v).getDiscrete() == 0);        
            System.out.println( "Allow merge to model with more arcs: " + caseInfo.allowMergeToModelWithMoreArcs );
        }
        else if ( option.equals("sortTOMSBy") ) {
            String s = ((Value.Str)v).getString();
            if ( s.equals("weight") ) { caseInfo.tomComparator = SEC.tomWeightComparator;}
            else if (s.equals("MML") ) { caseInfo.tomComparator = SEC.tomMMLComparator; }
            else { s = "Unknown option.  Ignoring"; }
            
            System.out.println( "Sorting TOMs by : " + s );
        }
        else if ( option.equals("regression") ) {
            caseInfo.regression = (((Value.Discrete)v).getDiscrete() == 0);        
            System.out.println( "Setting regression: " + caseInfo.regression );
        }
        else if ( option.equals("TOMCoster") ) {
            setTOMCoster((TOMCoster)((Value.Obj)v).getObj());            
            System.out.println( "Setting TOMCoster: " + tomCoster );
        }
        else if ( option.equals("TOMPrior") ) {
            String x = ((Value.Str)v).getString();
            setTOMCoster( new ExpertElicitedTOMCoster(arcProb,new StringReader(x), tom.data) );            
            System.out.println( "Setting TOMCoster: " + tomCoster );
        }
        else if ( option.equals("searchFactor") ) {
            double d = ((Value.Scalar)v).getContinuous();
            caseInfo.searchFactor = d;
        }
        else if ( option.equals("maxNumSECs") ) {
            int x = ((Value.Scalar)v).getDiscrete();
            caseInfo.maxNumSECs = x;
        }
        else if ( option.equals("minTotalPosterior") ) {
            double d = ((Value.Scalar)v).getContinuous();
            caseInfo.minTotalPosterior = d;
        }
        else if ( option.equals("useNetica") ) {
            caseInfo.useNetica = (((Value.Discrete)v).getDiscrete() == 0);
            if (caseInfo.useNetica) 
                { caseInfo.bNet = new BNetNetica(caseInfo.bNet.getDataType());}
            else 
                {caseInfo.bNet = new BNetStochastic(caseInfo.bNet.getDataType());}
            System.out.println( "Using netica lib for inference: " + caseInfo.useNetica );
        }
        else if ( option.equals("printArcWeights") ) {
            caseInfo.printArcWeights = (((Value.Discrete)v).getDiscrete() != 0);
            caseInfo.updateArcWeights = caseInfo.printArcWeights;
            System.out.println( "Printing Arc Weights: " + caseInfo.useNetica );
        }

        else if ( option.equals("cklJoinType") ) {
            caseInfo.cklJoinType = ((Value.Discrete)v).getDiscrete();
            if ( caseInfo.cklJoinType != 0 && caseInfo.cklJoinType != 3) {
                throw new RuntimeException("Unknown join type. Should be 0 or 3");
            }
            System.out.println( "Setting Model join type to CKL"+caseInfo.cklJoinType );
            //caseInfo.joinDAGs = false;
            //System.out.println( "Joining DAGs to form SECs: " + caseInfo.joinDAGs );
            
        }
        else { // return false if an invalid option is passed.
            return false;
        }
        return true;
    }
    
    /** Setter for TOMCoster. */
    public void setTOMCoster( TOMCoster tc) { 
        this.tomCoster = tc;
        caseInfo.tomCoster = tomCoster;
    }
    
    /** Set the structure of the given tom to mimic the 2d vector [[]] passed in structure. */
    public static void setTOMStructure( TOM tom, Value.Vector structure ) {
        
        // Extract vector into arc matrix. 
        boolean[][] arc = new boolean[structure.length()][structure.length()];
        for ( int i = 0; i < arc.length; i++ ) {
            for ( int j = 0; j < arc.length; j++ ) {
                Value v = ((Value.Vector)structure.elt(i)).elt(j);
                arc[i][j] = (((Value.Discrete)v).getDiscrete() != 0);                    
            }
        }
        
        // Attempt to find a consistent total ordering
        // Has the current variable been placed in ordering?
        boolean[] used = new boolean[arc.length];  
        int[] order = new int[arc.length];
        int numPlaced = 0;
        
        // Perform a topological sort on the DAG
        while ( numPlaced < arc.length ) {
            int changes = 0;
            for ( int i = 0; i < arc.length; i++) {
                if ( used[i] == false ) { // if var[i] not placed yet
                    for ( int j = 0; j < arc.length; j++ ) {
                        // if arc j->i exists and j has not been placed, we canot place i.
                        if ( (i!=j) && arc[i][j] && !used[j] ) {break;}
                        // If all Js have been tested with no problems, we can add i
                        if ( j == arc.length -1 ) { 
                            order[numPlaced] = i; 
                            used[i] = true; 
                            numPlaced++; changes ++;
                        }                        
                    }
                }
            }
            // If no changes are made the links specified are not consistent with a DAG structure.
            if ( changes == 0 ) { 
                throw new RuntimeException("Inconsistent network in setTOMStructure"); 
            }
        }
        
        // Remove all arcs in original network
        for ( int i = 0; i < arc.length; i++ ) { 
            for (int j = 0; j < arc.length; j++ ){ 
                if ( (i!=j) && tom.isArc(i,j) ) { tom.removeArc(i,j); }
            }
        }
        
        // Initialise order
        for ( int i = 0; i < order.length; i++ ) {
            tom.swapOrder(tom.nodeAt(i),order[i],true);
        }        
        
        // Add arcs as required.
        for ( int i = 0; i < arc.length; i++ ) { 
            for (int j = 0; j < arc.length; j++ ){ 
                if ( (i!=j) && arc[i][j] ) { tom.addArc(i,j); }
            }
        }
    }
    
    /** Modify the current arc probability.  This effects structureCost and the TOM mutation
     *  operators (skeletalChange, temporalChange, doubleSkeletalChange, parentSwapChange)
     */
    public void setArcProb( double arcProb )
    {
        if ( fixedArcProb ) { throw new RuntimeException("ArcProb value unchangable"); }
        this.arcProb = arcProb;
        caseInfo.arcProb = arcProb;
        updateMutationOperators( arcProb, temperature );
    }
    
    protected double getTemperature() { return this.temperature; }
    
    /** Modify the current search temperature (defaults to 1.0).  Higher temperatures increase the
     * likelihood of a bad (i.e. high MML cost) model being accepted.
     */
    public void setTemperature( double temperature )
    {
        if ( fixedTemperature ) { throw new RuntimeException("Temperature value unchangable"); }
        this.temperature = temperature;    
        caseInfo.temperature = temperature;
        updateMutationOperators( arcProb, temperature );
    }
    
    /** Create new Mutation operators */
    protected void updateMutationOperators( double arcProb, double temperature ) 
    {
        skeletalChange = new SkeletalChange( rand, arcProb, caseInfo, temperature );
        temporalChange = new TemporalChange( rand, arcProb, caseInfo, temperature );
        doubleSkeletalChange = new DoubleSkeletalChange( rand, arcProb, caseInfo, temperature );
        parentSwapChange = new ParentSwapChange( rand, arcProb, caseInfo, temperature );
        //tomCoster = new TOMCoster.UniformTOMCoster( arcProb, caseInfo );
        tomCoster.setArcProb(arcProb);
        
        caseInfo.tomCoster = tomCoster;
    }
    
    /** return true is search finished */
    public boolean isFinished() 
    {
        return searchDone;
    }
    
    /** Print out the cost of each structural component. */
    public void printDetailedCost( TOM tom )
    {
        // Calculate the cost to state the total ordering
        double totalOrderCost = 0; // FN.LogFactorial.logFactorial( numNodes );
        System.out.println( "totalOrderCost = " + totalOrderCost );
        
        // now calculate the cost to state all the links in the model.
        double linkCost = 0;
        int numLinks = tom.getNumEdges();
        
        
        int maxNumLinks = (numNodes * (numNodes - 1)) / 2;
        
        linkCost = -1.0 * ( numLinks * Math.log( arcProb ) + 
                            (maxNumLinks - numLinks) * Math.log(1-arcProb));
        
        System.out.println( "linkCost = " + linkCost + 
                            "\t(numLinks = " + numLinks + " arcProb = " + arcProb + ")" );
        
        
        double totalCost = 0;
        
        // for each node
        for (int i = 0; i < tom.getNumNodes(); i++) {
            
            Node currentNode = tom.getNode(i);        
            double tempNodeCost = caseInfo.nodeCache.getCost( currentNode, mmlModelLearner );
            System.out.println( "Cost : " + currentNode + "\t = " + tempNodeCost );
            
            totalCost += tempNodeCost;
        }
        System.out.println("total node cost = " + totalCost );
        
        System.out.println("Total network cost = " + (totalOrderCost + linkCost + totalCost) );
    }
    
    
    /** Accesor funciton for tom */
    public TOM getTOM() { return tom; }
    
    /** Accesor function for bestTOM */
    public TOM getBestTOM() { return bestTOM; }
    
    /** Accessor function for bestCost */
    public double getBestCost() { return bestCost; }
    
    /** return nodeCost + structureCost */
    public double costNetwork( ModelLearner modelLearner, boolean clean )
    {
        return costNodes(modelLearner,clean,null) + structureCost( clean );
    }
    
    /** Contains partial calculation of network cost. */
    private double[] mlCostArray;
    /** Contains partial calculation of network cost. */
    private double[] cleanMLCostArray;
    /** Contains partial calculation of network cost. */
    private double[] mmlCostArray;
    /** Contains partial calculation of network cost. */
    private double[] cleanMMLCostArray;
    
    /** Network cost. */
    private double mlCost = -1, cleanMLCost = -1, mmlCost = -1, cleanMMLCost = -1;
    
    /** 
     * Calculate the cost of stating all CPT's in the network      
     * Only nodes listed in updateNodes[] are reloaded from the cache. <br>
     * Passing null to updateNodes[] loads all nodes from cache. <br>
     * Function should be called with null the first time it is used (to initialise costs.) <br> 
     * NOTE: Structure cost not included
     */
    //     NOTE: Changes made here should also be made in TOM.getCost()
    public double costNodes( ModelLearner modelLearner, boolean clean, int[] updateNodes ) 
    {
        // Choose [clean|unclean][ML|MML] costing to modify.
        double costArray[];
        double totalCost;
        
        // work out which costArray and totalCost to use.
        if ( modelLearner == mmlModelLearner && clean == false) 
            { costArray = mmlCostArray;  totalCost = mmlCost; }    
        else if ( modelLearner == mmlModelLearner && clean == true) 
            { costArray = cleanMMLCostArray;  totalCost = cleanMMLCost; }    
        else if ( modelLearner == mlModelLearner && clean == false ) 
            { costArray = mlCostArray; totalCost = mlCost; }
        else if ( modelLearner == mlModelLearner && clean == true ) 
            { costArray = cleanMLCostArray; totalCost = cleanMLCost; }
        else { costArray = new double[numNodes]; totalCost = -1; }
        
        TOM tom2;
        if ( clean ) {         
            // Ensure cleaning is done properly.
            updateNodes = null;
            tom2 = cleantom;
            cleantom.setStructure(tom);
            cleantom.clean();
        } else { 
            tom2 = tom; 
        }
        
        // Calculate cost for all nodes.
        if ( clean || updateNodes == null || totalCost == -1) {
            totalCost = 0;
            for ( int i = 0; i < numNodes; i++ ) {
                Node currentNode = tom2.getNode(i);
                costArray[i] = caseInfo.nodeCache.getCost( currentNode, modelLearner );
                totalCost += costArray[i];
            }
        }
        // Update only the listed nodes.
        else {
            for (int i = 0; i < updateNodes.length; i++) {
                
                // extract node to update
                int x = updateNodes[i];
                Node currentNode = tom2.getNode( x );
                
                // remove old cost from total
                totalCost -= costArray[x];
                
                // Update costArray
                costArray[x] = caseInfo.nodeCache.getCost( currentNode, modelLearner );
                
                // add new cost to total.
                totalCost += costArray[x];
            }
        }
        
        // TODO: totalCost should only be recalculated occasionally, not every time.
        totalCost = 0;
        for ( int i = 0; i < numNodes; i++ ) {
            totalCost += costArray[i];
        }
        
        // Make sure final cost makes sense.
        if ( Double.isNaN( totalCost ) ) {
            throw new RuntimeException("totalCost == NAN");
        }
        
        // Save cost to appropriate position.
        if ( modelLearner == mmlModelLearner && clean == false) 
            { mmlCost = totalCost; }
        else if ( modelLearner == mmlModelLearner && clean == true) 
            { cleanMMLCost = totalCost; }
        else if ( modelLearner == mlModelLearner && clean == false ) 
            { mlCost = totalCost; }
        else if ( modelLearner == mlModelLearner && clean == true ) 
            { cleanMLCost = totalCost; }
        
        return totalCost;
    }
    
    /** return the cost to state the total ordering and the link matrix.
     *  A cleaan structure will have less arcs so cost will differ.
     * 
     *  NOTE: Function currently only supports unclean costs.
     */    
    protected double structureCost( boolean clean )
    {
        if (clean == true) { throw new RuntimeException("Clean TOMs not handled properly."); }
        else { return caseInfo.tomCoster.cost(tom); }
        /*        
                  int numNodes = tom.getNumNodes();
        
                  // Calculate the cost to state the total ordering
                  double totalOrderCost = 0; //? FN.LogFactorial.logFactorial( numNodes );
        
                  // now calculate the cost to state all the links in the model.
                  double linkCost = 0;
        
                  // Count the number of edges in TOM (or in cleaned version of TOM)
                  int numLinks = 0;
                  if ( clean == true ) {
                  for ( int i = 0; i < tom.node.length; i++ ) {
                  Node tempNode = caseInfo.nodeCache.getCleanNode( tom.node[i] );
                  numLinks += tempNode.parent.length;
                  }
                  if ( numLinks > tom.getNumEdges() ) {
                  throw new RuntimeException("Clean TOM has more edges than original?");
                  }
                  } 
                  else {
                  numLinks = tom.getNumEdges();
                  }
        
                  int maxNumLinks = (numNodes * (numNodes - 1)) / 2;
        
                  // Calculate cost of stating links.
                  linkCost = -1.0 * ( numLinks * Math.log( arcProb ) + 
                  (maxNumLinks - numLinks) * Math.log(1-arcProb));
        
                  // Total cost to state the structure of a TOM
                  return linkCost + totalOrderCost;
        */
    }
}
