let
	{ Standard operations }
	head = lambda xx . elt xx 0,
	tail = lambda xx . (cmpnt 1 (splitAt 1 xx)),

	{ Return number of times x occurs in xx }
	count = lambda x . lambda xx . 
		if length xx = 0 then 0 else
		if head xx = x then 1 + count x (tail xx) else
		count x (tail xx),

	getPosteriorWeightedArcFrequencies = lambda mmlecs . let
		{ Flatten list of MMLECs to a list of DAGs}
		secs = concat (map (lambda x . cmpnt 0 x) mmlecs),
		dags = concat (map (lambda x . cmpnt 0 x) secs),

		{ Create a vector of [(arcs,posterior)] }
		params = map (cmpnt 1) dags,
		posterior = map (cmpnt 2) dags,
		arcs = map (lambda param . map (cmpnt 1) param) params,
		vec = zip (arcs,posterior),
		len = length (elt arcs 0),

		{ if i->j exists in x, return weight. else return 0.0.  x is an element of vec. }
		getWeight = lambda i . lambda j . lambda x .
			if count j (elt (cmpnt 0 x) i) = 1 then (cmpnt 1 x) else 0.0,
	
		matrix = map (lambda j . map (lambda i . Library.sum (map (getWeight j i) vec)) (iota len)) (iota len)
	in matrix,

	{ Uncomment the following functions to add them to the environment for this session. }
	{ To permanently add them, add them (and the code above) to Camml/script/cammlBootstrap.fp }
	{
	f1 = addToEnvironment ( getPosteriorWeightedArcFrequencies, "pwaf", "Calculate posterior weighted arc frequencies"),
	}

	{ mapping vec2struct makes results more readable. }
	getPrettyResults = lambda mmlecs . map (Library.vec2struct) (getPosteriorWeightedArcFrequencies mmlecs),

a=0 in { getPosteriorWeightedArcFrequencies asiaUnclean}
	getPrettyResults asiaUnclean


