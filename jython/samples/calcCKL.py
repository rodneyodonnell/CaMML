"""
Script to calculate CKL, KL and Edit Distance between two networks
Usage: jython calcCKL true.dne learned.dne
"""

import sys
import user
sys.path.append(user.home+'/CAMML/jython/lib')

# Import useful libraries
import py_bNet
import py_util


def pyLoadNet( net, nameOrder = None ):
    """
    Load a network and python tuple containing (model,params)
    This differs from the standard loadNet function which returns a CDMS tuple.
    Specifying nameOrder ensures all variables are returned in the same order,
    if variables are NOT in a consistent order, then CKL calculation will be invalid.
    """
    my = py_util.loadNet( net, names=nameOrder )
    return my.cmpnt(0), my.cmpnt(1)


def calcDeltas( trueNet, learnedNet ):
    """
    Calculate CKL divergence and edit distance from the true net to the learned net.  
    Returns a dictionary with keys "KL", "CKL1", "CKL2", "CKL3" and "ED"
    """
    # Load true and learned networks.
    # Ensure that the learned network has the same variable ordering as the true network.
    trueM, trueY = pyLoadNet( trueNet )
    learnedM, learnedY = pyLoadNet( learnedNet, getLabels(trueM) )
	
    retDict = {}
    retDict["KL"] = trueM.ckl( trueY, learnedY, 0 ) 
    retDict["CKL1"] = trueM.ckl( trueY, learnedY, 1 ) 
    retDict["CKL2"] = trueM.ckl( trueY, learnedY, 2 ) 
    retDict["CKL3"] = trueM.ckl( trueY, learnedY, 3 ) 
    retDict["ED"] = py_bNet.editDistance( trueY, learnedY )
    return retDict


def getLabels( bnModel ):
    """
    Extract the labels (i.e variable names) from the given network
    """
    modelType = bnModel.t
    return modelType.dataSpace.labels


# Calculate CKL divergence
if __name__ == "__main__":
    import sys
    if len( sys.argv ) == 3:
        trueNet, learnedNet = sys.argv[1], sys.argv[2]
        delta = calcDeltas( trueNet, learnedNet )
        for k in ["KL", "CKL1", "CKL2", "CKL3", "ED"]:
            print k, '\t=\t', delta[k]
    else:
        # print usage instructions
        print __doc__  
    
