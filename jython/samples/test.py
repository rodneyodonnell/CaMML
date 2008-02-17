# Add jython libs to path and import.
import sys
import user
sys.path.append(user.home+'/CAMML/jython/lib')

# Import useful libraries
from py_bNet import *
from py_util import *


# run tests which have not been run yet.
runTests = True
gzip = True


# if cklBroken == True, all CKL and KL are marked as broken.
cklBroken = False

## Datasets to test. type: [(fName, #samples)]
dneRoot = user.home+"/Monash/Repository/data/dne/"
fList = [
	#(dneRoot + "Asia.dne", 100),
	(dneRoot + "Asia.dne", 1000),
	#(dneRoot + "Asia.dne", 4000),
	#(dneRoot + "Asia.dne", 16000),

	#(user.home+"/Repository/Monash/Papers/CKL/scripts/Metastatic/Metastatic.dne", 10),
	#(user.home+"/Repository/Monash/Papers/CKL/scripts/Metastatic/Metastatic.dne", 100),
	#(user.home+"/Repository/Monash/Papers/CKL/scripts/Metastatic/Metastatic.dne", 1000),
	#(user.home+"/Repository/Monash/Papers/CKL/scripts/Metastatic/Metastatic.dne", 4000),
	#(user.home+"/Repository/Monash/Papers/CKL/scripts/Metastatic/Metastatic.dne", 16000),

	#(dneRoot + "insurance.bif.dne",   100),
	#(dneRoot + "insurance.bif.dne",   250),
	#(dneRoot + "insurance.bif.dne",   500),
	#(dneRoot + "insurance.bif.dne",  1000),
	#(dneRoot + "insurance.bif.dne",  2000),
	#(dneRoot + "insurance.bif.dne",  4000),
	#(dneRoot + "insurance.bif.dne",  8000),
  	#(dneRoot + "insurance.bif.dne", 16000),
	#(dneRoot + "insurance.bif.dne", 1000000),

	#(dneRoot + "Alarm.dne",   100),
	#(dneRoot + "Alarm.dne",   250),
	#(dneRoot + "Alarm.dne",   500),
 	#(dneRoot + "Alarm.dne",  1000),
 	#(dneRoot + "Alarm.dne",  2000),
	#(dneRoot + "Alarm.dne",  4000),
 	#(dneRoot + "Alarm.dne",  8000),
 	#(dneRoot + "Alarm.dne", 16000),

 	#(dneRoot + "hailfinder.bif.dne",   100),
 	#(dneRoot + "hailfinder.bif.dne",   250),
 	#(dneRoot + "hailfinder.bif.dne",   500),
 	#(dneRoot + "hailfinder.bif.dne",  1000),
 	#(dneRoot + "hailfinder.bif.dne",  2000),
 	#(dneRoot + "hailfinder.bif.dne",  4000),
 	#(dneRoot + "hailfinder.bif.dne",  8000),
 	#(dneRoot + "hailfinder.bif.dne", 16000),
]

## List of local structure learners.
from camml.core.models import *
learn = { "multi"  : multinomial.AdaptiveCodeLearner.mmlAdaptiveCodeLearner,  ## Std multinomial learner (n+.5)/(N+m*.5)
	  "multi2" : multinomial.AdaptiveCodeLearner.mmlAdaptiveCodeLearner2, ## MinEKL multinomial (n+.1)/(N+m)
	  "CPT"    : cpt.CPTLearner.mmlAdaptiveCPTLearner,  # CPT learner using learn['multi']
	  "CPT2"   : cpt.CPTLearner.mmlAdaptiveCPTLearner2, # CPT learner using learn['multi2']
	  "tree"  : dTree.ForcedSplitDTreeLearner.multinomialDTreeLearner,  # DTree learner using learn['multi']
	  "tree2" : dTree.ForcedSplitDTreeLearner( multinomial.AdaptiveCodeLearner.mmlAdaptiveCodeLearner2 ), # DTree learner using learn['multi2']
	  "logit"  : logit.LogitLearner.logitLearner,  # Julian Neil's logit learner		  
	  }

