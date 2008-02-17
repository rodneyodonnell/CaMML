// File: NNetInt.java
// Class: NNetInt
// Authors: {deant,leighf,joshc}@csse.monash.edu.au

package cdms.plugin.nnet;

import java.io.*;
import cdms.core.*;
import cdms.core.Type.Scalar;

public final class NNetInt implements Serializable
{
/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1L;
//  private NNetSearch search = null;      // pointer to a search to be notified after each epoch by the onEpochComplete() callback method.
  private transient long objPtr;         // pointer to the underlying C++ object (stored as a long, in case 64-bit addressing is being used)
  private int inputVectorLength, outputVectorLength;  // lengths of input/output vectors, IFF vector types are specified (otherwise ignored)
  private Type inputType, outputType;    // data types that the net expects as input and output;  not all types are allowed:
                                         //
                                         // inputType can be any subclass of the "primitive" type Type.Scalar as long as it has upper and lower bounds and is either noncyclic
                                         // or unordered or symbolic; or it can be a Type.Vector of such; or it can be a Type.Structured.  In the latter case, only elements of
                                         // the structure that satisfy the above requirements are used - all other types, and nested structures and vectors are ignored.
                                         //
                                         // outputType can be any subclass of the "primitive" type Type.Discrete as long as has upper and lower bounds
                                         // or it can be a Type.Vector of such; or it can be a Type.Structured.  In the latter case, only elements of
                                         // the structure that satisfy the above requirement are used - all other types, and nested structures and vectors are ignored.


  // Constructors
  public NNetInt(Type inType, int inLength, Type outType, int outLength, int[] hiddenLayerSizes, boolean createLinks)  // inLength and outLength are lengths of input and output vectors, respectively IFF input/output types are Type.vector (otherwise ignored)
  {
    if (inType instanceof Type.Scalar)
    {
      if (badScalarInputType((Type.Scalar) inType))
        throw new IllegalArgumentException("Neural Net input type must have both upper and lower bounds, and must be either noncyclic, unordered, or symbolic");
    }
    else if (inType instanceof Type.Vector)
    {
      if (((Type.Vector) inType).elt instanceof Type.Scalar)
      {
        Type.Scalar elt = (Type.Scalar) ((Type.Vector) inType).elt;
        if (badScalarInputType(elt))
          throw new IllegalArgumentException("Neural Net input vector element type must have both upper and lower bounds, and must be either noncyclic, unordered, or symbolic");
      }
      else throw new IllegalArgumentException("Invalid Neural Net input vector element type");
      if (inLength<0) throw new IllegalArgumentException("Invalid Neural Net input vector length");
      inputVectorLength = inLength;
    }
    else if (!(inType instanceof Type.Structured)) throw new IllegalArgumentException("Invalid Neural Net input type");
    inputType = inType;
    if (outType instanceof Type.Discrete)
    {
      if (!(((Type.Discrete)outType).hasLWB() == Type.Scalar.YES && ((Type.Discrete)outType).hasUPB() == Type.Scalar.YES))
        throw new IllegalArgumentException("Neural Net output type must have both upper and lower bounds");
    }
    else if (outType instanceof Type.Vector)
    {
      if (((Type.Vector)outType).elt instanceof Type.Discrete)
      {
        Type.Discrete elt = (Type.Discrete) ((Type.Vector)outType).elt;
        if (!(elt.hasLWB() == Scalar.YES && elt.hasUPB() == Scalar.YES))
          throw new IllegalArgumentException("Neural Net output vector element type must have both upper and lower bounds");
      }
      else throw new IllegalArgumentException("Invalid Neural Net input vector element type");
      if (outLength<0) throw new IllegalArgumentException("Invalid Neural Net output vector length");
      outputVectorLength = outLength;
    }
    else if (!(outType instanceof Type.Structured)) throw new IllegalArgumentException("Invalid Neural Net output type");
    outputType = outType;
    objPtr = nativeCreateNNet((hiddenLayerSizes==null) ? 0 : hiddenLayerSizes.length, hiddenLayerSizes, createLinks);
  }


  public NNetInt(Type inType, int inLength, Type outType, int outLength, int[] hiddenLayerSizes) { this(inType, inLength, outType, outLength, hiddenLayerSizes, true); }  // no need to explicitly specify createLinks = true
  public NNetInt(NNetInt src)  // copy constructor
  {
    inputType = src.inputType;
    outputType = src.outputType;
    synchronized (src)
    {
      objPtr = nativeCreateNNet(src.objPtr);
    }
  }


