#define __ANDROID__
#define USE_EXTERNAL_GFX

%module(directors="1") mega
%{
#include "megaapi.h"
%}

//Automatic load of the native library
%pragma(java) jniclasscode=%{
  static 
  {
    try { System.loadLibrary("mega"); } 
    catch (UnsatisfiedLinkError e1) 
    {
		try { System.load(System.getProperty("user.dir")+"/libmega.so"); }
		catch(UnsatisfiedLinkError e2)
		{
			System.err.println("Native code library failed to load. \n" + e2);
			System.exit(1);
		}
    }
  }
%}

%insert("runtime") %{
#define SWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON
#define SWIG_JAVA_NO_DETACH_CURRENT_THREAD
%}

//Use compilation-time constants in Java
#ifdef SWIGJAVA
%javaconst(1);
#endif

//Generate inheritable wrappers for listener objects
%feature("director") MegaRequestListener;
%feature("director") MegaTransferListener;
%feature("director") MegaLogger;

%apply (char *STRING, size_t LENGTH) {(char *buffer, size_t size)};
%typemap(directorargout) (char *buffer, size_t size)
%{ jenv->DeleteLocalRef($input); %}

%feature("director") MegaGlobalListener;
%feature("director") MegaListener;
%feature("director") MegaTreeProcessor;
%feature("director") MegaGfxProcessor;

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


//Ignore sync features
%ignore mega::MegaApi::syncPathState;
%ignore mega::MegaApi::getSyncedNode;
%ignore mega::MegaApi::syncFolder;
%ignore mega::MegaApi::removeSync;
%ignore mega::MegaApi::resumeSync;
%ignore mega::MegaApi::getNumActiveSyncs;
%ignore mega::MegaApi::stopSyncs;
%ignore mega::MegaApi::getLocalPath;
%ignore mega::MegaApi::updateStatics;
%ignore mega::MegaApi::update;
%ignore mega::MegaApi::isIndexing;
%ignore mega::MegaApi::isWaiting;
%ignore mega::MegaApi::isSynced;
%ignore mega::MegaApi::setExcludedNames;
%ignore mega::MegaApi::moveToLocalDebris;
%ignore mega::MegaApi::MEGA_DEBRIS_FOLDER;
%ignore mega::MegaNode::getNodeKey;
%ignore mega::MegaNode::getAttrString;
%ignore mega::MegaNode::getLocalPath;
%ignore mega::MegaListener::onSyncStateChanged;
%ignore mega::MegaListener::onSyncFileStateChanged;

%newobject mega::MegaError::copy;
%newobject mega::MegaRequest::copy;
%newobject mega::MegaNode::copy;
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
%newobject mega::MegaApi::dumpSession;
%newobject mega::MegaApi::getNodeByPath;
%newobject mega::MegaApi::getNodeByHandle;
%newobject mega::MegaApi::getRootNode;
%newobject mega::MegaApi::getInboxNode;
%newobject mega::MegaApi::getRubbishNode;
%newobject mega::MegaApi::getParentNode;
%newobject mega::MegaApi::getMyEmail;
%newobject mega::MegaApi::getFingerprint;
%newobject mega::MegaApi::getNodeByFingerprint;
%newobject mega::MegaApi::hasFingerprint;

typedef long long time_t;
typedef long long uint64_t;
typedef long long int64_t;

%include "megaapi.h"
