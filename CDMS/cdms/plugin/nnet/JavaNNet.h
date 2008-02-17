#ifndef JavaNNetH
#define JavaNNetH

#include <stddef.h>           // definition of NULL
#include <jni.h>
#include "NNetClass.h"




void InitNNetClassMethodIDs(JNIEnv* jEnv);    // Need to call this once, when NNet library loads



class JavaException {};      // empty class, just used as an exception type for flagging Java exceptions



class JavaNNet : public NNet
{
public:
  union intORfloat { jint asInt; jfloat asFloat; };
  struct ComponentRange { intORfloat origin; intORfloat width; };
  struct InputComponentType { jint type; ComponentRange range; };
  struct InputRangeType { float min; float max; float avrg; };
  struct TrainingDataType { jobject Inputs, Outputs; };
private:
  JNIEnv* JavaEnv;
  jobject JavaObject;
  int numInputComponents, numOutputComponents;
  InputComponentType* InputComponent;   // number of discrete values (= type, type>0) OR integer or continuous range (type = 0, -1 respectively) for each input component
  int* OutputGroupSize;                 // groupings of output nodes into different variables
  InputRangeType* InputRange;
  void ThrowJavaIOError();                                // throws Java and C++ exceptions simultaneously
  void ThrowJavaRuntimeException(const char* message);    // throws Java and C++ exceptions simultaneously
  void GetInStreamClassMethodIDs();   // must always be called immediately prior to using a Java stream, because the stream class may have been un- then re-loaded since last use
  void GetOutStreamClassMethodIDs();  // must always be called immediately prior to using a Java stream, because the stream class may have been un- then re-loaded since last use
  void CreateInputRangeArray();       // sets up InputRange[] array;  also modifies the InputComponent[].range.width and .origin variables
protected:
  virtual void SetInputs(const void* InputPattern);
  virtual void SetTrainingInputValues(int patternNum, const void* TrainingData, float* inputValues);
  virtual void SetDesiredOutputs(int patternNum, const void* TrainingData, bool* desiredOutputs);
  virtual bool OnEpochComplete(double err);
  virtual void WriteInt(void* OutStream, int i);
  virtual void WriteBool(void* OutStream, bool b);
  virtual void WriteFloat(void* OutStream, float f);
  virtual int ReadInt(void* InStream);
  virtual bool ReadBool(void* InStream);
  virtual float ReadFloat(void* InStream);
  virtual void SkipBytes(void* InStream, int n);
public:
  void SetJavaObject(JNIEnv* jEnv, jobject jObj) { JavaEnv = jEnv; JavaObject = jObj; };
  JavaNNet(JNIEnv* jEnv, jobject jObj, int numInComponents, int numOutComponents, InputComponentType* inComponents, int* outSizes, int numInputs, int numOutputs, int numHiddenlayers, const int* layerSizes, bool createLinks = true)
          : NNet(numInputs, numOutputs, numHiddenlayers, layerSizes, createLinks)
          { SetJavaObject(jEnv, jObj); numInputComponents = numInComponents; numOutputComponents = numOutComponents; InputComponent = inComponents; OutputGroupSize = outSizes; CreateInputRangeArray(); };
  JavaNNet(JNIEnv* jEnv, jobject jObj, const JavaNNet& src);
  JavaNNet(JNIEnv* jEnv, jobject jObj, void* InStream);
  ~JavaNNet() { delete[] InputRange; delete[] OutputGroupSize; delete[] InputComponent; };
  float GetInputMin(int i)  { return InputRange[i].min; };
  float GetInputMax(int i)  { return InputRange[i].max; };
  float GetInputAvrg(int i) { return InputRange[i].avrg; };
  virtual void GetInputRange(int i, float& min, float& max, float& avrg) const { min = InputRange[i].min; max = InputRange[i].max; avrg = InputRange[i].avrg; };
  void Classify(const void* InputPattern, int* outputClassifications) { NNet::Classify(InputPattern, numOutputComponents, OutputGroupSize, outputClassifications); };
  void ProbabilisticClassify(const void* InputPattern, double** probabilities) { NNet::ProbabilisticClassify(InputPattern, numOutputComponents, OutputGroupSize, probabilities); };
  int GetNumOutputComponents() { return numOutputComponents; };
  const int* GetOutputGroupSize() { return OutputGroupSize; };
  static void GetInterfaceInfo(JNIEnv* jEnv, jobject jObj, int& numInputComponents, int& numOutputComponents, InputComponentType*& inComponents, int*& outSizes, int& numInputs, int& numOutputs);
};





#endif