  /** 
    Checks that NNet input type has upper and lower bounds, and is noncyclic, symbolic, or unordered.
    <p>
    The NNet has two different ways of representing an input value:
    <p>
    (1) as a single scalar input on or within the range [-1, 1] (actual input value is mapped linearly into/onto this range).
    (2) as a set of on/off (0 or 1) inputs, with one input node for each possible input value.<p>
    <p>
    Clearly, both cases require that the input be bounded above and below.
    Case (2) is used if the input is symbolic, or if it is un-ordered (or both).
    otherwise (for ordered, non-sybolic inputs) Case (1) is used...  provided
    the input isn't cyclic.
    <p>
    Ordered, non-symbolic data types with cyclical topology (e.g. hour of the
    day) cannot be represented properly by either (1) or (2), and therefore
    cannot be passed directly as inputs to the NNet.
    <p>
    In English then, the only types which are REJECTED are types that are
    unbounded (either above, below, or both), OR that are (simultaneously):
    cyclic, ordered, and NOT symbolic.
  */
  private static boolean badScalarInputType(Type.Scalar s)
  { 
    return (
             !(s.hasLWB() == Scalar.YES && s.hasUPB() == Scalar.YES)) || 
             !(
                (!(s.isCyclic && s.ckIsCyclic)) || (s instanceof Type.Symbolic) || 
                ((s instanceof Type.Discrete) && !(((Type.Discrete)s).isOrdered && ((Type.Discrete)s).ckIsOrdered)
              )
           ); 
  }

  // Access to input and output types
  public Type getInputType()         { return inputType; }
  public Type getOutputType()        { return outputType; }
  public int getInputVectorLength()  { return inputVectorLength; }
  public int getOutputVectorLength() { return outputVectorLength; }

  // Read/Write methods for various training parameters ("OpenGL style"); see parameter constants below
  public int getIntParam(int param)                      { return nativeGetIntParam(objPtr, param); }
  public boolean getBooleanParam(int param)              { return nativeGetBooleanParam(objPtr, param); }
  public float getFloatParam(int param)                  { return nativeGetFloatParam(objPtr, param); }
  public void setIntParam(int param, int value)          { nativeSetIntParam(objPtr, param, value); }
  public void setBooleanParam(int param, boolean value)  { nativeSetBooleanParam(objPtr, param, value); }
  public void setFloatParam(int param, float value)      { nativeSetFloatParam(objPtr, param, value); }

  // Int "param" constants
  public final static int iTrainingMethod = 0;               // possible methods to use for training the network,  DEFAULT = tmLevenbergMarquardt
      public final static int tmBackProp = 0;                    // allowed values for parameter "iTrainingMethod"
      public final static int tmBatchBackProp = 1;
      public final static int tmRProp = 2;
      public final static int tmLevenbergMarquardt = 3;
  // Boolean "param" constants
  public final static int bUnityOutputLayerDerivatives = 0;  // calculate weight changes as if output layer derivatives were 1 (even though they aren't),  DEFAULT = true
  public final static int bBiasDecay = 1;                    // apply decay factor to biases as well as links,  DEFAULT = false
  // Float "param" constants
  public final static int fOutputOffValue = 0;               // threshold values for training the outputs... NB MUST have OffValue < OnValue
  public final static int fOutputOnValue = 1;                //                 DEFAULTS:  OutputOffValue = 0.0,  OutputOnValue = 1.0
  public final static int fLearningRate = 2;                 // learning rate for backprop,  DEFAULT = 0.2
  public final static int fMomentumFactor = 3;               // "momentum" term (for backprop only),  DEFAULT = 0.5
  public final static int fDelta0 = 4;                       // initial Delta for the RProp method,  DEFAULT = 0.1
  public final static int fDeltaMin = 5;                     // minimum Delta for the RProp method,  DEFAULT = 1E-6 (=0.000001)
  public final static int fDeltaMax = 6;                     // maximum Delta for the RProp method,  DEFAULT = 50.0
  public final static int fMu = 7;                           // current Mu for Levenberg-Marquardt (changes as network is trained),  INITIAL DEFAULT = 0.3
  public final static int fMuMin = 8;                        // minimum Mu value for Levenberg-Marquardt,  DEFAULT = 0.001
  public final static int fMuInc = 9;                        // Mu increment value (additive) for Levenberg-Marquardt,  DEFAULT = 0.05
  public final static int fMuDec = 10;                       // Mu decrement factor (multiplicative) for Levenberg-Marquardt,  DEFAULT = 0.7
  public final static int fWeightDecayFactor = 11;           // weight decay factor - per epoch for epoch (batch) learning methods, othewise per pattern,  DEFAULT = 0 (no decay)
  // Nodal transfer function allowed values (used with the setTransferFunction() methods),  DEFAULT = tfSigmoidPos
  public final static int tfHardLimit = 0;                   // f(x) = -1 (x<=0)  |  =  1 (x>0)
  public final static int tfHHardlimitPos = 1;               // f(x) = -1 (x<=0)  |  =  0 (x>0)
  public final static int tfLinear = 2;                      // f(x) =  x
  public final static int tfLinearPos = 3;                   // f(x) =  0 (x<=0)  |  =  x (x>0)
  public final static int tfSaturatingLinear = 4;            // f(x) = -1 (x<=-1) |  = x (-1<x<1)  |  = 1 (x>=1)
  public final static int tfSaturatingLinearPos = 5;         // f(x) =  0 (x<=0)  |  = x  (0<x<1)  |  = 1 (x>=1)
  public final static int tfSigmoid = 6;                     // f(x) = 1 - 2/(1+exp(2x))       NB defined so that slope = 1 at x = 0
  public final static int tfSigmoidPos = 7;                  // f(x) = 1 - 1/(1+exp(4x))       NB defined so that slope = 1 at x = 0

