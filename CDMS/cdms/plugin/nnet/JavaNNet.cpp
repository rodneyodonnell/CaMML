#include "JavaNNet.h"
//#include <mem.h>
#include <new.h>
#include <string.h>

using namespace std;  // make sure that operator new() and operator new[] throw bad_alloc on failure




// global (but only locally visible) variables:
///////////////////////////////////////////////

static jmethodID midGetNumInputComponents, midGetNumOutputComponents, midGetInputComponentType, midGetInputComponentMin, midGetInputComponentMax,   // method ID's for Java NNet class
                   midGetOutputComponentNumValues, midGetInputComponentValues, midGetOutputComponentValues, midOnEpochComplete;

static jmethodID midWriteInt, midWriteBool, midWriteFloat, midReadInt, midReadBool, midReadFloat, midSkipBytes;                                     // method ID's for Java objectInput/OutputStream classes




// global/exported non-class functions
//////////////////////////////////////

void InitNNetClassMethodIDs(JNIEnv* jEnv)
{
  jclass JNNet = jEnv->FindClass("cdms/plugin/nnet/NNet");
  if (JNNet==NULL) throw JavaException();
  if ((midGetNumInputComponents = jEnv->GetMethodID(JNNet, "getNumInputComponents", "()I"))==NULL) throw JavaException();
  if ((midGetNumOutputComponents = jEnv->GetMethodID(JNNet, "getNumOutputComponents", "()I"))==NULL) throw JavaException();
  if ((midGetInputComponentType = jEnv->GetMethodID(JNNet, "getInputComponentType", "(I)I"))==NULL) throw JavaException();
  if ((midGetInputComponentMin = jEnv->GetMethodID(JNNet, "getInputComponentMin", "(I)I"))==NULL) throw JavaException();
  if ((midGetInputComponentMax = jEnv->GetMethodID(JNNet, "getInputComponentMax", "(I)I"))==NULL) throw JavaException();
  if ((midGetOutputComponentNumValues = jEnv->GetMethodID(JNNet, "getOutputComponentNumValues", "(I)I"))==NULL) throw JavaException();
  if ((midGetInputComponentValues = jEnv->GetMethodID(JNNet, "getInputComponentValues", "(ILcdms/core/Value$Vector;)[I"))==NULL) throw JavaException();
  if ((midGetOutputComponentValues = jEnv->GetMethodID(JNNet, "getOutputComponentValues", "(ILcdms/core/Value$Vector;)[I"))==NULL) throw JavaException();
  if ((midOnEpochComplete = jEnv->GetMethodID(JNNet, "onEpochComplete", "(ID)Z"))==NULL) throw JavaException();
}




// locally visible non-class function:
//////////////////////////////////////

inline void StaticThrowJavaRuntimeException(JNIEnv* JavaEnv, const char* message)
{
  if (JavaEnv->ExceptionOccurred()==NULL)   // otherwise just throw the existing Java exception
  {
    jclass runException = JavaEnv->FindClass("java/lang/RuntimeException");
    if (runException==NULL) JavaEnv->ThrowNew(runException, message);
  }
  throw JavaException();
}




// JavaNNet class members:
//////////////////////////

inline void JavaNNet::ThrowJavaIOError()
{
  if (JavaEnv->ExceptionOccurred()==NULL)   // otherwise just throw the existing Java exception
  {
    jclass IOException = JavaEnv->FindClass("java/io/IOException");
    if (IOException!=NULL) JavaEnv->ThrowNew(IOException, "Neural Net Error:  Serialization error");
  }
  throw JavaException();
}



inline void JavaNNet::ThrowJavaRuntimeException(const char* message)
{
  if (JavaEnv->ExceptionOccurred()==NULL)   // otherwise just throw the existing Java exception
  {
    jclass runException = JavaEnv->FindClass("java/lang/RuntimeException");
    if (runException==NULL) JavaEnv->ThrowNew(runException, message);
  }
  throw JavaException();
}



