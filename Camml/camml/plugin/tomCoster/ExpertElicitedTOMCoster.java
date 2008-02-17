//
// TOMCoster which accepts a string of expert ellicited priors.
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: ExpertElicitedTOMCoster.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.tomCoster;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import camml.core.search.TOM;
import camml.core.search.TOMCoster.DefaultTOMCosterImplementation;
import cdms.core.Type;
import cdms.core.Value;

/**
* ExpertElicitedTOMCoster implements TOMCoster and returns a cost biased by an
* expert. The costs returned are relative costs as normalising the costs is an
* exponential problem and CaMML only needs to know relative costings. <p>
* 
* There are four ways to specify priors. This gives an expert great freedom to
* specify a desired prior distribution, while keeping complexity to a minimum. <p>
*  
*  <b>Overview</b> <br>
*  An expert elicited prior distribution is entered as a string, referred to as the prior string.
*  This string consists of a label [set|ed|kt|arcs|tier] followed by options {...}   <p>

*  For example <br>
*  set {n = 2;} tier {0 < 1} <br>
*  This prior if valid for any two variable network and states that node0 comes before node1
*  in the total order, so (0 -> 1) and (0 1) are valid networks, but (0 <- 1) is not. <p>
*  
*  <b>TAGS</b>: <br>
*  <b> set</b> : set contains a list of variables and their values. <br>  
*  Valid variables include <br>
*  <i>	n</i> : Number of nodes in the network (required) <br>
*  <i>	edPrior</i> : Strength of belief in ed prior (defaults to ~0.73) <br>
*  <i>	ktPrior</i> : Strength of belief in kt prior (defaults to ~0.73) <br>
*  <i>	tierPrior</i> : Strength of belief in ed prior (defaults to 1.0, ie absolute belief.) <br>
*  <i>  ignoreBadLabels</i> : [true|false], when true labels not present in dataset may be specified 
*       in priors but will be ignored.  This allows identical prior information to be used when adding
*       and removing variables from a dataset.
*  <p>
*  
*  <b> ed </b> : Prior is based on edit distance from the expert specified network.
*  			By default, the cost per arc addition/deletion is 1 nit, which 
*  			corresponds to an expert prior of ~73%.  Arc reverses are counted
*  			as a deletion + an insertion.  This prior can be changed using 
*  			set {edPrior = 0.99;} <br>
*  To specify a diamond network we could use <i>ed {a -> b; a -> c; b -> d; c -> d;}</i>
*  or <i>ed {a -> b c; d <- b c;}</i>
*  <p>
*  
*  <b> kt </b> : kt stands for Kendall Tau which is effectively the
*  "bubble sort distance" between two total orderings.  For our kt prior
*  we find the minimal kt distance between the ordering of the current TOM and
*  a TOM compatible with the expert ellicited network.  We add this to the undirected
*  edit distance between the expert network and the current net. <br>
*  
*  kt = min(KendallTau(current TOM, TOM compatible with expert DAG)) + undirectedED(current TOM, expert DAG) <br>
*  
*  Using a kt prior instead of an ed prior should reward TOMs with a total ordering
*  closer to that specified by the expert DAG. 
*  
*  eg: TRUE = {a -> b; b -> c; c -> d;}  x = {b -> c; c -> d; d -> a;} <br> 
*  ed(true,false) = 1 <br>
*  kt(true,false) = 3 (a must have 3 "swaps" performed to be consistent with true ordering.) <br>
*  <p>
*  
*  <b> arcs </b> : the arcs tag allows us to specify individual arcs, unlike ed or kt
*  where we must specify a prior over the whold network.  If no value is specified
*  for a given arc, the default CaMML prior is used for that arc. <br>
*  
*  We can specify five types of pairwise relations.  Directed arc <i>-><i>, Undirected arc <i>--<i>, 
*  Ancestor <i>=><i>, Correlated <i>==<i> and tier <i><<<i> where <i> << <i> is pronounced 
*  as "preceeds". 
*  Each of these types may also be reversed <i><-<i>, <i><=<i> <i>>><i>. <br>
*  A directed arc gives the probability of a directed link from a -> b, an undirected arc
*  gives the probability of a directed arc from a -> b or b -> a.  
*  Ancestor gives the probability that a is an ancestor of b.
*  Correlated gives the probabilty that a and b are correlated, that is either one is an
*  ancestor of the other, or they have a common ancestral cause.
*  Precedence gives the chance that
*  a comes before b in the total ordering, irrespective of connections.  Though this may
*  seem less important, as metropolis sampling is done over TOM space it does shift the 
*  posterior noticably. <br>
*  
*  example: arcs {0 -> 1 0.9; 1 -- 2 0.4, 3 > 4 1.0;} <br> 
*  <p>
*  
*  <b> tier </b> : The final form of prior knowledge is tiers.  These are (generally deterministic)
*  constraints on the total ordering.  Though we could model the tiers tag using 
*  the arcs tag and precedence operator
*  this gets complicated when we have many variables and must specify all constraints. <br>
*  
*   example: tier {1 2 3 < 4 5 6 < 7 8 9;} <br>
*   Note about ordering: The example above shows 1,2 & 3 are before 4,5&6 in the total
*   ordering. So in our network 1 can be a parent of 5, but 5 cannot be a parent of 1.
* 
*   <p>
*   <b> combination </b> : We can combine any of the above priors with the restriction
*   that ed and kt cannot both be present.  It is also possible to have multiple tier and
*   arcs tags.  Multiple tiers tags is especially useful for specifying more complicated
*   tier structures. <br>
*   
*   example: set {n = 4; edPrior = 0.9;} ed {0 -> 1 2; 3 <- 1 2;} arcs {1 -- 2 0.0;} tier {0 < 3;}
*   This prior says an expert thinks we have a diamond structure (ed), is positive 1 -- 2 
*   are NOT connected, and is also positive 0 preceeds 3 in the total ordering.
* 
* @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
* @version $Revision: 1.28 $ $Date: 2007/05/15 10:21:35 $ 
* $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tomCoster/ExpertElicitedTOMCoster.java,v $
*/
public class ExpertElicitedTOMCoster extends DefaultTOMCosterImplementation {

	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5329133940445450115L;

