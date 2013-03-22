/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
