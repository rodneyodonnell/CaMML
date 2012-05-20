package camml.core.search;

import java.util.*;


/**
 * TOM is a Totally Ordered Model, it is a causal model where the time ordering of the nodes is
 * completely determined. This is a stronger definition than a Causal Network, as a causal
 * network implies only a partial time ordering on the nodes.
 * <p/>
 * TOM-space varies in time ordering of nodes, and connection between nodes, and the TOM class is
 * designed for easy searching of TOM-space. It can also be scored using some evaluation metric.
 * <p/>
 * NOTE: CoreTOM (and CoreNode) are meant as replacements for TOM and Node respectively, but are still a work in progress.
 * All of the logic specific to costing should be pulled out of TOM and put into callbacks in CoreTOM.
 * This should make future experimentation easier by decoupling mutation operations from costing operations.
 * For example, a search method which requires a dataCost and a networkCost should wire in listeners which calculate
 * the cost every time a mutation is performed and use this cost to direct their search.
 */
public class CoreTOM {
    /**
     * Set of callbacks called every time the specified function is called.
     */
    public interface Callback {
        /**
         * Callback called any time CoreTom.addArc() is successfully called (i.e., when it returns true)
         *
         * @param nodeX id of node with arc being added
         * @param nodeY id of node with arc being added
         * @param tom   tom containing nodes
         */
        void addArc(int nodeX, int nodeY, CoreTOM tom);

        /**
         * Callback called any time CoreTom.removeArc() is successfully called (i.e., when it returns true).
         *
         * @param nodeX id of node with arc being removed
         * @param nodeY id of node with arc being removed
         * @param tom   tom containing nodes
         */
        void removeArc(int nodeX, int nodeY, CoreTOM tom);

        /**
         * Callback called any time CoreTom.swapOrder() is called.
         * We guarantee there are no arcs between the two nodes (or any intervening nodes) when this callback is called.
         *
         * @param nodeX id of node being reordered
         * @param nodeY id of node being reordered
         * @param tom   tom containing nodes
         */
        void swapOrder(int nodeX, int nodeY, CoreTOM tom);
    }

    /**
     * Default callback implementations simply skip.
     */
    public abstract static class DefaultCallback implements Callback {

        /**
         * {@inheritDoc}
         */
        @Override
        public void addArc(int nodeX, int nodeY, CoreTOM tom) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeArc(int nodeX, int nodeY, CoreTOM tom) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void swapOrder(int nodeX, int nodeY, CoreTOM tom) {
        }
    }

    public static final int DEFAULT_MAX_NUM_PARENTS = 7;

    /**
     * Maximum number of parents a node is allowed.
     */
    private final int maxNumParents;

    /**
     * List of child -> parentList mappings.
     */
    private final CoreNode[] coreNodes;

    /**
     * Uses for calculating DAG hash.
     */
    private final DAGHash dagHash;

    /**
     * This is an index into the node array. It could be changed to an array of nodes itself,
     * but we believe this is more meaningful. <br>
     * <p/>
     * totalOrder[i] = i'th variable in total order <br>
     * <p/>
     * {@see variablePlace}
     */
    private final int[] totalOrder;

    /**
     * Stores where in the total ordering each variable is.  This is required to easily determining
     * if A is before B in the total ordering.  Otherwise a linear search of totalOrder must be
     * performed. <br>
     * <p/>
     * For the single straight line connected network (2) -> (1) -> (3) -> (0) -> (4) <br>
     * totalOrder    = { 2, 1, 3, 0, 4 } <br>
     * variablePlace = { 3, 1, 0, 2, 4 } <br>
     * <p/>
     * {@see totalOrder}
     */
    private final int[] variablePlace;

    /**
     * The edges between the nodes. The array is upper triangular, with supplied accessors
     * which don't care about the order of the nodes. The ordering of edge follows that of node.
     */
    private final BitSet[] edges;

