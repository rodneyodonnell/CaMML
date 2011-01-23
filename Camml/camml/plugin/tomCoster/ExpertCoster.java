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
// Various Expert Elicited TOMCoster functions.
//

// File: Tetrad4.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.tomCoster;


import camml.core.search.TOM;
import camml.core.search.TOMCoster;
import camml.core.search.TOMCoster.UniformTOMCoster;

/**
 * Class contains various expert elicited TOMCoster functions. <br>
 * These are very domain specific and are only provided as examples.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.7 $ $Date: 2006/08/22 03:13:35 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tomCoster/ExpertCoster.java,v $
 */
public class ExpertCoster {
    
    
    /** Static instance of EmptyTOMCoster */
    public static final EmptyTOMCoster emptyCoster = new EmptyTOMCoster();
    
    /**
     * TOMCoster giving all arcs an infinite cost.
     * This may be a convenient way to ensure a seatch returns an empty model.
     */
    public static class EmptyTOMCoster implements TOMCoster {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 854040978098916343L;
        /** Cost to add an arc to the network (= logP(arc) - logP(1-arc) )*/
        private final double costToAddArc = Double.POSITIVE_INFINITY;
    
        public void setArcProb(double arcProb) { 
            // ignore 
        }
        
        
        /** */
        public EmptyTOMCoster( ) { }
        
        
        
        /** Return the structre cost of the given TOM */
        public double cost( TOM tom ) { 
            if (tom.getNumEdges() == 0) {return 0.0;}
            else {return Double.POSITIVE_INFINITY;}
        }        

        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {
            return 0.0;
        }
        
