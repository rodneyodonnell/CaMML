{
  CaMML Initialisation script.
  useage: java cdms.core.Cdms cdms.plugin.fpli.Fpli\$Interpreter cammlBootstrap.fp

  Based on CDMS init script (cdms.fp)
}

{ Initialise Environment }

let

  { Splash }
  splash = let splashObj = co "cdms.plugin.desktop.Splash" in splashObj,
  showSplash = splash.toString (),   { Force the splash screen to be displayed. }

  { Install Modules }
  install = cv "cdms.core.Module$InstallModule",

  FNConfig = install ("cdms.core.FN",()),
  VectorConfig = install ("cdms.core.VectorFN",()),
  ModelFNConfig = install ("cdms.core.ModelFN",()),
  IOConfig = install ("cdms.core.IO",()),
  FpliConfig = install ("cdms.plugin.fpli.Fpli",()),
  DialogConfig = install ("cdms.plugin.dialog.Dialog",()),
  TwoDPlotConfig = install ("cdms.plugin.twodplot.TwoDPlot",()),
  DesktopConfig = install ("cdms.plugin.desktop.Desktop",()),
  MMLConfig = install ("cdms.plugin.mml87.Mml87",()),
  ModelConfig = install ("cdms.plugin.model.Model",()),
  LatexConfig = install ("cdms.plugin.latex.Latex",()),
  SearchConfig = install ("cdms.plugin.search.SearchModule",()),
  C5Config = install ("cdms.plugin.c5.C5",()),

  {----------- CAMML SPECIFIC MODULES ----------}
  CammlSearchPackage = install ("camml.core.search.SearchPackage",()),
  CammlModels = install ("camml.core.models.Models",()),

  {NeticaFN = install ("camml.plugin.netica.Netica", ()),}
  Rodo = install ("camml.plugin.rodoCamml.RodoCamml",()),
  Weka = install ("camml.plugin.weka.Weka", ()),
  RMI = install ("camml.plugin.rmi.CDMSEngine$EngineModule",()),  
  LibraryConfig = install ("camml.core.library.Library", ()),
  {FriedmanWrapper = install ("camml.plugin.friedman.FriedmanWrapper", ()),}
  {TetradWrapper = install ("camml.plugin.tetrad4.Tetrad4Module", ()),}

  AugmentFN = install ("camml.plugin.augment.Augment", ()),

 
  DEFAULTFN = lambda X . let 
    br = co "cdms.plugin.desktop.Browser", 
    brcall = br.setSubject X, 
    brset = br.setEnvMenu ENVMENU 
  in br,

  Formatter = cv "cdms.plugin.desktop.Formatter$FormatterFunction",

