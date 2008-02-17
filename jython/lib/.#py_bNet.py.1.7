#import java, camml, cdms
from py_cdms import *
import cdms

## Function to create a vector of random bayes nets.
def makeNet( rand, arity, arcProb, leafProb ):
	return camml.core.models.bNet.BNetGenerator.generator.generate( rand, arity, arcProb, leafProb )

## Make a data set using a (model,param) pair, numSamples and a random number generator.
def makeData( rand, my, n ):
	model,params = my
	dataX = model.generate( rand, n, params, triv )
	dataZ = cdms.core.VectorFN.UniformVector(n,triv)
	return ( dataX, dataZ )

## Make a data set using a (model,param) pair, numSamples and a random number generator.
# Same as makeData, but returns a function and waits for .apply(triv) before evaluating.
def makeDataLazy( rand, my, n ):
	model,params = my
	seed = cdms.core.Value.Discrete(rand.nextInt())
	#dataX = model.generate( rand, n, params, triv )
	dataZ = cdms.core.VectorFN.UniformVector(n,triv)
	dataFN = cml("lambda m . lambda seed . lambda y . lambda n . lambda lazy . generate m seed () y n")
	dataFN = dataFN.apply(model)
	dataFN = dataFN.apply(seed).apply(params).apply(cdms.core.Value.Discrete(n))

	return (dataFN,dataZ)

## Return true if y1 and y2 have identical arcs
def equalStructure( y1, y2 ):
	for i in range( y1.length() ):
		if not y1.elt(i).cmpnt(1).equals( y2.elt(i).cmpnt(1) ):
			return False
	return True

## Convert CDMS representation of BNet params to arc matrix
def makeArcMatrix(params):
	arcs,n = [],params.length()
	for i in range( n ):
		temp = params.elt(i).cmpnt(1)
		row = [0]*n
		for j in range( temp.length() ):
			row[ temp.intAt(j) ] = 1
		arcs.append( row )
	return arcs

## Convert CDMS representation of BNet params to arc matrix
## makeArcMartix2 returns None if a mixture model is specified.
def makeArcMatrix2(params):
	try:
		arcs,n = [],params.length()
		for i in range( n ):
			temp = params.elt(i).cmpnt(1)
			row = [0]*n
			for j in range( temp.length() ):
				row[ temp.intAt(j) ] = 1
			arcs.append( row )
		return arcs
	except:
		return None

## Calculate multi-part edit distance as described in diffArcMatrix
def editDistance( y0, y1 ):
	return diffArcMatrix( makeArcMatrix(y0), makeArcMatrix(y1) )

# Examine two arc matrix and return (ed,add,del,rev,undirected,correct)
# Mat1 is should be used as "true model"
# Mat2 may have undirected links, but mat1 may not.
def diffArcMatrix(mat1,mat2):
	add,sub,rev,undirected,correct = [0,0,0,0,0]
	for i in range(len(mat1)):
		for j in range(len(mat1)):
			if mat1[i][j] and not mat2[i][j] and not mat2[j][i]: sub += 1
			if mat1[i][j] and not mat2[i][j] and mat2[j][i]: rev += 1
			if mat1[i][j] and mat2[i][j] and mat2[j][i]: undirected += 1
			if mat1[i][j] and mat2[i][j] and not mat2[j][i]: correct += 1
			if i < j and not mat1[i][j] and not mat1[j][i] and (mat2[i][j] or mat2[j][i]): add += 1
	#return (add+sub+2*rev+undirected*1.0/2,add,sub,rev,undirected,correct)
	return {"ed":add+sub+2*rev+.5*undirected,"add":add,"sub":sub,"rev":rev,"und":undirected,"cor":correct}

# Return array of TOMs matching arcs.
def getTOMs( arcs, data ):
	#
	## Enumerate all valid TOMs
	names = data.t.elt.labels
	graph = camml.plugin.tetrad4.Tetrad4FN.arcMatrixToGraph(arcs,names)
	toms = camml.plugin.tetrad4.Tetrad4.enumerateDAGs(data, graph)
	#print "toms.length=",len(toms)
	return toms

mmlCPT = camml.core.search.SearchPackage.mmlCPTLearner
# Parameterize a TOM using supplied arcMatrix and data
# If a valid SEC is given, choose a random DAG from it.
def getParams( rand, arcs, data ):
	try:
		toms = getTOMs(arcs,data)
	except java.lang.RuntimeException:
		print "Invalid SEC, calling fixBrokenParams"
		return fixBrokenParams( rand, arcs, data )
	#
	## Choose a valid TOM at random
	tom = toms[ rand.nextInt(len(toms)) ]
	##
	## Parameterize using MML.
	return tom.makeParameters(mmlCPT)


# Take an inconsistent set of arcs and return a
# DAG that almost matches it.
def fixBrokenParams( rand, arcs, data ):
	tom = camml.core.search.TOM(data)
	tom.randomOrder( rand )
	n = len(arcs)
	for i in range(n):
		for j in range(n):
			if arcs[i][j]: tom.addArc(i,j)
	##
	## Ensure all arcs are facing the correct way
	## This algorithm is ugly, but should work.
	## If an undirected arc is found, we do not try to change it's orientation.
	change = 1
	totalChange = 0
	while change and totalChange < n*n:
		change = 0
		for i in range(n):
			for j in range(n):
				if arcs[i][j] and not arcs[j][i] and tom.isDirectedArc(i,j):
					tom.swapOrder(i,j,1)
					change = 1
		totalChange += change
	#
	if totalChange >= n*n:
		print "Bad Arcs : ", arcs
	##
	## Parameterize using MML.
	return tom.makeParameters(mmlCPT)

# Create a matrix where all arcs from mat0 and mat1 are present
def combineArcMatrix( mat0, mat1 ):
	mat = []
	for i in range(len(mat0)):
		mat.append([])
		for j in range(len(mat0[i])):
			mat[-1].append( mat0[i][j] or mat1[i][j] )
	return mat

# Add implied links to an arc-matrix.
# uses warshall's algorithm.
def addImpliedLinks( mat ):
	for k in range(len(mat)):
		for i in range(len(mat)):
			for j in range(len(mat)):
				mat[i][j] = mat[i][j] or (mat[i][k] and mat[k][j])
	return mat

# Return True if a total ordering exists that is consistent
# with the two arc matrices mat0 and mat1.
def validCommonTotalOrdering( mat0, mat1 ):
	mat = combineArcMatrix( mat0, mat1 )
	mat = addImpliedLinks( mat )
	# If diagonal contains a non-zero element, then
	# a cycle exists in the combined arc matrix.
	for i in range(len(mat0)):
		if mat[i][i] : return False
	return True

# Reurn true if mat constins no cucles
def isAcyclic( mat ):
	return validCommonTotalOrdering( mat, [[False]*len(mat)]*len(mat) )
