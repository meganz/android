JAVA_BASE_OUTPUT_PATH?=java

CXX=g++
SWIG=swig
JAVAC=javac

CPPFLAGS=-Wall -fPIC -g -isystem /usr/include/cryptopp -I /usr/local/include/ -I . -I include

CFLAGS_JAVA_LIBRARY= -I /usr/lib/jvm/java/include/ -I /usr/lib/jvm/java/include/linux
CFLAGS_PYTHON_LIBRARY= -I /usr/include/python2.7
CFLAGS_ANDROID_LIBRARY=

LIB=-lpthread -lcryptopp -lcurl
LIB_JAVA_LIBRARY= -lcryptopp -lcurl
LIB_PYTHON_LIBRARY= -lcryptopp -lcurl -lpython2.7
LIB_ANDROID_LIBRARY= -lcryptopp -lcurl

CPP_SOURCES= sdk/src/attrmap.cpp \
    sdk/src/backofftimer.cpp \
    sdk/src/base64.cpp \
    sdk/src/command.cpp \
    sdk/src/commands.cpp \
    sdk/src/db.cpp \
    sdk/src/file.cpp \
    sdk/src/fileattributefetch.cpp \
    sdk/src/filefingerprint.cpp \
    sdk/src/filesystem.cpp \
    sdk/src/http.cpp \
    sdk/src/json.cpp \
    sdk/src/megaclient.cpp \
    sdk/src/node.cpp \
    sdk/src/pubkeyaction.cpp \
    sdk/src/request.cpp \
    sdk/src/serialize64.cpp \
    sdk/src/share.cpp \
    sdk/src/sharenodekeys.cpp \
    sdk/src/sync.cpp \
    sdk/src/transfer.cpp \
    sdk/src/transferslot.cpp \
    sdk/src/proxy.cpp \
    sdk/src/pendingcontactrequest.cpp \
    sdk/src/treeproc.cpp \
    sdk/src/user.cpp \
    sdk/src/useralerts.cpp \
    sdk/src/utils.cpp \
    sdk/src/waiterbase.cpp  \
    sdk/src/crypto/cryptopp.cpp  \
    sdk/src/crypto/sodium.cpp  \
    sdk/src/db/sqlite.cpp  \
    sdk/src/posix/net.cpp  \
    sdk/src/posix/fs.cpp  \
    sdk/src/posix/waiter.cpp \
    sdk/src/mega_utf8proc.cpp \
    sdk/src/gfx.cpp \
    sdk/src/gfx/external.cpp \
    sdk/src/thread/posixthread.cpp \
    sdk/src/logging.cpp \
    sdk/src/mega_http_parser.cpp \
    sdk/src/mega_zxcvbn.cpp \
    sdk/src/mediafileattribute.cpp \
    sdk/src/mega_evt_tls.cpp \
    sdk/src/mega_ccronexpr.cpp\
    sdk/src/raid.cpp \
    sdk/src/testhooks.cpp \
    sdk/src/heartbeats.cpp
    
C_SOURCES = sdk/src/mega_glob.c

HEADERS= sdk/include/mega.h \
	    sdk/include/mega/account.h \
	    sdk/include/mega/attrmap.h \
	    sdk/include/mega/backofftimer.h \
	    sdk/include/mega/base64.h \
	    sdk/include/mega/command.h \
	    sdk/include/mega/console.h \
	    sdk/include/mega/db.h \
	    sdk/include/mega/file.h \
	    sdk/include/mega/fileattributefetch.h \
	    sdk/include/mega/filefingerprint.h \
	    sdk/include/mega/filesystem.h \
	    sdk/include/mega/http.h \
	    sdk/include/mega/json.h \
	    sdk/include/mega/megaapp.h \
	    sdk/include/mega/megaclient.h \
	    sdk/include/mega/node.h \
	    sdk/include/mega/pubkeyaction.h \
	    sdk/include/mega/request.h \
	    sdk/include/mega/serialize64.h \
	    sdk/include/mega/share.h \
	    sdk/include/mega/sharenodekeys.h \
	    sdk/include/mega/sync.h \
	    sdk/include/mega/transfer.h \
	    sdk/include/mega/transferslot.h \
	    sdk/include/mega/proxy.h \
	    sdk/include/mega/pendingcontactrequest.h \
	    sdk/include/mega/treeproc.h \
	    sdk/include/mega/types.h \
	    sdk/include/mega/user.h \
		sdk/include/mega/useralerts.h \
	    sdk/include/mega/utils.h \
	    sdk/include/mega/waiter.h \
	    sdk/include/mega/crypto/cryptopp.h  \
	    sdk/include/mega/db/sqlite.h  \
	    sdk/include/megaapi.h \
	    sdk/include/megaapi_impl.h \
	    sdk/include/mega/posix/meganet.h  \
		sdk/include/mega/posix/megasys.h  \
		sdk/include/mega/posix/megafs.h  \
		sdk/include/mega/posix/megawaiter.h \
		sdk/include/mega/config-android.h \
		sdk/include/mega/mega_glob.h \
		sdk/include/mega/gfx/external.h \
		sdk/include/mega/thread/posixthread.h \
		sdk/include/mega/logging.h \
	    sdk/include/mega/mega_http_parser.h \
	    sdk/include/mega/mega_zxcvbn.h \
		sdk/include/mega/mediafileattribute.h \
	    sdk/include/mega/mega_evt_tls.h \
	    sdk/include/mega/mega_evt_queue.h \
	    sdk/include/mega/raid.h \
		sdk/include/mega/testhooks.h \
		sdk/include/mega/heartbeats.h

OBJECTS=$(CPP_SOURCES:.cpp=.o) $(C_SOURCES:.c=.o)
C_WRAPPER_SOURCES = sdk/src/megaapi.cpp sdk/src/megaapi_impl.cpp
C_WRAPPER_OBJECTS = sdk/src/megaapi.o sdk/src/megaapi_impl.o

PYTHON_WRAPS=python_wrap.cxx
PYTHON_HEADERS=python_wrap.h 
PYTHON_OBJECTS=python_wrap.o
PYTHON_OUTPUT_PATH=python/
PYTHON_BINDINGS=$(addprefix $(PYTHON_OUTPUT_PATH), mega.py)
PYTHON_LIBRARY=$(addprefix $(PYTHON_OUTPUT_PATH),_mega.so)


JAVA_PACKAGE_NAME=nz.mega.sdk
JAVA_OUTPUT_PATH=$(JAVA_BASE_OUTPUT_PATH)/$(subst .,/,$(JAVA_PACKAGE_NAME))
JAVA_LIBRARY_OUTPUT_PATH=java
JAVA_LIBRARY=$(addprefix $(JAVA_LIBRARY_OUTPUT_PATH)/,libmega.so)

JAVA_WRAPS=java_wrap.cxx 
JAVA_HEADERS=java_wrap.h  
JAVA_OBJECTS=java_wrap.o

JAVA_BINDINGS=$(addprefix $(JAVA_OUTPUT_PATH)/, \
				AccountBalance.java BalanceList.java MegaApi.java MegaGlobalListener.java \
				MegaRequestListener.java NodeList.java ShareList.java UserList.java \
				AccountDetails.java mega.java MegaTransfer.java StringList.java \
				AccountPurchase.java megaJNI.java MegaTransferListener.java  PurchaseList.java \
				TransactionList.java AccountSession.java MegaListener.java SessionList.java \
				TreeProcessor.java AccountTransaction.java MegaError.java MegaRequest.java \
				Node.java Share.java User.java)

SWIG_INTERFACE=sdk/bindings/megaapi.i
