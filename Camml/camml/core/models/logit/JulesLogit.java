/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Julian Neil, Rodney O'Donnell
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
// Logit model class
//

// File: JulesLogit.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.logit;

import java.io.PrintStream;
import java.util.Arrays;

import camml.core.models.ModelLearner;
import camml.core.models.ModelLearner.LearnerException;
import cdms.core.*;

/**
 * Logistic Regression model, code largely borrowed from Julian Neil's C
 * implementation.
 * 
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.14 $ $Date: 2006/08/22 03:13:29 $ 
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/models/logit/JulesLogit.java,v $
 */
public class JulesLogit {

    /** half * log(pi)*/
    final static double hlg2pi = 0.5 * Math.log(2.0 * Math.PI);

    /** log(PI) */
    final static double lgpi = Math.log(Math.PI);

    /* current parameters c_k and d_{kix_i} */
    /** indexed by [xVal] */
    public double c[]; 

    /** parameter d indexed by [xVal][parent][parenVal] */
    public double d[][][];

    /** Maximum arity in dataset. */
    int maxArity = 0;
    
    /** Maxumum number of parameters */
    int maxParams = 0;
    
    int numParents = 0;
    
    private static boolean noOptimizationWarningPrinted = false;
    
    /** Setup everything as required by the given dataset.*/
    private void setup(Value.Vector xz) throws ModelLearner.LearnerException {

        // Calculate variable arities.
        Type.Structured xzType = (Type.Structured) ((Type.Vector) xz.t).elt;
        arity = new int[xzType.cmpnts.length];
        for (int i = 0; i < xzType.cmpnts.length; i++) {
            arity[i] = (int) ((Type.Discrete) xzType.cmpnts[i]).UPB
                - (int) ((Type.Discrete) xzType.cmpnts[i]).LWB + 1;
            if (arity[i] > maxArity) {maxArity = arity[i];}
        }
        
        numParents = arity.length-1;
        
        // Final node is the target node.
        // Total params = sum(parentArities-1) * (targetArity-1) + targetAtiry(-1)
        maxParams = 0;
        for ( int i = 0; i < numParents; i++) {
            maxParams += arity[i] - 1;
        }        
        maxParams = (maxParams+1) * (arity[arity.length-1]-1);
        
        
        // number of variables.
        int numVars = arity.length;
        int numCases = xz.length();
        
        //        If we flatten all variable values into a single list, then
        // rN[i] contains the index of the first value of variable i in
        // that list.  rN[nv] contains the total number of variable values 
        rN = new int[numVars + 1];
        rN[0] = 0;
        for (int i = 1; i <= numVars; i++) {
            rN[i] = rN[i - 1] + arity[i - 1];
        }

        //        Make storage for counts of cases with Y=y, Xi=xi 
        vN = new int[rN[numVars]][rN[numVars]];

        //        Now count cases for every pair of variable values
        for (int i = 0; i < rN[numVars]; i++) {
            for (int j = 0; j < rN[numVars]; j++) {
                vN[i][j] = 0;
            }
        }

        // Shortcut method to extract data from xz as an int[][]
        // NOTE: This is mildly dangerous and we must ensure that xzArray[i][j] is
        //       not modified!! (if it is, our original datasource is polluted.)
        int xzArray[][] = new int[arity.length][];
        for ( int i = 0; i < xzArray.length; i++) {
            Value.Vector tempVec = xz.cmpnt(i);
            if ( tempVec instanceof VectorFN.FastDiscreteVector ) { 
                xzArray[i] = ((VectorFN.FastDiscreteVector)tempVec).getData(false);
            }
            else {
                if ( !noOptimizationWarningPrinted ) {
                    System.out.println("WARNING: No optimisation possible in JulesLogit : " + tempVec.getClass());
                    noOptimizationWarningPrinted = true;
                }
                xzArray[i] = new int[ tempVec.length() ];
                for ( int j = 0; j < xzArray[i].length; j++ ) {
                    xzArray[i][j] = tempVec.intAt(j);
                }
            }
            
        }
        
        
        // This method took 47.85 sec on test data.
        //        for (int n = 0; n < numCases; n++) {
        //            for (int i = 0; i < numVars; i++) {
        //                for (int j = 0; j < numVars; j++) {
        //                    vN[rN[i] + xzArray[i][n]][rN[j] + xzArray[j][n]]++;
        //                }
        //            }
        //        }

        // This method took 42.64 sec on test data.
        //        for (int n = 0; n < numCases; n++) {
        //            for (int i = 0; i < numVars; i++) {
        //                int[] tmp = vN[rN[i] + xzArray[i][n]];
        //                for (int j = 0; j < numVars; j++) {
        //                    tmp[rN[j] + xzArray[j][n]]++;
        //                }
        //            }
        //        }


        // This method took 40.20 seconds.
        // Rearange loop so n is in the middle & move indirections out of inner loop        
        //        for (int i = 0; i < numVars; i++) {
        //            int rN_i = rN[i];
        //            int[] xz_i = xzArray[i];
        //            for (int j = 0; j < numVars; j++) {                
        //                int rN_j = rN[j];
        //                int[] xz_j = xzArray[j];
        //                for (int n = 0; n < numCases; n++) {
        //                    vN[rN_i + xz_i[n]][rN_j + xz_j[n]]++;
        //                }
        //            }
        //        }

        // This gets a lot slower again ... odd, 49.23 sec
        // Rearange loop so n is in the middle & move indirections out of inner loop
        // It is quicker to compare against 0 than any other int, so we count backwards.
        //        for (int i = numVars; --i>=0; ) {
        //            int rN_i = rN[i];
        //            int[] xz_i = xzArray[i];
        //            for (int j = numVars; --j >= 0; ) {                
        //                int rN_j = rN[j];
        //                int[] xz_j = xzArray[j];
        //                for (int n = numCases; --n >= 0; ) {
        //                    vN[rN_i + xz_i[n]][rN_j + xz_j[n]]++;
        //                }
        //            }
        //        }



        // 38.54 seconds.
        // Rearange loop so n is in the middle & move indirections out of inner loop
        // make vN a local variable so it's on the stack.
        // Made variables final brings time to 38.06 secs.
        //        final int vN[][] = this.vN; 
        //        for (int i = 0; i < numVars; i++) {
        //            final int rN_i = rN[i];
        //            final int[] xz_i = xzArray[i];
        //            for (int j = 0; j < numVars; j++) {                
        //                final int rN_j = rN[j];
        //                final int[] xz_j = xzArray[j];
        //                for (int n = 0; n < numCases; n++) {
        //                    vN[rN_i + xz_i[n]][rN_j + xz_j[n]]++;
        //                }
        //            }
        //        }


        ////////////////////////////////////////////////////////
        // rodo: Below is nasty looking optimized code.
        //       It is equivalent to:
        //
        //    for (int n = 0; n < numCases; n++) {
        //        for (int i = 0; i < numVars; i++) {
        //            for (int j = 0; j < numVars; j++) {
        //                vN[rN[i] + xzArray[i][n]][rN[j] + xzArray[j][n]]++;
        //            }
        //        }
        //    }
        //
        // Optimized for java 1.5
        // Things that seem to work:
        // - Inner loop runs over 'n' as this is the longest array.
        // - Declaring local variable for vN instead of using class variable
        // - Pushing rN_i, xz_i, rN_j & xz_j out of the innermost loop
        // - Declaring variables final
        // - Only looping until j from i..len as matrix is symetric.
        // 
        // Things that didn't work:
        // - Looping backwards for (i = numVars; i>=0; i--)
        //
        // Changing this code took the total run time from 213s
        // to 142s for my test cast (CaMML, logit model, kr-vs-kp.arff)
        ////////////////////////////////////////////////////////
        final int vN[][] = this.vN; 
        for (int i = 0; i < numVars; i++) {
            final int rN_i = rN[i];
            final int[] xz_i = xzArray[i];
            for (int j = i; j < numVars; j++) {      // only fill in half the matrix as it is symetric
                final int rN_j = rN[j];
                final int[] xz_j = xzArray[j];
                for (int n = 0; n < numCases; n++) {
                    vN[rN_i + xz_i[n]][rN_j + xz_j[n]]++;                    
                }
            }            
        }
        // Copy top half of matrix to bottom.
        for (int i = 0; i < vN.length; i++) {
            for (int j = 0; j < i; j++) {
                vN[i][j] = vN[j][i];
            }
        }
        ////////////////////////////////////////////////////////
        // End of nasty illegible code (hopefully)
        ////////////////////////////////////////////////////////
        
        

        if (maxArity > 20) {
            throw new ModelLearner.LearnerException("Arity too high for Logit Learner.");
        }
        
        Ndot = new int[Maxcell];
        Nyl = new int[Maxcell];
        
        kN = new int[maxArity+1];
        //pN = new int[maxArity+1];
        pN = new int[numParents+1];

        Sy = new double[maxArity];
        S = new double[maxArity*maxParams*maxArity];
        
        
        
        id = new double[maxParams];
        S2yz = new double[maxArity][maxArity];

        F = new double[maxParams][maxParams];
    }

