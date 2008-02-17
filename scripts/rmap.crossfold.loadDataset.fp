{ This script loads a network and systematically varies the size of the training date    }
{ CPT,DTree,Dual,rodo,F-CPT,F-DTree are all compared and averegr results taken.          }
let 

	Bootstrap = let
		{ -------------------- BOOTSTRAP --------------------- }
	  	{ Install Modules }
	  	install = cv "cdms.core.Module$InstallModule",
	  	FNConfig = install ("cdms.core.FN",()),
	  	VectorConfig = install ("cdms.core.VectorFN",()),
	   	ModelFNConfig = install ("cdms.core.ModelFN",()),
	  	LatexConfig = install ("cdms.plugin.latex.Latex",()),
	
		MMLConfig = install ("cdms.plugin.mml87.Mml87",()),	
		ModelConfig = install ("cdms.plugin.model.Model",()),
		C5Config = install ("cdms.plugin.c5.C5",()),

	  	CammlSearchPackage = install ("camml.core.search.SearchPackage",()),
	  	CammlModels = install ("camml.core.models.Models",()),
	  	{DTreeLearner = install ("camml.models.DTreeLearner", ()),}
	  	NeticeFN = install ("camml.plugin.netica.Netica", ()),
	  	{ FriedmanLearner = install ("camml.plugin.friedman.FriedmanLearner", ()),}
	  	FriedmanWrapper = install ("camml.plugin.friedman.FriedmanWrapper", ()),
	  	Weka = install ("camml.plugin.weka.Weka", ()),
		{RodoLearner = install ("camml.plugin.rodoCamml.RodoCammlLearner",()),}
		LibraryConfig = install ("camml.core.library.Library", ()),

		RMI = install ("camml.plugin.rmi.CDMSEngine$EngineModule",()),

	  	{ Add some useful functions to the environment. }
	  	f0 = addToEnvironment (lambda x . cmpnt 0 (elt x 0), 
				"extract", "Extract a single value from a vector of structures")
	  	{ ---------------- END BOOTSTRAP --------------------- }
	in (),


	rmap = RMI.rmap [ 
			"rmi://node01:1099/CalculatorService",
			"rmi://node02:1099/CalculatorService",
			"rmi://node03:1099/CalculatorService",
			"rmi://node04:1099/CalculatorService",
			"rmi://node05:1099/CalculatorService",
			"rmi://node06:1099/CalculatorService",
			"rmi://node07:1099/CalculatorService",
			"rmi://node08:1099/CalculatorService",
			"rmi://node09:1099/CalculatorService",
			{"rmi://node10:1099/CalculatorService",}
			"rmi://node11:1099/CalculatorService",
			"rmi://node12:1099/CalculatorService",
			"rmi://node13:1099/CalculatorService",
			"rmi://node14:1099/CalculatorService",
			"rmi://node15:1099/CalculatorService",
			"rmi://node16:1099/CalculatorService",
			"rmi://node17:1099/CalculatorService",
			"rmi://node18:1099/CalculatorService",
			"rmi://node19:1099/CalculatorService",
			"rmi://node20:1099/CalculatorService",
			"rmi://node21:1099/CalculatorService",
			"rmi://node22:1099/CalculatorService",
			"rmi://node23:1099/CalculatorService",
			"rmi://node24:1099/CalculatorService",
			"rmi://node25:1099/CalculatorService",
			"rmi://node26:1099/CalculatorService",
			"rmi://node27:1099/CalculatorService",
			"rmi://node28:1099/CalculatorService",
			"rmi://node29:1099/CalculatorService",
			"rmi://node30:1099/CalculatorService",
			"rmi://node31:1099/CalculatorService",
			"rmi://node32:1099/CalculatorService",
			"rmi://node33:1099/CalculatorService",
			"rmi://node34:1099/CalculatorService",
			"rmi://node35:1099/CalculatorService",
			"rmi://node36:1099/CalculatorService",
			"rmi://node37:1099/CalculatorService",
			"rmi://node38:1099/CalculatorService",
			"rmi://node39:1099/CalculatorService",
			"rmi://node40:1099/CalculatorService",
			"rmi://node41:1099/CalculatorService",
			"rmi://node42:1099/CalculatorService"
			 ],


	{ return n vectors, each containing every n'th item in list.  length list must be a multiple of x.  This us used to undo combineList. }
	getEveryNth = lambda list . lambda n . map (lambda x .  map ( lambda y . elt list (x+y*n) ) (iota ((length list)/n)) ) (iota n),

	{ splits vector into n groups.  the first length/n elements are returned as the first element of the vector, etc. }
	getGroupsOfN = lambda list . lambda n . map (lambda x .  map ( lambda y . elt list (y+x*((length list)/n)) ) (iota ((length list)/n)) ) (iota n),


	{ Generate Mean and standard deviation from a vector of data. }
	getStats = lambda x . let
		estimate = MML87.normalEstimator 0.00001 (getSufficient Model.normal x []),
		mu = cmpnt 0 estimate,		{ Estimate of Mean. }
		sd = cmpnt 1 estimate,		{ Estimate of Standard Deviation. }
		se = sd / sqrt folds		{ Estimate of Standard Error. }
	in ("mean = ",mu, "sd = ",sd, "se = ",se),


	{ Format takes a vector and returns a string containing each element on a new line. }
	format = lambda x . sconcat (concat ( map (lambda y . [y,newline]) x)),


	{ CombineList takes a list of lists [[]] and returns a list containing each combination from
	  the previous list }
	{ eg. combineList [[1,2,3],[a,b]] -> [[1,a],[1,b],[2,a],[2,b],[3,a],[3,b]]      }
	combineList = let
		combine = lambda x . lambda y . 
			concat (map ( lambda xx . map (lambda yy . concat [xx,[yy]]) y) x)
	in foldl combine [[]],

	{ Transpose matrix [i][j] -> [j][i] }
	transpose = lambda vec . map 
				(lambda i . map
					(lambda j . elt (elt vec j) i)					
				(iota (length vec))) 
			(iota (length (elt vec 0))),

	{ Find the minimum in a vector [continuous] -> continuous }
	min = foldl (lambda a . lambda b . if (a < b) then a else b) 99999999.9,


	{ Using a given arity, arcProb and leafProb create a random network }
	makeNet = lambda arity . lambda arcProb . lambda leafProb . 
		elt (generate CammlModels.superBNet 1234 (arity,arcProb,leafProb) () folds) 0,

	{ ([arity],arcProb,leafProb) -> (String,(model,params))     }
	makeNetStruct = lambda struct . let
		arity = cmpnt 0 struct,
		arcProb = cmpnt 1 struct,
		leafProb = cmpnt 2 struct, 
		net = makeNet arity arcProb leafProb,
		name = sconcat [ arity," arcProb = ", arcProb, " leafProb = ", leafProb ]
	in ( name, net ),



	{ Due to a bug in rmap, all functions in the global environment must be redeclared locally}
	map = map, iota = iota, cmpnt = cmpnt, elt = elt, generate = generate, 
	CammlModels.superBNet = CammlModels.superBNet, 	xValidation = xValidation, 
	Netica.convertToNeticaNet = Netica.convertToNeticaNet, logP = logP, length = length,
	Library.println = Library.println, Weka.loadMissingDiscretize = Weka.loadMissingDiscretize,
	CammlModels.maskMissing = CammlModels.maskMissing, C5.loadC5file = C5.loadC5file,

	{ --------------------------------------------------------------------------------------- }
	{                   Initialisation complete                                               }
	{ --------------------------------------------------------------------------------------- }



	{ --------------------------------------------------------------------------------------- }
	{                   Parameter Declaration                                                 }
	{ --------------------------------------------------------------------------------------- }


	numTestSamples = 100000,
	folds = 5,

	{ localParams are proportion to be used as training data. }
	localParamArray =  [ 0.9 ],	

	{ globalParams are of the form :  [ "filename"] }
	globalParamArray = [
				"/u/cluster2/postg/rodo/Data/c5/centralNervousSystem_outcome_filtered_disc.data"
 		],

	mappingOver = "crossvalidation seeds.",

	{ --------------------------------------------------------------------------------------- }
	{ makeTrainingData using (arity,arcProb,leafProb) as global and mapping over [numSamples] }
	{ return ( [x], [z] )                                                                     }
	{ --------------------------------------------------------------------------------------- }
	makeTrainData = lambda globalParams . lambda localParams . lambda fold . let
		fileName = globalParams,
		fullData = C5.loadC5file (fileName,()),
		trainProb = localParams,
		xData = xValidation folds trainProb fullData "aa",

		{ Extract appropriate portion of data }
		dataX = elt (cmpnt 0 xData) fold,
		dataZ = map (lambda x . ()) (iota (length dataX))
	in ( dataX, dataZ ),


 	{ -- Return a set of data appropriate for testing                                      -- }
	makeTestData = lambda globalParams . lambda localParams . lambda fold . let
		fileName = globalParams,
		fullData = C5.loadC5file (fileName,()),
		trainProb = localParams,
		xData = xValidation folds trainProb fullData "aa",

		{ Extract appropriate portion of data }
		{dataX = elt (cmpnt 1 xData) fold,}
		{dataZ = map (lambda x . ()) (iota (length dataX))}
		dataX = CammlModels.maskMissing (elt (cmpnt 1 xData) fold, []),
		dataZ = CammlModels.maskMissing ( dataX, [74] )
	in ( dataX, dataZ ),

	{ -- If a true model exists, return it in the form (m,y)                               -- }
	{ -- If no model is found, return a structure with length != 2 (nasty hack required..) -- }
	makeTrueModel = lambda globalParams . lambda localParams . lambda fold . ( (), (), () ),
		
	{ Funciton to give each model a name, could be network name, param list, etc.}
	makeModelName = lambda globalParams . globalParams,



	{ --------------------------------------------------------------------------------------- }
	{ Create search functions.                                                                }
	{ --------------------------------------------------------------------------------------- }
	makeBNetLearner = lambda options . cmpnt 0 (CammlSearchPackage.makeLearnerStruct options),
	
	makeCPTLearner = lambda multinomialOptions . let
		multiLearner = CammlModels.makeAdaptiveCodeLearner multinomialOptions
	in CammlModels.makeCPTLearner [("leafLearner",multiLearner)],

	makeDTreeLearner = lambda multinomialOptions . let
		multiLearner = CammlModels.makeAdaptiveCodeLearner multinomialOptions
	in CammlModels.makeForcedSplitDTreeLearner [("leafLearner",multiLearner)],

	cptLearnerMML5 = makeCPTLearner [("bias",0.5),("useMML",true)],
	cptLearner5 = makeCPTLearner [("bias",0.5),("useMML",false)],
	cptLearnerMML1 = makeCPTLearner [("bias",1.0),("useMML",true)],
	cptLearner1 = makeCPTLearner [("bias",1.0),("useMML",false)],

	dTreeLearnerMML5 = makeDTreeLearner [("bias",0.5),("useMML",true)],
	dTreeLearner5 = makeDTreeLearner [("bias",0.5),("useMML",false)],
	dTreeLearnerMML1 = makeDTreeLearner [("bias",1.0),("useMML",true)],
	dTreeLearner1 = makeDTreeLearner [("bias",1.0),("useMML",false)],

	{ List of searches to be run. }
	search = [
 			makeBNetLearner [ ("mmlLearner",cptLearnerMML5)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearner5)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearnerMML1)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearner1)  ]

			{
 			makeBNetLearner [ ("mmlLearner",cptLearnerMML5)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearner5)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearnerMML1)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearner1)  ], 

 			makeBNetLearner [ ("mmlLearner",cptLearnerMML5), ("mix", true)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearner5),    ("mix", true)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearnerMML1), ("mix", true)  ], 
 			makeBNetLearner [ ("mmlLearner",cptLearner1),    ("mix", true)  ], 

 			makeBNetLearner [ ("mmlLearner",dTreeLearnerMML5)  ], 
 			makeBNetLearner [ ("mmlLearner",dTreeLearner5)  ], 
 			makeBNetLearner [ ("mmlLearner",dTreeLearnerMML1)  ], 
 			makeBNetLearner [ ("mmlLearner",dTreeLearner1)  ], 

 			makeBNetLearner [ ("mmlLearner",dTreeLearnerMML5), ("mix", true)  ], 
 			makeBNetLearner [ ("mmlLearner",dTreeLearner5),    ("mix", true)  ], 
 			makeBNetLearner [ ("mmlLearner",dTreeLearnerMML1), ("mix", true)  ], 
 			makeBNetLearner [ ("mmlLearner",dTreeLearner1),    ("mix", true)  ],

			CammlSearchPackage.CPTMetropolisLearner,
			CammlSearchPackage.DTreeMetropolisLearner,
	  	  	CammlSearchPackage.DualMetropolisLearner,

			CammlSearchPackage.CPTMixtureLearner,
			CammlSearchPackage.DTreeMixtureLearner,
	  	  	CammlSearchPackage.DualMixtureLearner,

		  	FriedmanWrapper.parameterize_BDE,
		  	FriedmanWrapper.parameterize_MDL,

			FriedmanWrapper.parameterize_Tree,
			FriedmanWrapper.parameterize_MDL_Tree
			}
			
		 ],

	searchNames = [	
			"MMLAdaptive+0.5", "Adaptive+0.5", "MMLAdaptive+1.0", "Adaptive+1.0"
			{
			"MMLAdaptive+0.5", "Adaptive+0.5", "MMLAdaptive+1.0", "Adaptive+1.0",
			"MMLAdaptive+0.5Mix", "Adaptive+0.5Mix", "MMLAdaptive+1.0Mix", "Adaptive+1.0Mix",

			"MMLTreeAdaptice+0.5", "TreeAdaptice+0.5", "MMLTreeAdaptice+1.0", "TreeAdaptice+1.0",
			"MMLTreeAdaptice+0.5Mix", "TreeAdaptice+0.5Mix", "MMLTreeAdaptice+1.0Mix", "TreeAdaptice+1.0Mix",

			"CPTMetropolis", "DTreeMetropolis", "DualMetropolis",
			"CPTMixture", "DTreeMixture", "DualMixture",
		  	"Friedman.BDE",	"Friedman.MDL", "Friedman.Tree", "Friedman.MDL.Tree"
			}
			],

	{ --------------------------------------------------------------------------------------- }
	{                   The following should remain constant                                  }
	{ --------------------------------------------------------------------------------------- }



