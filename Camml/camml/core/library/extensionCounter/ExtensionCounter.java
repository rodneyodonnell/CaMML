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
// Functions Calculating the number of linear extensions of a TOM
//

// File: CountExtensions.java
// Author: rodo@dgs.monash.edu.au


/**
 * Class containting functions to calculate linear extensions of a TOM quickly. <br>
 * Calculating extensions (ie. total orderings which comply to the partial ordering
 * implied by a DAG) is a NP complete problem /ref{?} <br>
 *
 * However, by storing partial results we may be able to amortise this NP cost when
 * counting extensions on many TOMs.  Naturally we are still faced with a NP problem,
 * but this approach will hopefully move the "knee" a bit further along.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.12 $ $Date: 2007/02/21 07:12:37 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/library/ExtensionCounter.java,v $
 */

package camml.core.library.extensionCounter;

import java.util.Hashtable;

import camml.core.search.TOM;
import camml.plugin.netica.NeticaFn;
import cdms.core.Value;


public class ExtensionCounter {

    /** Test function, load in Netica network and return number of extensions 
     *  Performs comparison in prior probability using DAG and TOM based priors. */
    public static void main( String args[]) {        
        if (args.length != 1) { 
            System.out.println("Usage: java camml.core.library.ExtensionCounter net.dne"); 
            return;
        }
        Value.Structured my = NeticaFn.LoadNet._apply(args[0]);
        
        Value.Vector params = (Value.Vector)my.cmpnt(1);
        int n = params.length();
        TOM tom = new TOM(n);
        tom.setStructure(params);
        UnlabelledGraph64 dag = new UnlabelledGraph64(tom);
        double numPerms = DynamicCounter.dCounter.countPerms(dag);
        //System.out.println("numPerms = " + numPerms);

        double totalTOMs = Math.pow(2, n*(n-1)/2) * factorial(n);
        double tomPrior = (numPerms / totalTOMs);
        System.out.println("TOM prior = " + tomPrior + "\t=\t" + 
                           numPerms + " / " + totalTOMs);
        
        double totalDAGs = countDAGs(n);
        double dagPrior = (1 / totalDAGs);
        System.out.println("DAG prior = " + dagPrior + "\t=\t" + 
                           1 + " / " + totalDAGs);

        System.out.println("Ratio = " + (tomPrior/dagPrior) + " : 1");
    }
    
    public static double factorial(int n) {
        if (n == 0) { return 1.0; }
        else { return n * factorial(n-1); }
    }
    
    static Hashtable<Integer, Double> dagCountHash = new Hashtable<Integer, Double>();
    
    /** Return number of DAGs with n nodes */
    public static double countDAGs(int n) {        
        if (dagCountHash.containsKey(n)) { return dagCountHash.get(n); }
        if (n <= 1) return 1;
        double sum = 0;
        for (int k = 1; k <= n; k++) {
            sum += -Math.pow(-1,k) * choose(n,k) * Math.pow(2,k*(n-k)) * countDAGs(n-k);
        }
        dagCountHash.put(n, sum);
        return sum;
    }
    public static double choose( int n, int r) {
        return factorial(n)/(factorial(r)*factorial(n-r));
    }

}
