###############################
## Jython interface to CaMML ##
## Rodney O'Donnell 30/5/05  ##
###############################
import java
import camml
import cdms
import math

## These don't seem to be defined in jython??
True,False = 1,0

# Shortcut to run code from cdms' fpli language
cml = camml.plugin.scripter.CammlPlugin.runCammlCommand._apply


## List of models required.
modules = [
	  	"cdms.core.FN",
		"cdms.core.VectorFN",
		"cdms.core.ModelFN",
		"cdms.plugin.latex.Latex",
	
		"cdms.plugin.mml87.Mml87",
		"cdms.plugin.model.Model",

		"camml.core.search.SearchPackage",
		"camml.core.models.Models",
#		"camml.plugin.netica.Netica",
		"camml.plugin.friedman.FriedmanWrapper",
		"camml.plugin.weka.Weka",
		"camml.plugin.rodoCamml.RodoCamml",
		"camml.core.library.Library",

		"camml.plugin.rmi.CDMSEngine$EngineModule",
		"camml.plugin.augment.Augment",
		"camml.core.models.bNet.BNetFN" ]

# Install all modules listed in modules[]
install = cml( "cv \"cdms.core.Module$InstallModule\"" ).apply
for x in modules:
	install( cml( "(\"" + x + "\", () )" ) )


# Define triv
triv = cml( "()" )

# Return a vector full of triv.
def trivVec(n):
	return cdms.core.VectorFN.UniformVector( n, triv )

# convert a 2d array (of ints) from cdms to python
def cdms2python(a):
	x = []
	for i in range(a.length()):
		xx = []
		for j in range(a.elt(i).length()):
			xx.append( a.elt(i).intAt(j) )
		x.append( xx )
	return x

# convert a 2d array (of ints) from python to cdms
def python2cdms(a):
	x = []
	for i in a:
		x.append( cdms.core.VectorFN.FastDiscreteVector(i) )
	return cdms.core.VectorFN.FatVector(x)

# Dictionary flagging which warnings have been printed.
# if warn[x] == true, warning x has been printed.
warn = {}