	/** Flag if Kendall Tau distance should be used <br>
	 *  runUndirectedCost should also generally be used with KT. */
	private boolean runKTPrior = false;
	
	/** Flag if Edit Distance should be used <br>
	 *  Should generally not be used with (runKT == true) */
	private boolean runEDPrior = false;

	/** Variable labels */
	protected final String[] labels;
	
	/** Flag bad labels as '-2' in in readVarByName. */
	public boolean ignoreBadLabels = false;
	
	/** Use prior over direct connections */
	public boolean runDirectRelationPrior = false;
	
	/** Use prior over indirect relations .*/
	public boolean runIndirectRelationPrior = false;

	/** Flag if uniformTOMCoster should be run in addition to expert priors.*/
	public boolean runDefaultPrior = true;
	
	/** Cost per variation in edit distance. Default 1 nit (~73.2% confidence from expert)*/
	private double edPriorPenalty = 1;
	
	/** Cost per variation in kendlal tau distance. Default 1 nit (~73.2% confidence from expert)*/
	private double ktPriorPenalty = 1;
	
	/** Cost per variation in tier structure.*/
	private double tierProb = 1.0;

	public double getTierProb() { return tierProb; }

	/** Number of nodes in expected data. */
	private int numNodes = -1;
	
	/** Connection matrix used storing prior DAG specified for ed or kted. */
	public boolean[][] edPriorDAG;
	
	/** Direct relation priors. */
	public DirectRelationPrior[][] directPrior;
	
	/** Direct relation priors. */
	public IndirectRelationPrior[][] indirectPrior;

	protected double arcProb = .5;
	public double getArcProb() { return arcProb; }	
	public void setArcProb(double arcProb) {
		this.arcProb = arcProb;
		// Update direct priors.
		if ( runDefaultPrior || runDirectRelationPrior ) {
			for( RelationPrior rp2[] : directPrior )
				for ( RelationPrior rp : rp2 )
					rp.setArcP(arcProb);
		}
		// Update indirect priors.
		if ( runIndirectRelationPrior ) {
			for( RelationPrior rp2[] : indirectPrior )
				for ( RelationPrior rp : rp2 )
					rp.setArcP(arcProb);
		}
	}

	/** Convenience construcot which automatically extracts names.*/
	public ExpertElicitedTOMCoster(double arcP, Reader reader, Value.Vector data ) {		
		this(arcP,reader,((Type.Structured)((Type.Vector)data.t).elt).labels);		
	}
	
	/** Convenience constructor*/
	public ExpertElicitedTOMCoster(double arcP, Reader reader ) {
		this(arcP,reader,(String[])null);
	}
	
