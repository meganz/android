#ifdef SWIGJAVA
#define __ANDROID__
#endif

%module(directors="1") mega
%{
#include "megaapi.h"
%}

#ifdef SWIGJAVA

//Automatic load of the native library
%pragma(java) jniclasscode=%{
  static {
        try {
            System.loadLibrary("mega");
        } catch (UnsatisfiedLinkError e1) {
            try {
                System.load(System.getProperty("user.dir") + "/libmega.so");
            } catch (UnsatisfiedLinkError e2) {
                try {
                    System.load(System.getProperty("user.dir") + "/libs/libmegajava.so");
                } catch (UnsatisfiedLinkError e3) {
                    try {
                        System.load(System.getProperty("user.dir") + "/libs/mega.dll");
                    } catch (UnsatisfiedLinkError e4) {
                        System.err.println("Native code library failed to load. \n" + e1 + "\n" + e2 + "\n" + e3 + "\n" + e4);
                        System.exit(1);
                    }
                }
            }
        }
    }
%}

//Use compilation-time constants in Java
%javaconst(1);

//Reduce the visibility of internal classes
%typemap(javaclassmodifiers) mega::MegaApi "class";
%typemap(javaclassmodifiers) mega::MegaListener "class";
%typemap(javaclassmodifiers) mega::MegaRequestListener "class";
%typemap(javaclassmodifiers) mega::MegaTransferListener "class";
%typemap(javaclassmodifiers) mega::MegaGlobalListener "class";
%typemap(javaclassmodifiers) mega::MegaTreeProcessor "class";
%typemap(javaclassmodifiers) mega::MegaLogger "class";
%typemap(javaclassmodifiers) mega::NodeList "class";
%typemap(javaclassmodifiers) mega::TransferList "class";
%typemap(javaclassmodifiers) mega::ShareList "class";
%typemap(javaclassmodifiers) mega::UserList "class";

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
%feature("director") mega::MegaRequestListener;
%feature("director") mega::MegaTransferListener;
%feature("director") mega::MegaLogger;


#ifdef SWIGJAVA
%typemap(directorargout) (const char *time, int loglevel, const char *source, const char *message)
%{
	jenv->DeleteLocalRef(jtime); 
	jenv->DeleteLocalRef(jsource);
	jenv->DeleteLocalRef(jmessage); 
%}


%apply (char *STRING, size_t LENGTH) {(char *buffer, size_t size)};
%typemap(directorargout) (char *buffer, size_t size)
%{ jenv->DeleteLocalRef($input); %}
#endif


%feature("director") mega::MegaGlobalListener;
%feature("director") mega::MegaListener;
%feature("director") mega::MegaTreeProcessor;
%feature("director") mega::MegaGfxProcessor;


#ifdef SWIGJAVA
%typemap(directorargout) (const char* path)
%{ 
	jenv->DeleteLocalRef(jpath); 
%}

%apply (char *STRING, size_t LENGTH) {(char *bitmapData, size_t size)};
%typemap(directorin, descriptor="[B") (char *bitmapData, size_t size)
%{ 
	jbyteArray jb = (jenv)->NewByteArray($2);
	$input = jb;
%}
%typemap(directorargout) (char *bitmapData, size_t size)
%{ 
	jenv->GetByteArrayRegion($input, 0, $2, (jbyte *)$1);
	jenv->DeleteLocalRef($input);
%}
#endif

#ifdef SWIGPHP

//Disable the management of director parameters
//to workaround several SWIG bugs
%typemap(directorin) SWIGTYPE* %{ %}
%typemap(directorout) SWIGTYPE* %{ %}