    /**
     * Keep track of how many edges (links) are present in this TOM
     */
    private int numEdges;

    /**
     * List of callbacks to execute when the TOM structure changes.
     * <p/>
     * As callbacks frequently contain mutable state the not copied by the copy constructor.
     */
    private final List<Callback> callbacks = new ArrayList<Callback>();

    //////////////////////
    // simple accessors //
    //////////////////////

    /**
     * Return array of parents for specified node.
     * This list should not be mutated by the caller, otherwise results are undefined.
     */
    public int[] getParents(int node) {
        return coreNodes[node].getParents();
    }

    public int getNumNodes() {
        return coreNodes.length;
    }

    /**
     * return the Nth node in the total ordering.
     */
    public int nodeAt(int node) {
        return totalOrder[node];
    }

    /**
     * Return the position in the total ordering of node n
     */
    public int getNodePos(int node) {
        return variablePlace[node];
    }

    /**
     * check whether an arc exists between two nodes.
     */
    public boolean isArc(int nodeX, int nodeY) {
        if (nodeX == nodeY) {
            return false;
        }

        final int first = Math.min(nodeX, nodeY);
        final int second = Math.max(nodeX, nodeY);

        return edges[first].get(second);
    }

    /**
     * returns true if variable i is before variable j in the total ordering
     */
    public boolean before(int nodeI, int nodeJ) {
        return getNodePos(nodeI) < getNodePos(nodeJ);
    }

    /**
     * Does a directed arc exist from i to j?
     */
    public boolean isDirectedArc(int i, int j) {
        return isArc(i, j) && before(i, j);
    }

    /////////////////////////////////
    // pairwise mutation functions //
    /////////////////////////////////

    /**
     * add an arc by index.
     * Returns true if an operation was performed.
     */
    public boolean addArc(int x, int y) {
        if (isArc(x, y) || x == y) {
            return false;
        } else {
            setArc(x, y, true);
            for (Callback callback : callbacks) {
                callback.addArc(x, y, this);
            }
            return true;
        }
    }

    /**
     * remove an arc by index.
     * Returns true if an operation was performed.
     */
    public boolean removeArc(int x, int y) {
        if (!isArc(x, y)) {
            return false;
        } else {
            setArc(x, y, false);
            for (Callback callback : callbacks) {
                callback.removeArc(x, y, this);
            }
            return true;
        }
    }

    /**
     * set an arc.
     */
    protected void setArc(int nodeX, int nodeY, boolean arcValue) {
        if (nodeX == nodeY) {
            return;
        }

        final int first = Math.min(nodeX, nodeY);
        final int second = Math.max(nodeX, nodeY);

        final boolean oldEdge = edges[first].get(second);
        if (oldEdge == arcValue) {
            throw new RuntimeException("Link already present/absent??");
        }

        edges[first].set(second, arcValue);

        final int child, parent;
        if (before(nodeX, nodeY)) {
            parent = nodeX;
            child = nodeY;
        } else {
            child = nodeX;
            parent = nodeY;
        }

        if (arcValue) {
            numEdges += 1;
            if (getParents(child).length >= numEdges) {
                throw new Node.ExcessiveArcsException("MaxParents already reached, cannot add another.");
            }
            coreNodes[child].addParent(parent);
        } else {
            numEdges -= 1;
            coreNodes[child].removeParent(parent);
        }
    }