	/** Initialise Expert TOMCoster. See readPriors for format details. */
	public ExpertElicitedTOMCoster(double arcP, Reader reader, String[] labels) {
		this.labels = labels;
		if (labels != null) { setSize(labels.length); }
		try {			
			readPriors(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		setArcProb(arcP);
	}

	/** Return cost to state TOM structure given expert priors. <br>
	 *  NOTE: The cost returned is a relative cost (in nits).  
	 *  In theory we could normalise the cost, but this is impractical
	 *  and generally unnecesarry. 
	 *  */
	public double cost(TOM tom) {
		int n = tom.getNumNodes();
		boolean[][] arcMatrix = edPriorDAG;

		
		if (this.numNodes != n) {
			throw new RuntimeException("Specified prior and TOM sizes differ. " + this.numNodes + " != " + n);
		}
		
		// Calculate edit distances.
		double ed = 0;
		if (runEDPrior) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {			
					// A misdirected arc costs 2 (remove + add)
					if ((arcMatrix[j][i] && !tom.isDirectedArc(i, j))
							|| (!arcMatrix[j][i] && tom.isDirectedArc(i, j))) {
						ed = ed + edPriorPenalty;
					}
				}
			}
		}

		double kt = 0;
		if (runKTPrior) {
			int undirectedED = 0;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if ((i < j)	&& (arcMatrix[i][j] || arcMatrix[j][i]) != tom.isArc(i, j)) {
						undirectedED = undirectedED + 1;
					}
				}
			}

			// Extract total order from tom.
			int[] tomTotalOrder = new int[tom.getNumNodes()];
			for (int i = 0; i < tomTotalOrder.length; i++) {
				tomTotalOrder[i] = tom.nodeAt(i);
			}

