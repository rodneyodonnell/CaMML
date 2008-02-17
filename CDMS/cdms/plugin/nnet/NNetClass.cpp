#include "NNetClass.h"
#include <math.h>
#include <stdlib.h>
#include <float.h>
//#include <mem.h>
#include <new.h>
#include <string.h>

using namespace std;  // make sure that operator new() and operator new[] throw bad_alloc on failure




/////////////////////////////////////////////////
//  NNetNode class methods
/////////////////////////////////////////////////



// destructor:  deletes all links belonging to node
inline NNetNode::~NNetNode()
{
  delete[] Link;
  lastLink = -1;
}



inline bool NNetNode::DeleteLink(int i)
{
  if ((i>=0)&&(i<=lastLink))
  {
    if (lastLink==0)
    {
      lastLink--;
      Link = NULL;
    }
    else
    {
      NNetLink* NewLinks = new NNetLink[lastLink];
      if (i>0) memcpy(NewLinks, Link, i*sizeof(NNetLink));
      if (i<lastLink) memcpy(NewLinks+i, Link+i+1, (lastLink-i)*sizeof(NNetLink));
      lastLink--;
      delete[] Link;
      Link = NewLinks;
    }
    return true;
  }
  else return false;
}



inline bool NNetNode::DeleteLink(const int layr, const int ndNum)
{
  for (int i=lastLink; i>=0; i--)
    if (((layr==0)&&(Link[i].fromInput==ndNum))||((Link[i].fromInput<0)&&(Link[i].srcNode->nodeNum==ndNum)&&(Link[i].srcNode->layer==layr)))
      return DeleteLink(i);
  return false;
}



// copies the links of another node;  return value = change in total number of links
inline int NNetNode::CopyLinks(const NNetNode& node)
{
  int result = -lastLink;
  NNetLink* temp = new NNetLink[node.NumLinks()];   // if a bad_alloc exception occurs here, no change to the network has been made
  delete[] Link;
  Link = temp;
  lastLink = node.lastLink;
  memcpy(Link, node.Link, (NumLinks())*sizeof(NNetLink));  // copy the link information
  result += lastLink;
  return result;
}



// sum weighted inputs and bias, then apply transfer function
inline void NNetNode::ComputeOutput()
{
  sigma = bias;
  for (int i = lastLink; i >= 0; i--) sigma += Link[i].weightedVal();
  output = f();
}



// f(), the transfer function
inline float NNetNode::f() const
{
  switch (TransferFn)
  {
    case HARD_LIMIT: return (sigma>0) ? 1.0f : -1.0f;
    case HARD_LIMIT_POS: return (sigma>0) ? 1.0f : 0.0f;
    case LINEAR: return sigma;
    case LINEAR_POS: return (sigma>0) ? sigma : 0.0f;
    case SATURATING_LINEAR: return (sigma>=1.0) ? 1.0f : ((sigma<=-1.0) ? -1.0f : sigma);
    case SATURATING_LINEAR_POS: return (sigma>=1.0) ? 1.0f : ((sigma<=0) ? 0.0f : sigma);
    case SIGMOID: return (1.0f - 2.0f/(1.0f+exp(2.0*sigma)));       // defined so that slope = 1 when sigma = 0
    case SIGMOID_POS:  return (1.0f - 1.0f/(1.0f+exp(4.0*sigma)));  // defined so that slope = 1 when sigma = 0
    default: return 0.0f;  // shouldn't happen
  }
}



// derivative f' of the transfer function
inline float NNetNode::Df() const
{
  switch (TransferFn)
  {
    case HARD_LIMIT: return 0.0f;
    case HARD_LIMIT_POS: return 0.0f;
    case LINEAR: return 1.0f;
    case LINEAR_POS: return (sigma>=0) ? 1.0f : 0.0f;
    case SATURATING_LINEAR: return (sigma>1.0) ? 0.0f : ((sigma<-1.0) ? 0.0f : 1.0f);
    case SATURATING_LINEAR_POS: return (sigma>1.0) ? 0.0f : ((sigma<0) ? 0.0f : 1.0f);
    case SIGMOID: return (output+1.0f)*(1.0f-output);
    case SIGMOID_POS: return 4.0f*output*(1.0f-output);
    default: return 0.0f;  // shouldn't happen
  }
}



// obtain the range of output values for the node
// Note that for LINEAR and LINEAR_POS transfer functions, the returned range is [-1, 1] and [0, 1] respectively,
// even though the actual output magnitude can take any value.
inline void NNetNode::inlnGetOutputRange(float& min, float& max) const
{
  switch (TransferFn)
  {
    case HARD_LIMIT:
    case LINEAR:
    case SATURATING_LINEAR:
    case SIGMOID:
      min = -1.0f;
      max = 1.0f;
      break;
    case HARD_LIMIT_POS:
    case LINEAR_POS:
    case SATURATING_LINEAR_POS:
    case SIGMOID_POS:
      min = 0.0f;
      max = 1.0f;
      break;
    default: // shouldn't happen
      min = 0.0f;
      max = 0.0f;
  }
}

// external linkage version
void NNetNode::GetOutputRange(float& min, float& max) const { inlnGetOutputRange(min, max); };



// obtain the range of "active" values for sigma (i.e. where the derivative of the transfer fn. is large)
// Note that for LINEAR and LINEAR_POS transfer functions, the returned range is [-1, 1] and [0, 1] respectively,
// even though the derivative is unity for any (positive for LINEAR_POS) value of sigma.
// The range [-0.5, 0.5] is returned for HARD_LIMIT and HARD_LIMIT_POS, even though the derivative is always zero for these transfer functions
inline void NNetNode::inlnGetActiveSigmaRange(float& min, float& max) const
{
  switch (TransferFn)
  {
    case HARD_LIMIT:
    case HARD_LIMIT_POS:
      min = -0.5;
      max = 0.5;
      break;
    case LINEAR:
    case SATURATING_LINEAR:
      min = -1.0f;
      max = 1.0f;
      break;
    case LINEAR_POS:
    case SATURATING_LINEAR_POS:
      min = 0.0f;
      max = 1.0f;
      break;
    case SIGMOID:
      min = -0.6f;
      max = 0.6f;
      break;
    case SIGMOID_POS:
      min = -0.3f;
      max = 0.3f;
      break;
    default: // shouldn't happen
      min = 0.0f;
      max = 0.0f;
  }
}

// external linkage version
void NNetNode::GetActiveSigmaRange(float& min, float& max) const { inlnGetActiveSigmaRange(min, max); };



// create link array;  return value = change in total number of links
inline int NNetNode::SetLinks(float* const InputArray, NNetNode** const NodeArray, const NNetNodeRange* const srcRanges, const int srcRangeEntries)
{
  int result = -lastLink;
  int l = -1;
  for (int q = srcRangeEntries-1; q >= 0; q--) l += srcRanges[q].lastNode-srcRanges[q].firstNode+1;    // determine how many links are required
  NNetLink* temp = new NNetLink[l+1];     // allocate memory for them;  NB no change to node if bad_alloc exception is raised here
  delete[] Link;                          // delete previous set of links
  Link = temp;
  lastLink = l;
  result += l;
  for (int r = srcRangeEntries-1; r >= 0; r--)
  {
    int n = srcRanges[r].lastNode;
    int inptNum = n;
    int layer = srcRanges[r].layer;
    char* p = ((layer==0) ? (char*) &(InputArray[n]) : (char*) &(NodeArray[layer-1][n]));    // p = address of link source (which can be a node or an input)
    for (n -= srcRanges[r].firstNode; n >= 0; n--, l--)
      if (layer==0)
      {
        Link[l].weight = 0.0f;
        Link[l].fromInput = inptNum--;
        Link[l].srcNode = (NNetNode*) (p - ((char*) &(((NNetNode*) 0)->output)));  // ensure that even for links from inputs (which are stored as an array of floats)
        p -= sizeof(float);                                                        // "srcNode->output" retrieves the correct input value
      }
      else
      {
        Link[l].weight = 0.0f;
        Link[l].fromInput = -1;
        Link[l].srcNode = (NNetNode*) p;
        p -= sizeof(NNetNode);
      }
  }
  return result;
}



