package camml.plugin.dataCoster;

import java.util.Random;

import camml.core.search.AnnealSearch;
import camml.core.search.SearchPackage;
import camml.plugin.netica.NeticaFn;
import camml.plugin.weka.Converter;
import cdms.core.Type;
import cdms.core.Value;


/**
  * Class to load a .dne file and calculate the MML cost of a given dataset.
  */
public class DataCoster {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java camml.plugin.dataCoster.DataCoster modelfile.dne datafile.arff");
            return;
        }

        String modelfile = args[0];
        String datafile = args[1];

        // Load data from .arff file
        Value.Vector data = Converter.load(datafile, true, true);
        Type.Structured eltType = (Type.Structured)((Type.Vector)data.t).elt;
        System.out.println(String.format("Loaded %d variables, %d elements from %s", eltType.labels.length, data.length(), datafile));

        // We need a BNetSearch object to do the costing.
        Random rand = new Random();
        AnnealSearch annealSearch = new AnnealSearch(rand, data,  SearchPackage.mlCPTLearner, SearchPackage.mmlCPTLearner );

        // BY default we end up with an empty TOM, let's calculate the MML cost.
        double emptyCost = annealSearch.costNetwork(SearchPackage.mmlCPTLearner, false);
        System.out.println(String.format("Cost with no arcs : %s", emptyCost));
        annealSearch.printDetailedCost(annealSearch.getTOM());

        // Now we want to load a .dne from a file and calculate the cost.
        // Hopefully this works ... I don't have the 64 bit version of netica set up on my machine to test it.
        try {
            Value.Structured my = NeticaFn.LoadNet._apply(modelfile);
            Value.Vector params = (Value.Vector)my.cmpnt(1);
            System.out.println("Setting TOM Structure");
            annealSearch.getTOM().setStructure(params);
        } catch (UnsatisfiedLinkError e) {
            System.out.println("## ERROR: Couldn't load Netica libs.  Retaining original structure.");
        }
        annealSearch.printDetailedCost(annealSearch.getTOM());

        // Otherwise we can just add/remove arcs and set the order manually.
        annealSearch.getTOM().addArc(0,3);
        annealSearch.getTOM().addArc(0,2);
        annealSearch.printDetailedCost(annealSearch.getTOM());

        annealSearch.getTOM().swapOrder(0,3,true);
        annealSearch.printDetailedCost(annealSearch.getTOM());


    }
}
