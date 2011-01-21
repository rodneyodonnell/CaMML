//
// TODO: 1 line description of LogitLearner.java
//
// Copyright (C) 2006 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: LogitLearner.java
// Author: rodo@dgs.monash.edu.au

package camml.core.models.logit;

import camml.core.models.ModelLearner.DefaultImplementation;
import camml.core.models.bNet.BNetLearner;
import camml.core.models.cpt.CPTLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.Value.Model;
import cdms.core.Value.Structured;
import cdms.core.Value.Vector;

/**
 * TODO: Multi line description of LogitLearner.java
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.4 $ $Date: 2006/08/22 03:13:30 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/core/models/logit/LogitLearner.java,v $
 */

public class LogitLearner extends DefaultImplementation {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 8756964567398266052L;
    final public static LogitLearner logitLearner = new LogitLearner();
    public final static BNetLearner logitBNetLearner = new BNetLearner(CPTLearner.mlMultinomialCPTLearner, logitLearner,false,true);
    
    /**
     * @param modelType
     * @param iType
     */
    public LogitLearner() {    super(Type.MODEL, Type.TRIV); }

    /** Parameterize and cost data all in one hit.   */
    public double parameterizeAndCost( Value i, Value.Vector x, Value.Vector z )
        throws LearnerException
    {        
        // Shortcut: If x.arity == 1, then the model/data cost nothing to state.
        Type.Discrete xType = (Type.Discrete)((Type.Vector)x.t).elt;
        if ( xType.UPB == xType.LWB ) { return 0; }

        JulesLogit logit = new JulesLogit();
        logit.nodeCost(x,z);        
        return logit.getMMLCost();
    }

    
    /** Use JulesLogit to parameterize model. */
    public Structured parameterize(Value i, Vector x, Vector z)
        throws LearnerException {
        
        JulesLogit logit = new JulesLogit();
        logit.nodeCost(x,z);
        Value.Structured y = logit.getParams();
        Value.Model m = Logit.logit;
        Value s = m.getSufficient(x,z);
        return new Value.DefStructured(new Value[] {m,s,y} );
    }
    
    /** calls parameterize(i,x,z) */
    public Structured sParameterize(Model m, Value stats)
        throws LearnerException {
        Value.Vector s = (Value.Vector)stats;
        return parameterize(Value.TRIV,(Value.Vector)s.cmpnt(0),(Value.Vector)s.cmpnt(1));
    }

    /** Not implemented */
    public double sCost(Model m, Value stats, Value params)
        throws LearnerException {
        throw new RuntimeException("Not implemented");
    }


    public String getName() {
        return "LogitLearner";
    }

}