// init weights & bias using Nguyen-Widrow algorithm
inline void NNetNode::NguyenWidrowInitWeights(const NNet& Net, const bool RandomInit)
{
  int r = NumLinks();             // Number of links with non-constant inputs
  double biasOffset = 0.0, weightSumSqrs = 0.0;
  int biasSgnAdjust = 2;          // intially a dummy value, signalling that no valid assignment has yet been made
  float min, max, avrg;

  for (int n=0; n<=lastLink; n++)
  {
    if (Link[n].fromInput>=0) Net.GetInputRange(Link[n].fromInput, min, max, avrg);  // obtain range of link input values, and average input value
    else
    {
      Link[n].srcNode->inlnGetOutputRange(min, max);
      avrg = (max+min)*0.5f;                 // assume that hidden layer nodes will have mid-range average output
    }
    if (max==min)          // ignore constant inputs that provide no useful info (decrement r accordingly)
    {
      Link[n].weight = 0.0f;
      r--;
    }
    else
    {
      double w = (2*((double) rand())/RAND_MAX)-1.0;
      weightSumSqrs += w*w;
      w *= 2.0f/(max-min);           // scale weight according to input range ( no adjustment when input ranges from -1 to 1)
      biasOffset += avrg*w;          // adjust bias also
      if (biasSgnAdjust==2) biasSgnAdjust = (w>0 ? 1 : (w<0 ? -1 : 0));  // (part of the) bias has sign adjusted according to FIRST link with non-constant input
      Link[n].weight = w;
    }
  }

  inlnGetActiveSigmaRange(min, max);
  bias = 0.5f*(max+min)-biasOffset;                 // this part of the bias adjusts for active sigma range and the ranges of the link inputs
  if (r == 0) return;      // no non-constant inputs - all links will be zero, and bias will also be zero, not counting adjustment for active sigma range
  float wMag = 0.35f*(max-min);                     // weight vector magnitude, scaled according to active sigma range
  if (!RandomInit)
  {
    int layerSize = Net.layerSize[layer];
    if (layerSize>1)
    {
      wMag *= pow((double) layerSize, 1.0/r);
      bias += biasSgnAdjust*wMag*((((float) (2*nodeNum))/(layerSize-1))-1.0f);
    }
  }
  wMag /= sqrt(weightSumSqrs);
  for (int m=lastLink; m>=0; m--) Link[m].weight *= wMag;           // scale weights (weight vector) appropriately
}



/////////////////////////////////////////////////
//  NNet class methods
/////////////////////////////////////////////////


// protected method:
////////////////////

NNet::NNet() { InitNNet(); };



// private methods:
///////////////////



// set up initial default values, etc.  Called first thing by regular and stream NNet constructors
inline void NNet::InitNNet()
{
  // private member initialization
  numLayers = 0;
  numNodes = 0;
  numLinks = 0;
  layerSize = NULL;
  Node = NULL;
  Input = NULL;
  TrainingSet = NULL;
  numTrainingPatterns = 0;
  epoch = 0;
  patternsRemaining = 0;
  abortTraining = false;

  // public member initialization (training constants and options) - DEFAULTS.  (change these if you like!)
  trainingMethod = LEVENBERG_MARQUARDT;
  OutputOffValue = 0.0f;
  OutputOnValue = 1.0f;             // must have OffValue < OnValue
  UnityOutputLayerDerivatives = true;
  BiasDecay = false;
  LearningRate = 0.2f;
  MomentumFactor = 0.5f;
  delta0 = 0.1f;
  deltaMin = 1e-6f;
  deltaMax = 50.0f;
  mu = 0.3f;
  mu_min = 0.001;
  mu_inc = 0.05f;
  mu_dec = 0.7f;
  WeightDecayFactor = 0.0f;
}



// create a set of preprocessed inputs+desired outputs for training, IF memory is available
// if there is insufficient memory, nothing is created and no exception is thrown
void NNet::CreateTrainingSet(int NumPatterns, const void* TrainingData)
{
  TrainingSet = new(nothrow) TrainingPatterns[NumPatterns];
  if (TrainingSet == NULL) return;
  numTrainingPatterns = 0;
  int numInputs = NumInputs();
  int numOutputs = NumOutputs();
  float* pf;
  bool* pb;
  for (int n = 0; n<NumPatterns; n++)
  {
    pf = new(nothrow) float[numInputs];
    if (pf==NULL) { DeleteTrainingSet(); return; };
    TrainingSet[n].Input = pf;
    pb = new(nothrow) bool[numOutputs];
    if (pb==NULL) { delete[] TrainingSet[n].Input; DeleteTrainingSet(); return; };
    TrainingSet[n].DesiredOutput = pb;
    numTrainingPatterns++;
    SetTrainingInputValues(n, TrainingData, pf);
    SetDesiredOutputs(n, TrainingData, pb);
  }
}



// free memory used for creating training set
void NNet::DeleteTrainingSet()
{
  for(int i = numTrainingPatterns-1; i>=0; i--)
  {
    delete[] TrainingSet[i].Input;
    delete[] TrainingSet[i].DesiredOutput;
  }
  delete[] TrainingSet;
  TrainingSet = NULL;
  numTrainingPatterns = 0;
}



inline void NNet::GetTrainingPattern(const int n, bool*& DesiredOutput, const void* const TrainingData)
{
  if (DesiredOutput==NULL) // means that patterns are in TrainingSet array
  {
    memcpy(Input, TrainingSet[n].Input, NumInputs()*sizeof(float));
    DesiredOutput = TrainingSet[n].DesiredOutput;
  }
  else                     // no training patterns pre-stored, so must re-compute
  {
    SetTrainingInputValues(n, TrainingData, Input);
    SetDesiredOutputs(n, TrainingData, DesiredOutput);
  }
}



inline void NNet::FeedForward()
{
  int l, n, lastLayer = numLayers-2;
  NNetNode* Nd;
  for (l = 0; l<=lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]-1; n>=0; Nd++, n--) Nd->ComputeOutput();
}



// BACKPROPAGATION
//////////////////



inline void NNet::PrepareToBackProp(float*& prev_dw)
{
  int l, n, lastLayer = numLayers-2;
  NNetNode* Nd;
  for (l = 0; l<=lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]-1; n>=0; Nd++, n--) Nd->TData = new double;
  prev_dw = new float[numLinks+numNodes];         // reserve space for each weight and bias
  memset(prev_dw, 0, sizeof(float)*(numLinks+numNodes));         // set to 0
}



inline void NNet::FinishedBackProp(float* const prev_dw)
{
  int l, n, lastLayer = numLayers-2;
  NNetNode* Nd;
  delete[] prev_dw;
  for (l = 0; l<=lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]-1; n>=0; Nd++, n--) { delete ((double*) (Nd->TData)); Nd->TData = NULL; };   // un-reserve space for error at each node
}



inline double NNet::DoIterationBackProp(const bool* const DesiredOutput, float* const prev_dw)   // return value = sum of squared error of the outputs
{
  int l, n, lastLayer = numLayers-2;
  NNetNode* Nd;
  NNetLink* Link;
  double temp, result = 0.0;

  // feed forward
  for (l = 0; l<lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]; n>0; Nd++, n--)
    {
      Nd->ComputeOutput();
      *((double*) (Nd->TData)) = 0.0;     // TData points to a double-type value, the error at that node (actually, d(err^2)/d(output))
    }
  for (Nd = Node[lastLayer]+(n = layerSize[lastLayer+1]-1); n>=0; Nd--, n--)  // final layer
  {
    Nd->ComputeOutput();
    temp = DesiredOutput[n] ? (Nd->output>=OutputOnValue ? 0.0 : (Nd->output-OutputOnValue)) :           // compute error based on desired output
                              (Nd->output<=OutputOffValue ? 0.0 : (Nd->output-OutputOffValue));
    *((double*) (Nd->TData)) = temp;
    result += temp*temp;      // compute sum of squared errors
  }

  // back-propagate
  int i, p = numLinks+numNodes-1;
  float dw;
  for (l = lastLayer; l>=0; l--)
    for (Nd = Node[l]+(n = (layerSize[l+1]-1)); n>=0; Nd--, n--)
    {
      double delta = *((double*) (Nd->TData));
      if ((l!=lastLayer)||(!UnityOutputLayerDerivatives)) delta *= Nd->Df();   // adjust error according to transfer fn. derivative f'
      dw = MomentumFactor*prev_dw[p]-LearningRate*delta;                 // adjust the bias
      prev_dw[p--] = dw;
      if (BiasDecay) Nd->bias *= (1.0f-WeightDecayFactor);
      Nd->bias += dw;
      for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
      {
        if (Link->fromInput<0) *((double*)(Link->srcNode->TData)) += delta*Link->weight;     // backpropagate error down links (except not to inputs)
        dw = MomentumFactor*prev_dw[p]-LearningRate*delta*Link->srcValue();                  // adjust link weight
        prev_dw[p--] = dw;
        Link->weight *= (1.0f-WeightDecayFactor);
        Link->weight += dw;
      }
    }

  return result;
}