    /**
     * Swap nodeX and nodeY in the total ordering
     */
    public void swapOrder(int x, int y) {
        final int[] xConnected = new int[maxNumParents];
        final int[] yConnected = new int[maxNumParents];
        int xConnCount = 0;
        int yConnCount = 0;

        // Swap variables such that pos(x) < pos(y)
        if (getNodePos(x) > getNodePos(y)) {
            int temp = x;
            x = y;
            y = temp;
        }

        // Temporarily remove any arcs between i -> x or i -> y
        for (int i = getNodePos(x); i < getNodePos(y); i++) {
            if (isArc(nodeAt(i), x)) {
                xConnected[xConnCount++] = nodeAt(i);
                removeArc(x, nodeAt(i));
            }
            if (isArc(nodeAt(i), y)) {
                yConnected[yConnCount++] = nodeAt(i);
                removeArc(y, nodeAt(i));
            }
        }

        // Swap variables in total ordering
        int tmp;
        tmp = variablePlace[x];
        variablePlace[x] = variablePlace[y];
        variablePlace[y] = tmp;
        totalOrder[variablePlace[x]] = x;
        totalOrder[variablePlace[y]] = y;

        // Call callbacks here so no arcs will be present between the effected nodes.
        // Subsequent callbacks in the addArc() call below can take care of any updates required there.
        for (Callback callback : callbacks) {
            callback.swapOrder(x, y, this);
        }

        // Add back all arcs previously removed
        for (int i = 0; i < xConnCount; i++) {
            addArc(x, xConnected[i]);
        }
        for (int i = 0; i < yConnCount; i++) {
            addArc(y, yConnected[i]);
        }

    }

    /////////////////////////////////////
    // full network mutation functions //
    /////////////////////////////////////

    /**
     * remove all arcs from this TOM
     */
    public void clearArcs() {
        for (int i = 0; i < getNumNodes(); i++) {
            for (int j : getParents(i)) {
                removeArc(i, j);
            }
        }
    }

    /**
     * Set the current node ordering and edges based on arcs <br>
     */
    public void setStructure(int[][] parents) {
        // Remove all arcs from current TOM
        this.clearArcs();

        // Use simplistic (and possibly slow) algorithm to ensure
        // this TOM has an ordering consistent with the arc
        // structure in params.
        int changes = 1;
        while (changes != 0) {
            changes = 0;
            // for each arc
            for (int i = 0; i < parents.length; i++) {
                for (int j = 0; j < parents[i].length; j++) {

                    // if TOM ordering inconsistent with param
                    // ordering, swap ordering in TOM.
                    int nodeI = i;
                    int nodeJ = parents[i][j];

                    if (before(nodeI, nodeJ)) {
                        swapOrder(nodeI, nodeJ);
                        changes++;
                    }
                }
            }
        }

        // Add required arcs to TOM.
        for (int i = 0; i < parents.length; i++) {
            for (int j = 0; j < parents[i].length; j++) {
                this.addArc(i, parents[i][j]);
            }
        }

    }

    /**
     * Set the current node ordering and edges to those of tom2
     */
    public void setStructure(TOM tom2) {
        clearArcs();

        // set the ordering to be the same as tom2
        for (int i = 0; i < getNumNodes(); i++) {
            this.swapOrder(nodeAt(i), tom2.nodeAt(i));
        }

        // set arcs to be the same as
        for (int i = 0; i < getNumNodes(); i++) {
            for (int j : tom2.getNode(i).getParent()) {
                this.addArc(i, j);
            }
        }
    }

    /**
     * Set the total ordering of the tom to order.
     */
    public void setOrder(int[] order) {
        if (order.length != getNumNodes()) {
            throw new IllegalArgumentException("Invalid Ordering specified");
        }

        for (int i = 0; i < getNumNodes(); i++) {
            this.swapOrder(nodeAt(i), order[i]);
        }

        // If order[] is not a valid ordering (eg. duplicate values)
        // the total ordering may not match.
        if (!Arrays.equals(this.totalOrder, order)) {
            throw new RuntimeException("totalOrder != specified order");
        }
    }

    /**
     * randomise the total order, all arcs are retained, though their order may change.
     */
    public void randomOrder(Random rand, boolean regressionOrder) {

        if (regressionOrder) {
            // Start with nodes in fixed order (for regression).
            for (int i = 0; i < getNumNodes(); i++) {
                swapOrder(nodeAt(i), i);
            }
        }

        // Randomly permute total order
        for (int i = getNumNodes() - 1; i > 0; i--) {
            int j = (int) (rand.nextDouble() * (i + 1));
            swapOrder(nodeAt(i), nodeAt(j));
        }
    }