  // Methods that control and monitor the training process from a different thread (NB the NNet object is NOT multithread-enabled: ONLY the methods below are not "synchronized" internally)
  public boolean stopSet = false;        // flags that training should be stopped at the end of the epoch
//  public void setSearch(NNetSearch nns) { search = nns; }   // sets search to be notified after each epoch (NNetSearch afterEpoch() method is called)
  public void start() { stopSet = false; }
  public void stop() { stopSet = true;  }
  public void abortTraining()         { nativeAbortTraining(objPtr); }               // rapidly aborts training (without updating weights for the current epoch, for batch learning methods)
  public int currentEpoch()           { return nativeCurrentEpoch(objPtr); }         // returns current training epoch, starting at 1  (NB not in synch with GetpatternsRemaining())
  public int patternsRemaining()      { return nativePatternsRemaining(objPtr); }    // returns number of input patterns still remaining in current training epoch (NB not in synch with GetCurrentEpoch())

  // Methods to setup, modify, train, and use the NNet.  Those with a boolean return type return true on success, or false if their parameters were invalid
  public boolean deleteLink(long destNodeHandle, int linkNum)                        { return nativeDeleteLink(objPtr, destNodeHandle, linkNum); }             // delete a link from a node... can specify Node handle or layer/nodeNum, and
  public boolean deleteLink(long destNodeHandle, int layer, int nodeNum)             { return nativeDeleteLink(objPtr, destNodeHandle, layer, nodeNum); }      // can specify link number or link destination
  public boolean deleteLink(int destLayer, int destNodeNum, int linkNum)             { return nativeDeleteLink(objPtr, destLayer, destNodeNum, linkNum); }
  public boolean deleteLink( int destLayer, int destNodeNum, int layer, int nodeNum) { return nativeDeleteLink(objPtr, destLayer, destNodeNum, layer, nodeNum); }
  public boolean addOrSetLink(long destNodeHandle, int layer, int nodeNum, float weight)              { return nativeAddOrSetLink(objPtr, destNodeHandle, layer, nodeNum, weight); }          // add a single link to a node, or change the weight of an existing link
  public boolean addOrSetLink(int destLayer, int destNodeNum, int layer, int nodeNum, float weight)   { return nativeAddOrSetLink(objPtr, destLayer, destNodeNum, layer, nodeNum, weight); }  // as above, but you don't need the node handle
  public boolean addOrSetLink(long destNodeHandle, int layer, int nodeNum)                            { return nativeAddOrSetLink(objPtr, destNodeHandle, layer, nodeNum, 0.0f); }            // default weight of 0.0
  public boolean addOrSetLink(int destLayer, int destNodeNum, int layer, int nodeNum)                 { return nativeAddOrSetLink(objPtr, destLayer, destNodeNum, layer, nodeNum, 0.0f); }    // default weight of 0.0
  public boolean setConnectivity(int[] srcRanges, int layer, int firstNodeNum, int lastNodeNum)       { return nativeSetConnectivity(objPtr, srcRanges.length/3, srcRanges, layer, firstNodeNum, lastNodeNum); }
      // specify part of the network link graph: links from each node in srcRanges are created to each of the nodes in the range (layer, firstNodeNum) to (layer, lastNodeNum) inclusive
  public boolean setTransferFn(int layer, int firstNode, int lastNode, int fn)   { return nativeSetTransferFn(objPtr, layer, firstNode, lastNode, fn); }  // set the transfer fn. for a range of nodes
  public boolean setTransferFn(int firstLayer, int lastLayer, int fn)            { return nativeSetTransferFn(objPtr, firstLayer, lastLayer, fn); }       // set the transfer fn. for nodes in specified layers
  public void setTransferFn(int fn)                                              { nativeSetTransferFn(objPtr, fn); }                                     // set the transfer fn. for all nodes in the network
  public boolean nguyenWidrowInitWeights(int layer, int firstNode, int lastNode) { return nativeNWInit(objPtr, layer, firstNode, lastNode); }             // init weights & biases using Nguyen-Widrow algorithm (specified nodes)
  public boolean nguyenWidrowInitWeights(int firstLayer, int lastLayer)          { return nativeNWInit(objPtr, firstLayer, lastLayer); }                  // init weights & biases using Nguyen-Widrow algorithm (specified layers)
  public void nguyenWidrowInitWeights()                                          { nativeNWInit(objPtr); }                                                // init weights & biases using Nguyen-Widrow algorithm (whole network)
  public boolean randomInitWeights(int layer, int firstNode, int lastNode)       { return nativeRandomInit(objPtr, layer, firstNode, lastNode); }         // init weights & biases randomly; same as NW with 1 node per layer (specified nodes)
  public boolean randomInitWeights(int firstLayer, int lastLayer)                { return nativeRandomInit(objPtr, firstLayer, lastLayer); }              // init weights & biases randomly (specified layers)
  public void randomInitWeights()                                                { nativeRandomInit(objPtr); }                                            // init weights & biases randomly (whole network)