inline void NNet::TrainBackProp(const int NumPatterns, const void* const TrainingData)
{
  float* prev_dw = NULL;              // previous change in value, for each weight and bias
  bool* DesiredOutputs = NULL;
  try
  {
    PrepareToBackProp(prev_dw);  // init backprop-specific data structures
    CreateTrainingSet(NumPatterns, TrainingData);
    if (numTrainingPatterns==0) DesiredOutputs = new bool[NumOutputs()];    // make a place to store desired outputs IF they have to be generated on-the-fly
    double totalSquaredError;
    int errDenominator = NumOutputs()*NumPatterns;   // number of outputs * number of training patterns
    do
    {
      patternsRemaining = NumPatterns;
      epoch++;
      totalSquaredError = 0;
      for (int n = 0; (n<NumPatterns)&&(!abortTraining); n++)
      {
        GetTrainingPattern(n, DesiredOutputs, TrainingData);                     // Get input and desired output
        totalSquaredError += DoIterationBackProp(DesiredOutputs, prev_dw);
        patternsRemaining--;
      }
    }
    while (!(abortTraining||OnEpochComplete(totalSquaredError/errDenominator)));
  }
  catch(...)      // ensure training data structures are deleted even if an exception is raised
  {
    delete[] DesiredOutputs;
    DeleteTrainingSet();
    FinishedBackProp(prev_dw);
    throw;
  }
  delete[] DesiredOutputs;
  DeleteTrainingSet();
  FinishedBackProp(prev_dw);  // delete backprop-specific data structures
}



// BATCH BACKPROPAGATION
////////////////////////



inline void NNet::PrepareToBatchBackProp(double*& dw, float*& prev_dw)
{
  PrepareToBackProp(prev_dw);      // data structures are basically the same as for backprop
  dw = new double[numLinks+numNodes];         // reserve space for running dw, for each weight and bias
  memset(dw, 0, sizeof(double)*(numLinks+numNodes));         // set to 0
}



inline void NNet::FinishedBatchBackProp(double* const dw, float* const prev_dw)
{
  delete[] dw;
  FinishedBackProp(prev_dw);
}



inline double NNet::DoIterationBatchBackProp(const bool* const DesiredOutput, double* const dw)   // return value = sum of squared error of the outputs
{
  int l, n, lastLayer = numLayers-2;
  NNetNode* Nd;
  NNetLink* Link;
  double temp, result = 0.0;

  // feed forward
  for (l = 0; l<lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]; n>0; Nd++, n--)
    {
      Nd->ComputeOutput();
      *((double*) (Nd->TData)) = 0.0;     // TData points to a double-type value, the error at that node (actually, d(err^2)/d(output))
    }
  for (Nd = Node[lastLayer]+(n = layerSize[lastLayer+1]-1); n>=0; Nd--, n--)  // final layer
  {
    Nd->ComputeOutput();
    temp = DesiredOutput[n] ? (Nd->output>=OutputOnValue ? 0.0 : (Nd->output-OutputOnValue)) :           // compute error based on desired output
                              (Nd->output<=OutputOffValue ? 0.0 : (Nd->output-OutputOffValue));
    *((double*) (Nd->TData)) = temp;
    result += temp*temp;      // compute sum of squared errors
  }

  // back-propagate
  int i, p = numLinks+numNodes-1;
  for (l = lastLayer; l>=0; l--)
    for (Nd = Node[l]+(n = (layerSize[l+1]-1)); n>=0; Nd--, n--)
    {
      double delta = *((double*) (Nd->TData));
      if ((l!=lastLayer)||(!UnityOutputLayerDerivatives)) delta *= Nd->Df();   // adjust error according to transfer fn. derivative f'
      dw[p--] -= delta;                  // running d(error^2)/d(bias)
      for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
      {
        if (Link->fromInput<0) *((double*)(Link->srcNode->TData)) += delta*Link->weight;     // backpropagate error down links (except not to inputs)
        dw[p--] -= delta*Link->srcValue();                  // running d(error^2)/d(weight)
      }
    }

  return result;
}



inline void NNet::ProcessEpochBatchBackProp(double* const dw, float* const prev_dw)     // Update NNet on completion of training epoch (BackProp algorithm)
{
  int l, n, i, lastLayer = numLayers-2, p = numLinks+numNodes-1;
  NNetNode* Nd;
  NNetLink* Link;
  float temp;
  for (l = lastLayer; l>=0; l--)
    for (Nd = Node[l]+(n = (layerSize[l+1]-1)); n>=0; Nd--, n--)
    {
      temp = MomentumFactor*prev_dw[p]+LearningRate*dw[p];                 // adjust bias
      prev_dw[p] = temp;
      dw[p--] = 0.0;        // ensure dw[] array is reset to zero for next epoch
      if (BiasDecay) Nd->bias *= (1.0f-WeightDecayFactor);
      Nd->bias += temp;
      for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
      {
        temp = MomentumFactor*prev_dw[p]+LearningRate*dw[p];               // adjust link weight
        prev_dw[p] = temp;
        dw[p--] = 0.0;      // ensure dw[] array is reset to zero for next epoch
        Link->weight *= (1.0f-WeightDecayFactor);
        Link->weight += temp;
      }
    }
}



inline void NNet::TrainBatchBackProp(const int NumPatterns, const void* const TrainingData)
{
  double* dw = NULL;                  // running (cumulative) derivatives corresponding to each weight and bias
  float* prev_dw = NULL;              // previous change in value, for each weight and bias
  bool* DesiredOutputs = NULL;
  try
  {
    PrepareToBatchBackProp(dw, prev_dw);  // init batchbackprop-specific data structures
    CreateTrainingSet(NumPatterns, TrainingData);
    if (numTrainingPatterns==0) DesiredOutputs = new bool[NumOutputs()];    // make a place to store desired outputs IF they have to be generated on-the-fly
    double totalSquaredError;
    int errDenominator = NumOutputs()*NumPatterns;   // number of outputs * number of training patterns
    do
    {
      patternsRemaining = NumPatterns;
      epoch++;
      totalSquaredError = 0;
      for (int n = 0; (n<NumPatterns)&&(!abortTraining); n++)
      {
        GetTrainingPattern(n, DesiredOutputs, TrainingData);                     // Get input and desired output
        totalSquaredError += DoIterationBatchBackProp(DesiredOutputs, dw);
        patternsRemaining--;
      }
      if (abortTraining) break;
      ProcessEpochBatchBackProp(dw, prev_dw);
    }
    while (!OnEpochComplete(totalSquaredError/errDenominator));
  }
  catch(...)      // ensure training data structures are deleted even if an exception is raised
  {
    delete[] DesiredOutputs;
    DeleteTrainingSet();
    FinishedBatchBackProp(dw, prev_dw);
    throw;
  }
  delete[] DesiredOutputs;
  DeleteTrainingSet();
  FinishedBatchBackProp(dw, prev_dw);  // delete batchbackprop-specific data structures
}



// RPROP
////////



inline void NNet::PrepareToRProp(float*& delta, double*& dw, float*& prev_dw)
{
  PrepareToBatchBackProp(dw, prev_dw);         // data structures are basically the same as for batch-backprop
  delta = new float[numLinks+numNodes];    // reserve space for delta values, for each weight and bias
  for (int i = numLinks+numNodes-1; i>=0; i--) delta[i] = delta0;
}



inline void NNet::FinishedRProp(float* const delta, double* const dw, float* const prev_dw)
{
  delete[] delta;
  FinishedBatchBackProp(dw, prev_dw);
}


inline float NNet::RProp_ParameterUpdate(float& delta, double& dw, float& prev_dw)
{
  float result;
  if ((dw==0)||(prev_dw==0))
  {
    result = (dw>0) ? delta : ((dw<0) ? -delta : 0.0f);
    prev_dw = dw;
  }
  else if ((dw>0)==(prev_dw>0))
  {
    delta *= 1.2f;
    if (delta>deltaMax) delta = deltaMax;
    result = (dw>0) ? delta : ((dw<0) ? -delta : 0.0f);
    prev_dw = dw;
  }
  else
  {
    result = (prev_dw>0) ? -delta : ((prev_dw<0) ? delta : 0.0f);  // cancellation of previous weight change
    delta *= 0.5;
    if (delta<deltaMin) delta = deltaMin;
    prev_dw = 0.0f;
  }
  dw = 0.0;   // ensure dw's are zero for next epoch
  return result;
}



