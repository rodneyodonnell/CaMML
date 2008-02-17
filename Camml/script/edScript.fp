let
	{ Calculate the ED between two ordered parent sets. edNode [1,3,9] [3,11] => 3 }	
	edNode = lambda a . lambda b . 
		if length a = 0 then length b else
		if length b = 0 then length a else
		if elt a 0 = elt b 0 then edNode (cmpnt 1 (splitAt 1 a)) (cmpnt 1 (splitAt 1 b)) else
		if elt a 0 < elt b 0 then 1+edNode (cmpnt 1 (splitAt 1 a)) b else
		if elt a 0 > elt b 0 then 1+edNode (cmpnt 1 (splitAt 1 b)) a else
		"Impossible state reached",

	{ Calculate Edit disance between two sets of arc lista. }
	edArcs = lambda dag1 . lambda dag2 . 
		Library.sum (map (lambda i . edNode (elt dag1 i) (elt dag2 i)) (iota (length dag1))),

	{ Calculate ED between two DAGs/SECs/MMLECs}
	edNet = lambda net1 . lambda net2 . let
		arcs1 = map (cmpnt 1) (cmpnt 1 (CammlSearchPackage.getRepresentativeDAG net1)),
		arcs2 = map (cmpnt 1) (cmpnt 1 (CammlSearchPackage.getRepresentativeDAG net2))
	in edArcs arcs1 arcs2,

	{ Calcualte the edit distance between two lists of models.}
	edNetList = lambda list1 . lambda list2 . map (lambda net1 . map (edNet net1) list2) list1,

	{ Utility functions to extract a list of SECs or DAGs from a list of MMLECs }
	getSECs = lambda result . lambda mmlecNum . cmpnt 0 (elt result mmlecNum),
	getDAGs = lambda result . lambda mmlecNum . lambda secNum . getSECs (getSECs result mmlecNum) secNum,

	vecmin = foldl min 1000000,
	
	{ Return minimum ED between a list of DAGs}
	dagsED = lambda x1 . lambda x2 . vecmin (map vecmin (edNetList x1 x2)),
	secsED = lambda secs1 . lambda secs2 . let
		dags1 = concat (map (lambda x . cmpnt 0 x) secs1),
		dags2 = concat (map (lambda x . cmpnt 0 x) secs2)
	in dagsED dags1 dags2,

	{ Uncomment the following functions to add them to the environment for this session. }
	{ To permanently add them, add them (and the code above) to Camml/script/cammlBootstrap.fp }
	{
	f1 = addToEnvironment ( edNet, "edNet", "Calculate ED between two networks"),
	f2 = addToEnvironment ( edNetList, "edNetList", "Calculate ED between two lists of networks"),
	f3 = addToEnvironment ( getSECs, "getSECs", "Extract list of SECs from MMLEC"),
	f4 = addToEnvironment ( getDAGs, "getDAGs", "Extract list of DAGs from MMLEC"),
	}

	mmlecs = asiaUnclean,

a=0 in {edNetList (getDAGs result 1 0) (getDAGs cleanResult 0 0)}
	{edNetList (getSECs asia 0) (getSECs asia 1) }
       { [[3,6],[6,9],[6,9],[2,5],[5,8],[8,5],[1,4][4,1],[6,9],[6,9],[4,7],[4,7],[4,7],[11,8],[9,6],[5,8],[9,12],[5,8],[5,8],[5,2],[3,6],[8,11]] }
	{dagsED (getDAGs asia 0 0) (getDAGs asia 1 0) }
	map (lambda j . map ( lambda i . secsED (getSECs mmlecs j) (getSECs mmlecs i) ) (iota (length mmlecs))) (iota (length mmlecs))