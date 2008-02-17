let 
	{ Load in the true model. }
	trueStruct = Netica.loadNet "/home/rodo/Repository/CAMML/Asia.dne",
	trueModel  = cmpnt 0 trueStruct,
	trueParams = cmpnt 1 trueStruct,

	{ generate data to test model with } 
	data = generate trueModel 123 () trueParams 2000

in camml data