{ Generate data from <makeTrainingData globalParam>. Perform <folds> folds of crossvalidation }
{ using each search in <search> and each value in <localParamArray> for a total of            }
{ <folds> * length <search> * length <localParamArray> networks learned for each global param }
{ send an email home(rodo@bruce) with the result for each net.                                }
{ results of form [[search0.mean, search0.sd, search1.mean, search1.sd, ..., search(N-1).sd]] }
runFullTest = lambda globalParam . let
 
	trainDataCreator = makeTrainData globalParam,
	testDataCreator = makeTestData globalParam,
	trueModelCreator = makeTrueModel globalParam,
	modelName = makeModelName globalParam,

	{ Learn a model and calculate the logP of test data given that model. }
	logPTraining = lambda localParam . lambda s . lambda fold . let 
		
		xx = Library.println "Creating Training Data",
		{ Load trainData, testData and true model appropriate for this fold. }
		trainData = trainDataCreator localParam fold,
		testData = testDataCreator localParam fold,
		trueMY = trueModelCreator localParam fold,

		{ Perform search }
		trainX = cmpnt 0 trainData,
		trainZ = cmpnt 1 trainData,
		learnedMSY = s () trainX trainZ,
		learnedModel = cmpnt 0 learnedMSY,
		learnedParams = cmpnt 2 learnedMSY,
		
		{ Evaluate by KL distance }
		testX = cmpnt 0 testData,
		testZ = cmpnt 1 testData,
		logProbTestData = logP learnedModel learnedParams testZ testX,

		{ If we have a true model, work out logP Data given true model.  Else treat as 0.0 }
		trueLogProbTestData = if ((length trueMY) = 2) 
			then let
				trueModel = cmpnt 0 trueMY,
				trueParams = cmpnt 1 trueMY
			in (logP trueModel trueParams testZ testX)
			else 0.0,

		kl = ( trueLogProbTestData - logProbTestData ) / (length testX)

	in kl,


	combinedCombinations = combineList [ localParamArray, search, (iota folds)],
	unravelledResultVec = rmap (lambda x . logPTraining (elt x 0 ) (elt x 1) (elt x 2)) combinedCombinations,

	{ resultVector = [[[kl]]], indexed by [localParam][search][fold] }
	resultVector = getGroupsOfN (getGroupsOfN unravelledResultVec ((length search) * (length localParamArray))) (length localParamArray),

	
	{ Take minVec as a vaseline for searches. Subtract this from all results. }
	makeBaseline = lambda vec . let 
			minVec = map min (transpose vec)
		in map (lambda subVec . map (lambda i . (elt subVec i) - (elt minVec i)) (iota (length subVec))) vec,
	baselineVec = map makeBaseline resultVector,
	{averageResults = map (map getStats) baselineVec,}


	averageResults = map (map getStats) resultVector,



	resultString = sconcat [ "modelName = ", modelName, newline,
				 "mapping over : ", mappingOver, newline,
				 "folds = ", folds, newline,
				 "searchNames = ", searchNames, newline,
				  map ( lambda sampleIndex .
					sconcat [ elt localParamArray sampleIndex, "  ", elt averageResults sampleIndex, newline ]
				      ) ( iota (length localParamArray) )
				],

	headerString = sconcat [ "fullResults : ", modelName ],
	fullMail = sendMail "bruce.csse.monash.edu.au" "rodo" "rodo" headerString resultString
in resultString,



fullFullResults = 	map runFullTest globalParamArray,


modelNameArray = map makeModelName globalParamArray,
fullFullResultsHeader = sconcat ["fullFullResults : modelNameArray = ", modelNameArray ],
fullFullMail = sendMail "bruce.csse.monash.edu.au" "rodo" "rodo" fullFullResultsHeader  (format fullFullResults)
in fullFullResults