void JavaNNet::GetInStreamClassMethodIDs()
{
  jclass ObjectInputStream = JavaEnv->FindClass("java/io/ObjectInputStream");
  if (ObjectInputStream==NULL) throw JavaException();
  if ((midReadInt = JavaEnv->GetMethodID(ObjectInputStream, "readInt", "()I"))==NULL) throw JavaException();
  if ((midReadBool = JavaEnv->GetMethodID(ObjectInputStream, "readBoolean", "()Z"))==NULL) throw JavaException();
  if ((midReadFloat = JavaEnv->GetMethodID(ObjectInputStream, "readFloat", "()F"))==NULL) throw JavaException();
  if ((midSkipBytes = JavaEnv->GetMethodID(ObjectInputStream, "skipBytes", "(I)I"))==NULL) throw JavaException();
}



void JavaNNet::GetOutStreamClassMethodIDs()
{
  jclass ObjectOutputStream = JavaEnv->FindClass("java/io/ObjectOutputStream");
  if (ObjectOutputStream==NULL) throw JavaException();
  if ((midWriteInt = JavaEnv->GetMethodID(ObjectOutputStream, "writeInt", "(I)V"))==NULL) throw JavaException();
  if ((midWriteBool = JavaEnv->GetMethodID(ObjectOutputStream, "writeBoolean", "(Z)V"))==NULL) throw JavaException();
  if ((midWriteFloat = JavaEnv->GetMethodID(ObjectOutputStream, "writeFloat", "(F)V"))==NULL) throw JavaException();
}



// sets up InputRange[] array;  also modifies the InputComponent[].range.width and .origin variables
void JavaNNet::CreateInputRangeArray()
{
  int n = NumInputs();
  InputRange = new InputRangeType[n--];
  try
  {
    for (int i = numInputComponents-1; i>=0; i--)
    {
      int t = InputComponent[i].type;
      if (t>1)   // enumerated type
      {
        if (t==2) { InputRange[n].min = -1.0f;  InputRange[n].max = 1.0f; InputRange[n--].avrg = 0.0f; }
        else for (float av = 1.0f/t; t>0; t--) { InputRange[n].min = 0.0f;  InputRange[n].max = 1.0f; InputRange[n--].avrg = av; }
      }
      else if (t==0)   // integer range
      {
        jint wdth = InputComponent[i].range.width.asInt;
        if (wdth!=0)
          if (wdth<0) { InputRange[n].min = 0.0f;  InputRange[n].max = 1.0f; InputRange[n--].avrg = 0.5f; }
          else
          {
            jint org = InputComponent[i].range.origin.asInt;
            if (org>=0) { InputRange[n].min = 0.0f;  InputRange[n].max = 1.0f; InputRange[n--].avrg = 0.5f; }
            else if (-2*org>wdth)
            {
              InputComponent[i].range.width.asInt = -org;
              InputComponent[i].range.origin.asInt = 0;
              InputRange[n].avrg = (InputRange[n].max = ((float) (wdth+org))/(-org))*0.5f-0.5f;
              InputRange[n--].min = -1.0f;
            }
            else
            {
              InputComponent[i].range.width.asInt = (wdth += org);
              InputComponent[i].range.origin.asInt = 0;
              InputRange[n].avrg = (InputRange[n].min = ((float) org)/wdth)*0.5f+0.5f;
              InputRange[n--].max = 1.0f;
            }
          }
      }
      else if (t==-1)  // continuous range
      {
        jfloat wdth = InputComponent[i].range.width.asFloat;
        if (wdth!=0)
          if (wdth<0) { InputRange[n].min = 0.0f;  InputRange[n].max = 1.0f; InputRange[n--].avrg = 0.5f; }
          else
          {
            jfloat org = InputComponent[i].range.origin.asFloat;
            if (org>=0) { InputRange[n].min = 0.0f;  InputRange[n].max = 1.0f; InputRange[n--].avrg = 0.5f; }
            else if (-2*org>wdth)
            {
              InputComponent[i].range.width.asFloat = -org;
              InputComponent[i].range.origin.asFloat = 0.0f;
              InputRange[n].avrg = (InputRange[n].max = -(wdth+org)/org)*0.5f-0.5f;
              InputRange[n--].min = -1.0f;
            }
            else
            {
              InputComponent[i].range.width.asFloat = (wdth += org);
              InputComponent[i].range.origin.asFloat = 0.0f;
              InputRange[n].avrg = (InputRange[n].min = org/wdth)*0.5f+0.5f;
              InputRange[n--].max = 1.0f;
            }
          }
      }
    }
    if (n!=-1) ThrowJavaRuntimeException("Neural Net Error:  Internal error during neural net initialization");
  }
  catch(...)
  {
    delete[] InputRange;
    throw;
  }
}