  public void train(Value.Vector inputData, Value.Vector outputData)               // Train the network
  {
    // TYPE CHECKING:  Must ensure that the element type of inputData and outputData match inputType and outputType respectively - they must be of those types or of COMPATIBLE
    // types (e.g. descendant types, or structured types with components that are descendant types, etc.).  Also, for vector types the lengths of the element vectors MUST be
    //  inputVectorlength and/or outputVectorLength, respectively, and descendant structured types MUST have the SAME number of components, to be compatible in the above sense.
    if (inputData.length() != outputData.length()) throw new IllegalArgumentException("For Neural Net training, number of input and output patterns must be the same");
    nativeTrain(objPtr, inputData.length(), inputData, outputData);
  }

  public double measureError(Value.Vector inputData, Value.Vector outputData)      // Return normalized error measure of network performance on the training data (doesn't affect network)
  {
    // NB type checking below exactly the same as for train()...
    // TYPE CHECKING:  Must ensure that the element type of inputData and outputData match inputType and outputType respectively - they must be of those types or of COMPATIBLE
    // types (e.g. descendant types, or structured types with components that are descendant types, etc.).  Also, for vector types the lengths of the element vectors MUST be
    //  inputVectorlength and/or outputVectorLength, respectively, and descendant structured types MUST have the SAME number of components, to be compatible in the above sense.
    if (inputData.length() != outputData.length()) throw new IllegalArgumentException("For Neural Net training, number of input and output patterns must be the same");
    return nativeMeasureError(objPtr, inputData.length(), inputData, outputData);
  }


