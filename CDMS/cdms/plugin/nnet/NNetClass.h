#ifndef NNetClassH
#define NNetClassH
#include <stddef.h>        // definition of NULL



// type definition for defining a range of nodes within a layer; indices are zero-based, and lastNode is included in the range
struct NNetNodeRange { int layer, firstNode, lastNode; };


class NNetNode
{
  friend class NNet;
  friend class NNetLink;
public:     // symmetric (-1, 1) and positive (0, 1) transfer (thresholding) functions.  NOTE: if more are added must update NNetLastTranferFn, f(), Df(), inlnGetActiveSigmaRange() and inlnGetOutputRange()
  enum TransferFnType { HARD_LIMIT, HARD_LIMIT_POS, LINEAR, LINEAR_POS, SATURATING_LINEAR, SATURATING_LINEAR_POS, SIGMOID, SIGMOID_POS } TransferFn;  // type of thresholding function to use
  float bias;
private:
  float output;
  double sigma;                 // pre-thresholding sum of weighted inputs and bias;  stored because calculation of f' could require it (for some choices of f() )
  NNetLink* Link;               // array of incoming links
  int lastLink;                 // number of links in links array "Link[]" minus 1
  void* TData;                  // Training data - what this holds depends on training method being used;  typecast as appropriate.
  int layer, nodeNum;           // layer and index of node in layer (zero based, except layer zero is input layer and contains no nodes)
  NNetNode() { TransferFn = SIGMOID_POS;  bias = 0.0f;  output = 0.0f;  sigma = 0.0;  Link = NULL;  lastLink = -1;  TData = NULL; };  // default constructor, inits members
  ~NNetNode();                  // destructor:  deletes all links belonging to node
  int CopyLinks(const NNetNode& node);     // copies the links of another node
  void ComputeOutput();                                          // sum weighted inputs and apply transfer function
  float f() const;                                               // f(sigma), the transfer function
  float Df() const;                                              // derivative f' of the transfer function
  bool DeleteLink(int i);
  bool DeleteLink(const int layr, const int ndNum);
  int SetLinks(float* const InputArray, NNetNode** const NodeArray, const NNetNodeRange* const srcRanges, const int srcRangeEntries);  // create link array
  void NguyenWidrowInitWeights(const NNet& Net, const bool RandomInit = false);       // init weights & bias using Nguyen-Widrow algorithm
  void inlnGetActiveSigmaRange(float& min, float& max) const;    // obtain the range of "active" values for sigma (i.e. where the derivative of the transfer fn. is large)
  void inlnGetOutputRange(float& min, float& max) const;         // obtain the range of output values for the node (depends on transfer function)
public:
  float Output() const { return output; };
  int Layer() const { return layer; };
  int NodeNum() const { return nodeNum; };   // number of node in layer (0 = first node, 1 = second node... etc.)
  int NumLinks() const { return lastLink+1; };
  const NNetLink* GetLinkArray() const { return Link; };
  void GetActiveSigmaRange(float& min, float& max) const;    // obtain the range of "active" values for sigma (i.e. where the derivative of the transfer fn. is large), external linkage version
  void GetOutputRange(float& min, float& max) const;         // obtain the range of output values for the node (depends on transfer function), external linkage version
};




class NNetLink
{
  friend class NNet;
  friend class NNetNode;
public:
  void GetSource(int& layer, int& nodeNum) const { if (fromInput>=0) { layer=0; nodeNum=fromInput; } else { layer=srcNode->layer; nodeNum=srcNode->nodeNum; }};
  mutable float weight;          // link weight
private:
  int fromInput;         // = input no. if link is from an input, or -1 if link is from a node.
  NNetNode* srcNode;     // Node from which this link connects (if an input, pointer is set so that srcNode->output points to correct input value)
  float srcValue() { return srcNode->output; };      // returns value at input (source) end of link
  float weightedVal() { return weight*srcValue(); }; // returns weighted output of link
};




