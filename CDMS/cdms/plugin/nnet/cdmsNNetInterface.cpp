#include "JavaNNet.h"
#include "cdmsNNetInterface.h"
#include <stddef.h>                  // definition of NULL
#include <new.h>
#include <signal.h>


using namespace std;  // make sure that operator new() and operator new[] throw bad_alloc on failure


const jlong jlongNULL = (jlong)(void*) NULL;  // define the jlong equivalent of the NULL value, just in case NULL is non-zero

static jclass jdoubleArrayClass;  // initialized by the function Java_cdms_plugin_nnet_NNet_nativeInitNNetLibrary(), used by ..._probabilisticClassify()



class FPException {};      // empty class, just used as an exception type for flagging Floating-Point exceptions



void FloatErrHandler(int)
{
  throw FPException();
}



void ThrowJavaException(JNIEnv* JavaEnv, const char* message)
{
  if (JavaEnv->ExceptionOccurred()==NULL)   // otherwise just throw the existing Java exception
  {
    jclass runException = JavaEnv->FindClass("java/lang/RuntimeException");
    if (runException==NULL) return;   // throws a NoClassDefFoundError
    JavaEnv->ThrowNew(runException, message);
  }
}



void ThrowJavaOutOfMemoryError(JNIEnv* JavaEnv, const char* message)
{
  if (JavaEnv->ExceptionOccurred()==NULL)   // otherwise just throw the existing Java exception
  {
    jclass outOfMemErr = JavaEnv->FindClass("java/lang/OutOfMemoryError");
    if (outOfMemErr==NULL) return;   // throws a NoClassDefFoundError
    JavaEnv->ThrowNew(outOfMemErr, message);
  }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_getNodeTransferFn(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  try { return ((NNetNode*) NodePtr)->TransferFn; }    // NB this is possible because the transfer function constant definitions match the C++ enum constants
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeTransferFn():  Bad node handle supplied"); return 0; }
}



void JNICALL Java_cdms_plugin_nnet_NNet_setNodeTransferFn(JNIEnv* JavaEnv, jobject, jlong NodePtr, jint tf)
{
  try
  {
    if ((tf<0)||(tf>NNetLastTransferFn)) ThrowJavaException(JavaEnv, "Neural Net Error - setNodeTransferFn():  Invalid transfer fn. specified");
    else (((NNetNode*) NodePtr)->TransferFn) = ((NNetNode::TransferFnType) tf);    // NB this is possible because the transfer function constant definitions match the C++ enum constants
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setNodeTransferFn():  Bad node handle supplied"); }
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_getNodeBias(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((NNetNode*) NodePtr)->bias; }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeBias():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeBias():  Bad node handle supplied"); }
  return 0.0f;
}



void JNICALL Java_cdms_plugin_nnet_NNet_setNodeBias(JNIEnv* JavaEnv, jobject, jlong NodePtr, jfloat bias)
{
  signal(SIGFPE, FloatErrHandler);
  try { ((NNetNode*) NodePtr)->bias = bias; }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - setNodeBias():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setNodeBias():  Bad node handle supplied"); }
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_getNodeOutput(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((NNetNode*) NodePtr)->Output(); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeOutput():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeOutput():  Bad node handle supplied"); }
  return 0.0f;
}



jint JNICALL Java_cdms_plugin_nnet_NNet_getNodeLayer(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  try { return ((NNetNode*) NodePtr)->Layer(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeLayer():  Bad node handle supplied"); return 0; }

}



jint JNICALL Java_cdms_plugin_nnet_NNet_getNodeNum(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  try { return ((NNetNode*) NodePtr)->NodeNum(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeNum():  Bad node handle supplied"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_getNodeNumLinks(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  try { return ((NNetNode*) NodePtr)->NumLinks(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeNumLinks():  Bad node handle supplied"); return 0; }
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_getNodeActiveSigmaMin(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    float min, max;
    ((NNetNode*) NodePtr)->GetActiveSigmaRange(min, max);
    return min;
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeActiveSigmaMin():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeActiveSigmaMin():  Bad node handle supplied"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_getNodeActiveSigmaMax(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    float min, max;
    ((NNetNode*) NodePtr)->GetActiveSigmaRange(min, max);
    return max;
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeActiveSigmaMax():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeActiveSigmaMax():  Bad node handle supplied"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_getNodeOutputMin(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    float min, max;
    ((NNetNode*) NodePtr)->GetOutputRange(min, max);
    return min;
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeOutputMin():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeOutputMin():  Bad node handle supplied"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_getNodeOutputMax(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    float min, max;
    ((NNetNode*) NodePtr)->GetOutputRange(min, max);
    return max;
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeOutputMax():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeOutputMax():  Bad node handle supplied"); }
  return 0.0f;
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeInitNNetLibrary(JNIEnv* JavaEnv, jclass)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    InitNNetClassMethodIDs(JavaEnv);
    jdoubleArrayClass = JavaEnv->FindClass("[D");
  }
  catch(const JavaException&) {}
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Internal Error:  Error initializing Java/C++ interface"); }
}



jlong JNICALL Java_cdms_plugin_nnet_NNet_nativeCreateNNet__I_3IZ(JNIEnv* JavaEnv, jobject JavaObj, jint numHiddenLayers, jintArray JavaLayerSizes, jboolean createLinks)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    int numInputs, numOutputs, numInputComponents, numOutputComponents, *outSizes;
    jint* jLayerSizes;
    JavaNNet::InputComponentType* inComponents;
    jlong result = jlongNULL;
    JavaNNet::GetInterfaceInfo(JavaEnv, JavaObj, numInputComponents, numOutputComponents, inComponents, outSizes, numInputs, numOutputs);
    if (numHiddenLayers==0)
    {
      try { result = (jlong) new JavaNNet(JavaEnv, JavaObj, numInputComponents, numOutputComponents, inComponents, outSizes, numInputs, numOutputs, numHiddenLayers, NULL, createLinks!=0); }
      catch(...)
      {
        delete[] inComponents;
        delete[] outSizes;
        throw;
      }
    }
    else if (sizeof(int)==sizeof(jint))
    {
      jLayerSizes = JavaEnv->GetIntArrayElements(JavaLayerSizes, NULL);
      if (jLayerSizes!=NULL)
      {
        try { result = (jlong) new JavaNNet(JavaEnv, JavaObj, numInputComponents, numOutputComponents, inComponents, outSizes, numInputs, numOutputs, numHiddenLayers, (int*) jLayerSizes, createLinks!=0); }
        catch(...)
        {
          delete[] inComponents;
          delete[] outSizes;
          jthrowable javaException = JavaEnv->ExceptionOccurred();
          if (javaException==NULL) JavaEnv->ReleaseIntArrayElements(JavaLayerSizes, jLayerSizes, JNI_ABORT);
          else
          {
            JavaEnv->ExceptionClear();                                                       // must clear pending Java exception before releasing array elements
            JavaEnv->ReleaseIntArrayElements(JavaLayerSizes, jLayerSizes, JNI_ABORT);        // ... then we can re throw it (if one was thrown)
            if (JavaEnv->Throw(javaException)!=0) ThrowJavaException(JavaEnv, "Neural Net Error (net constructor):  Unable to re-throw temporarily cleared Java exception");
          }
          throw;
        }
        JavaEnv->ReleaseIntArrayElements(JavaLayerSizes, jLayerSizes, JNI_ABORT);
      }
    }
    else
    {
      int* layerSizes = NULL;
      try
      {
        layerSizes = new int[numHiddenLayers];
        jLayerSizes = JavaEnv->GetIntArrayElements(JavaLayerSizes, NULL);
        if (jLayerSizes!=NULL)
        {
          try { for (int i = numHiddenLayers-1; i>=0; i--) layerSizes[i] = jLayerSizes[i]; }
          catch(...)
          {
            JavaEnv->ReleaseIntArrayElements(JavaLayerSizes, jLayerSizes, JNI_ABORT);
            throw;
          }
          JavaEnv->ReleaseIntArrayElements(JavaLayerSizes, jLayerSizes, JNI_ABORT);
          result = (jlong) new JavaNNet(JavaEnv, JavaObj, numInputComponents, numOutputComponents, inComponents, outSizes, numInputs, numOutputs, numHiddenLayers, layerSizes, createLinks!=0);
        }
      }
      catch(...)
      {
        delete[] inComponents;
        delete[] outSizes;
        delete[] layerSizes;
        throw;
      }
      delete[] layerSizes;
    }
    return result;
  }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaOutOfMemoryError(JavaEnv, "Neural Net Error:  Insufficient memory to create neural net"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error:  Internal error creating neural net"); }
  return jlongNULL;
}



jlong JNICALL Java_cdms_plugin_nnet_NNet_nativeCreateNNet__J(JNIEnv* JavaEnv, jobject JavaObj, jlong JavaNNetPtr)
{
  signal(SIGFPE, FloatErrHandler);
  try { return (jlong) new JavaNNet(JavaEnv, JavaObj, *(JavaNNet*) JavaNNetPtr); }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaOutOfMemoryError(JavaEnv, "Neural Net Error:  Insufficient memory to create neural net"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error:  Internal error creating copy of neural net"); }
  return jlongNULL;
}



jlong JNICALL Java_cdms_plugin_nnet_NNet_nativeCreateNNet__Ljava_io_ObjectInputStream_2(JNIEnv* JavaEnv, jobject JavaObj, jobject InStream)
{
  signal(SIGFPE, FloatErrHandler);
  try { return (jlong) new JavaNNet(JavaEnv, JavaObj, InStream); }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaOutOfMemoryError(JavaEnv, "Neural Net Error:  Insufficient memory to create neural net"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error:  Internal error deserializing neural net from stream"); }
  return jlongNULL;
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeSaveNNetToStream(JNIEnv* JavaEnv, jobject JavaObj, jlong JavaNNetPtr, jobject OutStream)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    ((JavaNNet*) JavaNNetPtr)->SetJavaObject(JavaEnv, JavaObj);
    ((JavaNNet*) JavaNNetPtr)->SaveToStream(OutStream);
  }
  catch(const JavaException&) {}
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error:  Internal error serializing neural net to stream"); }
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeDeleteNNet(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { delete ((JavaNNet*) JavaNNetPtr); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error:  Internal error during neural net finalization"); }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeGetIntParam(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint param)
{
  try
  {
    switch (param)
    {
      case cdms_plugin_nnet_NNet_iTrainingMethod:  return (jint) ((JavaNNet*) JavaNNetPtr)->trainingMethod;   // NB this is possible because the training method constant definitions match the C++ enum constants
      default:  ThrowJavaException(JavaEnv, "Neural Net Error - getIntParam():  Unknown parameter");
    }
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getIntParam():  Bad net pointer"); }
  return 0;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeGetBooleanParam(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint param)
{
  try
  {
    switch (param)
    {
      case cdms_plugin_nnet_NNet_bUnityOutputLayerDerivatives:  return ((JavaNNet*) JavaNNetPtr)->UnityOutputLayerDerivatives;
      case cdms_plugin_nnet_NNet_bBiasDecay:                    return ((JavaNNet*) JavaNNetPtr)->BiasDecay;
      default:  ThrowJavaException(JavaEnv, "Neural Net Error - getBooleanParam():  Unknown parameter");
    }
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getBooleanParam():  Bad net pointer"); }
  return false;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_nativeGetFloatParam(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint param)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    switch (param)
    {
      case cdms_plugin_nnet_NNet_fOutputOffValue:     return ((JavaNNet*) JavaNNetPtr)->OutputOffValue;
      case cdms_plugin_nnet_NNet_fOutputOnValue:      return ((JavaNNet*) JavaNNetPtr)->OutputOnValue;
      case cdms_plugin_nnet_NNet_fLearningRate:       return ((JavaNNet*) JavaNNetPtr)->LearningRate;
      case cdms_plugin_nnet_NNet_fMomentumFactor:     return ((JavaNNet*) JavaNNetPtr)->MomentumFactor;
      case cdms_plugin_nnet_NNet_fDelta0:             return ((JavaNNet*) JavaNNetPtr)->delta0;
      case cdms_plugin_nnet_NNet_fDeltaMin:           return ((JavaNNet*) JavaNNetPtr)->deltaMin;
      case cdms_plugin_nnet_NNet_fDeltaMax:           return ((JavaNNet*) JavaNNetPtr)->deltaMax;
      case cdms_plugin_nnet_NNet_fMu:                 return ((JavaNNet*) JavaNNetPtr)->mu;
      case cdms_plugin_nnet_NNet_fMuMin:              return ((JavaNNet*) JavaNNetPtr)->mu_min;
      case cdms_plugin_nnet_NNet_fMuInc:              return ((JavaNNet*) JavaNNetPtr)->mu_inc;
      case cdms_plugin_nnet_NNet_fMuDec:              return ((JavaNNet*) JavaNNetPtr)->mu_dec;
      case cdms_plugin_nnet_NNet_fWeightDecayFactor:  return ((JavaNNet*) JavaNNetPtr)->WeightDecayFactor;
      default:  ThrowJavaException(JavaEnv, "Neural Net Error - getFloatParam():  Unknown parameter");
    }
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getFloatParam():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getFloatParam():  Bad net pointer"); }
  return 0.0f;
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeSetIntParam(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint param, jint value)
{
  try
  {
    switch (param)
    {
      case cdms_plugin_nnet_NNet_iTrainingMethod:
        if ((value<0)||(value>NNetLastTrainingMethod)) ThrowJavaException(JavaEnv, "Neural Net Error - setIntParam():  Invalid training method constant");
        else ((JavaNNet*) JavaNNetPtr)->trainingMethod = (JavaNNet::TrainingMethodType) value;   // NB this is possible because the training method constant definitions match the C++ enum constants
        break;
      default:  ThrowJavaException(JavaEnv, "Neural Net Error - setIntParam():  Unknown parameter");
    }
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setIntParam():  Bad net pointer"); }
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeSetBooleanParam(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint param, jboolean value)
{
  try
  {
    switch (param)
    {
      case cdms_plugin_nnet_NNet_bUnityOutputLayerDerivatives:  ((JavaNNet*) JavaNNetPtr)->UnityOutputLayerDerivatives = (value!=0);  break;
      case cdms_plugin_nnet_NNet_bBiasDecay:                    ((JavaNNet*) JavaNNetPtr)->BiasDecay = (value!=0);                    break;
      default:  ThrowJavaException(JavaEnv, "Neural Net Error - setBooleanParam():  Unknown parameter");
    }
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setBooleanParam():  Bad net pointer"); }
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeSetFloatParam(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint param, jfloat value)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    switch (param)
    {
      case cdms_plugin_nnet_NNet_fOutputOffValue:     ((JavaNNet*) JavaNNetPtr)->OutputOffValue = value;      break;
      case cdms_plugin_nnet_NNet_fOutputOnValue:      ((JavaNNet*) JavaNNetPtr)->OutputOnValue = value;       break;
      case cdms_plugin_nnet_NNet_fLearningRate:       ((JavaNNet*) JavaNNetPtr)->LearningRate = value;        break;
      case cdms_plugin_nnet_NNet_fMomentumFactor:     ((JavaNNet*) JavaNNetPtr)->MomentumFactor = value;      break;
      case cdms_plugin_nnet_NNet_fDelta0:             ((JavaNNet*) JavaNNetPtr)->delta0 = value;              break;
      case cdms_plugin_nnet_NNet_fDeltaMin:           ((JavaNNet*) JavaNNetPtr)->deltaMin = value;            break;
      case cdms_plugin_nnet_NNet_fDeltaMax:           ((JavaNNet*) JavaNNetPtr)->deltaMax = value;            break;
      case cdms_plugin_nnet_NNet_fMu:                 ((JavaNNet*) JavaNNetPtr)->mu = value;                  break;
      case cdms_plugin_nnet_NNet_fMuMin:              ((JavaNNet*) JavaNNetPtr)->mu_min = value;              break;
      case cdms_plugin_nnet_NNet_fMuInc:              ((JavaNNet*) JavaNNetPtr)->mu_inc = value;              break;
      case cdms_plugin_nnet_NNet_fMuDec:              ((JavaNNet*) JavaNNetPtr)->mu_dec = value;              break;
      case cdms_plugin_nnet_NNet_fWeightDecayFactor:  ((JavaNNet*) JavaNNetPtr)->WeightDecayFactor = value;   break;
      default:  ThrowJavaException(JavaEnv, "Neural Net Error - setFloatParam():  Unknown parameter");
    }
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - setFloatParam():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setFloatParam():  Bad net pointer"); }
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeAbortTraining(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { ((JavaNNet*) JavaNNetPtr)->AbortTraining(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - abortTraining():  Bad net pointer"); }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeCurrentEpoch(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->CurrentEpoch(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - currentEpoch():  Bad net pointer"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativePatternsRemaining(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->PatternsRemaining(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - patternsRemaining():  Bad net pointer"); return 0; }
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeDeleteLink__JJI(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jlong destNodeHandle, jint linkNum)
{
  try { return ((JavaNNet*) JavaNNetPtr)->DeleteLink((NNetNode*) destNodeHandle, linkNum); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeDeleteLink__JJII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jlong destNodeHandle, jint layer, jint nodeNum)
{
  try { return ((JavaNNet*) JavaNNetPtr)->DeleteLink((NNetNode*) destNodeHandle, layer, nodeNum); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeDeleteLink__JIII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint destLayer, jint destNodeNum, jint linkNum)
{
  try { return ((JavaNNet*) JavaNNetPtr)->DeleteLink(destLayer, destNodeNum, linkNum); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeDeleteLink__JIIII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint destLayer, jint destNodeNum, jint layer, jint nodeNum)
{
  try { return ((JavaNNet*) JavaNNetPtr)->DeleteLink(destLayer, destNodeNum, layer, nodeNum); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - deleteLink():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeAddOrSetLink__JJIIF(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jlong destNodeHandle, jint layer, jint nodeNum, jfloat weight)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->AddOrSetLink((NNetNode*) destNodeHandle, layer, nodeNum, weight); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - addOrSetLink():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - addOrSetLink():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - addOrSetLink():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeAddOrSetLink__JIIIIF(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint destLayer, jint destNodeNum, jint layer, jint nodeNum, jfloat weight)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->AddOrSetLink(destLayer, destNodeNum, layer, nodeNum, weight); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - addOrSetLink():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - addOrSetLink():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - addOrSetLink():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeSetConnectivity(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint numSrcRanges, jintArray JavaSrcRanges, jint layer, jint firstNodeNum, jint lastNodeNum)
{
  try
  {
    jboolean result;
    jint* jSrcRanges;
    NNetNodeRange setRange = { layer, firstNodeNum, lastNodeNum };
    jSrcRanges = JavaEnv->GetIntArrayElements(JavaSrcRanges, NULL);
    if (jSrcRanges==NULL) return false;
    try
    {
      if (sizeof(int)==sizeof(jint)) result = ((JavaNNet*) JavaNNetPtr)->SetConnectivity(numSrcRanges, (NNetNodeRange*) jSrcRanges, setRange);
      else
      {
        int i, j;
        NNetNodeRange* srcRanges = new NNetNodeRange[numSrcRanges];
        try
        {
          for (i = numSrcRanges-1, j = i*3; i>=0; i--)
          {
            srcRanges[i].layer = jSrcRanges[j];
            srcRanges[i].firstNode = jSrcRanges[j+1];
            srcRanges[i].lastNode = jSrcRanges[j+2];
            j -= 3;
          }
          result = ((JavaNNet*) JavaNNetPtr)->SetConnectivity(numSrcRanges, srcRanges, setRange);
        }
        catch(...)
        {
          delete[] srcRanges;
          throw;
        }
        delete[] srcRanges;
      }
    }
    catch(...)
    {
      JavaEnv->ReleaseIntArrayElements(JavaSrcRanges, jSrcRanges, JNI_ABORT);
      throw;
    }
    JavaEnv->ReleaseIntArrayElements(JavaSrcRanges, jSrcRanges, JNI_ABORT);
    return result;
  }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - setConnectivity():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setConnectivity():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeSetTransferFn__JIIII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint layer, jint firstNodeNum, jint lastNodeNum, jint fn)
{
  try
  {
    NNetNodeRange setRange = { layer, firstNodeNum, lastNodeNum };
    if ((fn<0)||(fn>NNetLastTransferFn)) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Invalid transfer fn. specified"); return false; }
    else return ((JavaNNet*) JavaNNetPtr)->SetTransferFn(setRange, (NNetNode::TransferFnType) fn);
  }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeSetTransferFn__JIII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint firstLayer, jint lastLayer, jint fn)
{
  try
  {
    if ((fn<0)||(fn>NNetLastTransferFn)) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Invalid transfer fn. specified"); return false; }
    else return ((JavaNNet*) JavaNNetPtr)->SetTransferFn(firstLayer, lastLayer, (NNetNode::TransferFnType) fn);
  }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Internal error"); }
  return false;
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeSetTransferFn__JI(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint fn)
{
  try
  {
    if ((fn<0)||(fn>NNetLastTransferFn)) ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Invalid transfer fn. specified");
    else ((JavaNNet*) JavaNNetPtr)->SetTransferFn((NNetNode::TransferFnType) fn);
  }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Insufficient memory to perform operation"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - setTransferFn():  Internal error"); }
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeNWInit__JIII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint layer, jint firstNodeNum, jint lastNodeNum)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    NNetNodeRange setRange = { layer, firstNodeNum, lastNodeNum };
    return ((JavaNNet*) JavaNNetPtr)->NguyenWidrowInitWeights(setRange);
  }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeNWInit__JII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint firstLayer, jint lastLayer)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->NguyenWidrowInitWeights(firstLayer, lastLayer); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Internal error"); }
  return false;
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeNWInit__J(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  signal(SIGFPE, FloatErrHandler);
  try { ((JavaNNet*) JavaNNetPtr)->NguyenWidrowInitWeights(); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - nguyenWidrowInitWeights():  Internal error"); }
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeRandomInit__JIII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint layer, jint firstNodeNum, jint lastNodeNum)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    NNetNodeRange setRange = { layer, firstNodeNum, lastNodeNum };
    return ((JavaNNet*) JavaNNetPtr)->RandomInitWeights(setRange);
  }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Internal error"); }
  return false;
}



jboolean JNICALL Java_cdms_plugin_nnet_NNet_nativeRandomInit__JII(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint firstLayer, jint lastLayer)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->RandomInitWeights(firstLayer, lastLayer); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Internal error"); }
  return false;
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeRandomInit__J(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  signal(SIGFPE, FloatErrHandler);
  try { ((JavaNNet*) JavaNNetPtr)->RandomInitWeights(); }
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - randomInitWeights():  Internal error"); }
}



void JNICALL Java_cdms_plugin_nnet_NNet_nativeTrain(JNIEnv* JavaEnv, jobject JavaObj, jlong JavaNNetPtr, jint numPatterns, jobject inputData, jobject outputData)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    ((JavaNNet*) JavaNNetPtr)->SetJavaObject(JavaEnv, JavaObj);
    JavaNNet::TrainingDataType TrainingData = { inputData, outputData };
    ((JavaNNet*) JavaNNetPtr)->Train(numPatterns, &TrainingData);
  }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - train():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - train():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - train():  Internal error"); }
}



jdouble JNICALL Java_cdms_plugin_nnet_NNet_nativeMeasureError(JNIEnv* JavaEnv, jobject JavaObj, jlong JavaNNetPtr, jint numPatterns, jobject inputData, jobject outputData)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    ((JavaNNet*) JavaNNetPtr)->SetJavaObject(JavaEnv, JavaObj);
    JavaNNet::TrainingDataType TrainingData = { inputData, outputData };
    return ((JavaNNet*) JavaNNetPtr)->MeasureError(numPatterns, &TrainingData);
  }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - measureError():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - measureError():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - measureError():  Internal error"); }
  return 0.0;
}



jintArray JNICALL Java_cdms_plugin_nnet_NNet_nativeClassify(JNIEnv* JavaEnv, jobject JavaObj, jlong JavaNNetPtr, jintArray inputPattern)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    ((JavaNNet*) JavaNNetPtr)->SetJavaObject(JavaEnv, JavaObj);
    jint* resultElements;
    int n = ((JavaNNet*) JavaNNetPtr)->GetNumOutputComponents();
    jintArray result = JavaEnv->NewIntArray(n);
    if (result==NULL) return NULL;
    if (sizeof(int)==sizeof(jint))
    {
      if ((resultElements = JavaEnv->GetIntArrayElements(result, NULL))!=NULL)
      {
        try { ((JavaNNet*) JavaNNetPtr)->Classify((void*) inputPattern, (int*) resultElements); }
        catch(...)
        {
          jthrowable javaException = JavaEnv->ExceptionOccurred();
          if (javaException==NULL) JavaEnv->ReleaseIntArrayElements(result, resultElements, JNI_ABORT);
          else
          {
            JavaEnv->ExceptionClear();                                               // must clear pending Java exception before releasing array elements
            JavaEnv->ReleaseIntArrayElements(result, resultElements, JNI_ABORT);     // ... then we can re throw it (if one was thrown)
            if (JavaEnv->Throw(javaException)!=0) ThrowJavaException(JavaEnv, "Neural Net Error - classify():  Unable to re-throw temporarily cleared Java exception");
          }
          throw;
        }
        JavaEnv->ReleaseIntArrayElements(result, resultElements, 0);
      }
    }
    else     // sizeof(int) != sizeof(jint)
    {
      int* classifications = new int[n];
      try
      {
        ((JavaNNet*) JavaNNetPtr)->Classify((void*) inputPattern, classifications);
        resultElements = JavaEnv->GetIntArrayElements(result, NULL);
        if (resultElements!=NULL)
        {
          for (n--; n>=0; n--) resultElements[n]=classifications[n];
          JavaEnv->ReleaseIntArrayElements(result, resultElements, 0);
        }
      }
      catch(...)
      {
        delete[] classifications;
        throw;
      }
      delete[] classifications;
    }
    return result;
  }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - classify():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - classify():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - classify():  Internal error"); }
  return NULL;
}



jobjectArray JNICALL Java_cdms_plugin_nnet_NNet_nativeProbabilisticClassify(JNIEnv* JavaEnv, jobject JavaObj, jlong JavaNNetPtr, jintArray inputPattern)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    ((JavaNNet*) JavaNNetPtr)->SetJavaObject(JavaEnv, JavaObj);
    int n = ((JavaNNet*) JavaNNetPtr)->GetNumOutputComponents();
    jobjectArray result = JavaEnv->NewObjectArray(n, jdoubleArrayClass, NULL);
    if (result==NULL) return NULL;
    double** prob = new double*[n];
    try
    {
      const int* GrpSizes = ((JavaNNet*) JavaNNetPtr)->GetOutputGroupSize();
      if (sizeof(double)==sizeof(jdouble))
      {
        int m = n-1;
        jdoubleArray* row = new jdoubleArray[n];
        try
        {
          for (; m>=0; m--)
            if (GrpSizes[m]>0)
            {
              row[m] = JavaEnv->NewDoubleArray(GrpSizes[m]);
              if (row[m]==NULL) { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Unable to create return array"); throw JavaException(); };
              JavaEnv->SetObjectArrayElement(result, m, row[m]);
              if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
              if ((prob[m] = (double*) (JavaEnv->GetDoubleArrayElements(row[m], NULL)))==NULL)
                { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Unable to access return array elements"); throw JavaException(); }
            }
          ((JavaNNet*) JavaNNetPtr)->ProbabilisticClassify((void*) inputPattern, prob);
        }
        catch(...)
        {
          jthrowable javaException = JavaEnv->ExceptionOccurred();
          if (javaException==NULL) for (m++; m<n; m++) if (GrpSizes[m]>0) JavaEnv->ReleaseDoubleArrayElements(row[m], prob[m], JNI_ABORT);
          else
          {
            JavaEnv->ExceptionClear();                                                                                // must clear pending Java exception before releasing array elements
            for (m++; m<n; m++) if (GrpSizes[m]>0) JavaEnv->ReleaseDoubleArrayElements(row[m], prob[m], JNI_ABORT);   // ... then we can re throw it (if one was thrown)
            if (JavaEnv->Throw(javaException)!=0) ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Unable to re-throw temporarily cleared Java exception");
          }
          delete[] row;
          throw;
        }
        for (m++; m<n; m++) if (GrpSizes[m]>0) JavaEnv->ReleaseDoubleArrayElements(row[m], prob[m], 0);
        delete[] row;
      }
      else   // sizeof(double) != sizeof(jdouble)
      {
        jdoubleArray row;
        jdouble* rowElements;
        int m = --n;
        try
        {
          for (; m>=0; m--) if (GrpSizes[m]>0) prob[m] = new double[GrpSizes[m]];
          ((JavaNNet*) JavaNNetPtr)->ProbabilisticClassify((void*) inputPattern, prob);
          for (; n>=0; n--)
            if (GrpSizes[n]>0)
            {
              row = JavaEnv->NewDoubleArray(GrpSizes[n]);
              if (row==NULL) { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Unable to create return array"); throw JavaException(); }
              JavaEnv->SetObjectArrayElement(result, n, row);
              if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
              rowElements = JavaEnv->GetDoubleArrayElements(row, NULL);
              if (rowElements==NULL) { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Unable to access return array elements"); throw JavaException(); }
              for (int i = GrpSizes[n]-1; i>=0; i--) rowElements[i] = prob[n][i];
              JavaEnv->ReleaseDoubleArrayElements(row, rowElements, 0);
              delete[] prob[n];
            }
        }
        catch(...)
        {
          for(; n>m; n--) if (GrpSizes[n]>0) delete[] prob[n];
          throw;
        }
      }
    }
    catch(...) { delete[] prob; throw; }
    delete[] prob;
    return result;
  }
  catch(const JavaException&) {}
  catch(const bad_alloc&) { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Insufficient memory to perform operation"); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Internal floating-point math error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - probabilisticClassify():  Internal error"); }
  return NULL;
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeNumLayers(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->NumLayers(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - numLayers():  Bad net pointer"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeTotalNumNodes(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->TotalNumNodes(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - totalNumNodes():  Bad net pointer"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeTotalNumLinks(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->TotalNumLinks(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - totalNumLinks():  Bad net pointer"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeNumNodesInLayer(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint l)
{
  try { return ((JavaNNet*) JavaNNetPtr)->NumNodesInLayer(l); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - numNodesInLayer():  Bad net pointer"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeNumInputs(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->NumInputs(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - numInputs():  Bad net pointer"); return 0; }
}



jint JNICALL Java_cdms_plugin_nnet_NNet_nativeNumOutputs(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr)
{
  try { return ((JavaNNet*) JavaNNetPtr)->NumOutputs(); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - numOutputs():  Bad net pointer"); return 0; }
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_nativeInputValue(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint i)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->InputValue(i); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - inputValue():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - inputValue():  Bad net pointer"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_nativeOutputValue(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint i)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->OutputValue(i); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - outputValue():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - outputValue():  Bad net pointer"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_nativeGetInputMin(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint i)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->GetInputMin(i); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getInputMin():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getInputMin():  Bad net pointer"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_nativeGetInputMax(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint i)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->GetInputMax(i); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getInputMax():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getInputMax():  Bad net pointer"); }
  return 0.0f;
}



jfloat JNICALL Java_cdms_plugin_nnet_NNet_nativeGetInputAvrg(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint i)
{
  signal(SIGFPE, FloatErrHandler);
  try { return ((JavaNNet*) JavaNNetPtr)->GetInputAvrg(i); }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getInputAvrg():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getInputAvrg():  Bad net pointer"); }
  return 0.0f;
}



jlong JNICALL Java_cdms_plugin_nnet_NNet_nativeGetNodeHandle(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint layer, jint nodeNum)
{
  try
  {
    jlong result = (jlong) ((JavaNNet*) JavaNNetPtr)->GetNode(layer, nodeNum);
    if (result==jlongNULL) ThrowJavaException(JavaEnv, "Neural Net Error - getNodeHandle():  Invalid parameters");
    return result;
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeHandle():  Bad net pointer"); return jlongNULL; }
}



jlongArray JNICALL Java_cdms_plugin_nnet_NNet_nativeGetNodeHandles(JNIEnv* JavaEnv, jobject, jlong JavaNNetPtr, jint layer)
{
  try
  {
    int n = ((JavaNNet*) JavaNNetPtr)->NumNodesInLayer(layer);
    NNetNode* firstNodeHandle = ((JavaNNet*) JavaNNetPtr)->GetNodesInLayer(layer);
    jlongArray result = JavaEnv->NewLongArray(n);
    if (result==NULL) return NULL;
    if (n==0) return result;
    jlong* resultElements = JavaEnv->GetLongArrayElements(result, NULL);
    if (resultElements==NULL) return result;
    for (n--; n>=0; n--) resultElements[n] = (jlong) (firstNodeHandle+n);
    JavaEnv->ReleaseLongArrayElements(result, resultElements, 0);
    return result;
  }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeHandles():  Bad net pointer"); return NULL; }
}



jintArray JNICALL Java_cdms_plugin_nnet_NNet_nativeGetLinkData(JNIEnv* JavaEnv, jobject, jlong NodePtr)
{
  signal(SIGFPE, FloatErrHandler);
  try
  {
    jfloat temp;
    int layer, nodeNum, n = ((NNetNode*) NodePtr)->NumLinks();
    const NNetLink* Link = ((NNetNode*) NodePtr)->GetLinkArray();
    jintArray result = JavaEnv->NewIntArray(n*3);
    if (result==NULL) return NULL;
    if (n==0) return result;
    jint* resultElements = JavaEnv->GetIntArrayElements(result, NULL);
    if (resultElements==NULL) return result;
    try
    {
      n--;
      for(int m = n*3; n>=0; n--)
      {
        Link[n].GetSource(layer, nodeNum);
        resultElements[m] = layer;
        resultElements[m+1] = nodeNum;
        if (sizeof(float)==sizeof(jint)) resultElements[m+2] = *((jint*) &(Link[n].weight));
        else resultElements[m+2] = *((jint*) &(temp = Link[n].weight));
        m -= 3;
      }
    }
    catch(...)
    {
      JavaEnv->ReleaseIntArrayElements(result, resultElements, JNI_ABORT);
      throw;
    }
    JavaEnv->ReleaseIntArrayElements(result, resultElements, 0);
    return result;
  }
  catch(const FPException&) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeLinkInfo():  Floating-point conversion error"); }
  catch(...) { ThrowJavaException(JavaEnv, "Neural Net Error - getNodeLinkInfo():  Internal error"); }
  return NULL;
}