//Rename some overloaded functions to workaround a bug in SWIG
%rename (getInSharesAll, fullname=1) mega::MegaApi::getInShares();
%rename (getOutSharesAll, fullname=1) mega::MegaApi::getOutShares();
%rename (getTransfersAll, fullname=1) mega::MegaApi::getTransfers();
%rename ("$ignore", fullname=1) mega::MegaApi::startUpload(const char*, MegaNode*, int64_t, MegaTransferListener*);
%rename ("$ignore", fullname=1) mega::MegaApi::startUpload(const char*, MegaNode*, int64_t);
%rename ("$ignore", fullname=1) mega::MegaApi::startUpload(const char*, MegaNode*, const char*, MegaTransferListener*);
%rename ("$ignore", fullname=1) mega::MegaApi::startUpload(const char*, MegaNode*, const char*);
%rename ("$ignore", fullname=1) mega::MegaApi::startUpload(const char*, MegaNode*, const char*, int64_t, MegaTransferListener*);
%rename ("$ignore", fullname=1) mega::MegaApi::startUpload(const char*, MegaNode*, const char*, int64_t);
#endif

%ignore mega::MegaApi::MEGA_DEBRIS_FOLDER;
%ignore mega::MegaNode::getNodeKey;
%ignore mega::MegaNode::getAttrString;
%ignore mega::MegaListener::onSyncStateChanged;
%ignore mega::MegaListener::onSyncFileStateChanged;
%ignore mega::MegaTransfer::getListener;
%ignore mega::MegaRequest::getListener;
%ignore mega::MegaHashSignature;

%newobject mega::MegaError::copy;
%newobject mega::MegaRequest::copy;
%newobject mega::MegaTransfer::copy;
%newobject mega::MegaTransferList::copy;
%newobject mega::MegaNode::copy;
%newobject mega::MegaNodeList::copy;
%newobject mega::MegaShare::copy;
%newobject mega::MegaShareList::copy;
%newobject mega::MegaUser::copy;
%newobject mega::MegaUserList::copy;
%newobject mega::MegaContactRequest::copy;
%newobject mega::MegaContactRequestList::copy;
%newobject mega::MegaRequest::getPublicMegaNode;
%newobject mega::MegaTransfer::getPublicMegaNode;
%newobject mega::MegaNode::getBase64Handle;
%newobject mega::MegaApi::getChildren;
%newobject mega::MegaApi::getChildNode;
%newobject mega::MegaApi::getContacts;
%newobject mega::MegaApi::getTransfers;
%newobject mega::MegaApi::getContact;
%newobject mega::MegaApi::getInShares;
%newobject mega::MegaApi::getOutShares;
%newobject mega::MegaApi::getNodePath;
%newobject mega::MegaApi::getBase64PwKey;
%newobject mega::MegaApi::getStringHash;
%newobject mega::MegaApi::search;
%newobject mega::MegaApi::ebcEncryptKey;
%newobject mega::MegaApi::getMyEmail;
%newobject mega::MegaApi::getMyUserHandle;
%newobject mega::MegaApi::dumpSession;
%newobject mega::MegaApi::getNodeByPath;
%newobject mega::MegaApi::getNodeByHandle;
%newobject mega::MegaApi::getRootNode;
%newobject mega::MegaApi::getInboxNode;
%newobject mega::MegaApi::getRubbishNode;
%newobject mega::MegaApi::getParentNode;
%newobject mega::MegaApi::getFingerprint;
%newobject mega::MegaApi::getNodeByFingerprint;
%newobject mega::MegaApi::hasFingerprint;
%newobject mega::MegaApi::exportMasterKey;
%newobject mega::MegaApi::getTransferByTag;
%newobject mega::MegaApi::getCRC;
%newobject mega::MegaApi::getNodeByCRC;
%newobject mega::MegaApi::getSessionTransferURL;
%newobject mega::MegaApi::getIncomingContactRequests;
%newobject mega::MegaApi::getOutgoingContactRequests;
%newobject mega::MegaApi::getPendingOutShares;
%newobject mega::MegaApi::getContactRequestByHandle;
%newobject mega::MegaRequest::getMegaAccountDetails;
%newobject mega::MegaRequest::getPricing;
%newobject mega::MegaAccountDetails::getSubscriptionMethod;
%newobject mega::MegaAccountDetails::getSubscriptionCycle;

typedef long long time_t;
typedef long long uint64_t;
typedef long long int64_t;

%include "megaapi.h"