    /**
     * Set all arcs randomly while maintaining initial total ordering.
     *
     * @param rand random number generator
     * @param p    probability of any given arc being set.
     */
    public void randomArcs(java.util.Random rand, double p) {
        for (int i = 0; i < getNumNodes() - 1; i++) {
            for (int j = i + 1; j < getNumNodes(); j++) {
                boolean state = (rand.nextDouble() < p);
                if ((state && !isArc(i, j)) || (!state && isArc(i, j))) {
                    setArc(i, j, state);
                }
            }
        }
    }

    /**
     * Randomize the total ordering then fix it so it is consistent with arcs present.
     */
    public void buildOrder(Random rand, boolean regressionOrder) {
        CoreTOM tempTOM = new CoreTOM(this);

        BitSet[] ancestors = getAncestorBits();
        final int n = getNumNodes();

        clearArcs();
        randomOrder(rand, regressionOrder);

        // do a topological sort to get nodes back to correct order.
        int numChanges = 1;
        while (numChanges != 0) {
            numChanges = 0;
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    int varI = nodeAt(i);
                    int varJ = nodeAt(j);

                    // if VarJ is before VarI, swap them.
                    if (ancestors[varI].get(varJ)) {
                        numChanges++;
                        swapOrder(varI, varJ);
                    }
                }
            }
        }

        // Add all original arcs to TOM
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (tempTOM.isArc(i, j)) {
                    addArc(i, j);
                }
            }
        }
    }


    ////////////////////////////////
    // complex relation accessors //
    ////////////////////////////////

    /**
     * is a an ancestor of x? (I think its O(numNodes^2), might be O(numNodes^3)
     */
    public boolean isAncestor(int ancestorNode, int descendantNode) {
        // quick check if a is not before descendantNode.
        // Also catches case when ancestorNode == descendantNode.
        if (!before(ancestorNode, descendantNode)) {
            return false;
        }

        boolean[] checked = new boolean[getNumNodes()];
        return isAncestor(ancestorNode, descendantNode, checked);
    }

    /**
     * is d a descendant of x?
     */
    public boolean isDescendant(int d, int x) {
        return isAncestor(x, d);
    }

    /**
     * Return true if node1 and node2 have are correlated, that is
     * NOT d-separated. Correlation occurs when one node is an descendant
     * of the other, or they have a common ancestor.
     */
    public boolean isCorrelated(int node1, int node2) {
        boolean a1[] = flagAncestors(node1, new boolean[getNumNodes()]);
        boolean a2[] = flagAncestors(node2, new boolean[getNumNodes()]);
        a1[node1] = true;
        a2[node2] = true;
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] && a2[i]) return true;
        }
        return false;
    }

    /**
     * Calculate an array of BitSets where (x[i].get(j) = true) implies i <= j exists.
     */
    private BitSet[] getAncestorBits() {
        return getAncestorBits(-1, new BitSet[getNumNodes()]);
    }

    /**
     * Get bits for given index, if index == -1 get all bits.
     */
    private BitSet[] getAncestorBits(int index, BitSet[] bits) {
        // if index == -1, calculate all ancestors.
        if (index == -1) {
            for (int i = 0; i < bits.length; i++) {
                if (bits[i] == null) {
                    getAncestorBits(i, bits);
                }
            }
        }
        // if ancestors already calculated, nothing to do. Return.
        else if (bits[index] != null) {
            return bits;
        }
        // Calculate ancestors of ancestors.  And them together to get this node's ancestors.
        else {
            int[] parents = getParents(index);
            BitSet ancestors = new BitSet(bits.length);

            for (int parent : parents) {
                // get Parent's ancestors.
                BitSet parAnc = bits[parent];
                if (parAnc == null) {
                    parAnc = getAncestorBits(parent, bits)[parent];
                }
                ancestors.or(parAnc);
                ancestors.set(parent);
            }
            bits[index] = ancestors;
        }

        return bits;
    }

    /**
     * returns true if 'a' is an ancestor of 'd'
     */
    private boolean isAncestor(int a, int d, boolean[] checked) {
        int[] parent = getParents(d);
        for (int i = 0; i < parent.length; i++) {
            if (parent[i] == a) {
                return true;
            }
            if (!checked[parent[i]]) {
                checked[parent[i]] = true;
                if (isAncestor(a, parent[i], checked)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * set flagged[i] = true if node[i] is an ancestor of node[n]
     */
    private boolean[] flagAncestors(int n, boolean[] flagged) {
        int[] parents = getParents(n);
        for (int i = 0; i < parents.length; i++) {
            if (!flagged[parents[i]]) {
                flagAncestors(parents[i], flagged);
            }
        }
        flagged[n] = true;
        return flagged;
    }

    ///////////////////
    // Constructors. //
    ///////////////////

    public CoreTOM(int numNodes, int maxNumParents) {
        this(numNodes, maxNumParents, new DAGHash(new Random(123), numNodes));
    }

    public CoreTOM(int numNodes, int maxNumParents, DAGHash dagHash) {

        this.maxNumParents = maxNumParents;
        this.dagHash = dagHash;

        coreNodes = new CoreNode[numNodes];
        totalOrder = new int[numNodes];
        variablePlace = new int[numNodes];
        edges = new BitSet[numNodes];
        numEdges = 0;

        // iterate over the structure.
        for (int i = 0; i < numNodes; i++) {
            totalOrder[i] = i;
            variablePlace[i] = i;

            coreNodes[i] = new CoreNode(i, new int[0]);
            edges[i] = new BitSet(i);
        }
    }

    public CoreTOM(CoreTOM coreTOM) {

        int len = coreTOM.getNumNodes();
        this.totalOrder = coreTOM.totalOrder.clone();
        this.variablePlace = coreTOM.variablePlace.clone();
        this.numEdges = coreTOM.numEdges;
        this.maxNumParents = coreTOM.maxNumParents;
        this.dagHash = coreTOM.dagHash;

        this.coreNodes = new CoreNode[len];
        this.edges = new BitSet[len];

        for (int i = 0; i < len; i++) {
            coreNodes[i] = new CoreNode(i, coreTOM.getParents(i));
            edges[i] = (BitSet) coreTOM.edges[i].clone();
        }
    }

    //////////////
    // equality //
    //////////////

    /**
     * Two DAGs are considered equal if they have identical arcs, and those arcs are in the same
     * direction.  Defining DAG equality here may be more useful than TOM equality.
     */
    public boolean equals(Object o) {
        return o instanceof CoreTOM && dagEquals((CoreTOM) o);
    }

    public boolean dagEquals(CoreTOM tom) {
        if (getNumNodes() != tom.getNumNodes()) {
            return false;
        }

        for (int i = 0; i < getNumNodes(); i++) {
            if (!Arrays.equals(getParents(i), tom.getParents(i))) {
                return false;
            }
        }

        return true;
    }

    public boolean tomEquals(CoreTOM tom) {
        return dagEquals(tom) && Arrays.equals(totalOrder, tom.totalOrder);
    }

    /**
     * Return tomHash(tom=this, ml=0, clean=false) as an positive integer
     */
    public int hashCode() {
        return (int) dagHash.hash(this, 0);
    }


    ///////////////
    // Accessors //
    ///////////////

    public int getMaxNumParents() {
        return maxNumParents;
    }

    public CoreNode getCoreNode(int node) {
        return coreNodes[node];
    }

    public int[] cloneTotalOrder() {
        return totalOrder.clone();
    }

    public int getNumEdges() {
        return numEdges;
    }
}
