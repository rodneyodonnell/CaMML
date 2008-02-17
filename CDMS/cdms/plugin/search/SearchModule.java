package cdms.plugin.search;

import cdms.core.*;

public class SearchModule extends Module
{
  public static java.net.URL helpURL = Module.createStandardURL(SearchModule.class);

  public String getModuleName() {return "Search";}
  public java.net.URL getHelp() {return helpURL;}
  public void install(Value params) throws Exception
  {
    Environment.env.add("xval","Search", XValidation.xValidation, "A cross-validation data formatter.  folds -> [data] -> [([train], [test])]");
  }
}
