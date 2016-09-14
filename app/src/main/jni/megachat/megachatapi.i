#ifdef SWIGJAVA
#define __ANDROID__
#endif

%module(directors="1") mega
%{
#include "megachatapi.h"
%}

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

