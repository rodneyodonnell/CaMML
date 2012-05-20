package camml.core.search;

import cdms.core.Value;

/**
 * Utility class for working with TOMs.
 */
public final class TomUtil {

    /**
     * Extract the parent/child connections from a CDMS parameterisation of a BNet.
     *
     * @param params CDMS parameterisation
     * @return array of parents nodes per child
     */
    public static int[][] getParentArrays(Value.Vector params) {
        int parents[][] = new int[params.length()][];
        for (int i = 0; i < params.length(); i++) {
            Value.Vector arcVec = (Value.Vector) params.cmpnt(1).elt(i);
            parents[i] = new int[arcVec.length()];
            for (int j = 0; j < arcVec.length(); j++) {
                parents[i][j] = arcVec.intAt(j);
            }
        }
        return parents;
    }
}
