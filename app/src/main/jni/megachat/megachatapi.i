#ifdef SWIGJAVA
#define __ANDROID__
#endif

%module(directors="1") megachat
%{
#include "megachatapi.h"
%}
%import "megaapi.h"

#ifdef SWIGJAVA

//Use compilation-time constants in Java
%javaconst(1);

//Make the "delete" method protected
%typemap(javadestruct, methodname="delete", methodmodifiers="protected synchronized") SWIGTYPE 
{   
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        $jnicall;
      }
      swigCPtr = 0;
    }
}

%javamethodmodifiers copy ""

%runtime
%{
    extern JavaVM *MEGAjvm;
    namespace webrtc 
    {
        class JVM 
        {
            public:
                static void Initialize(JavaVM* jvm, jobject context);
        };
    };
%}

%typemap(check) mega::MegaApi *megaApi
%{
    if (!MEGAjvm)
    { 
        jenv->GetJavaVM(&MEGAjvm);
    }
    
    MEGAjvm->AttachCurrentThread(&jenv, NULL);
    jclass appGlobalsClass = jenv->FindClass("android/app/AppGlobals");
    jmethodID getInitialApplicationMID = jenv->GetStaticMethodID(appGlobalsClass,"getInitialApplication","()Landroid/app/Application;");
    jobject context = jenv->CallStaticObjectMethod(appGlobalsClass, getInitialApplicationMID);

    // Initialize the Java environment (currently only used by the audio manager).
    webrtc::JVM::Initialize(MEGAjvm, context);
    //MEGAjvm->DetachCurrentThread();
%}

#endif

//Generate inheritable wrappers for listener objects
%feature("director") megachat::MegaChatRequestListener;
%feature("director") megachat::MegaChatCallListener;
%feature("director") megachat::MegaChatVideoListener;
%feature("director") megachat::MegaChatListener;

typedef long long time_t;
typedef long long uint64_t;
typedef long long int64_t;

%include "megachatapi.h"