    /** reciprocal of diagonal elements */
    double id[];// = new double[Maxparm];
    double Sy[];// = new double[Maxs];
    double S[];// = new double[Maxs * Maxp * Maxs];

    // TODO: Allocate more efficiently.
    double F[][]; // = new double[Maxparm][Maxparm]; /* to store Fisher */

    
    // Global vars used by camml.
    //final int &Maxp = 7;

    //final int &Maxs = 100;//5; // TODO: Defalut should be ~ 20

    /** Max number of params     */
    //final int &Maxparm = ((Maxs - 1) * (1 + Maxp * (Maxs - 1)));

    final int Maxcell = 65000;


    final int Pcap = 15;

    int numconvfails;

    double amlcost;

    int[] Ndot;

    int npstates;

    int[] Nyl;

    final int[] pstate = new int[Maxcell];

    double darccost; /* -log (parc / (1-parc)) */

    public double emlcost;

    double emmlcost; /* mml and ml with first-order model */

    final static double sig = 3.0; // default value.

    final static double invsigsq = 1.0 / (sig * sig);

    final static double lgsig = Math.log(sig);

    double mmlcost;

    int numbadcoms;

    /** Number of states of each var */
    int[] arity; 

    /** For variable Y, to index a value */
    int kN[];// = new int[Maxs + 1]; 

    int pN[]; //= new int[Maxs + 1]; 
    /*
     * by a value of Y=k and a value of the
     * ith parent of Y Xi=xi, we'll use the
     * following index: kN[k] + pN[i] + xi
     */

    int LUn, detSign;

    //int[] coms;

    //int[] tcoms;

    final int[][] estate = new int[Maxcell][];

    int vN[][];

    /** rN[i]+xi = indx of Xi=xi in vN[][] */
    int rN[];


    final static PrintStream stderr = System.err;

    boolean mapest = true; /* flag calculation of MAP parameter estimates for
                              first order model */


    public final static double log(double d) {
        return Math.log(d);
    }

    public final static void fprintf(PrintStream s, String str) {
        s.println(str);
    }

    public final static void fprintf(PrintStream s, String str, String str2) {
        s.println(str + str2);
    }

    public final static void fprintf(PrintStream s, String x, int x2) {
        s.println(x + x2);
    }