  public Value classify(Value inputPattern)   // Find most probable classification; result is a (non-null reference to a) Value with Type "outputType"
  {
    // TYPE CHECKING:  Must ensure that the type of inputPattern is type compatible with inputType.
    // Also, for vector types the lengths MUST be inputVectorlength and descendant structured types MUST have the 
    // SAME number of components, to be compatible in the above sense.

    int[] resultComponents = nativeClassify(objPtr, convertInputValueToIntArray(inputPattern));

    if (outputType instanceof Type.Structured)
    {
      Type.Structured st = (Type.Structured) outputType;            
      int numCmpnts = st.cmpnts.length;

      /* Create an array of heavy values and use DefStructured.  This could be improved upon one day if feeling energetic. LF */
      Value.Discrete[] vals = new Value.Discrete[numCmpnts];
      for (int i = numCmpnts-1; i>=0; i--)
        if (st.cmpnts[i] instanceof Type.Discrete)
          vals[i] = new Value.Discrete((Type.Discrete) st.cmpnts[i],(int) (((Type.Discrete) st.cmpnts[i]).LWB + ((resultComponents[i]>0) ? resultComponents[i] : 0)));
      return new Value.DefStructured((Type.Structured) outputType,vals);
    }
    else if (outputType instanceof Type.Vector)
    {
      int[] mappedResult = new int[outputVectorLength];
      int vLWB = (int) ((Type.Discrete) (((Type.Vector) outputType).elt)).LWB;
      for (int i = outputVectorLength-1; i>=0; i--) 
        mappedResult[i] = vLWB + ((resultComponents[i]>0) ? resultComponents[i] : 0);
      return new VectorFN.DiscreteVector((Type.Vector)outputType,mappedResult);
    }
    else return new Value.Discrete((Type.Discrete)outputType,(int) (((Type.Discrete) outputType).LWB + ((resultComponents[0]>0) ? resultComponents[0] : 0)));
  }

  // Find probability distribution of classifications; returns a Value with Type as follows:
  public Value probabilisticClassify(Value inputPattern)
  {
    // TYPE CHECKING:  Must ensure that the type of inputPattern matches inputType - it must be of Type 'inputType' or of a COMPATIBLE
    // type (e.g. a descendant type, or a structured type with components that are descendant types, etc.).  Also, if inputPattern is a vector type (Type.Vector and descendants),
    // then the length MUST be outputVectorlength. Descendant structured types MUST have the SAME number of components, to be compatible in the above sense.
    @SuppressWarnings("unused") 
    double[][] componentProb = nativeProbabilisticClassify(objPtr, convertInputValueToIntArray(inputPattern));
    // ??????? create an appropriate output Value variable for representing probability distributions over Type.Discrete, Type.Structured and Type.Vector;
    // then fill in probabilities from the componentProb array.  NOTE:  Discrete types with only 1 possible value (UPB=LWB) will have no information returned
    // (i.e. componentProb[i] = null) and the probability needs to be filled in as 1.0 manually.
    // finally, return the created structure.
    return null;  // ...just so it compiles!
  }


  // NNet inspection functions.  NB except for getNodeHandle(s) these are all "checked", in that they simply return 0 if a parameter is out of range.
  public int numLayers()               { return nativeNumLayers(objPtr); }             // returns the number of layers in the NNet, including both input and output layers
  public int totalNumNodes()           { return nativeTotalNumNodes(objPtr); }         // returns the total number of nodes in the NNet, NOT COUNTING THE INPUT LAYER
  public int totalNumLinks()           { return nativeTotalNumLinks(objPtr); }         // returns the total number of links in the NNet, NOT COUNTING NODE BIASES
  public int numNodesInLayer(int l)    { return nativeNumNodesInLayer(objPtr, l); }    // returns the number of nodes in layer l, l=0 represents the input layer
  public int numInputs()               { return nativeNumInputs(objPtr); }             // returns the number of input nodes
  public int numOutputs()              { return nativeNumOutputs(objPtr); }            // returns the number of output nodes
  public float inputValue(int i)       { return nativeInputValue(objPtr, i); }         // return the current value of the i'th input
  public float outputValue(int i)      { return nativeOutputValue(objPtr, i); }        // return the current value of the i'th output
  public float getInputMin(int i)      { return nativeGetInputMin(objPtr, i); }        // returns the minimum value that the i'th input will take (given the NNet's input type)
  public float getInputMax(int i)      { return nativeGetInputMax(objPtr, i); }        // returns the maximum value that the i'th input will take (given the NNet's input type)
  public float getInputAvrg(int i)     { return nativeGetInputAvrg(objPtr, i); }       // returns the "average" value of the i'th input; assumes average of "range" type input variables is mid-range, and that all symbols occur with equal frequncy for symbolic (or discrete & unordered) input variables
  public long getNodeHandle(int layer, int nodeNum) { return nativeGetNodeHandle(objPtr, layer, nodeNum); }  // returns a handle (pointer) to a node - throws exception if node such node exists
  public long[] getNodeHandles(int layer)           { return nativeGetNodeHandles(objPtr, layer); }          // returns the handles (pointers) to all nodes in a layer - returns zero-length array if layer is out of range