			// Calculate kendallTau distance.
			int[] tempOrder = generate(convertMatrix(arcMatrix), tomTotalOrder);
			kt = kendallTau(tomTotalOrder, tempOrder) + undirectedED;
			if (kt != 0.0) {kt *= ktPriorPenalty;}  // Check for zero to avoid 0*inf=NaN issues.
		}
				
		double directCost = 0; // Cost to state direct relationships.
		double indirectCost = 0; // Cost to state indirect relationships.
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {

				if ( runDirectRelationPrior || runDefaultPrior) {
					RelationPrior p = directPrior[i][j];
					double x = p.relationCost(tom);
					directCost += x;
				}
				
				if ( runIndirectRelationPrior ) {
					IndirectRelationPrior p2 = indirectPrior[i][j];
					if (p2.priorSet ) {
						double x = p2.relationCost(tom);
						indirectCost += x;
					}
				}
			}
		}

		// Convert edit distance to a tom cost.
		return ed + kt + directCost + indirectCost;
	}

	/** Read in priors from an input stream.  <br>
	 *  Expected file format : <br>
	 *  set { n = 10; edWeight = 0.75; tierWeight = 1.0}   // General settings.
	 *  ed { 0 -> 1 2; 3 <- 2 1; }   // List of arcs (arrowc can face either way)
	 *  arcs {0 -> 1 0.9; 1 -- 2 0.4, 3 < 4 1.0;} // Set directed, undirected and temporal priors. 
	 *  tier { 1 2 3 < 4 5 6 < 7 8 9; }  // Set tier constraints.  */
	public void readPriors(Reader in) throws IOException {
		
		// Create StreamTokenizer using c++ style comments.
		StreamTokenizer st = new StreamTokenizer(in);
		st.eolIsSignificant(false);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.wordChars('_','_');

		
		// Create hash for storing tokens in.
		HashMap<String,Object> hash = new HashMap<String,Object>();
				
		while ( st.nextToken() != StreamTokenizer.TT_EOF ) {
			if (st.ttype != StreamTokenizer.TT_WORD) {badToken("word",st);} 
			
			if ( "set".equals(st.sval) ) { readSet(hash,st); }
			else if ( "ed".equals(st.sval)) { 
				if (runKTPrior||runEDPrior) { badToken("kt or ed already specified",st); } 
				runEDPrior = true; runDefaultPrior = false; readED(hash,st); 
			}
			else if ( "kt".equals(st.sval)) { 
				if (runKTPrior||runEDPrior) { badToken("kt or ed already specified",st); } 
				runKTPrior = true; runDefaultPrior = false; readED(hash,st); 
			}
			else if ( "arcs".equals(st.sval)) { 
				readArcs(hash,st);
			}
			else if ( "tier".equals(st.sval)) { 
				readTiers(hash,st);
			}
			else { badToken("set|ed|kt|arcs|tier",st); }
		}
	}

	/** Convenience function to throw exceptions when bad tokens are read. */
	protected static void badToken(String expected, StreamTokenizer st) throws IOException {
		throw new IOException("Bad token: expected <" + expected + "> found " + st);
	}

	/** throws an exception if 0 < p <= 1 */
	protected static void checkRange(double p) {
		if (p < 0.0 || p > 1.0) { throw new RuntimeException("Probability out of range " + p); }
	}
	/** Throw exception if p is not a non-zero probability  */
	protected static void checkPositive(double p) {
		if (p <= 0.0 || p > 1.0) { throw new RuntimeException("Probability out of range " + p); }
	}
	
	/** Read through stream tokenizer from '{' to '}' adding all
	 *  "x=y" combinations to hash. x = key, y = val. */
	private void readSet(HashMap<String,Object> hash, StreamTokenizer st) throws IOException {
		
		if (st.nextToken() != '{')  {badToken("{",st);}
		
		while (true) {
			if (st.nextToken() == '}') {break;};
			String x = st.sval;
			if (st.nextToken() != '=') {badToken("=",st);}
			st.nextToken();
			Object y=null;
			if (st.ttype == StreamTokenizer.TT_WORD) { y = st.sval; }
			else if (st.ttype == StreamTokenizer.TT_NUMBER) {y = new Double(st.nval);}
			else {badToken("word or number",st);}
			//System.out.println(x + "\t = \t" + y);
			if (st.nextToken() != ';') {badToken(";",st);}
			hash.put(x,y);
		}
		
		// Set a few variables we may have read in.
		if (hash.containsKey("edPrior")) { 
			double p = ((Double)hash.get("edPrior")).doubleValue();
			checkPositive(p);
			this.edPriorPenalty = Math.log(p) - Math.log(1-p);
			hash.remove("edPrior");
		}
		if (hash.containsKey("ktPrior")) {
			double p = ((Double)hash.get("ktPrior")).doubleValue();
			checkPositive(p);
			this.ktPriorPenalty = Math.log(p) - Math.log(1-p);
			hash.remove("ktPrior");
		}
		if (hash.containsKey("tierPrior")) {
			double p = ((Double)hash.get("tierPrior")).doubleValue();
			checkPositive(p);
			this.tierProb = p;
			hash.remove("tierPrior");
		}
		if (hash.containsKey("ignoreBadLabels")) {
			String s = (String)hash.get("ignoreBadLabels");
			if ("true".equals(s)) { ignoreBadLabels = true; }
			else if ("false".equals(s)) { ignoreBadLabels = false; }
			else { throw new RuntimeException("Expected true of false");}
			hash.remove("ignoreBadLabels");
		}

		if (hash.containsKey("n")) {
			setSize( ((Double)hash.get("n")).intValue() );
			hash.remove("n");
		}
		if (hash.size() > 0) {
			throw new RuntimeException("Unknown entries in set: " + hash.toString());
		}

	}
	
	public void setSize( int numNodes ) {
		this.numNodes = numNodes;
		directPrior = new DirectRelationPrior[numNodes][];
		indirectPrior = new IndirectRelationPrior[numNodes][];
		for ( int i = 0; i < directPrior.length; i++) {
			directPrior[i] = new DirectRelationPrior[i];
			indirectPrior[i] = new IndirectRelationPrior[i];
			for ( int j = 0; j < directPrior[i].length; j++) {
				directPrior[i][j] = new DirectRelationPrior(i,j,numNodes,arcProb);
				indirectPrior[i][j] = new IndirectRelationPrior(i,j,numNodes,arcProb);
			}
		}		
	}

	/** Read in matrix from user.  
	 *  Each line may take the form 0 -> 1 2 3; or 1 <- 0 1 2; */
	private void readED(HashMap hash, StreamTokenizer st) throws IOException {
		if (st.nextToken() != '{')  {badToken("{",st);}
		
		
		int n = numNodes;
		if (n == -1) {throw new RuntimeException("n not set, use set {n = x;}"); }
		if (edPriorDAG == null) {edPriorDAG = new boolean[n][n];}
		
		while (true) {
			if (st.nextToken() == '}') {break;};
			
			// Read in x
			//if (st.ttype != StreamTokenizer.TT_NUMBER) {badToken("variable #",st);}
			//int x = (int)st.nval;			
			int x; st.pushBack();
			if ((x = readVarByName(st)) == -1) { badToken("variable #", st); }
			// discard bad label and contine on next token.
			if (x == -2) continue;

			
			// Read in -> or <-, this tells us if x is a parent or child of [y].
			boolean xParent = true;
			st.nextToken();
			if (st.ttype == '-') {
				if (st.nextToken() != '>') {badToken("->",st);}
				xParent = true;
			}
			// FIXME: If 0<-1 is encountered, streamtokenizer gets confused and
			//        parses tokens as {0, <, -1} instead of {0,<,-,1}
			//        I'm not sure of the easiest way to fix this.
			else if (st.ttype == '<') {
				if (st.nextToken() != '-') {badToken("<-",st);}
				xParent = false;				
			}
			
			// Read all values of 'y' and add arcs to connectionMatrix
			while (st.nextToken() != ';') {
				//if (st.ttype != StreamTokenizer.TT_NUMBER) {badToken("variable #",st);}
				//int y = (int)st.nval;
				int y; st.pushBack();
				if ((y = readVarByName(st)) == -1) { badToken("variable #", st); }
				
				if (xParent) {edPriorDAG[y][x] = true;}
				else {edPriorDAG[x][y] = true;}
			}
		}
		
		
	}
	 	
	/** Convenience function to set directedPrior[x][y],  directedPriot[y][x]
	 *  undirectedPrior[x][y] or undirectedPrior[y][x] */
	private void setP(int x, int y, String op, double p ) {
		if ( "->".equals(op) || "<-".equals(op) ||
			 ">>".equals(op) || "<<".equals(op) ||	
			 "--".equals(op) ) {
			runDirectRelationPrior = true;
			runDefaultPrior = false;
			if (x>y) {directPrior[x][y].setP( op, p );}
			else {directPrior[y][x].setP2( op, p );}
		}
		else if ("=>".equals(op) || "<=".equals(op) ||
		 	     "==".equals(op) ) {
			runIndirectRelationPrior = true;
			if (x>y) {indirectPrior[x][y].setP( op, p );}
			else {indirectPrior[y][x].setP2( op, p );}
		 }
		else {
			throw new RuntimeException("Unrecognised operation : " + op);
		}
	}

	
	/** Read arcs in a pairwise manner. There are three options <br>
	 *  a -> b 0.9; means expert is 90% sure a directed arc exists from a to b <br>
	 *  a -- b 0.8; means expert is 80% sure an undirected arc exists from a to b <br>
	 *  a => b 0.9; means expert is 90% sure a directed chain of arcs exists from a to b <br>
	 *  a == b 0.8; means expert is 80% sure an a to b are correlated by a directed chain
	 *              (in either direction) or a common ancestor. <br>
	 *  a >> b 0.7; means expert is 70% sure a is before be in the total order. <br>
	 *  If 1.0 is specified, the relationship is treated as a hard constraint. <br>
	 *  "a >> b 0.9 c 0.8;" is equivelant to "a >> b 0.9; a >> c 0.8;" 
	 *  */	
	private void readArcs(HashMap hash, StreamTokenizer st) throws IOException {
		if (st.nextToken() != '{')  {badToken("{",st);}
			
		int n = numNodes;
		if (n == -1) {throw new RuntimeException("n not set, use set {n = x;}"); }
		
		while (st.nextToken() != '}') {
			
			// Read in x			
			//if (st.ttype != StreamTokenizer.TT_NUMBER) {badToken("variable #",st);}
			//int x = (int)st.nval;
			int x; st.pushBack();
			if ((x = readVarByName(st)) == -1) { badToken("variable #", st); }
			// discard bad label and continue.
			if (x == -2) continue;
			
			
			// Read in a two character operation.			
			String validOperations = "'->' '<-' '--' '=>' '<=' '==' '>>' or '<<'"; 
			String operation = "";
			st.nextToken();
			if (st.ttype != '<' && st.ttype != '>' && st.ttype != '-' && st.ttype != '=') {
				badToken(validOperations,st);
			}
			operation += (char)st.ttype;

			st.nextToken();
			if (st.ttype != '<' && st.ttype != '>' && st.ttype != '-' && st.ttype != '=') {
				badToken(validOperations,st);
			}
			operation += (char)st.ttype;

			
			
			
				
			// Read all values of 'y' and add arcs to connectionMatrix
			while (st.nextToken() != ';') {
				//if (st.ttype != StreamTokenizer.TT_NUMBER) {badToken("variable #",st);}
				//int y = (int)st.nval;
				int y; st.pushBack();
				if ((y = readVarByName(st)) == -1) { badToken("variable #", st); }									
				
				if (st.nextToken() != StreamTokenizer.TT_NUMBER) {badToken("probability",st);}
				double p = st.nval;
				checkRange(p);
					
				setP( x, y, operation, p );
					
			}
		}			
	}
	
	/** If next token is an int or a variable name, return corresponding
	 *  variable number.  If int out of range or name not found, return -1 <br>
	 *  if (ignoreBadLabels == true), returns -2 on bad labels. */
	protected int readVarByName( StreamTokenizer st) throws IOException{
		st.nextToken();
		if (st.ttype == StreamTokenizer.TT_NUMBER) {
			int x = (int)st.nval;
			if (x >= 0 && x < numNodes) { return x; }
		}
		else if (labels != null && st.ttype == StreamTokenizer.TT_WORD) {			
			String s = st.sval;
			for ( int i = 0; i < labels.length; i++) {
				if (s.equals(labels[i])) {
					return i;
				}
			}
			// if bad labels are ok, return -2.  If not, return -1 as normal.
			if (ignoreBadLabels) { return -2; }
		}
		// Failed, return -1
		return -1;
	}
	
	/** Read in tiers. <br>
	 *  Format is: tier {a b > c d > e; a > f} <br>
	 *  This above is equivalent too arcs {a>>c 1.0; a >> d; 1.0; a >> e 1.0; b >> c 1.0;
	 *  	b >> d 1.0;  b >> e 1.0; c >> e 1.0; d >> e 1.0; a >> f 1.0; } */
	private void readTiers(HashMap hash, StreamTokenizer st) throws IOException {
		if (st.nextToken() != '{')  {badToken("{",st);}
		
		System.out.println("labels = " + Arrays.toString(labels) + "\t in readVarByName");
		
		int n = numNodes;
		if (n == -1) {throw new RuntimeException("n not set, use set {n = x;}"); }
		
		while (st.nextToken() != '}') {
			st.pushBack();
			ArrayList<Integer> xList = new ArrayList<Integer>();
			char operation = 0;
			boolean semicolon = false;
			
			while (!semicolon) { // while not ';'			
				// Read in list of numbers
				ArrayList<Integer> list = new ArrayList<Integer>();
				int next;
				while ( (next = readVarByName(st)) != -1) {
					// -2 flags a bad label which should be discarded. 
					if (next != -2)	{ list.add( next ); }	
				}
			
				// First operation, can be '<' or '>'
				if (operation == 0) { 
					if (st.ttype == '<' || st.ttype == '>') { operation = (char)st.ttype;}
					else { badToken("> or <",st);}
				}
				// Operation must be the same as previous operation.
				else if (operation == '<' || operation == '>') {
					if (st.ttype == ';') {semicolon = true;}	
					else if ( st.ttype != operation) { badToken(""+operation,st); }
				}
				
				//System.out.println("xList = " + xList);
				//System.out.println("list = " + list);
				for (int i = 0; i < xList.size(); i++) {
					for (int j = 0; j < list.size(); j++) {
						int x = xList.get(i).intValue();
						int y = list.get(j).intValue();
						if (operation == '>') {
							setP(x,y,">>",tierProb);
						}
						else if (operation == '<') {
							setP(x,y,"<<",tierProb);
						}
						else {throw new RuntimeException("Unreachable code reached.");}
					}
				}
			
				xList.addAll(list);
			}
		}
	}

	