    public final static void fprintf(PrintStream s, String x, double x2) {
        s.println(x + x2);
    }

    public final static void exit(int x) {
        System.out.println("\n\nEXIT(" + x + ")\n\n");
    }

    public static class Node {
        public Node() {
            ndad = 0;
            nd = 0;
            dads = new int[0];
        }

        public int ndad;

        public int nd;

        public int[] dads;

        public String toString() {
            String s = nd + " <- ";
            for (int i = 0; i < dads.length; i++) {
                s += dads[i] + " ";
            }
            return s;
        }

    }

    /**
     * Function to calculate message cost of a node. Assumes var has parents as
     * shewn in dads. Cost includes prior cost of coefficients, Fisher, but not
     * linear-extensions count.
     * 
     * To find message lengths maximises the likelihood * parameter prior.
     * 
     * It also uses and maintains a cache of node info in vcache. The above
     * describes action if code < 0. If code >=0, it looks at the parents stored
     * for the node and removes those which are less than significant, as shewn
     * by mml. It then computes max liklihood estimates for the remainder and
     * computes node signatures based on them and fixes the global skig1 etc.
     * 
     * Note action is the same for codes 0 and 1.
     */
    //public double nodeCost(Node node, Value.Vector xz) throws LearnerException {
    //    return nodeCost(LogitFN.getX(node,xz),LogitFN.getZ(node,xz));
    //}
        
    public double nodeCost(Value.Vector x, Value.Vector z) throws LearnerException {
        Value.Vector xz = LogitFN.combineXZ(x,z);                
        setup(xz);
        
        Type.Structured zType = (Type.Structured)((Type.Vector)z.t).elt;
        Node node = new Node();
        node.ndad = zType.cmpnts.length;
        node.nd = zType.cmpnts.length;   // This works as xz.cmpnt(node.nd) == x
        node.dads = new int[node.ndad];
        for (int i = 0; i < node.dads.length; i++) {node.dads[i] = i;}
        
        
        
        int i, k, xi;

        int y, ry;
        int ndad;
        int dad[];
        int nfreeparms;

        double sumpsq, sumlogri;
        double ldetF[] = new double[1]; /* log of determinant of Fisher */

        amlcost = -1.0; /* -loglikelihood for printing report */

        y = node.nd; // Node #
        ry = arity[y]; // arity of y        
        
        dad = node.dads; // Parents of y
        ndad = node.ndad; // dads.length

        /* Need to calculate message length of current node. */
        if (!datacounts(node, xz)) {
            numbadcoms++;
            throw new LearnerException("call to datacounts() failed");
        }

        /* Initialize arrays used to index counts */
        pN[0] = 0;
        sumlogri = 0.0;
        for (i = 0; i < ndad; i++) {
            pN[i + 1] = pN[i] + arity[dad[i]];
            sumlogri += log(arity[dad[i]]);
            //dPrint("pN["+(i+1)+"] = " + pN[i+1]);
        }
        kN[0] = 0;
        for (k = 0; k < arity[y]; k++) {
            kN[k + 1] = kN[k] + pN[ndad];
            //dPrint("kN["+(k+i)+"] = " + kN[k+1]);
        }

        if (!estimateparams(node)) {
            fprintf(stderr,
                    "nodecost() error: Unable to estimate parameters.\n");
            fprintf(stderr, "Node %3d <--", y + 1);
            for (i = 0; i < ndad; i++) {
                fprintf(stderr, " %3d", dad[i] + 1);
            }
            fprintf(stderr, "\n");
            exit(1);
        }

        calcsums(node, 2);
        nfreeparms = calcfisher(node);

        if (!cholesky(nfreeparms, F)) {
            fprintf(stderr,
                    "\nnodecost() error: Fisher singular or negative.\n");
            fprintf(stderr, "Node %3d <--", y + 1);
            for (i = 0; i < ndad; i++) {
                fprintf(stderr, " %3d", dad[i] + 1);
            }
            fprintf(stderr, "\nFisher\n");
            display(nfreeparms, F);
            exit(1);
        }

        if (!chlogdet(ldetF)) { // & removed so it looks like "java syntax"
            fprintf(stderr, "nodecost() error: Negative Fisher determinant.\n");
            fprintf(stderr, "Node %3d <--", y + 1);
            for (i = 0; i < ndad; i++) {
                fprintf(stderr, " %3d", dad[i] + 1);
            }
            fprintf(stderr, "\n");
            exit(1);
        }

        /*
         * If M is numParents, K is numVariables, Pf is number of free
         * parameters Pt is total number of parameters. ry is arity of y. ri is
         * arity of Xi
         * 
         * param prior = 0.5*Pf*log(2*pi*sigsq) - 0.5*log(ry)*( 1 + sum_i( ri -
         * 1 ) ) - 0.5*( ry-1 ) * sum_i( log ri ) + 0.5*sumpsq*invsigsq from
         * Fisher = 0.5*log(det(F)) from LL = -LL from kappa ~=
         * -0.5*Pf*log(2*pi) + 0.5*log(Pf*pi)
         */

        /* calculate sum (param^2) */

        sumpsq = 0.0;
        for (k = 0; k < ry; k++) {
            sumpsq += c[k] * c[k];
            //System.out.println("c["+k+"] = " + c[k]);
            for (i = 0; i < ndad; i++) {
                for (xi = 0; xi < arity[dad[i]]; xi++) {
                    sumpsq += d[k][i][xi] * d[k][i][xi];
                    //System.out.println("d["+k+"]["+i+"]["+xi+"] = " + d[k][i][xi]);
                }
            }
        }

        if ( nfreeparms == 0) { mmlcost = 0;}
        else {
            mmlcost = nfreeparms * (hlg2pi + lgsig)
                + 0.5 * (-log(ry) * (1 + pN[ndad] - ndad) - (ry - 1) * sumlogri + sumpsq * invsigsq + ldetF[0]) 
                + emlcost 
                - nfreeparms * hlg2pi + 0.5 * (log(nfreeparms) + lgpi);
        }

        return mmlcost;

        /*
         * printf("MML = %f + %f + %f + %f + %f\n", ndad*darccost, nfreeparms*(
         * hlg2pi + lgsig ) + 0.5*( - log(ry)*( 1 + pN[ndad] - ndad ) -
         * (ry-1)*sumlogri + sumpsq*invsigsq), 0.5*ldetF, emlcost,
         * -nfreeparms*hlg2pi + 0.5*(log(nfreeparms) + lgpi));
         */

        /* mmlcost does not yet include contribution from parc (darccost) */
        // We don't want it to in CaMML2.
        //mmlcost += ndad * darccost;
    }