  // Node inspection/modification methods;  may throw RuntimeException OR CAUSE UNEXPECTED SIDE EFFECTS OR A SYSTEM CRASH if nodeHandle is invalid!!!
  public synchronized native int getNodeTransferFn(long nodeHandle);
  public synchronized native void setNodeTransferFn(long nodeHandle, int transferFn);
  public synchronized native float getNodeBias(long nodeHandle);
  public synchronized native void setNodeBias(long nodeHandle, float bias);
  public synchronized native float getNodeOutput(long nodeHandle);
  public native int getNodeLayer(long nodeHandle);
  public native int getNodeNum(long nodeHandle);                 // number of node in layer (0 = first node, 1 = second node... etc.)
  public synchronized native int getNodeNumLinks(long nodeHandle);
  public native float getNodeActiveSigmaMin(long nodeHandle);    // obtain the range of "active" values for sigma (i.e. where the derivative of the transfer fn. is large)
  public native float getNodeActiveSigmaMax(long nodeHandle);
  public native float getNodeOutputMin(long nodeHandle);         // obtain the range of output values for the node (depends only on transfer function)
  public native float getNodeOutputMax(long nodeHandle);

  public final class NNetLink implements Serializable { /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4573073853726730112L;
int layer; int nodeNum; float weight; }    // simple structure for storing link info
  public NNetLink[] getNodeLinkInfo(long nodeHandle)
  {
    int[] linkData = nativeGetLinkData(nodeHandle);
    int n = linkData.length/3;
    NNetLink[] result = new NNetLink[n];
    int i = linkData.length-3;
    for (n--; n>=0; n--)
    {
      result[n].layer = linkData[i];
      result[n].nodeNum = linkData[i+1];
      result[n].weight = Float.intBitsToFloat(linkData[i+2]);
      i -= 3;
    }
    return result;
  }


  // "Callback" methods, called from within the C++ code
//  public boolean onEpochComplete(int epoch, double err) { if (search!=null) search.afterEpoch(epoch, err); return stopSet; }  // called on completion of each training epoch


  public int getNumInputComponents()
  {
    if (inputType instanceof Type.Scalar) return 1;
    else if (inputType instanceof Type.Vector) return inputVectorLength;
    else return ((Type.Structured) inputType).cmpnts.length;
  }


  public int getNumOutputComponents()
  {
    if (outputType instanceof Type.Discrete) return 1;
    else if (outputType instanceof Type.Vector) return outputVectorLength;
    else return ((Type.Structured) outputType).cmpnts.length;
  }


  public int getInputComponentType(int i)
  {
    Type cmpType;
    if (inputType instanceof Type.Structured) cmpType = ((Type.Structured)inputType).cmpnts[i];
    else if (inputType instanceof Type.Vector) cmpType = ((Type.Vector)inputType).elt;
    else cmpType = inputType;
    if (cmpType instanceof Type.Scalar)
    {
      if (badScalarInputType((Type.Scalar)cmpType)) return -2;  // invalid component, so ignore
      if (cmpType instanceof Type.Continuous) return -1;  // -1 flags a continuous range
      if (!(cmpType instanceof Type.Discrete)) return -2;  // if not Cts, should be Discrete... otherwise invalid.
      if (!(((Type.Discrete)cmpType).isOrdered && ((Type.Discrete)cmpType).ckIsOrdered)||(cmpType instanceof Type.Symbolic)) return (int) (((Type.Discrete)cmpType).UPB-((Type.Discrete)cmpType).LWB+1);  // symbolic type
      return 0;                                                                                                                                          // integer range
    }
    return -2;  // -2 flags an unused/invalid component type
  }


  public int getInputComponentMin(int i)
  {
    Type cmpType;
    if (inputType instanceof Type.Structured) cmpType = ((Type.Structured)inputType).cmpnts[i];
    else if (inputType instanceof Type.Vector) cmpType = ((Type.Vector)inputType).elt;
    else cmpType = inputType;
    if (cmpType instanceof Type.Continuous) return Float.floatToIntBits((float) ((Type.Continuous)cmpType).LWB);
    if (cmpType instanceof Type.Discrete) return (int) ((Type.Discrete)cmpType).LWB;
    return 0;
  }


  public int getInputComponentMax(int i)
  {
    Type cmpType;
    if (inputType instanceof Type.Structured) cmpType = ((Type.Structured)inputType).cmpnts[i];
    else if (inputType instanceof Type.Vector) cmpType = ((Type.Vector)inputType).elt;
    else cmpType = inputType;
    if (cmpType instanceof Type.Continuous) return Float.floatToIntBits((float) ((Type.Continuous)cmpType).UPB);
    if (cmpType instanceof Type.Discrete) return (int) ((Type.Discrete)cmpType).UPB;
    return 0;
  }


