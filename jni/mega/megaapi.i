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
%feature("director") MegaGlobalListener;
%feature("director") MegaListener;
%feature("director") TreeProcessor;

//Ignore internal classes
%ignore MegaCurlHttpIO;
%ignore PosixFileAccess;
%ignore threadEntryPoint;
%ignore HttpIO;
%ignore MegaApp;
%ignore FileAccess;
%ignore SearchTreeProcessor;
%ignore RequestQueue;

//Don't wrap internal constructors
//(these objects should be created internally only)

%ignore mega::Node::Node;
%ignore mega::User::User;
%ignore mega::Share::Share;
%ignore ArrayWrapper::ArrayWrapper;
%ignore NodeList::NodeList;
%ignore UserList::UserList;
//%ignore BalanceList::BalanceList;
//%ignore SessionList::SessionList;
//%ignore PurchaseList::PurchaseList;
//%ignore TransactionList::TransactionList;
%ignore StringList::StringList;
%ignore MegaRequest::MegaRequest;
%ignore MegaRequest::getNumDetails;
%ignore MegaTransfer::MegaTransfer;
%ignore MegaError::MegaError;

//Don't wrap internal setters
//(This objects should be modified internally only)
%rename("$ignore", regextarget=1, fullname=1) "MegaRequest::set.*";
%rename("$ignore", regextarget=1, fullname=1) "MegaTransfer::set.*";
%rename("$ignore", regextarget=1, fullname=1) "MegaError::set.*";

//Tell SWIG that these classes exist to avoid warnings
namespace mega {
class HttpIO {};
class MegaApp {};
class FileAccess {};
class AccountDetails {};
class AccountBalance {};
class AccountSession {};
class AccountPurchase {};
class AccountTransaction {};
class User {};
class Node {};
class Share {};
}

%newobject MegaError::copy;
%newobject MegaRequest::copy;
%newobject MegaRequest::getPublicNode;

%newobject MegaApi::getChildren;
%newobject MegaApi::getContacts;
%newobject MegaApi::getInShares;
%newobject MegaApi::getOutShares;
%newobject MegaApi::getNodePath;
%newobject MegaApi::getBase64PwKey;
%newobject MegaApi::getStringHash;
%newobject MegaApi::search;
%newobject MegaApi::ebcEncryptKey;
%newobject MegaApi::getMyEmail;


typedef long long uint64_t;
typedef uint64_t handle;
typedef uint32_t dstime;
typedef long long int64_t;
typedef int64_t m_off_t;

//Include all new classes
%include "megaapi.h"

//Tell SWIG about tempates
%template(NodeList) ArrayWrapper<mega::Node*>;
%template(UserList) ArrayWrapper<mega::User*>;
//%template(BalanceList) ArrayWrapper<mega::AccountBalance>;
//%template(SessionList) ArrayWrapper<mega::AccountSession>;
//%template(PurchaseList) ArrayWrapper<mega::AccountPurchase>;
//%template(TransactionList) ArrayWrapper<mega::AccountTransaction>;
%template(StringList) ArrayWrapper<const char*>;
%template(ShareList) ArrayWrapper<mega::Share*>;

//Constants for Python
#ifdef SWIGPYTHON
%constant int User_VISIBILITY_UNKNOWN = -1;
%constant int User_HIDDEN = 0;
%constant int User_VISIBLE = 1;
%constant int User_ME = 2;
%constant int Node_TYPE_UNKNOWN = -1;
%constant int Node_FILENODE = 0;
%constant int Node_FOLDERNODE = 1;
%constant int Node_ROOTNODE = 2;
%constant int Node_INCOMINGNODE = 3;
%constant int Node_RUBBISHNODE = 4;
%constant int Node_MAILNODE = 5;
#endif


%extend mega::User
{
public:
#ifndef SWIGPYTHON
	enum { VISIBILITY_UNKNOWN = -1, HIDDEN = 0, VISIBLE, ME };
#endif
	
	const char* getEmail() { return $self->email.c_str(); }
	int getVisibility()	{ return $self->show; }
	long long getTimestamp() { return $self->ctime; }
	const char *toString() { return $self->email.c_str(); }
	const char *__str__() { return $self->email.c_str(); }
}

%extend mega::Share
{
	const char *getAccessLevel()
	{
		switch($self->access)
		{
			case mega::ACCESS_UNKNOWN: return "";
			case mega::RDONLY: return "r";
			case mega::RDWR: return "rw";
			case mega::FULL: return "full";
			case mega::OWNER: return "own"; 
		}
		return "";
	}
		
	User *getUser() { return $self->user; }
	long long getTimestamp() { return $self->ts; }
}

%extend mega::AccountBalance
{
public:
	double getAmount() { return $self->amount; }
	const char* getCurrency() { return $self->currency; }
}

%extend mega::AccountSession
{
public:
	long long getTimestamp() { return $self->timestamp; }
	long long getMru() { return $self->mru; }
	const char* getUserAgent() { return $self->useragent.c_str(); }
	const char* getIp() { return $self->ip.c_str(); }
	const char* getCountry() { return $self->country; }
	int getCurrent() { return $self->current; }
}

%extend mega::AccountPurchase
{
public:
	long long getTimestamp() { return $self->timestamp; }
	const char *getHandle() { return $self->handle; }
	const char *getCurrency() { return $self->currency; }
	double getAmount() { return $self->amount; }
	int getMethod() { return $self->method; }
}

%extend mega::AccountTransaction
{
public:
	long long getTimestamp() { return $self->timestamp; }
	const char *getHandle() { return $self->handle; }
	const char *getCurrency() { return $self->currency; }
	double getDelta() { return $self->delta; }
}

%newobject mega::AccountDetails::getBalances;
%newobject mega::AccountDetails::getSessions;
%newobject mega::AccountDetails::getPurchases;
%newobject mega::AccountDetails::getTransactions;
%extend mega::AccountDetails
{
public:
	long long getUsedStorage() { return $self->storage_used; }
	long long getMaxStorage() { return $self->storage_max; }
	long long getOwnUsedTransfer() { return $self->transfer_own_used; }
	long long getSrvUsedTransfer() { return $self->transfer_srv_used; }
	long long getMaxTransfer() { return $self->transfer_max; }
	long getSrvRatio() { return $self->srv_ratio; }
	int getProLevel() { return $self->pro_level; }
	char getSubscriptionType() { return $self->subscription_type; }
	long getProExpiration() { return $self->pro_until; }
		
	/*BalanceList* getBalances() 
	{ 
	    return new BalanceList(&($self->balances[0]), $self->balances.size());
	}
	
	SessionList* getSessions() 
	{
	    return new SessionList(&($self->sessions[0]), $self->sessions.size());
	}
	
	PurchaseList* getPurchases() 
	{ 
	    return new PurchaseList(&($self->purchases[0]), $self->purchases.size());
	}
	
	TransactionList* getTransactions() 
	{ 
	    return new TransactionList(&($self->transactions[0]), $self->transactions.size());
	}*/
}
