//
// Functions to learn BNet's from raw data.
//
// Copyright (C) 2002 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: BNetLearner.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.bNet;

import cdms.core.*;
import camml.core.models.*;
import camml.core.search.*;

import camml.core.library.WallaceRandom;
import camml.core.library.BlockingSearch;
import camml.core.models.cpt.CPTLearner;

/** Class able to perform Metropolis, Mixture and Anneal Searches and handle options. */
public class BNetLearner extends ModelLearner.DefaultImplementation
{   
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1626619938392043901L;

	/** Static instance of MakeBNetLearner */
	public static final MakeBNetLearner creator = new MakeBNetLearner();

	/** Static instance of metropolis search. */
	public static BNetLearner metropolis = 
		(BNetLearner)creator._apply( new String[] {"searchType"}, 
									 new Value[] {new Value.Str("Metropolis")} );

	/** Static instance of metropolis search using mixture models. */
	public static BNetLearner mixMetropolis = 
		(BNetLearner)creator._apply( new String[] {"searchType", "mix"}, 
									 new Value[] {new Value.Str("Metropolis"), Value.TRUE} );

	/** Static instance of Anneal search. */
	public static BNetLearner anneal = 
		(BNetLearner)creator._apply( new String[] {"searchType"}, 
									 new Value[] {new Value.Str("Anneal")} );



	

	/** return "BNetLearner" */
	public String getName() { return "BNetLearner"; }    
	
	/** Maximum Likelyhood estimator used */
	final ModelLearner mlLearner;
	
	/** MML estimator used */
	final ModelLearner mmlLearner;
	
	/** Should a mixture model be returned?	 */
	final boolean mix;
	
	/** Parameterize() returns tree of results, metropolis debug only. */
	final boolean fullResults;
	
	/** Metropolis or Anneal */
	String searchType;
	public String getSearchType() { return searchType; }
	public void setSearchType( String searchType) {
		this.searchType = searchType;
	}
	
	
	/** Options passed to search during parameterize step */
	protected String[] options = new String[0];
	
	/** Options passed to search during parameterize step */
	protected Value[] optionVal = new Value[0];
	
	/** Pass in extra options to be passed to BNetSearch */
	public void setOptions( String[] options, Value[] optionVal ) {	
		if ( options.length != optionVal.length ) {
			throw new RuntimeException("Option length mismatch in BNetLearner.setOptions()");
		}
		
		this.options = options;
		this.optionVal = optionVal;	
	}
	
	/** Constuctor currently only specifies Type.MODEL, this needs to be fixed. */
	public BNetLearner( ModelLearner mlLearner, ModelLearner mmlLearner, 
			boolean mix, boolean fullResults )
	{
		super( Type.MODEL, Type.TRIV );
		
		// Set default values.
		this.mmlLearner = mmlLearner;
		this.mlLearner = mlLearner;
		this.mix = mix;
		this.fullResults = fullResults;
		
		this.searchType = "Metropolis";
	}
	
	
	
	/** Parameterize and return (m,s,y) */
	public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
	throws LearnerException
	{
		
		if ( x.length() != z.length() ) {
			throw new RuntimeException("Length mismatch in RodoCammlLearner.parameterize");
		}
		
		WallaceRandom rand = new WallaceRandom( new int[] {377777, -888} );
		
		// Run  Search
		final BNetSearch s;
		if ( searchType.equals("Metropolis") ) {
			s = new MetropolisSearch( rand, x, mlLearner, mmlLearner );
		}
		else if ( searchType.equals("Anneal") ) {
			s = new AnnealSearch( rand, x, mlLearner, mmlLearner ) ;
		}
		else if ( searchType.equals("FixedStructure") ) {
			s = new FixedStructureSearch( rand, x, mlLearner, mmlLearner );
		}
		else { throw new RuntimeException("Unknown search type: " + searchType); }
		
		// Initialise extra options
		for ( int i = 0; i < options.length; i++ ) {
			System.out.println( "Setting : "+options[i] + " = " + optionVal[i] );
			boolean res = s.setOption( options[i], optionVal[i] );
			if ( res == false ) { throw new RuntimeException("Invalid option: " + options[i]);}
		}
		
		// Run the search.
		new BlockingSearch ( s ).start();
		
		if ( fullResults == true ) { 
			Value.Model tomModel = s.getBNet();
			Value tomParams = s.getBestParams( mmlLearner );
			Value stats = tomModel.getSufficient(x,z);
			Value fullResults = ((MetropolisSearch)s).getResults();
			return new Value.DefStructured( new Value[] {tomModel,stats,tomParams, fullResults} );
		}
		
		// For a single model
		if ( mix == false ) {
			Value.Model tomModel = s.getBNet();
			Value tomParams = s.getBestParams( mmlLearner );
			Value stats = tomModel.getSufficient(x,z);	    	    
			return new Value.DefStructured( new Value[] {tomModel,stats,tomParams} );
		}
		else {
			if ( s instanceof MetropolisSearch ) {
				Value.Structured my = ((MetropolisSearch)s).getMixResults( );
				return new Value.DefStructured(new Value[] {my.cmpnt(0), Value.TRIV ,my.cmpnt(1)});
			}
			else { throw new RuntimeException("mix option only available on metropolis search"); }
		}
	}
	
	
	/** Parameterize and return (m,s,y) */
	public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
	{
		throw new RuntimeException("Not implemented");
	}
	