inline void NNet::ProcessEpochRProp(float* const delta, double* const dw, float* const prev_dw)     // Update NNet on completion of training epoch (RProp algorithm)
{
  int l, n, i, lastLayer = numLayers-2, p = numLinks+numNodes-1;
  NNetNode* Nd;
  NNetLink* Link;
  for (l = lastLayer; l>=0; l--)
    for (Nd = Node[l]+(n = (layerSize[l+1]-1)); n>=0; Nd--, n--)
    {
      if (BiasDecay) Nd->bias *= (1.0f-WeightDecayFactor);                 // adjust bias
      Nd->bias += RProp_ParameterUpdate(delta[p], dw[p], prev_dw[p]);
      p--;
      for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
      {
        Link->weight *= (1.0f-WeightDecayFactor);                          // adjust link weight
        Link->weight += RProp_ParameterUpdate(delta[p], dw[p], prev_dw[p]);
        p--;
      }
    }
}



inline void NNet::TrainRProp(const int NumPatterns, const void* const TrainingData)
{
  abortTraining = false;
  float* delta = NULL;                // adjustment size for each weight and bias
  double* dw = NULL;                  // running (cumulative) derivatives corresponding to each weight and bias
  float* prev_dw = NULL;              // previous change in value, for each weight and bias
  bool* DesiredOutputs = NULL;
  try
  {
    PrepareToRProp(delta, dw, prev_dw);  // init RProp-specific data structures
    CreateTrainingSet(NumPatterns, TrainingData);
    if (numTrainingPatterns==0) DesiredOutputs = new bool[NumOutputs()];    // make a place to store desired outputs IF they have to be generated on-the-fly
    double totalSquaredError;
    int errDenominator = NumOutputs()*NumPatterns;   // number of outputs * number of training patterns
    do
    {
      patternsRemaining = NumPatterns;
      epoch++;
      totalSquaredError = 0;
      for (int n = 0; (n<NumPatterns)&&(!abortTraining); n++)
      {
        GetTrainingPattern(n, DesiredOutputs, TrainingData);                     // Get input and desired output
        totalSquaredError += DoIterationBatchBackProp(DesiredOutputs, dw);       // RPROP iterations same as for Batch-BackProp
        patternsRemaining--;
      }
      if (abortTraining) break;
      ProcessEpochRProp(delta, dw, prev_dw);
    }
    while (!OnEpochComplete(totalSquaredError/errDenominator));
  }
  catch(...)      // ensure training data structures are deleted even if an exception is raised
  {
    delete[] DesiredOutputs;
    DeleteTrainingSet();
    FinishedRProp(delta, dw, prev_dw);
    throw;
  }
  delete[] DesiredOutputs;
  DeleteTrainingSet();
  FinishedRProp(delta, dw, prev_dw);  // delete RProp-specific data structures
}



// LEVENBERG-MARQUARDT
//////////////////////



inline void NNet::PrepareToLevenbergMarquardt(double*& E, double*& JtE, double**& JtJ, const int lastParam, const int lastOutput)
{
  int l, n, lastLayer = numLayers-2;
  int numParams = lastParam+1, numOutputs = lastOutput+1;
  NNetNode* Nd;
  for (l = 0; l<lastLayer; l++)                                                                         // needn't store all error derivatives for last layer,
    for (Nd = Node[l], n = layerSize[l+1]-1; n>=0; Nd++, n--) Nd->TData = new double[numOutputs];       // since d(output x)/d(output y) = 0 for all x != y.
  for (Nd = Node[lastLayer], n = layerSize[lastLayer+1]-1; n>=0; Nd++, n--) Nd->TData = new double;     // Note: TData[1] = error, TData[0] = d(error)/d(sigma)
  E = new double[layerSize[lastLayer+1]];
  JtE = new double[numParams];
  memset(JtE, 0, sizeof(double)*numParams);         // set JtE vector to 0
  JtJ = new double*[numParams];
  memset(JtJ, 0, sizeof(double*)*numParams);        // just in case there is a bad_alloc exception next, and delete[] is called on some of these elements
  double** JtJRow = JtJ;
  for (n = 0; n<numParams; JtJRow++, n++)
  {
    *JtJRow = new double[n+1];                      // NB element  JtJ[a][b]  is stored only if b<=a
    memset(*JtJRow, 0, sizeof(double)*(n+1));       // set JtJ matrix elements to zero
  }
}



inline void NNet::FinishedLevenbergMarquardt(double* const E, double* const JtE, double** const JtJ, const int lastParam)
{
  int l, n, lastLayer = numLayers-2;
  NNetNode* Nd;
  for (double** JtJRow = (JtJ+lastParam); JtJRow>=JtJ; JtJRow--) delete[] (*JtJ);
  delete[] JtJ;
  delete[] JtE;
  delete[] E;
  for (l = 0; l<lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]-1; n>=0; Nd++, n--) { delete[] ((double*) (Nd->TData)); Nd->TData = NULL; };
  for (Nd = Node[lastLayer], n = layerSize[lastLayer+1]-1; n>=0; Nd++, n--) { delete ((double*) (Nd->TData)); Nd->TData = NULL; };
}



inline double NNet::DoIterationLevenbergMarquardt(const bool* const DesiredOutput, double* const E, double* const JtE, double** const JtJ, const int lastParam, const int lastOutput)
// return value = sum of squared error of the outputs
{
  int l, n, lastLayer = numLayers-2;
  int i, j, k, l2, m, q, NodeP, p = lastParam;
  NNetNode *Nd, *Nd2;
  NNetLink *Link, *Link2;
  double temp, temp2, linkSrcVal, result = 0.0;
  double *TData, *TData2, *srcTData;

  // feed forward
  m = lastOutput+1;   // m = numOutputs
  for (l = 0; l<lastLayer; l++)
    for (Nd = Node[l], n = layerSize[l+1]; n>0; Nd++, n--)
    {
      Nd->ComputeOutput();
      memset(Nd->TData, 0, sizeof(double)*m);     // TData points to an array of double-type values, the error at that node wrt each output (actually, d(err^2)/d(output))
    }
  for (Nd = Node[lastLayer]+(n=lastOutput); n>=0; Nd--, n--)  // final layer
  {
    Nd->ComputeOutput();
    temp = DesiredOutput[n] ? (Nd->output>=OutputOnValue ? 0.0 : (Nd->output-OutputOnValue)) :           // compute error based on desired output
                              (Nd->output<=OutputOffValue ? 0.0 : (Nd->output-OutputOffValue));
    E[n] = temp;
    *((double*) (Nd->TData)) = 1.0;
    result += temp*temp;      // compute sum of squared errors
  }

  // back-propagate
  for (Nd = Node[lastLayer]+(n = lastOutput); n>=0; Nd--, n--)   // special processing for output layer
  {
    if (!UnityOutputLayerDerivatives) *((double*) (Nd->TData)) *= Nd->Df();   // adjust error according to transfer fn. derivative f'
    temp = *((double*) (Nd->TData));
    JtE[p] += E[n]*temp;                                           // JtE - bias weight term
    JtJ[p][p] += temp*temp;                                        // JtJ - bias weight term*bias weight term
    NodeP = p;
    p--;
    for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
    {
      if (Link->fromInput<0) ((double*)(Link->srcNode->TData))[n] += temp*Link->weight;     // backpropagate error down links (except not to inputs)
      JtE[p] += E[n]*(temp2 = temp*Link->srcValue());              // JtE - link weight term
      q = NodeP;
      JtJ[q--][p] += temp2*temp;                                   // JtJ - link weight term*bias weight term
      for (Link2 = Nd->Link; q>=p; Link2++) JtJ[q--][p] += temp2*temp*Link2->srcValue();   // JtJ - link weight term*link weight term
      p--;
    }
  }
  for (l = lastLayer-1; l>=0; l--)                                 // processing for remaining layers...
    for (Nd = Node[l]+(n = (layerSize[l+1]-1)); n>=0; Nd--, n--)
    {
      TData = (double*) (Nd->TData);
      temp2 = Nd->Df();
      temp = 0.0;
      for (k = lastOutput; k>=0; k--) temp += E[k]*(TData[k] *= temp2);  // adjust derivative by f', and add up JtE bias weight term contributions
      JtE[p] += temp;                                                    // JtE - bias weight term;  temp = sum-over-k(E[k]*dE[k]/d(sigma))
      // compute JtJ bias weight term:
      q = lastParam;
      for (Nd2 = Node[lastLayer]+(m = lastOutput); m>=0; Nd2--, m--)  // output layer
      {
        JtJ[q--][p] += (temp2 = TData[m]*(*((double*) (Nd2->TData))));                                            // JtJ - bias weight term*output node bias weight term
        for (Link2 = Nd2->Link, j = Nd2->lastLink; j >= 0; Link2++, j--) JtJ[q--][p] += temp2*Link2->srcValue();  // JtJ - bias weight term*output node link weight term
      }
      for (l2 = lastLayer-1; ; l2--)                             // other layers (NB loop stops when Nd==Nd2)
        for (Nd2 = Node[l2]+(m = (layerSize[l2+1]-1)); m>=0; Nd2--, m--)
        {
          TData2 = (double*) (Nd2->TData);
          temp2 = 0.0;
          for (k = lastOutput; k>=0; k--) temp2 += TData[k]*TData2[k];
          JtJ[q--][p] += temp2;                                                                                     // JtJ - bias weight term*bias weight term
          if (Nd==Nd2) goto EXIT_NESTED_1;
          for (Link2 = Nd2->Link, j = Nd2->lastLink; j >= 0; Link2++, j--) JtJ[q--][p] += temp2*Link2->srcValue();  // JtJ - bias weight term*link weight term
        }
EXIT_NESTED_1:
      p--;
      for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
      {
        srcTData = (double*) (Link->srcNode->TData);
        if (Link->fromInput<0) for (k = lastOutput; k>=0; k--) srcTData[k] += TData[k]*Link->weight;     // backpropagate error down links (except not to inputs)
        JtE[p] += temp*(linkSrcVal = Link->srcValue());                              // JtE - link weight term
        // compute JtJ link weight term
        q = lastParam;
        for (Nd2 = Node[lastLayer]+(m = lastOutput); m>=0; Nd2--, m--)  // output layer
        {
          JtJ[q--][p] += (temp2 = TData[m]*(*((double*) (Nd2->TData)))*linkSrcVal);                                 // JtJ - link weight term*output node bias weight term
          for (Link2 = Nd2->Link, j = Nd2->lastLink; j >= 0; Link2++, j--) JtJ[q--][p] += temp2*Link2->srcValue();  // JtJ - link weight term*output node link weight term
        }
        for (l2 = lastLayer-1; ; l2--)                             // other layers (NB loop stops when q<p)
          for (Nd2 = Node[l2]+(m = (layerSize[l2+1]-1)); m>=0; Nd2--, m--)
          {
            TData2 = (double*) (Nd2->TData);
            temp2 = 0.0;
            for (k = lastOutput; k>=0; k--) temp2 += TData[k]*TData2[k]*linkSrcVal;
            JtJ[q--][p] += temp2;                                                                                     // JtJ - link weight term*bias weight term
            j = Nd2->lastLink;
            if (j>(q-p)) j=q-p;
            for (Link2 = Nd2->Link; j >= 0; Link2++, j--) JtJ[q--][p] += temp2*Link2->srcValue();  // JtJ - link weight term*link weight term
            if (q<p) goto EXIT_NESTED_2;
          }
EXIT_NESTED_2:
        p--;
      }
    }

  return result;
}



