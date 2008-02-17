{
	Rodney O'Donnell
	Basis of CaMML Scripting language.
	Last Modified 9-3-05

	Working Functions : 
	Set, Show, Load, Alias, Exit, Run

	Skeleton Functions : 
	Sample, Search, Print, Read, Write, Save, Clean, Parameterize, Help, Log

	Not working:
	Clear

	Things to do:
	Java functions to store ArcProb, Search, Tiers, etc. more conveniently
	Make on/off boolean type
	
	notes : 
	- To make sure a function is evaluated lazily
	  - give it a parameter, eg.  x = lambda a . print x
	- All functions callable by the user should start with a capital letter.
	- All functions MUST evaluate, lazyness may cause problems.	
	- clear doesn't work.

	Formatted to 80 lines.
	901234567890123456789012345678901234567890123456789012345678901234567890
}


let 
	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ ------------------- Install Modules -------------------------------- }
	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ These functions are needed to set up the environment before other    }
	{ commands may be run.                                                 }
	{								       								   }
	{ cv "x" creates a new value from the java class specified in "x".     }
	{ In this case a function to install modules is created and saved as   }
	{ install.  Using this install function several CDMS modules are       }
	{ installed, each containing a set of useful functions and values.     }
	{ -------------------------------------------------------------------- }
 
	install = cv "cdms.core.Module$InstallModule",
	FNConfig = install ("cdms.core.FN",()),
	VectorConfig = install ("cdms.core.VectorFN",()),
	CammlPluginConfig = install ("camml.plugin.scripter.CammlPlugin", ()),


	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ -------------------- Alias Commands -------------------------------- }
	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ Alias commands provide an easy alternative to using full names of    }
	{ functions.  This is especially true when they are added from an      }
	{ environment such as CammlPlugin.                                     }
	{ -------------------------------------------------------------------- }

	printString = CammlPlugin.printString, 
	quitValue = CammlPlugin.quitValue,
	nullValue = CammlPlugin.nullValue,
	loadData = CammlPlugin.loadCammlFile,
	cammlAdd = CammlPlugin.cammlAdd,
	cammlSet = CammlPlugin.cammlSet,
	cammlGet = CammlPlugin.cammlGet,
	runCammlScript = CammlPlugin.runCammlScript,


	
	{ This should really be a type... }
	on = true,
	off = false,

	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ -------------------- Environment ----------------------------------- }
	{ ------------- (ie. Declaring Variables ) --------------------------- }
	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ Environment contains all the constants and variables used in the     }
	{ language.  Variable names are preceeded with var_ This was necesarry }
	{ to implement the cammlSet and cammlGet funcitons for saving variables}
	{ in what is otherwise a language with no concept of variables.        }
	{ The strings associated with each variable may be thought of as a     }
	{ pointer to the values.  The operators cammlSet and cammlGet operate  }
	{ on these pointers.                                                   }
	{ -------------------------------------------------------------------- } 

	{ arcProb is a list of prior probabilities on nodes in the network.
	  Each link is in a list with form (i,j,p) which means the probability
	  of i and j being connected is p.  Currently a list is being used, 
	  this may be replaced by a different (possibly matrix) representation
	  at some stage.  }
	var_arcProb = [], 		   { Format (i,j,p). p = prob i->j}	
	arcProb = "var_arcProb", 
	
	var_constraintProb = 0,		
	constraintProb = "var_constraintProb",	
	
	var_mutateProb = [0.33,0.33,0.34], { (p(flip), p(doubleFile), p(swap) }
	mutateProb = "var_mutateProb",

	var_metric = "MML",         
	metric = "var_metric",	
	
	var_temperature = 1,         { Search Temperature              }
	temperature = "var_temperature",

	var_tier = [(0,[])],         { Format (tier#, [nodes on tier]) }
	tier = "var_tier",	
	

	var_nodeName = [""],	     { List of node names }
	nodeName = "var_nodeName", 

	var_case = [("",[])],        { Format [("name", [(data)])] }
	case = "var_case",
	
	var_sample = [],
	sample = "var_sample", 

	var_search = [],             { Format [("name",time] }
	search = "var_search",	
	


	{ -- General housekeeping variables -- }
	var_autoSave = 0,
	autosave = "var_autoSave", 

	var_directory = ".",
	directory = "var_directory",

	{ What is printed at different verbosity levels?                     }
	{ Verbosity > 5 = Print Stack Traces of Errors                       }
	{ Verbosity > 4 = Print Jave Errors (no Trace)                       }
	{ Verbosity > 2 = Print "Camml >" Prompt                             }
	{ Verbosity > 1 = Print on Errors (Description, not java.Exception.? }	
	var_verbosity = 5,
	verbosity = "var_verbosity", 

	var_warnings = on,
	warnings = "var_warnings",
	
	var_logFile = (on, "camml.log"),
	logFile = "var_logFile",
	
	
		

	{ all is required for functions such as Show all }
	var_all = [ arcProb, constraintProb, mutateProb, metric, temperature ],
	var_allName = ["arc Prob", "constraint Prob", "Mutation Prob", "Metric", "Temperature"],
	all = "var_all",
	

	{ Alias used by List function. }
	searches = "list_searches",
	cases = "list_cases",
	models = "list_models",
	nodes = "list_nodes",
	samples = "list_samples",

	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ ------------------    User Functions.         ---------------------- }
	{ -------------------------------------------------------------------- }
	{ -------------------------------------------------------------------- }
	{ Functions callable by the user of the language.  Functions originally}
	{ defined in CaMML functional Spec jan 30, 2002.                       }
	{ -------------------------------------------------------------------- }
	

	{ ---------------------------- Set ----------------------------------- }
	{ Set a variable to a new value.                                       }
	{ Set takes 2 (or more) parameters.  This first value is always a      }
	{ string identifying the variable to be modified.  The second value is }
	{ the new value for that variable.  If the variable to be modified is  }
	{ either var_arcProb or var_tiers then further arguments may be used.  }
	{								       								   }
	{ arcProb and tiers are special cases as they are more complex than    }
	{ many variables, and they are used frequently so shortcuts are        }
	{ desirable.  Setting arcProb or tiers adds a new entry to a vector    }
	{ containing all previously set values.  Each new value is simply      }
	{ appended to the vector. This is shown below as [a,b]= concat a with b}
	{                                                                      }
	{ Example.                                                             }
	{ Set metric "MML"         : var_metric = "MML"                        }
	{ Set arcProb 1 2 0.3      : var_arcProb = [var_arcProb,(1,2,0.3)]     }
	{ Set tier 3 [1,2,3]       : var_ties = [var_tier, (3,[2,3,4])]        }
	{ -------------------------------------------------------------------- }

	insertTier = lambda oldTier . lambda newTier . 
		cammlSet tier (concat [oldTier,newTier]),
	setArc = lambda i . lambda j . lambda p . 
		cammlSet arcProb  (concat [ cammlGet arcProb, [(i,j,p)]]),
	setTier = lambda N . lambda list . 
		insertTier (cammlGet tier) [(N,list)],

	Set = lambda x . 
		if (x = arcProb) then setArc else 
		if (x = tier) then setTier else
		(cammlSet x),

	{ ---------------------------- Show ---------------------------------- }
	{ Show shows a single or a group of environment variables.             }
	{ Show takes one parameter, either the name of a variable or "var_all" }
	{ If passed a single variable the variable's value is printed, if      }
	{ "var_all" is passed, a list of common variables is printed.          }
	{                                                                      }
	{ Example:                                                             }
	{ Show arcProb		: Print out the value of arcProb               }
	{ Show all              : Print commonly used variables.               }
	{ -------------------------------------------------------------------- }
	Show = lambda x . 
		if (x = "var_all") then 
			map (lambda y . printString (sconcat [y," = ",
			 toString(cammlGet y)])) (cammlGet x)
		else printString (toString (cammlGet x)), 	


	{ --------------------------- Clear ---------------------------------- }
	{ Clear is not yet working                                             }
	{ -------------------------------------------------------------------- }
	Clear = lambda name . printString "Clear not yet implemented",
{
	Clear = lambda name . 
	let 
		
		single 	= lambda i . if (hd (cmpnt i oldEnv ) = name )
				then (cmpnt i defaultEnv ) 
				else (cmpnt i oldEnv ),
		mapSingle = mapT single (iota( numVariables ) ),
		newEnv	= if ( name = "all" ) then defaultEnv else (mapSingle)	

	in newEnv,		
}

	{ --------------------------- Sample --------------------------------- }
	{ Sample samples from the active model.  This is stored in environment }
	{ -------------------------------------------------------------------- }
	Sample = lambda n . 
			cammlSet sample "--Sample",

	{ --------------------------- Break ---------------------------------- }
	{ Breaks search i.  If only one search, then i not needed.             }
	{ -------------------------------------------------------------------- }
	Break = lambda N . 
		printString (sconcat ["Break Search ", 
		 toString(elt (cammlGet search) (N-1) )] ),



	{ --------------------------- List ----------------------------------- }
	{ Lists all searches,cases,models, or nodes                            }
	{ -------------------------------------------------------------------- }
	List = lambda name . 
		if (name = searches) then 
			( printString "Searches = ?" )
		else if (name = cases) then 
			( printString "Cases = ?" )
		else if (name = models) then 
			( printString "Models = ?" )	
		else if (name = nodes) then 
			( printString "Nodes = ?" )
		else if (name = samples) then 
			( printString "Sample = ?" )
		else printString 
			(sconcat ["Unknown list : ", name]),
	

	{ --------------------------- Search --------------------------------- }
	{ Starts a search.                                                     }
	{ For each search we increment searchIndex so each has a unique ID.    }
	{ -------------------------------------------------------------------- }
	searchIndex = 0,
	startNewSearch = lambda searchName . lambda N . 
		let xx=cammlSet "searchIndex" (searchIndex+1) in  {Increment index.}
		(searchName, searchIndex-1, N),
	Search = lambda N . 
		cammlSet search (concat[ cammlGet search, [startNewSearch (sconcat ["search ",searchIndex]) N] ]),

	{ --------------------------- Print ---------------------------------- }
	{ Print out model, MMLEC SEC etc.                                      }
	{ ccm = model | MMLEC | SEC      (ccm = Class of Causal Model)         }
	{ selection = current | m | posterior                                  }
	{ params = parameters                                                  }
	{ arc = arcProb                                                        }
	{ stats = stats                                                        }
	{ -------------------------------------------------------------------- }

	Print = lambda ccm . lambda selection . lambda params . lambda arc . 
		lambda stats .
	let
		printStats  = sconcat[ccm," ",selection," ",
				      params," ",arc," ",stats],
		printArc    = printStats, 
		printParams = printArc,

		printM         = printParams,
		printCurrent   = printParams ,
		printPosterior = sconcat[ccm," Posterior "],

		printSelection = 
			if (selection = "m") then 
				printM 
			else if (selection = "current") then 
				printCurrent
			else if (selection = "posterior") then 
				printPosterior
			else 
				(printString "Invalid option : selection"),



		printModel = printSelection,
		printMMLEC = printSelection,
		printSEC   = printSelection,
		
		printCCM = 
			if (ccm = "model") then 
				printModel 
			else if (ccm = "MMLEC") then 
				printMMLEC 
			else if (ccm = "SEC") then 
				printSEC 
			else 
				"Invalid option : Class of Causal Model "

	in printString printCCM,


	{ --------------------------- Read ----------------------------------- }
	{ Read in a model.                                                     }
	{ -------------------------------------------------------------------- }
	Read = lambda x . lambda file . 
		printStr "Read Function not implemented.", 


	{ --------------------------- Write ---------------------------------- }
	{ Write out a model.                                                   }
	{ -------------------------------------------------------------------- }
	Write = lambda x . lambda file . 
		printStr "Write Function not implemented.", 

	{ --------------------------- Load ----------------------------------- }
	{ Load a set of cases.                                                 }
	{ -------------------------------------------------------------------- }
	Load = lambda file . 
		cammlSet case (concat [[(file,loadData file)],cammlGet case]),


 

	{ --------------------------- Save ----------------------------------- }
	{ Save a set of cases.                                                 }
	{ -------------------------------------------------------------------- }
	Save = lambda file . 
		printStr "Save Function not implemented.", 

	{ --------------------------- Clean ---------------------------------- }
	{ Clean model.  Remove insignificant arcs, etc.                        }
	{ -------------------------------------------------------------------- }
	Clean = lambda x . 
		printStr "Clean Function not implemented.", 

	{ ---------------------- Parameterize -------------------------------- }
	{ Parameterize a model.                                                }
	{ -------------------------------------------------------------------- }
	Parameterize = lambda x . 
		printStr "Parameterize Function not implemented.", 



	{ ------------------------ Alias  ------------------------------------ }
	{ Make an alias for a command. eg. alias "sh" show                     }
	{ -------------------------------------------------------------------- }
	Alias = lambda name . lambda cmd . 
		cammlAdd name cmd,
	

	{ --------------------------- Quit ----------------------------------- }
	{ Quit the program. Returns quitValue which flags the program to exit. }
	{ Alias : q, exit                                                      }
	{ -------------------------------------------------------------------- }
	Quit = quitValue,
	quit = Quit, q = quit, Q = quit, Exit = quit, exit = quit,

	{ --------------------------- Help ----------------------------------- }
	{ Gives Help on everything.  Alias is h                                }
	{ -------------------------------------------------------------------- }
	Help = sconcat
	["Help not yet fully implemented.", newline,
	 "common commands :                         ", newline,
	 "Set x y    -> x = y                       ", newline,
	 "Show x     -> prints x                    ", newline,
	 "Show all   -> prints important variables  ", newline,
	 "Quit       -> quit program                ", newline
	],

	h = Help, help = Help,   

	{ --------------------------- Run ------------------------------------ }
	{ Quit the program. Returns quitValue which flags the program to exit. }
	{ Alias : q, exit                                                      }
	{ -------------------------------------------------------------------- }
	Run = runCammlScript,

	{ --------------------------- Log ------------------------------------ }
	{ Turn Log on/off and set file name                                    }
	{ -------------------------------------------------------------------- }

	Log =  lambda onOff . 
		if (onOff = on) then 
			(lambda filename . CammlPlugin.startLog filename) 
		else 
			{needs extra parameter so it is not lazy}
			( CammlPlugin.stopLog "") 

in
	true    












