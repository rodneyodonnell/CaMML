package camml.core.newgui;

import java.util.Random;

import camml.core.library.WallaceRandom;
import camml.core.models.ModelLearner;
import camml.core.models.cpt.CPTLearner;
import camml.core.models.dual.DualLearner;
import camml.core.models.logit.LogitLearner;
import camml.core.models.multinomial.BDELearner;
import camml.core.search.SearchPackage;

/**Interface to define search parameters for the GUI.
 * These could all be defined in GUIModel, but are done here for clarity
 * and ease of maintenance.
 * 
 */
public interface GUIAvailableParameters {

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
			CPTLearner.mlMultinomialCPTLearner,
			BDELearner.bdeLearner
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
			"BDe CPT Learner (ESS = 5)"
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
	
	
	
	//Default string for expert priors, when user presses "New" button:
	public static final String defaultNewExpertPriorString = 
			"set {" +
			"\n\t//Must specify number of variables in data set: i.e. 'n=10;'" +
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
			"\nkt {" +
			"\n\t//Prior based on Kendall Tau - effectively 'bubble sort distance'" +
			"\n\t// between two total orderings. Minimal KT distance added to" +
			"\n\t// undirected edit distance to determine prior for a given structure." +
			"\n\t//Note: This section optional. Can be removed if not used." +
			"\n}" +
			"\ntier {" +
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