inline void NNet::ProcessEpochLevenbergMarquardt(double* const JtE, double** const JtJ, const int lastParam, const double lastErr, const double currentErr)   // Update NNet on completion of training epoch (LM algorithm)
{
  int l, n, i, j, k, s, lastLayer = numLayers-2;
  double temp, temp2, temp3;
  bool muError = false;
  NNetNode* Nd;
  NNetLink* Link;
  double *JtJ_j, *Dw;
  // We need to solve (JtJ+mu.I).Dw = JtE.   Since (JtJ+mu.I) is symmetric, positive definite, we use Cholesky's method and factorise to LU form with U = transpose L
  for (j = 0; j<=lastParam; j++)         // run through matrix JtJ... when done resulting matrix = L, where (JtJ+mu.I) = L.[transpose of L]
  {
    temp = 0.0;
    for (k = 0; k<j; k++)
    {
      temp2 = JtJ[j][k];
      for (s = 0; s<k; s++) temp2 -= JtJ[j][s]*JtJ[k][s];
      temp3 = JtJ[k][k];
      if (ldexp(temp3, 50)<=fabs(temp2))
      {
        muError = true;     // effectively division-by-zero;  result magnitude would be >= 2^50;
        goto UPDATE_MU;     // skip weight update
      }
      else temp2 /= temp3;
      JtJ[j][k] = temp2;
      temp += temp2*temp2;
    }
    temp2 = JtJ[j][j]+mu-temp;
    if (temp2<=0)
    {
      muError = true;     // shouldn't happen unless mu = approx. 0, then can be due to round off error... result would be a non-invertible L matrix or a div-by-zero err
      goto UPDATE_MU;     // skip weight update
    }
    JtJ[j][j] = sqrt(temp2);
    if (abortTraining) return;  // rapid exit if abortTraining gets set
  }
  for (j = 0; j<=lastParam; j++)         // solve L.x = JtE, and store result (x) in JtE.
  {
    JtJ_j = JtJ[j];
    temp = JtE[j];
    for (k = 0; k<j; k++) temp -= JtJ_j[k]*JtE[k];
    temp2 = JtJ[j][j];
    if (ldexp(temp2, 50)<=fabs(temp))
    {
      muError = true;     // effectively division-by-zero;  result magnitude would be >= 2^50;
      goto UPDATE_MU;     // skip weight update
    }
    JtE[j] = temp/temp2;
    if (abortTraining) return;  // rapid exit if abortTraining gets set
  }
  for (j = lastParam; j>=0; j--)         // solve [transpose of L].y = x, and store result (y) in JtE.
  {
    JtJ_j = JtJ[j];
    temp = JtE[j];
    temp2 = JtJ[j][j];
    if (ldexp(temp2, 50)<=fabs(temp))
    {
      muError = true;     // effectively division-by-zero;  result magnitude would be >= 2^50;
      goto UPDATE_MU;     // skip weight update
    }
    JtE[j] = (temp /= temp2);
    for (k = j-1; k>=0; k--) JtE[k] -= JtJ_j[k]*temp;
    if (abortTraining) return;  // rapid exit if abortTraining gets set
  }
  // Now Dw = JtE, so we can update the weights
  Dw = JtE+lastParam;
  for (l = lastLayer; l>=0; l--)
    for (Nd = Node[l]+(n = (layerSize[l+1]-1)); n>=0; Nd--, n--)
    {
      if (BiasDecay) Nd->bias *= (1.0f-WeightDecayFactor);                 // adjust bias
      Nd->bias -= (*Dw);
      Dw--;
      for (Link = Nd->Link, i = Nd->lastLink; i >= 0; Link++, i--)
      {
        Link->weight *= (1.0f-WeightDecayFactor);                          // adjust link weight
        Link->weight -= (*Dw);
        Dw--;
      }
    }
UPDATE_MU:
  LevenbergMarquardt_UpdateMu(lastErr, currentErr, muError);               // update mu
  memset(JtE, 0, sizeof(double)*(lastParam+1));                            // reset JtE vector to 0
  for (n = 0; n<=lastParam; n++) memset(JtJ[n], 0, sizeof(double)*(n+1));  // reset JtJ matrix elements to zero
}



// Update the L-M method's "mu" parameter
inline void NNet::LevenbergMarquardt_UpdateMu(const double lastErr, const double currentErr, const bool numericalProblem)
{
  if ((currentErr>lastErr)||numericalProblem) mu += mu_inc;
  else
  { mu *= mu_dec;
    if (mu<mu_min) mu = mu_min;
  }
}



