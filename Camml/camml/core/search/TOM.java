/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lucas Hope,  Lloyd Allison, Kevin Korb
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
// TOM for CaMML
//

// File: TOM.java
// Author: rodo@dgs.monash.edu.au, lhope@csse.monash.edu.au


package camml.core.search;


import camml.core.models.ModelLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;

import java.util.Random;

/**
 * TOM is a Totally Ordered Model, it is a causal model where the time ordering of the nodes is
 * completely determined. This is a stronger definition than a Causal Network, as a causal
 * network implies only a partial time ordering on the nodes.
 * <p/>
 * TOM-space varies in time ordering of nodes, and connection between nodes, and the TOM class is
 * designed for easy searching of TOM-space. It can also be scored using some evaluation metric.
 */
public class TOM implements Cloneable {

    /**
     * Object containing actual node ordering and connections.
     */
    protected final CoreTOM coreTOM;

    /**
     * Nodes used for parameterisation, etc.
     */
    private final Node[] nodes;

    /**
     * standard CDMS way of representing a dataset
     */
    protected final Value.Vector data;

    /**
     * caseInfo contains information about the current problem.
     */
    public final CaseInfo caseInfo;


    public int getMaxNumParents() {
        return coreTOM.getMaxNumParents();
    }

    /**
     * Builder for creating TOMs.
     *
     * @return builder
     */
    public static TOMBuilder builder() {
        return new TOMBuilder();
    }

    private TOM(TOMBuilder builder) {

        this.data = builder.data != null ? builder.data : builder.caseInfo.data;
        this.caseInfo = builder.caseInfo != null ? builder.caseInfo : new CaseInfo(null, null, data, null, null, -1, null);

        Type.Structured structure = (Type.Structured) ((Type.Vector) data.t).elt;
        final int numNodes = structure.cmpnts.length;
        final int maxNumParents = builder.maxNumParents != 0 ? builder.maxNumParents : CoreTOM.DEFAULT_MAX_NUM_PARENTS;

        this.coreTOM = new CoreTOM(numNodes, maxNumParents);
        this.nodes = new Node[numNodes];

        // iterate over the structure.
        for (int i = 0; i < numNodes; i++) {
            nodes[i] = new Node(coreTOM.getCoreNode(i));
        }
    }

    /**
     * Builder for creating TOMs.
     */
    public static class TOMBuilder {

        private Value.Vector data;
        private CaseInfo caseInfo;
        private int maxNumParents;

        private TOMBuilder() {
        }

        /**
         * Set dataset to be used by the TOM.
         * <p/>
         * If not specified, caseInfo.data is used.
         */
        public TOMBuilder setData(Value.Vector data) {
            this.data = data;
            return this;
        }

        /**
         * Create a fake dataset for use with this TOM.
         * NOTE: This constructor should be avoided as some TOM functions may not function correctly.
         */
        public TOMBuilder createFakeDataFromNumNodes(int numNodes) {
            setData(SearchDataCreator.generateData(1, numNodes));
            return this;
        }

        public TOMBuilder setCaseInfo(CaseInfo caseInfo) {
            this.caseInfo = caseInfo;
            return this;
        }

        public TOMBuilder setMaxNumParents(int maxNumParents) {
            this.maxNumParents = maxNumParents;
            return this;
        }

        public TOM build() {
            return new TOM(this);
        }
    }

    /**
     * get the data being evaluated
     */
    public Value.Vector getData() {
        return data;
    }


    // Delegated to coreTOM.

    /**
     * return number of variables in data.
     */
    public int getNumNodes() {
        return coreTOM.getNumNodes();
    }

    /**
     * return the number of edges present in this TOM
     */
    public int getNumEdges() {
        return coreTOM.getNumEdges();
    }

    /**
     * return whether an arc exists between two node indices.
     */
    public boolean isArc(int x, int y) {
        return coreTOM.isArc(x, y);
    }

    /**
     * add an arc by index.
     * Returns true if an operation was performed.
     */
    public boolean addArc(int x, int y) {
        return coreTOM.addArc(x, y);
    }

    /**
     * remove an arc by index.
     * Returns true if an operation was performed.
     */
    public boolean removeArc(int x, int y) {
        return coreTOM.removeArc(x, y);
    }

    /**
     * returns true if variable i is before variable j in the total ordering
     */
    public boolean before(int nodeI, int nodeJ) {
        return coreTOM.before(nodeI, nodeJ);
    }

