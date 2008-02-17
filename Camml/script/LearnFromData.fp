let 
	{ Load data from .cas file }
	data = CammlModule.oldCammlLoad "/home/rodo/AsiaCases.1000.cas",
	
	{ Load data in C5 format }
	{ data = C5.loadC5file ("/home/rodo/zoo.data",()), }

	search = CammlSearchPackage.runCPTMetropolis

in search data