inline void NNet::TrainLevenbergMarquardt(const int NumPatterns, const void* const TrainingData)
{
  int lastParam = numLinks+numNodes-1;    // Adjustable parameters = link wieghts and node biases
  int lastOutput = NumOutputs()-1;
  double* E = NULL;                      // error vector
  double* JtE = NULL;                    // [transpose J].[error vector]  - this is a vector
  double** JtJ = NULL;                   // [transpose of J].[J]  - symmetric matrix, only lower left half stored
  bool* DesiredOutputs = NULL;
  try
  {
    PrepareToLevenbergMarquardt(E, JtE, JtJ, lastParam, lastOutput);  // init Levenberg-Marquardt-specific data structures
    CreateTrainingSet(NumPatterns, TrainingData);
    if (numTrainingPatterns==0) DesiredOutputs = new bool[lastOutput+1];    // make a place to store desired outputs IF they have to be generated on-the-fly
    double lastAvrgErr, totalSquaredError = DBL_MAX;
    int errDenominator = 2*NumOutputs()*NumPatterns;   // number of outputs * number of training patterns
    do
    {
      patternsRemaining = NumPatterns;
      epoch++;
      lastAvrgErr = totalSquaredError;
      totalSquaredError = 0;
      for (int n = 0; (n<NumPatterns)&&(!abortTraining); n++)
      {
        GetTrainingPattern(n, DesiredOutputs, TrainingData);                     // Get input and desired output
        totalSquaredError += DoIterationLevenbergMarquardt(DesiredOutputs, E, JtE, JtJ, lastParam, lastOutput);
        patternsRemaining--;
      }
      if (abortTraining) break;
      totalSquaredError /= errDenominator;
      ProcessEpochLevenbergMarquardt(JtE, JtJ, lastParam, lastAvrgErr, totalSquaredError);
      if (abortTraining) break;
    }
    while (!OnEpochComplete(totalSquaredError));
  }
  catch(...)      // ensure training data structures are deleted even if an exception is raised
  {
    delete[] DesiredOutputs;
    DeleteTrainingSet();
    FinishedLevenbergMarquardt(E, JtE, JtJ, lastParam);  // delete Levenberg-Marquardt-specific data structures
    throw;
  }
  delete[] DesiredOutputs;
  DeleteTrainingSet();
  FinishedLevenbergMarquardt(E, JtE, JtJ, lastParam);    // delete Levenberg-Marquardt-specific data structures
}



// public methods:
//////////////////



// destructor - cleans up allocated memory
NNet::~NNet()
{
  DeleteTrainingSet();
  for(int l = numLayers-2; l>=0; l--) delete[] Node[l];
  delete[] Node;
  delete[] Input;
  delete[] layerSize;
}



// init default MultiLayer perceptron feedforward network with specified layers and layer sizes
NNet::NNet(int numInputs, int numOutputs, int numHiddenLayers, const int* layerSizes, bool createLinks)
{
  InitNNet();
  if (numHiddenLayers<0) numHiddenLayers=0;
  if (numInputs<=0) numInputs=1;
  if (numOutputs<=0) numOutputs=1;
  layerSize = new int[numHiddenLayers+2];
  try
  {
    layerSize[0] = numInputs;
    if (numHiddenLayers>0) memcpy(layerSize+1, layerSizes, numHiddenLayers*sizeof(int));
    layerSize[numHiddenLayers+1] = numOutputs;
    Node = new NNetNode* [numHiddenLayers+1];
    Input = new float[numInputs];
    numLayers = 1;
    for (int l = 0; l <= numHiddenLayers; l++)
    {
      if (layerSize[numLayers]<=0) layerSize[numLayers] = 1;
      int n = layerSize[numLayers];
      NNetNode* Nd = (Node[l] = new NNetNode[n]);
      numLayers++;
      numNodes += n;
      for (n--; n>=0 ; n--) { Nd[n].layer = l+1; Nd[n].nodeNum = n; };
    }
    if (createLinks)
    {
      NNetNodeRange range = { 0, 0, (numInputs-1) };
      NNetNode *Nd, *aNode;
      for (int l = 0; l < (numLayers-1); l++)  // iterate through source layers
      {
        int n = layerSize[l+1]-1; // last node in destination layer
        aNode = &(Node[l][n]);       // remember this node...
        numLinks += aNode->SetLinks(Input, Node, &range, 1);
        range.layer++;            // prepare new range to use for next layer
        range.lastNode = n;
        for (n--, Nd = Node[l]; n>=0; n--) numLinks += Nd[n].CopyLinks(*aNode);  // copy the links to all remaining nodes in layer (quicker than calling SetLinks() multiple times)
      }
    }
  }
  catch(...)
  {
    for(numLayers -= 2; numLayers >= 0; numLayers--) delete[] Node[numLayers];
    delete[] Node;
    Node = NULL;
    delete[] Input;
    Input = NULL;
    delete[] layerSize;
    layerSize = NULL;
    numLayers = 0;
    numNodes = 0;
    numLinks = 0;
    throw;     // re-throw exception
  }
}



// copy constructor
NNet::NNet(const NNet& src)
{
  Node = NULL;
  Input = NULL;
  TrainingSet = NULL;
  numTrainingPatterns = 0;
  epoch = 0;
  patternsRemaining = 0;
  abortTraining = false;
  layerSize = new int[src.numLayers];
  numLayers = 1;
  try
  {
    memcpy(layerSize, src.layerSize, src.numLayers*sizeof(int));
    Node = new NNetNode* [src.numLayers-1];
    Input = new float[NumInputs()];
    for (int l = 0; l < src.numLayers-1; l++)
    {
      int n = layerSize[numLayers];
      NNetNode* Nd = (Node[l] = new NNetNode[n]);
      NNetNode* srcNd = src.Node[l];
      memcpy(Nd, srcNd, n*sizeof(NNetNode));
      for(n--; n>=0; n--) {Nd[n].TData = NULL;  Nd[n].Link = NULL; }  // must do this first in case a bad_alloc is raised below
      for( n=layerSize[numLayers]; n>=0; n--)
      {
        int i = Nd[n].NumLinks();
        NNetLink* Link = (Nd[n].Link = new NNetLink[i]);
        memcpy(Link, srcNd[n].Link, i*sizeof(NNetLink));
        for(i--; i>=0; i--)
          Link[i].srcNode = (Link[i].fromInput>=0) ? ((NNetNode*) (((char*) (Input+Link[i].fromInput)) - ((char*) &(((NNetNode*) 0)->output)))) :
                                                     Node[Link[i].srcNode->layer-1]+(Link[i].srcNode->nodeNum) ;
      }
      numLayers++;
    }
  }
  catch(...)
  {
    for(numLayers -= 2; numLayers >= 0; numLayers--) delete[] Node[numLayers];
    delete[] Node;
    Node = NULL;
    delete[] Input;
    Input = NULL;
    delete[] layerSize;
    layerSize = NULL;
    numLayers = 0;
    numNodes = 0;
    numLinks = 0;
    throw;     // re-throw exception
  }
  numNodes = src.numNodes;
  numLinks = src.numLinks;
  trainingMethod = src.trainingMethod;
  OutputOffValue = src.OutputOffValue;
  OutputOnValue = src.OutputOnValue;
  UnityOutputLayerDerivatives = src.UnityOutputLayerDerivatives;
  BiasDecay = src.BiasDecay;
  LearningRate = src.LearningRate;
  MomentumFactor = src.MomentumFactor;
  delta0 = src.delta0;
  deltaMin = src.deltaMin;
  deltaMax = src.deltaMax;
  mu = src.mu;
  mu_min = src.mu_min;
  mu_inc = src.mu_inc;
  mu_dec = src.mu_dec;
  WeightDecayFactor = src.WeightDecayFactor;
}