    /**
     * Swap nodeX and nodeY in the total ordering
     */
    public void swapOrder(int x, int y) {
        coreTOM.swapOrder(x, y);
    }

    /* randomise the total order */
    public void randomOrder(Random rand) {
        coreTOM.randomOrder(rand, caseInfo.regression);
    }

    /**
     * remove all arcs from this TOM
     */
    public void clearArcs() {
        coreTOM.clearArcs();
    }


    /**
     * set all arcs randomly (0.5 prob of an arc)
     */
    public void randomArcs(Random generator) {
        coreTOM.randomArcs(generator, 0.5);
    }

    /**
     * set all arcs randomly (p prob of an arc)
     */
    public void randomArcs(Random rand, double p) {
        coreTOM.randomArcs(rand, p);
    }

    /**
     * return the Nth node in the total ordering.
     */
    public int nodeAt(int node) {
        return coreTOM.nodeAt(node);
    }

    /**
     * Return the position in the total ordering of node n
     */
    public int getNodePos(int node) {
        return coreTOM.getNodePos(node);
    }

    public int[] getParents(int childNode) {
        return coreTOM.getParents(childNode);
    }

    /**
     * Randomize the total ordering then fix it so it is consistent with arcs present.
     */
    public void buildOrder(Random rand) {
        coreTOM.buildOrder(rand, caseInfo.regression);
    }

    /**
     * Does a directed arc exist from i to j?
     */
    public boolean isDirectedArc(int i, int j) {
        return coreTOM.isDirectedArc(i, j);
    }

    /**
     * Two DAGs are considered equal if they have identical arcs, and those arcs are in the same
     * direction.  Defining DAG equality here may be more useful than TOM equality.
     */
    public boolean equals(Object o) {
        return (o instanceof TOM && coreTOM.dagEquals(((TOM) o).coreTOM));
    }

    /**
     * Set the current node ordering and edges based on arcs <br>
     */
    public void setStructure(int[][] parents) {
        coreTOM.setStructure(parents);
    }

    /**
     * Set the current node ordering and edges to those of tom2
     */
    public void setStructure(TOM tom2) {
        coreTOM.setStructure(tom2);
    }


    /**
     * Set the total ordering of the tom to order.
     */
    public void setOrder(int[] order) {
        coreTOM.setOrder(order);
    }


    /**
     * is a an ancestor of x? (I think its O(numnodes^2), might be O(nnumnodes^3)
     */
    public boolean isAncestor(int ancestorNode, int descendantNode) {
        return coreTOM.isAncestor(ancestorNode, descendantNode);
    }

    /**
     * is d a descendant of x?
     */
    public boolean isDescendant(int d, int x) {
        return coreTOM.isDescendant(d, x);
    }

    /**
     * Return true if node1 and node2 have are correlated, that is
     * NOT d-seperated. Correlation occurs when one node is an descendant
     * of the other, or they have a common ancestor.
     */
    public boolean isCorrelated(int node1, int node2) {
        return coreTOM.isCorrelated(node1, node2);
    }


    public String toNodeString(String[] name, int i) {
        StringBuffer s = new StringBuffer();
        s.append(name[i] + " : ");
        for (int parent : getParents(i)) {
            s.append(" <- " + name[parent]);
        }
        return s.toString();
    }


    /**
     * Create ascii version of TOM
     */
    public String toString() {
        String[] names = new String[getNumNodes()];
        Type.Structured dataType = (Type.Structured) ((Type.Vector) data.t).elt;
        for (int i = 0; i < names.length; i++) {
            if (dataType.labels != null) {
                names[i] = dataType.labels[i];
            } else {
                names[i] = "var(" + i + ")";
            }
        }

        StringBuffer s = new StringBuffer();
        for (int i = 0; i < getNumNodes(); i++) {
            s.append(toNodeString(names, i));
            s.append('\n');
        }
        return s.toString();
    }


    /**
     * Accessor function for node[]
     */
    public Node getNode(int n) {
        return nodes[n];
    }