class NNet
{
friend class NNetNode;
private:
  int numLayers, numNodes, numLinks;   // input and output layers are BOTH counted as layers, but the input layer is considered to contain NO nodes.
  int* layerSize;             // array of layer sizes; layerSize[0] = number of inputs
  NNetNode** Node;            // 2D array of layers/nodes, NOT including input layer
protected:
  float* Input;               // array of input values
private:
  struct TrainingPatterns { float* Input; bool* DesiredOutput; } *TrainingSet; // array of training data
  int numTrainingPatterns;    // number of training patterns stored in TrainingSet
  volatile int epoch, patternsRemaining;  // holds training epoch and number of current input patterns still remaining (may be read from a different thread)
  volatile bool abortTraining;            // set to true to rapidly abort training (without updating weights for the current epoch, for batch learning methods)
  void InitNNet();                        // set up initial default values, etc.  Called first thing by regular and stream NNet constructors
  void CreateTrainingSet(int NumPatterns, const void* TrainingData);   // create a set of preprocessed inputs+desired outputs for training, *if* memory is available.
  void DeleteTrainingSet();                              // free memory used for creating training set
  void FeedForward();
  void GetTrainingPattern(const int n, bool*& DesiredOutput, const void* const TrainingData);
  void PrepareToBackProp(float*& prev_dw);
  void FinishedBackProp(float* const prev_dw);
  double DoIterationBackProp(const bool* const DesiredOutput, float* const prev_dw);    // return value = sum of squared error of the outputs
  void TrainBackProp(const int NumPatterns, const void* const TrainingData);
  void PrepareToBatchBackProp(double*& dw, float*& prev_dw);
  void FinishedBatchBackProp(double* const dw, float* const prev_dw);
  double DoIterationBatchBackProp(const bool* const DesiredOutput, double* const dw);    // return value = sum of squared error of the outputs
  void ProcessEpochBatchBackProp(double* const dw, float* const prev_dw);                // Update NNet on completion of training epoch (BackProp algorithm)
  void TrainBatchBackProp(const int NumPatterns, const void* const TrainingData);
  void PrepareToRProp(float*& delta, double*& dw, float*& prev_dw);
  void FinishedRProp(float* const delta, double* const dw, float* const prev_dw);
  float RProp_ParameterUpdate(float& delta, double& dw, float& prev_dw);                 // updates a single parameter using the RProp algorithm
  void ProcessEpochRProp(float* const delta, double* const dw, float* const prev_dw);    // Update NNet on completion of training epoch (RProp algorithm)
  void TrainRProp(const int NumPatterns, const void* const TrainingData);
  void PrepareToLevenbergMarquardt(double*& E, double*& JtE, double**& JtJ, const int lastParam, const int lastOutput);
  void FinishedLevenbergMarquardt(double* const E, double* const JtE, double** const JtJ, const int lastParam);
  double DoIterationLevenbergMarquardt(const bool* const DesiredOutput, double* const E, double* const JtE, double** const JtJ, const int lastParam, const int lastOutput);   // return value = sum of squared error of the outputs
  void ProcessEpochLevenbergMarquardt(double* const JtE, double** const JtJ, const int lastParam, const double lastErr, const double currentErr);   // Update NNet on completion of training epoch (LM algorithm)
  void LevenbergMarquardt_UpdateMu(const double lastErr, const double currentErr, const bool numericalProblem);                  // Update the "mu" parameter
  void TrainLevenbergMarquardt(const int NumPatterns, const void* const TrainingData);
  bool RangeOK(const NNetNodeRange R) { return (R.layer>0)&&(R.layer<(numLayers-1))&&(R.firstNode>=0)&&(R.lastNode>=R.firstNode)&&(R.lastNode<layerSize[R.layer]); };
  bool srcRangeOK(const NNetNodeRange R) { return (R.layer>=0)&&(R.layer<(numLayers-1))&&(R.firstNode>=0)&&(R.lastNode>=R.firstNode)&&(R.lastNode<layerSize[R.layer]); };  // include inputs in allowed range
protected:
  NNet();  // default constructor - intended ONLY for use only when network data is to be loaded from a stream (hence protected)... network is not fully init'ed, and NNet function calls may crash!!!
  bool IsAborted() { return abortTraining; };
  // **************** MUST override these methods to instantiate a NNet...
  virtual void SetInputs(const void* InputPattern) = 0;                                                    // set NNet Input[] array from input pattern data
  virtual void SetTrainingInputValues(int patternNum, const void* TrainingData, float* inputValues) = 0;   // set "inputValues" array from training data
  virtual void SetDesiredOutputs(int patternNum, const void* TrainingData, bool* desiredOutputs) = 0;      // set "desiredOutputs" array from training data
  virtual bool OnEpochComplete(double err) = 0;    // Called after each epoch of training; return true to stop, false to proceed to next epoch (NB may examine "epoch" variable in this function)
  // the following stream-IO methods should throw an exception on failure (any kind except bad_alloc - it doesn't matter)
  virtual void WriteInt(void* OutStream, int i) = 0;             // Writer methods for writing data to a custom stream (return true on success)
  virtual void WriteBool(void* OutStream, bool b) = 0;           //
  virtual void WriteFloat(void* OutStream, float f) = 0;         //
  virtual int ReadInt(void* InStream) = 0;                       // Reader methods...
  virtual bool ReadBool(void* InStream) = 0;                     //
  virtual float ReadFloat(void* InStream) = 0;                   //
  virtual void SkipBytes(void* InStream, int n) = 0;             // Skip next n bytes of input stream (should also throw an exception on failure)
  // *********************************************************************
public:
  // training constants and options...
  // NOTE:  Default values are set in InitNNet().  If more such variables are added, InitNNet() and Load/SaveToStream() methods must be updated appropriately.
  enum TrainingMethodType { BACKPROP, BATCH_BACKPROP, RPROP, LEVENBERG_MARQUARDT } trainingMethod;   // Possible methods to use for training the network;  if more are added be sure to update NNetLastTrainingMethod, below
  bool UnityOutputLayerDerivatives;                // calculate weight changes as if output layer derivatives were 1 (even though they aren't)
  bool BiasDecay;                                  // Apply decay factor to biases as well as links
  float OutputOffValue, OutputOnValue;             // threshold values for training the outputs... NB MUST have OffValue < OnValue
  float LearningRate;                              // learning rate for backprop
  float MomentumFactor;                            // "momentum" term (for backprop only)
  float delta0, deltaMin, deltaMax;                // initial, miniumum and maximum delta for the RProp method
  float mu, mu_min, mu_inc, mu_dec;                // current mu, minimum mu, and increment value/decrement factor for Levenberg-Marquardt
  float WeightDecayFactor;                         // weight decay factor - per epoch for epoch-learning methods, othewise per pattern

