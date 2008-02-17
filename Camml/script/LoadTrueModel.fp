let 
	{ name of network }
	{ name = "/home/rodo/Camml/Asia.dnet",  }
	name = "/home/rodo/Car_Diagnosis_2.dnet",

	{ Load in the true model. }
	trueStruct = Netica.loadNet name,
	trueModel  = cmpnt 0 trueStruct,
	trueParams = cmpnt 1 trueStruct	

in CammlModels.visualiseInferenceBNet trueModel trueParams ()