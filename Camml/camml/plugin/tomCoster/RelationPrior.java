package camml.plugin.tomCoster;

import java.io.Serializable;

import camml.core.search.TOM;

public interface RelationPrior extends Serializable {

    /** Update default priors */
    public abstract void setArcP(double arcP);

    /** Set prior of given type. */
    public abstract void setP(String type, double prob);

    /** setP with operation reversed. setP2("->",.9) == setP("<-",.9), etc. */
    public abstract void setP2(String type, double prob);

    /** Return probability of relationship between nodeI and nodeJ in the given tom. */
    public abstract double relationProb(TOM tom);

    /** Return log probability of relationship betwee nodeI and nodeJ in the given tom.. */
    public abstract double relationCost(TOM tom);

}