# Create a dual model learner. params should be arrays of strings (indexing learn{})
# for models applicable with zero, one, or many parents.
def dual( learner ):
	for i in range(len(learner)): learner[i] = learn[learner[i]]
	return camml.core.models.dual.DualLearner([learn["multi"]], [learn["CPT"]], learner)

# Same as dual, mut learn['multi2'] is default instead of learn['multi']
def dual2( learner ):
	for i in range(len(learner)): learner[i] = learn[learner[i]]
	return camml.core.models.dual.DualLearner([learn["multi2"]], [learn["CPT2"]], learner)

learn["dualCL"] = dual(["CPT","logit"])
learn["dualCL2"] = dual2(["CPT2","logit"])

learn["dualCT"] = dual(["CPT","tree"])
learn["dualCT2"] = dual2(["CPT2","tree2"])

learn["dualCTL"] = dual(["CPT","tree","logit"])
learn["dualCTL2"] = dual2(["CPT2","tree2","logit"])

learn["dualTL"] = dual(["tree","logit"])
learn["dualTL2"] = dual2(["tree2","logit"])




## Local structure to use.
localStructureLearner = [
	("logit",   "LogitLearner.learnerStruct"),
	#("dualCL",  "dualCL.learnerStruct"),
	("dualCTL", "dualCTL.learnerStruct"),
	#("dualTL",  "dualTL.learnerStruct"),
	("CPT",     "CPTLearner.learnerStruct"),
	#("dualCT",  "dualCT.learnerStruct"),
	("tree",    "ForcedSplitDTreeLearner.learnerStruct"),

	("FBDe",    camml.plugin.friedman.FriedmanLearner.modelLearner_BDE ),
	#("FMDL",    camml.plugin.friedman.FriedmanLearner.modelLearner_MDL ),
	#("FMDL-Tree",    camml.plugin.friedman.FriedmanLearner.modelLearner_MDL_Tree ),
	("FBDe-Tree",    camml.plugin.friedman.FriedmanLearner.modelLearner_Tree ),

	###### NOTE: The code below adds some extra learners too!
	]
											

## makeBNetLearner( option[], optionVal[] )
makeBNetLearner = camml.core.models.bNet.BNetLearner.MakeBNetLearner()._apply

## Model learners to use.
bNetLearnerArray = []
for x in localStructureLearner:
	# Local structure learners passed as strings.
	if not isinstance( x[1], camml.core.models.ModelLearner ):
		bNetLearnerArray.append( makeBNetLearner(["mmlLearner","fullResults"],[cml(x[1]),cml("true")]) )
	# Full BNet learners passed as modelLearners.
	else:
		bNetLearnerArray.append( x[1] )

## Add a few extra learner options with nothing to do with local structure.
#localStructureLearner.append(("CKL3-join","ckl joining enabled"))
#bNetLearnerArray.append( makeBNetLearner(["cklJoinType","fullResults"],[cml("3 "),cml("true")]) )

#localStructureLearner.append(("noJoinDAGs","DAG -> SEC joining disabled"))
#bNetLearnerArray.append( makeBNetLearner(["joinDAGs","fullResults"],[cml("false "),cml("true")]) )

#################################








folds = 10

import sys
if sys.argv.count('Run'):
	runTests = True
	sys.argv.pop(sys.argv.index('Run'))
foldArray = map( int, sys.argv[1:] )
if len(foldArray) == 0: foldArray = range(folds)

# Function to split data into 'folds' sections.
# split("fName").cmpnt(0) -> Training, split("fName").cmpnt(1) -> Test
split = cml("lambda data . xValidation " + str(folds) + " 0.9 data \"namename\"" ).apply


## Nasty workaround for jython bug.
## None != unserialise(serialise(None))
def isNone( x ):
	if x == None: return True
	if str(x) == str(None): return True
	return False

