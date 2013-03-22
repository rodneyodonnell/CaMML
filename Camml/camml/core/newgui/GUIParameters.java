package camml.core.newgui;

import java.text.DecimalFormat;
import java.util.Random;

import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner;
import camml.core.models.cpt.CPTLearner;
import camml.core.models.dual.DualLearner;
import camml.core.models.logit.LogitLearner;
import camml.core.search.SearchPackage;

/**Interface to define search parameters for the GUI.
 * These could all be defined in GUIModel or elsewhere, but are done here for clarity
 * and ease of maintenance.
 * 
 */
public interface GUIParameters {
	
	//TODO: Version number 
	public String versionNumber = "1.00";
	
	//Formatting options for results table
	public static final DecimalFormat formatPosterior = new DecimalFormat("0.00000");		//Posteriors to 5 DP
	public static final DecimalFormat formatRelativePrior = new DecimalFormat("0.0000");	//Relative priors to 4DP
	public static final DecimalFormat formatBestMML = new DecimalFormat("0.000");			//3 DP
	public static final DecimalFormat formatWeight = new DecimalFormat("0.000");			//3 DP
	
	//Minumum and maximum allowable values for search factor, maxSECs and minTotalPosterior.
	public static final double minSearchFactor = 0.05;			//Minimum search factor value allowable
	public static final double maxSearchFactor = 50.0;			//Maximum search factor value allowable
	
	public static final int minSECs = 3;						//Minimum value for "maxSECs" that is considered valid
	public static final int maxSECs = 1000;						//Maximum value for "maxSECs" that is considered valid
	
	public static final double min_minTotalPosterior = 0.30;		//Minumum value for "minTotalPosterior" that is considered valid
	public static final double max_minTotalPosterior = 1.00;		//Maximum value for "minTotalPosterior" that is considered valid
	

	//Available search types: These options will be presented to user in combobox.
	//Note: MMLLearners.length must equal MLLLearnerNames.length
	public static final ModelLearner[] MMLLearners = {
			SearchPackage.mmlCPTLearner,
			SearchPackage.dTreeLearner,
			LogitLearner.logitLearner,
			DualLearner.dualCPTDTreeLearner,
			DualLearner.dualCPTLogitLearner,
			DualLearner.dualDTreeLogitLearner,
			DualLearner.dualCPTDTreeLogitLearner,
			CPTLearner.mlMultinomialCPTLearner
	};
	
	//Names associated with the above learners.
	public static final String[] MMLLearnerNames = {
			"MML: CPT",
			"MML: DTree",
			"MML: Logit",
			"MML: CPT + DTree",
			"MML: CPT + Logit",
			"MML: DTree + Logit",
			"MML: CPT + DTree + Logit",
			"Max. Likelihood: CPT",
	};
	
	//ML Learner used for all instances of MetropolisSearch
	public static final ModelLearner MLLearner = CPTLearner.mlMultinomialCPTLearner;
	
	
	/*Available Random Number Generators
	 * Presently: 2 each of Java/Wallace random, one for random seeds and
	 *  the other for set seeds.
	 * Note: RNGs.length must equal RNGsString.length
	 */
	public static final Random[] RNGs = {
		new Random(),
		new Random(),
		new WallaceRandom(),
		new WallaceRandom()
	};
	
	//To display in the GUI combo box:
	public static final String[] RNGsString = {
		"Java RNG - Random Seed",
		"Java RNG - Set Seed",
		"Wallace RNG - Random Seed",
		"Wallace RNG - Set Seeds"
	};
	
	//For the above RNG, should a set seed be used (or a random one instead?)
	//Used by GUI to enable/disable RNG seed textboxes.
	//Note: RNGUseSetSeed.length must equal RNGs.length
	public static final boolean[] RNGUseSetSeed = {
		false,
		true,
		false,
		true
	};
	
	//Wallace RNGs require 2 seeds. True if RNG requires 2nd seed, false otherwise.
	//Note: RNGUseSetSeed2.length must equal RNGs.length
	public static final boolean[] RNGUseSetSeed2 = {
		false,
		false,
		false,
		true
	};
	
	
	
	/*Default string for expert priors, when user presses "New" button
	 * (Also when the user first checks the 'use expert priors' checkbox)
	 */
	public static final String defaultNewExpertPriorString = 
			"set {" +
			"\n\t//Can specify number of variables in data set: i.e. 'n=10;'" +
			"\n\t//May specify tier prior (defaults to 1.0): i.e. 'tierPrior = 0.9;'" +
			"\n\t//May specify edit distance (ed) prior (defaults to ~0.73)" +
			"\n\t//May specify KT prior (defaults to ~0.73)" +
			"\n}" +
			"\ned {" +
			"\n\t//Prior in format of edit distance from specified network or part" +
			"\n\t// network. Prior based on edit distance from this network." +
			"\n\t//Note: This section optional. Can be removed if not used." +
			"\n\t// Example: To specify a diamond network we could use" +
			"\n\t// 'a -> b; a -> c; b -> d; c -> d;' or 'a -> b c; d <- b c;'" +
			"\n}" +
			"\n\n//Use kt { ... ) for Kendall Tau (KT) prior." +
			"\n//KT prior - effectively 'bubble sort distance' between two total orderings." +
			"\n//Minimal KT distance added to undirected edit distance to determine prior for" +
			"\n//a given structure." +
			"\n\ntier {" +
			"\n\t//Tiers allow a total ordering of variables to be specified." +
			"\n\t//Format: 'A B C < D E F < G H I;' means variables A,B,C are before" +
			"\n\t// D,E,F in the total ordering of variables, (i.e. A can be a parent" +
			"\n\t// of D, but D cannot be a parent of A) and so on." +
			"\n}" +
			"\narcs {" +
			"\n\t//Allows individual arc relationships to be specified." +
			"\n\t//Available arc types. Note: specified in format 'A -> B 0.7;'" +
			"\n\t// where the number is the probability of that arc existing." +
			"\n\t//Directed arc: i.e. A -> B or B <- A" +
			"\n\t//Undirected arc: i.e. A -- B" +
			"\n\t//Ancestor: A => B or B <= A" +
			"\n\t//Correlated: A == B" +
			"\n\t//Tier: A << B or B >> A" +
			"\n}";
	
}
