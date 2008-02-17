###############################
## Jython interface to CaMML ##
## Rodney O'Donnell 30/5/05  ##
###############################
import java
import camml
import cdms
import math

from py_cdms import *
from py_bNet import *

cluster = True

# Initialise rmap server
if cluster:
	try:
		rmap = camml.plugin.rmi.CDMSEngine.rmap.apply( cml("()") )
	except java.lang.RuntimeException:
		cluster = False
		print "RuntimeException: Setting cluster = False"

else:
		nameVec = cml( str(["rmi://localhost:1099/CalculatorService"]) )
		rmap = camml.plugin.rmi.CDMSEngine.rmap.apply( nameVec )


## Create a BNetLearner with given options
makeBNetLearner = lambda x: camml.core.models.bNet.BNetLearner.MakeBNetLearner().apply(x).cmpnt(0) 

annealFN = makeBNetLearner( cml(""" [("searchType","Anneal")] """) )
metrololisFN = makeBNetLearner( cml(""" [("searchType","Metropolis")] """) )


## Apply each element of tupple succesively.
## Useful for running searches.
## Returns a (model,params) pair.
tupleFN = cml(
	"""
	lambda fn . lambda xx . let
	msy = fn (cmpnt 0 xx) (cmpnt 1 xx) (cmpnt 2 xx)
	in (cmpnt 0 msy, cmpnt 2 msy)
	""" )



######################################################
# Parameterize a single dataset on the local machine #
######################################################
# r = java.util.Random(123)
# my = makeNet(r,[2,2,2,2],0.5,1.0)
# x,z = makeData( r, (my.cmpnt(0),my.cmpnt(1)), 100 )
#
## parameterize using (x,z)
# struct = cdms.core.Value.DefStructured( [triv, x, z] )
# print tupleFN.apply( annealFN ).apply( struct )
######################################################

#################################################################
# Generate 5 datasets and parameterize them on remote machines. #
#################################################################
# r = java.util.Random(123)
#
# data = []
# for i in range(5):
# 	my = makeNet(r,[2,2,2,2],0.5,1.0)
#	x,z = makeData( r, (my.cmpnt(0),my.cmpnt(1)), 100 )
#	struct = cdms.core.Value.DefStructured( [triv, x, z] )
#	data.append( struct )
# data = cdms.core.VectorFN.FatVector( data )
#
# x1 = rmap.apply( tupleFN.apply( annealFN ) )
# x2 = x1.apply( data )
#
# print x2
##################################################################
	

## Apply a fn to values in valArray
def rmap1( fn, valArray ):
	return rmap.apply(fn).apply(valArray)


## Apply various functions to a single value
def rmap2( fnArray, val ):
	# Use a camml lambda script to reverse order of variables.
	return rmap.apply( cml("lambda v . lambda fn . fn v").apply(val) ).apply( fnArray )


## map a vector of functionf over a vector of values.
## An array is returned indexed by [fn][val]
def rmap3( fnArray, valArray ):	
	# Use a camml lambda script to reverse order of variables.
	structArray = []
	for fn in fnArray:
		for v in valArray:
			structArray.append( cdms.core.Value.DefStructured( [fn,v] ) )
	structVec = cdms.core.VectorFN.FatVector( structArray )
	retVec = rmap.apply( cml("lambda x . (cmpnt 0 x) (cmpnt 1 x)") ).apply( structVec )

	retArray = []
	ii=0
	for i in range(len(fnArray)):
		temp = []
		for j in range(len(valArray)):
			temp.append( retVec.elt(ii) )
			ii = ii+1
		retArray.append( temp )
	return retArray



#class FNClass1( cdms.core.Value.Function ):
#	def __init__(self):
#		t = cdms.core.Type.FUNCTION
#		cdms.core.Value.Function.__init__(self,t)
#	def apply(self,v):
#		print v.t
#		return v
#
#fnClass1 = FNClass1()
#
#
#print fnClass1.apply( cml("\"abc\"") )
#
#x1 = rmap.apply(fnClass1)
#x2 = x1.apply(a)