# Return ({train},{test}) given a full dataset and a local filename.
# All files are saved to disk to ensure the same splits are always used.
def loadSplitData( fullData, localName ):
	train,test = {},{}
	mkdir2( localName+"/train" )
	mkdir2( localName+"/test" )
	splitData = None
	for i in range(folds):
		train[i] = loadData( localName + "/train/train" + str(i) + ".cas", False )
		test[i] = loadData( localName + "/test/test" + str(i) + ".cas", False )
		if train[i] == None or test[i] == None:
			if splitData == None: splitData = split( fullData )
			train[i] = splitData.cmpnt(0).elt(i)
			test[i] = splitData.cmpnt(1).elt(i)
			saveData( localName + "/train/train" + str(i) + ".cas", train[i], False )
			saveData( localName + "/test/test" + str(i) + ".cas", test[i], False )
			# Reload data from disk.
			# This forces train[i].cmpnt(j) to be a FastDiscreteVector instead
			# of a SubsetVector, and makes some optimizations possible.
			train[i] = loadData( localName + "/train/train" + str(i) + ".cas", False )
			test[i] = loadData( localName + "/test/test" + str(i) + ".cas", False )
	return (train,test)


# Generate 'folds' datasets of size 'n' based on the net provided
def makeData( m, y, localName, n ):
	train = {}
	mkdir2( localName+"/train" )
	splitData = None
	for i in foldArray:
		fName = localName + "/train/train" + str(i) + "." + str(n) + ".cas"
		train[i] = loadData( fName, False )
		if train[i] == None:
			train[i] = m.generate( camml.core.library.WallaceRandom([i+7,i+77]), n, y, triv )
			saveData( fName, train[i], False )
			# Reload data from disk.
			# This forces train[i].cmpnt(j) to be a FastDiscreteVector instead
			# of a SubsetVector, and makes some optimizations possible.
			train[i] = loadData( fName, False )
	return train
		


# Result of the search.
# Indexed by ['localName']['learnerName'][fold]['msy'|'logPTrain'|time]
fullResults = {}

def getLocalName(xx):
	netName,samples = xx
	return netName.split("/")[-1] + "." + str(samples)

