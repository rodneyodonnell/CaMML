from py_cdms import *
import os

def loadData(fName, check=True):
	if os.path.isfile(fName):
		if fName.endswith(".cas"): return camml.plugin.rodoCamml.RodoCammlIO.load(fName)
		elif fName.endswith(".arff"): return camml.plugin.weka.Converter.load(fName,True,True)
		else: raise Exception("Unknown file type.")
	else:
		if check: raise Exception("File not found" + fName)
		else: return None
	
def saveData(fName, data, overwritable = False):
	if os.path.isfile(fName) and (overwritable == False): raise Exception("File already exists : " + fName)
	if fName.endswith(".cas"): camml.plugin.rodoCamml.RodoCammlIO.store(fName,data,0)
	elif fName.endswith(".arff"): raise Exception(".arff export not implemented")
	else: raise Exception("Unknown file type. : " + fName )


def loadNet(fName, check=True,names=None):
	if os.path.isfile(fName):
		my = camml.plugin.netica.NeticaFn.LoadNet._apply(fName)
		if not warn[loadNet]:
			print "Warning: loadNet bahaviour changed. reorder no longer called."
			warn[loadNet] = True
		#names = map(lambda i: "var"+str(i),range(my.cmpnt(1).length()))
		if (names != None):
			my = camml.plugin.netica.NeticaFn.ReorderNet._apply(names,my)
		return my
	else:
		if check: raise Exception("File not found" + fName)
		else: return None

warn[loadNet] = False

# Save an netice network, accepts my or msy as argument.
def saveNet(fName, my, overwritable=False):
	if os.path.isfile(fName) and (overwritable == False): raise Exception("File already exists : " + fName)
	camml.plugin.netica.NeticaFn.SaveNet.saveNet._apply(fName,my.cmpnt(0),my.cmpnt(my.length()-1))
			
def saveNet2(fName, m, y, overwritable=False):
	"Save a netica newtowk given a model/parameter pair"
	if os.path.isfile(fName) and (overwritable == False): raise Exception("File already exists : " + fName)
	camml.plugin.netica.NeticaFn.SaveNet.saveNet._apply(fName,m,y)
			
# Load object from disk, expect a gzipfile if gzip==True
def loadObject(fName, check=True, gzip=False):
	if gzip: fName = fName + ".gz"
	if os.path.isfile(fName):
		from java import io
		fs = io.FileInputStream(fName)
		if gzip: fs = java.util.zip.GZIPInputStream(fs)
		ins=io.ObjectInputStream(fs)
		x=ins.readObject()
		ins.close()
		return x
	else:
		if check: raise Exception("File not found" + fName)
		else: return None

# Save object to disk, compress it if gzip=True
def saveObject(fName,x,overwritable=False, gzip=False):
	if gzip: fName = fName + ".gz"
	if os.path.isfile(fName) and (overwritable == False): raise Exception("File already exists : " + fName)
	from java import io
	fs = io.FileOutputStream(fName)
	if gzip: fs = java.util.zip.GZIPOutputStream(fs)
	outs=io.ObjectOutputStream(fs)
	outs.writeObject(x)
	outs.close()
		

## Taken from http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/82465
def mkdir2(newdir):
 	if os.path.isdir(newdir):
		pass
	elif os.path.isfile(newdir):
		raise OSError("a file with the same name as the desired " \
					  "dir, '%s', already exists." % newdir)
	else:
		head, tail = os.path.split(newdir)
		if head and not os.path.isdir(head):
			mkdir2(head)
		if tail:
			os.mkdir(newdir)

## Padd (or truncate) a string to the given length.
def pad( s, n):
	return (s+" "*n)[:n]
