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

package camml.core.models.dTree;

/**
 * CatlanTable produces details about Catlan numbers. <br>
 * the nth catalan number is {[2n]C[n]}/(n+1)
 */
public class CatlanTable {
    static int maxCalculated = 0;
    
    /**
     * catlan[n] = number of values representable by a string of exactly
     * numBits[n] bits
     */
    protected static double[] catlan = new double[maxCalculated];
    
    /**
     * maxRepresentable[n] = maximum number representable in no more than
     * numBits[n] bits
     */
    protected static double[] maxRepresentable = new double[maxCalculated];
    
    /** Total probability of numbers requiring numBits[n] bits */
    protected static double[] prob = new double[maxCalculated];
    
    /** Total probaiblity of numbers requiring no more than numBits[n] bits */
    protected static double[] totalProb = new double[maxCalculated];
    
    /** All requests above 514 will throw an UnreliableResultException */
    public static final int maxReliable = 514;
    
    /** calculate nCr */
    public static double nCr(int n, int r) {
        
        // factorial exceeds the bounds of a double too easliy. only works
        // to 170_C_85
        //         return factorial(n) / ( factorial(n-r) * factorial(r) );
        
        // By changing the number order we extend out range from 354_C_177
        // to 1028_C_514
        // before we run into overflow issues. Order used (maybe not best,
        // but better than
        // linear) is {1,n,2,n-1,3,n-2....n/2}
        int order[] = new int[n];
        int start = 1;
        int end = n;
        for (int i = 0; i < order.length; i++) {
            if ((i & 1) == 0) { // if even
                order[i] = start;
                start++;
            } else { // if odd
                order[i] = end;
                end--;
            }
        }
        
        double val = 1;
        
        // the formula n!/((n-r)!*r!) can be expanded to reduce the number
        // of multiplications
        // needed and the chance of overflow. We do this by grouping all
        // multiplications and
        // divisions by i into a single step and cancelling as required.
        // (1). for all i < n, we multiply by i
        // (2). for all i < n-r we divide by i,
        // (3). for all i < r we divide by i.
        // We loop over all values of i from 1 to n, so (1) is performed
        // every step.
        // if ( i < n-r && i < r ) then we have (i / i / i == 1/i) so we
        // divide by i.
        // if ( i < n-r XOR i < r ) we have (i / i == 1) so no
        // multiplicaiton is required.
        // if ( !i < n-r && !i < r ) we simply have i. so multiply by i.
        for (int i = 0; i < n; i++) {
            int x = order[i];
            
            // condition 1 & 2
            boolean c1 = x <= r;
            boolean c2 = x <= (n - r);
            
            if (c1 && c2) {
                val = val / x; // divide by i
            } else if ((c1 && !c2) || (!c1 && c2)) {
                // val = val; // unchanged
            } else if (!c1 && !c2) {
                val = val * x; // multiply by i.
            } else {
                throw new RuntimeException("Unreachable state");
            }
        }
        return val;
    }
    
    /** In class constructer call setMaxCalculated */
    static {
        setMaxCalculated(maxReliable);
    }
    
    /** calculate all values up to n */
    protected static void setMaxCalculated(int n) {
        
        double[] oldCatlan = catlan;
        double[] oldMaxRepresentable = maxRepresentable;
        double[] oldProb = prob;
        double[] oldTotalProb = totalProb;
        
        catlan = new double[n];
        maxRepresentable = new double[n];
        prob = new double[n];
        totalProb = new double[n];
        
        // copy across values from old arrays.
        for (int i = 0; i < maxCalculated; i++) {
            catlan[i] = oldCatlan[i];
            maxRepresentable[i] = oldMaxRepresentable[i];
            prob[i] = oldProb[i];
            totalProb[i] = oldTotalProb[i];
        }
        
        for (int i = maxCalculated; i < n; i++) {
            catlan[i] = nCr(2 * i, i) / (i + 1);
            prob[i] = catlan[i] / Math.pow(2, getNumBits(i));
            
            if (i > 0) {
                maxRepresentable[i] = maxRepresentable[i - 1] + catlan[i];
                totalProb[i] = totalProb[i - 1] + prob[i];
            } else {
                maxRepresentable[i] = catlan[i];
                totalProb[i] = prob[i];
            }
            
            //         if ( i > 400 ) {
            //             System.out.println("i = " + i + "\t" +
            //                        "ncr(2*i,i) = " + nCr(2*i,i) + "\t" +
            //                        "catlan[i] = " + catlan[i] + "\t" +
            //                        "max[i] = " + maxRepresentable[i] + "\t" +
            //                        "prob[i] = " + prob[i] + "\t" +
            //                        "totalProb[i] = " + totalProb[i]
            //                        );
            //         }
        }
        
        maxCalculated = n;
    }
    
    /** lazily calculate and return numBits[n] */
    public static int getNumBits(int n) {
        return 2 * n + 1;
    }
    
    /** lazily calculate and return catlan[n] */
    public static double getCatlan(int n) {
        return catlan[n];
    }
    
    /** lazily calculate and return maxRepresentable[n] */
    public static double getMaxRepresentable(int n) {
        return maxRepresentable[n];
    }
    
    /** lazily calculate and return prob[n] */
    public static double getProb(int n) {
        return prob[n];
    }
    
    /** lazily calculate and return totalProb[n] */
    public static double getTotalProb(int n) {
        return totalProb[n];
    }
    
}