for (netName,samples) in fList:
	localName = netName.split("/")[-1] + "." + str(samples)
	print "\nRunning :", localName,
	#
	## Name of file fullResult[localName] is stored in
	fullResultFileName = localName + "/" + localName  + ".obj"
	mkdir2( localName )
	#
	## Load train and test datasets.
	if runTests:
		my = loadNet(netName)
		trueM,trueY = my.cmpnt(0),my.cmpnt(1)
		trainData = makeData( trueM, trueY, localName, samples )
	#
	## Load any partially computed results.
	if not fullResults.has_key(localName):
		fullResults[localName] = loadObject( fullResultFileName, False, gzip=gzip )
		if fullResults[localName] == None:
			fullResults[localName] = {}
	resultUpdated = False
	#
	## For each learner:
	for learnerIndex in range(len(bNetLearnerArray)):
		learner = bNetLearnerArray[learnerIndex]
		learnerName = localStructureLearner[learnerIndex][0]
		print "\n",pad(learnerName,15), "\t",
		#
		if not fullResults[localName].has_key(learnerName):
			fullResults[localName][learnerName] = {}
		#
		for fold in foldArray:
			#
			## If there is nothing to do, continue.
			if fullResults[localName][learnerName].has_key(fold) and runTests == False:
				if not isNone(fullResults[localName][learnerName][fold]):
					continue
			#
			## Ensure fold has been initialised.
			if not fullResults[localName][learnerName].has_key(fold) or isNone(fullResults[localName][learnerName][fold]):
				fullResults[localName][learnerName][fold] = {}
			#
			## Print dot for each step of work.
			print '.',
			#
			## Load results for this [net][learner][fold] combinaiton.
			## result <= fullResults[localName][learnerName][fold]
			if fullResults[localName][learnerName][fold].has_key('ckl0') and (not isNone(fullResults[localName][learnerName][fold]['ckl0'])):
				continue
			resultFileName = localName + "/" + learnerName + "-" + str(fold) + ".obj"
			result = loadObject( resultFileName, False, gzip=gzip )
			#
			## Do the real work if required.
			if runTests == True:
				# If no result recorded, run learner.
				if isNone(result): # == None :
					## Learn model, and count time taken to do it.
					t = -java.lang.System.currentTimeMillis()
					msy = learner.parameterize( triv, trainData[fold], trainData[fold] )
					t += java.lang.System.currentTimeMillis()
					result = { "msy" : msy, "time" : t }
					#
					## Save object so recalculation is not necesarry.
					saveObject( resultFileName, result, gzip=gzip )
				msy = result["msy"]
				learnedM = msy.cmpnt(0)
				learnedY = msy.cmpnt(2)
				#
				## BUGFIX: Must mark all CKLs as broken before repairing them.
				if (cklBroken):
					result['ckl0'] = None
					result['ckl1'] = None
					result['ckl2'] = None
					result['ckl3'] = None
					result['ckl0.2'] = None
					result['ckl1.2'] = None
					result['ckl2.2'] = None
					result['ckl3.2'] = None
					saveObject( resultFileName, result, True, gzip=gzip )
					print "Setting ckl =", result['ckl3']
				#
				## Add KL and CKL variants.
				## Scores are multiplied by 1/p(intervention) to be comparable to regular KL
				if (not cklBroken) and ((not result.has_key('ckl0')) or (isNone(result['ckl0']))):					
					result["ckl0"] = trueM.ckl( trueY, learnedY, 0 ) # ckl0 == kl
					#result["ckl1"] = trueM.ckl( trueY, learnedY, 1 )
					#result["ckl2"] = trueM.ckl( trueY, learnedY, 2 )
					result["ckl3"] = trueM.ckl( trueY, learnedY, 3 )
					saveObject( resultFileName, result, True, gzip=gzip)
				#
				## KL & CKL results using minEKL params.
				if (not cklBroken) and ((not result.has_key('ckl0.2')) or (isNone(result['ckl0.2']))) and result["msy"].length() == 4:
					key2 = str(learnerName)+"2"
					if learn.has_key( key2 ):
						tom = camml.core.search.TOM(trainData[fold])
						tom.setStructure(learnedY)
						minEKLParams = tom.makeParameters( learn[key2] )
						result["ckl0.2"] = trueM.ckl( trueY, minEKLParams, 0 ) # ckl0 == kl
						#result["ckl1.2"] = trueM.ckl( trueY, minEKLParams, 1 )
						#result["ckl2.2"] = trueM.ckl( trueY, minEKLParams, 2 )
						result["ckl3.2"] = trueM.ckl( trueY, minEKLParams, 3 )
						saveObject( resultFileName, result, True, gzip=gzip )
				#
				## Calcualte Edit distance.
				if not result.has_key('ed'):
					# ed has the form: (ed,add,del,rev,undirected,correct)					
					ed = editDistance(trueY, learnedY)
					result["ed.total"] = ed['ed']
					result["ed.add"] = ed['add']
					result["ed.del"] = ed['sub']
					result["ed.rev"] = ed['rev']
					result["ed.undirected"] = ed['und']
					saveObject( resultFileName, result, True, gzip=gzip )
				#
				## Save mml cost, #params and #arcs
				if not result.has_key('#arcs') and result["msy"].length() == 4:
					resultVec = result["msy"].cmpnt(3)
					tomStruct = resultVec.elt(0).cmpnt(0).elt(0).cmpnt(0).elt(0)
					result["mml"] = tomStruct.doubleCmpnt(4)
					result["#arcs"] = tomStruct.doubleCmpnt(6)
					result["#params"] = tomStruct.doubleCmpnt(7)
					result["posterior"] = tomStruct.doubleCmpnt(2)
					saveObject( resultFileName, result, True, gzip=gzip )
					# (BNetModel=Model(TYPE|TYPE,TYPE),
					#  [Node]=TYPE,
					#  posterior=Continuous,
					#  cleanML=Continuous,
					#  bestMML=Continuous,
					#  numVisits=Discrete,
					#  numArcs=Discrete,
					#  numParams=Discrete,
					#  logNumExtensions=Continuous,
					#  datCost=Continuous)
				## #arcs not listed, but only single model given.
				elif not result.has_key('#arcs'):
					msy = result["msy"]
					m = msy.cmpnt(0)
					y = msy.cmpnt(2)
					result["#params"] = m.getNumParams(y)
					t = 0
					vec = y.cmpnt(1)
					for i in range(vec.length()): t += vec.elt(i).length()
					result["#arcs"] = t
					saveObject( resultFileName, result, True, gzip=gzip )
			#
			## Cache results.
			fullResults[localName][learnerName][fold] = result
			resultUpdated = True
			#
			## Save space... maybe.
			if not isNone(result): # != None:
				#msy = result["msy"]
				#result["msy2"] = result["msy"].cmpnt(2)
				result["msy"] = None
			#
			if isNone(result) and runTests == False:
				print fold,
		## End [fold] loop
	## End [learner] loop
	saveObject( fullResultFileName, fullResults[localName], True, gzip=gzip )
				