void JavaNNet::SetInputs(const void* InputPattern)
{
  intORfloat* ComponentValue = (intORfloat*) JavaEnv->GetIntArrayElements((jintArray) InputPattern, NULL);
  if (ComponentValue==NULL) ThrowJavaRuntimeException("Neural Net Error:  Unable to obtain neural net input array element values from Java array");

  int n = NumInputs()-1;                           // here we assume that the correct number of components was passed in the array;
  for (int i = numInputComponents-1; i>=0; i--)    // this should be checked on the Java side.
  {
    int t = InputComponent[i].type;
    if (t>1)   // enumerated type
    {
      jint v = ComponentValue[i].asInt - InputComponent[i].range.origin.asInt;
      if (t==2) Input[n--] = (v>0) ? 1.0f : -1.0f;
      else for (t--; t>=0; t--) Input[n--] = ((t==v) ? 1.0f : 0.0f);
    }
    else if (t==0)   // integer range
    {
      jint wdth = InputComponent[i].range.width.asInt;
      if (wdth!=0) Input[n--] = ((float) (ComponentValue[i].asInt - InputComponent[i].range.origin.asInt))/wdth;
    }
    else if (t==-1)  // continuous range
    {
      jfloat wdth = InputComponent[i].range.width.asFloat;
      if (wdth!=0) Input[n--] = (ComponentValue[i].asFloat - InputComponent[i].range.origin.asFloat)/wdth;
    }
  }

  JavaEnv->ReleaseIntArrayElements((jintArray) InputPattern, (jint*) ComponentValue, JNI_ABORT);
}



void JavaNNet::SetTrainingInputValues(int patternNum, const void* TrainingData, float* inputValues)
{
  jintArray InputData = (jintArray) JavaEnv->CallObjectMethod(JavaObject, midGetInputComponentValues, patternNum, ((TrainingDataType*) TrainingData)->Inputs);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  intORfloat* ComponentValue = (intORfloat*) JavaEnv->GetIntArrayElements(InputData, NULL);
  if (ComponentValue==NULL) ThrowJavaRuntimeException("Neural Net Error:  Unable to obtain neural net input array element values from Java array");

  int n = NumInputs()-1;                             // here we assume that the correct number of components was passed in the array;
  for (int i = numInputComponents-1; i>=0; i--)    // this should be checked on the Java side.
  {
    int t = InputComponent[i].type;
    if (t>1)   // enumerated type
    {
      jint v = ComponentValue[i].asInt - InputComponent[i].range.origin.asInt;
      if (t==2) inputValues[n--] = (v>0) ? 1.0f : -1.0f;
      else for (t--; t>=0; t--) inputValues[n--] = ((t==v) ? 1.0f : 0.0f);
    }
    else if (t==0)   // integer range
    {
      jint wdth = InputComponent[i].range.width.asInt;
      if (wdth!=0) inputValues[n--] = ((float) (ComponentValue[i].asInt - InputComponent[i].range.origin.asInt))/wdth;
    }
    else if (t==-1)  // continuous range
    {
      jfloat wdth = InputComponent[i].range.width.asFloat;
      if (wdth!=0) inputValues[n--] = (ComponentValue[i].asFloat - InputComponent[i].range.origin.asFloat)/wdth;
    }
  }

  JavaEnv->ReleaseIntArrayElements(InputData, (jint*) ComponentValue, JNI_ABORT);
}



