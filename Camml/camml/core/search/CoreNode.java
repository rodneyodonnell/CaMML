package camml.core.search;

/**
 * Each node in the model is described by the Node class.  This class also
 * contains information for the calculation of message lengths, and the
 * parameters of the model. <br>
 * <p/>
 * There are no get/set methods for the parent and child nodes, this will be performed from
 * the TOM class. <br>
 * <p/>
 * By default, node costs and clean Nodes (@see cleanNode) are cached, but parameters are not.
 * Initial time taken to perform these operations will vary depending on the modelLearner used, but
 * should be (fairly) quick to extract from the cache once these initial calculations are done.
 *
 * @see {Node.cleanNode}
 */
class CoreNode {

    private final int childNode;

    private int[] parents;

    public CoreNode(int childNode, int[] parents) {
        this.childNode = childNode;
        this.parents = parents;
    }

    /**
     * Add a single parent to this node.
     *
     * @param node to add
     */
    public void addParent(int node) {
        parents = insertSorted(parents, node);
    }

    /**
     * remove a single parent from this node.
     *
     * @param node to add
     */
    public void removeParent(int node) {
        parents = removeSorted(parents, node);
    }

    /**
     * Print a little ascii version of a node's connections.   Looks like  : "1  : <- 2 <- 3"
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getChildNode()).append(" : ");

        for (int parent : getParents()) {
            sb.append(" <- ").append(parent);
        }

        return sb.toString();
    }

    private static int[] insertSorted(int[] array, int val) {
        final int[] newArray = new int[array.length + 1];
        int i = 0;
        while (i < array.length && array[i] < val) {
            newArray[i] = array[i];
            i++;
        }
        newArray[i] = val;
        i++;
        while (i < newArray.length) {
            newArray[i] = array[i - 1];
            i++;
        }
        return newArray;
    }

    private static int[] removeSorted(int[] array, int val) {
        final int[] newArray = new int[array.length - 1];
        int i = 0;
        while (array[i] != val) {
            newArray[i] = array[i];
            i++;
        }
        while (i < newArray.length) {
            newArray[i] = array[i + 1];
            i++;
        }
        return newArray;
    }

    /**
     * Id of this node.
     *
     * @return node id
     */
    public int getChildNode() {
        return childNode;
    }

    /**
     * List of Parents of this node.
     * The array returned by the fn should not be modified, and doing so may cause undefined results.
     *
     * @return list of parent ids
     */
    public int[] getParents() {
        return parents;
    }
}