debug = [0]
def printAverage( key, sort = True ):
	import py_stats
	print "\nTotal - ", key
	t = {}
	fr = fullResults
	netNames = map( getLocalName, fList )	
	#netNames = fr.keys()
	#netNames.sort() # Make a consistent ordering.
	for data in netNames:
		print "\n",data, key
		summary = []
		for k in localStructureLearner: #fr['balance-scale.arff'].keys():
			if fr[data].keys().count(k[0]) == 0: continue
			k = k[0]
			t[k] = []
			for i in fr[data][k].keys():
				## I think there is a bug here in jython serialisation
				debug[0] = fr[data][k][i]
				try:
					if not isNone(debug[0]):# != None:
						if fr[data][k][i].has_key(key) and (not isNone(fr[data][k][i][key])):
							t[k].append( fr[data][k][i][key] )
						elif key.endswith(".2") and  fr[data][k][i].has_key(key[:-2]) and (not isNone(fr[data][k][i][key[:-2]])):
							t[k].append( fr[data][k][i][key[:-2]] )
				except AttributeError:					
					print "Ignoring exception, probably due to buggy serialisation", debug[0], k, i, str(debug[0])
					continue # Do nothing.
			if len(t[k]) > 1 :
				summary.append( {"mean":py_stats.mean(t[k]), "sterr":py_stats.sterr(t[k]),"len":len(t[k]), "name": k } )
			else: summary.append( {"mean":0, "sterr":0,"len":len(t[k]), "name": k } ) 
		if sort: summary.sort(lambda a,b: a["mean"] > b["mean"])
		#
		for x in summary: print pad(x["name"],11), "\tmean=",x["mean"], "\tsterr=",x["sterr"], "\tlen", x["len"]



# Print out a table for easy-insertion into latex
def printLatex( key ):
	import py_stats
	fr = fullResults
	#
	## Extract keys.
	files = fr.keys()
	learners = fr[files[0]].keys()
	folds = fr[files[0]][learners[0]].keys()
	#
	## Begin printing latex.
	## -- Header.
	print "\\begin{table}[htbp]"
	print "\t\\centering"
	print "\t\\scriptsize"
	print "\t\\begin{tabular}{|c|",
	for _ in files: print "rr|",
	print "} \\hline"
	#
	## File names.
	print "\tDataset ",
	for f in files: print "& \\multicolumn{2}{|c|}{",f[:-4],"} ",
	print "\\tabularnewline \\hline \\hline"
	print "\tMetric   ",
	for _ in files: print "& Mean    & SE            ",
	print "\\tabularnewline \\hline"
	#
	## Print out actual data.
	for x in learners:
		print "\t"+str(x), "\t",
		for f in files:
			data = []
			for xx in fr[f][x].values():
				if xx.has_key(key): data.append( xx[key] )
			#print "&", py_stats.mean(data), "& ", py_stats.sterr(data),
			if len(data) > 1: print "& %4.3f & %4.3f" % (py_stats.mean(data), py_stats.sterr(data)),
			else: print "& XXX & XXX",
		print " \\tabularnewline"
	#
	## Print footer.
	print "\t\\hline"
	print "\t\\end{tabular}"
	print "\t\\caption{",key,"}"
	print "\t\\label{}"
	print "\\end{table}"



