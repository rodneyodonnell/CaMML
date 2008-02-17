package cdms.plugin.nnet;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*; 

/**
 * <p>Title: CDMS Neural Network </p>
 * <p>Description: NNet JPanel </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Monash University </p>
 * @author Enes Makalic
 * @version 0.1
 */
public class NNetControlPanel extends JPanel
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 604664405914768256L;

/** Main function for debugging the panel. */
  public static void main(String[] args)
  {
    JFrame f = new JFrame("NNet Control Panel");
    f.getContentPane().add(new NNetControlPanel());
    f.pack();
    f.setVisible(true);
  }

  public NNetControlPanel()
  {
    super(new BorderLayout(10,10)); 

    JPanel title = new JPanel(); /* panel title */
    JPanel trainingM = new JPanel(); /* NNet training methods */
    JPanel nodalTransfer = new JPanel(); /* Nodal transfer function values */
    JPanel panelCB = new JPanel(); /* various checkboxes */
    JPanel weightDecay = new JPanel(); /* weight/decay slider */
    JPanel group = new JPanel(); /* group two panels (slider/checkboxes) */

    /* create layout for slider */
    group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));

    /* create empty border */
    group.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    addTrainingMethods(trainingM); /* create components for the training method panel */
    addNodalValues(nodalTransfer); /* add nodal transfer values */
    addCheckBoxes(panelCB); /* add checkboxes */
    addSlider(weightDecay); /* add slider */

    group.add(weightDecay); /* group two panels */
    group.add(panelCB);
    group.setBorder(BorderFactory.createTitledBorder("Other"));

    add("North", title); /* add title to top panel */
    add("West", trainingM); /* add training method panel to top panel */
    add("Center", nodalTransfer);
    add("East", group); /* add slider/checkboxes to top panel */
  }

  /* Method to create slider with desired options */
  public JSlider createSlider(JSlider js, int min, int max, int initial, int major_t, int minor_t)
  {
    js = new JSlider(JSlider.HORIZONTAL, min, max, initial); /* create slider */

    js.setMajorTickSpacing(major_t); /* setup slider */
    js.setMinorTickSpacing(minor_t);
    js.setPaintTicks(true);
    js.setPaintLabels(true);

    return js; /* return setup slider */
  }

  /* Add slider for weight decay setting */
  public void addSlider(JPanel weightDecay)
  {
    final String text = "Select weight decay factor:";
    final int initial = 0, min = 0, max = 30; /* slider setup values */
    final int major_ticks = 5, minor_ticks = 1;
    JSlider weight = null; /* slider */
    JLabel label = new JLabel(text, JLabel.LEFT); /* label above slider */

    /* create a slider */
    weight = createSlider(weight, min, max, initial,major_ticks,minor_ticks);

    /* create layout for slider */
    weightDecay.setLayout(new BoxLayout(weightDecay, BoxLayout.Y_AXIS));

    /* create empty border */
    weightDecay.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    weightDecay.add(label);  /* add label */
    weightDecay.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    weightDecay.add(weight); /* add slider to panel */
  }

  /* Add checkboxes to panel */
  public void addCheckBoxes(JPanel panelCB)
  {
    final String decay = "Apply decay factor to biases and links";
    final String unity = "Assume output layer derivatives equal one";
    JCheckBox decayCB = new JCheckBox(decay); /* create checkboxes */
    JCheckBox unityCB = new JCheckBox(unity);

    decayCB.setSelected(false); /* default values for checkboxes */
    unityCB.setSelected(false);

    /* Set layout style */
    panelCB.setLayout(new BoxLayout(panelCB, BoxLayout.Y_AXIS));

    panelCB.add(decayCB); /* add checkboxes to panel */
    panelCB.add(unityCB);

    /* create an empty border around panel */
    panelCB.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
  }

  /* Method to add nodal transfer values to panel */
  public void addNodalValues(JPanel nodalTransfer)
  {
    /* Nodal transfer function values  */
    final JLabel label = new JLabel("Select a threshold function:", JLabel.LEFT);
    final String hardLimit  = "Hard limit";
    final String hardLimitPos  = "Hard limit pos";
    final String linear = "Linear";
    final String linearPos = "Linear pos";
    final String saturatingLinear = "Saturating linear";
    final String saturatingLinearPos = "Saturating linear pos";
    final String sigmoid = "Sigmoid";
    final String sigmoidPos = "Sigmoid pos";

    ButtonGroup group = new ButtonGroup();  /* radio buttons */
    JRadioButton hardLimitRB, hardLimitPosRB, linearRB, linearPosRB;
    JRadioButton saturatingLinearRB, saturatingLinearPosRB, sigmoidRB;
    JRadioButton sigmoidPosRB;

    /* Set layout style for training methods section */
    nodalTransfer.setLayout(new BoxLayout(nodalTransfer, BoxLayout.Y_AXIS));

    /* add label above radio buttons */
    nodalTransfer.add(label); /* add label to panel */
    nodalTransfer.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */

    /* create radio buttons */
    hardLimitRB = new JRadioButton(hardLimit);
    hardLimitRB.setActionCommand(hardLimit);
    hardLimitPosRB = new JRadioButton(hardLimitPos);
    hardLimitPosRB.setActionCommand(hardLimitPos);
    linearRB = new JRadioButton(linear);
    linearRB.setActionCommand(linear);
    linearPosRB = new JRadioButton(linearPos);
    linearPosRB.setActionCommand(linearPos);
    saturatingLinearRB = new JRadioButton(saturatingLinear);
    saturatingLinearRB.setActionCommand(saturatingLinear);
    saturatingLinearPosRB = new JRadioButton(saturatingLinearPos);
    saturatingLinearPosRB.setActionCommand(saturatingLinearPos);
    sigmoidRB = new JRadioButton(sigmoid);
    sigmoidRB.setActionCommand(sigmoid);
    sigmoidPosRB = new JRadioButton(sigmoidPos);
    sigmoidPosRB.setActionCommand(sigmoidPos);
    sigmoidPosRB.setSelected(true); /* default training method */

    group.add(hardLimitRB); /* group buttons */
    group.add(hardLimitPosRB);
    group.add(linearRB);
    group.add(linearPosRB);
    group.add(saturatingLinearRB);
    group.add(saturatingLinearPosRB);
    group.add(sigmoidRB);
    group.add(sigmoidPosRB);

    nodalTransfer.add(hardLimitRB); /* add buttons to panel */
    nodalTransfer.add(hardLimitPosRB);
    nodalTransfer.add(linearRB);
    nodalTransfer.add(linearPosRB);
    nodalTransfer.add(saturatingLinearRB);
    nodalTransfer.add(saturatingLinearPosRB);
    nodalTransfer.add(sigmoidRB);
    nodalTransfer.add(sigmoidPosRB);

    /* create an empty border around panel */
    nodalTransfer.setBorder(BorderFactory.createTitledBorder("Threshold function"));
  }

  /* Method to add components to training methods panel */
  public void addTrainingMethods(JPanel trainingM)
  {
    /* NNET training methods */
    final JLabel trainLabel = new JLabel("Select a training method:", JLabel.LEFT);
    final String backProp = "Backpropagation";
    final String batchBackProp = "Batch Backpropagation";
    final String rProp = "Radial Propagation";
    final String levenM = "Levenberg-Marquardt";
    final String text[] = {backProp, batchBackProp, rProp, levenM};

    final JPanel options = new JPanel(); /* create a panel for options */
    JPanel backPropPanel = new JPanel(); /* create panel options foe each method */
    JPanel batchPanel = new JPanel();
    JPanel rPropPanel = new JPanel();
    JPanel levenMPanel = new JPanel();

    options.setLayout(new CardLayout()); /* options for different training methods */

    createBackPropPanel(backPropPanel); /* create panel for backprop options */
    createRPropPanel(rPropPanel); /* create panel for rProp options */
    createLevenMPanel(levenMPanel); /* create panel for Levenberg-Marquardt options */

    options.add(levenMPanel, levenM); /* add panels to cardlayout top panel */
    options.add(backPropPanel, backProp);
    options.add(batchPanel, batchBackProp);
    options.add(rPropPanel, rProp);

    JComboBox methods = new JComboBox(text); /* create combobox */
    methods.setEditable(false); /* combo box non-editable */
    methods.setSelectedIndex(3); /* default is Levenberg-Marquardt */

    methods.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent evt) /* Called every time a different combobox item is selected */
      {
        CardLayout c = (CardLayout) options.getLayout();
        c.show(options, (String) evt.getItem());
      }});

    /* Set layout style for training methods section */
    trainingM.setLayout(new BoxLayout(trainingM, BoxLayout.Y_AXIS));

    /* add label above radio buttons */
    trainingM.add(trainLabel); /* add label to panel */
    trainingM.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    trainingM.add(methods);
    trainingM.add(options);

    /* create an empty border around panel */
    trainingM.setBorder(BorderFactory.createTitledBorder("Traing methods"));
  }

  /* Create options for leven Panel */
  public void createLevenMPanel(JPanel levenMPanel)
  {
    final String text1 = "Select mu increment value:";
    final String text2 = "Select mu decrement factor:";

    final int initial1 = 2, min1 = 0, max1 = 100; /* slider setup values */
    final int major_ticks1 = 10, minor_ticks1 = 5;

    final int initial2 = 7, min2 = 0, max2 = 100;
    final int major_ticks2 = 10, minor_ticks2 = 5;

    JSlider js1 = null, js2 = null; /* sliders */
    JLabel label1 = new JLabel(text1); /* labels above sliders */
    JLabel label2 = new JLabel(text2);

    /* create sliders */
    js1 = createSlider(js1, min1, max1, initial1,major_ticks1,minor_ticks1);
    js2 = createSlider(js2, min2, max2, initial2,major_ticks2,minor_ticks2);

    /* create layout for slider */
    levenMPanel.setLayout(new BoxLayout(levenMPanel, BoxLayout.Y_AXIS));

    /* create empty border */
    levenMPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    levenMPanel.add(label1);  /* add label */
    levenMPanel.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    levenMPanel.add(js1); /* add slider to panel */

    levenMPanel.add(label2);  /* add label */
    levenMPanel.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    levenMPanel.add(js2); /* add slider to panel */
  }

  /* Create options for the rProp panel */
  public void createRPropPanel(JPanel rPropPanel)
  {
    final String text1 = "Select initial delta value:";
    final int initial1 = 1, min1 = 0, max1 = 100; /* slider setup values */
    final int major_ticks1 = 10, minor_ticks1 = 5;
    JSlider js1 = null; /* sliders */
    JLabel label1 = new JLabel(text1); /* labels above sliders */

    /* create sliders */
    js1 = createSlider(js1, min1, max1, initial1,major_ticks1,minor_ticks1);

    /* create layout for slider */
    rPropPanel.setLayout(new BoxLayout(rPropPanel, BoxLayout.Y_AXIS));

    /* create empty border */
    rPropPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    rPropPanel.add(label1);  /* add label */
    rPropPanel.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    rPropPanel.add(js1); /* add slider to panel */
  }

  /* Create options for the backprop panel */
  public void createBackPropPanel(JPanel backPropPanel)
  {
    final String text1 = "Select learning rate:";
    final String text2 = "Select momentum:";

    final int initial1 = 2, min1 = 0, max1 = 100; /* slider setup values */
    final int major_ticks1 = 10, minor_ticks1 = 5;

    final int initial2 = 5, min2 = 0, max2 = 100;
    final int major_ticks2 = 10, minor_ticks2 = 5;

    JSlider js1 = null, js2 = null; /* sliders */
    JLabel label1 = new JLabel(text1); /* labels above sliders */
    JLabel label2 = new JLabel(text2);

    /* create sliders */
    js1 = createSlider(js1, min1, max1, initial1,major_ticks1,minor_ticks1);
    js2 = createSlider(js2, min2, max2, initial2,major_ticks2,minor_ticks2);

    /* create layout for slider */
    backPropPanel.setLayout(new BoxLayout(backPropPanel, BoxLayout.Y_AXIS));

    /* create empty border */
    backPropPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    backPropPanel.add(label1);  /* add label */
    backPropPanel.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    backPropPanel.add(js1); /* add slider to panel */

    backPropPanel.add(label2);  /* add label */
    backPropPanel.add(Box.createRigidArea(new Dimension(0, 5))); /* add some empty space */
    backPropPanel.add(js2); /* add slider to panel */
  }

}

