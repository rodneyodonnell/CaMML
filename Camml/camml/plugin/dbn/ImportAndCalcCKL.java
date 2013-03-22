package camml.plugin.dbn;

import camml.core.models.bNet.BNet;
import camml.plugin.netica.NeticaFn;
import cdms.core.Type;
import cdms.core.Value;

/**Very small wrapper class for importing and calculating KL or CKL between two BNs which are
 * file in Netica format.<br>
 * Basically the same function as Rodo's Jython code for calculating CKL.
 */
public class ImportAndCalcCKL {

	/**
	 * @param trueModelPath The file path/name of the true model.
	 * @param learnedModelPath The file path/name of the learned model.
	 * @param CKLType 0 = KL; 1=CKL1, 2=CKL2, 3=CKL3
	 * @return KL/CKL divergence
	 * @throws Exception
	 */
	public static double calcCKL( String trueModelPath, String learnedModelPath, int CKLType ) throws Exception
	{
		//First: Open the models (py_util.loadNet)
		Value.Structured trueModel = NeticaFn.LoadNet._apply(trueModelPath);
		Value.Structured learnedModel = NeticaFn.LoadNet._apply(learnedModelPath);
		
		//Probably need to reorder the variables in the learned model parameters...
		String[] origNames = ((Type.Structured)((Type.Model)((Type.Structured)trueModel.t).cmpnts[0]).dataSpace).labels;	// o_0
		//System.out.println( Arrays.toString(origNames) );
		
		
		Value.Structured learnedModelReordered = NeticaFn.ReorderNet._apply( origNames, learnedModel );
		//Second: Extract the BNet model, and the parameters...
		BNet bnTrue = (BNet)trueModel.cmpnt(0);
		
		Value.Vector paramsTrue = (Value.Vector)trueModel.cmpnt(1);
		Value.Vector paramsLearned = (Value.Vector)learnedModelReordered.cmpnt(1);
		
		//Third: Calculate KL, CKL
		double CKL_val = bnTrue.ckl(paramsTrue, paramsLearned, CKLType);
		
		return CKL_val;
	}
}