##       CPT     logit   tree    dualCTL FBDe    FBDe-Tree       CPT     logit   tree    dualCTL FBDe    FBDe-Tree
#100     2.51943905853219        2.02359672190047        2.62941023148433        2.12954133802921        2.34604003693818        2.33419286940233        0.0694948396605709      0.0768886201184336      0.0611148768334269      0.0825104365696476      0.0746107382248885      0.0603777642812566
#250     1.19521550202474        0.928409610615971       1.221540029215  0.967573704082845       1.09217105447672        1.12111026576862        0.0368205465028033      0.0304690992805078      0.0272705388608668      0.0336877769071289      0.0368593863857644      0.0336194524394969
#500     0.688038531733925       0.541583503132701       0.688214090561733       0.499872558226807       0.668122415004979       0.592706039753213       0.00836402821147317     0.0137613430451682      0.0148462206189041      0.0127430576611298      0.0435613070443528      0.0178592872838602
#1000    0.431299778235286       0.333311839758005       0.382409468339251       0.26275595333864        0.340930711506147       0.319050685198104       0.00880212728934749     0.00566947204251877     0.0126984580405312      0.00627684207088484     0.00729449458048782     0.0116728699236637
#2000    0.256763855866256       0.217307961535414       0.16457258209177        0.161700309768556       0.218034740033999       0.180263852856524       0.00355693784971772     0.00228705855240371     0.00579507985090179     0.00293357171490586     0.014460767250541       0.0178254870634067
#4000    0.163972659629181       0.17259686843658        0.0917144556039023      0.104369552778066       0.128989921442116       0.0927858416133182      0.00579496503001752     0.00216718062809148     0.00254843190917298     0.00203782853023144     0.0107286688619848      0.00548779648564163
#8000    0.0843462256364669      0.140523736543457       0.0478884199743987      0.0497319774056606      0.0923768088199045      0.0527424774241677      0.00548935877050282     0.000847543225608642    0.00151948101319544     0.00107470738081549     0.0143418504813185      0.00632925465781915
#16000  0.0444904670594145      0.126798713928617       0.0262981280091877      0.0256896441897714      0.0267734111018942      0.0230326929607415      0.00320298081338963     0.00053781887166556     0.000601474935594947    0.000916813409838037    0.001723455681472       0.000316244548159804
def printGnuplot( key ):
	import py_stats
	fr = fullResults
	#
	## Extract keys.
	files = map( getLocalName, fList )	
	#learners = map( lambda x : x[0], localStructureLearner )
	# Learners in order required by gnuplot scripts.
	learners = ['CPT', 'logit', 'tree', 'dualCTL',   'FBDe', 'FBDe-Tree']
	folds = fr[files[0]][learners[0]].keys()
	#
	## Print Learner Key
	print "# ",
	for x in learners: print x,
	print
	#
	## Print out actual data.
	for f in files:
		n = f.split('.')[-1]     # Print number of cases
		print str(n), "\t",
		for x in learners:
			data = []
			for xx in fr[f][x].values():
				if xx.has_key(key): data.append( xx[key] )
			if len(data) > 1: print "%4.8f\t" % py_stats.mean(data),
			else: print "& XXX & XXX",
		for x in learners:
			data = []
			for xx in fr[f][x].values():
				if xx.has_key(key): data.append( xx[key] )
			if len(data) > 1: print "%4.8f\t" % py_stats.sterr(data),
			else: print "& XXX & XXX",
		print

for x in ['ckl0','ckl3','#arcs','#params', 'ed.total']:
	print '\n\n#', x
	printGnuplot(x)