// load network from a stream (returns true on success)
bool NNet::LoadFromStream(void* InStream)
{
  class StreamErr{};      // for use as an exception type, to throw (type is irrelevant so long as it is different from bad_alloc)
  this->NNet::~NNet();    // delete any existing information in base NNet class
  InitNNet();   // default initialization
  try
  {
    int i, paramBytes = ReadInt(InStream);  // read number of bytes of parameter data

    // Read the parameters, in the order they were written
    if (paramBytes<sizeof(int)) goto PARAMS_READ;                // trainingMethod
    i = ReadInt(InStream);
    if ((i<0)||(i>NNetLastTrainingMethod)) throw StreamErr();
    trainingMethod = (TrainingMethodType) i;
    paramBytes -= sizeof(int);
    if (paramBytes<1) goto PARAMS_READ;                 // UnityOutputLayerDerivatives
    UnityOutputLayerDerivatives = ReadBool(InStream);
    paramBytes--;
    if (paramBytes<1) goto PARAMS_READ;                 // BiasDecay
    BiasDecay = ReadBool(InStream);
    paramBytes--;
     if (paramBytes<sizeof(float)) goto PARAMS_READ;    // OutputOffValue
    OutputOffValue = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // OutputOnValue
    OutputOnValue = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // LearningRate
    LearningRate = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // MomentumFactor
    MomentumFactor = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // delta0
    delta0 = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // deltaMin
    deltaMin = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // deltaMax
    deltaMax = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // mu
    mu = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // mu_min
    mu_min = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // mu_inc
    mu_inc = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // mu_dec
    mu_dec = ReadFloat(InStream);
    paramBytes -= sizeof(float);
    if (paramBytes<sizeof(float)) goto PARAMS_READ;     // WeightDecayFactor
    WeightDecayFactor = ReadFloat(InStream);
    paramBytes -= sizeof(float);

    SkipBytes(InStream, paramBytes);  // skip any remaining bytes of paramter info.  This would happen if a stream created by a later version of NNet was read by an earlier version
    paramBytes = 0;

PARAMS_READ:
    if (paramBytes!=0) throw StreamErr();  // means there was an odd number of bytes left over, where a whole variable was expected

    int finalNumLayers = ReadInt(InStream);
    if (finalNumLayers<2) throw StreamErr();
    layerSize = new int[finalNumLayers];
    Node = new NNetNode* [finalNumLayers-1];
    int layerSz = ReadInt(InStream);
    if (layerSz<=0) throw StreamErr();
    layerSize[0] = layerSz;
    Input = new float[layerSz];
    numLayers = 1;
    for (int l=1; l<finalNumLayers; l++)        // Read each layer in turn (excluding input layer)
    {
      layerSz = ReadInt(InStream);
      if (layerSz<=0) throw StreamErr();
      layerSize[l] = layerSz;
      NNetNode* Nd = (Node[l-1] = new NNetNode[layerSz]);
      numLayers++;
      numNodes += layerSz;
      for (int n = layerSz-1; n>=0; n--)          // Read each node in turn
      {
        int i = ReadInt(InStream);
        if ((i<0)||(i>NNetLastTransferFn)) throw StreamErr();
        Nd[n].TransferFn = (NNetNode::TransferFnType) i;
        Nd[n].bias = ReadFloat(InStream);
        i = ReadInt(InStream);    // number of links
        if (i<0) throw StreamErr();
        NNetLink* Link = (Nd[n].Link = new NNetLink[i]);
        numLinks += i--;
        Nd[n].lastLink = i;
        for (; i>=0; i--)                           // Read each link in turn
        {
          Link[i].weight = ReadFloat(InStream);
          int layer = ReadInt(InStream);
          int nodeNum = ReadInt(InStream);
          if ((layer<0)||(layer>=l)||(nodeNum<0)||(nodeNum>=layerSize[layer])) throw StreamErr();
          if (layer==0)
          {
            Link[i].fromInput = nodeNum;
            Link[i].srcNode = (NNetNode*) (((char*)(Input+nodeNum)) - ((char*) &(((NNetNode*) 0)->output)));
          }
          else
          {
            Link[i].fromInput = -1;
            Link[i].srcNode = Node[layer-1]+nodeNum;
          }
        }
      }
    }
  }
  catch(const bad_alloc&)  // after a bad_alloc, NNet object is cleaned up and then exception is re-thrown
  {
    for(numLayers -= 2; numLayers >= 0; numLayers--) delete[] Node[numLayers];
    delete[] Node;
    Node = NULL;
    delete[] Input;
    Input = NULL;
    delete[] layerSize;
    layerSize = NULL;
    numLayers = 0;
    numNodes = 0;
    numLinks = 0;
    throw;
  }
  catch(...)  // anything else is considered an IO error, and false is returned after some cleanup
  {
    for(numLayers -= 2; numLayers >= 0; numLayers--) delete[] Node[numLayers];
    delete[] Node;
    Node = NULL;
    delete[] Input;
    Input = NULL;
    delete[] layerSize;
    layerSize = NULL;
    numLayers = 0;
    numNodes = 0;
    numLinks = 0;
    return false;
  }
  return true;
}



// save network to stream (returns true on success)
bool NNet::SaveToStream(void* OutStream)
{
  int paramBytes =        // Number of bytes written to stream for the parameters under the heading "training constants and options". THIS MAY CHANGE IF THE NNET CLASS IS EXTENDED
    sizeof(int)+                  // This is for the "trainingMethod" enum variable
    2+                            // UnityOutputLayerDerivatives+BiasDecay.   NB: **** Bool's are stored to stream as single bytes, REGARDLESS OF sizeof(bool)
    11*sizeof(float);             // OutputOffValue+OutputOnValue+LearningRate+MomentumFactor+delta0/Min/Max+mu/_inc/_dec+WeightDecayFactor;
    // <-- ADD SIZES OF ADDITIONAL PARAMETERS HERE
  try
  {
    WriteInt(OutStream, paramBytes);                           // Write the number of bytes used for parameters...
    WriteInt(OutStream, trainingMethod);                       // Write the parameters.  NOTE: NEW PARAMETERS MUST BE ADDED TO THE *END* OF THIS SEQUENCE
    WriteBool(OutStream, UnityOutputLayerDerivatives);
    WriteBool(OutStream, BiasDecay);
    WriteFloat(OutStream, OutputOffValue);
    WriteFloat(OutStream, OutputOnValue);
    WriteFloat(OutStream, LearningRate);
    WriteFloat(OutStream, MomentumFactor);
    WriteFloat(OutStream, delta0);
    WriteFloat(OutStream, deltaMin);
    WriteFloat(OutStream, deltaMax);
    WriteFloat(OutStream, mu);
    WriteFloat(OutStream, mu_min);
    WriteFloat(OutStream, mu_inc);
    WriteFloat(OutStream, mu_dec);
    WriteFloat(OutStream, WeightDecayFactor);
    // <-- WRITE ADDITIONAL PARAMETERS HERE

    WriteInt(OutStream, numLayers);
    WriteInt(OutStream, NumInputs());      // Input Layer
    for (int l=1; l<numLayers; l++)        // Write each layer in turn (excluding input layer)
    {
      int layerSz = layerSize[l];
      WriteInt(OutStream, layerSz);
      NNetNode* Nd = Node[l];
      for (int n = layerSz-1; n>=0; n--)        // Write each node in turn
      {
        WriteInt(OutStream, Nd[n].TransferFn);
        WriteFloat(OutStream, Nd[n].bias);
        WriteInt(OutStream, Nd[n].NumLinks());
        NNetLink* Link = Nd[n].Link;
        for (int i = Nd[n].lastLink; i>=0; i--)    // Write each link in turn
        {
          WriteFloat(OutStream, Link[i].weight);
          if (Link[i].fromInput>=0)
          {
            WriteInt(OutStream, 0);     // layer 0 (input layer)
            WriteInt(OutStream, Link[i].fromInput);     // Node index (actually input index in this case)
          }
          else
          {
            WriteInt(OutStream, Link[i].srcNode->layer);
            WriteInt(OutStream, Link[i].srcNode->nodeNum);
          }
        }
      }
    }
  }
  catch(...) { return false; }
  return true;
}



// delete the linkNum'th link from node destNode
bool NNet::DeleteLink(NNetNode* destNode, int linkNum)
{
  bool result = destNode->DeleteLink(linkNum);
  if (result) numLinks--;
  return result;
}



// delete the link from (layer, nodeNum) leading into node destNode
bool NNet::DeleteLink(NNetNode* destNode, int layer, int nodeNum)
{
  bool result = destNode->DeleteLink(layer, nodeNum);
  if (result) numLinks--;
  return result;
}



// Add a single link to a node, or change the weight of an existing link
// NB new links are added to the END of the link array
bool NNet::AddOrSetLink(NNetNode* destNode, int layer, int nodeNum, float weight)
{
  if ((layer<0)||(nodeNum<0)||(layer>=destNode->layer)||(nodeNum>=layerSize[layer])) return false;
  if (layer==0)
    for (int i = destNode->lastLink; i>=0; i--)
    {
      NNetLink* Link = (destNode->Link)+i;
      if (Link->fromInput==nodeNum) { Link->weight = weight; return true; }
    }
  else
    for (int i = destNode->lastLink; i>=0; i--)
    {
      NNetLink* Link = (destNode->Link)+i;
      if ((Link->fromInput<0)&&(Link->srcNode->nodeNum==nodeNum)&&(Link->srcNode->layer==layer)) { Link->weight = weight; return true; }
    }
  int nLinks = destNode->NumLinks();
  NNetLink* NewLinks = new NNetLink[nLinks+1];
  if (nLinks>0) memcpy(NewLinks, destNode->Link, nLinks*sizeof(NNetLink));
  destNode->lastLink++;
  NewLinks[nLinks].weight = weight;
  if (layer==0)
  {
    NewLinks[nLinks].fromInput = nodeNum;
    NewLinks[nLinks].srcNode =  (NNetNode*) (((char*) (Input+nodeNum)) - ((char*) &(((NNetNode*) 0)->output)));
  }
  else
  {
    NewLinks[nLinks].fromInput = -1;
    NewLinks[nLinks].srcNode =  Node[layer-1]+nodeNum;
  }
  delete[] destNode->Link;
  destNode->Link = NewLinks;
  numLinks++;
  return true;
}