  public int getOutputComponentNumValues(int i)
  {
    Type cmpType;
    if (outputType instanceof Type.Structured) cmpType = ((Type.Structured)outputType).cmpnts[i];
    else if (outputType instanceof Type.Vector) cmpType = ((Type.Vector)outputType).elt;
    else cmpType = outputType;
    if ((!(cmpType instanceof Type.Discrete))||(!(((Type.Discrete)cmpType).hasLWB() == Type.Scalar.YES && ((Type.Discrete)cmpType).hasUPB() == Type.Scalar.YES))) return 0;
    int result = (int) (((Type.Discrete)cmpType).UPB-((Type.Discrete)cmpType).LWB+1);
    if (result>1) return result;
    return 0;
  }


  public int[] getInputComponentValues(int patn, Value.Vector inputPatterns) { return convertInputValueToIntArray(inputPatterns.elt(patn)); }


  public int[] getOutputComponentValues(int patn, Value.Vector outputPatterns)
  {
    Value outPattn = outputPatterns.elt(patn);
    int[] result;
    if (outputType instanceof Type.Structured)
    {
      int numCmpnts = ((Type.Structured) outputType).cmpnts.length;
      result = new int[numCmpnts];
      for (int i = numCmpnts-1; i>=0; i--)
        if (((Type.Structured) outputType).cmpnts[i] instanceof Type.Discrete)
          result[i] = (int) (((Value.Discrete) ((Value.Structured) outPattn).cmpnt(i)).getDiscrete() - ((Type.Discrete) (((Type.Structured) outputType).cmpnts[i])).LWB);
    }
    else if (outputType instanceof Type.Vector)
    {
      result = new int[outputVectorLength];
      for (int i = outputVectorLength-1; i>=0; i--) result[i] = (int) ((((Value.Vector) outPattn).intAt(i)) - ((Type.Discrete) (((Type.Vector) outputType).elt)).LWB);
    }
    else
    {
      result = new int[1];
      result[0] = (int) (((Value.Discrete) outPattn).getDiscrete() - ((Type.Discrete) outputType).LWB);
    }
    return result;
  }


  // Helper method to convert an input object to a form suitable for handling on the C++ side - called from getInputComponentValues(), classify(), and probabilisticClassify()
  private int[] convertInputValueToIntArray(Value inPattn)
  {
    int[] result;
    if (inputType instanceof Type.Structured)
    {
      int numCmpnts = ((Type.Structured)inputType).cmpnts.length;
      result = new int[numCmpnts];
      for (int i = numCmpnts-1; i>=0; i--)
        if (((Type.Structured) inputType).cmpnts[i] instanceof Type.Continuous) result[i] = Float.floatToIntBits((float) ((Value.Continuous) (((Value.Structured) inPattn).cmpnt(i))).getContinuous());
        else if (((Type.Structured) inputType).cmpnts[i] instanceof Type.Discrete) result[i] = (int) ((Value.Discrete) (((Value.Structured) inPattn).cmpnt(i))).getDiscrete();
    }
    else if (inputType instanceof Type.Vector)
    {
      int numElts = inputVectorLength;
      result = new int[numElts];
      if (((Type.Vector)inputType).elt instanceof Type.Continuous)
        for (int i = numElts-1; i>=0; i--) result[i] = Float.floatToIntBits((float) ((Value.Vector) inPattn).doubleAt(i));
      else for (int i = numElts-1; i>=0; i--) result[i] = ((Value.Vector) inPattn).intAt(i);
    }
    else
    {
      result = new int[1];
      if (inputType instanceof Type.Continuous) result[0] = Float.floatToIntBits((float) ((Value.Continuous) inPattn).getContinuous()); else result[0] = (int) ((Value.Discrete) inPattn).getDiscrete();
    }
    return result;
  }

  // Serialization methods
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject();
    objPtr = nativeCreateNNet(stream);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException
  {
    stream.defaultWriteObject();
    nativeSaveNNetToStream(objPtr, stream);
  }

  // "Destructor" for the class - ensures the underlying C++ class destructor gets called
  protected void finalize() throws Throwable
  {
    if (objPtr != 0) nativeDeleteNNet(objPtr);
    super.finalize();
  }