# Create two dimensional array with all entries initialised to val.
def makeArray( n1, n2, val=None ):
	x = [None] * n1
	for i in range(n1):
		x[i] = [val]*n2
	return x

# Not defined in jython
def sum(xx):
	total = 0
	for x in xx: total += x
	return total



rev3 = {}
add3 = {}
sub3 = {}

for (netName,samples) in fList:
	rev3[(netName,samples)] = rev2 = {}
	add3[(netName,samples)] = add2 = {}
	sub3[(netName,samples)] = sub2 = {}
	#
	## Load true model
	my = loadNet(netName)
	trueM,trueY = my.cmpnt(0),my.cmpnt(1)
	localName = netName.split("/")[-1] + "." + str(samples)
	n = trueY.length()
	trueTOM = camml.core.search.TOM(n)
	trueTOM.setStructure( trueY )
	#
	## For each learner:
	for learnerIndex in range(len(bNetLearnerArray)):
		learner = bNetLearnerArray[learnerIndex]
		learnerName = localStructureLearner[learnerIndex][0]
		#
		## Create arrays to store #revs and #toggles 
		add2[learnerName] = add = makeArray( n, n, 0 )
		sub2[learnerName] = sub = makeArray( n, n, 0 )
		rev2[learnerName] = rev = makeArray( n, n, 0 )
		#
		## For each fold
		for fold in foldArray:
			#
			## Load learned model
			resultFileName = localName + "/" + learnerName + "-" + str(fold) + ".obj"
			results = loadObject( resultFileName, False, gzip=gzip )
			msy = results['msy']
			learnedY = msy.cmpnt(2)
			tom = camml.core.search.TOM(n)
			tom.setStructure( learnedY )
			for i in range(n):
				for j in range(i+1, n):
					if trueTOM.isArc(i,j) and not tom.isArc(i,j):
						sub[i][j] += 1
					if not trueTOM.isArc(i,j) and tom.isArc(i,j):
						add[i][j] += 1
					if trueTOM.isArc(i,j) and tom.isArc(i,j) and (trueTOM.before(i,j) != tom.before(i,j)):
						rev[i][j] += 1
					

#printLatex('logPTrain')
#printLatex('time')
#printLatex('logPMix')
#printLatex('mml')
#printLatex('#arcs')
#printLatex('#params')

#printAverage('time',False)

#printAverage('logPTrain', False)

#printAverage('logPMix')

#printAverage('mml')

#printAverage('#arcs',False)

#printAverage('#params',False)

#printAverage('posterior')

#printAverage('logPMinEKL', False)

#printAverage( 'ckl0', False )
#printAverage( 'ckl3', False )

#printAverage( 'ckl0.2', False )
#printAverage( 'ckl3.2', False )

#printAverage( 'ed.add', False )
#printAverage( 'ed.del', False )
#printAverage( 'ed.rev', False )

#printAverage('#arcs',False)
#printAverage('#params',False)



#printAverage( 'ckl0', True )
#printAverage( 'ckl3', True )

#printAverage( 'ed.total', True )

#printAverage( 'ed.add', True )
#printAverage( 'ed.del', True )
#printAverage( 'ed.rev', True )

#printAverage('#arcs',True)
#printAverage('#params',True)