void JavaNNet::SetDesiredOutputs(int patternNum, const void* TrainingData, bool* desiredOutputs)
{
  jintArray OutputData = (jintArray) JavaEnv->CallObjectMethod(JavaObject, midGetOutputComponentValues, patternNum, ((TrainingDataType*) TrainingData)->Outputs);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  jint* ComponentValue = JavaEnv->GetIntArrayElements(OutputData, NULL);
  if (ComponentValue==NULL) ThrowJavaRuntimeException("Neural Net Error:  Unable to obtain neural net desired output array element values from Java array");
  int n = NumOutputs()-1;                           // here we assume that the correct number of components was passed in the array;
  for (int i = numOutputComponents-1; i>=0; i--)    // this should be checked on the Java side.
    for (int j = OutputGroupSize[i]-1; j>=0; j--) desiredOutputs[n--] = (j==ComponentValue[i]);
  JavaEnv->ReleaseIntArrayElements(OutputData, ComponentValue, JNI_ABORT);
}



bool JavaNNet::OnEpochComplete(double err)
{
  bool result = (JavaEnv->CallBooleanMethod(JavaObject, midOnEpochComplete, CurrentEpoch(), err))||(IsAborted());
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  return result;
}



void JavaNNet::WriteInt(void* OutStream, int i)
{
  JavaEnv->CallVoidMethod((jobject) OutStream, midWriteInt, i);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
}



void JavaNNet::WriteBool(void* OutStream, bool b)
{
  JavaEnv->CallVoidMethod((jobject) OutStream, midWriteBool, b);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
}



void JavaNNet::WriteFloat(void* OutStream, float f)
{
  JavaEnv->CallVoidMethod((jobject) OutStream, midWriteFloat, f);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
}



int JavaNNet::ReadInt(void* InStream)
{
  int result = JavaEnv->CallIntMethod((jobject) InStream, midReadInt);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  return result;
}



bool JavaNNet::ReadBool(void* InStream)
{
  bool result = ((JavaEnv->CallBooleanMethod((jobject) InStream, midReadBool))!=0);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  return result;
}



float JavaNNet::ReadFloat(void* InStream)
{
  float result = JavaEnv->CallFloatMethod((jobject) InStream, midReadFloat);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  return result;
}



void JavaNNet::SkipBytes(void* InStream, int n)
{
  int skipped = JavaEnv->CallIntMethod((jobject) InStream, midSkipBytes, n);
  if (JavaEnv->ExceptionOccurred()!=NULL) throw JavaException();
  if (skipped!=n) ThrowJavaIOError();
}



JavaNNet::JavaNNet(JNIEnv* jEnv, jobject jObj, const JavaNNet& src) : NNet(src)
{
  JavaEnv = jEnv;
  JavaObject = jObj;
  numInputComponents = src.numInputComponents;
  numOutputComponents = src.numOutputComponents;
  InputComponent = new InputComponentType[numInputComponents];
  OutputGroupSize = NULL;
  InputRange = NULL;
  try
  {
    OutputGroupSize = new int[numOutputComponents];
    InputRange = new InputRangeType[NumInputs()];
    memcpy(InputComponent, src.InputComponent, numInputComponents*sizeof(InputComponentType));
    memcpy(OutputGroupSize, src.OutputGroupSize, numOutputComponents*sizeof(int));
    memcpy(InputRange, src.InputRange, NumInputs()*sizeof(InputRangeType));
  }
  catch(...)
  {
    delete[] InputComponent;
    delete[] OutputGroupSize;
    delete[] InputRange;
    throw;
  }
}