  // control and monitor the training process from a different thread (NB The NNet object is NOT thread safe. ONLY the three items below can be used from another thread)
  void AbortTraining() { abortTraining = true; };  // rapidly aborts training (without updating weights for the current epoch, for batch learning methods)
  int CurrentEpoch() { return epoch; };            // returns current training epoch, starting at 1  (NB not synchronized with GetpatternsRemaining())
  int PatternsRemaining() { return patternsRemaining; };  // returns number of input patterns still remaining in current training epoch (NB not synchronized with GetCurrentEpoch())

  // setup, train, and use the NNet
  ~NNet();  // destructor - cleans up allocated memory
  NNet(int numInputs, int numOutputs, int numHiddenlayers, const int* layerSizes, bool createLinks = true);         // init default MultiLayer perceptron feedforward network with specified layers and layer sizes
  NNet(const NNet& src);                // copy constructor  NB thread safety:  src NNet should not be in use (i.e. having its methods executed) by another thread
  bool LoadFromStream(void* InStream);  // load network from a stream (returns true on success)
  bool SaveToStream(void* OutStream);   // save network to stream (returns true on success)
  bool DeleteLink(NNetNode* destNode, int linkNum);                    // delete a link from a node... can specify Node pointer or layer/nodeNum, and
  bool DeleteLink(NNetNode* destNode, int layer, int nodeNum);         // can specify link number or link destination.  Returns true on success.
  bool DeleteLink(const int destLayer, const int destNodeNum, const int linkNum)
    { if ((destLayer<=0)||(destLayer>=numLayers)||(destNodeNum<0)||(destNodeNum>=layerSize[destLayer])) return false; return DeleteLink(Node[destLayer-1]+destNodeNum, linkNum); }
  bool DeleteLink(const int destLayer, const int destNodeNum, const int layer, const int nodeNum)
    { if ((destLayer<=0)||(destLayer>=numLayers)||(destNodeNum<0)||(destNodeNum>=layerSize[destLayer])) return false; return DeleteLink(Node[destLayer-1]+destNodeNum, layer, nodeNum); }
  bool AddOrSetLink(NNetNode* destNode, int layer, int nodeNum, float weight = 0.0f);    // Add a single link to a node, or change the weight of an existing link
  bool AddOrSetLink(const int destLayer, const int destNodeNum, const int layer, const int nodeNum, const float weight = 0.0f)   // as above, but you don't need the node pointer
    { if ((destLayer<=0)||(destLayer>=numLayers)||(destNodeNum<0)||(destNodeNum>=layerSize[destLayer])) return false; return AddOrSetLink(Node[destLayer-1]+destNodeNum, layer, nodeNum, weight); }
  bool SetConnectivity(int srcRangeEntries, const NNetNodeRange* srcRanges, const NNetNodeRange& setRange);      // specify part of the network link graph; returns true on success
  bool SetTransferFn(const NNetNodeRange& setRange, NNetNode::TransferFnType fn);                        // set the transfer fn. for a range of nodes; returns true on success
  bool SetTransferFn(int firstLayer, int lastLayer, NNetNode::TransferFnType fn);                        // set the transfer fn. for nodes in specified layers; ...''...
  void SetTransferFn(const NNetNode::TransferFnType fn) { SetTransferFn(1, numLayers-1, fn); };          // set the transfer fn. for all nodes in the network
  bool NguyenWidrowInitWeights(const NNetNodeRange& setRange, bool checkRange = true);    // init weights & biases using Nguyen-Widrow algorithm (specified nodes)
  bool NguyenWidrowInitWeights(int firstLayer, int lastLayer);                            // init weights & biases using Nguyen-Widrow algorithm (specified layers)
  void NguyenWidrowInitWeights() { NguyenWidrowInitWeights(1, numLayers-1); };            // init weights & biases using Nguyen-Widrow algorithm (whole network)
  bool RandomInitWeights(const NNetNodeRange& setRange, bool checkRange = true);          // init weights & biases randomly; same as NW with 1 node per layer (specified nodes)
  bool RandomInitWeights(int firstLayer, int lastLayer);                                  // init weights & biases randomly (specified layers)
  void RandomInitWeights() { RandomInitWeights(1, numLayers-1); };                        // init weights & biases randomly (whole network)
  void Train(int NumPatterns, const void* TrainingData);            // Train the network
  double MeasureError(int NumPatterns, const void* TrainingData);   // Return normalized error measure of network performance on the training data (doesn't affect network)
  int Classify(const void* const InputPattern) { int c; Classify(InputPattern, 1, &(layerSize[numLayers-1]), &c); return c; };   // Find most probable classification (return value = index of output node with maximum value)
  bool Classify(const void* InputPattern, int numOutputSets, const int* sizes, int* outputClassifications);  // as above, but treats output nodes as being grouped into adjacent sets each of which classifies a separate property
  void ProbabilisticClassify(const void* const InputPattern, double* const probabilities) { ProbabilisticClassify(InputPattern, 1, &(layerSize[numLayers-1]), &probabilities); } ;  // Find probability distribution of classifications
  bool ProbabilisticClassify(const void* InputPattern, int numOutputSets, const int* sizes, double* const * probabilities);   // see Classify().  returns false if params are invalid.