        /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
         *  ie. cost(tom) - cost(tom.addArc(i,j))
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArc( TOM tom, int node1, int node2 ) {
            if ( tom.isArc(node1,node2) ) { return costToAddArc; }
            else { return -costToAddArc; }
        }
        
        
        /** Return the difference in cost between tom and tom with several arcs toggled.
         *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArcs( TOM tom, int node1[], int node2[] ) {

            int added = 0;
            for ( int i = 0; i < node1.length; i++) {
                if (tom.isArc(node1[i],node2[i])) {added++;} else {added--;}
            }
            if (added > 0) return Double.POSITIVE_INFINITY;
            else { return 0.0; }
            
        }

        /** Remove all arcs */
        public TOM repairTOM(TOM tom) { 
            int n = tom.getNumNodes();
            
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (tom.isArc(i,j)) { tom.removeArc(i,j);}
                }
            }
            return tom; 
        }

    }

    
    /** 
     * SetsCoster is a TOMCoster for the 'sets' dataset (which needs a better name...)
     * The dataset contains "months post drug start", "Viral Load increase",
     * a "Letter" (not sure what it means) and numerous binary attributes as to whether
     * a mutation has occured in a particular spot.
     * 
     *  Our prior says Months must be the first variable, Viral Load must be the last,
     *  but apart from that we use the regular CaMML prior.  We assume 'Months' is
     *  the first variable and 'Viral Load' is second.
     */
    public static class SetsCoster extends UniformTOMCoster {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 7827183714769474224L;

        public SetsCoster( double arcP ) {super(arcP);}
        
        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {
            // tom.node[0] must not be swapped to after another node 
            if ((node1 == 0) && tom.before(node1,node2)) return 10000;
            if ((node2 == 0) && tom.before(node2,node1)) return 10000;
            
            // tom.node[1] must not be swapped to before another node 
            if ((node1 == 1) && tom.before(node2,node1)) return 10000;
            if ((node2 == 1) && tom.before(node1,node2)) return 10000;
                
            // All other swaps are fine.
            return 0.0;
        }
        
        protected String orderString(TOM tom) {
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < tom.getNumNodes(); i++) {
                s.append( tom.nodeAt(i) + " ");
            }
            return s.toString();
        }
        
        /** No repairs required by default */
        public TOM repairTOM(TOM tom) { 
            int n = tom.getNumNodes();
            
            // Swap node[0] to pos[0]
            int pos = tom.getNodePos(0);
            while ( pos != 0 ) {
                int prev = tom.nodeAt(pos-1);
                //System.out.println("pos[0] = " + pos + "\t" + orderString(tom));
                if (tom.isArc(0,prev)) {tom.removeArc(0,prev);}
                tom.swapOrder(0,prev,true);
                pos = tom.getNodePos(0);
            }

            // Swap node[1] to pos[n-1]
            pos = tom.getNodePos(1);
            while ( pos != n-1 ) {
                int next = tom.nodeAt(pos+1);
                //System.out.println("pos[1] = " + pos + "\t" + orderString(tom));
                if (tom.isArc(1,next)) {tom.removeArc(1,next);}
                tom.swapOrder(1,next,true);
                pos = tom.getNodePos(1);
            }

            //System.out.println("Repaired : " + orderString(tom) + "\n");
            // ensure node[0] is first.
            //if (tom.nodeAt(0) != 0 ) { tom.swapOrder(0,tom.nodeAt(0),true); }            
            // ensure node[0] is last.
            //if (tom.nodeAt(n-1) != 1) { tom.swapOrder(tom.nodeAt(n-1),1,true); }             
            
            return tom; 
        }

        
    }

    /** ALUCoster is a TOMCoster for the ALU datasets,
     *  The ALU dataset contains 3n+1 variables, where the first n represent 'a'
     *  the second n represent 'b' and the third n represent 'c=a+b' with the final
     *  variable representing the carry flag.
     *
     *  Our prior is that {A,B} < {C,carry}
     */
    public static class ALUCoster implements TOMCoster {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -1896038961896797631L;

        /** Probability of arc existence */
        private double arcProb;
       
        /** Cost to add an arc to the network (= logP(arc) - logP(1-arc) )*/
        private double costToAddArc;

        public void setArcProb(double arcProb) {
            this.arcProb = arcProb;
            this.costToAddArc = Math.log(arcProb) - Math.log(1-arcProb);
        }


        /** */
        public ALUCoster( double arcP ) {
            setArcProb(arcP);
            this.costToAddArc = Math.log(arcProb) - Math.log(1-arcProb);
        }

        /** a = [0..n-1], b = [n..2n-1], c=[2n-1..3n-1], carry = n
         *  [a,b] are on first tier, [c,carry] on second tier */
        public boolean badTiers( TOM tom ) {
            int n = tom.getNumNodes();
            int tierBoundry = ((n-1)/3)*2;
            for (int i = tierBoundry; i < n; i++) {
                // If we see a tier1 node on tier2, then badTiers = true.
                if (tom.nodeAt(i) < tierBoundry) { return true; }
            }
            return false;
        }
        
        /** Return the structre cost of the given TOM */
        public double cost( TOM tom ) {
            // Calculate the cost to state the total ordering
            double totalOrderCost = 0;//FN.LogFactorial.logFactorial( tom.node.length );
            
            // now calculate the cost to state all the links in the model.
            double linkCost = 0;
            int numLinks = tom.getNumEdges();
            
            int numNodes = tom.getNumNodes();
            int maxNumLinks = (numNodes * (numNodes - 1)) / 2;
            
            linkCost = -1.0 * ( numLinks * Math.log( arcProb ) +
                                (maxNumLinks - numLinks) * Math.log(1-arcProb));

            if ( badTiers(tom) ) { return Double.POSITIVE_INFINITY; }
            else {return linkCost + totalOrderCost; }
            
        }

        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {
                        
            int n = tom.getNumNodes();
            int tierBoundry = ((n-1)/3)*2;
            
            // If tiers would be violated, return infinite, if not return 0. 
            if (node1 < tierBoundry && node2 > tierBoundry) { 
                return Double.POSITIVE_INFINITY;
            }
            else { return 0.0; }
        }

        /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
         *  ie. cost(tom) - cost(tom.addArc(i,j))
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArc( TOM tom, int node1, int node2 ) {
            if ( tom.isArc(node1,node2) ) { return costToAddArc; }
            else { return -costToAddArc; }
        }

        /** Return the difference in cost between tom and tom with several arcs toggled.
         *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArcs( TOM tom, int node1[], int node2[] ) {

            int added = 0;
            for ( int i = 0; i < node1.length; i++) {
                if (tom.isArc(node1[i],node2[i])) {added++;} else {added--;}
            }
            return added * costToAddArc;
        }

        /** Tom is repaired by setting the total ordering to [0,1,2,3,4,...n] */
        public TOM repairTOM(TOM tom) {
            if (!badTiers(tom)) { System.out.println("No repair required"); return tom; }

            int n = tom.getNumNodes();

            // Initialise arc matrix.
            boolean arcMatrix[][] = new boolean[n][n];
            for ( int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    if ( i != j && tom.isDirectedArc(i,j) ) { arcMatrix[i][j] = true;}
                }

            // Clear all arcs from tom
            tom.clearArcs();

            // Create out new total ordering
            int newOrder[] = new int[n];
            for (int i = 0; i < newOrder.length; i++) {newOrder[i] = i;}
                
            tom.setOrder(newOrder);

            // Add back all arcs that were present in the original ordering
            // and are still valid in the new ordering.
            for ( int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    if ( arcMatrix[i][j] && tom.before(i,j) ) { tom.addArc(i,j); }
                }

            return tom;
        }

    }

            
    /** EcoliCoster is a TOMCoster for the ecoli dataset,
     *  it uses an expert prior consisting of 'tiers' of variables,
     *  For each tier member that occurs before its parent tier, we add a cost of
     *  10000 nits. Everything else is left as normal for DefaulTOMCoster.
     */
    public static class EcoliCoster implements TOMCoster {
        //public static class UniformTOMCoster extends DefaultTOMCosterImplementation {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 2578957043598708636L;

        /** Probability of arc existence */
        private double arcProb;

        static int numEcoliCalls = 0;

        //private final double tierCost = 10000;
        private final double tierCost = Double.POSITIVE_INFINITY;

        /** Cost to add an arc to the network (= logP(arc) - logP(1-arc) )*/
        private double costToAddArc;

        public void setArcProb(double arcProb) {
            this.arcProb = arcProb;
            this.costToAddArc = Math.log(arcProb) - Math.log(1-arcProb);
        }


        /** */
        public EcoliCoster( double arcP ) {
            setArcProb(arcP);
            this.costToAddArc = Math.log(arcProb) - Math.log(1-arcProb);
        }

        /** If node is in a tier, return that tier.  If not, return -1. */
        public int getTier(int node) {
            if ( node > 12 && node < 235) { return (node - 12) % 6; }
            else return -1;
        }

        /** Return the number of variables which preceed variables on upper tiers
         *  If a node preceeds a node on a tier above it, we pay 10000 nits.
         *  If a node preceeds a node on a tier n tiers above it we pay n*10000 nits.
         *  If a node preceeds multiple nodes, the penalty is cumulative.
         */
        public int numBadTiers( TOM tom ) {
            // Number of nodes found on the current tier.
            int[] tier = new int[6];

            int bad = 0;
            //int oldBad = 0;

            //for (int i = 0; i < tom.totalOrder.length; i++) { System.out.print(tom.totalOrder[i] + " "); }
            //System.out.println();

            for (int i = 0; i < tom.getNumNodes(); i++) {
                // Current node
                int node = tom.nodeAt(i);
                if ( node > 12 && node < 235) {
                    int t = getTier(node); // Tier of the current node
                    for (int j = t+1; j < 6; j++) {
                        bad += tier[j]*(j-t);
                    }
                    tier[t]++;
                }

                //System.out.print("i = " + i + "\tnewBad = " + (bad - oldBad) +  "\ttotal = " + bad + "\ttier = ");
                //for ( int j = 0; j < tier.length; j++) { System.out.print(tier[j] + "\t"); }
                //System.out.println();
                //oldBad = bad;
            }

            numEcoliCalls++;
            // if ( numEcoliCalls % 1000 == 0) {System.out.println("Bad = " + bad);}
            return bad;

        }

        /** Return the structre cost of the given TOM */
        public double cost( TOM tom ) {
            // Calculate the cost to state the total ordering
            double totalOrderCost = 0;//FN.LogFactorial.logFactorial( tom.node.length );

            // now calculate the cost to state all the links in the model.
            double linkCost = 0;
            int numLinks = tom.getNumEdges();

            int numNodes = tom.getNumNodes();
            int maxNumLinks = (numNodes * (numNodes - 1)) / 2;

            linkCost = -1.0 * ( numLinks * Math.log( arcProb ) +
                                (maxNumLinks - numLinks) * Math.log(1-arcProb));

            // Return cost.  It is done like this so the return value is not
            // a NaN when (badTiers == 0) && (tierCost == Double.POSITIVE_INFINITY)
            int badTiers = numBadTiers(tom);
            if ( badTiers == 0 ) { return linkCost + totalOrderCost; }
            else {return linkCost + totalOrderCost + numBadTiers(tom)*tierCost; }

        }

        /** Return the difference in cost between tom and tom.swapOrder(node1,node2) <br>
         *  ie. cost(tom) - cost(tom.swapOrder(node1,node2).
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToSwapOrder( TOM tom, int node1, int node2 ) {

            int t1 = getTier(node1);
            int t2 = getTier(node2);
            // Nodes do not have a 'tier relationship'
            if ( (t1 == -1) || (t2 == -1) ) { return 0.0; }

            // If tiers are equal, no change.
            // If inequal use tierCost (note: tierCost may be infinite so (t2-t1)*tierCost doesn't work)
            if (t2-t1==0) {return 0.0;}
            else return (t2 - t1) * tierCost;

        }

        /** Return the difference in cost between tom and tom.add/removeArc(node1,node2)
         *  ie. cost(tom) - cost(tom.addArc(i,j))
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArc( TOM tom, int node1, int node2 ) {
            if ( tom.isArc(node1,node2) ) { return costToAddArc; }
            else { return -costToAddArc; }
        }

        /** Return the difference in cost between tom and tom with several arcs toggled.
         *  All Arcs (Node1[i],Node2[i]) are toggles, node1 & node2 must be of the same length.
         *  This function is provided as an optimisation to Metropolis and Anneal searches.
         */
        public double costToToggleArcs( TOM tom, int node1[], int node2[] ) {

            int added = 0;
            for ( int i = 0; i < node1.length; i++) {
                if (tom.isArc(node1[i],node2[i])) {added++;} else {added--;}
            }
            return added * costToAddArc;
        }

        /** Implement any TOM repairs required */
        public TOM repairTOM(TOM tom) {
            if (numBadTiers(tom) == 0) { System.out.println("No repair required"); return tom; }

            int n = tom.getNumNodes();

            //              // Print out pre-repair oderd.
            //System.out.println("Repairing TOM");
            //              for (int i = 0; i < n; i++) { System.out.print(tom.nodeAt(i) + " "); }
            //              System.out.println();
            //              for (int i = 0; i < n; i++) { System.out.print(getTier(tom.nodeAt(i)) + " "); }
            //              System.out.println();


            // Initialise arc matrix.
            boolean arcMatrix[][] = new boolean[n][n];
            for ( int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    if ( i != j && tom.isArc(i,j) ) { arcMatrix[i][j] = true;}
                }

            // Clear all arcs from tom
            //System.out.println("clearArcs");
            tom.clearArcs();

            //System.out.println("Examine total ordering");
            // Examine the total ordering of 'tom' to find the relative position of each member of each tier.
            // (note on magic numbers: we have 6 tiers, each with 37 variables).
            int index[] = new int[6];
            int array[] = new int[6*37];
            for ( int i = 0; i < n; i++ ) {
                int x = tom.nodeAt(i);
                int tierX = getTier(x);
                if ( tierX != -1) {
                    array[tierX*37+index[tierX]] = x;
                    index[tierX]++;
                }
            }
            //              System.out.println("array:");
            //              for (int i = 0; i < array.length; i++) { System.out.print(array[i] + " "); }
            //              System.out.println();

            //System.out.println("Create new ordering");
            // Create out new total ordering
            int newOrder[] = new int[n];
            int arrayIndex = 0;
            for (int i = 0; i < n; i++) {
                int oldNodeI = tom.nodeAt(i);
                if ( getTier(oldNodeI) == -1 ) { newOrder[i] = oldNodeI; }
                else { newOrder[i] = array[arrayIndex]; arrayIndex ++; }
            }

            //System.out.println("set new ordering");
            tom.setOrder(newOrder);

            //System.out.println("Add arcs");

            // Add back all arcs that were present in the original ordering
            // and are still valid in the new ordering.
            for ( int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    if ( arcMatrix[i][j] && tom.before(i,j) ) { tom.addArc(i,j); }
                }

            // Print out post-repaired order
            // System.out.println("Post-Repair");
            //              for (int i = 0; i < n; i++) { System.out.print(tom.nodeAt(i) + " "); }
            //              System.out.println();
            //              for (int i = 0; i < n; i++) { System.out.print(getTier(tom.nodeAt(i)) + " "); }
            //              System.out.println("\n\n");


            return tom;
        }

    }

}