JavaNNet::JavaNNet(JNIEnv* jEnv, jobject jObj, void* InStream)  // default base (NNet) class constructor used
{
  JavaEnv = jEnv;
  JavaObject = jObj;
  GetInStreamClassMethodIDs();
  if (!LoadFromStream(InStream)) ThrowJavaIOError();
  int nIn, nOut;
  GetInterfaceInfo(jEnv, jObj, numInputComponents, numOutputComponents, InputComponent, OutputGroupSize, nIn, nOut);
  try
  {
    if ((nIn!=NumInputs())||(nOut!=NumOutputs())) ThrowJavaIOError();
    CreateInputRangeArray();
  }
  catch(...)
  {
    delete[] InputComponent;
    delete[] OutputGroupSize;
    throw;
  }
};



// retrieves information about the CDMS input and output types for the network
void JavaNNet::GetInterfaceInfo(JNIEnv* jEnv, jobject jObj, int& numInputComponents, int& numOutputComponents, InputComponentType*& inComponents, int*& outSizes, int& numInputs, int& numOutputs)
{
  inComponents = NULL;
  outSizes = NULL;
  numInputComponents = jEnv->CallIntMethod(jObj, midGetNumInputComponents);
  if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
  numOutputComponents = jEnv->CallIntMethod(jObj, midGetNumOutputComponents);
  if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
  inComponents = new InputComponentType[numInputComponents];
  try
  {
    outSizes = new int[numOutputComponents];
    numInputs = 0;
    numOutputs = 0;
    int i;
    for (i = numInputComponents-1; i>=0; i--)
    {
      inComponents[i].type = jEnv->CallIntMethod(jObj, midGetInputComponentType, i);
      if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
      inComponents[i].range.origin.asInt = jEnv->CallIntMethod(jObj, midGetInputComponentMin, i);
      if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
      if (inComponents[i].type>1)  // enumerated type;  can ignore enumerations with exactly 1 possible value (i.e. constant input)
      {
        if (inComponents[i].type==2) numInputs++; else numInputs += inComponents[i].type;  // boolean (2-valued) inputs map to single input,
      }                                                                                    // otherwise 1 input per possible value
      else if (inComponents[i].type==0)   // integer range
      {
        jint max = jEnv->CallIntMethod(jObj, midGetInputComponentMax, i);
        if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
        if ((inComponents[i].range.width.asInt = max-inComponents[i].range.origin.asInt)>0)
        {
          numInputs++;
          if (max<=0)
          {
             inComponents[i].range.origin.asInt = max;
             inComponents[i].range.width.asInt = -inComponents[i].range.width.asInt;
          }
        }
        else if (inComponents[i].range.width.asInt<0) StaticThrowJavaRuntimeException(jEnv, "Neural Net Error:  Invalid range declared for discrete input variable (max < min)");
      }
      else if (inComponents[i].type==-1)  // continuous range
      {
        intORfloat max;
        max.asInt = jEnv->CallIntMethod(jObj, midGetInputComponentMax, i);
        if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
        if ((inComponents[i].range.width.asFloat = max.asFloat-inComponents[i].range.origin.asFloat)>0)
        {
          numInputs++;
          if (max.asFloat<=0)
          {
             inComponents[i].range.origin.asFloat = max.asFloat;
             inComponents[i].range.width.asFloat = -inComponents[i].range.width.asFloat;
          }
        }
        else if (inComponents[i].range.width.asFloat<0) StaticThrowJavaRuntimeException(jEnv, "Neural Net Error:  Invalid range declared for scalar input variable (max < min)");
      }
    }
    for (i = numOutputComponents-1; i>=0; i--)
    {
      numOutputs += (outSizes[i] = jEnv->CallIntMethod(jObj, midGetOutputComponentNumValues, i));
      if (jEnv->ExceptionOccurred()!=NULL) throw JavaException();
    }
  }
  catch(...)
  {
    delete[] inComponents;
    inComponents = NULL;
    delete[] outSizes;
    outSizes = NULL;
    throw;
  }
}

