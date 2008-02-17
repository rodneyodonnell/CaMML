{
  CDMS

  Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
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
  C5Config = install ("cdms.plugin.c5.C5",()), 
  CammlModelConfig = install ("camml.models.Models",()), 
  
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
      ("C5 Data",["data","names"],identity,C5.loadC5file,
       lambda x . Desktop.show ("C5 Data",DEFAULTFN x)),
      ("Lambda Script",["*"],identity,identity,
       lambda x . let so = CreateScriptWindow (fst x) in so.load (fst x)),
      ("Delimited Text",["*"],Dialog.loadDelimitedFileGui,
       lambda x . (fst x,IO.loadDelimitedFile x),
       lambda x . Desktop.show (fst x,DEFAULTFN (snd x)))
    ],
  MenuAdd = lambda X . (),


  { Lambda Script }
  LambdaToolbar = 
   [ 
     ("Load Script","Load a script.","cdms/plugin/desktop/images/Open24.gif",
      lambda X . X.load ""),
     ("Save Script","Save script.","cdms/plugin/desktop/images/Save24.gif",
      lambda X . X.save ""),
     ("Run the script","Run the script.","cdms/plugin/desktop/images/Play24.gif",
      lambda X . Desktop.show ("Script results",(Fpli.interpreter (X.getEditorText ())))),
     ("Syntax and type checker","Run the syntax and type checker.",
      "cdms/plugin/desktop/images/FastForward24.gif",
      lambda X . Desktop.show ("Script check", (Fpli.checker (X.getEditorText ())))),
     ("Help for Lambda Script","Help for Lambda Script.",
      "cdms/plugin/desktop/images/About24.gif",
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
                  "cdms/plugin/desktop/images/Open24.gif",MenuLoadData),
                 ("-","","","",()),
                 ("Exit","","Exit.","",MenuExit) ]),
  arrangeMenu = ("Arrange","noimage","","", 
               [ ("Rows","","","cdms/plugin/desktop/images/AlignJustifyHorizontal24.gif",
                  Desktop.arrangeRows),
                 ("Columns","","","cdms/plugin/desktop/images/AlignJustifyVertical24.gif",
                  Desktop.arrangeColumns) ] ),
  helpMenu = ("Help","noimage","","", 
    [ ("About","","About CDMS.","cdms/plugin/desktop/images/About24.gif",MenuAbout) ] ),

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
    "cdms/plugin/desktop/images/Open24.gif",MenuLoadData),
   ("Script","Scripting window.","cdms/plugin/desktop/images/History24.gif",MenuScript),
   ("Arrange rows","Arrange rows.",
    "cdms/plugin/desktop/images/AlignJustifyHorizontal24.gif",Desktop.arrangeRows),
   ("Arrange cols","Arrange columns.",
    "cdms/plugin/desktop/images/AlignJustifyVertical24.gif",Desktop.arrangeColumns),
   ("Help","About CDMS.","cdms/plugin/desktop/images/About24.gif",MenuAbout) ],
  
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
  ApplyMenu = ("Apply Function", "Apply Function", "Select Function to Apply...", "", 
               Dialog.apply),
  PrintMenu = ("Print...","","","cdms/plugin/desktop/images/Print24.gif",Desktop.printobj),
  HelpMenu = ("Help","","","cdms/plugin/desktop/images/Help24.gif",
              lambda x . Desktop.show ("Help",Desktop.help x)),
  PlotMenu = ("Graph","Graph","Graph","",[("Plot","Plot","Plot","",TwoDPlot.plot),
              ("Histogram","","","",TwoDPlot.histogram)]),
  FormatMenu = ("Format Data", "Format Data", 
                "Format data into (output columns, input columns)", "", Formatter),
  ENVMENU = [ BrMenu, AddMenu, ApplyMenu, PlotMenu, PrintMenu, FormatMenu, HelpMenu ],

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