# for i in [1] : print fullResults['insurance.bif.dne.16000']['logit'][i]['ckl3']
#{9: {'ckl3': -4.095926243973921, 'ed.total': 9.0, 'mml': 212540.92998354393, 'ed.del': 4, 'ckl0': 0.12557904042768442, 'ed.rev': 1, 'ed.undirected': 0, 'posterior': 0.0240900213944765, 'time': 5058550L, '#params': 361.0, '#arcs': 51.0, 'msy': None, 'ed.add': 3}, 8: {'ckl3': -1.6542967084974478, 'ed.total': 7.0, 'mml': 213058.06776141108, 'ed.del': 3, 'ckl0': 0.12850925682841208, 'ed.rev': 1, 'ed.undirected': 0, 'posterior': 0.04287837468892151, 'time': 5661848L, '#params': 362.0, '#arcs': 51.0, 'msy': None, 'ed.add': 2}, 7: {'ckl3': 13.352606865353223, 'ed.total': 17.0, 'mml': 212144.78071052305, 'ed.del': 6, 'ckl0': 0.12990826249691878, 'ed.rev': 4, 'ed.undirected': 0, 'posterior': 9.96065405530829E-4, 'time': 7933123L, '#params': 352.0, '#arcs': 49.0, 'msy': None, 'ed.add': 3}, 6: {'ckl3': -1.5640103737087543, 'ed.total': 9.0, 'mml': 212286.63712635642, 'ed.del': 4, 'ckl0': 0.1262774143248908, 'ed.rev': 1, 'ed.undirected': 0, 'posterior': 0.034228489318498395, 'time': 7723273L, '#params': 359.0, '#arcs': 51.0, 'msy': None, 'ed.add': 3}, 5: {'ckl3': -1.6335845228809802, 'ed.total': 10.0, 'mml': 212417.03418377842, 'ed.del': 5, 'ckl0': 0.12843293633541286, 'ed.rev': 1, 'ed.undirected': 0, 'posterior': 0.0011130137938902276, 'time': 8784271L, '#params': 353.0, '#arcs': 50.0, 'msy': None, 'ed.add': 3}, 4: {'ckl3': -4.064720173046787, 'ed.total': 9.0, 'mml': 212629.4978269495, 'ed.del': 4, 'ckl0': 0.1257701414078607, 'ed.rev': 1, 'ed.undirected': 0, 'posterior': 0.10451482068405006, 'time': 7989968L, '#params': 361.0, '#arcs': 51.0, 'msy': None, 'ed.add': 3}, 3: {'ckl3': -3.940168074314826, 'ed.total': 12.0, 'mml': 212223.5356658442, 'ed.del': 4, 'ckl0': 0.12525368863410158, 'ed.rev': 2, 'ed.undirected': 0, 'posterior': 0.052067475518743994, 'time': 7126363L, '#params': 362.0, '#arcs': 52.0, 'msy': None, 'ed.add': 4}, 2: {'ckl3': 6.038074471605142, 'ed.total': 11.0, 'mml': 211843.88948317379, 'ed.del': 5, 'ckl0': 0.12707284568955046, 'ed.rev': 2, 'ed.undirected': 0, 'posterior': 0.00921496343573722, 'time': 7929303L, '#params': 355.0, '#arcs': 49.0, 'msy': None, 'ed.add': 2}, 1: {'ckl3': -4.057451806685783, 'ed.total': 8.0, 'mml': 212100.8309857214, 'ed.del': 3, 'ckl0': 0.12445748845537885, 'ed.rev': 1, 'ed.undirected': 0, 'posterior': 0.007270056835316302, 'time': 5766203L, '#params': 365.0, '#arcs': 52.0, 'msy': None, 'ed.add': 3}, 0: {'ckl3': 0.035465385041277386, 'ed.total': 12.0, 'ed.undirected': 0, 'ed.rev': 2, 'ckl0': 0.1267260646859576, 'ed.del': 5, 'mml': 212765.86390200735, 'posterior': 0.0010779399159958356, '#params': 358.0, 'time': 7980288L, '#arcs': 50.0, 'msy': None, 'ed.add': 3}}

#print "\nfr : ", fullResults['insurance.bif.dne.16000']['logit'][1]['ckl0']
#print "trueM.ckl( trueY, learnedY, 0 )", trueM.ckl( trueY, learnedY, 0 )


#print "\nfr : ", fullResults['insurance.bif.dne.16000']['logit'][1]['ckl3']
#print "trueM.ckl( trueY, learnedY, 3 )", trueM.ckl( trueY, learnedY, 3 )



# fr = fullResults
# for net in fr.keys():
# 	for ll in fr[net].keys():
# 		for fold in fr[net][ll].keys():
# 			if fr[net][ll][fold]['ckl3'] < 0:
# 				print net, ll, fold, fr[net][ll][fold]['ckl3']