    public double getMMLCost() {
        return mmlcost;
    }

    public boolean estimateparams(Node node) throws LearnerException {
        int i, k, xi, ri, ry;
        //int i, j;
        //int k, l, xi, xj;
        //int ri, rj, ry;
        //int ps; /* parent state index */

        int Fi; /* Indicex for Fisher */

        double t;
        double NLLold; /* NLL for last iteration */
        // double dp; /* change in params over iteration */
        double delLL, olddelLL; /* change in likelihood */
        // double delta; /* sum of abs( d(LL)/d(params) ) */
        double scale;

        int iters;
        boolean converged; /* counter to indicate convergence */
        boolean conservative; /* use conservative scale adjustment */
        int maxiters; /* maximum number of iterations */

        //double dLL[] = new double[Maxparm]; // vector of LL first derivatives
        double dLL[] = new double[maxParams];
        //double h[] = new double[Maxparm]; // vector of changes to params
        double h[] = new double[maxParams];

        int ndad, dad[];
        int y, yk, yry;
        int Nyk[], Nyry[];
        int p, pxi, pri;

        conservative = false;
        maxiters = 500;
        
        eststart: do {
            
            y = node.nd;
            ndad = node.ndad;
            dad = node.dads;
            
            /* Initialize parameters */        
            c = new double[arity[y]];
            d = new double[arity[y]][ndad][];
            
            for (k = 0; k < arity[y]; k++) {
                c[k] = 0.0;
                for (i = 0; i < ndad; i++) {
                    ri = arity[dad[i]];
                    d[k][i] = new double[ri];
                    for (xi = 0; xi < ri; xi++) {
                        d[k][i][xi] = 0.0;
                    }
                }
            }
            
            /*
             * Now use the gereralized Newton method to adjust the parameters until
             * they settle on a value that gives zeroes partial first derivatives
             * for the likelihood.
             */
            
            ry = arity[y] - 1;
            iters = 0;
            delLL = -1.0;
            olddelLL = -1.0;
            converged = false;
            // delta = 1e+99;
            emlcost = 0.0;
            if (!conservative) {
                scale = 0.5;
            } else {
                scale = 0.2;
            }
            
            do {
                iters++;
                
                NLLold = emlcost;
                calcsums(node, 2);
                calcfisher(node);
                olddelLL = delLL;
                delLL = emlcost - NLLold;
                // System.out.println("delLL = " + delLL);
                if (iters > 2) {
                    if (!conservative) {
                        if (iters == 3) {
                            scale = 1.0;
                            olddelLL = -olddelLL;
                        } else if (delLL > 0.0) {
                            // System.out.println("scale = " + scale);
                            scale *= 0.5;
                        } else if (scale < 0.999) {
                            // scale *= 1.1892071; /* doubles after 4 iterations */
                            scale *= 1.0905077; /*
                                                 * doubles after 6 iterations
                                                 */
                        }
                    } else if (delLL > 0.0) {
                        scale *= 0.5;
                    }
                    
                }
                
                /* Find the first derivatives */
                
                Fi = ry;
                // delta = 0.0;
                yry = rN[y] + ry;
                Nyry = vN[yry];
                // System.out.println("mapest = " + mapest);
                for (k = 0; k < ry; k++) {
                    yk = rN[y] + k;
                    Nyk = vN[yk];
                    dLL[k] = Nyk[yk] - Nyry[yry] - Sy[k];
                    if (mapest) {
                        dLL[k] -= (c[k] - c[ry]) * invsigsq;
                    }
                    // delta += fabs(dLL[k]);
                    for (i = 0; i < ndad; i++) {
                        p = dad[i];
                        ri = arity[p] - 1;
                        pri = rN[p] + ri;
                        for (xi = 0; xi < ri; xi++, Fi++) {
                            pxi = rN[p] + xi;
                            dLL[Fi] = Nyk[pxi] - Nyry[pxi] - S[kN[k] + pN[i] + xi]
                                - Nyk[pri] + Nyry[pri] + S[kN[k] + pN[i] + ri];
                            if (mapest) {
                                dLL[Fi] -= (d[k][i][xi] + d[ry][i][ri]
                                            - d[ry][i][xi] - d[k][i][ri])
                                    * invsigsq;
                            }
                            // delta += fabs(dLL[Fi]);
                        }
                    }
                }
                
                /*
                 * now solve the equation F * h = dLL to get the parameter
                 * increments
                 */
                
                if (!cholesky(Fi, F)) {
                    fprintf(stderr,
                            "estimateparams() error:  Fisher nearly singular.\n");
                    calcfisher(node);
                    fprintf(stderr, "Fisher: \n");
                    display(Fi, F);
                    return false;
                }
                chsolve(Fi, h, dLL);
                
                for (i = 0; i < ndad; i++) {
                    ri = arity[dad[i]] - 1;
                    d[ry][i][ri] = 0.0;
                    for (xi = 0; xi < ri; xi++) {
                        d[ry][i][xi] = 0.0;
                    }
                }
                
                /* add increments to parameters */
                
                Fi = ry;
                c[ry] = 0;
                // dp = 0.0;
                for (k = 0; k < ry; k++) {
                    t = (scale >= 1.0) ? h[k] : scale * h[k];
                    c[k] += t;
                    if (c[k] > Pcap) {
                        c[k] = Pcap;
                    } else if (c[k] < -Pcap) {
                        c[k] = -Pcap;
                    }
                    /*
                     * printf("c[%2d] = %8.4f dLL = %8.4f h = %8.4f F = %8.4f\n" ,
                     * k, c[k], dLL[k], h[k], F[k][k]); dp += fabs(t);
                     */
                    c[ry] -= c[k];
                    for (i = 0; i < ndad; i++) {
                        ri = arity[dad[i]] - 1;
                        d[k][i][ri] = 0.0;
                        for (xi = 0; xi < ri; xi++, Fi++) {
                            t = (scale >= 1.0) ? h[Fi] : scale * h[Fi];
                            d[k][i][xi] += t;
                            if (d[k][i][xi] > Pcap) {
                                d[k][i][xi] = Pcap;
                            } else if (d[k][i][xi] < -Pcap) {
                                d[k][i][xi] = -Pcap;
                            }
                            /*
                             * printf("d[%2d][%2d][%2d] = %8.4f dLL = %8.4f h =
                             * %8.4f F = %8.4f\n", k, i, xi, d[k][i][xi], dLL[Fi],
                             * h[Fi], F[Fi][Fi]); dp += fabs(t);
                             */
                            d[k][i][ri] -= d[k][i][xi];
                            d[ry][i][xi] -= d[k][i][xi];
                        }
                        d[ry][i][ri] -= d[k][i][ri];
                    }
                }
                
                if (Double.isNaN(emlcost)) {
                    break;
                }
                if (olddelLL < 0.00001 && delLL > -0.01 && delLL < 0.00001) {
                    converged = true;
                }
                
                /*
                 * printf("%d: -LL %9.4f dll %8.4f sc %8.4f\n", iters, emlcost,
                 * delLL, scale);
                 */
                
            } while (!converged && iters < maxiters);
            
            /*
             * printf("iters %3d params %3d\n\n", iters, Fi); for (k=0; k<=ry; k++) {
             * printf("c[%2d] = %8.4f\n" , k, c[k]); for (i=0; i<ndad; i++) { ri =
             * states[dad[i]]-1; for (xi=0; xi<=ri; xi++) {
             * printf("d[%2d][%2d][%2d] = %8.4f\n", k, i, xi, d[k][i][xi]); } } }
             */
            
            if (!converged) {
                if (!conservative) {
                    numconvfails++;
                    conservative = true;
                    maxiters = 150;
                    continue eststart;
                    //throw new LearnerException("goto statement removed...");                
                }
                /*
                  fprintf(stderr, "\nestimateparams(): Total Convergence failure.\n");
                  fprintf(stderr, "Node %d  <--", y + 1);
                  for (i = 0; i < ndad; i++) {
                  fprintf(stderr, " %3d", dad[i] + 1);
                  }
                  fprintf(stderr, "\n");
                */
                //return false;
                throw new LearnerException("Total convergence failure.");
            } else {
                return true;
            }
        } while(true);
    }

