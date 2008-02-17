let 
	{ Files to load }
	files = [
		{"/home/rodo/Repository/data/julianNeil/flare.cas",
		"/home/rodo/Repository/data/julianNeil/icu.cas",
		"/home/rodo/Repository/data/julianNeil/nursery.12960.cas",
		"/home/rodo/Repository/data/julianNeil/popularity.cas",
		"/home/rodo/Repository/data/julianNeil/vote.cas",
		"/home/rodo/Repository/data/julianNeil/zoo.101.cas",
		"/home/rodo/Repository/data/julianNeil/temp.cas",
		"/home/rodo/Repository/data/julianNeil/temp2.cas",
		"/home/rodo/AsiaCases.1000.cas"}

		"/home/rodo/Repository/data/julianNeil/zoo.101.cas"
	],

	{ Searches to run }
	searches = [
		CammlModels.makeBNetLearner [],                                          { Default CaMML Learner }
		{CammlModels.makeBNetLearner [("mmlLearner",LogitLearner.learnerStruct)],} { CaMML using Logit local structure }
		{CammlModels.makeBNetLearner [("mmlLearner",dualCTL.learnerStruct)],}      { CaMML using Hybrid local structure }	
		{Tetrad4Module.FCI,}
		{Tetrad4Module.GES,}
		{Tetrad4Module.PC}
	],


	{ Create vector containing all datasets in "files" }
	dataVec = map RodoCamml.load files,

	{ Take search and data as paremters, return a model }
	run = lambda s . lambda d . let
		searchFN = (cmpnt 0 s),
		msy = searchFN () d d,
		y = cmpnt 2 msy
	in map (cmpnt 1) y
	

in map (lambda d . Library.vec2struct (map (lambda s . run s d) searches) ) dataVec