//	public double costToSwapOrder(TOM tom, int node1, int node2) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	public double costToToggleArc(TOM tom, int node1, int node2) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	public double costToToggleArcs(TOM tom, int[] node1, int[] node2) {
//		// TODO Auto-generated method stub
//		return 0;
//	}

	/** Add implied links to an arcMatrix */
	public static boolean[][] addImpliedConstraints(boolean[][] arcMatrix) {
		int n = arcMatrix.length;

		// Warshall's algorithm.
		// http://www.csse.monash.edu.au/~lloyd/tildeAlgDS/Graph/Directed/
		for (int k = 0; k < n; k++) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					arcMatrix[i][j] = arcMatrix[i][j] || arcMatrix[i][k] && arcMatrix[k][j];
				}
			}
		}
		
		return arcMatrix;
	}
	
	/** Repair TOM so all constraints are met. */
	public TOM repairTOM(TOM tom) {
		// Repair TOM attempting to retain arc directions as well as tiers.
		// If current arcs and teirs are incompatible, repairTOM is 
		// called (from within repairTOM) with useArcDirections=false
		return repairTOM(tom,true);
		//return repairTOM(tom,false);
	}
	
	/** If useArcDirections == true, arc directions in TOM are used as constraints and are
	 *  given equal weight to tiers. */
	public TOM repairTOM(TOM tom, boolean useArcDirections) {

		int n = tom.getNumNodes();
		boolean oc[][] = new boolean[n][n];  // order constraint matrix oc[i][j] == i < j
		
		// Extract order constraint matrix from temporalCost.
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				double pBefore = directPrior[i][j].pBefore();
				oc[j][i] = pBefore >= tierProb;
				oc[i][j] = (1-pBefore) >= tierProb;

				if (useArcDirections) {
					if (tom.isDirectedArc(i, j)) oc[j][i] = true;
					if (tom.isDirectedArc(j, i)) oc[i][j] = true;
				}
				
				oc[i][j] |= ((edPriorDAG != null) && edPriorDAG[i][j] &&  
							((runEDPrior && edPriorPenalty == Double.POSITIVE_INFINITY) || 
									(runKTPrior && ktPriorPenalty == Double.POSITIVE_INFINITY)) ) ;

				oc[j][i] |= ((edPriorDAG != null) && edPriorDAG[j][i] &&  
						((runEDPrior && edPriorPenalty == Double.POSITIVE_INFINITY) || 
								(runKTPrior && ktPriorPenalty == Double.POSITIVE_INFINITY)) ) ;

			}
		}
		
		// Add implied links.
		addImpliedConstraints(oc);
		
		// Find nearest valid total ordering (using KTau distance)
		int[] currentOrder = new int[n];
		for (int i = 0; i < n; i++) {currentOrder[i]=tom.nodeAt(i);}
		int[] newOrder;
		try {
			newOrder = generate(convertMatrix(oc),currentOrder);
		}
		// Network generated is cyclic.  Remove requirement that all 
		// arc directions in current network are preserved and try again.
		// If this is second try, something has gone wrong...
		catch (RuntimeException e) {
			if (useArcDirections == true) { return repairTOM(tom,false); }
			else throw e;
		}
		
		boolean arcMatrix[][] = new boolean[n][n];
		for (int i = 0; i < arcMatrix.length; i++) {
			for (int j = 0; j < arcMatrix.length; j++) {
				arcMatrix[i][j] = tom.isDirectedArc(i,j);
			}
		}
		tom.clearArcs();
		tom.setOrder(newOrder);

		// Add arcs required by constraints.
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				if ( directPrior[i][j].pArc() == 1.0 ) {
					tom.addArc(i,j);
				}
				if ((runEDPrior && edPriorPenalty == Double.POSITIVE_INFINITY ||
					 runKTPrior && ktPriorPenalty == Double.POSITIVE_INFINITY) &&
					 (edPriorDAG[j][i] || edPriorDAG[i][j])) {
					tom.addArc(i,j);
				}
			}
		}
		
		// Attempt to add all arcs previously present (some may be reversed)
		for (int i = 0; i < arcMatrix.length; i++) {
			for (int j = 0; j < arcMatrix.length; j++) {
				if (arcMatrix[i][j]) {
					if (tom.before(i,j)) { 
						if (tom.getNode(j).getNumParents() < tom.getMaxNumParents())
							{tom.addArc(i,j);}
					} 
					else if (tom.getNode(i).getNumParents() < tom.getMaxNumParents()) {
						if (!tom.isArc(i,j)){tom.addArc(i,j);}
					}
				}
			}
		}

		
		// Add/remove arcs which are constrained to not exist.
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				if (tom.isDirectedArc(i,j) && directPrior[i][j].pArcIJ() == 0) {
					tom.removeArc(i,j);
				}
				else if (tom.isDirectedArc(j,i) && directPrior[i][j].pArcJI() == 0) {
					tom.removeArc(j,i);
				} 
					
				if (tom.isDirectedArc(i,j)) {
					if ((runEDPrior && edPriorPenalty == Double.POSITIVE_INFINITY ||
						 runKTPrior && ktPriorPenalty == Double.POSITIVE_INFINITY) &&
						 !edPriorDAG[j][i]) {
						tom.removeArc(i,j);
					}
				}

				if (tom.isDirectedArc(j,i)) {
					if ((runEDPrior && edPriorPenalty == Double.POSITIVE_INFINITY ||
						 runKTPrior && ktPriorPenalty == Double.POSITIVE_INFINITY) &&
						 !edPriorDAG[i][j]) {
						tom.removeArc(j,i);
					}
				}
			}
		}
		
		return tom;
	}
	
	

	
	/** Return kendall tau (ie bubble sort) distance between two x1 and x2 <br>
	 *  x1 and x2 must both contain the same set of unique integers from (0..n-1) */
	public static int kendallTau(int[] x1, int[] x2) {
		int n = x1.length;
		// use x1[] and x2[] to create xCombined.
		// xCombined is a single list which will have the same kTau distance
		// as kTau(x1,x2).
		int[] x2Index = new int[n];
		for (int i = 0; i < n; i++) {
			x2Index[x2[i]] = i;
		}

		int[] xCombined = new int[n];
		for (int i = 0; i <= n - 1; i++) {
			xCombined[i] = x2Index[x1[i]];
		}

		// Return xTau of xCombined.
		return kTau(xCombined, xCombined.length);
	}

	/** Return the number of misordered pairs in x[]. */
	public static int kTau(int[] x, int n) {

		int split = x[0];
		int swaps = 0;
		
		int aI = 0; // index into a
		int bI = 0; // index into b
		int[] aArray = new int[n]; // all elements < split
		int[] bArray = new int[n]; // all elements >= split

		if (n <= 1) { return 0;	}  // base case.
		else {
			// Split x into [a] if x[i] < split and [b] is x[i] >= split
			for (int i = 1; i < n; i++) {
				if (x[i] < split) {
					aArray[aI] = x[i];
					// It takes bI+1 swaps to move x[i] into [a]
					swaps = swaps + bI + 1;
					aI++;
				} else {
					bArray[bI] = x[i];
					bI++;
				}
			}
		}
		// Recursively calculate swaps needed on [a] and [b]
		return kTau(aArray, aI) + kTau(bArray, bI) + swaps;
	}

	/** Use Kevin's (flawed) algorithm to generate a total ordering
	 *  consistent with the graph g that is as close as possible
	 *  to the order specified by t. <br>
	 *  
	 *  @param g: A list of tiers. ie. [[0,3],[2,1]] implies (0 and 3) are 
	 *  before (2 and 1) in the total ordering.
	 *  */
	public static int[] generate(int[][] g, int[] t) {

		int p = 0;
		int o = t.length;
		int[] tPrim = new int[o];

		for (int i = 0; i < g.length; i++) {
			int[] rArray = g[i];
			for (int k = 0; k < t.length; k++) {
				for (int l = 0; l < rArray.length; l++) {
					if (t[k] == rArray[l]) {
						tPrim[p] = t[k];
						p = p + 1;
					}
				}
			}

		}
		return tPrim;
	}

	/** Conert matrix from arcMatrix format to a list of tiers. */
	public static int[][] convertMatrix(boolean[][] arcMatrix) {
		
		int n = arcMatrix.length;

		// tier[i] stores which tier node[i] is on.
		int tier[] = new int[n];
		Arrays.fill(tier,-1);
						
		// Initialiis all nodes are unused.
		boolean used[] = new boolean[n];
		int numUsed = 0;
		
		int currentTier = 0;
		
		ArrayList<int[]> tierList = new ArrayList<int[]>();
		while (numUsed < n) {
		
			// Loop through matrix, finding all nodes with only used parents.
			for (int i = 0; i < n; i++) {
				// If node[i] hasn't been used yet.
				if (used[i] == false) {
					// Check to see if we can put node on current tier.
					boolean found = false;
					for (int j = 0; j < n; j++) {
						// An arc j->i exists, and j hasn't been used yet.
						// so i cannot be on this tier.
						if (arcMatrix[i][j] && used[j] == false) {
							found = true; break;
						}
					}
					// i is on this tier.
					if (!found) {tier[i] = currentTier;}
				}
			}
			
			// Count how many nodes on current tier.
			int tierSize = 0;
			for (int i = 0; i < n; i++) { 
				if (tier[i] == currentTier) {tierSize++;}
			}
		
			if (tierSize == 0) {
				throw new RuntimeException("Conversion to tiers failed: Cyclic network?");
			}
			
			// Create 'thisTier' storing all nodes on the current tier.
			int[] thisTier = new int[tierSize];
			int ii = 0;
			for (int i = 0; i < n; i++) {
				if (tier[i] == currentTier) {
					thisTier[ii++] = i;
					used[i] = true;
					numUsed++;
				}
			}
			tierList.add(thisTier);
			currentTier++;
		}
		
		
		
		// Extract tiers from tierList.
		int[][] tiers = new int[tierList.size()][];
		for (int i = 0; i < tiers.length; i++) {
			tiers[i] = tierList.get(i);
		}

		return tiers;
	}	
	
	/** "Optimised" version of costToToggleArc. */
	public double costToToggleArc( TOM tom, int node1, int node2 ) {
		if (runEDPrior || runKTPrior ) { 
			return super.costToToggleArc(tom,node1,node2); 
		}
		
		int n = tom.getNumNodes();
		int min = node1, max = node2;
		if (min > max) { int temp = min; min = max; max = temp;}
		
		
		////////////// Calc oldCost ///////////////
		double directCost = 0; // Cost to state direct relationships.
		double indirectCost = 0; // Cost to state indirect relationships.
		if ( runDirectRelationPrior || runDefaultPrior) {
			directCost = directPrior[max][min].relationCost(tom);
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {
				if ( runIndirectRelationPrior ) {
					IndirectRelationPrior p2 = indirectPrior[i][j];
					if (p2.priorSet ) {
						double x = p2.relationCost(tom);
						indirectCost += x;
					}
				}
			}
		}

		double oldCost =  directCost + indirectCost;
		///////////////////////////////////////////
		
		// toggle arc.
		if ( tom.isArc(node1,node2) ) { tom.removeArc(node1,node2); }
		else { tom.addArc( node1, node2 ); }

		////////////// Calc newCost ///////////////
		directCost = 0; // Cost to state direct relationships.
		indirectCost = 0; // Cost to state indirect relationships.
		if ( runDirectRelationPrior || runDefaultPrior) {
			directCost = directPrior[max][min].relationCost(tom);
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < i; j++) {				
				if ( runIndirectRelationPrior ) {
					IndirectRelationPrior p2 = indirectPrior[i][j];
					if (p2.priorSet ) {
						double x = p2.relationCost(tom);
						indirectCost += x;
					}
				}
			}
		}

		double newCost =  directCost + indirectCost;
		///////////////////////////////////////////

		// untoggle arc.
		if ( tom.isArc(node1,node2) ) { tom.removeArc(node1,node2); }
		else { tom.addArc( node1, node2 ); }
		
		return newCost - oldCost;
	}

}
		