	/** Parameterize and return (m,s,y) */
	public Value.Structured sParameterize( Value.Model model, Value s )
	throws LearnerException
	{	
		return parameterize( Value.TRIV, 
				(Value.Vector)((Value.Structured)s).cmpnt(0), 
				(Value.Vector)((Value.Structured)s).cmpnt(1) );
	}
	
	
	
	
	/**
	 * return cost.  This is read directly out of parameters.  Ideally it should be calculated
	 * using parameters and data as currently it entirely ignores data.
	 */
	public double sCost(Value.Model m, Value s, Value y)
	{	
		throw new RuntimeException("Not implemented");
	} 
	
	
	public String toString() { return "MetropolisLearner"; }    
	
	
	
	/** Default implementation of makeBNetLearner */
	public static final MakeBNetLearner makeBNetLearner = new MakeBNetLearner();

	/** 
	 * [(Str,Value)] -> (BNetLearner Structure) <p> 
	 * 
	 * Creates a {@link BNetLearner} given a vector of options.  Each element in the options
	 * vector contains a structure of ("OptionName",OptionValue) where OptionName mush
	 * be a Value.String, and OptionValue is an appropriate value for that option. <p>
	 * 
	 *  Possible options include: <br>
	 *	<b>mlLearner</b>  -- Used to create maximum likelyhood scores for SEC testing.
	 *				  		default {@link CPTLearner#mlMultinomialCPTLearner} <br>
	 *	<b>mmlLearner</b> -- Learner used to represent local structure. 
	 *				  		Default {@link CPTLearner#mmlAdaptiveCPTLearner} <br>
	 *	<b>mix</b>        -- Should a mixture model be returned? <br>
	 *	<b>searchType</b> -- "Metropolis" or "Anneal" <br>
	 *  <b>fullResults</b>-- Instead of returning a (m,s,y) structure a (m,s,y,f) structure
	 *  			  is returned where f is a full heirachy of MMLECs, SECs & DAGs along
	 *  			  with values for MML, ML, posterior, relative prior, etc. <br>
	 *  <p>   
	 *  For further options see {@link BNetSearch#setOption(String, Value)}
	 *  */
	public static class MakeBNetLearner extends MakeModelLearner
	{
		/** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -5089046930481996467L;

		public MakeBNetLearner( ) { }
		
		/** Shortcut apply method */
		public ModelLearner _apply( String[] option, Value[] optionVal ) {  
			
			// Set default values.
			ModelLearner mlLearner = CPTLearner.mlMultinomialCPTLearner;
			ModelLearner mmlLearner = CPTLearner.mmlAdaptiveCPTLearner;
			boolean mix = false;	    
			boolean fullResults = false;
			
			String searchType = "Metropolis";
			//double arcProb = -1;
			
			// Search options for overrides.
			for ( int i = 0; i < option.length; i++ ) {
				// If none of the options listed are valid, validOption = false.
				boolean validOption = true;
				if ( option[i].equals("mlLearner") ) {
					mlLearner = ((FunctionStruct)optionVal[i]).getLearner();
				}
				else if ( option[i].equals("mmlLearner") ) {
					mmlLearner = ((FunctionStruct)optionVal[i]).getLearner();
				}
				else if ( option[i].equals("mix") ) {
					mix = (((Value.Discrete)optionVal[i]).getDiscrete() == 0);
				}
				else if ( option[i].equals("searchType") ) {
					searchType = ((Value.Str)optionVal[i]).getString();
				}
				else if ( option[i].equals("fullResults") ) {
					fullResults = (((Value.Discrete)optionVal[i]).getDiscrete() == 0);
				}
				else { validOption = false; }
				
				// Remove the current option from the list of options.
				// All remaining options are passed to the search object later.
				if ( validOption == true ) {
					option = remove(option,i);
					optionVal = remove(optionVal,i);
					i--;
				}
			}
			
			// Initialise a BNetLearner with all options specified.
			BNetLearner learner = new BNetLearner( mlLearner, mmlLearner, mix, fullResults );
			learner.setSearchType( searchType );	    
			learner.setOptions( option, optionVal );
			
			return learner;
		}
		
		public String[] getOptions() { return new String[] {
				"mlLearner  -- Used to create maximum likelyhood scores for SEC testing",
				"mmlLearner -- Learner used for each subModel.  Default is AdaptiveCode",
				"mix        -- Should a mixture model be returned?",
				"searchType -- \"Metropolis\" or \"Anneal\"",
				" Extra options may be passed to Metropolis and Anneal searches including.",
				"arcProb    -- Fix the probability of arc existance at a value",
				"temperature -- Fix temperature at a set value",
				"currentTOM  -- Set initial TOM to a given value",
				"clean       -- Should model cleaning be used?"
		}; }
	}
	
}