    void display(int n, /*const*/double A[][]) {
        int i, j;

        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                fprintf(System.out, "%7.2f  ", A[i][j]);
            }
            fprintf(System.out, "\n");
        }
        fprintf(System.out, "\n");
    }

    /*        ----------------  FUN int datacounts (node, data) ----------*/

    /*        Finds several counts of the passed data for the given node.     If 
     *        the node is called Y, we need the following counts:
     *                Cases with Y=y
     *                Cases with Y=y, Parent Xi = xi
     *                Cases with Parents in state ps
     *                Cases with Y=y, Parents in state ps
     *
     *        Parent states are counted with dad[0] as the most significant
     *        and dad[n] as the least significant, meaning that every time the
     *        parent state is incremented, the value of dad[n] changes by 1.
     *        Leaves the number of parent combos in coms.
     *
     *        returns 0 if the number of parent states > Maxcell
     *        returns 1 otherwise
     */

    boolean datacounts(Node node, Value.Vector data) {
        int i;
        int d;
        int ps, kps;
        int n, ndad, dad[];
        int y;
        
        y = node.nd;
        ndad = node.ndad;
        dad = node.dads;

        /* Zero counts */
        int coms = 1;
        for (i = 0; i < ndad; i++) {
            coms *= arity[dad[i]];
        }

        int tcoms = arity[y] * coms;
        if (tcoms > Maxcell) {
            return false;
        }

        Arrays.fill(Ndot, 0, coms, 0);
        Arrays.fill(Nyl, 0, tcoms, 0);

        /* Now go through database and count cases with
         * Y=k, Y=k&Xi=xi, Parents=ps
         */

        npstates = 0;
        int numCases = data.length();
        for (n = 0; n < numCases; n++) {
            ps = 0;
            for (i = 0; i < ndad; i++) {
                d = dad[i];
                ps = ps * arity[d] + data.cmpnt(d).intAt(n);
            }
            kps = ps * arity[y] + data.cmpnt(y).intAt(n);
            if (Ndot[ps] == 0) {
                int[] temp = new int[arity.length];
                for (int ii = 0; ii < temp.length; ii++) {
                    try {
                        temp[ii] = data.cmpnt(ii).intAt(n);
                    } catch (RuntimeException e) {
                        System.out.println("---EXCEPTION---");
                        System.out.println("arity = " + Arrays.toString(arity));
                        System.out.println("data = " + data);
                        throw e;
                    }
                }
                estate[npstates] = temp;
                pstate[npstates] = ps;
                npstates++;
            }
            Nyl[kps]++;
            Ndot[ps]++;
        }
        
        return true;
    }

    void chsolve(int n, double x[], double b[]) {
        int j, k;
        double s;

        /*                                       ^     ^           ^
         * Do forward substitution (Solve Lc = b for c.     Store c in x)
         */

        for (j = 0; j < n; j++) {
            s = b[j];
            for (k = j - 1; k >= 0; k--) {
                s -= F[k][j] * x[k];
            }
            x[j] = s * id[j];
        }

        /*                                     ^       ^        
         * Do back substitution (Solve L'x = c for x.)
         */

        for (j = n - 1; j >= 0; j--) {
            s = x[j];
            for (k = j + 1; k < n; k++) {
                s -= F[j][k] * x[k];
            }
            x[j] = s * id[j];
        }
    }

    boolean cholesky(int n, double F[][]) {
        int i, j, k;
        double s;

        detSign = 1;
        LUn = n;
        for (i = 0; i < n; i++) { /* Cholesky decomposition */
            s = F[i][i];
            for (k = i - 1; k >= 0; k--) {
                s -= F[k][i] * F[k][i];
            }
            if (s <= 0) {
                detSign = -1;
                return false;
            }
            id[i] = 1.0 / Math.sqrt(s);
            for (j = i + 1; j < n; j++) {
                s = F[j][i];
                for (k = i - 1; k >= 0; k--) {
                    s -= F[k][i] * F[k][j];
                }
                F[i][j] = s * id[i];
            }
        }
        return true;
    }

    boolean chlogdet(double ldet[]) {
        int i;

        if (detSign == 1) {
            ldet[0] = 0.0;
            for (i = 0; i < LUn; i++) {
                ldet[0] -= log(id[i]);
            }
            ldet[0] *= 2.0;
            return true;
        } else {
            return false;
        }
    }

    /** logP returns the probability of data given parent data. */
    public double[] logP(Node node, int[] parentVal) {

        //int ry = arity[node.nd] - 1;
        int ry = c.length-1;
        double prob[] = new double[ry + 1];
        double total = 0.0;

        /* calc conditional probs for this parent combo */
        for (int i = 0; i <= ry; i++) {
            prob[i] = c[i];
            for (int j = 0; j < node.ndad; j++) {
                prob[i] += d[i][j][parentVal[node.dads[j]]];
            }
            prob[i] = Math.exp(prob[i]);
            total += prob[i];
        }

        // Normalise probabilities
        total = 1.0 / total;
        for (int k = 0; k <= ry; k++) {
            prob[k] *= total;
        }
        return prob;
    }


    /** Return (c,d) parameter structure */
    public Value.Structured getParams() {
        Value.Vector cVec = LogitFN.makeVec(c);
        Value.Vector dVec = LogitFN.makeVec(d);
        return new Value.DefStructured(new Value[] {cVec,dVec});
    }
    
    /*        ----------------  FUN void calcsums (node, diag) ------*/

    /*        Find the data sums necessary for calculation of the first and/or second
     *        derivatives of the log likelihood. If diag=1 only calculate the first
     *        derivatives and the diagonals of the second derivative matrix.    If
     *        diag!=1 also calculate the second derivative off diagonal sums.     This
     *        functions needs valid data counts.    These are set by a call to
     *        datacounts(node, data).     This function also calculates the log
     *        likelihood, and the node's conditional probabilities.
     *        
     *        There is no particularly efficient way to calculate these sums.
     *        
     *        Sums necessary for first derivatives are:
     *        Sum_{ps} Ndot[ps](Pr(k|ps) - Pr(ry|ps))
     *        Sum_{ps:Xi=v} Ndot[ps](Pr(k|ps) - Pr(ry|ps))
     *        
     *        To calculate the second derivatives we need the following sums:
     *        Sum_{ps} Ndot[ps](Pr(k|ps) + Pr(ry|ps) - (Pr(k|ps) - Pr(ry|ps))^2
     *        Sum_{ps} Ndot[ps](Pr(ry|ps) - (Pr(k|ps)-Pr(ry|ps))*(Pr(l|ps)-Pr(ry|ps))
     *        Sum_{ps:Xi=xi} Ndot[ps](Pr(k|ps) + Pr(ry|ps) - (Pr(k|ps) - Pr(ry|ps))^2
     *        Sum_{ps:Xi=xi}
     *                Ndot[ps](Pr(ry|ps) - (Pr(k|ps)-Pr(ry|ps))*(Pr(l|ps)-Pr(ry|ps))
     *        Sum_{ps:Xi=xi, Xj=xj}
     *                Ndot[ps](Pr(k|ps) + Pr(ry|ps) - (Pr(k|ps) - Pr(ry|ps))^2
     *        Sum_{ps:Xi=xi, Xj=xj}
     *                Ndot[ps](Pr(ry|ps) - (Pr(k|ps)-Pr(ry|ps))*(Pr(l|ps)-Pr(ry|ps))
     */

    //Sf Sy[Maxs];
    //Sf S[Maxs*Maxp*Maxs];

    double S2yz[][];// = new double[Maxs][Maxs];
    double S2ij[][];// = new double[Maxs * Maxp * Maxs][Maxs * Maxp * Maxs];

    void calcsums(Node node, int diag) {

        int i, j;
        int k, l;
        int kn, ln; /* used to index values of Y in a flat array */
        int kix;
        /// int xi, xj;
        int y;
        int ry;
        int n;

        int pofs[] = new int[numParents]; /* Offset of parent i in state ps into sums arrays */
        int kpofs[] = new int[numParents];
        int lpofs[] = new int[numParents];
        double s[];

        double Q;
        double P[] = new double[maxArity]; /* P[k] is Pr(Y=k|Parents = ps) */
        double Pry;

        int es, ps, kps; /* parent combination counter */
        int pv[]; /* value of parent[i] in current combo */

        //double C[] = new double[Maxs];            /* used to calculate 2nd deriv */

        double t, t2, t3;

        int ndad, dad[];

        y = node.nd;
        ndad = node.ndad;
        dad = node.dads;

        /* Zero necessary Sums */

        ry = arity[y] - 1;

        kix = kN[ry + 1]; /* number of combos of Y with one parent */        
        //System.out.println("allocating S2ij[" + (kix+1)+"]["+(kix+1)+"]");
        S2ij = new double[kix+1][kix+1];
        
        
        if (diag == 1) {
            for (k = 0; k < ry; k++) {
                Sy[k] = 0.0;
                S2yz[k][k] = 0.0;
            }
            for (i = 0; i < kix; i++) { /* for each Y=k,Xi=xi combo */
                S[i] = 0.0;
                S2ij[i][i] = 0.0;
            }
        } else {
            for (k = 0; k < ry; k++) {
                Sy[k] = 0.0;
                for (l = 0; l <= k; l++) {
                    S2yz[k][l] = 0.0;
                }
            }

            /* for each Y=k,Xi=xi combo */

            //Arrays.fill(S, 0, kix, 0);
            Arrays.fill(S, 0);
            //memset(S, 0, kix*sizeof(Sf));

            // TODO: I think this is what the code below is doing?
            for (int ii = 0; ii < S2ij.length; ii++) {
                Arrays.fill(S2ij[ii], 0);
            }
            /*
              j = sizeof(Sf);
              for (i=0; i<kix; i++, j += sizeof(Sf)) {
              memset(S2ij[i], 0, j);
              }
            */
        }

        /* Go through each parent instantiation that is present in the data
         * and set all the relevant conditional probabilities.    While we're at it,
         * may as well recalculate Log Likelihood
         */

        emlcost = 0.0;
        kps = 0;

        for (es = 0; es < npstates; es++) {
            pv = estate[es];
            ps = pstate[es];
            kps = ps * (ry + 1);
            n = Ndot[ps];

            //System.out.println("pv["+es+"] = "+Arrays.toString(pv));

            /* calc conditional probs for this parent combo */

            Q = 0.0;
            for (k = 0; k <= ry; k++) {

                P[k] = c[k];
                for (i = 0; i < ndad; i++) {
                    P[k] += d[k][i][pv[dad[i]]];
                }
                P[k] = Math.exp(P[k]);
                Q += P[k];
            }
            Q = 1.0 / Q;
            for (k = 0; k <= ry; k++, kps++) {
                P[k] *= Q;
                /*            printf("P[%d] = %5f\n", k, P[k]); */
                emlcost -= Nyl[kps] * log(P[k]);
            }
            /*        printf("\n"); */
            Pry = P[ry];

            /* Find necessary sums */

            if (diag == 1) {

                /* Calc only first deriv sums and diagonals of second
                 * deriv.
                 */

                for (k = 0; k < ry; k++) {
                    kn = kN[k];
                    t = P[k] - Pry;
                    t2 = n * t;
                    t3 = n * (P[k] + Pry - t * t);
                    Sy[k] += t2;
                    S2yz[k][k] += t3;
                    for (i = 0; i < ndad; i++) {
                        kix = kn + pN[i] + pv[dad[i]];

                        S[kix] += t2;
                        S2ij[kix][kix] += t3;
                    }
                }

            } else {

                /* find offsets of parents with their values for this
                 * parent state into the S and Sij arrays
                 */

                for (i = 0; i < ndad; i++) {
                    pofs[i] = pN[i] + pv[dad[i]];
                }

                /* Calc all first and second deriv sums. */

                for (k = 0; k < ry; k++) {
                    kn = kN[k];
                    t = P[k] - Pry;
                    t2 = n * t;
                    t3 = n * (P[k] + Pry - t * t);

                    Sy[k] += t2;
                    S2yz[k][k] += t3;
                    for (i = 0; i < ndad; i++) {
                        kpofs[i] = kix = kn + pofs[i];
                        S[kix] += t2;
                        s = S2ij[kix];
                        s[kix] += t3;
                        for (j = 0; j < i; j++) {
                            s[kpofs[j]] += t3;
                        }
                    }

                    for (l = 0; l < k; l++) {
                        ln = kN[l];
                        t3 = n * (P[ry] - t * (P[l] - P[ry]));
                        S2yz[k][l] += t3;
                        for (i = 0; i < ndad; i++) {
                            lpofs[i] = ln + pofs[i];
                            s = S2ij[kpofs[i]];
                            for (j = 0; j <= i; j++) {
                                s[lpofs[j]] += t3;
                            }
                        }
                    }
                }
                /*
                  for (k=0; k<ry; k++) {
                  printf("Sy[%d] = %5f\n", k, Sy[k]);
                  }
                  for (k=0; k<ry; k++) {
                  for (i=0; i<ndad; i++) {
                  for (xi=0; xi<states[dad[i]]; xi++) {
                  printf("S[%d][%d][%d] = %5f\n", k,i,xi,
                  S[ kN[k]+pN[i]+xi ]);
                  }
                  }
                  }
                  for (k=0; k<ry; k++) {
                  for (l=0; l<=k; l++) {
                  printf("S2yz[%d][%d] = %5f\n", k,l,S2yz[k][l]);
                  }
                  }
                  for (k=0; k<ry; k++) {
                  for (l=0; l<=k; l++) {
                  for (i=0; i<ndad; i++) {
                  for (xi=0; xi<states[dad[i]]; xi++) {
                  for (j=0; j<i; j++) {
                  for (xj=0; xj<states[dad[j]]; xj++) {
                  printf("S2ij[%d][%d][%d][%d][%d][%d] = %5f\n",
                  k,i,xi,l,j,xj,
                  S2ij[kN[k]+pN[i]+xi][kN[l]+pN[j]+xj]);
                  }
                  }
                  printf("S2ij[%d][%d][%d][%d][%d][%d] = %5f\n",
                  k,i,xi,l,i,xi,
                  S2ij[kN[k]+pN[i]+xi][kN[l]+pN[i]+xi]);
                  }
                  }
                  }
                  }
                  printf("\n---\n");
                */

            }

        }
    }

    /*        ----------------  FUN int calcfisher (node) -----------------*/

    /* Calculate the expected Fisher Information matrix.
     * Needs calcsums(node, 2) called prior to calling this function to
     * give the correct matrix.     Leaves the Fisher in global F.
     *
     * Returns the size of the Fisher matrix.
     */

    int calcfisher(Node node) {
        int i, j;
        int k, l;
        int kn, ln;
        int ki, kir, kj, kjr, lj, ljr;
        int xi, xj;
        int ri, rj, ry;
        int Fi, Fj; /* Indices for Fisher */

        int ndad, dad[];
        int y;
        double s;

        y = node.nd;
        ndad = node.ndad;
        dad = node.dads;

        /*    Now set the terms in the Fisher. */

        ry = arity[y] - 1;
        Fi = ry;
        for (k = 0; k < ry; k++) {
            kn = kN[k];
            Fj = ry;
            for (l = 0; l <= k; l++) {
                ln = kN[l];
                F[k][l] = S2yz[k][l]; /* c[k].c[l] terms */
                for (j = 0; j < ndad; j++) {
                    kj = kn + pN[j];
                    lj = ln + pN[j];
                    rj = arity[dad[j]] - 1;
                    s = S2ij[kj + rj][lj + rj];
                    for (xj = 0; xj < rj; xj++, Fj++) {
                        F[Fj][k] = S2ij[kj + xj][lj + xj] - s; /* c[k].d[l][j][xj] */
                    }
                }
            }
            F[k][k] += invsigsq + invsigsq; /* include param prior in Fisher */
            /* Have to swap the S2ij indices if l > k. */

            for (l = k + 1; l < ry; l++) {
                ln = kN[l];
                for (j = 0; j < ndad; j++) {
                    kj = kn + pN[j];
                    lj = ln + pN[j];
                    rj = arity[dad[j]] - 1;
                    s = S2ij[lj + rj][kj + rj];
                    for (xj = 0; xj < rj; xj++, Fj++) {
                        F[Fj][k] = S2ij[lj + xj][kj + xj] - s; /* c[k].d[l][j][xj] */
                    }
                }
            }

            /*    Now do the d[k][i][xi].d[l][j][xj] terms */

            for (i = 0; i < ndad; i++) {
                ki = kn + pN[i];
                ri = arity[dad[i]] - 1;
                kir = ki + ri;
                for (xi = 0; xi < ri; xi++, Fi++, ki++) {

                    /* Row Fi corresponds to param d[k][i][xi]
                     * Now examine all columns Fj < Fi
                     * This is messy but as efficient as possible
                     */

                    Fj = ry;
                    for (l = 0; l <= k; l++) {
                        ln = kN[l];

                        /* for j<i we need to go through all values of xj */

                        for (j = 0; j < i; j++) {
                            lj = ln + pN[j];
                            rj = arity[dad[j]] - 1;
                            ljr = lj + rj;
                            for (xj = 0; xj < rj; xj++, Fj++, lj++) {
                                F[Fi][Fj] = S2ij[ki][lj] + S2ij[kir][ljr]
                                    - S2ij[kir][lj] - S2ij[ki][ljr];
                            }
                        }

                        /* for j=i if xj < xi then Fisher term is just
                         *       sum_ps:ri Ndot[ps]*C[k][l]
                         */

                        lj = ln + pN[j];
                        rj = arity[dad[j]] - 1;
                        ljr = lj + rj;
                        for (xj = 0; xj < xi; xj++, Fj++) {
                            F[Fi][Fj] = S2ij[kir][ljr];
                        }

                        /* if j=i && xi=xj set the Fisher term */

                        F[Fi][Fj] = S2ij[ki][lj + xi] + S2ij[kir][ljr];
                        Fj++;

                        /* if l=k then Fj=Fi and this row is finished. */

                        if (l == k) {
                            F[Fi][Fi] += 4 * invsigsq; /* from param prior */
                            break;
                        }

                        /* for j=i if xj > xi then set Fisher term */

                        for (xj = xi + 1; xj < rj; xj++, Fj++) {
                            F[Fi][Fj] = S2ij[kir][ljr];
                        }

                        /* if l<k, for j>i we need to go through all values of xj */
                        /* need to swap i and j indices. */

                        lj = ln + pN[i];
                        ljr = lj + ri;
                        lj += xi;
                        for (j = i + 1; j < ndad; j++) {
                            kj = kn + pN[j];
                            rj = arity[dad[j]] - 1;
                            kjr = kj + rj;
                            for (xj = 0; xj < rj; xj++, Fj++, kj++) {
                                F[Fi][Fj] = S2ij[kj][lj] + S2ij[kjr][ljr]
                                    - S2ij[kjr][lj] - S2ij[kj][ljr];
                            }
                        }
                    }
                }
            }
        }
        return Fi;
    }
}