    /**
     * Add every possible edge in this TOM to a maximum of maxParents. <br>
     * If the addition of extra nodes would give the node an infinite MML cost then it is not added.
     */
    public void fillArcs(int maxParents) {
        // Add up to maxParents arcs starting with nodes just before current node in total ordering.
        for (int child = 0; child < getNumNodes(); child++) {
            for (int parentIndex = getNodePos(child) - 1; (parentIndex >= 0) &&
                    (getParents(child).length < coreTOM.getMaxNumParents()); parentIndex--) {

                addArc(nodeAt(parentIndex), child);

                // If adding this arc makes the cost infinite, then don't add it.
                // Removing this can leave the tom in a nasty state of having an infinite cost.
                // This most commonly occurs when we try and build a CPT with way to many states
                // and an exception is thrown upstream.

                if (caseInfo.nodeCache != null &&
                        Double.isInfinite(caseInfo.nodeCache.getMMLCost(getNode(child)))) {
                    removeArc(nodeAt(parentIndex), child);
                    // break;
                }

            }
        }
    }

    /**
     * Using the current list of connections, create the parameter list required to interact with
     * a BNet model.  <br>
     * The format is : [ ( [parents], (submodel,subparams) ) ]
     */
    public Value.Vector makeParameters(ModelLearner modelLearner)
            throws ModelLearner.LearnerException {
        int numVars = getNumNodes();

        // Create arrays to hold initial structures.
        String name[] = new String[numVars];
        Value.Vector subParents[] = new Value.Vector[numVars];
        Value.Model[] subModel = new Value.Model[numVars];
        Value subModelParam[] = new Value[numVars];
        Value.Structured subParam[] = new Value.Structured[numVars];
        Value.Structured localStructure[] = new Value.Structured[numVars];


        // initialise name
        Type.Structured dataType = (Type.Structured) ((Type.Vector) data.t).elt;
        for (int i = 0; i < name.length; i++) {
            if (dataType.labels != null) {
                name[i] = dataType.labels[i];
            } else {
                name[i] = "var(" + i + ")";
            }
        }

        // set value of parents.
        for (int i = 0; i < subParents.length; i++) {
            subParents[i] = new VectorFN.FastDiscreteVector(getParents(i).clone());
        }

        // set CPT models and parameters for nodes.
        for (int i = 0; i < subModel.length; i++) {
            Value.Structured msy = getNode(i).learnModel(modelLearner, data);
            subModel[i] = (Value.Model) msy.cmpnt(0);
            subModelParam[i] = msy.cmpnt(2);
        }

        // ( subModel, subParam )
        for (int i = 0; i < subParam.length; i++) {
            subParam[i] = new Value.DefStructured(new Value[]{subModel[i], subModelParam[i]});
        }


        // ( [parants], ( subModel, subParam ) )
        for (int i = 0; i < localStructure.length; i++) {
            localStructure[i] = new Value.DefStructured(new Value[]{new Value.Str(name[i]),
                    subParents[i],
                    subParam[i]});
        }


        return new VectorFN.FatVector(localStructure);
    }


    /**
     * Return tomHash(tom=this, ml=0, clean=false) as an positive integer
     */
    public int hashCode() {
        long hash = caseInfo.tomHash.hash(this, 0);
        return ((int) hash);
    }


    /**
     * Clone the TOM.
     * - Deep copy of Node[] node <br>
     * - Deep copy of int[] totalOrder
     * - Deep copy of int[][] edge
     * - Shallow copy of Value.Vector data
     */
    public Object clone() {
        return new TOM(this);
    }

    private TOM(TOM tom) {
        this.data = tom.data;
        this.caseInfo = tom.caseInfo;

        int len = tom.getNumNodes();
        this.nodes = new Node[len];

        this.coreTOM = new CoreTOM(tom.coreTOM);

        // iterate over the structure.
        for (int i = 0; i < len; i++) {
            nodes[i] = new Node(coreTOM.getCoreNode(i));
        }
    }

    /**
     * Remove insignificant arcs from TOM
     */
    synchronized public void clean() {
        caseInfo.tomCleaner.cleanTOM(this);
    }


    /**
     * Calculate cost of TOM.
     */
    // NOTE: Changes made here should also be made in BNetSearch.costNodes
    public double getCost() {
        double structureCost = caseInfo.tomCoster.cost(this);

        double totalCost = 0;

        // for each node
        for (int i = 0; i < getNumNodes(); i++) {

            Node currentNode = getNode(i);
            double tempNodeCost = caseInfo.nodeCache.getMMLCost(currentNode);
            totalCost += tempNodeCost;
        }

        // return linkCost + totalOrderCost + totalCost;
        return structureCost + totalCost;
    }

}