{ 
   ******************************************************************************
    Menus
    
    type Menu = (Name,shortDesc,longDesc,ImageFile,[Menu]|ActionFunction|TRIV)
    Accelerators still need to be implemented.
    If Name == "-" then a separator is used. 
}

  MenuLoadData = lambda X . Dialog.loadFileDialog 
    [ 
      ("Genebank DNA",["*"],identity,
       lambda x . (fst x,IO.loadGeneBankFile (fst x)),
       lambda x . Desktop.show (fst x,DEFAULTFN (snd x))),
      ("C5 Data",["data","test"],identity,C5.loadC5file,
       lambda x . Desktop.show ("C5 Data",DEFAULTFN x)),
      ("Lambda Script",["*"],identity,identity,
       lambda x . let so = CreateScriptWindow (fst x) in so.load (fst x)),
      ("oldCamml", ["cas"], identity, lambda x . RodoCamml.load (fst x),
	lambda x . Desktop.show ("CaMML Data",DEFAULTFN x)),

      ("weka", ["arff"], identity, lambda x . Weka.load (fst x),
	lambda x . Desktop.show ("CaMML Data",DEFAULTFN x)),
      ("weka(discretize & replace missing)", ["arff"], identity, 
	lambda x . Weka.loadMissingDiscretize (fst x),
	lambda x . Desktop.show ("CaMML Data",DEFAULTFN x)),

      ("Delimited Text",["*"],Dialog.loadDelimitedFileGui,
       lambda x . (fst x,IO.loadDelimitedFile x),
       lambda x . Desktop.show (fst x,DEFAULTFN (snd x)))
    ],
  MenuAdd = lambda X . (),


  { Lambda Script }
  LambdaToolbar = 
   [ 
     ("Load Script","Load a script.","images/Open24.gif",
      lambda X . X.load ""),
     ("Save Script","Save script.","images/Save24.gif",
      lambda X . X.save ""),
     ("Run the script","Run the script.","images/Play24.gif",
      lambda X . Desktop.show ("Script results",(Fpli.interpreter (X.getEditorText ())))),
     ("Syntax and type checker","Run the syntax and type checker.",
      "images/FastForward24.gif",
      lambda X . Desktop.show ("Script check", (Fpli.checker (X.getEditorText ())))),
     ("Help for Lambda Script","Help for Lambda Script.",
      "images/About24.gif",
      lambda X . let v = Desktop.viewhtml "<h1>Help</h1>" in Desktop.show ("Lambda Scrip Help",v) ) 
   ],

  CreateScriptWindow = lambda x . let 
    so = co "cdms.plugin.desktop.ScriptWindow",
    z = so.setToolbar LambdaToolbar,
    show = Desktop.show (x,so)
  in so,

  MenuScript = lambda X . CreateScriptWindow "Lambda Script",

  MenuExit = lambda X . Desktop.dispose (),
  
  MenuAbout = lambda X . let
    v = Desktop.viewhtml (loadText "cdms/plugin/desktop/help.html")
  in Desktop.show ("Help",v),

  fileMenu = ("File","","","noimage", 
               [ ("Load Data...","","Load data from a text file.",
                  "images/Open24.gif",MenuLoadData),
                 ("-","","","",()),
                 ("Exit","","Exit.","",MenuExit) ]),
  arrangeMenu = ("Arrange","noimage","","", 
               [ ("Rows","","","images/AlignJustifyHorizontal24.gif",
                  Desktop.arrangeRows),
                 ("Columns","","","images/AlignJustifyVertical24.gif",
                  Desktop.arrangeColumns) ] ),
  helpMenu = ("Help","noimage","","", 
    [ ("About","","About CDMS.","images/About24.gif",MenuAbout) ] ),

  MENU = [ fileMenu, arrangeMenu, helpMenu ],

  { ***************************************************************************** }



  {
    ******************************************************************************
    Toolbar 
  
    data Toolbar = [(shortDesc,longDesc,ImageFile,ActionFunction)]

    if shortDesc == "-" then a separator is used.
  } 
  TOOLBAR = [
   ("Load Data","Load data from a text file.",
    "images/Open24.gif",MenuLoadData),
   ("Script","Scripting window.","images/History24.gif",MenuScript),
   ("Arrange rows","Arrange rows.",
    "images/AlignJustifyHorizontal24.gif",Desktop.arrangeRows),
   ("Arrange cols","Arrange columns.",
    "images/AlignJustifyVertical24.gif",Desktop.arrangeColumns),
   ("Help","About CDMS.","images/About24.gif",MenuAbout) ],
  
  { ****************************************************************************** }

	{ Add some useful functions to the environment. }

	f0 = addToEnvironment (lambda x . cmpnt 0 (elt x 0), 
			"extract", "Extract a single value from a vector of structures"),

	f1 = addToEnvironment ( lambda x . let
			my = CammlSearchPackage.getRepresentativeDAG x,
			model = cmpnt 0 my,
			params = cmpnt 1 my
		in CammlModels.visualiseInferenceBNet model params (),
		"ShowNet", "Show the best representative tom of MMLEC, SEC or DAG"),
	{
	f2 = addToEnvironment ( lambda x . let
			my = CammlSearchPackage.getRepresentativeDAG x,
			bNet = cmpnt 0 my,
			params = cmpnt 1 my
		in Netica.saveNet (Dialog.saveFileChooser [".dne",".dnet"],bNet,params),
		"SaveNet", "Show the best representative tom of MMLEC, SEC or DAG as a netica file"),
        }

	{ -- ADD Shortcut options for running BNetLearner with various options --}
	f4 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false), ("clean", false), ("joinSECs", false)]) () data data
		in cmpnt 3 result,
	"cammlUncleanNoJoin", "Run CaMML with cleaning and joining disabled."),

	f5 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false), ("joinSECs", false)]) () data data
		in cmpnt 3 result,
	"cammlNoJoin", "Run CaMML with joining disabled"),


	f5 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false), ("cklJoinType", 3), ("joinDAGs",false)]) () data data
		in cmpnt 3 result,
	"cammlCKLJoin", "Run CaMML with joining by CKL3"),


	f5 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false), ("mmlLearner",mmlIntCPT.learnerStruct)]) () data data
		in cmpnt 3 result,
	"cammlInt", "Run CaMML with intervention models"),

	f5 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false), ("mmlLearner",mmlIntCPT.learnerStruct), ("cklJoinType", 3), ("joinDAGs",false)]) () data data
		in cmpnt 3 result,
	"cammlIntCKL", "Run CaMML with intervention models and CKL joining"),

	f5 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false), ("mmlLearner",mmlIntCPT.learnerStruct), ("mlLearner",mlIntCPT.learnerStruct), ("cklJoinType", 3)]) () data data
		in cmpnt 3 result,
	"cammlIntMLCKL", "Run CaMML with intervention models and CKL joining"),


	f6 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mix", false)]) () data data
		in cmpnt 3 result,
	"camml", "Run CaMML with default options"),

	makeCPTLearner = lambda multinomialOptions . let
		multiLearner = CammlModels.makeMultinomialLearner multinomialOptions
	in CammlModels.makeCPTLearner [("leafLearner",multiLearner)],

	makeDTreeLearner = lambda multinomialOptions . let
		multiLearner = CammlModels.makeMultinomialLearner multinomialOptions
	in CammlModels.makeForcedSplitDTreeLearner [("leafLearner",multiLearner)],

	multinomialLearnerMML5 = CammlModels.makeMultinomialLearner [("adaptive",true),("bias",0.5),("useMML",true)],
	cptLearnerMML5 = makeCPTLearner [("adaptive",true),("bias",0.5),("useMML",true)],
	dTreeLearnerMML5 = makeDTreeLearner [("adaptive",true),("bias",0.5),("useMML",true)],

	{ -- Create a multiLearner which chooses between Multinomial, CPT, and DTree learners -- }
	{ -- Our prior asserts to use a multinomial with 0 parents, CPT with 1 parent and     -- }
	{ -- give CPT and DTree equal waiting for more parents.                               -- }
	multiPriorFN = lambda ixz . let 
		i = cmpnt 0 ixz,
		x = cmpnt 1 ixz,
		z = cmpnt 2 ixz,
		numParents = length (elt z 0)
	in if (numParents = 0) then [1,0,0] 
		else if (numParents = 1) then [0,1,0] 
		else [0,0.5,0.5],
	multiLearnerMML5 = CammlModels.makeMultiLearner [("learner", [multinomialLearnerMML5,cptLearnerMML5,dTreeLearnerMML5]), ("prior",multiPriorFN)],

	f7 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mmlLearner",dTreeLearnerMML5)]) () data data
		in cmpnt 3 result,
	"cammlDTree", "Run camml using Decision Trees to represent local structure"),

	f8 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mmlLearner",multiLearnerMML5)]) () data data
		in cmpnt 3 result,
	"cammlDual", "Run camml with dual models (DTree/CPT) to represent local structure."),

	f9 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mmlLearner",LogitLearner.learnerStruct)]) () data data
		in cmpnt 3 result,
	"cammlLogit", "Run camml with dual models (DTree/CPT) to represent local structure."),

	f9 = addToEnvironment (
		lambda data . let
			result = cmpnt 0 (CammlModels.makeBNetLearner [("fullResults",true), ("mmlLearner",dualCTL.learnerStruct)]) () data data
		in cmpnt 3 result,
	"cammlDualCTL", "Run camml with dual models (DTree/CPT) to represent local structure."),


  { ****************************************************************************** }


  {
    ******************************************************************************
    Environment Menus 
  
    data Menu = (Name,shortDesc,longDesc,ImageFile,[Menu]|ActionFunction|TRIV)
    data MainMenu = [Menu]
    data Toolbar = [(shortDesc,longDesc,ImageFile,ActionFunction)]

    if shortDesc == "-" then a separator is used.
  } 

  BrMenu = ("View/Derive","View/Derive","View/Derive value.","",
            lambda x . Desktop.show ("View",DEFAULTFN x)),
  AddMenu = ("Add/Copy","Add Value to Environment.","Add Value to Environment.","", 
             Dialog.addToEnvironmentDialog),
  ViewTypeMenu = ("View Type","","","Display the type information for this value",
                  Dialog.viewTypeFN),
  ApplyMenu = ("Apply Function", "Apply Function", "Select Function to Apply...", "", 
               Dialog.apply),
  PrintMenu = ("Print...","","","images/Print24.gif",Desktop.printobj),
  HelpMenu = ("Help","","","images/Help24.gif",
              lambda x . Desktop.show ("Help",Desktop.help x)),
  PlotMenu = ("Graph","Graph","Graph","",[("Plot","Plot","Plot","",TwoDPlot.plot),
              ("Histogram","","","",TwoDPlot.histogram)]),
  FormatMenu = ("Format Data", "Format Data", 
                "Format data into (output columns, input columns)", "", Formatter),
  ExtractMenu = ("Extract","","","",
              lambda x . Desktop.show ("Extract", extract x)),
  ENVMENU = [ BrMenu, AddMenu, ApplyMenu, ViewTypeMenu, PlotMenu, PrintMenu, FormatMenu, ExtractMenu, HelpMenu ],

  { ****************************************************************************** }

  test = let
    succ = lambda n . n+1   {successor function},
    fact = lambda n . if n<=0 then 1 else n*fact(n-1),
    first = lambda n . lambda L . if n<=0 then nil else hd L : first (n-1) tl L,
    ones = 1 : ones    {an infinite list!},

    step = lambda n .                      { return every n_th element of a List }
      let rec s = lambda m . lambda L .
        if null L then nil
          else if m <= 1 then hd L : step n tl L else s (m-1) tl L
      in s n,

    gen = lambda n . lambda f .                    { n : f(n) : f(f(n)) : ... }
      let rec ns = n : gen (f n) f
      in ns

  in fact 5 : map succ (iota 10) : makeVector (step 2 (first 10 ones)) : nil


in 

  [ true, false, makeVector test, 
    Desktop.setMainMenu MENU,
    Desktop.setToolBar TOOLBAR,
    Desktop.setEnvMenu ENVMENU,
    Desktop.setValueViewer DEFAULTFN,
    Desktop.setTitle "Core Data Mining Software (CDMS) Version 1", 
    Desktop.setVisible true, splash.dispose () ]