  // NNet Library initialization code
  static
  {
    System.loadLibrary("cdmsNNets");
    nativeInitNNetLibrary();
  }

  // Native (C++) methods.  Some of these may throw RuntimeException and/or OutOfMemoryError.  Stream load/save methods can also throw IOException.
  private static native void nativeInitNNetLibrary();
  private native long nativeCreateNNet(int numHiddenLayers, int[] hiddenLayerSizes, boolean createLinks);
  private native long nativeCreateNNet(long srcObjPtr);
  private native long nativeCreateNNet(ObjectInputStream stream) throws IOException;
  private synchronized native void nativeSaveNNetToStream(long objPtr, ObjectOutputStream stream) throws IOException;
  private native void nativeDeleteNNet(long objPtr);
  private synchronized native int nativeGetIntParam(long objPtr, int param);
  private synchronized native boolean nativeGetBooleanParam(long objPtr, int param);
  private synchronized native float nativeGetFloatParam(long objPtr, int param);
  private synchronized native void nativeSetIntParam(long objPtr, int param, int value);
  private synchronized native void nativeSetBooleanParam(long objPtr, int param, boolean value);
  private synchronized native void nativeSetFloatParam(long objPtr, int param, float value);
  private native void nativeAbortTraining(long objPtr);
  private native int nativeCurrentEpoch(long objPtr);
  private native int nativePatternsRemaining(long objPtr);
  private synchronized native boolean nativeDeleteLink(long objPtr, long destNodeHandle, int linkNum);
  private synchronized native boolean nativeDeleteLink(long objPtr, long destNodeHandle, int layer, int nodeNum);
  private synchronized native boolean nativeDeleteLink(long objPtr, int destLayer, int destNodeNum, int linkNum);
  private synchronized native boolean nativeDeleteLink(long objPtr, int destLayer, int destNodeNum, int layer, int nodeNum);
  private synchronized native boolean nativeAddOrSetLink(long objPtr, long destNodeHandle, int layer, int nodeNum, float weight);
  private synchronized native boolean nativeAddOrSetLink(long objPtr, int destLayer, int destNodeNum, int layer, int nodeNum, float weight);
  private synchronized native boolean nativeSetConnectivity(long objPtr, int numSrcRanges, int[] srcRanges, int layer, int firstNodeNum, int lastNodeNum);
  private synchronized native boolean nativeSetTransferFn(long objPtr, int layer, int firstNode, int lastNode, int fn);
  private synchronized native boolean nativeSetTransferFn(long objPtr, int firstLayer, int lastLayer, int fn);
  private synchronized native void nativeSetTransferFn(long objPtr, int fn);
  private synchronized native boolean nativeNWInit(long objPtr, int layer, int firstNode, int lastNode);
  private synchronized native boolean nativeNWInit(long objPtr, int firstLayer, int lastLayer);
  private synchronized native void nativeNWInit(long objPtr);
  private synchronized native boolean nativeRandomInit(long objPtr, int layer, int firstNode, int lastNode);
  private synchronized native boolean nativeRandomInit(long objPtr, int firstLayer, int lastLayer);
  private synchronized native void nativeRandomInit(long objPtr);
  private synchronized native void nativeTrain(long objPtr, int numPatterns, Value.Vector inputData, Value.Vector outputData);
  private synchronized native double nativeMeasureError(long objPtr, int numPatterns, Value.Vector inputData, Value.Vector outputData);
  private synchronized native int[] nativeClassify(long objPtr, int[] inputPattern);
  private synchronized native double[][] nativeProbabilisticClassify(long objPtr, int[] inputPattern);
  private native int nativeNumLayers(long objPtr);
  private native int nativeTotalNumNodes(long objPtr);
  private synchronized native int nativeTotalNumLinks(long objPtr);
  private native int nativeNumNodesInLayer(long objPtr, int l);
  private native int nativeNumInputs(long objPtr);
  private native int nativeNumOutputs(long objPtr);
  private synchronized native float nativeInputValue(long objPtr, int i);
  private synchronized native float nativeOutputValue(long objPtr, int i);
  private native float nativeGetInputMin(long objPtr, int i);
  private native float nativeGetInputMax(long objPtr, int i);
  private native float nativeGetInputAvrg(long objPtr, int i);
  private native long nativeGetNodeHandle(long objPtr, int layer, int nodeNum);
  private native long[] nativeGetNodeHandles(long objPtr, int layer);
  private synchronized native int[] nativeGetLinkData(long nodeHandle);
}