// specify part of the network link graph
bool NNet::SetConnectivity(int srcRangeEntries, const NNetNodeRange* srcRanges, const NNetNodeRange& setRange)
{
  int l = setRange.layer-1;
  int n = setRange.firstNode;
  if (!RangeOK(setRange)) return false;
  for (int i = srcRangeEntries-1; i>=0; i--) if (!srcRangeOK(srcRanges[i])||(srcRanges[i].layer>=setRange.layer)) return false;
  NNetNode* setNd = Node[l]+n;
  NNetNode* Nd = Node[l];
  numLinks += setNd->SetLinks(Input, Node, srcRanges, srcRangeEntries);
  for (n++; n <= setRange.lastNode; n++) numLinks += Nd[n].CopyLinks(*setNd);
  return true;
}



// set the transfer fn. for a range of nodes
bool NNet::SetTransferFn(const NNetNodeRange& setRange, NNetNode::TransferFnType fn)
{
  if (!RangeOK(setRange)) return false;
  int l = setRange.layer-1;
  NNetNode* Nd = Node[l];
  for (int n = setRange.firstNode; n <= setRange.lastNode; n++) Nd[n].TransferFn = fn;
  return true;
}



// set the transfer fn. for nodes in specified layers
bool NNet::SetTransferFn(int firstLayer, int lastLayer, NNetNode::TransferFnType fn)
{
  if ((firstLayer<=0)||(firstLayer>lastLayer)||(lastLayer>=numLayers)) return false;
  for (int l = firstLayer; l <= lastLayer; l++)
    for (int n = layerSize[l+1]-1; n>=0; n--) Node[l][n].TransferFn = fn;
  return true;
}



// init weights & biases using Nguyen-Widrow algorithm (specified nodes)
bool NNet::NguyenWidrowInitWeights(const NNetNodeRange& setRange, bool checkRange)
{
  if (checkRange&&(!RangeOK(setRange))) return false;
  for (int n = setRange.firstNode; n <= setRange.lastNode; n++) Node[setRange.layer-1][n].NguyenWidrowInitWeights(*this);
  return true;
}



// init weights & biases using Nguyen-Widrow algorithm (specified layers)
bool NNet::NguyenWidrowInitWeights(int firstLayer, int lastLayer)
{
  if ((firstLayer<=0)||(firstLayer>lastLayer)||(lastLayer>=numLayers)) return false;
  for (int l = firstLayer; l <= lastLayer; l++)
  {
    NNetNodeRange range = { l, 0, (layerSize[l]-1) };
    NguyenWidrowInitWeights(range, false);
  }
  return true;
}



// init weights & biases randomly; same as NW with 1 node per layer (specified nodes)
bool NNet::RandomInitWeights(const NNetNodeRange& setRange, bool checkRange)
{
  if (checkRange&&(!RangeOK(setRange))) return false;
  for (int n = setRange.firstNode; n <= setRange.lastNode; n++) Node[setRange.layer-1][n].NguyenWidrowInitWeights(*this, true);
  return true;
}



// init weights & biases randomly (specified layers)
bool NNet::RandomInitWeights(int firstLayer, int lastLayer)
{
  if ((firstLayer<=0)||(firstLayer>lastLayer)||(lastLayer>=numLayers)) return false;
  for (int l = firstLayer; l <= lastLayer; l++)
  {
    NNetNodeRange range = { l, 0, (layerSize[l]-1) };
    RandomInitWeights(range, false);
  }
  return true;
}



// Train the network
void NNet::Train(int NumPatterns, const void* TrainingData)
{
  epoch = 0;
  abortTraining = false;
  switch (trainingMethod)
  {
    case BACKPROP:
      TrainBackProp(NumPatterns, TrainingData);
      break;
    case BATCH_BACKPROP:
      TrainBatchBackProp(NumPatterns, TrainingData);
      break;
    case RPROP:
      TrainRProp(NumPatterns, TrainingData);
      break;
    case LEVENBERG_MARQUARDT:
      TrainLevenbergMarquardt(NumPatterns, TrainingData);
      break;
  }
}



// Return normalized error measure of network performance on the training data (doesn't affect network)
// NOTE: This function may be called from within OnEpochComplete() .
double NNet::MeasureError(int NumPatterns, const void* TrainingData)
{
  bool* DesiredOutputs = new bool[NumOutputs()];    // make a place to store desired outputs
  double temp, totalSquaredError = 0;
  NNetNode* Nd;
  int patn, lastOutput = NumOutputs()-1, lastNodeLayer = numLayers-2;
  for (patn = 0; (patn<NumPatterns)&&(!abortTraining); patn++)
  {
    GetTrainingPattern(patn, DesiredOutputs, TrainingData);                     // Get input and desired output
    FeedForward();
    Nd = Node[lastNodeLayer];
    for (int n = lastOutput; n>=0 ; Nd++, n--)
    {
      temp = DesiredOutputs[n] ? (Nd->output>=OutputOnValue ? 0.0 : (Nd->output-OutputOnValue)) :           // compute error based on desired output
                                 (Nd->output<=OutputOffValue ? 0.0 : (Nd->output-OutputOffValue));
      totalSquaredError += temp*temp;
    }
  }
  return (patn>0) ? totalSquaredError/(NumOutputs()*patn) : 1.0;
}



// Use the NNet to classify the input data pattern.
// Output nodes as grouped into contiguous sets each of which classifies a separate property (variable).
// Return value is false if the number of output groups and their sizes don't match the number of outputs, true otherwise.
// NB if the total sizes of the ouput sets specified is < number of output nodes, all classifications will be made despite the false return value.
bool NNet::Classify(const void* InputPattern, int numOutputSets, const int* sizes, int* outputClassifications)
{
  SetInputs(InputPattern);
  FeedForward();
  NNetNode* outputNode = Node[numLayers-2];
  int classification, i, setSize, n, count = NumOutputs();
  float maxVal;
  for (n = 0; n<numOutputSets; n++)
  {
    setSize = sizes[n];
    if (count<setSize) return false; // oops... more output nodes were specified than actually exist - so stop before we try to access nonexistent ones!
    classification = -1;  // returns -1 for sets with size 0.
    maxVal = -FLT_MAX;   // just anything guaranteed to be smaller than any node output
    for (i = setSize-1; i>=0; i--) if ((outputNode[i].output)>=maxVal) { maxVal = outputNode[i].output; classification = i; }
    outputClassifications[n] = classification;
    outputNode += setSize;
    count-= setSize;
  }
  if (count!=0) return false;  // not all output nodes were used - probably indicates bad parameter values were passed
  return true;
}



// Use the NNet to find the probability distribution over classification properties (variables) for the input data pattern.
// ...similar to Classify() above...
bool NNet::ProbabilisticClassify(const void* InputPattern, int numOutputSets, const int* sizes, double* const * probabilities)
{
  SetInputs(InputPattern);
  FeedForward();
  NNetNode* outputNode = Node[numLayers-2];
  int i, setSize, count = NumOutputs();
  double temp, total, addVal, outputRange = (OutputOnValue-OutputOffValue);
  if (outputRange<=0) outputRange = 1.0;  // just in case of stupid Output On/Off Values, avoid division by zero and negative probabilities (results will still be incorrect)
  for (int n = 0; n<numOutputSets; n++)
  {
    setSize = sizes[n];
    if (setSize==1) { probabilities[n][0] = 1.0; outputNode++; continue; }  // deal with trivial case
    if (setSize<=0) { continue; }                                           // deal with trivial case
    if (count<setSize) return false; // oops... more output nodes were specified than actually exist - so stop before we try to access nonexistent ones!
    total = 0.0;
    for (i = setSize-1; i>=0; i--)
    {
      temp = outputNode[i].output-OutputOffValue;
      temp = (temp<0) ? 0 : (temp>outputRange) ? outputRange : temp;
      total += temp;
    }
    if (total>=outputRange) addVal = 0;                                    // if probabilities sum to >=1 then normalize...
    else { addVal = (outputRange-total)/setSize; total = outputRange; }    // but if they sum to <1, ADD an equal value to each output to make up the difference
    for (i = setSize-1; i>=0; i--)
    {
      temp = outputNode[i].output-OutputOffValue;
      temp = (temp<0) ? 0 : (temp>outputRange) ? outputRange : temp;
      probabilities[n][i] = (temp+addVal)/total;
    }
    outputNode += setSize;
    count-= setSize;
  }
  if (count!=0) return false;  // not all output nodes were used - probably indicates bad parameter values were passed
  return true;
}
