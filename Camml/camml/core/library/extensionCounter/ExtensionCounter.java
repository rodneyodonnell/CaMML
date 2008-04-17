//
// Functions Calculating the number of linear extensions of a TOM
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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