  // NNet inspection functions
  int NumLayers() const { return numLayers; };
  int TotalNumNodes() const { return numNodes; };
  int TotalNumLinks() const { return numLinks; };
  int NumNodesInLayer(int l) const { return ((l<=0)||(l>=numLayers)) ? 0 : layerSize[l]; };
  int NumInputs() const { return layerSize[0]; };
  int NumOutputs() const { return layerSize[numLayers-1]; };
  float InputValue(const int i) const { return ((i>=0)&&(i<layerSize[0])) ? Input[i] : 0.0f; };
  float OutputValue(const int i) const { return ((i>=0)&&(i<layerSize[numLayers-1])) ? Node[numLayers-2][i].output : 0.0f; };
  NNetNode* GetNode(const int layer, const int nodeNum) const { return ((nodeNum>=0)&&(nodeNum<NumNodesInLayer(layer))) ? (Node[layer-1]+nodeNum) : NULL; }
  NNetNode* GetNodesInLayer(const int layer) const { return ((layer>0)&&(layer<numLayers)) ? Node[layer-1] : NULL; };
  // **************** MUST override this method to instantiate a NNet:
  virtual void GetInputRange(int i, float& min, float& max, float& avrg) const = 0;       // returns the min, max, and average values of the i'th input element (node)
};



const int NNetLastTransferFn = NNetNode::SIGMOID_POS;              // MUST update this if new tranfer fns are added!!!
const int NNetLastTrainingMethod = NNet::LEVENBERG_MARQUARDT;      // MUST update this if more training methods are added!!!



#endif
